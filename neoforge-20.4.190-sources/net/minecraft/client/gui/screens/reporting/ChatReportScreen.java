package net.minecraft.client.gui.screens.reporting;

import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.UUID;
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
import net.minecraft.client.multiplayer.chat.report.ChatReport;
import net.minecraft.client.multiplayer.chat.report.Report;
import net.minecraft.client.multiplayer.chat.report.ReportReason;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChatReportScreen extends AbstractReportScreen<ChatReport.Builder> {
    private static final int BUTTON_WIDTH = 120;
    private static final Component TITLE = Component.translatable("gui.chatReport.title");
    private static final Component SELECT_CHAT_MESSAGE = Component.translatable("gui.chatReport.select_chat");
    private final LinearLayout layout = LinearLayout.vertical().spacing(8);
    private MultiLineEditBox commentBox;
    private Button sendButton;
    private Button selectMessagesButton;
    private Button selectReasonButton;

    private ChatReportScreen(Screen pLastScreen, ReportingContext pReportingContext, ChatReport.Builder pReportBuilder) {
        super(TITLE, pLastScreen, pReportingContext, pReportBuilder);
    }

    public ChatReportScreen(Screen pLastScreen, ReportingContext pReportingContext, UUID pReportId) {
        this(pLastScreen, pReportingContext, new ChatReport.Builder(pReportId, pReportingContext.sender().reportLimits()));
    }

    public ChatReportScreen(Screen pLastScreen, ReportingContext pReportContext, ChatReport pReport) {
        this(pLastScreen, pReportContext, new ChatReport.Builder(pReport, pReportContext.sender().reportLimits()));
    }

    @Override
    protected void init() {
        this.layout.defaultCellSetting().alignHorizontallyCenter();
        this.layout.addChild(new StringWidget(this.title, this.font));
        this.selectMessagesButton = this.layout
            .addChild(
                Button.builder(
                        SELECT_CHAT_MESSAGE,
                        p_299790_ -> this.minecraft.setScreen(new ChatSelectionScreen(this, this.reportingContext, this.reportBuilder, p_299791_ -> {
                                this.reportBuilder = p_299791_;
                                this.onReportChanged();
                            }))
                    )
                    .width(280)
                    .build()
            );
        this.selectReasonButton = Button.builder(
                SELECT_REASON, p_299796_ -> this.minecraft.setScreen(new ReportReasonSelectionScreen(this, this.reportBuilder.reason(), p_299789_ -> {
                        this.reportBuilder.setReason(p_299789_);
                        this.onReportChanged();
                    }))
            )
            .width(280)
            .build();
        this.layout.addChild(CommonLayouts.labeledElement(this.font, this.selectReasonButton, OBSERVED_WHAT_LABEL));
        this.commentBox = this.createCommentBox(280, 9 * 8, p_299797_ -> {
            this.reportBuilder.setComments(p_299797_);
            this.onReportChanged();
        });
        this.layout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, MORE_COMMENTS_LABEL, p_299798_ -> p_299798_.paddingBottom(12)));
        LinearLayout linearlayout = this.layout.addChild(LinearLayout.horizontal().spacing(8));
        linearlayout.addChild(Button.builder(CommonComponents.GUI_BACK, p_313439_ -> this.onClose()).width(120).build());
        this.sendButton = linearlayout.addChild(Button.builder(SEND_REPORT, p_313438_ -> this.sendReport()).width(120).build());
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
        IntSet intset = this.reportBuilder.reportedMessages();
        if (intset.isEmpty()) {
            this.selectMessagesButton.setMessage(SELECT_CHAT_MESSAGE);
        } else {
            this.selectMessagesButton.setMessage(Component.translatable("gui.chatReport.selected_chat", intset.size()));
        }

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
