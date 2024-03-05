package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FocusableTextWidget extends MultiLineTextWidget {
    private static final int BACKGROUND_COLOR = 1426063360;
    private static final int PADDING = 4;
    private final boolean alwaysShowBorder;

    public FocusableTextWidget(int pMaxWidth, Component pMessage, Font pFont) {
        this(pMaxWidth, pMessage, pFont, true);
    }

    public FocusableTextWidget(int pMaxWidth, Component pMessage, Font pFont, boolean pAlwaysShowBorder) {
        super(pMessage, pFont);
        this.setMaxWidth(pMaxWidth);
        this.setCentered(true);
        this.active = true;
        this.alwaysShowBorder = pAlwaysShowBorder;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        pNarrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.isFocused() || this.alwaysShowBorder) {
            int i = this.getX() - 4;
            int j = this.getY() - 4;
            int k = this.getWidth() + 8;
            int l = this.getHeight() + 8;
            int i1 = this.alwaysShowBorder ? (this.isFocused() ? -1 : -6250336) : -1;
            pGuiGraphics.fill(i + 1, j, i + k, j + l, 1426063360);
            pGuiGraphics.renderOutline(i, j, k, l, i1);
        }

        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public void playDownSound(SoundManager pHandler) {
    }
}
