package com.mojang.realmsclient;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import com.mojang.realmsclient.client.Ping;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PingResult;
import com.mojang.realmsclient.dto.RealmsNews;
import com.mojang.realmsclient.dto.RealmsNotification;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RegionPingResult;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.gui.RealmsServerList;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsCreateRealmScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongConfirmationScreen;
import com.mojang.realmsclient.gui.screens.RealmsLongRunningMcoTaskScreen;
import com.mojang.realmsclient.gui.screens.RealmsPendingInvitesScreen;
import com.mojang.realmsclient.gui.screens.RealmsPopupScreen;
import com.mojang.realmsclient.gui.task.DataFetcher;
import com.mojang.realmsclient.util.RealmsPersistence;
import com.mojang.realmsclient.util.RealmsUtil;
import com.mojang.realmsclient.util.task.GetServerDetailsTask;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.LoadingDotsWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PopupScreen;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsObjectSelectionList;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.CommonLinks;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsMainScreen extends RealmsScreen {
    static final ResourceLocation INFO_SPRITE = new ResourceLocation("icon/info");
    static final ResourceLocation NEW_REALM_SPRITE = new ResourceLocation("icon/new_realm");
    static final ResourceLocation EXPIRED_SPRITE = new ResourceLocation("realm_status/expired");
    static final ResourceLocation EXPIRES_SOON_SPRITE = new ResourceLocation("realm_status/expires_soon");
    static final ResourceLocation OPEN_SPRITE = new ResourceLocation("realm_status/open");
    static final ResourceLocation CLOSED_SPRITE = new ResourceLocation("realm_status/closed");
    private static final ResourceLocation INVITE_SPRITE = new ResourceLocation("icon/invite");
    private static final ResourceLocation NEWS_SPRITE = new ResourceLocation("icon/news");
    static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation LOGO_LOCATION = new ResourceLocation("textures/gui/title/realms.png");
    private static final ResourceLocation NO_REALMS_LOCATION = new ResourceLocation("textures/gui/realms/no_realms.png");
    private static final Component TITLE = Component.translatable("menu.online");
    private static final Component LOADING_TEXT = Component.translatable("mco.selectServer.loading");
    static final Component SERVER_UNITIALIZED_TEXT = Component.translatable("mco.selectServer.uninitialized");
    static final Component SUBSCRIPTION_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredList");
    private static final Component SUBSCRIPTION_RENEW_TEXT = Component.translatable("mco.selectServer.expiredRenew");
    static final Component TRIAL_EXPIRED_TEXT = Component.translatable("mco.selectServer.expiredTrial");
    private static final Component PLAY_TEXT = Component.translatable("mco.selectServer.play");
    private static final Component LEAVE_SERVER_TEXT = Component.translatable("mco.selectServer.leave");
    private static final Component CONFIGURE_SERVER_TEXT = Component.translatable("mco.selectServer.configure");
    static final Component SERVER_EXPIRED_TOOLTIP = Component.translatable("mco.selectServer.expired");
    static final Component SERVER_EXPIRES_SOON_TOOLTIP = Component.translatable("mco.selectServer.expires.soon");
    static final Component SERVER_EXPIRES_IN_DAY_TOOLTIP = Component.translatable("mco.selectServer.expires.day");
    static final Component SERVER_OPEN_TOOLTIP = Component.translatable("mco.selectServer.open");
    static final Component SERVER_CLOSED_TOOLTIP = Component.translatable("mco.selectServer.closed");
    static final Component UNITIALIZED_WORLD_NARRATION = Component.translatable("gui.narrate.button", SERVER_UNITIALIZED_TEXT);
    private static final Component NO_REALMS_TEXT = Component.translatable("mco.selectServer.noRealms");
    private static final Component NO_PENDING_INVITES = Component.translatable("mco.invites.nopending");
    private static final Component PENDING_INVITES = Component.translatable("mco.invites.pending");
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_COLUMNS = 3;
    private static final int BUTTON_SPACING = 4;
    private static final int CONTENT_WIDTH = 308;
    private static final int LOGO_WIDTH = 128;
    private static final int LOGO_HEIGHT = 34;
    private static final int LOGO_TEXTURE_WIDTH = 128;
    private static final int LOGO_TEXTURE_HEIGHT = 64;
    private static final int LOGO_PADDING = 5;
    private static final int HEADER_HEIGHT = 44;
    private static final int FOOTER_PADDING = 11;
    private static final int NEW_REALM_SPRITE_WIDTH = 40;
    private static final int NEW_REALM_SPRITE_HEIGHT = 20;
    private static final int ENTRY_WIDTH = 216;
    private static final int ITEM_HEIGHT = 36;
    private static final boolean SNAPSHOT = !SharedConstants.getCurrentVersion().isStable();
    private static boolean snapshotToggle = SNAPSHOT;
    private final CompletableFuture<RealmsAvailability.Result> availability = RealmsAvailability.get();
    @Nullable
    private DataFetcher.Subscription dataSubscription;
    private final Set<UUID> handledSeenNotifications = new HashSet<>();
    private static boolean regionsPinged;
    private final RateLimiter inviteNarrationLimiter;
    private final Screen lastScreen;
    private Button playButton;
    private Button backButton;
    private Button renewButton;
    private Button configureButton;
    private Button leaveButton;
    RealmsMainScreen.RealmSelectionList realmSelectionList;
    private RealmsServerList serverList;
    private List<RealmsServer> availableSnapshotServers = List.of();
    private volatile boolean trialsAvailable;
    @Nullable
    private volatile String newsLink;
    long lastClickTime;
    private final List<RealmsNotification> notifications = new ArrayList<>();
    private Button addRealmButton;
    private RealmsMainScreen.NotificationButton pendingInvitesButton;
    private RealmsMainScreen.NotificationButton newsButton;
    private RealmsMainScreen.LayoutState activeLayoutState;
    @Nullable
    private HeaderAndFooterLayout layout;

    public RealmsMainScreen(Screen pLastScreen) {
        super(TITLE);
        this.lastScreen = pLastScreen;
        this.inviteNarrationLimiter = RateLimiter.create(0.016666668F);
    }

    @Override
    public void init() {
        this.serverList = new RealmsServerList(this.minecraft);
        this.realmSelectionList = new RealmsMainScreen.RealmSelectionList();
        Component component = Component.translatable("mco.invites.title");
        this.pendingInvitesButton = new RealmsMainScreen.NotificationButton(
            component, INVITE_SPRITE, p_293547_ -> this.minecraft.setScreen(new RealmsPendingInvitesScreen(this, component))
        );
        Component component1 = Component.translatable("mco.news");
        this.newsButton = new RealmsMainScreen.NotificationButton(component1, NEWS_SPRITE, p_307015_ -> {
            String s = this.newsLink;
            if (s != null) {
                ConfirmLinkScreen.confirmLinkNow(this, s);
                if (this.newsButton.notificationCount() != 0) {
                    RealmsPersistence.RealmsPersistenceData realmspersistence$realmspersistencedata = RealmsPersistence.readFile();
                    realmspersistence$realmspersistencedata.hasUnreadNews = false;
                    RealmsPersistence.writeFile(realmspersistence$realmspersistencedata);
                    this.newsButton.setNotificationCount(0);
                }
            }
        });
        this.newsButton.setTooltip(Tooltip.create(component1));
        this.playButton = Button.builder(PLAY_TEXT, p_302303_ -> play(this.getSelectedServer(), this)).width(100).build();
        this.configureButton = Button.builder(CONFIGURE_SERVER_TEXT, p_86672_ -> this.configureClicked(this.getSelectedServer())).width(100).build();
        this.renewButton = Button.builder(SUBSCRIPTION_RENEW_TEXT, p_86622_ -> this.onRenew(this.getSelectedServer())).width(100).build();
        this.leaveButton = Button.builder(LEAVE_SERVER_TEXT, p_86679_ -> this.leaveClicked(this.getSelectedServer())).width(100).build();
        this.addRealmButton = Button.builder(Component.translatable("mco.selectServer.purchase"), p_300620_ -> this.openTrialAvailablePopup())
            .size(100, 20)
            .build();
        this.backButton = Button.builder(CommonComponents.GUI_BACK, p_293552_ -> this.minecraft.setScreen(this.lastScreen)).width(100).build();
        if (RealmsClient.ENVIRONMENT == RealmsClient.Environment.STAGE) {
            this.addRenderableWidget(
                CycleButton.booleanBuilder(Component.literal("Snapshot"), Component.literal("Release"))
                    .create(5, 5, 100, 20, Component.literal("Realm"), (p_305606_, p_305607_) -> {
                        snapshotToggle = p_305607_;
                        this.availableSnapshotServers = List.of();
                        this.debugRefreshDataFetchers();
                    })
            );
        }

        this.updateLayout(RealmsMainScreen.LayoutState.LOADING);
        this.updateButtonStates();
        this.availability.thenAcceptAsync(p_293549_ -> {
            Screen screen = p_293549_.createErrorScreen(this.lastScreen);
            if (screen == null) {
                this.dataSubscription = this.initDataFetcher(this.minecraft.realmsDataFetcher());
            } else {
                this.minecraft.setScreen(screen);
            }
        }, this.screenExecutor);
    }

    public static boolean isSnapshot() {
        return SNAPSHOT && snapshotToggle;
    }

    @Override
    protected void repositionElements() {
        if (this.layout != null) {
            this.realmSelectionList.setSize(this.width, this.height - this.layout.getFooterHeight() - this.layout.getHeaderHeight());
            this.layout.arrangeElements();
        }
    }

    private void updateLayout() {
        if (this.serverList.isEmpty() && this.availableSnapshotServers.isEmpty() && this.notifications.isEmpty()) {
            this.updateLayout(RealmsMainScreen.LayoutState.NO_REALMS);
        } else {
            this.updateLayout(RealmsMainScreen.LayoutState.LIST);
        }
    }

    private void updateLayout(RealmsMainScreen.LayoutState pLayoutState) {
        if (this.activeLayoutState != pLayoutState) {
            if (this.layout != null) {
                this.layout.visitWidgets(p_293554_ -> this.removeWidget(p_293554_));
            }

            this.layout = this.createLayout(pLayoutState);
            this.activeLayoutState = pLayoutState;
            this.layout.visitWidgets(p_272289_ -> {
            });
            this.repositionElements();
        }
    }

    private HeaderAndFooterLayout createLayout(RealmsMainScreen.LayoutState pLayoutState) {
        HeaderAndFooterLayout headerandfooterlayout = new HeaderAndFooterLayout(this);
        headerandfooterlayout.setHeaderHeight(44);
        headerandfooterlayout.addToHeader(this.createHeader());
        Layout layout = this.createFooter(pLayoutState);
        layout.arrangeElements();
        headerandfooterlayout.setFooterHeight(layout.getHeight() + 22);
        headerandfooterlayout.addToFooter(layout);
        switch(pLayoutState) {
            case LOADING:
                headerandfooterlayout.addToContents(new LoadingDotsWidget(this.font, LOADING_TEXT));
                break;
            case NO_REALMS:
                headerandfooterlayout.addToContents(this.createNoRealmsContent());
                break;
            case LIST:
                headerandfooterlayout.addToContents(this.realmSelectionList);
        }

        return headerandfooterlayout;
    }

    private Layout createHeader() {
        int i = 90;
        LinearLayout linearlayout = LinearLayout.horizontal().spacing(4);
        linearlayout.defaultCellSetting().alignVerticallyMiddle();
        linearlayout.addChild(this.pendingInvitesButton);
        linearlayout.addChild(this.newsButton);
        LinearLayout linearlayout1 = LinearLayout.horizontal();
        linearlayout1.defaultCellSetting().alignVerticallyMiddle();
        linearlayout1.addChild(SpacerElement.width(90));
        linearlayout1.addChild(ImageWidget.texture(128, 34, LOGO_LOCATION, 128, 64), LayoutSettings::alignHorizontallyCenter);
        linearlayout1.addChild(new FrameLayout(90, 44)).addChild(linearlayout, LayoutSettings::alignHorizontallyRight);
        return linearlayout1;
    }

    private Layout createFooter(RealmsMainScreen.LayoutState pLayoutState) {
        GridLayout gridlayout = new GridLayout().spacing(4);
        GridLayout.RowHelper gridlayout$rowhelper = gridlayout.createRowHelper(3);
        if (pLayoutState == RealmsMainScreen.LayoutState.LIST) {
            gridlayout$rowhelper.addChild(this.playButton);
            gridlayout$rowhelper.addChild(this.configureButton);
            gridlayout$rowhelper.addChild(this.renewButton);
            gridlayout$rowhelper.addChild(this.leaveButton);
        }

        gridlayout$rowhelper.addChild(this.addRealmButton);
        gridlayout$rowhelper.addChild(this.backButton);
        return gridlayout;
    }

    private LinearLayout createNoRealmsContent() {
        LinearLayout linearlayout = LinearLayout.vertical().spacing(10);
        linearlayout.defaultCellSetting().alignHorizontallyCenter();
        linearlayout.addChild(ImageWidget.texture(130, 64, NO_REALMS_LOCATION, 130, 64));
        FocusableTextWidget focusabletextwidget = new FocusableTextWidget(308, NO_REALMS_TEXT, this.font, false);
        linearlayout.addChild(focusabletextwidget);
        return linearlayout;
    }

    void updateButtonStates() {
        RealmsServer realmsserver = this.getSelectedServer();
        this.addRealmButton.active = this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING;
        this.playButton.active = realmsserver != null && this.shouldPlayButtonBeActive(realmsserver);
        this.renewButton.active = realmsserver != null && this.shouldRenewButtonBeActive(realmsserver);
        this.leaveButton.active = realmsserver != null && this.shouldLeaveButtonBeActive(realmsserver);
        this.configureButton.active = realmsserver != null && this.shouldConfigureButtonBeActive(realmsserver);
    }

    boolean shouldPlayButtonBeActive(RealmsServer pRealmsServer) {
        boolean flag = !pRealmsServer.expired && pRealmsServer.state == RealmsServer.State.OPEN;
        return flag && (pRealmsServer.isCompatible() || this.isSelfOwnedServer(pRealmsServer));
    }

    private boolean shouldRenewButtonBeActive(RealmsServer pRealmsServer) {
        return pRealmsServer.expired && this.isSelfOwnedServer(pRealmsServer);
    }

    private boolean shouldConfigureButtonBeActive(RealmsServer pRealmsServer) {
        return this.isSelfOwnedServer(pRealmsServer) && pRealmsServer.state != RealmsServer.State.UNINITIALIZED;
    }

    private boolean shouldLeaveButtonBeActive(RealmsServer pRealmsServer) {
        return !this.isSelfOwnedServer(pRealmsServer);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.dataSubscription != null) {
            this.dataSubscription.tick();
        }
    }

    public static void refreshPendingInvites() {
        Minecraft.getInstance().realmsDataFetcher().pendingInvitesTask.reset();
    }

    public static void refreshServerList() {
        Minecraft.getInstance().realmsDataFetcher().serverListUpdateTask.reset();
    }

    private void debugRefreshDataFetchers() {
        for(DataFetcher.Task<?> task : this.minecraft.realmsDataFetcher().getTasks()) {
            task.reset();
        }
    }

    private DataFetcher.Subscription initDataFetcher(RealmsDataFetcher pDataFetcher) {
        DataFetcher.Subscription datafetcher$subscription = pDataFetcher.dataFetcher.createSubscription();
        datafetcher$subscription.subscribe(pDataFetcher.serverListUpdateTask, p_305616_ -> {
            this.serverList.updateServersList(p_305616_.serverList());
            this.availableSnapshotServers = p_305616_.availableSnapshotServers();
            this.refreshListAndLayout();
            boolean flag = false;

            for(RealmsServer realmsserver : this.serverList) {
                if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
                    flag = true;
                }
            }

            if (!regionsPinged && flag) {
                regionsPinged = true;
                this.pingRegions();
            }
        });
        callRealmsClient(RealmsClient::getNotifications, p_304053_ -> {
            this.notifications.clear();
            this.notifications.addAll(p_304053_);

            for(RealmsNotification realmsnotification : p_304053_) {
                if (realmsnotification instanceof RealmsNotification.InfoPopup realmsnotification$infopopup) {
                    PopupScreen popupscreen = realmsnotification$infopopup.buildScreen(this, this::dismissNotification);
                    if (popupscreen != null) {
                        this.minecraft.setScreen(popupscreen);
                        this.markNotificationsAsSeen(List.of(realmsnotification));
                        break;
                    }
                }
            }

            if (!this.notifications.isEmpty() && this.activeLayoutState != RealmsMainScreen.LayoutState.LOADING) {
                this.refreshListAndLayout();
            }
        });
        datafetcher$subscription.subscribe(pDataFetcher.pendingInvitesTask, p_300619_ -> {
            this.pendingInvitesButton.setNotificationCount(p_300619_);
            this.pendingInvitesButton.setTooltip(p_300619_ == 0 ? Tooltip.create(NO_PENDING_INVITES) : Tooltip.create(PENDING_INVITES));
            if (p_300619_ > 0 && this.inviteNarrationLimiter.tryAcquire(1)) {
                this.minecraft.getNarrator().sayNow(Component.translatable("mco.configure.world.invite.narration", p_300619_));
            }
        });
        datafetcher$subscription.subscribe(pDataFetcher.trialAvailabilityTask, p_293548_ -> this.trialsAvailable = p_293548_);
        datafetcher$subscription.subscribe(pDataFetcher.newsTask, p_300622_ -> {
            pDataFetcher.newsManager.updateUnreadNews(p_300622_);
            this.newsLink = pDataFetcher.newsManager.newsLink();
            this.newsButton.setNotificationCount(pDataFetcher.newsManager.hasUnreadNews() ? Integer.MAX_VALUE : 0);
        });
        return datafetcher$subscription;
    }

    private void markNotificationsAsSeen(Collection<RealmsNotification> pNotifications) {
        List<UUID> list = new ArrayList<>(pNotifications.size());

        for(RealmsNotification realmsnotification : pNotifications) {
            if (!realmsnotification.seen() && !this.handledSeenNotifications.contains(realmsnotification.uuid())) {
                list.add(realmsnotification.uuid());
            }
        }

        if (!list.isEmpty()) {
            callRealmsClient(p_274625_ -> {
                p_274625_.notificationsSeen(list);
                return null;
            }, p_274630_ -> this.handledSeenNotifications.addAll(list));
        }
    }

    private static <T> void callRealmsClient(RealmsMainScreen.RealmsCall<T> pCall, Consumer<T> pOnFinish) {
        Minecraft minecraft = Minecraft.getInstance();
        CompletableFuture.<T>supplyAsync(() -> {
            try {
                return pCall.request(RealmsClient.create(minecraft));
            } catch (RealmsServiceException realmsserviceexception) {
                throw new RuntimeException(realmsserviceexception);
            }
        }).thenAcceptAsync(pOnFinish, minecraft).exceptionally(p_274626_ -> {
            LOGGER.error("Failed to execute call to Realms Service", p_274626_);
            return null;
        });
    }

    private void refreshListAndLayout() {
        RealmsServer realmsserver = this.getSelectedServer();
        this.realmSelectionList.clear();

        for(RealmsNotification realmsnotification : this.notifications) {
            if (this.addListEntriesForNotification(realmsnotification)) {
                this.markNotificationsAsSeen(List.of(realmsnotification));
                break;
            }
        }

        for(RealmsServer realmsserver1 : this.availableSnapshotServers) {
            this.realmSelectionList.addEntry(new RealmsMainScreen.AvailableSnapshotEntry(realmsserver1));
        }

        for(RealmsServer realmsserver2 : this.serverList) {
            RealmsMainScreen.Entry realmsmainscreen$entry;
            if (isSnapshot() && !realmsserver2.isSnapshotRealm()) {
                if (realmsserver2.state == RealmsServer.State.UNINITIALIZED) {
                    continue;
                }

                realmsmainscreen$entry = new RealmsMainScreen.ParentEntry(realmsserver2);
            } else {
                realmsmainscreen$entry = new RealmsMainScreen.ServerEntry(realmsserver2);
            }

            this.realmSelectionList.addEntry(realmsmainscreen$entry);
            if (realmsserver != null && realmsserver.id == realmsserver2.id) {
                this.realmSelectionList.setSelected(realmsmainscreen$entry);
            }
        }

        this.updateLayout();
        this.updateButtonStates();
    }

    private boolean addListEntriesForNotification(RealmsNotification pNotification) {
        if (!(pNotification instanceof RealmsNotification.VisitUrl)) {
            return false;
        } else {
            RealmsNotification.VisitUrl realmsnotification$visiturl = (RealmsNotification.VisitUrl)pNotification;
            Component component = realmsnotification$visiturl.getMessage();
            int i = this.font.wordWrapHeight(component, 216);
            int j = Mth.positiveCeilDiv(i + 7, 36) - 1;
            this.realmSelectionList.addEntry(new RealmsMainScreen.NotificationMessageEntry(component, j + 2, realmsnotification$visiturl));

            for(int k = 0; k < j; ++k) {
                this.realmSelectionList.addEntry(new RealmsMainScreen.EmptyEntry());
            }

            this.realmSelectionList.addEntry(new RealmsMainScreen.ButtonEntry(realmsnotification$visiturl.buildOpenLinkButton(this)));
            return true;
        }
    }

    private void pingRegions() {
        new Thread(() -> {
            List<RegionPingResult> list = Ping.pingAllRegions();
            RealmsClient realmsclient = RealmsClient.create();
            PingResult pingresult = new PingResult();
            pingresult.pingResults = list;
            pingresult.worldIds = this.getOwnedNonExpiredWorldIds();

            try {
                realmsclient.sendPingResults(pingresult);
            } catch (Throwable throwable) {
                LOGGER.warn("Could not send ping result to Realms: ", throwable);
            }
        }).start();
    }

    private List<Long> getOwnedNonExpiredWorldIds() {
        List<Long> list = Lists.newArrayList();

        for(RealmsServer realmsserver : this.serverList) {
            if (this.isSelfOwnedNonExpiredServer(realmsserver)) {
                list.add(realmsserver.id);
            }
        }

        return list;
    }

    private void onRenew(@Nullable RealmsServer pRealmsServer) {
        if (pRealmsServer != null) {
            String s = CommonLinks.extendRealms(pRealmsServer.remoteSubscriptionId, this.minecraft.getUser().getProfileId(), pRealmsServer.expiredTrial);
            this.minecraft.keyboardHandler.setClipboard(s);
            Util.getPlatform().openUri(s);
        }
    }

    private void configureClicked(@Nullable RealmsServer pRealmsServer) {
        if (pRealmsServer != null && this.minecraft.isLocalPlayer(pRealmsServer.ownerUUID)) {
            this.minecraft.setScreen(new RealmsConfigureWorldScreen(this, pRealmsServer.id));
        }
    }

    private void leaveClicked(@Nullable RealmsServer pRealmsServer) {
        if (pRealmsServer != null && !this.minecraft.isLocalPlayer(pRealmsServer.ownerUUID)) {
            Component component = Component.translatable("mco.configure.world.leave.question.line1");
            Component component1 = Component.translatable("mco.configure.world.leave.question.line2");
            this.minecraft
                .setScreen(
                    new RealmsLongConfirmationScreen(
                        p_231253_ -> this.leaveServer(p_231253_, pRealmsServer), RealmsLongConfirmationScreen.Type.INFO, component, component1, true
                    )
                );
        }
    }

    @Nullable
    private RealmsServer getSelectedServer() {
        return this.realmSelectionList.getSelected() instanceof RealmsMainScreen.ServerEntry realmsmainscreen$serverentry
            ? realmsmainscreen$serverentry.getServer()
            : null;
    }

    private void leaveServer(boolean pConfirmed, final RealmsServer pServer) {
        if (pConfirmed) {
            (new Thread("Realms-leave-server") {
                    @Override
                    public void run() {
                        try {
                            RealmsClient realmsclient = RealmsClient.create();
                            realmsclient.uninviteMyselfFrom(pServer.id);
                            RealmsMainScreen.this.minecraft.execute(RealmsMainScreen::refreshServerList);
                        } catch (RealmsServiceException realmsserviceexception) {
                            RealmsMainScreen.LOGGER.error("Couldn't configure world", (Throwable)realmsserviceexception);
                            RealmsMainScreen.this.minecraft
                                .execute(
                                    () -> RealmsMainScreen.this.minecraft
                                            .setScreen(new RealmsGenericErrorScreen(realmsserviceexception, RealmsMainScreen.this))
                                );
                        }
                    }
                })
                .start();
        }

        this.minecraft.setScreen(this);
    }

    void dismissNotification(UUID p_275349_) {
        callRealmsClient(p_274628_ -> {
            p_274628_.notificationsDismiss(List.of(p_275349_));
            return null;
        }, p_305610_ -> {
            this.notifications.removeIf(p_274621_ -> p_274621_.dismissable() && p_275349_.equals(p_274621_.uuid()));
            this.refreshListAndLayout();
        });
    }

    public void resetScreen() {
        this.realmSelectionList.setSelected(null);
        refreshServerList();
    }

    @Override
    public Component getNarrationMessage() {
        return (Component)(switch(this.activeLayoutState) {
            case LOADING -> CommonComponents.joinForNarration(super.getNarrationMessage(), LOADING_TEXT);
            case NO_REALMS -> CommonComponents.joinForNarration(super.getNarrationMessage(), NO_REALMS_TEXT);
            case LIST -> super.getNarrationMessage();
        });
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
        if (isSnapshot()) {
            pGuiGraphics.drawString(this.font, "Minecraft " + SharedConstants.getCurrentVersion().getName(), 2, this.height - 10, -1);
        }

        if (this.trialsAvailable && this.addRealmButton.active) {
            RealmsPopupScreen.renderDiamond(pGuiGraphics, this.addRealmButton);
        }

        switch(RealmsClient.ENVIRONMENT) {
            case STAGE:
                this.renderEnvironment(pGuiGraphics, "STAGE!", -256);
                break;
            case LOCAL:
                this.renderEnvironment(pGuiGraphics, "LOCAL!", 8388479);
        }
    }

    private void openTrialAvailablePopup() {
        this.minecraft.setScreen(new RealmsPopupScreen(this, this.trialsAvailable));
    }

    public static void play(@Nullable RealmsServer pRealmsServer, Screen pLastScreen) {
        play(pRealmsServer, pLastScreen, false);
    }

    public static void play(@Nullable RealmsServer pRealmsServer, Screen pLastScreen, boolean pAllowSnapshots) {
        if (pRealmsServer != null) {
            if (!isSnapshot() || pAllowSnapshots) {
                Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new GetServerDetailsTask(pLastScreen, pRealmsServer)));
                return;
            }

            switch(pRealmsServer.compatibility) {
                case COMPATIBLE:
                    Minecraft.getInstance().setScreen(new RealmsLongRunningMcoTaskScreen(pLastScreen, new GetServerDetailsTask(pLastScreen, pRealmsServer)));
                    break;
                case UNVERIFIABLE:
                    confirmToPlay(
                        pRealmsServer,
                        pLastScreen,
                        Component.translatable("mco.compatibility.unverifiable.title").withColor(-171),
                        Component.translatable("mco.compatibility.unverifiable.message"),
                        CommonComponents.GUI_CONTINUE
                    );
                    break;
                case NEEDS_DOWNGRADE:
                    confirmToPlay(
                        pRealmsServer,
                        pLastScreen,
                        Component.translatable("selectWorld.backupQuestion.downgrade").withColor(-2142128),
                        Component.translatable(
                            "mco.compatibility.downgrade.description",
                            Component.literal(pRealmsServer.activeVersion).withColor(-171),
                            Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171)
                        ),
                        Component.translatable("mco.compatibility.downgrade")
                    );
                    break;
                case NEEDS_UPGRADE:
                    confirmToPlay(
                        pRealmsServer,
                        pLastScreen,
                        Component.translatable("mco.compatibility.upgrade.title").withColor(-171),
                        Component.translatable(
                            "mco.compatibility.upgrade.description",
                            Component.literal(pRealmsServer.activeVersion).withColor(-171),
                            Component.literal(SharedConstants.getCurrentVersion().getName()).withColor(-171)
                        ),
                        Component.translatable("mco.compatibility.upgrade")
                    );
            }
        }
    }

    private static void confirmToPlay(RealmsServer pRealmsServer, Screen pLastScreen, Component pTitle, Component pMessage, Component pConfirmButton) {
        Minecraft.getInstance().setScreen(new ConfirmScreen(p_305615_ -> {
            Screen screen;
            if (p_305615_) {
                screen = new RealmsLongRunningMcoTaskScreen(pLastScreen, new GetServerDetailsTask(pLastScreen, pRealmsServer));
                refreshServerList();
            } else {
                screen = pLastScreen;
            }

            Minecraft.getInstance().setScreen(screen);
        }, pTitle, pMessage, pConfirmButton, CommonComponents.GUI_CANCEL));
    }

    public static Component getVersionComponent(String pVersion, boolean pCompatible) {
        return getVersionComponent(pVersion, pCompatible ? -8355712 : -2142128);
    }

    public static Component getVersionComponent(String pVersion, int pColor) {
        return (Component)(StringUtils.isBlank(pVersion)
            ? CommonComponents.EMPTY
            : Component.translatable("mco.version", Component.literal(pVersion).withColor(pColor)));
    }

    boolean isSelfOwnedServer(RealmsServer pServer) {
        return this.minecraft.isLocalPlayer(pServer.ownerUUID);
    }

    private boolean isSelfOwnedNonExpiredServer(RealmsServer pServer) {
        return this.isSelfOwnedServer(pServer) && !pServer.expired;
    }

    private void renderEnvironment(GuiGraphics pGuiGraphics, String pText, int pColor) {
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate((float)(this.width / 2 - 25), 20.0F, 0.0F);
        pGuiGraphics.pose().mulPose(Axis.ZP.rotationDegrees(-20.0F));
        pGuiGraphics.pose().scale(1.5F, 1.5F, 1.5F);
        pGuiGraphics.drawString(this.font, pText, 0, 0, pColor, false);
        pGuiGraphics.pose().popPose();
    }

    @OnlyIn(Dist.CLIENT)
    class AvailableSnapshotEntry extends RealmsMainScreen.Entry {
        private static final Component START_SNAPSHOT_REALM = Component.translatable("mco.snapshot.start");
        private static final int TEXT_PADDING = 5;
        private final Tooltip tooltip;
        private final RealmsServer parent;

        public AvailableSnapshotEntry(RealmsServer pParent) {
            this.parent = pParent;
            this.tooltip = Tooltip.create(Component.translatable("mco.snapshot.tooltip"));
        }

        @Override
        public void render(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pHovering,
            float pPartialTick
        ) {
            pGuiGraphics.blitSprite(RealmsMainScreen.NEW_REALM_SPRITE, pLeft - 5, pTop + pHeight / 2 - 10, 40, 20);
            int i = pTop + pHeight / 2 - 9 / 2;
            pGuiGraphics.drawString(RealmsMainScreen.this.font, START_SNAPSHOT_REALM, pLeft + 40 - 2, i - 5, 8388479);
            pGuiGraphics.drawString(
                RealmsMainScreen.this.font, Component.translatable("mco.snapshot.description", this.parent.name), pLeft + 40 - 2, i + 5, -8355712
            );
            this.tooltip.refreshTooltipForNextRenderPass(pHovering, this.isFocused(), new ScreenRectangle(pLeft, pTop, pWidth, pHeight));
        }

        /**
         * Called when a mouse button is clicked within the GUI element.
         * <p>
         * @return {@code true} if the event is consumed, {@code false} otherwise.
         *
         * @param pMouseX the X coordinate of the mouse.
         * @param pMouseY the Y coordinate of the mouse.
         * @param pButton the button that was clicked.
         */
        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            this.addSnapshotRealm();
            return true;
        }

        /**
         * Called when a keyboard key is pressed within the GUI element.
         * <p>
         * @return {@code true} if the event is consumed, {@code false} otherwise.
         *
         * @param pKeyCode   the key code of the pressed key.
         * @param pScanCode  the scan code of the pressed key.
         * @param pModifiers the keyboard modifiers.
         */
        @Override
        public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
            if (CommonInputs.selected(pKeyCode)) {
                this.addSnapshotRealm();
                return true;
            } else {
                return super.keyPressed(pKeyCode, pScanCode, pModifiers);
            }
        }

        private void addSnapshotRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RealmsMainScreen.this.minecraft
                .setScreen(
                    new PopupScreen.Builder(RealmsMainScreen.this, Component.translatable("mco.snapshot.createSnapshotPopup.title"))
                        .setMessage(Component.translatable("mco.snapshot.createSnapshotPopup.text"))
                        .addButton(
                            Component.translatable("mco.selectServer.create"),
                            p_307016_ -> RealmsMainScreen.this.minecraft.setScreen(new RealmsCreateRealmScreen(RealmsMainScreen.this, this.parent.id))
                        )
                        .addButton(CommonComponents.GUI_CANCEL, PopupScreen::onClose)
                        .build()
                );
        }

        @Override
        public Component getNarration() {
            return Component.translatable(
                "gui.narrate.button",
                CommonComponents.joinForNarration(START_SNAPSHOT_REALM, Component.translatable("mco.snapshot.description", this.parent.name))
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    class ButtonEntry extends RealmsMainScreen.Entry {
        private final Button button;

        public ButtonEntry(Button pButton) {
            this.button = pButton;
        }

        /**
         * Called when a mouse button is clicked within the GUI element.
         * <p>
         * @return {@code true} if the event is consumed, {@code false} otherwise.
         *
         * @param pMouseX the X coordinate of the mouse.
         * @param pMouseY the Y coordinate of the mouse.
         * @param pButton the button that was clicked.
         */
        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            this.button.mouseClicked(pMouseX, pMouseY, pButton);
            return true;
        }

        /**
         * Called when a keyboard key is pressed within the GUI element.
         * <p>
         * @return {@code true} if the event is consumed, {@code false} otherwise.
         *
         * @param pKeyCode   the key code of the pressed key.
         * @param pScanCode  the scan code of the pressed key.
         * @param pModifiers the keyboard modifiers.
         */
        @Override
        public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
            return this.button.keyPressed(pKeyCode, pScanCode, pModifiers) ? true : super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }

        @Override
        public void render(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pHovering,
            float pPartialTick
        ) {
            this.button.setPosition(RealmsMainScreen.this.width / 2 - 75, pTop + 4);
            this.button.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        }

        /**
         * Sets the focus state of the GUI element.
         *
         * @param pFocused {@code true} to apply focus, {@code false} to remove focus
         */
        @Override
        public void setFocused(boolean pFocused) {
            super.setFocused(pFocused);
            this.button.setFocused(pFocused);
        }

        @Override
        public Component getNarration() {
            return this.button.getMessage();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class CrossButton extends ImageButton {
        private static final WidgetSprites SPRITES = new WidgetSprites(
            new ResourceLocation("widget/cross_button"), new ResourceLocation("widget/cross_button_highlighted")
        );

        protected CrossButton(Button.OnPress pOnPress, Component pMessage) {
            super(0, 0, 14, 14, SPRITES, pOnPress);
            this.setTooltip(Tooltip.create(pMessage));
        }
    }

    @OnlyIn(Dist.CLIENT)
    class EmptyEntry extends RealmsMainScreen.Entry {
        @Override
        public void render(
            GuiGraphics p_302489_,
            int p_302486_,
            int p_302498_,
            int p_302485_,
            int p_302492_,
            int p_302487_,
            int p_302488_,
            int p_302496_,
            boolean p_302491_,
            float p_302497_
        ) {
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }
    }

    @OnlyIn(Dist.CLIENT)
    abstract class Entry extends ObjectSelectionList.Entry<RealmsMainScreen.Entry> {
        private static final int STATUS_LIGHT_WIDTH = 10;
        private static final int STATUS_LIGHT_HEIGHT = 28;
        private static final int PADDING = 7;

        protected void renderStatusLights(RealmsServer pRealmsServer, GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY) {
            int i = pX - 10 - 7;
            int j = pY + 2;
            if (pRealmsServer.expired) {
                this.drawRealmStatus(pGuiGraphics, i, j, pMouseX, pMouseY, RealmsMainScreen.EXPIRED_SPRITE, () -> RealmsMainScreen.SERVER_EXPIRED_TOOLTIP);
            } else if (pRealmsServer.state == RealmsServer.State.CLOSED) {
                this.drawRealmStatus(pGuiGraphics, i, j, pMouseX, pMouseY, RealmsMainScreen.CLOSED_SPRITE, () -> RealmsMainScreen.SERVER_CLOSED_TOOLTIP);
            } else if (RealmsMainScreen.this.isSelfOwnedServer(pRealmsServer) && pRealmsServer.daysLeft < 7) {
                this.drawRealmStatus(
                    pGuiGraphics,
                    i,
                    j,
                    pMouseX,
                    pMouseY,
                    RealmsMainScreen.EXPIRES_SOON_SPRITE,
                    () -> {
                        if (pRealmsServer.daysLeft <= 0) {
                            return RealmsMainScreen.SERVER_EXPIRES_SOON_TOOLTIP;
                        } else {
                            return (Component)(pRealmsServer.daysLeft == 1
                                ? RealmsMainScreen.SERVER_EXPIRES_IN_DAY_TOOLTIP
                                : Component.translatable("mco.selectServer.expires.days", pRealmsServer.daysLeft));
                        }
                    }
                );
            } else if (pRealmsServer.state == RealmsServer.State.OPEN) {
                this.drawRealmStatus(pGuiGraphics, i, j, pMouseX, pMouseY, RealmsMainScreen.OPEN_SPRITE, () -> RealmsMainScreen.SERVER_OPEN_TOOLTIP);
            }
        }

        private void drawRealmStatus(
            GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY, ResourceLocation pSpriteLocation, Supplier<Component> pTooltipSupplier
        ) {
            pGuiGraphics.blitSprite(pSpriteLocation, pX, pY, 10, 28);
            if (RealmsMainScreen.this.realmSelectionList.isMouseOver((double)pMouseX, (double)pMouseY)
                && pMouseX >= pX
                && pMouseX <= pX + 10
                && pMouseY >= pY
                && pMouseY <= pY + 28) {
                RealmsMainScreen.this.setTooltipForNextRenderPass(pTooltipSupplier.get());
            }
        }

        protected void renderThirdLine(GuiGraphics pGuiGraphics, int pTop, int pLeft, RealmsServer pServer) {
            int i = this.textX(pLeft);
            int j = this.firstLineY(pTop);
            int k = this.thirdLineY(j);
            if (!RealmsMainScreen.this.isSelfOwnedServer(pServer)) {
                pGuiGraphics.drawString(RealmsMainScreen.this.font, pServer.owner, i, this.thirdLineY(j), -8355712, false);
            } else if (pServer.expired) {
                Component component = pServer.expiredTrial ? RealmsMainScreen.TRIAL_EXPIRED_TEXT : RealmsMainScreen.SUBSCRIPTION_EXPIRED_TEXT;
                pGuiGraphics.drawString(RealmsMainScreen.this.font, component, i, k, -2142128, false);
            }
        }

        protected void renderClampedName(GuiGraphics pGuiGraphics, String pName, int pX, int pY, int pVersionTextX, int pColor) {
            int i = pVersionTextX - pX;
            if (RealmsMainScreen.this.font.width(pName) > i) {
                String s = RealmsMainScreen.this.font.plainSubstrByWidth(pName, i - RealmsMainScreen.this.font.width("... "));
                pGuiGraphics.drawString(RealmsMainScreen.this.font, s + "...", pX, pY, pColor, false);
            } else {
                pGuiGraphics.drawString(RealmsMainScreen.this.font, pName, pX, pY, pColor, false);
            }
        }

        protected int versionTextX(int pLeft, int pWidth, Component pVersionComponent) {
            return pLeft + pWidth - RealmsMainScreen.this.font.width(pVersionComponent) - 20;
        }

        protected int firstLineY(int pTop) {
            return pTop + 1;
        }

        protected int lineHeight() {
            return 2 + 9;
        }

        protected int textX(int pLeft) {
            return pLeft + 36 + 2;
        }

        protected int secondLineY(int pFirstLineY) {
            return pFirstLineY + this.lineHeight();
        }

        protected int thirdLineY(int pFirstLineY) {
            return pFirstLineY + this.lineHeight() * 2;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum LayoutState {
        LOADING,
        NO_REALMS,
        LIST;
    }

    @OnlyIn(Dist.CLIENT)
    static class NotificationButton extends SpriteIconButton.CenteredIcon {
        private static final ResourceLocation[] NOTIFICATION_ICONS = new ResourceLocation[]{
            new ResourceLocation("notification/1"),
            new ResourceLocation("notification/2"),
            new ResourceLocation("notification/3"),
            new ResourceLocation("notification/4"),
            new ResourceLocation("notification/5"),
            new ResourceLocation("notification/more")
        };
        private static final int UNKNOWN_COUNT = Integer.MAX_VALUE;
        private static final int SIZE = 20;
        private static final int SPRITE_SIZE = 14;
        private int notificationCount;

        public NotificationButton(Component pMessage, ResourceLocation pSprite, Button.OnPress pOnPress) {
            super(20, 20, pMessage, 14, 14, pSprite, pOnPress);
        }

        int notificationCount() {
            return this.notificationCount;
        }

        public void setNotificationCount(int pNotificationCount) {
            this.notificationCount = pNotificationCount;
        }

        @Override
        public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            if (this.active && this.notificationCount != 0) {
                this.drawNotificationCounter(pGuiGraphics);
            }
        }

        private void drawNotificationCounter(GuiGraphics pGuiGraphics) {
            pGuiGraphics.blitSprite(NOTIFICATION_ICONS[Math.min(this.notificationCount, 6) - 1], this.getX() + this.getWidth() - 5, this.getY() - 3, 8, 8);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class NotificationMessageEntry extends RealmsMainScreen.Entry {
        private static final int SIDE_MARGINS = 40;
        private static final int OUTLINE_COLOR = -12303292;
        private final Component text;
        private final int frameItemHeight;
        private final List<AbstractWidget> children = new ArrayList<>();
        @Nullable
        private final RealmsMainScreen.CrossButton dismissButton;
        private final MultiLineTextWidget textWidget;
        private final GridLayout gridLayout;
        private final FrameLayout textFrame;
        private int lastEntryWidth = -1;

        public NotificationMessageEntry(Component pText, int pFrameItemHeight, RealmsNotification pNotification) {
            this.text = pText;
            this.frameItemHeight = pFrameItemHeight;
            this.gridLayout = new GridLayout();
            int i = 7;
            this.gridLayout.addChild(ImageWidget.sprite(20, 20, RealmsMainScreen.INFO_SPRITE), 0, 0, this.gridLayout.newCellSettings().padding(7, 7, 0, 0));
            this.gridLayout.addChild(SpacerElement.width(40), 0, 0);
            this.textFrame = this.gridLayout.addChild(new FrameLayout(0, 9 * 3 * (pFrameItemHeight - 1)), 0, 1, this.gridLayout.newCellSettings().paddingTop(7));
            this.textWidget = this.textFrame
                .addChild(
                    new MultiLineTextWidget(pText, RealmsMainScreen.this.font).setCentered(true),
                    this.textFrame.newChildLayoutSettings().alignHorizontallyCenter().alignVerticallyTop()
                );
            this.gridLayout.addChild(SpacerElement.width(40), 0, 2);
            if (pNotification.dismissable()) {
                this.dismissButton = this.gridLayout
                    .addChild(
                        new RealmsMainScreen.CrossButton(
                            p_275478_ -> RealmsMainScreen.this.dismissNotification(pNotification.uuid()), Component.translatable("mco.notification.dismiss")
                        ),
                        0,
                        2,
                        this.gridLayout.newCellSettings().alignHorizontallyRight().padding(0, 7, 7, 0)
                    );
            } else {
                this.dismissButton = null;
            }

            this.gridLayout.visitWidgets(this.children::add);
        }

        /**
         * Called when a keyboard key is pressed within the GUI element.
         * <p>
         * @return {@code true} if the event is consumed, {@code false} otherwise.
         *
         * @param pKeyCode   the key code of the pressed key.
         * @param pScanCode  the scan code of the pressed key.
         * @param pModifiers the keyboard modifiers.
         */
        @Override
        public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
            return this.dismissButton != null && this.dismissButton.keyPressed(pKeyCode, pScanCode, pModifiers)
                ? true
                : super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }

        private void updateEntryWidth(int pEntryWidth) {
            if (this.lastEntryWidth != pEntryWidth) {
                this.refreshLayout(pEntryWidth);
                this.lastEntryWidth = pEntryWidth;
            }
        }

        private void refreshLayout(int pWidth) {
            int i = pWidth - 80;
            this.textFrame.setMinWidth(i);
            this.textWidget.setMaxWidth(i);
            this.gridLayout.arrangeElements();
        }

        @Override
        public void renderBack(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pIsMouseOver,
            float pPartialTick
        ) {
            super.renderBack(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, pIsMouseOver, pPartialTick);
            pGuiGraphics.renderOutline(pLeft - 2, pTop - 2, pWidth, 36 * this.frameItemHeight - 2, -12303292);
        }

        @Override
        public void render(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pHovering,
            float pPartialTick
        ) {
            this.gridLayout.setPosition(pLeft, pTop);
            this.updateEntryWidth(pWidth - 4);
            this.children.forEach(p_280688_ -> p_280688_.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick));
        }

        /**
         * Called when a mouse button is clicked within the GUI element.
         * <p>
         * @return {@code true} if the event is consumed, {@code false} otherwise.
         *
         * @param pMouseX the X coordinate of the mouse.
         * @param pMouseY the Y coordinate of the mouse.
         * @param pButton the button that was clicked.
         */
        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (this.dismissButton != null) {
                this.dismissButton.mouseClicked(pMouseX, pMouseY, pButton);
            }

            return true;
        }

        @Override
        public Component getNarration() {
            return this.text;
        }
    }

    @OnlyIn(Dist.CLIENT)
    class ParentEntry extends RealmsMainScreen.Entry {
        private final RealmsServer server;
        private final Tooltip tooltip;

        public ParentEntry(RealmsServer pServer) {
            this.server = pServer;
            this.tooltip = Tooltip.create(Component.translatable("mco.snapshot.parent.tooltip"));
        }

        /**
         * Called when a mouse button is clicked within the GUI element.
         * <p>
         * @return {@code true} if the event is consumed, {@code false} otherwise.
         *
         * @param pMouseX the X coordinate of the mouse.
         * @param pMouseY the Y coordinate of the mouse.
         * @param pButton the button that was clicked.
         */
        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            return true;
        }

        @Override
        public void render(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pHovering,
            float pPartialTick
        ) {
            int i = this.textX(pLeft);
            int j = this.firstLineY(pTop);
            RealmsUtil.renderPlayerFace(pGuiGraphics, pLeft, pTop, 32, this.server.ownerUUID);
            Component component = RealmsMainScreen.getVersionComponent(this.server.activeVersion, -8355712);
            int k = this.versionTextX(pLeft, pWidth, component);
            this.renderClampedName(pGuiGraphics, this.server.getName(), i, j, k, -8355712);
            if (component != CommonComponents.EMPTY) {
                pGuiGraphics.drawString(RealmsMainScreen.this.font, component, k, j, -8355712, false);
            }

            pGuiGraphics.drawString(RealmsMainScreen.this.font, this.server.getDescription(), i, this.secondLineY(j), -8355712, false);
            this.renderThirdLine(pGuiGraphics, pTop, pLeft, this.server);
            this.renderStatusLights(this.server, pGuiGraphics, pLeft + pWidth, pTop, pMouseX, pMouseY);
            this.tooltip.refreshTooltipForNextRenderPass(pHovering, this.isFocused(), new ScreenRectangle(pLeft, pTop, pWidth, pHeight));
        }

        @Override
        public Component getNarration() {
            return Component.literal(this.server.name);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class RealmSelectionList extends RealmsObjectSelectionList<RealmsMainScreen.Entry> {
        public RealmSelectionList() {
            super(RealmsMainScreen.this.width, RealmsMainScreen.this.height, 0, 36);
        }

        public void setSelected(@Nullable RealmsMainScreen.Entry p_86849_) {
            super.setSelected(p_86849_);
            RealmsMainScreen.this.updateButtonStates();
        }

        @Override
        public int getMaxPosition() {
            return this.getItemCount() * 36;
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    @OnlyIn(Dist.CLIENT)
    interface RealmsCall<T> {
        T request(RealmsClient pRealmsClient) throws RealmsServiceException;
    }

    @OnlyIn(Dist.CLIENT)
    class ServerEntry extends RealmsMainScreen.Entry {
        private static final int SKIN_HEAD_LARGE_WIDTH = 36;
        private final RealmsServer serverData;
        @Nullable
        private final Tooltip tooltip;

        public ServerEntry(RealmsServer pServerData) {
            this.serverData = pServerData;
            boolean flag = RealmsMainScreen.this.isSelfOwnedServer(pServerData);
            if (RealmsMainScreen.isSnapshot() && flag && pServerData.isSnapshotRealm()) {
                this.tooltip = Tooltip.create(Component.translatable("mco.snapshot.paired", pServerData.parentWorldName));
            } else if (!flag && pServerData.needsUpgrade()) {
                this.tooltip = Tooltip.create(Component.translatable("mco.snapshot.friendsRealm.upgrade", pServerData.owner));
            } else if (!flag && pServerData.needsDowngrade()) {
                this.tooltip = Tooltip.create(Component.translatable("mco.snapshot.friendsRealm.downgrade", pServerData.activeVersion));
            } else {
                this.tooltip = null;
            }
        }

        @Override
        public void render(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pHovering,
            float pPartialTick
        ) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                pGuiGraphics.blitSprite(RealmsMainScreen.NEW_REALM_SPRITE, pLeft - 5, pTop + pHeight / 2 - 10, 40, 20);
                int i = pTop + pHeight / 2 - 9 / 2;
                pGuiGraphics.drawString(RealmsMainScreen.this.font, RealmsMainScreen.SERVER_UNITIALIZED_TEXT, pLeft + 40 - 2, i, 8388479);
            } else {
                RealmsUtil.renderPlayerFace(pGuiGraphics, pLeft, pTop, 32, this.serverData.ownerUUID);
                this.renderFirstLine(pGuiGraphics, pTop, pLeft, pWidth);
                this.renderSecondLine(pGuiGraphics, pTop, pLeft);
                this.renderThirdLine(pGuiGraphics, pTop, pLeft, this.serverData);
                this.renderStatusLights(this.serverData, pGuiGraphics, pLeft + pWidth, pTop, pMouseX, pMouseY);
                if (this.tooltip != null) {
                    this.tooltip.refreshTooltipForNextRenderPass(pHovering, this.isFocused(), new ScreenRectangle(pLeft, pTop, pWidth, pHeight));
                }
            }
        }

        private void renderFirstLine(GuiGraphics pGuiGraphics, int pTop, int pLeft, int pWidth) {
            int i = this.textX(pLeft);
            int j = this.firstLineY(pTop);
            Component component = RealmsMainScreen.getVersionComponent(this.serverData.activeVersion, this.serverData.isCompatible());
            int k = this.versionTextX(pLeft, pWidth, component);
            this.renderClampedName(pGuiGraphics, this.serverData.getName(), i, j, k, -1);
            if (component != CommonComponents.EMPTY) {
                pGuiGraphics.drawString(RealmsMainScreen.this.font, component, k, j, -8355712, false);
            }
        }

        private void renderSecondLine(GuiGraphics pGuiGraphics, int pTop, int pLeft) {
            int i = this.textX(pLeft);
            int j = this.firstLineY(pTop);
            int k = this.secondLineY(j);
            if (this.serverData.worldType == RealmsServer.WorldType.MINIGAME) {
                Component component = Component.literal(this.serverData.getMinigameName()).withStyle(ChatFormatting.GRAY);
                pGuiGraphics.drawString(
                    RealmsMainScreen.this.font, Component.translatable("mco.selectServer.minigameName", component).withColor(-171), i, k, -1, false
                );
            } else {
                pGuiGraphics.drawString(RealmsMainScreen.this.font, this.serverData.getDescription(), i, this.secondLineY(j), -8355712, false);
            }
        }

        private void playRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RealmsMainScreen.play(this.serverData, RealmsMainScreen.this);
        }

        private void createUnitializedRealm() {
            RealmsMainScreen.this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            RealmsCreateRealmScreen realmscreaterealmscreen = new RealmsCreateRealmScreen(RealmsMainScreen.this, this.serverData);
            RealmsMainScreen.this.minecraft.setScreen(realmscreaterealmscreen);
        }

        /**
         * Called when a mouse button is clicked within the GUI element.
         * <p>
         * @return {@code true} if the event is consumed, {@code false} otherwise.
         *
         * @param pMouseX the X coordinate of the mouse.
         * @param pMouseY the Y coordinate of the mouse.
         * @param pButton the button that was clicked.
         */
        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                this.createUnitializedRealm();
            } else if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
                if (Util.getMillis() - RealmsMainScreen.this.lastClickTime < 250L && this.isFocused()) {
                    this.playRealm();
                }

                RealmsMainScreen.this.lastClickTime = Util.getMillis();
            }

            return true;
        }

        /**
         * Called when a keyboard key is pressed within the GUI element.
         * <p>
         * @return {@code true} if the event is consumed, {@code false} otherwise.
         *
         * @param pKeyCode   the key code of the pressed key.
         * @param pScanCode  the scan code of the pressed key.
         * @param pModifiers the keyboard modifiers.
         */
        @Override
        public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
            if (CommonInputs.selected(pKeyCode)) {
                if (this.serverData.state == RealmsServer.State.UNINITIALIZED) {
                    this.createUnitializedRealm();
                    return true;
                }

                if (RealmsMainScreen.this.shouldPlayButtonBeActive(this.serverData)) {
                    this.playRealm();
                    return true;
                }
            }

            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }

        @Override
        public Component getNarration() {
            return (Component)(this.serverData.state == RealmsServer.State.UNINITIALIZED
                ? RealmsMainScreen.UNITIALIZED_WORLD_NARRATION
                : Component.translatable("narrator.select", this.serverData.name));
        }

        public RealmsServer getServer() {
            return this.serverData;
        }
    }
}
