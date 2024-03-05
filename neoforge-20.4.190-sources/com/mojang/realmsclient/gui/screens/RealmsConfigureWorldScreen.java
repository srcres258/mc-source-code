package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.dto.WorldTemplate;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsWorldSlotButton;
import com.mojang.realmsclient.util.task.CloseServerTask;
import com.mojang.realmsclient.util.task.OpenServerTask;
import com.mojang.realmsclient.util.task.SwitchMinigameTask;
import com.mojang.realmsclient.util.task.SwitchSlotTask;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsConfigureWorldScreen extends RealmsScreen {
    private static final ResourceLocation EXPIRED_SPRITE = new ResourceLocation("realm_status/expired");
    private static final ResourceLocation EXPIRES_SOON_SPRITE = new ResourceLocation("realm_status/expires_soon");
    private static final ResourceLocation OPEN_SPRITE = new ResourceLocation("realm_status/open");
    private static final ResourceLocation CLOSED_SPRITE = new ResourceLocation("realm_status/closed");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component WORLD_LIST_TITLE = Component.translatable("mco.configure.worlds.title");
    private static final Component TITLE = Component.translatable("mco.configure.world.title");
    private static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
    private static final Component SERVER_EXPIRING_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
    private static final Component SERVER_EXPIRING_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
    private static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
    private static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
    private static final int DEFAULT_BUTTON_WIDTH = 80;
    private static final int DEFAULT_BUTTON_OFFSET = 5;
    @Nullable
    private Component toolTip;
    private final RealmsMainScreen lastScreen;
    @Nullable
    private RealmsServer serverData;
    private final long serverId;
    private int leftX;
    private int rightX;
    private Button playersButton;
    private Button settingsButton;
    private Button subscriptionButton;
    private Button optionsButton;
    private Button backupButton;
    private Button resetWorldButton;
    private Button switchMinigameButton;
    private boolean stateChanged;
    private final List<RealmsWorldSlotButton> slotButtonList = Lists.newArrayList();

    public RealmsConfigureWorldScreen(RealmsMainScreen pLastScreen, long pServerId) {
        super(TITLE);
        this.lastScreen = pLastScreen;
        this.serverId = pServerId;
    }

    @Override
    public void init() {
        if (this.serverData == null) {
            this.fetchServerData(this.serverId);
        }

        this.leftX = this.width / 2 - 187;
        this.rightX = this.width / 2 + 190;
        this.playersButton = this.addRenderableWidget(
            Button.builder(
                    Component.translatable("mco.configure.world.buttons.players"),
                    p_280722_ -> this.minecraft.setScreen(new RealmsPlayerScreen(this, this.serverData))
                )
                .bounds(this.centerButton(0, 3), row(0), 100, 20)
                .build()
        );
        this.settingsButton = this.addRenderableWidget(
            Button.builder(
                    Component.translatable("mco.configure.world.buttons.settings"),
                    p_280716_ -> this.minecraft.setScreen(new RealmsSettingsScreen(this, this.serverData.clone()))
                )
                .bounds(this.centerButton(1, 3), row(0), 100, 20)
                .build()
        );
        this.subscriptionButton = this.addRenderableWidget(
            Button.builder(
                    Component.translatable("mco.configure.world.buttons.subscription"),
                    p_280725_ -> this.minecraft.setScreen(new RealmsSubscriptionInfoScreen(this, this.serverData.clone(), this.lastScreen))
                )
                .bounds(this.centerButton(2, 3), row(0), 100, 20)
                .build()
        );
        this.slotButtonList.clear();

        for(int i = 1; i < 5; ++i) {
            this.slotButtonList.add(this.addSlotButton(i));
        }

        this.switchMinigameButton = this.addRenderableWidget(
            Button.builder(
                    Component.translatable("mco.configure.world.buttons.switchminigame"),
                    p_280711_ -> this.minecraft
                            .setScreen(
                                new RealmsSelectWorldTemplateScreen(
                                    Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME
                                )
                            )
                )
                .bounds(this.leftButton(0), row(13) - 5, 100, 20)
                .build()
        );
        this.optionsButton = this.addRenderableWidget(
            Button.builder(
                    Component.translatable("mco.configure.world.buttons.options"),
                    p_280720_ -> this.minecraft
                            .setScreen(
                                new RealmsSlotOptionsScreen(
                                    this, this.serverData.slots.get(this.serverData.activeSlot).clone(), this.serverData.worldType, this.serverData.activeSlot
                                )
                            )
                )
                .bounds(this.leftButton(0), row(13) - 5, 90, 20)
                .build()
        );
        this.backupButton = this.addRenderableWidget(
            Button.builder(
                    Component.translatable("mco.configure.world.backup"),
                    p_280715_ -> this.minecraft.setScreen(new RealmsBackupScreen(this, this.serverData.clone(), this.serverData.activeSlot))
                )
                .bounds(this.leftButton(1), row(13) - 5, 90, 20)
                .build()
        );
        this.resetWorldButton = this.addRenderableWidget(
            Button.builder(
                    Component.translatable("mco.configure.world.buttons.resetworld"),
                    p_300627_ -> this.minecraft
                            .setScreen(
                                RealmsResetWorldScreen.forResetSlot(
                                    this, this.serverData.clone(), () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen()))
                                )
                            )
                )
                .bounds(this.leftButton(2), row(13) - 5, 90, 20)
                .build()
        );
        this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_BACK, p_307024_ -> this.onClose()).bounds(this.rightX - 80 + 8, row(13) - 5, 70, 20).build()
        );
        this.backupButton.active = true;
        if (this.serverData == null) {
            this.hideMinigameButtons();
            this.hideRegularButtons();
            this.playersButton.active = false;
            this.settingsButton.active = false;
            this.subscriptionButton.active = false;
        } else {
            this.disableButtons();
            if (this.isMinigame()) {
                this.hideRegularButtons();
            } else {
                this.hideMinigameButtons();
            }
        }
    }

    private RealmsWorldSlotButton addSlotButton(int pIndex) {
        int i = this.frame(pIndex);
        int j = row(5) + 5;
        RealmsWorldSlotButton realmsworldslotbutton = new RealmsWorldSlotButton(i, j, 80, 80, pIndex, p_167389_ -> {
            RealmsWorldSlotButton.State realmsworldslotbutton$state = ((RealmsWorldSlotButton)p_167389_).getState();
            if (realmsworldslotbutton$state != null) {
                switch(realmsworldslotbutton$state.action) {
                    case NOTHING:
                        break;
                    case JOIN:
                        this.joinRealm(this.serverData);
                        break;
                    case SWITCH_SLOT:
                        if (realmsworldslotbutton$state.minigame) {
                            this.switchToMinigame();
                        } else if (realmsworldslotbutton$state.empty) {
                            this.switchToEmptySlot(pIndex, this.serverData);
                        } else {
                            this.switchToFullSlot(pIndex, this.serverData);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unknown action " + realmsworldslotbutton$state.action);
                }
            }
        });
        if (this.serverData != null) {
            realmsworldslotbutton.setServerData(this.serverData);
        }

        return this.addRenderableWidget(realmsworldslotbutton);
    }

    private int leftButton(int pIndex) {
        return this.leftX + pIndex * 95;
    }

    private int centerButton(int pRow, int pColumn) {
        return this.width / 2 - (pColumn * 105 - 5) / 2 + pRow * 105;
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
        this.toolTip = null;
        pGuiGraphics.drawCenteredString(this.font, WORLD_LIST_TITLE, this.width / 2, row(4), -1);
        if (this.serverData == null) {
            pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 17, -1);
        } else {
            String s = this.serverData.getName();
            int i = this.font.width(s);
            int j = this.serverData.state == RealmsServer.State.CLOSED ? -6250336 : 8388479;
            int k = this.font.width(this.title);
            pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 12, -1);
            pGuiGraphics.drawCenteredString(this.font, s, this.width / 2, 24, j);
            int l = Math.min(this.centerButton(2, 3) + 80 - 11, this.width / 2 + i / 2 + k / 2 + 10);
            this.drawServerStatus(pGuiGraphics, l, 7, pMouseX, pMouseY);
            if (this.isMinigame()) {
                pGuiGraphics.drawString(
                    this.font,
                    Component.translatable("mco.configure.world.minigame", this.serverData.getMinigameName()),
                    this.leftX + 80 + 20 + 10,
                    row(13),
                    -1,
                    false
                );
            }
        }
    }

    private int frame(int p_88488_) {
        return this.leftX + (p_88488_ - 1) * 98;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
        if (this.stateChanged) {
            this.lastScreen.resetScreen();
        }
    }

    private void fetchServerData(long pServerId) {
        new Thread(() -> {
            RealmsClient realmsclient = RealmsClient.create();

            try {
                RealmsServer realmsserver = realmsclient.getOwnWorld(pServerId);
                this.minecraft.execute(() -> {
                    this.serverData = realmsserver;
                    this.disableButtons();
                    if (this.isMinigame()) {
                        this.show(this.switchMinigameButton);
                    } else {
                        this.show(this.optionsButton);
                        this.show(this.backupButton);
                        this.show(this.resetWorldButton);
                    }

                    for(RealmsWorldSlotButton realmsworldslotbutton : this.slotButtonList) {
                        realmsworldslotbutton.setServerData(realmsserver);
                    }
                });
            } catch (RealmsServiceException realmsserviceexception) {
                LOGGER.error("Couldn't get own world", (Throwable)realmsserviceexception);
                this.minecraft.execute(() -> this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this.lastScreen)));
            }
        }).start();
    }

    private void disableButtons() {
        this.playersButton.active = !this.serverData.expired;
        this.settingsButton.active = !this.serverData.expired;
        this.subscriptionButton.active = true;
        this.switchMinigameButton.active = !this.serverData.expired;
        this.optionsButton.active = !this.serverData.expired;
        this.resetWorldButton.active = !this.serverData.expired;
    }

    private void joinRealm(RealmsServer pServer) {
        if (this.serverData.state == RealmsServer.State.OPEN) {
            RealmsMainScreen.play(pServer, new RealmsConfigureWorldScreen(this.lastScreen, this.serverId));
        } else {
            this.openTheWorld(true, new RealmsConfigureWorldScreen(this.lastScreen, this.serverId));
        }
    }

    private void switchToMinigame() {
        RealmsSelectWorldTemplateScreen realmsselectworldtemplatescreen = new RealmsSelectWorldTemplateScreen(
            Component.translatable("mco.template.title.minigame"), this::templateSelectionCallback, RealmsServer.WorldType.MINIGAME
        );
        realmsselectworldtemplatescreen.setWarning(
            Component.translatable("mco.minigame.world.info.line1"), Component.translatable("mco.minigame.world.info.line2")
        );
        this.minecraft.setScreen(realmsselectworldtemplatescreen);
    }

    private void switchToFullSlot(int pSlot, RealmsServer pServer) {
        Component component = Component.translatable("mco.configure.world.slot.switch.question.line1");
        Component component1 = Component.translatable("mco.configure.world.slot.switch.question.line2");
        this.minecraft
            .setScreen(
                new RealmsLongConfirmationScreen(
                    p_307023_ -> {
                        if (p_307023_) {
                            this.stateChanged();
                            this.minecraft
                                .setScreen(
                                    new RealmsLongRunningMcoTaskScreen(
                                        this.lastScreen,
                                        new SwitchSlotTask(
                                            pServer.id, pSlot, () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen()))
                                        )
                                    )
                                );
                        } else {
                            this.minecraft.setScreen(this);
                        }
                    },
                    RealmsLongConfirmationScreen.Type.INFO,
                    component,
                    component1,
                    true
                )
            );
    }

    private void switchToEmptySlot(int pSlot, RealmsServer pServer) {
        Component component = Component.translatable("mco.configure.world.slot.switch.question.line1");
        Component component1 = Component.translatable("mco.configure.world.slot.switch.question.line2");
        this.minecraft
            .setScreen(
                new RealmsLongConfirmationScreen(
                    p_307020_ -> {
                        if (p_307020_) {
                            this.stateChanged();
                            RealmsResetWorldScreen realmsresetworldscreen = RealmsResetWorldScreen.forEmptySlot(
                                this, pSlot, pServer, () -> this.minecraft.execute(() -> this.minecraft.setScreen(this.getNewScreen()))
                            );
                            this.minecraft.setScreen(realmsresetworldscreen);
                        } else {
                            this.minecraft.setScreen(this);
                        }
                    },
                    RealmsLongConfirmationScreen.Type.INFO,
                    component,
                    component1,
                    true
                )
            );
    }

    private void drawServerStatus(GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY) {
        if (this.serverData.expired) {
            this.drawRealmStatus(pGuiGraphics, pX, pY, pMouseX, pMouseY, EXPIRED_SPRITE, () -> SERVER_EXPIRED_TOOLTIP);
        } else if (this.serverData.state == RealmsServer.State.CLOSED) {
            this.drawRealmStatus(pGuiGraphics, pX, pY, pMouseX, pMouseY, CLOSED_SPRITE, () -> SERVER_CLOSED_TOOLTIP);
        } else if (this.serverData.state == RealmsServer.State.OPEN) {
            if (this.serverData.daysLeft < 7) {
                this.drawRealmStatus(
                    pGuiGraphics,
                    pX,
                    pY,
                    pMouseX,
                    pMouseY,
                    EXPIRES_SOON_SPRITE,
                    () -> {
                        if (this.serverData.daysLeft <= 0) {
                            return SERVER_EXPIRING_SOON_TOOLTIP;
                        } else {
                            return (Component)(this.serverData.daysLeft == 1
                                ? SERVER_EXPIRING_IN_DAY_TOOLTIP
                                : Component.translatable("mco.selectServer.expires.days", this.serverData.daysLeft));
                        }
                    }
                );
            } else {
                this.drawRealmStatus(pGuiGraphics, pX, pY, pMouseX, pMouseY, OPEN_SPRITE, () -> SERVER_OPEN_TOOLTIP);
            }
        }
    }

    private void drawRealmStatus(
        GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY, ResourceLocation pSprite, Supplier<Component> pTooltipSupplier
    ) {
        pGuiGraphics.blitSprite(pSprite, pX, pY, 10, 28);
        if (pMouseX >= pX && pMouseX <= pX + 9 && pMouseY >= pY && pMouseY <= pY + 27) {
            this.setTooltipForNextRenderPass(pTooltipSupplier.get());
        }
    }

    private boolean isMinigame() {
        return this.serverData != null && this.serverData.worldType == RealmsServer.WorldType.MINIGAME;
    }

    private void hideRegularButtons() {
        this.hide(this.optionsButton);
        this.hide(this.backupButton);
        this.hide(this.resetWorldButton);
    }

    private void hide(Button pButton) {
        pButton.visible = false;
    }

    private void show(Button pButton) {
        pButton.visible = true;
    }

    private void hideMinigameButtons() {
        this.hide(this.switchMinigameButton);
    }

    public void saveSlotSettings(RealmsWorldOptions pWorldOptions) {
        RealmsWorldOptions realmsworldoptions = this.serverData.slots.get(this.serverData.activeSlot);
        pWorldOptions.templateId = realmsworldoptions.templateId;
        pWorldOptions.templateImage = realmsworldoptions.templateImage;
        RealmsClient realmsclient = RealmsClient.create();

        try {
            realmsclient.updateSlot(this.serverData.id, this.serverData.activeSlot, pWorldOptions);
            this.serverData.slots.put(this.serverData.activeSlot, pWorldOptions);
        } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't save slot settings", (Throwable)realmsserviceexception);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this));
            return;
        }

        this.minecraft.setScreen(this);
    }

    public void saveSettings(String pKey, String pValue) {
        String s = Util.isBlank(pValue) ? null : pValue;
        RealmsClient realmsclient = RealmsClient.create();

        try {
            realmsclient.update(this.serverData.id, pKey, s);
            this.serverData.setName(pKey);
            this.serverData.setDescription(s);
            this.stateChanged();
        } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't save settings", (Throwable)realmsserviceexception);
            this.minecraft.setScreen(new RealmsGenericErrorScreen(realmsserviceexception, this));
            return;
        }

        this.minecraft.setScreen(this);
    }

    public void openTheWorld(boolean pJoin, Screen pLastScreen) {
        this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new OpenServerTask(this.serverData, this, pJoin, this.minecraft)));
    }

    public void closeTheWorld(Screen pLastScreen) {
        this.minecraft.setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new CloseServerTask(this.serverData, this)));
    }

    public void stateChanged() {
        this.stateChanged = true;
    }

    private void templateSelectionCallback(@Nullable WorldTemplate p_167395_) {
        if (p_167395_ != null && WorldTemplate.WorldTemplateType.MINIGAME == p_167395_.type) {
            this.stateChanged();
            this.minecraft
                .setScreen(new RealmsLongRunningMcoTaskScreen(this.lastScreen, new SwitchMinigameTask(this.serverData.id, p_167395_, this.getNewScreen())));
        } else {
            this.minecraft.setScreen(this);
        }
    }

    public RealmsConfigureWorldScreen getNewScreen() {
        RealmsConfigureWorldScreen realmsconfigureworldscreen = new RealmsConfigureWorldScreen(this.lastScreen, this.serverId);
        realmsconfigureworldscreen.stateChanged = this.stateChanged;
        return realmsconfigureworldscreen;
    }
}
