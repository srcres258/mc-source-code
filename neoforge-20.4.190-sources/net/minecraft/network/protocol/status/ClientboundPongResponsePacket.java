package net.minecraft.network.protocol.status;

import net.minecraft.network.ClientPongPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundPongResponsePacket implements Packet<ClientPongPacketListener> {
    private final long time;

    public ClientboundPongResponsePacket(long pTime) {
        this.time = pTime;
    }

    public ClientboundPongResponsePacket(FriendlyByteBuf pBuffer) {
        this.time = pBuffer.readLong();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeLong(this.time);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientPongPacketListener pHandler) {
        pHandler.handlePongResponse(this);
    }

    public long getTime() {
        return this.time;
    }
}
