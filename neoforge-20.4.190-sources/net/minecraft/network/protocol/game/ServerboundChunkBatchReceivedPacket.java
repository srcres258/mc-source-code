package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundChunkBatchReceivedPacket(float desiredChunksPerTick) implements Packet<ServerGamePacketListener> {
    public ServerboundChunkBatchReceivedPacket(FriendlyByteBuf p_294171_) {
        this(p_294171_.readFloat());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeFloat(this.desiredChunksPerTick);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleChunkBatchReceived(this);
    }
}
