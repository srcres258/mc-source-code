package net.minecraft.client.gui.screens.inventory;

import net.minecraft.client.gui.screens.recipebook.SmeltingRecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.FurnaceMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FurnaceScreen extends AbstractFurnaceScreen<FurnaceMenu> {
    private static final ResourceLocation LIT_PROGRESS_SPRITE = new ResourceLocation("container/furnace/lit_progress");
    private static final ResourceLocation BURN_PROGRESS_SPRITE = new ResourceLocation("container/furnace/burn_progress");
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/furnace.png");

    public FurnaceScreen(FurnaceMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, new SmeltingRecipeBookComponent(), pPlayerInventory, pTitle, TEXTURE, LIT_PROGRESS_SPRITE, BURN_PROGRESS_SPRITE);
    }
}
