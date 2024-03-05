package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.util.VisibleForDebug;
import org.slf4j.Logger;

public abstract class ServerCommonPacketListenerImpl implements ServerCommonPacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int LATENCY_CHECK_INTERVAL = 15000;
    private static final Component TIMEOUT_DISCONNECTION_MESSAGE = Component.translatable("disconnect.timeout");
    protected final MinecraftServer server;
    public final Connection connection;
    private long keepAliveTime;
    private boolean keepAlivePending;
    private long keepAliveChallenge;
    private int latency;
    private volatile boolean suspendFlushingOnServerThread = false;

    public ServerCommonPacketListenerImpl(MinecraftServer pServer, Connection pConnection, CommonListenerCookie pCookie) {
        this.server = pServer;
        this.connection = pConnection;
        this.keepAliveTime = Util.getMillis();
        this.latency = pCookie.latency();
    }

    /**
     * Invoked when disconnecting, the parameter is a Component describing the reason for termination
     */
    @Override
    public void onDisconnect(Component pReason) {
        if (this.isSingleplayerOwner()) {
            LOGGER.info("Stopping singleplayer server as player logged out");
            this.server.halt(false);
        }
    }

    @Override
    public void handleKeepAlive(ServerboundKeepAlivePacket pPacket) {
        if (this.keepAlivePending && pPacket.getId() == this.keepAliveChallenge) {
            int i = (int)(Util.getMillis() - this.keepAliveTime);
            this.latency = (this.latency * 3 + i) / 4;
            this.keepAlivePending = false;
        } else if (!this.isSingleplayerOwner()) {
            this.disconnect(TIMEOUT_DISCONNECTION_MESSAGE);
        }
    }

    @Override
    public void handlePong(ServerboundPongPacket pPacket) {
    }

    @Override
    public void handleCustomPayload(ServerboundCustomPayloadPacket pPacket) {
    }

    @Override
    public void handleResourcePackResponse(ServerboundResourcePackPacket pPacket) {
        PacketUtils.ensureRunningOnSameThread(pPacket, this, this.server);
        if (pPacket.action() == ServerboundResourcePackPacket.Action.DECLINED && this.server.isResourcePackRequired()) {
            LOGGER.info("Disconnecting {} due to resource pack {} rejection", this.playerProfile().getName(), pPacket.id());
            this.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
        }
    }

    protected void keepConnectionAlive() {
        this.server.getProfiler().push("keepAlive");
        long i = Util.getMillis();
        if (i - this.keepAliveTime >= 15000L) {
            if (this.keepAlivePending) {
                this.disconnect(TIMEOUT_DISCONNECTION_MESSAGE);
            } else {
                this.keepAlivePending = true;
                this.keepAliveTime = i;
                this.keepAliveChallenge = i;
                this.send(new ClientboundKeepAlivePacket(this.keepAliveChallenge));
            }
        }

        this.server.getProfiler().pop();
    }

    public void suspendFlushing() {
        this.suspendFlushingOnServerThread = true;
    }

    public void resumeFlushing() {
        this.suspendFlushingOnServerThread = false;
        this.connection.flushChannel();
    }

    public void send(Packet<?> pPacket) {
        this.send(pPacket, null);
    }

    @Override
    public void send(Packet<?> pPacket, @Nullable PacketSendListener pListener) {
        if (!net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().canSendPacket(pPacket, this)) {
            return;
        }

        boolean flag = !this.suspendFlushingOnServerThread || !this.server.isSameThread();

        try {
            this.connection.send(pPacket, pListener, flag);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Sending packet");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Packet being sent");
            crashreportcategory.setDetail("Packet class", () -> pPacket.getClass().getCanonicalName());
            throw new ReportedException(crashreport);
        }
    }

    public void disconnect(Component pReason) {
        this.connection.send(new ClientboundDisconnectPacket(pReason), PacketSendListener.thenRun(() -> this.connection.disconnect(pReason)));
        this.connection.setReadOnly();
        this.server.executeBlocking(this.connection::handleDisconnection);
    }

    protected boolean isSingleplayerOwner() {
        return this.server.isSingleplayerOwner(this.playerProfile());
    }

    protected abstract GameProfile playerProfile();

    @VisibleForDebug
    public GameProfile getOwner() {
        return this.playerProfile();
    }

    public int latency() {
        return this.latency;
    }

    /**
     * Creates a new cookie for this connection.
     *
     * @param pClientInformation The client information.
     * @deprecated Use {@link #createCookie(ClientInformation,
     *             net.neoforged.neoforge.network.connection.ConnectionType)} instead,
     *             keeping the connection type information available.
     * @return The cookie.
     */
    @Deprecated
    protected CommonListenerCookie createCookie(ClientInformation pClientInformation) {
        return new CommonListenerCookie(this.playerProfile(), this.latency, pClientInformation);
    }

    /**
     * Creates a new cookie for this connection.
     *
     * @param p_301973_ The client information.
     * @param connectionType Whether the connection is modded.
     * @return The cookie.
     */
    protected CommonListenerCookie createCookie(ClientInformation p_301973_, net.neoforged.neoforge.network.connection.ConnectionType connectionType) {
        return new CommonListenerCookie(this.playerProfile(), this.latency, p_301973_, connectionType);
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public net.minecraft.util.thread.ReentrantBlockableEventLoop<?> getMainThreadEventLoop() {
        return server;
    }
}
