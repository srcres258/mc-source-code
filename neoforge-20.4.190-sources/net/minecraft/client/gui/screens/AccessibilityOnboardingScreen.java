package net.minecraft.client.gui.screens;

import com.mojang.text2speech.Narrator;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.renderer.PanoramaRenderer;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AccessibilityOnboardingScreen extends Screen {
    private static final Component ONBOARDING_NARRATOR_MESSAGE = Component.translatable("accessibility.onboarding.screen.narrator");
    private static final int PADDING = 4;
    private static final int TITLE_PADDING = 16;
    private final PanoramaRenderer panorama = new PanoramaRenderer(TitleScreen.CUBE_MAP);
    private final LogoRenderer logoRenderer;
    private final Options options;
    private final boolean narratorAvailable;
    private boolean hasNarrated;
    private float timer;
    private final Runnable onClose;
    @Nullable
    private FocusableTextWidget textWidget;

    public AccessibilityOnboardingScreen(Options pOptions, Runnable pOnClose) {
        super(Component.translatable("accessibility.onboarding.screen.title"));
        this.options = pOptions;
        this.onClose = pOnClose;
        this.logoRenderer = new LogoRenderer(true);
        this.narratorAvailable = Minecraft.getInstance().getNarrator().isActive();
    }

    @Override
    public void init() {
        int i = this.initTitleYPos();
        FrameLayout framelayout = new FrameLayout(this.width, this.height - i);
        framelayout.defaultChildLayoutSetting().alignVerticallyTop().padding(4);
        LinearLayout linearlayout = framelayout.addChild(LinearLayout.vertical());
        linearlayout.defaultCellSetting().alignHorizontallyCenter().padding(2);
        this.textWidget = new FocusableTextWidget(this.width - 16, this.title, this.font);
        linearlayout.addChild(this.textWidget, p_293597_ -> p_293597_.paddingBottom(16));
        AbstractWidget abstractwidget = this.options.narrator().createButton(this.options, 0, 0, 150);
        abstractwidget.active = this.narratorAvailable;
        linearlayout.addChild(abstractwidget);
        if (this.narratorAvailable) {
            this.setInitialFocus(abstractwidget);
        }

        linearlayout.addChild(
            CommonButtons.accessibility(150, p_280782_ -> this.closeAndSetScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), false)
        );
        linearlayout.addChild(
            CommonButtons.language(
                150, p_280781_ -> this.closeAndSetScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())), false
            )
        );
        framelayout.addChild(
            Button.builder(CommonComponents.GUI_CONTINUE, p_267841_ -> this.onClose()).build(),
            framelayout.newChildLayoutSettings().alignVerticallyBottom().padding(8)
        );
        framelayout.arrangeElements();
        FrameLayout.alignInRectangle(framelayout, 0, i, this.width, this.height, 0.5F, 0.0F);
        framelayout.visitWidgets(this::addRenderableWidget);
    }

    private int initTitleYPos() {
        return 90;
    }

    @Override
    public void onClose() {
        this.close(this.onClose);
    }

    private void closeAndSetScreen(Screen pScreen) {
        this.close(() -> this.minecraft.setScreen(pScreen));
    }

    private void close(Runnable pOnClose) {
        this.options.onboardAccessibility = false;
        this.options.save();
        Narrator.getNarrator().clear();
        pOnClose.run();
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
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.handleInitialNarrationDelay();
        this.logoRenderer.renderLogo(pGuiGraphics, this.width, 1.0F);
        if (this.textWidget != null) {
            this.textWidget.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.panorama.render(0.0F, 1.0F);
        pGuiGraphics.fill(0, 0, this.width, this.height, -1877995504);
    }

    private void handleInitialNarrationDelay() {
        if (!this.hasNarrated && this.narratorAvailable) {
            if (this.timer < 40.0F) {
                ++this.timer;
            } else if (this.minecraft.isWindowActive()) {
                Narrator.getNarrator().say(ONBOARDING_NARRATOR_MESSAGE.getString(), true);
                this.hasNarrated = true;
            }
        }
    }
}
