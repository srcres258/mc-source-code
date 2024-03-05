package net.minecraft.network;

import com.google.common.base.Suppliers;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.util.Mth;
import net.minecraft.util.SampleLogger;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Connection extends SimpleChannelInboundHandler<Packet<?>> {
    private static final float AVERAGE_PACKETS_SMOOTHING = 0.75F;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Marker ROOT_MARKER = MarkerFactory.getMarker("NETWORK");
    public static final Marker PACKET_MARKER = Util.make(MarkerFactory.getMarker("NETWORK_PACKETS"), p_202569_ -> p_202569_.add(ROOT_MARKER));
    public static final Marker PACKET_RECEIVED_MARKER = Util.make(MarkerFactory.getMarker("PACKET_RECEIVED"), p_202562_ -> p_202562_.add(PACKET_MARKER));
    public static final Marker PACKET_SENT_MARKER = Util.make(MarkerFactory.getMarker("PACKET_SENT"), p_202557_ -> p_202557_.add(PACKET_MARKER));
    public static final AttributeKey<ConnectionProtocol.CodecData<?>> ATTRIBUTE_SERVERBOUND_PROTOCOL = AttributeKey.valueOf("serverbound_protocol");
    public static final AttributeKey<ConnectionProtocol.CodecData<?>> ATTRIBUTE_CLIENTBOUND_PROTOCOL = AttributeKey.valueOf("clientbound_protocol");
    public static final Supplier<NioEventLoopGroup> NETWORK_WORKER_GROUP = Suppliers.memoize(
        () -> new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Client IO #%d").setDaemon(true).build())
    );
    public static final Supplier<EpollEventLoopGroup> NETWORK_EPOLL_WORKER_GROUP = Suppliers.memoize(
        () -> new EpollEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build())
    );
    public static final Supplier<DefaultEventLoopGroup> LOCAL_WORKER_GROUP = Suppliers.memoize(
        () -> new DefaultEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Local Client IO #%d").setDaemon(true).build())
    );
    private final PacketFlow receiving;
    private final Queue<Consumer<Connection>> pendingActions = Queues.newConcurrentLinkedQueue();
    /**
     * The active channel
     */
    private Channel channel;
    /**
     * The address of the remote party
     */
    private SocketAddress address;
    @Nullable
    private volatile PacketListener disconnectListener;
    /**
     * The PacketListener instance responsible for processing received packets
     */
    @Nullable
    private volatile PacketListener packetListener;
    /**
     * A Component indicating why the network has shutdown.
     */
    @Nullable
    private Component disconnectedReason;
    private boolean encrypted;
    private boolean disconnectionHandled;
    private int receivedPackets;
    private int sentPackets;
    private float averageReceivedPackets;
    private float averageSentPackets;
    private int tickCount;
    private boolean handlingFault;
    @Nullable
    private volatile Component delayedDisconnect;
    @Nullable
    BandwidthDebugMonitor bandwidthDebugMonitor;

    public Connection(PacketFlow pReceiving) {
        this.receiving = pReceiving;
    }

    @Override
    public void channelActive(ChannelHandlerContext pContext) throws Exception {
        super.channelActive(pContext);
        this.channel = pContext.channel();
        this.address = this.channel.remoteAddress();
        if (this.delayedDisconnect != null) {
            this.disconnect(this.delayedDisconnect);
        }
        net.neoforged.neoforge.network.connection.ConnectionUtils.setConnection(pContext, this);
    }

    public static void setInitialProtocolAttributes(Channel pChannel) {
        pChannel.attr(ATTRIBUTE_SERVERBOUND_PROTOCOL).set(ConnectionProtocol.HANDSHAKING.codec(PacketFlow.SERVERBOUND));
        pChannel.attr(ATTRIBUTE_CLIENTBOUND_PROTOCOL).set(ConnectionProtocol.HANDSHAKING.codec(PacketFlow.CLIENTBOUND));
    }

    @Override
    public void channelInactive(ChannelHandlerContext pContext) {
        this.disconnect(Component.translatable("disconnect.endOfStream"));
        net.neoforged.neoforge.network.connection.ConnectionUtils.removeConnection(pContext);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext pContext, Throwable pException) {
        if (pException instanceof SkipPacketException) {
            LOGGER.debug("Skipping packet due to errors", pException.getCause());
        } else {
            boolean flag = !this.handlingFault;
            this.handlingFault = true;
            if (this.channel.isOpen()) {
                if (pException instanceof TimeoutException) {
                    LOGGER.debug("Timeout", pException);
                    this.disconnect(Component.translatable("disconnect.timeout"));
                } else {
                    Component component = Component.translatable("disconnect.genericReason", "Internal Exception: " + pException);
                    LOGGER.error("Exception caught in connection", pException); // Neo: Always log critical network exceptions
                    if (flag) {
                        LOGGER.debug("Failed to sent packet", pException);
                        if (this.getSending() == PacketFlow.CLIENTBOUND) {
                            ConnectionProtocol connectionprotocol = this.channel.attr(ATTRIBUTE_CLIENTBOUND_PROTOCOL).get().protocol();
                            Packet<?> packet = (Packet<?>)(connectionprotocol == ConnectionProtocol.LOGIN
                                ? new ClientboundLoginDisconnectPacket(component)
                                : new ClientboundDisconnectPacket(component));
                            this.send(packet, PacketSendListener.thenRun(() -> this.disconnect(component)));
                        } else {
                            this.disconnect(component);
                        }

                        this.setReadOnly();
                    } else {
                        LOGGER.debug("Double fault", pException);
                        this.disconnect(component);
                    }
                }
            }
        }
    }

    protected void channelRead0(ChannelHandlerContext pContext, Packet<?> pPacket) {
        if (this.channel.isOpen()) {
            PacketListener packetlistener = this.packetListener;
            if (packetlistener == null) {
                throw new IllegalStateException("Received a packet before the packet listener was initialized");
            } else {
                if (packetlistener.shouldHandleMessage(pPacket)) {
                    try {
                        genericsFtw(pPacket, packetlistener);
                    } catch (RunningOnDifferentThreadException runningondifferentthreadexception) {
                    } catch (RejectedExecutionException rejectedexecutionexception) {
                        this.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
                    } catch (ClassCastException classcastexception) {
                        LOGGER.error("Received {} that couldn't be processed", pPacket.getClass(), classcastexception);
                        this.disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
                    }

                    ++this.receivedPackets;
                }
            }
        }
    }

    private static <T extends PacketListener> void genericsFtw(Packet<T> pPacket, PacketListener pListener) {
        pPacket.handle((T)pListener);
    }

    public void suspendInboundAfterProtocolChange() {
        this.channel.config().setAutoRead(false);
    }

    public void resumeInboundAfterProtocolChange() {
        this.channel.config().setAutoRead(true);
    }

    /**
     * Sets the {@link net.minecraft.network.PacketListener} for this {@code Connection}, no checks are made if this listener is suitable for the particular connection state (protocol)
     */
    public void setListener(PacketListener pHandler) {
        Validate.notNull(pHandler, "packetListener");
        PacketFlow packetflow = pHandler.flow();
        if (packetflow != this.receiving) {
            throw new IllegalStateException("Trying to set listener for wrong side: connection is " + this.receiving + ", but listener is " + packetflow);
        } else {
            ConnectionProtocol connectionprotocol = pHandler.protocol();
            ConnectionProtocol connectionprotocol1 = this.channel.attr(getProtocolKey(packetflow)).get().protocol();
            if (connectionprotocol1 != connectionprotocol) {
                throw new IllegalStateException(
                    "Trying to set listener for protocol "
                        + connectionprotocol.id()
                        + ", but current "
                        + packetflow
                        + " protocol is "
                        + connectionprotocol1.id()
                );
            } else {
                this.packetListener = pHandler;
                this.disconnectListener = null;
            }
        }
    }

    public void setListenerForServerboundHandshake(PacketListener pPacketListener) {
        if (this.packetListener != null) {
            throw new IllegalStateException("Listener already set");
        } else if (this.receiving == PacketFlow.SERVERBOUND
            && pPacketListener.flow() == PacketFlow.SERVERBOUND
            && pPacketListener.protocol() == ConnectionProtocol.HANDSHAKING) {
            this.packetListener = pPacketListener;
        } else {
            throw new IllegalStateException("Invalid initial listener");
        }
    }

    public void initiateServerboundStatusConnection(String pHostName, int pPort, ClientStatusPacketListener pDisconnectListener) {
        this.initiateServerboundConnection(pHostName, pPort, pDisconnectListener, ClientIntent.STATUS);
    }

    public void initiateServerboundPlayConnection(String pHostName, int pPort, ClientLoginPacketListener pDisconnectListener) {
        this.initiateServerboundConnection(pHostName, pPort, pDisconnectListener, ClientIntent.LOGIN);
    }

    private void initiateServerboundConnection(String pHostName, int pPort, PacketListener pDisconnectListener, ClientIntent pIntention) {
        this.disconnectListener = pDisconnectListener;
        this.runOnceConnected(
            p_293714_ -> {
                p_293714_.setClientboundProtocolAfterHandshake(pIntention);
                this.setListener(pDisconnectListener);
                p_293714_.sendPacket(
                    new ClientIntentionPacket(SharedConstants.getCurrentVersion().getProtocolVersion(), pHostName, pPort, pIntention), null, true
                );
            }
        );
    }

    public void setClientboundProtocolAfterHandshake(ClientIntent pIntention) {
        this.channel.attr(ATTRIBUTE_CLIENTBOUND_PROTOCOL).set(pIntention.protocol().codec(PacketFlow.CLIENTBOUND));
    }

    public void send(Packet<?> pPacket) {
        this.send(pPacket, null);
    }

    public void send(Packet<?> pPacket, @Nullable PacketSendListener pSendListener) {
        this.send(pPacket, pSendListener, true);
    }

    public void send(Packet<?> pPacket, @Nullable PacketSendListener pListener, boolean pFlush) {
        if (this.isConnected()) {
            this.flushQueue();
            this.sendPacket(pPacket, pListener, pFlush);
        } else {
            this.pendingActions.add(p_293706_ -> p_293706_.sendPacket(pPacket, pListener, pFlush));
        }
    }

    public void runOnceConnected(Consumer<Connection> pAction) {
        if (this.isConnected()) {
            this.flushQueue();
            pAction.accept(this);
        } else {
            this.pendingActions.add(pAction);
        }
    }

    private void sendPacket(Packet<?> pPacket, @Nullable PacketSendListener pSendListener, boolean pFlush) {
        ++this.sentPackets;
        if (this.channel.eventLoop().inEventLoop()) {
            this.doSendPacket(pPacket, pSendListener, pFlush);
        } else {
            this.channel.eventLoop().execute(() -> this.doSendPacket(pPacket, pSendListener, pFlush));
        }
    }

    private void doSendPacket(Packet<?> pPacket, @Nullable PacketSendListener pSendListener, boolean pFlush) {
        ChannelFuture channelfuture = pFlush ? this.channel.writeAndFlush(pPacket) : this.channel.write(pPacket);
        if (pSendListener != null) {
            channelfuture.addListener(p_243167_ -> {
                if (p_243167_.isSuccess()) {
                    pSendListener.onSuccess();
                } else {
                    Packet<?> packet = pSendListener.onFailure();
                    if (packet != null) {
                        ChannelFuture channelfuture1 = this.channel.writeAndFlush(packet);
                        channelfuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    }
                }
            });
        }

        channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public void flushChannel() {
        if (this.isConnected()) {
            this.flush();
        } else {
            this.pendingActions.add(Connection::flush);
        }
    }

    private void flush() {
        if (this.channel.eventLoop().inEventLoop()) {
            this.channel.flush();
        } else {
            this.channel.eventLoop().execute(() -> this.channel.flush());
        }
    }

    private static AttributeKey<ConnectionProtocol.CodecData<?>> getProtocolKey(PacketFlow pPacketFlow) {
        return switch(pPacketFlow) {
            case CLIENTBOUND -> ATTRIBUTE_CLIENTBOUND_PROTOCOL;
            case SERVERBOUND -> ATTRIBUTE_SERVERBOUND_PROTOCOL;
        };
    }

    /**
     * Will iterate through the outboundPacketQueue and dispatch all Packets
     */
    private void flushQueue() {
        if (this.channel != null && this.channel.isOpen()) {
            synchronized(this.pendingActions) {
                Consumer<Connection> consumer;
                while((consumer = this.pendingActions.poll()) != null) {
                    consumer.accept(this);
                }
            }
        }
    }

    /**
     * Checks timeouts and processes all packets received
     */
    public void tick() {
        this.flushQueue();
        PacketListener packetlistener = this.packetListener;
        if (packetlistener instanceof TickablePacketListener tickablepacketlistener) {
            tickablepacketlistener.tick();
        }

        if (!this.isConnected() && !this.disconnectionHandled) {
            this.handleDisconnection();
        }

        if (this.channel != null) {
            this.channel.flush();
        }

        if (this.tickCount++ % 20 == 0) {
            this.tickSecond();
        }

        if (this.bandwidthDebugMonitor != null) {
            this.bandwidthDebugMonitor.tick();
        }
    }

    protected void tickSecond() {
        this.averageSentPackets = Mth.lerp(0.75F, (float)this.sentPackets, this.averageSentPackets);
        this.averageReceivedPackets = Mth.lerp(0.75F, (float)this.receivedPackets, this.averageReceivedPackets);
        this.sentPackets = 0;
        this.receivedPackets = 0;
    }

    /**
     * Returns the socket address of the remote side. Server-only.
     */
    public SocketAddress getRemoteAddress() {
        return this.address;
    }

    public String getLoggableAddress(boolean pLogIps) {
        if (this.address == null) {
            return "local";
        } else {
            return pLogIps ? net.neoforged.neoforge.network.DualStackUtils.getAddressString(this.address) : "IP hidden";
        }
    }

    /**
     * Closes the channel with a given reason. The reason is stored for later and will be used for informational purposes (info log on server,
     * disconnection screen on the client). This method is also called on the client when the server requests disconnection via
     * {@code ClientboundDisconnectPacket}.
     *
     * Closing the channel this way does not send any disconnection packets, it simply terminates the underlying netty channel.
     */
    public void disconnect(Component pMessage) {
        if (this.channel == null) {
            this.delayedDisconnect = pMessage;
        }

        if (this.isConnected()) {
            this.channel.close().awaitUninterruptibly();
            this.disconnectedReason = pMessage;
        }
    }

    /**
     * True if this {@code Connection} uses a memory connection (single player game). False may imply both an active TCP connection or simply no active connection at all
     */
    public boolean isMemoryConnection() {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    /**
     * The receiving packet direction (i.e. SERVERBOUND on the server and CLIENTBOUND on the client).
     */
    public PacketFlow getReceiving() {
        return this.receiving;
    }

    /**
     * The sending packet direction (i.e. SERVERBOUND on the client and CLIENTBOUND on the server)
     */
    public PacketFlow getSending() {
        return this.receiving.getOpposite();
    }

    public static Connection connectToServer(InetSocketAddress pAddress, boolean pUseEpollIfAvailable, @Nullable SampleLogger pBandwithLogger) {
        Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        if (pBandwithLogger != null) {
            connection.setBandwidthLogger(pBandwithLogger);
        }

        ChannelFuture channelfuture = connect(pAddress, pUseEpollIfAvailable, connection);
        channelfuture.syncUninterruptibly();
        return connection;
    }

    public static ChannelFuture connect(InetSocketAddress pAddress, boolean pUseEpollIfAvailable, final Connection pConnection) {
        net.neoforged.neoforge.network.DualStackUtils.checkIPv6(pAddress.getAddress());
        Class<? extends SocketChannel> oclass;
        EventLoopGroup eventloopgroup;
        if (Epoll.isAvailable() && pUseEpollIfAvailable) {
            oclass = EpollSocketChannel.class;
            eventloopgroup = NETWORK_EPOLL_WORKER_GROUP.get();
        } else {
            oclass = NioSocketChannel.class;
            eventloopgroup = NETWORK_WORKER_GROUP.get();
        }

        return new Bootstrap().group(eventloopgroup).handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel p_129552_) {
                Connection.setInitialProtocolAttributes(p_129552_);

                try {
                    p_129552_.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException channelexception) {
                }

                ChannelPipeline channelpipeline = p_129552_.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
                Connection.configureSerialization(channelpipeline, PacketFlow.CLIENTBOUND, pConnection.bandwidthDebugMonitor);
                pConnection.configurePacketHandler(channelpipeline);
            }
        }).channel(oclass).connect(pAddress.getAddress(), pAddress.getPort());
    }

    public static void configureSerialization(ChannelPipeline pPipeline, PacketFlow pFlow, @Nullable BandwidthDebugMonitor pBandwithMonitor) {
        PacketFlow packetflow = pFlow.getOpposite();
        AttributeKey<ConnectionProtocol.CodecData<?>> attributekey = getProtocolKey(pFlow);
        AttributeKey<ConnectionProtocol.CodecData<?>> attributekey1 = getProtocolKey(packetflow);
        pPipeline.addLast("splitter", new Varint21FrameDecoder(pBandwithMonitor))
            .addLast("decoder", new PacketDecoder(attributekey))
            .addLast("prepender", new Varint21LengthFieldPrepender())
            .addLast("encoder", new PacketEncoder(attributekey1))
            .addLast("unbundler", new PacketBundleUnpacker(attributekey1))
            .addLast("bundler", new PacketBundlePacker(attributekey));
    }

    public void configurePacketHandler(ChannelPipeline pPipeline) {
        pPipeline.addLast(new FlowControlHandler()).addLast("packet_handler", this);
    }

    private static void configureInMemoryPacketValidation(ChannelPipeline pPipeline, PacketFlow pFlow) {
        PacketFlow packetflow = pFlow.getOpposite();
        AttributeKey<ConnectionProtocol.CodecData<?>> attributekey = getProtocolKey(pFlow);
        AttributeKey<ConnectionProtocol.CodecData<?>> attributekey1 = getProtocolKey(packetflow);
        pPipeline.addLast("validator", new PacketFlowValidator(attributekey, attributekey1));
    }

    public static void configureInMemoryPipeline(ChannelPipeline pPipeline, PacketFlow pFlow) {
        configureInMemoryPacketValidation(pPipeline, pFlow);
    }

    /**
     * Prepares a clientside Connection for a local in-memory connection ("single player").
     * Establishes a connection to the socket supplied and configures the channel pipeline (only the packet handler is necessary,
     * since this is for an in-memory connection). Returns the newly created instance.
     */
    public static Connection connectToLocalServer(SocketAddress pAddress) {
        final Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        new Bootstrap().group(LOCAL_WORKER_GROUP.get()).handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel p_129557_) {
                Connection.setInitialProtocolAttributes(p_129557_);
                ChannelPipeline channelpipeline = p_129557_.pipeline();
                Connection.configureInMemoryPipeline(channelpipeline, PacketFlow.CLIENTBOUND);
                connection.configurePacketHandler(channelpipeline);
            }
        }).channel(LocalChannel.class).connect(pAddress).syncUninterruptibly();
        return connection;
    }

    /**
     * Enables encryption for this connection using the given decrypting and encrypting ciphers.
     * This adds new handlers to this connection's pipeline which handle the decrypting and encrypting.
     * This happens as part of the normal network handshake.
     *
     * @see net.minecraft.network.protocol.login.ClientboundHelloPacket
     * @see net.minecraft.network.protocol.login.ServerboundKeyPacket
     */
    public void setEncryptionKey(Cipher pDecryptingCipher, Cipher pEncryptingCipher) {
        this.encrypted = true;
        this.channel.pipeline().addBefore("splitter", "decrypt", new CipherDecoder(pDecryptingCipher));
        this.channel.pipeline().addBefore("prepender", "encrypt", new CipherEncoder(pEncryptingCipher));
    }

    public boolean isEncrypted() {
        return this.encrypted;
    }

    /**
     * Returns {@code true} if this {@code Connection} has an active channel, {@code false} otherwise.
     */
    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen();
    }

    /**
     * Returns {@code true} while this connection is still connecting, i.e. {@link #channelActive} has not fired yet.
     */
    public boolean isConnecting() {
        return this.channel == null;
    }

    /**
     * Gets the current handler for processing packets
     */
    @Nullable
    public PacketListener getPacketListener() {
        return this.packetListener;
    }

    /**
     * If this channel is closed, returns the exit message, null otherwise.
     */
    @Nullable
    public Component getDisconnectedReason() {
        return this.disconnectedReason;
    }

    /**
     * Switches the channel to manual reading modus
     */
    public void setReadOnly() {
        if (this.channel != null) {
            this.channel.config().setAutoRead(false);
        }
    }

    /**
     * Enables or disables compression for this connection. If {@code threshold} is >= 0 then a {@link CompressionDecoder} and {@link CompressionEncoder}
     * are installed in the pipeline or updated if they already exist. If {@code threshold} is < 0 then any such codec are removed.
     *
     * Compression is enabled as part of the connection handshake when the server sends {@link net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket}.
     */
    public void setupCompression(int pThreshold, boolean pValidateDecompressed) {
        if (pThreshold >= 0) {
            if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder) {
                ((CompressionDecoder)this.channel.pipeline().get("decompress")).setThreshold(pThreshold, pValidateDecompressed);
            } else {
                this.channel.pipeline().addBefore("decoder", "decompress", new CompressionDecoder(pThreshold, pValidateDecompressed));
            }

            if (this.channel.pipeline().get("compress") instanceof CompressionEncoder) {
                ((CompressionEncoder)this.channel.pipeline().get("compress")).setThreshold(pThreshold);
            } else {
                this.channel.pipeline().addBefore("encoder", "compress", new CompressionEncoder(pThreshold));
            }
        } else {
            if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder) {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") instanceof CompressionEncoder) {
                this.channel.pipeline().remove("compress");
            }
        }
    }

    /**
     * Checks if the channel is no longer active and if so, processes the disconnection
     * by notifying the current packet listener, which will handle things like removing the player from the world (serverside) or
     * showing the disconnection screen (clientside).
     */
    public void handleDisconnection() {
        if (this.channel != null && !this.channel.isOpen()) {
            if (this.disconnectionHandled) {
                LOGGER.warn("handleDisconnection() called twice");
            } else {
                this.disconnectionHandled = true;
                PacketListener packetlistener = this.getPacketListener();
                PacketListener packetlistener1 = packetlistener != null ? packetlistener : this.disconnectListener;
                if (packetlistener1 != null) {
                    Component component = Objects.requireNonNullElseGet(
                        this.getDisconnectedReason(), () -> Component.translatable("multiplayer.disconnect.generic")
                    );
                    packetlistener1.onDisconnect(component);
                }
            }
        }
    }

    public float getAverageReceivedPackets() {
        return this.averageReceivedPackets;
    }

    public float getAverageSentPackets() {
        return this.averageSentPackets;
    }

    public Channel channel() {
        return this.channel;
    }

    public PacketFlow getDirection() {
        return this.receiving;
    }

    public void setBandwidthLogger(SampleLogger pBandwithLogger) {
        this.bandwidthDebugMonitor = new BandwidthDebugMonitor(pBandwithLogger);
    }
}
