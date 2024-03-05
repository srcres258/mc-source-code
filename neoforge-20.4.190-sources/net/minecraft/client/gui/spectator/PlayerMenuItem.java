package net.minecraft.client.gui.spectator;

import com.mojang.authlib.GameProfile;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerMenuItem implements SpectatorMenuItem {
    private final GameProfile profile;
    private final Supplier<PlayerSkin> skin;
    private final Component name;

    public PlayerMenuItem(GameProfile pProfile) {
        this.profile = pProfile;
        this.skin = Minecraft.getInstance().getSkinManager().lookupInsecure(pProfile);
        this.name = Component.literal(pProfile.getName());
    }

    @Override
    public void selectItem(SpectatorMenu pMenu) {
        Minecraft.getInstance().getConnection().send(new ServerboundTeleportToEntityPacket(this.profile.getId()));
    }

    @Override
    public Component getName() {
        return this.name;
    }

    @Override
    public void renderIcon(GuiGraphics pGuiGraphics, float pShadeColor, int pAlpha) {
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, (float)pAlpha / 255.0F);
        PlayerFaceRenderer.draw(pGuiGraphics, this.skin.get(), 2, 2, 12);
        pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
