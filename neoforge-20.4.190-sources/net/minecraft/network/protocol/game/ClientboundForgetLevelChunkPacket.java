package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.ChunkPos;

public record ClientboundForgetLevelChunkPacket(ChunkPos pos) implements Packet<ClientGamePacketListener> {
    public ClientboundForgetLevelChunkPacket(FriendlyByteBuf p_178858_) {
        this(p_178858_.readChunkPos());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeChunkPos(this.pos);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleForgetLevelChunk(this);
    }
}
