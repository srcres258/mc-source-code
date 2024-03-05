package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.recipebook.PlaceRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OverlayRecipeComponent implements Renderable, GuiEventListener {
    private static final ResourceLocation OVERLAY_RECIPE_SPRITE = new ResourceLocation("recipe_book/overlay_recipe");
    static final ResourceLocation FURNACE_OVERLAY_HIGHLIGHTED_SPRITE = new ResourceLocation("recipe_book/furnace_overlay_highlighted");
    static final ResourceLocation FURNACE_OVERLAY_SPRITE = new ResourceLocation("recipe_book/furnace_overlay");
    static final ResourceLocation CRAFTING_OVERLAY_HIGHLIGHTED_SPRITE = new ResourceLocation("recipe_book/crafting_overlay_highlighted");
    static final ResourceLocation CRAFTING_OVERLAY_SPRITE = new ResourceLocation("recipe_book/crafting_overlay");
    static final ResourceLocation FURNACE_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE = new ResourceLocation("recipe_book/furnace_overlay_disabled_highlighted");
    static final ResourceLocation FURNACE_OVERLAY_DISABLED_SPRITE = new ResourceLocation("recipe_book/furnace_overlay_disabled");
    static final ResourceLocation CRAFTING_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE = new ResourceLocation("recipe_book/crafting_overlay_disabled_highlighted");
    static final ResourceLocation CRAFTING_OVERLAY_DISABLED_SPRITE = new ResourceLocation("recipe_book/crafting_overlay_disabled");
    private static final int MAX_ROW = 4;
    private static final int MAX_ROW_LARGE = 5;
    private static final float ITEM_RENDER_SCALE = 0.375F;
    public static final int BUTTON_SIZE = 25;
    private final List<OverlayRecipeComponent.OverlayRecipeButton> recipeButtons = Lists.newArrayList();
    private boolean isVisible;
    private int x;
    private int y;
    private Minecraft minecraft;
    private RecipeCollection collection;
    @Nullable
    private RecipeHolder<?> lastRecipeClicked;
    float time;
    boolean isFurnaceMenu;

    public void init(Minecraft pMinecraft, RecipeCollection pCollection, int pX, int pY, int p_100199_, int p_100200_, float p_100201_) {
        this.minecraft = pMinecraft;
        this.collection = pCollection;
        if (pMinecraft.player.containerMenu instanceof AbstractFurnaceMenu) {
            this.isFurnaceMenu = true;
        }

        boolean flag = pMinecraft.player.getRecipeBook().isFiltering((RecipeBookMenu<?>)pMinecraft.player.containerMenu);
        List<RecipeHolder<?>> list = pCollection.getDisplayRecipes(true);
        List<RecipeHolder<?>> list1 = flag ? Collections.emptyList() : pCollection.getDisplayRecipes(false);
        int i = list.size();
        int j = i + list1.size();
        int k = j <= 16 ? 4 : 5;
        int l = (int)Math.ceil((double)((float)j / (float)k));
        this.x = pX;
        this.y = pY;
        float f = (float)(this.x + Math.min(j, k) * 25);
        float f1 = (float)(p_100199_ + 50);
        if (f > f1) {
            this.x = (int)((float)this.x - p_100201_ * (float)((int)((f - f1) / p_100201_)));
        }

        float f2 = (float)(this.y + l * 25);
        float f3 = (float)(p_100200_ + 50);
        if (f2 > f3) {
            this.y = (int)((float)this.y - p_100201_ * (float)Mth.ceil((f2 - f3) / p_100201_));
        }

        float f4 = (float)this.y;
        float f5 = (float)(p_100200_ - 100);
        if (f4 < f5) {
            this.y = (int)((float)this.y - p_100201_ * (float)Mth.ceil((f4 - f5) / p_100201_));
        }

        this.isVisible = true;
        this.recipeButtons.clear();

        for(int i1 = 0; i1 < j; ++i1) {
            boolean flag1 = i1 < i;
            RecipeHolder<?> recipeholder = flag1 ? list.get(i1) : list1.get(i1 - i);
            int j1 = this.x + 4 + 25 * (i1 % k);
            int k1 = this.y + 5 + 25 * (i1 / k);
            if (this.isFurnaceMenu) {
                this.recipeButtons.add(new OverlayRecipeComponent.OverlaySmeltingRecipeButton(j1, k1, recipeholder, flag1));
            } else {
                this.recipeButtons.add(new OverlayRecipeComponent.OverlayRecipeButton(j1, k1, recipeholder, flag1));
            }
        }

        this.lastRecipeClicked = null;
    }

    public RecipeCollection getRecipeCollection() {
        return this.collection;
    }

    @Nullable
    public RecipeHolder<?> getLastRecipeClicked() {
        return this.lastRecipeClicked;
    }

    /**
     * Called when a mouse button is clicked within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     *
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     * @param pButton the button that was clicked.
     */
    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton != 0) {
            return false;
        } else {
            for(OverlayRecipeComponent.OverlayRecipeButton overlayrecipecomponent$overlayrecipebutton : this.recipeButtons) {
                if (overlayrecipecomponent$overlayrecipebutton.mouseClicked(pMouseX, pMouseY, pButton)) {
                    this.lastRecipeClicked = overlayrecipecomponent$overlayrecipebutton.recipe;
                    return true;
                }
            }

            return false;
        }
    }

    /**
     * Checks if the given mouse coordinates are over the GUI element.
     * <p>
     * @return {@code true} if the mouse is over the GUI element, {@code false} otherwise.
     *
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     */
    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return false;
    }

    /**
     * Renders the graphical user interface (GUI) element.
     *
     * @param pGuiGraphics the GuiGraphics object used for rendering.
     * @param pMouseX      the x-coordinate of the mouse cursor.
     * @param pMouseY      the y-coordinate of the mouse cursor.
     * @param pPartialTick the partial tick time.
     */
    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.isVisible) {
            this.time += pPartialTick;
            RenderSystem.enableBlend();
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate(0.0F, 0.0F, 1000.0F);
            int i = this.recipeButtons.size() <= 16 ? 4 : 5;
            int j = Math.min(this.recipeButtons.size(), i);
            int k = Mth.ceil((float)this.recipeButtons.size() / (float)i);
            int l = 4;
            pGuiGraphics.blitSprite(OVERLAY_RECIPE_SPRITE, this.x, this.y, j * 25 + 8, k * 25 + 8);
            RenderSystem.disableBlend();

            for(OverlayRecipeComponent.OverlayRecipeButton overlayrecipecomponent$overlayrecipebutton : this.recipeButtons) {
                overlayrecipecomponent$overlayrecipebutton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            }

            pGuiGraphics.pose().popPose();
        }
    }

    public void setVisible(boolean pIsVisible) {
        this.isVisible = pIsVisible;
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    /**
     * Sets the focus state of the GUI element.
     *
     * @param pFocused {@code true} to apply focus, {@code false} to remove focus
     */
    @Override
    public void setFocused(boolean pFocused) {
    }

    /**
     * {@return {@code true} if the GUI element is focused, {@code false} otherwise}
     */
    @Override
    public boolean isFocused() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    class OverlayRecipeButton extends AbstractWidget implements PlaceRecipe<Ingredient> {
        final RecipeHolder<?> recipe;
        private final boolean isCraftable;
        protected final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> ingredientPos = Lists.newArrayList();

        public OverlayRecipeButton(int pX, int pY, RecipeHolder<?> pRecipe, boolean pIsCraftable) {
            super(pX, pY, 200, 20, CommonComponents.EMPTY);
            this.width = 24;
            this.height = 24;
            this.recipe = pRecipe;
            this.isCraftable = pIsCraftable;
            this.calculateIngredientsPositions(pRecipe);
        }

        protected void calculateIngredientsPositions(RecipeHolder<?> pRecipe) {
            this.placeRecipe(3, 3, -1, pRecipe, pRecipe.value().getIngredients().iterator(), 0);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
            this.defaultButtonNarrationText(pNarrationElementOutput);
        }

        @Override
        public void addItemToSlot(Iterator<Ingredient> pIngredients, int pSlot, int pMaxAmount, int pY, int pX) {
            ItemStack[] aitemstack = pIngredients.next().getItems();
            if (aitemstack.length != 0) {
                this.ingredientPos.add(new OverlayRecipeComponent.OverlayRecipeButton.Pos(3 + pX * 7, 3 + pY * 7, aitemstack));
            }
        }

        @Override
        public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            ResourceLocation resourcelocation;
            if (this.isCraftable) {
                if (OverlayRecipeComponent.this.isFurnaceMenu) {
                    resourcelocation = this.isHoveredOrFocused()
                        ? OverlayRecipeComponent.FURNACE_OVERLAY_HIGHLIGHTED_SPRITE
                        : OverlayRecipeComponent.FURNACE_OVERLAY_SPRITE;
                } else {
                    resourcelocation = this.isHoveredOrFocused()
                        ? OverlayRecipeComponent.CRAFTING_OVERLAY_HIGHLIGHTED_SPRITE
                        : OverlayRecipeComponent.CRAFTING_OVERLAY_SPRITE;
                }
            } else if (OverlayRecipeComponent.this.isFurnaceMenu) {
                resourcelocation = this.isHoveredOrFocused()
                    ? OverlayRecipeComponent.FURNACE_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE
                    : OverlayRecipeComponent.FURNACE_OVERLAY_DISABLED_SPRITE;
            } else {
                resourcelocation = this.isHoveredOrFocused()
                    ? OverlayRecipeComponent.CRAFTING_OVERLAY_DISABLED_HIGHLIGHTED_SPRITE
                    : OverlayRecipeComponent.CRAFTING_OVERLAY_DISABLED_SPRITE;
            }

            pGuiGraphics.blitSprite(resourcelocation, this.getX(), this.getY(), this.width, this.height);
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate((double)(this.getX() + 2), (double)(this.getY() + 2), 150.0);

            for(OverlayRecipeComponent.OverlayRecipeButton.Pos overlayrecipecomponent$overlayrecipebutton$pos : this.ingredientPos) {
                pGuiGraphics.pose().pushPose();
                pGuiGraphics.pose()
                    .translate((double)overlayrecipecomponent$overlayrecipebutton$pos.x, (double)overlayrecipecomponent$overlayrecipebutton$pos.y, 0.0);
                pGuiGraphics.pose().scale(0.375F, 0.375F, 1.0F);
                pGuiGraphics.pose().translate(-8.0, -8.0, 0.0);
                if (overlayrecipecomponent$overlayrecipebutton$pos.ingredients.length > 0) {
                    pGuiGraphics.renderItem(
                        overlayrecipecomponent$overlayrecipebutton$pos.ingredients[Mth.floor(OverlayRecipeComponent.this.time / 30.0F)
                            % overlayrecipecomponent$overlayrecipebutton$pos.ingredients.length],
                        0,
                        0
                    );
                }

                pGuiGraphics.pose().popPose();
            }

            pGuiGraphics.pose().popPose();
        }

        @OnlyIn(Dist.CLIENT)
        protected class Pos {
            public final ItemStack[] ingredients;
            public final int x;
            public final int y;

            public Pos(int pX, int pY, ItemStack[] pIngredients) {
                this.x = pX;
                this.y = pY;
                this.ingredients = pIngredients;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class OverlaySmeltingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
        public OverlaySmeltingRecipeButton(int p_100262_, int p_100263_, RecipeHolder<?> p_301104_, boolean p_100265_) {
            super(p_100262_, p_100263_, p_301104_, p_100265_);
        }

        @Override
        protected void calculateIngredientsPositions(RecipeHolder<?> p_301196_) {
            Ingredient ingredient = p_301196_.value().getIngredients().get(0);
            ItemStack[] aitemstack = ingredient.getItems();
            this.ingredientPos.add(new OverlayRecipeComponent.OverlayRecipeButton.Pos(10, 10, aitemstack));
        }
    }
}
