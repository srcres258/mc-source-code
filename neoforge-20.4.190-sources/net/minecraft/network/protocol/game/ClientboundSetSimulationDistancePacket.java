package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundSetSimulationDistancePacket(int simulationDistance) implements Packet<ClientGamePacketListener> {
    public ClientboundSetSimulationDistancePacket(FriendlyByteBuf p_195800_) {
        this(p_195800_.readVarInt());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.simulationDistance);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetSimulationDistance(this);
    }
}
