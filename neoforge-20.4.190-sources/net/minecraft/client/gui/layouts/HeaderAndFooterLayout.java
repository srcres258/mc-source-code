package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HeaderAndFooterLayout implements Layout {
    public static final int DEFAULT_HEADER_AND_FOOTER_HEIGHT = 36;
    private static final int CONTENT_MARGIN_TOP = 30;
    private final FrameLayout headerFrame = new FrameLayout();
    private final FrameLayout footerFrame = new FrameLayout();
    private final FrameLayout contentsFrame = new FrameLayout();
    private final Screen screen;
    private int headerHeight;
    private int footerHeight;

    public HeaderAndFooterLayout(Screen pScreen) {
        this(pScreen, 36);
    }

    public HeaderAndFooterLayout(Screen pScreen, int pHeight) {
        this(pScreen, pHeight, pHeight);
    }

    public HeaderAndFooterLayout(Screen pScreen, int pHeaderHeight, int pFooterHeight) {
        this.screen = pScreen;
        this.headerHeight = pHeaderHeight;
        this.footerHeight = pFooterHeight;
        this.headerFrame.defaultChildLayoutSetting().align(0.5F, 0.5F);
        this.footerFrame.defaultChildLayoutSetting().align(0.5F, 0.5F);
    }

    @Override
    public void setX(int pX) {
    }

    @Override
    public void setY(int pY) {
    }

    @Override
    public int getX() {
        return 0;
    }

    @Override
    public int getY() {
        return 0;
    }

    @Override
    public int getWidth() {
        return this.screen.width;
    }

    @Override
    public int getHeight() {
        return this.screen.height;
    }

    public int getFooterHeight() {
        return this.footerHeight;
    }

    public void setFooterHeight(int pFooterHeight) {
        this.footerHeight = pFooterHeight;
    }

    public void setHeaderHeight(int pHeaderHeight) {
        this.headerHeight = pHeaderHeight;
    }

    public int getHeaderHeight() {
        return this.headerHeight;
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> pVisitor) {
        this.headerFrame.visitChildren(pVisitor);
        this.contentsFrame.visitChildren(pVisitor);
        this.footerFrame.visitChildren(pVisitor);
    }

    @Override
    public void arrangeElements() {
        int i = this.getHeaderHeight();
        int j = this.getFooterHeight();
        this.headerFrame.setMinWidth(this.screen.width);
        this.headerFrame.setMinHeight(i);
        this.headerFrame.setPosition(0, 0);
        this.headerFrame.arrangeElements();
        this.footerFrame.setMinWidth(this.screen.width);
        this.footerFrame.setMinHeight(j);
        this.footerFrame.arrangeElements();
        this.footerFrame.setY(this.screen.height - j);
        this.contentsFrame.setMinWidth(this.screen.width);
        this.contentsFrame.arrangeElements();
        int k = i + 30;
        int l = this.screen.height - j - this.contentsFrame.getHeight();
        this.contentsFrame.setPosition(0, Math.min(k, l));
    }

    public <T extends LayoutElement> T addToHeader(T pChild) {
        return this.headerFrame.addChild(pChild);
    }

    public <T extends LayoutElement> T addToHeader(T pChild, Consumer<LayoutSettings> pLayoutSettingsFactory) {
        return this.headerFrame.addChild(pChild, pLayoutSettingsFactory);
    }

    public <T extends LayoutElement> T addToFooter(T pChild) {
        return this.footerFrame.addChild(pChild);
    }

    public <T extends LayoutElement> T addToFooter(T pChild, Consumer<LayoutSettings> pLayoutSettingsFactory) {
        return this.footerFrame.addChild(pChild, pLayoutSettingsFactory);
    }

    public <T extends LayoutElement> T addToContents(T pChild) {
        return this.contentsFrame.addChild(pChild);
    }

    public <T extends LayoutElement> T addToContents(T pChild, Consumer<LayoutSettings> pLayoutSettingFactory) {
        return this.contentsFrame.addChild(pChild, pLayoutSettingFactory);
    }
}
