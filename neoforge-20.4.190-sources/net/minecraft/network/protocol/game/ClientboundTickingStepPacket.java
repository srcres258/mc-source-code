package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.TickRateManager;

public record ClientboundTickingStepPacket(int tickSteps) implements Packet<ClientGamePacketListener> {
    public ClientboundTickingStepPacket(FriendlyByteBuf p_309129_) {
        this(p_309129_.readVarInt());
    }

    public static ClientboundTickingStepPacket from(TickRateManager pTickRateManager) {
        return new ClientboundTickingStepPacket(pTickRateManager.frozenTicksToRun());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.tickSteps);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleTickingStep(this);
    }
}
