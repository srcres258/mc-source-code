package net.minecraft.network.protocol.login;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientboundCustomQueryPacket(int transactionId, CustomQueryPayload payload) implements Packet<ClientLoginPacketListener> {
    private static final int MAX_PAYLOAD_SIZE = 1048576;

    public ClientboundCustomQueryPacket(FriendlyByteBuf p_179810_) {
        this(p_179810_.readVarInt(), readPayload(p_179810_.readResourceLocation(), p_179810_));
    }

    private static CustomQueryPayload readPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
        return readUnknownPayload(pId, pBuffer);
    }

    private static DiscardedQueryPayload readUnknownPayload(ResourceLocation pId, FriendlyByteBuf pBuffer) {
        int i = pBuffer.readableBytes();
        if (i >= 0 && i <= 1048576) {
            pBuffer.skipBytes(i);
            return new DiscardedQueryPayload(pId);
        } else {
            throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.transactionId);
        pBuffer.writeResourceLocation(this.payload.id());
        this.payload.write(pBuffer);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientLoginPacketListener pHandler) {
        pHandler.handleCustomQuery(this);
    }
}
