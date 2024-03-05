package net.minecraft.client.gui.components.toasts;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TutorialToast implements Toast {
    private static final ResourceLocation BACKGROUND_SPRITE = new ResourceLocation("toast/tutorial");
    public static final int PROGRESS_BAR_WIDTH = 154;
    public static final int PROGRESS_BAR_HEIGHT = 1;
    public static final int PROGRESS_BAR_X = 3;
    public static final int PROGRESS_BAR_Y = 28;
    private final TutorialToast.Icons icon;
    private final Component title;
    @Nullable
    private final Component message;
    private Toast.Visibility visibility = Toast.Visibility.SHOW;
    private long lastProgressTime;
    private float lastProgress;
    private float progress;
    private final boolean progressable;

    public TutorialToast(TutorialToast.Icons pIcon, Component pTitle, @Nullable Component pMessage, boolean pProgressable) {
        this.icon = pIcon;
        this.title = pTitle;
        this.message = pMessage;
        this.progressable = pProgressable;
    }

    @Override
    public Toast.Visibility render(GuiGraphics pGuiGraphics, ToastComponent pToastComponent, long pTimeSinceLastVisible) {
        pGuiGraphics.blitSprite(BACKGROUND_SPRITE, 0, 0, this.width(), this.height());
        this.icon.render(pGuiGraphics, 6, 6);
        if (this.message == null) {
            pGuiGraphics.drawString(pToastComponent.getMinecraft().font, this.title, 30, 12, -11534256, false);
        } else {
            pGuiGraphics.drawString(pToastComponent.getMinecraft().font, this.title, 30, 7, -11534256, false);
            pGuiGraphics.drawString(pToastComponent.getMinecraft().font, this.message, 30, 18, -16777216, false);
        }

        if (this.progressable) {
            pGuiGraphics.fill(3, 28, 157, 29, -1);
            float f = Mth.clampedLerp(this.lastProgress, this.progress, (float)(pTimeSinceLastVisible - this.lastProgressTime) / 100.0F);
            int i;
            if (this.progress >= this.lastProgress) {
                i = -16755456;
            } else {
                i = -11206656;
            }

            pGuiGraphics.fill(3, 28, (int)(3.0F + 154.0F * f), 29, i);
            this.lastProgress = f;
            this.lastProgressTime = pTimeSinceLastVisible;
        }

        return this.visibility;
    }

    public void hide() {
        this.visibility = Toast.Visibility.HIDE;
    }

    public void updateProgress(float pProgress) {
        this.progress = pProgress;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Icons {
        MOVEMENT_KEYS(new ResourceLocation("toast/movement_keys")),
        MOUSE(new ResourceLocation("toast/mouse")),
        TREE(new ResourceLocation("toast/tree")),
        RECIPE_BOOK(new ResourceLocation("toast/recipe_book")),
        WOODEN_PLANKS(new ResourceLocation("toast/wooden_planks")),
        SOCIAL_INTERACTIONS(new ResourceLocation("toast/social_interactions")),
        RIGHT_CLICK(new ResourceLocation("toast/right_click"));

        private final ResourceLocation sprite;

        private Icons(ResourceLocation pSprite) {
            this.sprite = pSprite;
        }

        public void render(GuiGraphics pGuiGraphics, int pX, int pY) {
            RenderSystem.enableBlend();
            pGuiGraphics.blitSprite(this.sprite, pX, pY, 20, 20);
        }
    }
}
