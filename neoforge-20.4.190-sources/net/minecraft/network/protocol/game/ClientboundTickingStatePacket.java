package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.TickRateManager;

public record ClientboundTickingStatePacket(float tickRate, boolean isFrozen) implements Packet<ClientGamePacketListener> {
    public ClientboundTickingStatePacket(FriendlyByteBuf p_309182_) {
        this(p_309182_.readFloat(), p_309182_.readBoolean());
    }

    public static ClientboundTickingStatePacket from(TickRateManager pTickRateManager) {
        return new ClientboundTickingStatePacket(pTickRateManager.tickrate(), pTickRateManager.isFrozen());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeFloat(this.tickRate);
        pBuffer.writeBoolean(this.isFrozen);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleTickingState(this);
    }
}
