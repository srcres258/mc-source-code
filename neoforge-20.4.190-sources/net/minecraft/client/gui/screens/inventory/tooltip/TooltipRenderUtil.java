package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TooltipRenderUtil {
    public static final int MOUSE_OFFSET = 12;
    private static final int PADDING = 3;
    public static final int PADDING_LEFT = 3;
    public static final int PADDING_RIGHT = 3;
    public static final int PADDING_TOP = 3;
    public static final int PADDING_BOTTOM = 3;
    private static final int BACKGROUND_COLOR = -267386864;
    private static final int BORDER_COLOR_TOP = 1347420415;
    private static final int BORDER_COLOR_BOTTOM = 1344798847;

    public static void renderTooltipBackground(GuiGraphics pGuiGraphics, int pX, int pY, int pWidth, int pHeight, int pZ) {
        renderTooltipBackground(pGuiGraphics, pX, pY, pWidth, pHeight, pZ, BACKGROUND_COLOR, BACKGROUND_COLOR, BORDER_COLOR_TOP, BORDER_COLOR_BOTTOM);
    }

    // Forge: Allow specifying colors for the inner border gradient and a gradient instead of a single color for the background and outer border
    public static void renderTooltipBackground(GuiGraphics p_282666_, int p_281901_, int p_281846_, int p_281559_, int p_283336_, int p_283422_, int backgroundTop, int backgroundBottom, int borderTop, int borderBottom)
    {
        int i = p_281901_ - 3;
        int j = p_281846_ - 3;
        int k = p_281559_ + 3 + 3;
        int l = p_283336_ + 3 + 3;
        renderHorizontalLine(p_282666_, i, j - 1, k, p_283422_, backgroundTop);
        renderHorizontalLine(p_282666_, i, j + l, k, p_283422_, backgroundBottom);
        renderRectangle(p_282666_, i, j, k, l, p_283422_, backgroundTop, backgroundBottom);
        renderVerticalLineGradient(p_282666_, i - 1, j, l, p_283422_, backgroundTop, backgroundBottom);
        renderVerticalLineGradient(p_282666_, i + k, j, l, p_283422_, backgroundTop, backgroundBottom);
        renderFrameGradient(p_282666_, i, j + 1, k, l, p_283422_, borderTop, borderBottom);
    }

    private static void renderFrameGradient(
        GuiGraphics pGuiGraphics, int pX, int pY, int pWidth, int pHeight, int pZ, int pTopColor, int pBottomColor
    ) {
        renderVerticalLineGradient(pGuiGraphics, pX, pY, pHeight - 2, pZ, pTopColor, pBottomColor);
        renderVerticalLineGradient(pGuiGraphics, pX + pWidth - 1, pY, pHeight - 2, pZ, pTopColor, pBottomColor);
        renderHorizontalLine(pGuiGraphics, pX, pY - 1, pWidth, pZ, pTopColor);
        renderHorizontalLine(pGuiGraphics, pX, pY - 1 + pHeight - 1, pWidth, pZ, pBottomColor);
    }

    private static void renderVerticalLine(GuiGraphics pGuiGraphics, int pX, int pY, int pLength, int pZ, int pColor) {
        pGuiGraphics.fill(pX, pY, pX + 1, pY + pLength, pZ, pColor);
    }

    private static void renderVerticalLineGradient(
        GuiGraphics pGuiGraphics, int pX, int pY, int pLength, int pZ, int pTopColor, int pBottomColor
    ) {
        pGuiGraphics.fillGradient(pX, pY, pX + 1, pY + pLength, pZ, pTopColor, pBottomColor);
    }

    private static void renderHorizontalLine(GuiGraphics pGuiGraphics, int pX, int pY, int pLength, int pZ, int pColor) {
        pGuiGraphics.fill(pX, pY, pX + pLength, pY + 1, pZ, pColor);
    }

    /**
    * @deprecated Forge: Use gradient overload instead
    */
    @Deprecated
    private static void renderRectangle(GuiGraphics pGuiGraphics, int pX, int pY, int pWidth, int pHeight, int pZ, int pColor) {
        renderRectangle(pGuiGraphics, pX, pY, pWidth, pHeight, pZ, pColor, pColor);
    }

    // Forge: Allow specifying a gradient instead of a single color for the background
    private static void renderRectangle(GuiGraphics p_281392_, int p_282294_, int p_283353_, int p_282640_, int p_281964_, int p_283211_, int p_282349_, int colorTo) {
        p_281392_.fillGradient(p_282294_, p_283353_, p_282294_ + p_282640_, p_283353_ + p_281964_, p_283211_, p_282349_, colorTo);
    }
}
