package net.minecraft.network.protocol.common;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.BeeDebugPayload;
import net.minecraft.network.protocol.common.custom.BrainDebugPayload;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.BreezeDebugPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.common.custom.GameEventDebugPayload;
import net.minecraft.network.protocol.common.custom.GameEventListenerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestAddMarkerDebugPayload;
import net.minecraft.network.protocol.common.custom.GameTestClearMarkersDebugPayload;
import net.minecraft.network.protocol.common.custom.GoalDebugPayload;
import net.minecraft.network.protocol.common.custom.HiveDebugPayload;
import net.minecraft.network.protocol.common.custom.NeighborUpdatesDebugPayload;
import net.minecraft.network.protocol.common.custom.PathfindingDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiAddedDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiRemovedDebugPayload;
import net.minecraft.network.protocol.common.custom.PoiTicketCountDebugPayload;
import net.minecraft.network.protocol.common.custom.RaidsDebugPayload;
import net.minecraft.network.protocol.common.custom.StructuresDebugPayload;
import net.minecraft.network.protocol.common.custom.VillageSectionsDebugPayload;
import net.minecraft.network.protocol.common.custom.WorldGenAttemptDebugPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundCustomPayloadPacket(CustomPacketPayload payload) implements Packet<ClientCommonPacketListener> {
    private static final int MAX_PAYLOAD_SIZE = 1048576;
    public static final Map<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>> KNOWN_TYPES = ImmutableMap.<ResourceLocation, FriendlyByteBuf.Reader<? extends CustomPacketPayload>>builder(
            
        )
        .put(BrandPayload.ID, BrandPayload::new)
        .put(BeeDebugPayload.ID, BeeDebugPayload::new)
        .put(BrainDebugPayload.ID, BrainDebugPayload::new)
        .put(BreezeDebugPayload.ID, BreezeDebugPayload::new)
        .put(GameEventDebugPayload.ID, GameEventDebugPayload::new)
        .put(GameEventListenerDebugPayload.ID, GameEventListenerDebugPayload::new)
        .put(GameTestAddMarkerDebugPayload.ID, GameTestAddMarkerDebugPayload::new)
        .put(GameTestClearMarkersDebugPayload.ID, GameTestClearMarkersDebugPayload::new)
        .put(GoalDebugPayload.ID, GoalDebugPayload::new)
        .put(HiveDebugPayload.ID, HiveDebugPayload::new)
        .put(NeighborUpdatesDebugPayload.ID, NeighborUpdatesDebugPayload::new)
        .put(PathfindingDebugPayload.ID, PathfindingDebugPayload::new)
        .put(PoiAddedDebugPayload.ID, PoiAddedDebugPayload::new)
        .put(PoiRemovedDebugPayload.ID, PoiRemovedDebugPayload::new)
        .put(PoiTicketCountDebugPayload.ID, PoiTicketCountDebugPayload::new)
        .put(RaidsDebugPayload.ID, RaidsDebugPayload::new)
        .put(StructuresDebugPayload.ID, StructuresDebugPayload::new)
        .put(VillageSectionsDebugPayload.ID, VillageSectionsDebugPayload::new)
        .put(WorldGenAttemptDebugPayload.ID, WorldGenAttemptDebugPayload::new)
        .build();

    /**
     * @deprecated Use {@link #ClientboundCustomPayloadPacket(FriendlyByteBuf, io.netty.channel.ChannelHandlerContext, net.minecraft.network.ConnectionProtocol)} instead, as this variant can only read vanilla payloads.
     */
    @Deprecated()
    public ClientboundCustomPayloadPacket(FriendlyByteBuf p_295835_) {
        this(readPayload(p_295835_.readResourceLocation(), p_295835_));
    }

    /**
     * Reads a custom payload packet from the given buffer.
     *
     * @param buffer The buffer to read from
     * @param context The context of the channel in which this packet is read
     * @param protocol The protocol of the connection
     */
    public ClientboundCustomPayloadPacket(FriendlyByteBuf buffer, io.netty.channel.ChannelHandlerContext context, net.minecraft.network.ConnectionProtocol protocol) {
        this(readPayload(buffer.readResourceLocation(), buffer, context, protocol));
    }

    /**
     * Reads the payload from the given buffer.
     *
     * @param p_294367_ The id of the payload
     * @param p_294321_ The buffer to read from
     * @param context The context of the channel in which this packet is read
     * @param protocol The protocol of the connection
     * @return The payload
     */
    private static CustomPacketPayload readPayload(ResourceLocation p_294367_, FriendlyByteBuf p_294321_, io.netty.channel.ChannelHandlerContext context, net.minecraft.network.ConnectionProtocol protocol) {
        FriendlyByteBuf.Reader<? extends CustomPacketPayload> reader = net.neoforged.neoforge.network.registration.NetworkRegistry.getInstance().getReader(p_294367_, context, protocol, KNOWN_TYPES);
        return (CustomPacketPayload)(reader != null ? reader.apply(p_294321_) : readUnknownPayload(p_294367_, p_294321_));
    }

    /**
     * @deprecated Use {@link #readPayload(ResourceLocation, FriendlyByteBuf,
     *             io.netty.channel.ChannelHandlerContext,
     *             net.minecraft.network.ConnectionProtocol)} instead, as this variant
     *             can only read vanilla payloads.
     */
    @Deprecated
    private static CustomPacketPayload readPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
        FriendlyByteBuf.Reader<? extends CustomPacketPayload> reader = KNOWN_TYPES.get(pId);
        return (CustomPacketPayload)(reader != null ? reader.apply(pBuffer) : readUnknownPayload(pId, pBuffer));
    }

    private static DiscardedPayload readUnknownPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
        int i = pBuffer.readableBytes();
        if (i >= 0 && i <= 1048576) {
            pBuffer.skipBytes(i);
            return new DiscardedPayload(pId);
        } else {
            throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
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
    public void handle(ClientCommonPacketListener pHandler) {
        pHandler.handleCustomPayload(this);
    }
}
