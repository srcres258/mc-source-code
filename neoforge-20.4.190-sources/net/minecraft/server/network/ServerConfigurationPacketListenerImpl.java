package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.Nullable;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.config.JoinWorldTask;
import net.minecraft.server.network.config.ServerResourcePackConfigurationTask;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.TagNetworkSerialization;
import net.minecraft.world.flag.FeatureFlags;
import org.slf4j.Logger;

public class ServerConfigurationPacketListenerImpl extends ServerCommonPacketListenerImpl implements TickablePacketListener, ServerConfigurationPacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component DISCONNECT_REASON_INVALID_DATA = Component.translatable("multiplayer.disconnect.invalid_player_data");
    private final GameProfile gameProfile;
    private final Queue<ConfigurationTask> configurationTasks = new ConcurrentLinkedQueue<>();
    @Nullable
    private ConfigurationTask currentTask;
    private ClientInformation clientInformation;
    private net.neoforged.neoforge.network.connection.ConnectionType connectionType = net.neoforged.neoforge.network.connection.ConnectionType.VANILLA;
    private boolean isHandlingModdedConfigurationPhase = false;

    public ServerConfigurationPacketListenerImpl(MinecraftServer pServer, Connection pConnection, CommonListenerCookie pCookie) {
        super(pServer, pConnection, pCookie);
        this.gameProfile = pCookie.gameProfile();
        this.clientInformation = pCookie.clientInformation();
    }

    @Override
    protected GameProfile playerProfile() {
        return this.gameProfile;
    }

    /**
     * Invoked when disconnecting, the parameter is a Component describing the reason for termination
     */
    @Override
    public void onDisconnect(Component pReason) {
        LOGGER.info("{} lost connection: {}", this.gameProfile, pReason.getString());
        super.onDisconnect(pReason);
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    public void startConfiguration() {
        //Unregister all known play channels, and register all known configuration channels, then run negotiation.
        this.send(new net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload(net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().getInitialServerUnregisterChannels()));
        this.send(new net.neoforged.neoforge.network.payload.MinecraftRegisterPayload(net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().getInitialServerListeningChannels()));
        this.send(new net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload());
        this.send(new net.minecraft.network.protocol.common.ClientboundPingPacket(0));
    }

    private void runConfiguration() {
        this.send(new ClientboundCustomPayloadPacket(new BrandPayload(this.server.getServerModName())));
        LayeredRegistryAccess<RegistryLayer> layeredregistryaccess = this.server.registries();
        this.send(new ClientboundUpdateEnabledFeaturesPacket(FeatureFlags.REGISTRY.toNames(this.server.getWorldData().enabledFeatures())));
        this.send(
            new ClientboundRegistryDataPacket(
                new RegistryAccess.ImmutableRegistryAccess(RegistrySynchronization.networkedRegistries(layeredregistryaccess)).freeze()
            )
        );
        this.send(new ClientboundUpdateTagsPacket(TagNetworkSerialization.serializeTagsToNetwork(layeredregistryaccess)));
        this.addOptionalTasks();
        this.configurationTasks.add(new JoinWorldTask());
        this.startNextTask();
    }

    public void returnToWorld() {
        this.configurationTasks.add(new JoinWorldTask());
        this.startNextTask();
    }

    private void addOptionalTasks() {
        this.server.getServerResourcePack().ifPresent(p_296496_ -> this.configurationTasks.add(new ServerResourcePackConfigurationTask(p_296496_)));

        this.configurationTasks.add(new net.neoforged.neoforge.network.configuration.ModdedConfigurationPhaseStarted(this));
        this.configurationTasks.addAll(net.neoforged.fml.ModLoader.get().postEventWithReturn(new net.neoforged.neoforge.network.event.OnGameConfigurationEvent(this)).getConfigurationTasks());
        this.configurationTasks.add(new net.neoforged.neoforge.network.configuration.ModdedConfigurationPhaseCompleted(this));
    }

    @Override
    public void handleCustomPayload(net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket p_294276_) {
        if (p_294276_.payload() instanceof net.neoforged.neoforge.network.payload.MinecraftRegisterPayload minecraftRegisterPayload) {
            this.connectionType = this.connectionType.withMinecraftRegisterPayload();
            net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onMinecraftRegister(this, minecraftRegisterPayload.newChannels());
            return;
        }
        if (p_294276_.payload() instanceof net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload minecraftUnregisterPayload) {
            this.connectionType = this.connectionType.withMinecraftRegisterPayload();
            net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onMinecraftUnregister(this, minecraftUnregisterPayload.forgottenChannels());
            return;
        }
        if (p_294276_.payload() instanceof net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload moddedEnvironmentPayload) {
            this.connectionType = this.connectionType.withNeoForgeQueryPayload();
            net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance()
                    .onModdedConnectionDetectedAtServer(
                            this,
                            moddedEnvironmentPayload.configuration(),
                            moddedEnvironmentPayload.play()
                    );
            return;
        }

        if (!isHandlingModdedConfigurationPhase) {
            super.handleCustomPayload(p_294276_);
            return;
        }

        net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onModdedPacketAtServer(this, p_294276_);
    }

    @Override
    public void handlePong(net.minecraft.network.protocol.common.ServerboundPongPacket p_295142_) {
        super.handlePong(p_295142_);
        if (p_295142_.getId() == 0) {
            if (!this.connectionType.isNeoForge() && !net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onVanillaOrOtherConnectionDetectedAtServer(this)) {
                return;
            }

            this.runConfiguration();
        }
    }

    @Override
    public void handleClientInformation(ServerboundClientInformationPacket pPacket) {
        this.clientInformation = pPacket.information();
    }

    @Override
    public void handleResourcePackResponse(ServerboundResourcePackPacket pPacket) {
        super.handleResourcePackResponse(pPacket);
        if (pPacket.action().isTerminal()) {
            this.finishCurrentTask(ServerResourcePackConfigurationTask.TYPE);
        }
    }

    @Override
    public void handleConfigurationFinished(ServerboundFinishConfigurationPacket pPacket) {
        if (this.connectionType == net.neoforged.neoforge.network.connection.ConnectionType.OTHER) {
            //We need to also initialize this here, as the client may have sent the packet before we have finished our configuration.
            net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onModdedConnectionDetectedAtServer(this, java.util.Set.of(), java.util.Set.of());
        }
        net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onConfigurationFinished(this);
        this.connection.suspendInboundAfterProtocolChange();
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.server);
        this.finishCurrentTask(JoinWorldTask.TYPE);

        try {
            PlayerList playerlist = this.server.getPlayerList();
            if (playerlist.getPlayer(this.gameProfile.getId()) != null) {
                this.disconnect(PlayerList.DUPLICATE_LOGIN_DISCONNECT_MESSAGE);
                return;
            }

            Component component = playerlist.canPlayerLogin(this.connection.getRemoteAddress(), this.gameProfile);
            if (component != null) {
                this.disconnect(component);
                return;
            }

            ServerPlayer serverplayer = playerlist.getPlayerForLogin(this.gameProfile, this.clientInformation);
            playerlist.placeNewPlayer(this.connection, serverplayer, this.createCookie(this.clientInformation, this.connectionType));
            this.connection.resumeInboundAfterProtocolChange();
        } catch (Exception exception) {
            LOGGER.error("Couldn't place player in world", (Throwable)exception);
            this.connection.send(new ClientboundDisconnectPacket(DISCONNECT_REASON_INVALID_DATA));
            this.connection.disconnect(DISCONNECT_REASON_INVALID_DATA);
        }
    }

    @Override
    public void tick() {
        this.keepConnectionAlive();
    }

    private void startNextTask() {
        if (this.currentTask != null) {
            throw new IllegalStateException("Task " + this.currentTask.type().id() + " has not finished yet");
        } else if (this.isAcceptingMessages()) {
            ConfigurationTask configurationtask = this.configurationTasks.poll();
            if (configurationtask != null) {
                this.currentTask = configurationtask;
                configurationtask.start(this::send);
            }
        }
    }

    public void finishCurrentTask(ConfigurationTask.Type pTaskType) {
        ConfigurationTask.Type configurationtask$type = this.currentTask != null ? this.currentTask.type() : null;
        if (!pTaskType.equals(configurationtask$type)) {
            throw new IllegalStateException("Unexpected request for task finish, current task: " + configurationtask$type + ", requested: " + pTaskType);
        } else {
            this.currentTask = null;
            this.startNextTask();
        }
    }

    public void onModdedConfigurationPhaseStarted() {
        isHandlingModdedConfigurationPhase = true;
        finishCurrentTask(net.neoforged.neoforge.network.configuration.ModdedConfigurationPhaseStarted.TYPE);
    }

    public void onModdedConfigurationPhaseEnded() {
        isHandlingModdedConfigurationPhase = false;
        finishCurrentTask(net.neoforged.neoforge.network.configuration.ModdedConfigurationPhaseCompleted.TYPE);
    }

    public net.neoforged.neoforge.network.connection.ConnectionType getConnectionType() {
        return connectionType;
    }
}
