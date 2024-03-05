package net.minecraft.network.protocol.common;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;

public record ServerboundCustomPayloadPacket(CustomPacketPayload payload) implements Packet<ServerCommonPacketListener> {
    private static final int MAX_PAYLOAD_SIZE = 32767;
    public static final Map<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>> KNOWN_TYPES = ImmutableMap.<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>>builder(
            
        )
        .put(BrandPayload.ID, BrandPayload::new)
        .build();

    /**
     * Creates a new packet with a custom payload from the network.
     * @param p_296108_ The buffer to read the packet from.
     * @deprecated Use {@link #ServerboundCustomPayloadPacket(FriendlyByteBuf, io.netty.channel.ChannelHandlerContext, net.minecraft.network.ConnectionProtocol)} instead, as this variant can only read vanilla payloads.
     */
    @Deprecated
    public ServerboundCustomPayloadPacket(FriendlyByteBuf p_296108_) {
        this(readPayload(p_296108_.readResourceLocation(), p_296108_));
    }

    /**
     * Creates a new packet with a custom payload from the network.
     *
     * @param p_296108_ The buffer to read the packet from.
     * @param context The context of the channel handler
     * @param protocol The protocol of the connection
     */
    public ServerboundCustomPayloadPacket(FriendlyByteBuf p_296108_, io.netty.channel.ChannelHandlerContext context, net.minecraft.network.ConnectionProtocol protocol) {
        this(readPayload(p_296108_.readResourceLocation(), p_296108_, context, protocol));
    }

    /**
     * Reads the payload from the given buffer.
     *
     * @param p_294367_ The id of the payload
     * @param p_294321_ The buffer to read from
     * @param context The context of the channel handler
     * @param protocol The protocol of the connection
     * @return The payload
     */
    private static CustomPacketPayload readPayload(ResourceLocation p_294367_, FriendlyByteBuf p_294321_, io.netty.channel.ChannelHandlerContext context, net.minecraft.network.ConnectionProtocol protocol) {
        FriendlyByteBuf.Reader<? extends CustomPacketPayload> reader = net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().getReader(p_294367_, context, protocol, KNOWN_TYPES);
        return (CustomPacketPayload)(reader != null ? reader.apply(p_294321_) : readUnknownPayload(p_294367_, p_294321_));
    }

    /**
     * Reads the payload from the given buffer.
     *
     * @param pId     The id of the payload
     * @param pBuffer The buffer to read from
     * @deprecated Use {@link #readPayload(ResourceLocation, FriendlyByteBuf,
     *             io.netty.channel.ChannelHandlerContext,
     *             net.minecraft.network.ConnectionProtocol)} instead, as this variant
     *             can only read vanilla payloads.
     * @return The payload
     */
    @Deprecated
    private static CustomPacketPayload readPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
        FriendlyByteBuf.Reader<? extends CustomPacketPayload> reader = KNOWN_TYPES.get(pId);
        return (CustomPacketPayload)(reader != null ? reader.apply(pBuffer) : readUnknownPayload(pId, pBuffer));
    }

    private static DiscardedPayload readUnknownPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
        int i = pBuffer.readableBytes();
        if (i >= 0 && i <= 32767) {
            pBuffer.skipBytes(i);
            return new DiscardedPayload(pId);
        } else {
            throw new IllegalArgumentException("Payload may not be larger than 32767 bytes");
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeResourceLocation(this.payload.id());
        this.payload.write(pBuffer);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ServerCommonPacketListener pHandler) {
        pHandler.handleCustomPayload(this);
    }
}
