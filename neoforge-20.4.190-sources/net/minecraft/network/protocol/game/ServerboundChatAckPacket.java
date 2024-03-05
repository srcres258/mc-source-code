package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundChatAckPacket(int offset) implements Packet<ServerGamePacketListener> {
    public ServerboundChatAckPacket(FriendlyByteBuf p_242339_) {
        this(p_242339_.readVarInt());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.offset);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleChatAck(this);
    }
}
