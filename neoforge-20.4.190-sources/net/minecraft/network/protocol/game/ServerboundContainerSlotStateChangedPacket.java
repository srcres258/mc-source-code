package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundContainerSlotStateChangedPacket(int slotId, int containerId, boolean newState) implements Packet<ServerGamePacketListener> {
    public ServerboundContainerSlotStateChangedPacket(FriendlyByteBuf p_307271_) {
        this(p_307271_.readVarInt(), p_307271_.readVarInt(), p_307271_.readBoolean());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.slotId);
        pBuffer.writeVarInt(this.containerId);
        pBuffer.writeBoolean(this.newState);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleContainerSlotStateChanged(this);
    }
}
