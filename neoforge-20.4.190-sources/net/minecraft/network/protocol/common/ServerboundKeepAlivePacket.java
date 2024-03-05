package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundKeepAlivePacket implements Packet<ServerCommonPacketListener> {
    private final long id;

    public ServerboundKeepAlivePacket(long pId) {
        this.id = pId;
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ServerCommonPacketListener pHandler) {
        pHandler.handleKeepAlive(this);
    }

    public ServerboundKeepAlivePacket(FriendlyByteBuf pBuffer) {
        this.id = pBuffer.readLong();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeLong(this.id);
    }

    public long getId() {
        return this.id;
    }
}
