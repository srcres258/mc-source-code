package net.minecraft.client.gui.screens;

import java.util.Arrays;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SoundOptionsScreen extends OptionsSubScreen {
    private OptionsList list;

    private static OptionInstance<?>[] buttonOptions(Options pOptions) {
        return new OptionInstance[]{pOptions.showSubtitles(), pOptions.directionalAudio()};
    }

    public SoundOptionsScreen(Screen pLastScreen, Options pOptions) {
        super(pLastScreen, pOptions, Component.translatable("options.sounds.title"));
    }

    @Override
    protected void init() {
        this.list = this.addRenderableWidget(new OptionsList(this.minecraft, this.width, this.height - 64, 32, 25));
        this.list.addBig(this.options.getSoundSourceOptionInstance(SoundSource.MASTER));
        this.list.addSmall(this.getAllSoundOptionsExceptMaster());
        this.list.addBig(this.options.soundDevice());
        this.list.addSmall(buttonOptions(this.options));
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, p_280829_ -> {
            this.minecraft.options.save();
            this.minecraft.setScreen(this.lastScreen);
        }).bounds(this.width / 2 - 100, this.height - 27, 200, 20).build());
    }

    private OptionInstance<?>[] getAllSoundOptionsExceptMaster() {
        return Arrays.stream(SoundSource.values())
            .filter(p_247780_ -> p_247780_ != SoundSource.MASTER)
            .map(p_247779_ -> this.options.getSoundSourceOptionInstance(p_247779_))
            .toArray(p_247778_ -> new OptionInstance[p_247778_]);
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
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderDirtBackground(pGuiGraphics);
    }
}
