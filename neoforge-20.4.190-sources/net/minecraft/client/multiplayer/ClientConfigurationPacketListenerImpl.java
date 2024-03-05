package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ClientConfigurationPacketListener;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket;
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientConfigurationPacketListenerImpl extends ClientCommonPacketListenerImpl implements TickablePacketListener, ClientConfigurationPacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final GameProfile localGameProfile;
    private RegistryAccess.Frozen receivedRegistries;
    private FeatureFlagSet enabledFeatures;
    private net.neoforged.neoforge.network.connection.ConnectionType connectionType = net.neoforged.neoforge.network.connection.ConnectionType.VANILLA;
    private boolean initializedConnection = false;
    private java.util.Map<net.minecraft.resources.ResourceLocation, net.minecraft.network.chat.Component> failureReasons = new java.util.HashMap<>();

    public ClientConfigurationPacketListenerImpl(Minecraft pMinecraft, Connection pConnection, CommonListenerCookie pCommonListenerCookie) {
        super(pMinecraft, pConnection, pCommonListenerCookie);
        this.localGameProfile = pCommonListenerCookie.localGameProfile();
        this.receivedRegistries = pCommonListenerCookie.receivedRegistries();
        this.enabledFeatures = pCommonListenerCookie.enabledFeatures();
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    @Override
    protected RegistryAccess.Frozen registryAccess() {
        return this.receivedRegistries;
    }

    @Override
    protected void handleCustomPayload(net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket p_295727_, CustomPacketPayload p_295411_) {
        if (p_295411_ instanceof net.neoforged.neoforge.network.payload.MinecraftRegisterPayload minecraftRegisterPayload) {
            this.connectionType = this.connectionType.withMinecraftRegisterPayload();
            net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onMinecraftRegister(this, minecraftRegisterPayload.newChannels());
            return;
        }
        if (p_295411_ instanceof net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload minecraftUnregisterPayload) {
            this.connectionType = this.connectionType.withMinecraftRegisterPayload();
            net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onMinecraftUnregister(this, minecraftUnregisterPayload.forgottenChannels());
            return;
        }
        if (p_295411_ instanceof net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload) {
            this.connectionType = this.connectionType.withNeoForgeQueryPayload();
            net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onNetworkQuery(this);
            return;
        }
        if (p_295411_ instanceof net.neoforged.neoforge.network.payload.ModdedNetworkPayload moddedNetworkPayload) {
            this.initializedConnection = true;
            net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onModdedNetworkConnectionEstablished(this, moddedNetworkPayload.configuration(), moddedNetworkPayload.play());
            return;
        }
        if (p_295411_ instanceof net.neoforged.neoforge.network.payload.ModdedNetworkSetupFailedPayload setupFailedPayload) {
            failureReasons = setupFailedPayload.failureReasons();
        }
        if (!this.connectionType.isNeoForge() && p_295411_ instanceof net.minecraft.network.protocol.common.custom.BrandPayload) {
            this.initializedConnection = true;
            if (!net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onVanillaNetworkConnectionEstablished(this)) {
                return;
            }
        }
        if (this.connectionType.isNotVanilla()) {
            net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onModdedPacketAtClient(this, p_295727_);
            return;
        }

        this.handleUnknownCustomPayload(p_295411_);
    }

    private void handleUnknownCustomPayload(CustomPacketPayload pPayload) {
        LOGGER.warn("Unknown custom packet payload: {}", pPayload.id());
    }

    @Override
    public void handleRegistryData(ClientboundRegistryDataPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);
        RegistryAccess.Frozen registryaccess$frozen = ClientRegistryLayer.createRegistryAccess()
            .replaceFrom(ClientRegistryLayer.REMOTE, pPacket.registryHolder())
            .compositeAccess();
        if (!this.connection.isMemoryConnection()) {
            registryaccess$frozen.registries().forEach(p_296478_ -> p_296478_.value().resetTags());
        }

        this.receivedRegistries = registryaccess$frozen;
    }

    @Override
    public void handleEnabledFeatures(ClientboundUpdateEnabledFeaturesPacket pPacket) {
        this.enabledFeatures = FeatureFlags.REGISTRY.fromNames(pPacket.features());
        //Fallback detection layer for vanilla servers
        if (!this.connectionType.isNeoForge()) {
            this.initializedConnection = true;
            net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onVanillaNetworkConnectionEstablished(this);
        }
    }

    @Override
    public void handleConfigurationFinished(ClientboundFinishConfigurationPacket pPacket) {
        this.connection.suspendInboundAfterProtocolChange();
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.minecraft);

        if (!this.initializedConnection && !this.connectionType.isNeoForge()) {
            //Fallback detection for servers with a delayed brand payload (BungeeCord)
            net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onVanillaNetworkConnectionEstablished(this);
        }

        this.connection
            .setListener(
                new ClientPacketListener(
                    this.minecraft,
                    this.connection,
                    new CommonListenerCookie(
                        this.localGameProfile,
                        this.telemetryManager,
                        this.receivedRegistries,
                        this.enabledFeatures,
                        this.serverBrand,
                        this.serverData,
                        this.postDisconnectScreen,
                        this.connectionType
                    )
                )
            );
        this.connection.resumeInboundAfterProtocolChange();
        net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().onConfigurationFinished(this);
        this.connection.send(new ServerboundFinishConfigurationPacket());
    }

    @Override
    public void tick() {
        this.sendDeferredPackets();
    }

    /**
     * Invoked when disconnecting, the parameter is a Component describing the reason for termination
     */
    @Override
    public void onDisconnect(Component pReason) {
        super.onDisconnect(pReason);
        this.minecraft.clearDownloadedResourcePacks();
    }

    @Override
    protected net.minecraft.client.gui.screens.Screen createDisconnectScreen(net.minecraft.network.chat.Component p_296470_) {
        final net.minecraft.client.gui.screens.Screen superScreen = super.createDisconnectScreen(p_296470_);
        if (failureReasons.isEmpty())
            return superScreen;

        return new net.neoforged.neoforge.client.gui.ModMismatchDisconnectedScreen(superScreen, p_296470_, failureReasons);
    }

    public net.neoforged.neoforge.network.connection.ConnectionType getConnectionType() {
        return connectionType;
    }
}
