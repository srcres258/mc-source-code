package net.minecraft.network.protocol.login;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundLoginAcknowledgedPacket() implements Packet<ServerLoginPacketListener> {
    public ServerboundLoginAcknowledgedPacket(FriendlyByteBuf p_295418_) {
        this();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ServerLoginPacketListener pHandler) {
        pHandler.handleLoginAcknowledgement(this);
    }

    @Override
    public ConnectionProtocol nextProtocol() {
        return ConnectionProtocol.CONFIGURATION;
    }
}
