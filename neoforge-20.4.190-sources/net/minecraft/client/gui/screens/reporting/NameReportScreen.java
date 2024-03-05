package net.minecraft.client.gui.screens.reporting;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.report.NameReport;
import net.minecraft.client.multiplayer.chat.report.Report;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NameReportScreen extends AbstractReportScreen<NameReport.Builder> {
    private static final int BUTTON_WIDTH = 120;
    private static final Component TITLE = Component.translatable("gui.abuseReport.name.title");
    private final LinearLayout layout = LinearLayout.vertical().spacing(8);
    private MultiLineEditBox commentBox;
    private Button sendButton;

    private NameReportScreen(Screen pLastScreen, ReportingContext pReportingContext, NameReport.Builder pReportBuilder) {
        super(TITLE, pLastScreen, pReportingContext, pReportBuilder);
    }

    public NameReportScreen(Screen pLastScreen, ReportingContext pReportingContext, UUID pReportedProfileId, String pReportedName) {
        this(pLastScreen, pReportingContext, new NameReport.Builder(pReportedProfileId, pReportedName, pReportingContext.sender().reportLimits()));
    }

    public NameReportScreen(Screen pLastScreen, ReportingContext pReportingContext, NameReport pReport) {
        this(pLastScreen, pReportingContext, new NameReport.Builder(pReport, pReportingContext.sender().reportLimits()));
    }

    @Override
    protected void init() {
        this.layout.defaultCellSetting().alignHorizontallyCenter();
        this.layout.addChild(new StringWidget(this.title, this.font));
        Component component = Component.literal(this.reportBuilder.report().getReportedName()).withStyle(ChatFormatting.YELLOW);
        this.layout
            .addChild(
                new StringWidget(Component.translatable("gui.abuseReport.name.reporting", component), this.font),
                p_300033_ -> p_300033_.alignHorizontallyLeft().padding(0, 8)
            );
        this.commentBox = this.createCommentBox(280, 9 * 8, p_300016_ -> {
            this.reportBuilder.setComments(p_300016_);
            this.onReportChanged();
        });
        this.layout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, MORE_COMMENTS_LABEL, p_299902_ -> p_299902_.paddingBottom(12)));
        LinearLayout linearlayout = this.layout.addChild(LinearLayout.horizontal().spacing(8));
        linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, p_313440_ -> this.onClose()).width(120).build());
        this.sendButton = linearlayout.addChild(Button.builder(SEND_REPORT, p_313441_ -> this.sendReport()).width(120).build());
        this.onReportChanged();
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    private void onReportChanged() {
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
