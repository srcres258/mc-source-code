package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CreativeInventoryListener implements ContainerListener {
    private final Minecraft minecraft;

    public CreativeInventoryListener(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    /**
     * Sends the contents of an inventory slot to the client-side Container. This doesn't have to match the actual contents of that slot.
     */
    @Override
    public void slotChanged(AbstractContainerMenu pContainerToSend, int pSlotInd, ItemStack pStack) {
        this.minecraft.gameMode.handleCreativeModeItemAdd(pStack, pSlotInd);
    }

    @Override
    public void dataChanged(AbstractContainerMenu pContainerMenu, int pDataSlotIndex, int pValue) {
    }
}
