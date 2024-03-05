package net.minecraft.client.gui.screens;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class UnsupportedGraphicsWarningScreen extends Screen {
    private static final int BUTTON_PADDING = 20;
    private static final int BUTTON_MARGIN = 5;
    private static final int BUTTON_HEIGHT = 20;
    private final Component narrationMessage;
    private final FormattedText message;
    private final ImmutableList<UnsupportedGraphicsWarningScreen.ButtonOption> buttonOptions;
    private MultiLineLabel messageLines = MultiLineLabel.EMPTY;
    private int contentTop;
    private int buttonWidth;

    protected UnsupportedGraphicsWarningScreen(
        Component pTitle, List<Component> pMessage, ImmutableList<UnsupportedGraphicsWarningScreen.ButtonOption> pButtonOptions
    ) {
        super(pTitle);
        this.message = FormattedText.composite(pMessage);
        this.narrationMessage = CommonComponents.joinForNarration(pTitle, ComponentUtils.formatList(pMessage, CommonComponents.EMPTY));
        this.buttonOptions = pButtonOptions;
    }

    @Override
    public Component getNarrationMessage() {
        return this.narrationMessage;
    }

    @Override
    public void init() {
        for(UnsupportedGraphicsWarningScreen.ButtonOption unsupportedgraphicswarningscreen$buttonoption : this.buttonOptions) {
            this.buttonWidth = Math.max(this.buttonWidth, 20 + this.font.width(unsupportedgraphicswarningscreen$buttonoption.message) + 20);
        }

        int l = 5 + this.buttonWidth + 5;
        int i1 = l * this.buttonOptions.size();
        this.messageLines = MultiLineLabel.create(this.font, this.message, i1);
        int i = this.messageLines.getLineCount() * 9;
        this.contentTop = (int)((double)this.height / 2.0 - (double)i / 2.0);
        int j = this.contentTop + i + 9 * 2;
        int k = (int)((double)this.width / 2.0 - (double)i1 / 2.0);

        for(UnsupportedGraphicsWarningScreen.ButtonOption unsupportedgraphicswarningscreen$buttonoption1 : this.buttonOptions) {
            this.addRenderableWidget(
                Button.builder(unsupportedgraphicswarningscreen$buttonoption1.message, unsupportedgraphicswarningscreen$buttonoption1.onPress)
                    .bounds(k, j, this.buttonWidth, 20)
                    .build()
            );
            k += l;
        }
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
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.contentTop - 9 * 2, -1);
        this.messageLines.renderCentered(pGuiGraphics, this.width / 2, this.contentTop);
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderDirtBackground(pGuiGraphics);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public static final class ButtonOption {
        final Component message;
        final Button.OnPress onPress;

        public ButtonOption(Component pMessage, Button.OnPress pOnPress) {
            this.message = pMessage;
            this.onPress = pOnPress;
        }
    }
}
