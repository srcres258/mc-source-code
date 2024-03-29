package net.minecraft.client.gui.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GenericDirtMessageScreen extends Screen {
    public GenericDirtMessageScreen(Component p_96061_) {
        super(p_96061_);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected boolean shouldNarrateNavigation() {
        return false;
    }

    @Override
    public void render(GuiGraphics p_281274_, int p_283012_, int p_282072_, float p_282608_) {
        super.render(p_281274_, p_283012_, p_282072_, p_282608_);
        p_281274_.drawCenteredString(this.font, this.title, this.width / 2, 70, 16777215);
    }

    @Override
    public void renderBackground(GuiGraphics p_295014_, int p_294262_, int p_295912_, float p_296387_) {
        this.renderDirtBackground(p_295014_);
    }
}
