package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsInviteScreen extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component NAME_LABEL = Component.translatable("mco.configure.world.invite.profile.name").withColor(-6250336);
    private static final Component INVITING_PLAYER_TEXT = Component.translatable("mco.configure.world.players.inviting").withColor(-6250336);
    private static final Component NO_SUCH_PLAYER_ERROR_TEXT = Component.translatable("mco.configure.world.players.error").withColor(-65536);
    private EditBox profileName;
    private Button inviteButton;
    private final RealmsServer serverData;
    private final RealmsConfigureWorldScreen configureScreen;
    private final Screen lastScreen;
    @Nullable
    private Component message;

    public RealmsInviteScreen(RealmsConfigureWorldScreen pConfigureScreen, Screen pLastScreen, RealmsServer pServerData) {
        super(GameNarrator.NO_TITLE);
        this.configureScreen = pConfigureScreen;
        this.lastScreen = pLastScreen;
        this.serverData = pServerData;
    }

    @Override
    public void init() {
        this.profileName = new EditBox(
            this.minecraft.font, this.width / 2 - 100, row(2), 200, 20, null, Component.translatable("mco.configure.world.invite.profile.name")
        );
        this.addWidget(this.profileName);
        this.setInitialFocus(this.profileName);
        this.inviteButton = this.addRenderableWidget(
            Button.builder(Component.translatable("mco.configure.world.buttons.invite"), p_88721_ -> this.onInvite())
                .bounds(this.width / 2 - 100, row(10), 200, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL, p_280729_ -> this.minecraft.setScreen(this.lastScreen))
                .bounds(this.width / 2 - 100, row(12), 200, 20)
                .build()
        );
    }

    private void onInvite() {
        if (Util.isBlank(this.profileName.getValue())) {
            this.showMessage(NO_SUCH_PLAYER_ERROR_TEXT);
        } else {
            long i = this.serverData.id;
            String s = this.profileName.getValue().trim();
            this.inviteButton.active = false;
            this.profileName.setEditable(false);
            this.showMessage(INVITING_PLAYER_TEXT);
            CompletableFuture.<RealmsServer>supplyAsync(() -> {
                try {
                    return RealmsClient.create().invite(i, s);
                } catch (Exception exception) {
                    LOGGER.error("Couldn't invite user");
                    return null;
                }
            }, Util.ioPool()).thenAcceptAsync(p_289618_ -> {
                if (p_289618_ != null) {
                    this.serverData.players = p_289618_.players;
                    this.minecraft.setScreen(new RealmsPlayerScreen(this.configureScreen, this.serverData));
                } else {
                    this.showMessage(NO_SUCH_PLAYER_ERROR_TEXT);
                }

                this.profileName.setEditable(true);
                this.inviteButton.active = true;
            }, this.screenExecutor);
        }
    }

    private void showMessage(Component pMessage) {
        this.message = pMessage;
        this.minecraft.getNarrator().sayNow(pMessage);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) {
            this.minecraft.setScreen(this.lastScreen);
            return true;
        } else {
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
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
        pGuiGraphics.drawString(this.font, NAME_LABEL, this.width / 2 - 100, row(1), -1, false);
        if (this.message != null) {
            pGuiGraphics.drawCenteredString(this.font, this.message, this.width / 2, row(5), -1);
        }

        this.profileName.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }
}
