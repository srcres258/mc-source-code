package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.item.ItemStack;

public class ServerboundSetCreativeModeSlotPacket implements Packet<ServerGamePacketListener> {
    private final int slotNum;
    private final ItemStack itemStack;

    public ServerboundSetCreativeModeSlotPacket(int pSlotNum, ItemStack pItemStack) {
        this.slotNum = pSlotNum;
        this.itemStack = pItemStack.copy();
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleSetCreativeModeSlot(this);
    }

    public ServerboundSetCreativeModeSlotPacket(FriendlyByteBuf pBuffer) {
        this.slotNum = pBuffer.readShort();
        this.itemStack = pBuffer.readItem();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeShort(this.slotNum);
        pBuffer.writeItem(this.itemStack);
    }

    public int getSlotNum() {
        return this.slotNum;
    }

    public ItemStack getItem() {
        return this.itemStack;
    }
}
