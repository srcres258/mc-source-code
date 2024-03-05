package net.minecraft.network.protocol.status;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerPingPacketListener;

public class ServerboundPingRequestPacket implements Packet<ServerPingPacketListener> {
    private final long time;

    public ServerboundPingRequestPacket(long pTime) {
        this.time = pTime;
    }

    public ServerboundPingRequestPacket(FriendlyByteBuf pBuffer) {
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
    public void handle(ServerPingPacketListener pHandler) {
        pHandler.handlePingRequest(this);
    }

    public long getTime() {
        return this.time;
    }
}
