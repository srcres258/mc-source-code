package net.minecraft.server.network;

import com.google.common.primitives.Ints;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.CrashReportCategory;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.TickablePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ServerLoginPacketListener;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public class ServerLoginPacketListenerImpl implements ServerLoginPacketListener, TickablePacketListener {
    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_TICKS_BEFORE_LOGIN = 600;
    private static final Component DISCONNECT_UNEXPECTED_QUERY = Component.translatable("multiplayer.disconnect.unexpected_query_response");
    private final byte[] challenge;
    final MinecraftServer server;
    final Connection connection;
    private volatile ServerLoginPacketListenerImpl.State state = ServerLoginPacketListenerImpl.State.HELLO;
    /**
     * How long has player been trying to login into the server.
     */
    private int tick;
    @Nullable
    String requestedUsername;
    @Nullable
    public GameProfile authenticatedProfile;
    private final String serverId = "";

    public ServerLoginPacketListenerImpl(MinecraftServer pServer, Connection pConnection) {
        this.server = pServer;
        this.connection = pConnection;
        this.challenge = Ints.toByteArray(RandomSource.create().nextInt());
    }

    @Override
    public void tick() {
        if (this.state == ServerLoginPacketListenerImpl.State.VERIFYING) {
            this.verifyLoginAndFinishConnectionSetup(Objects.requireNonNull(this.authenticatedProfile));
        }

        if (this.state == ServerLoginPacketListenerImpl.State.WAITING_FOR_DUPE_DISCONNECT
            && !this.isPlayerAlreadyInWorld(Objects.requireNonNull(this.authenticatedProfile))) {
            this.finishLoginAndWaitForClient(this.authenticatedProfile);
        }

        if (this.tick++ == 600) {
            this.disconnect(Component.translatable("multiplayer.disconnect.slow_login"));
        }
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    public void disconnect(Component pReason) {
        try {
            LOGGER.info("Disconnecting {}: {}", this.getUserName(), pReason.getString());
            this.connection.send(new ClientboundLoginDisconnectPacket(pReason));
            this.connection.disconnect(pReason);
        } catch (Exception exception) {
            LOGGER.error("Error whilst disconnecting player", (Throwable)exception);
        }
    }

    private boolean isPlayerAlreadyInWorld(GameProfile pProfile) {
        return this.server.getPlayerList().getPlayer(pProfile.getId()) != null;
    }

    /**
     * Invoked when disconnecting, the parameter is a ChatComponent describing the reason for termination
     */
    @Override
    public void onDisconnect(Component pReason) {
        LOGGER.info("{} lost connection: {}", this.getUserName(), pReason.getString());
    }

    public String getUserName() {
        String s = this.connection.getLoggableAddress(this.server.logIPs());
        return this.requestedUsername != null ? this.requestedUsername + " (" + s + ")" : s;
    }

    @Override
    public void handleHello(ServerboundHelloPacket pPacket) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet");
        Validate.validState(Player.isValidUsername(pPacket.name()), "Invalid characters in username");
        this.requestedUsername = pPacket.name();
        GameProfile gameprofile = this.server.getSingleplayerProfile();
        if (gameprofile != null && this.requestedUsername.equalsIgnoreCase(gameprofile.getName())) {
            this.startClientVerification(gameprofile);
        } else {
            if (this.server.usesAuthentication() && !this.connection.isMemoryConnection()) {
                this.state = ServerLoginPacketListenerImpl.State.KEY;
                this.connection.send(new ClientboundHelloPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.challenge));
            } else {
                this.startClientVerification(UUIDUtil.createOfflineProfile(this.requestedUsername));
            }
        }
    }

    void startClientVerification(GameProfile pAuthenticatedProfile) {
        this.authenticatedProfile = pAuthenticatedProfile;
        this.state = ServerLoginPacketListenerImpl.State.VERIFYING;
    }

    private void verifyLoginAndFinishConnectionSetup(GameProfile pProfile) {
        PlayerList playerlist = this.server.getPlayerList();
        Component component = playerlist.canPlayerLogin(this.connection.getRemoteAddress(), pProfile);
        if (component != null) {
            this.disconnect(component);
        } else {
            if (this.server.getCompressionThreshold() >= 0 && !this.connection.isMemoryConnection()) {
                this.connection
                    .send(
                        new ClientboundLoginCompressionPacket(this.server.getCompressionThreshold()),
                        PacketSendListener.thenRun(() -> this.connection.setupCompression(this.server.getCompressionThreshold(), true))
                    );
            }

            boolean flag = playerlist.disconnectAllPlayersWithProfile(pProfile);
            if (flag) {
                this.state = ServerLoginPacketListenerImpl.State.WAITING_FOR_DUPE_DISCONNECT;
            } else {
                this.finishLoginAndWaitForClient(pProfile);
            }
        }
    }

    private void finishLoginAndWaitForClient(GameProfile pProfile) {
        this.state = ServerLoginPacketListenerImpl.State.PROTOCOL_SWITCHING;
        this.connection.send(new ClientboundGameProfilePacket(pProfile));
    }

    @Override
    public void handleKey(ServerboundKeyPacket pPacket) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.KEY, "Unexpected key packet");

        final String s;
        try {
            PrivateKey privatekey = this.server.getKeyPair().getPrivate();
            if (!pPacket.isChallengeValid(this.challenge, privatekey)) {
                throw new IllegalStateException("Protocol error");
            }

            SecretKey secretkey = pPacket.getSecretKey(privatekey);
            Cipher cipher = Crypt.getCipher(2, secretkey);
            Cipher cipher1 = Crypt.getCipher(1, secretkey);
            s = new BigInteger(Crypt.digestData("", this.server.getKeyPair().getPublic(), secretkey)).toString(16);
            this.state = ServerLoginPacketListenerImpl.State.AUTHENTICATING;
            this.connection.setEncryptionKey(cipher, cipher1);
        } catch (CryptException cryptexception) {
            throw new IllegalStateException("Protocol error", cryptexception);
        }

        Thread thread = new Thread("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet()) {
            @Override
            public void run() {
                String s1 = Objects.requireNonNull(ServerLoginPacketListenerImpl.this.requestedUsername, "Player name not initialized");

                try {
                    ProfileResult profileresult = ServerLoginPacketListenerImpl.this.server.getSessionService().hasJoinedServer(s1, s, this.getAddress());
                    if (profileresult != null) {
                        GameProfile gameprofile = profileresult.profile();
                        ServerLoginPacketListenerImpl.LOGGER.info("UUID of player {} is {}", gameprofile.getName(), gameprofile.getId());
                        ServerLoginPacketListenerImpl.this.startClientVerification(gameprofile);
                    } else if (ServerLoginPacketListenerImpl.this.server.isSingleplayer()) {
                        ServerLoginPacketListenerImpl.LOGGER.warn("Failed to verify username but will let them in anyway!");
                        ServerLoginPacketListenerImpl.this.startClientVerification(UUIDUtil.createOfflineProfile(s1));
                    } else {
                        ServerLoginPacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                        ServerLoginPacketListenerImpl.LOGGER.error("Username '{}' tried to join with an invalid session", s1);
                    }
                } catch (AuthenticationUnavailableException authenticationunavailableexception) {
                    if (ServerLoginPacketListenerImpl.this.server.isSingleplayer()) {
                        ServerLoginPacketListenerImpl.LOGGER.warn("Authentication servers are down but will let them in anyway!");
                        ServerLoginPacketListenerImpl.this.startClientVerification(UUIDUtil.createOfflineProfile(s1));
                    } else {
                        ServerLoginPacketListenerImpl.this.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
                        ServerLoginPacketListenerImpl.LOGGER.error("Couldn't verify username because servers are unavailable");
                    }
                }
            }

            @Nullable
            private InetAddress getAddress() {
                SocketAddress socketaddress = ServerLoginPacketListenerImpl.this.connection.getRemoteAddress();
                return ServerLoginPacketListenerImpl.this.server.getPreventProxyConnections() && socketaddress instanceof InetSocketAddress
                    ? ((InetSocketAddress)socketaddress).getAddress()
                    : null;
            }
        };
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    @Override
    public void handleCustomQueryPacket(ServerboundCustomQueryAnswerPacket pPacket) {
        this.disconnect(DISCONNECT_UNEXPECTED_QUERY);
    }

    @Override
    public void handleLoginAcknowledgement(ServerboundLoginAcknowledgedPacket pPacket) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.PROTOCOL_SWITCHING, "Unexpected login acknowledgement packet");
        CommonListenerCookie commonlistenercookie = CommonListenerCookie.createInitial(Objects.requireNonNull(this.authenticatedProfile));
        ServerConfigurationPacketListenerImpl serverconfigurationpacketlistenerimpl = new ServerConfigurationPacketListenerImpl(
            this.server, this.connection, commonlistenercookie
        );
        this.connection.setListener(serverconfigurationpacketlistenerimpl);
        serverconfigurationpacketlistenerimpl.startConfiguration();
        this.state = ServerLoginPacketListenerImpl.State.ACCEPTED;
    }

    @Override
    public void fillListenerSpecificCrashDetails(CrashReportCategory pCrashReportCategory) {
        pCrashReportCategory.setDetail("Login phase", () -> this.state.toString());
    }

    static enum State {
        HELLO,
        KEY,
        AUTHENTICATING,
        NEGOTIATING,
        VERIFYING,
        WAITING_FOR_DUPE_DISCONNECT,
        PROTOCOL_SWITCHING,
        ACCEPTED;
    }
}
