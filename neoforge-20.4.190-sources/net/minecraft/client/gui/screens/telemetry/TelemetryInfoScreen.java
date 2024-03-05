package net.minecraft.client.gui.screens.telemetry;

import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TelemetryInfoScreen extends Screen {
    private static final int PADDING = 8;
    private static final Component TITLE = Component.translatable("telemetry_info.screen.title");
    private static final Component DESCRIPTION = Component.translatable("telemetry_info.screen.description").withStyle(ChatFormatting.GRAY);
    private static final Component BUTTON_PRIVACY_STATEMENT = Component.translatable("telemetry_info.button.privacy_statement");
    private static final Component BUTTON_GIVE_FEEDBACK = Component.translatable("telemetry_info.button.give_feedback");
    private static final Component BUTTON_SHOW_DATA = Component.translatable("telemetry_info.button.show_data");
    private static final Component CHECKBOX_OPT_IN = Component.translatable("telemetry_info.opt_in.description");
    private final Screen lastScreen;
    private final Options options;
    @Nullable
    private TelemetryEventWidget telemetryEventWidget;
    private double savedScroll;

    public TelemetryInfoScreen(Screen pLastScreen, Options pOptions) {
        super(TITLE);
        this.lastScreen = pLastScreen;
        this.options = pOptions;
    }

    @Override
    public Component getNarrationMessage() {
        return CommonComponents.joinForNarration(super.getNarrationMessage(), DESCRIPTION);
    }

    @Override
    protected void init() {
        FrameLayout framelayout = new FrameLayout();
        framelayout.defaultChildLayoutSetting().padding(8);
        framelayout.setMinHeight(this.height);
        LinearLayout linearlayout = framelayout.addChild(LinearLayout.vertical(), framelayout.newChildLayoutSettings().align(0.5F, 0.0F));
        linearlayout.defaultCellSetting().alignHorizontallyCenter().paddingBottom(8);
        linearlayout.addChild(new StringWidget(this.getTitle(), this.font));
        linearlayout.addChild(new MultiLineTextWidget(DESCRIPTION, this.font).setMaxWidth(this.width - 16).setCentered(true));
        GridLayout gridlayout = this.twoButtonContainer(
            Button.builder(BUTTON_PRIVACY_STATEMENT, this::openPrivacyStatementLink).build(),
            Button.builder(BUTTON_GIVE_FEEDBACK, this::openFeedbackLink).build()
        );
        linearlayout.addChild(gridlayout);
        Layout layout = this.createLowerSection();
        framelayout.arrangeElements();
        layout.arrangeElements();
        int i = gridlayout.getY() + gridlayout.getHeight();
        int j = layout.getHeight();
        int k = this.height - i - j - 16;
        this.telemetryEventWidget = new TelemetryEventWidget(0, 0, this.width - 40, k, this.minecraft.font);
        this.telemetryEventWidget.setScrollAmount(this.savedScroll);
        this.telemetryEventWidget.setOnScrolledListener(p_262168_ -> this.savedScroll = p_262168_);
        this.setInitialFocus(this.telemetryEventWidget);
        linearlayout.addChild(this.telemetryEventWidget);
        linearlayout.addChild(layout);
        framelayout.arrangeElements();
        FrameLayout.alignInRectangle(framelayout, 0, 0, this.width, this.height, 0.5F, 0.0F);
        framelayout.visitWidgets(this::addRenderableWidget);
    }

    private Layout createLowerSection() {
        LinearLayout linearlayout = LinearLayout.vertical();
        linearlayout.defaultCellSetting().alignHorizontallyCenter().paddingBottom(4);
        if (this.minecraft.extraTelemetryAvailable()) {
            linearlayout.addChild(this.createTelemetryCheckbox());
        }

        linearlayout.addChild(
            this.twoButtonContainer(
                Button.builder(BUTTON_SHOW_DATA, this::openDataFolder).build(), Button.builder(CommonComponents.GUI_DONE, this::openLastScreen).build()
            )
        );
        return linearlayout;
    }

    private AbstractWidget createTelemetryCheckbox() {
        OptionInstance<Boolean> optioninstance = this.options.telemetryOptInExtra();
        Checkbox checkbox = Checkbox.builder(CHECKBOX_OPT_IN, this.minecraft.font).selected(optioninstance).onValueChange(this::onOptInChanged).build();
        checkbox.active = this.minecraft.extraTelemetryAvailable();
        return checkbox;
    }

    private void onOptInChanged(AbstractWidget p_309196_, boolean p_309095_) {
        if (this.telemetryEventWidget != null) {
            this.telemetryEventWidget.onOptInChanged(p_309095_);
        }
    }

    private void openLastScreen(Button p_261672_) {
        this.minecraft.setScreen(this.lastScreen);
    }

    private void openPrivacyStatementLink(Button p_295613_) {
        ConfirmLinkScreen.confirmLinkNow(this, "http://go.microsoft.com/fwlink/?LinkId=521839");
    }

    private void openFeedbackLink(Button p_261531_) {
        ConfirmLinkScreen.confirmLinkNow(this, "https://aka.ms/javafeedback?ref=game");
    }

    private void openDataFolder(Button p_261840_) {
        Path path = this.minecraft.getTelemetryManager().getLogDirectory();
        Util.getPlatform().openUri(path.toUri());
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderDirtBackground(pGuiGraphics);
    }

    private GridLayout twoButtonContainer(AbstractWidget pButton1, AbstractWidget pButton2) {
        GridLayout gridlayout = new GridLayout();
        gridlayout.defaultCellSetting().alignHorizontallyCenter().paddingHorizontal(4);
        gridlayout.addChild(pButton1, 0, 0);
        gridlayout.addChild(pButton2, 0, 1);
        return gridlayout;
    }
}
