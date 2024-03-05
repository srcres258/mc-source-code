package net.minecraft.client.gui.screens.reporting;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.Optionull;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.report.Report;
import net.minecraft.client.multiplayer.chat.report.ReportReason;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.client.multiplayer.chat.report.SkinReport;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SkinReportScreen extends AbstractReportScreen<SkinReport.Builder> {
    private static final int BUTTON_WIDTH = 120;
    private static final int SKIN_WIDTH = 85;
    private static final int FORM_WIDTH = 178;
    private static final Component TITLE = Component.translatable("gui.abuseReport.skin.title");
    private final LinearLayout layout = LinearLayout.vertical().spacing(8);
    private MultiLineEditBox commentBox;
    private Button sendButton;
    private Button selectReasonButton;

    private SkinReportScreen(Screen pLastScreen, ReportingContext pReportingContext, SkinReport.Builder pReportBuilder) {
        super(TITLE, pLastScreen, pReportingContext, pReportBuilder);
    }

    public SkinReportScreen(Screen pLastScreen, ReportingContext pReportingContext, UUID pReportId, Supplier<PlayerSkin> pSkinGetter) {
        this(pLastScreen, pReportingContext, new SkinReport.Builder(pReportId, pSkinGetter, pReportingContext.sender().reportLimits()));
    }

    public SkinReportScreen(Screen pLastScreen, ReportingContext pReportingContext, SkinReport pReport) {
        this(pLastScreen, pReportingContext, new SkinReport.Builder(pReport, pReportingContext.sender().reportLimits()));
    }

    @Override
    protected void init() {
        this.layout.defaultCellSetting().alignHorizontallyCenter();
        this.layout.addChild(new StringWidget(this.title, this.font));
        LinearLayout linearlayout = this.layout.addChild(LinearLayout.horizontal().spacing(8));
        linearlayout.defaultCellSetting().alignVerticallyMiddle();
        linearlayout.addChild(new PlayerSkinWidget(85, 120, this.minecraft.getEntityModels(), this.reportBuilder.report().getSkinGetter()));
        LinearLayout linearlayout1 = linearlayout.addChild(LinearLayout.vertical().spacing(8));
        this.selectReasonButton = Button.builder(
                SELECT_REASON, p_299945_ -> this.minecraft.setScreen(new ReportReasonSelectionScreen(this, this.reportBuilder.reason(), p_299969_ -> {
                        this.reportBuilder.setReason(p_299969_);
                        this.onReportChanged();
                    }))
            )
            .width(178)
            .build();
        linearlayout1.addChild(CommonLayouts.labeledElement(this.font, this.selectReasonButton, OBSERVED_WHAT_LABEL));
        this.commentBox = this.createCommentBox(178, 9 * 8, p_299919_ -> {
            this.reportBuilder.setComments(p_299919_);
            this.onReportChanged();
        });
        linearlayout1.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, MORE_COMMENTS_LABEL, p_300017_ -> p_300017_.paddingBottom(12)));
        LinearLayout linearlayout2 = this.layout.addChild(LinearLayout.horizontal().spacing(8));
        linearlayout2.addChild(Button.builder(CommonComponents.GUI_BACK, p_313444_ -> this.onClose()).width(120).build());
        this.sendButton = linearlayout2.addChild(Button.builder(SEND_REPORT, p_313443_ -> this.sendReport()).width(120).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
        this.onReportChanged();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    private void onReportChanged() {
        ReportReason reportreason = this.reportBuilder.reason();
        if (reportreason != null) {
            this.selectReasonButton.setMessage(reportreason.title());
        } else {
            this.selectReasonButton.setMessage(SELECT_REASON);
        }

        Report.CannotBuildReason report$cannotbuildreason = this.reportBuilder.checkBuildable();
        this.sendButton.active = report$cannotbuildreason == null;
        this.sendButton.setTooltip(Optionull.map(report$cannotbuildreason, Report.CannotBuildReason::tooltip));
    }

    /**
     * Called when a mouse button is released within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     *
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     * @param pButton the button that was released.
     */
    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        return super.mouseReleased(pMouseX, pMouseY, pButton) ? true : this.commentBox.mouseReleased(pMouseX, pMouseY, pButton);
    }
}
