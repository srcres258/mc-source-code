package net.minecraft.network.protocol.configuration;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundFinishConfigurationPacket() implements Packet<ClientConfigurationPacketListener> {
    public ClientboundFinishConfigurationPacket(FriendlyByteBuf p_296427_) {
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
    public void handle(ClientConfigurationPacketListener pHandler) {
        pHandler.handleConfigurationFinished(this);
    }

    @Override
    public ConnectionProtocol nextProtocol() {
        return ConnectionProtocol.PLAY;
    }
}
