package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ErrorScreen;
import net.minecraft.client.gui.screens.FaviconTexture;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.LoadingDotsText;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WorldSelectionList extends ObjectSelectionList<WorldSelectionList.Entry> {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault());
    static final ResourceLocation ERROR_HIGHLIGHTED_SPRITE = new ResourceLocation("world_list/error_highlighted");
    static final ResourceLocation ERROR_SPRITE = new ResourceLocation("world_list/error");
    static final ResourceLocation MARKED_JOIN_HIGHLIGHTED_SPRITE = new ResourceLocation("world_list/marked_join_highlighted");
    static final ResourceLocation MARKED_JOIN_SPRITE = new ResourceLocation("world_list/marked_join");
    static final ResourceLocation WARNING_HIGHLIGHTED_SPRITE = new ResourceLocation("world_list/warning_highlighted");
    static final ResourceLocation WARNING_SPRITE = new ResourceLocation("world_list/warning");
    static final ResourceLocation JOIN_HIGHLIGHTED_SPRITE = new ResourceLocation("world_list/join_highlighted");
    static final ResourceLocation JOIN_SPRITE = new ResourceLocation("world_list/join");
    static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation ICON_MISSING = new ResourceLocation("textures/misc/unknown_server.png");
    private static final ResourceLocation FORGE_EXPERIMENTAL_WARNING_ICON = new ResourceLocation("neoforge","textures/gui/experimental_warning.png");
    static final Component FROM_NEWER_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.fromNewerVersion1").withStyle(ChatFormatting.RED);
    static final Component FROM_NEWER_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.fromNewerVersion2").withStyle(ChatFormatting.RED);
    static final Component SNAPSHOT_TOOLTIP_1 = Component.translatable("selectWorld.tooltip.snapshot1").withStyle(ChatFormatting.GOLD);
    static final Component SNAPSHOT_TOOLTIP_2 = Component.translatable("selectWorld.tooltip.snapshot2").withStyle(ChatFormatting.GOLD);
    static final Component WORLD_LOCKED_TOOLTIP = Component.translatable("selectWorld.locked").withStyle(ChatFormatting.RED);
    static final Component WORLD_REQUIRES_CONVERSION = Component.translatable("selectWorld.conversion.tooltip").withStyle(ChatFormatting.RED);
    static final Component INCOMPATIBLE_VERSION_TOOLTIP = Component.translatable("selectWorld.incompatible.tooltip").withStyle(ChatFormatting.RED);
    static final Component WORLD_EXPERIMENTAL = Component.translatable("selectWorld.experimental");
    private final SelectWorldScreen screen;
    private CompletableFuture<List<LevelSummary>> pendingLevels;
    @Nullable
    private List<LevelSummary> currentlyDisplayedLevels;
    private String filter;
    private final WorldSelectionList.LoadingHeader loadingHeader;

    public WorldSelectionList(
        SelectWorldScreen pScreen,
        Minecraft pMinecraft,
        int pWidth,
        int pHeight,
        int pY,
        int pItemHeight,
        String pFilter,
        @Nullable WorldSelectionList pWorlds
    ) {
        super(pMinecraft, pWidth, pHeight, pY, pItemHeight);
        this.screen = pScreen;
        this.loadingHeader = new WorldSelectionList.LoadingHeader(pMinecraft);
        this.filter = pFilter;
        if (pWorlds != null) {
            this.pendingLevels = pWorlds.pendingLevels;
        } else {
            this.pendingLevels = this.loadLevels();
        }

        this.handleNewLevels(this.pollLevelsIgnoreErrors());
    }

    @Override
    protected void clearEntries() {
        this.children().forEach(WorldSelectionList.Entry::close);
        super.clearEntries();
    }

    @Nullable
    private List<LevelSummary> pollLevelsIgnoreErrors() {
        try {
            return this.pendingLevels.getNow(null);
        } catch (CancellationException | CompletionException completionexception) {
            return null;
        }
    }

    void reloadWorldList() {
        this.pendingLevels = this.loadLevels();
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
            Optional<WorldSelectionList.WorldListEntry> optional = this.getSelectedOpt();
            if (optional.isPresent()) {
                if (optional.get().canJoin()) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    optional.get().joinWorld();
                }

                return true;
            }
        }

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        List<LevelSummary> list = this.pollLevelsIgnoreErrors();
        if (list != this.currentlyDisplayedLevels) {
            this.handleNewLevels(list);
        }

        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    private void handleNewLevels(@Nullable List<LevelSummary> pLevels) {
        if (pLevels == null) {
            this.fillLoadingLevels();
        } else {
            this.fillLevels(this.filter, pLevels);
        }

        this.currentlyDisplayedLevels = pLevels;
    }

    public void updateFilter(String pFilter) {
        if (this.currentlyDisplayedLevels != null && !pFilter.equals(this.filter)) {
            this.fillLevels(pFilter, this.currentlyDisplayedLevels);
        }

        this.filter = pFilter;
    }

    private CompletableFuture<List<LevelSummary>> loadLevels() {
        LevelStorageSource.LevelCandidates levelstoragesource$levelcandidates;
        try {
            levelstoragesource$levelcandidates = this.minecraft.getLevelSource().findLevelCandidates();
        } catch (LevelStorageException levelstorageexception) {
            LOGGER.error("Couldn't load level list", (Throwable)levelstorageexception);
            this.handleLevelLoadFailure(levelstorageexception.getMessageComponent());
            return CompletableFuture.completedFuture(List.of());
        }

        if (levelstoragesource$levelcandidates.isEmpty()) {
            CreateWorldScreen.openFresh(this.minecraft, null);
            return CompletableFuture.completedFuture(List.of());
        } else {
            return this.minecraft.getLevelSource().loadLevelSummaries(levelstoragesource$levelcandidates).exceptionally(p_233202_ -> {
                this.minecraft.delayCrash(CrashReport.forThrowable(p_233202_, "Couldn't load level list"));
                return List.of();
            });
        }
    }

    private void fillLevels(String pFilter, List<LevelSummary> pLevels) {
        this.clearEntries();
        pFilter = pFilter.toLowerCase(Locale.ROOT);

        for(LevelSummary levelsummary : pLevels) {
            if (this.filterAccepts(pFilter, levelsummary)) {
                this.addEntry(new WorldSelectionList.WorldListEntry(this, levelsummary));
            }
        }

        this.notifyListUpdated();
    }

    private boolean filterAccepts(String pFilter, LevelSummary pLevel) {
        return pLevel.getLevelName().toLowerCase(Locale.ROOT).contains(pFilter) || pLevel.getLevelId().toLowerCase(Locale.ROOT).contains(pFilter);
    }

    private void fillLoadingLevels() {
        this.clearEntries();
        this.addEntry(this.loadingHeader);
        this.notifyListUpdated();
    }

    private void notifyListUpdated() {
        this.setScrollAmount(this.getScrollAmount());
        this.screen.triggerImmediateNarration(true);
    }

    private void handleLevelLoadFailure(Component pExceptionMessage) {
        this.minecraft.setScreen(new ErrorScreen(Component.translatable("selectWorld.unable_to_load"), pExceptionMessage));
    }

    @Override
    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() + 20;
    }

    @Override
    public int getRowWidth() {
        return super.getRowWidth() + 50;
    }

    public void setSelected(@Nullable WorldSelectionList.Entry pSelected) {
        super.setSelected(pSelected);
        this.screen
            .updateButtonStatus(
                pSelected instanceof WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry ? worldselectionlist$worldlistentry.summary : null
            );
    }

    public Optional<WorldSelectionList.WorldListEntry> getSelectedOpt() {
        WorldSelectionList.Entry worldselectionlist$entry = this.getSelected();
        return worldselectionlist$entry instanceof WorldSelectionList.WorldListEntry worldselectionlist$worldlistentry
            ? Optional.of(worldselectionlist$worldlistentry)
            : Optional.empty();
    }

    public SelectWorldScreen getScreen() {
        return this.screen;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        if (this.children().contains(this.loadingHeader)) {
            this.loadingHeader.updateNarration(pNarrationElementOutput);
        } else {
            super.updateWidgetNarration(pNarrationElementOutput);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry extends ObjectSelectionList.Entry<WorldSelectionList.Entry> implements AutoCloseable {
        @Override
        public void close() {
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class LoadingHeader extends WorldSelectionList.Entry {
        private static final Component LOADING_LABEL = Component.translatable("selectWorld.loading_list");
        private final Minecraft minecraft;

        public LoadingHeader(Minecraft pMinecraft) {
            this.minecraft = pMinecraft;
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
            int i = (this.minecraft.screen.width - this.minecraft.font.width(LOADING_LABEL)) / 2;
            int j = pTop + (pHeight - 9) / 2;
            pGuiGraphics.drawString(this.minecraft.font, LOADING_LABEL, i, j, 16777215, false);
            String s = LoadingDotsText.get(Util.getMillis());
            int k = (this.minecraft.screen.width - this.minecraft.font.width(s)) / 2;
            int l = j + 9;
            pGuiGraphics.drawString(this.minecraft.font, s, k, l, -8355712, false);
        }

        @Override
        public Component getNarration() {
            return LOADING_LABEL;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public final class WorldListEntry extends WorldSelectionList.Entry implements AutoCloseable {
        private static final int ICON_WIDTH = 32;
        private static final int ICON_HEIGHT = 32;
        private final Minecraft minecraft;
        private final SelectWorldScreen screen;
        final LevelSummary summary;
        private final FaviconTexture icon;
        @Nullable
        private Path iconFile;
        private long lastClickTime;

        public WorldListEntry(WorldSelectionList pWorldSelectionList, LevelSummary pSummary) {
            this.minecraft = pWorldSelectionList.minecraft;
            this.screen = pWorldSelectionList.getScreen();
            this.summary = pSummary;
            this.icon = FaviconTexture.forWorld(this.minecraft.getTextureManager(), pSummary.getLevelId());
            this.iconFile = pSummary.getIcon();
            this.validateIconFile();
            this.loadIcon();
        }

        private void validateIconFile() {
            if (this.iconFile != null) {
                try {
                    BasicFileAttributes basicfileattributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    if (basicfileattributes.isSymbolicLink()) {
                        List<ForbiddenSymlinkInfo> list = this.minecraft.directoryValidator().validateSymlink(this.iconFile);
                        if (!list.isEmpty()) {
                            WorldSelectionList.LOGGER.warn("{}", ContentValidationException.getMessage(this.iconFile, list));
                            this.iconFile = null;
                        } else {
                            basicfileattributes = Files.readAttributes(this.iconFile, BasicFileAttributes.class);
                        }
                    }

                    if (!basicfileattributes.isRegularFile()) {
                        this.iconFile = null;
                    }
                } catch (NoSuchFileException nosuchfileexception) {
                    this.iconFile = null;
                } catch (IOException ioexception) {
                    WorldSelectionList.LOGGER.error("could not validate symlink", (Throwable)ioexception);
                    this.iconFile = null;
                }
            }
        }

        @Override
        public Component getNarration() {
            Component component = Component.translatable(
                "narrator.select.world_info",
                this.summary.getLevelName(),
                Component.translationArg(new Date(this.summary.getLastPlayed())),
                this.summary.getInfo()
            );
            if (this.summary.isLocked()) {
                component = CommonComponents.joinForNarration(component, WorldSelectionList.WORLD_LOCKED_TOOLTIP);
            }

            if (this.summary.isExperimental()) {
                component = CommonComponents.joinForNarration(component, WorldSelectionList.WORLD_EXPERIMENTAL);
            }

            return Component.translatable("narrator.select", component);
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
            String s = this.summary.getLevelName();
            String s1 = this.summary.getLevelId();
            long i = this.summary.getLastPlayed();
            if (i != -1L) {
                s1 = s1 + " (" + WorldSelectionList.DATE_FORMAT.format(Instant.ofEpochMilli(i)) + ")";
            }

            if (StringUtils.isEmpty(s)) {
                s = I18n.get("selectWorld.world") + " " + (pIndex + 1);
            }

            Component component = this.summary.getInfo();
            pGuiGraphics.drawString(this.minecraft.font, s, pLeft + 32 + 3, pTop + 1, 16777215, false);
            pGuiGraphics.drawString(this.minecraft.font, s1, pLeft + 32 + 3, pTop + 9 + 3, -8355712, false);
            pGuiGraphics.drawString(this.minecraft.font, component, pLeft + 32 + 3, pTop + 9 + 9 + 3, -8355712, false);
            RenderSystem.enableBlend();
            pGuiGraphics.blit(this.icon.textureLocation(), pLeft, pTop, 0.0F, 0.0F, 32, 32, 32, 32);
            RenderSystem.disableBlend();
            renderExperimentalWarning(pGuiGraphics, pMouseX, pMouseY, pTop, pLeft);
            if (this.minecraft.options.touchscreen().get() || pHovering) {
                pGuiGraphics.fill(pLeft, pTop, pLeft + 32, pTop + 32, -1601138544);
                int j = pMouseX - pLeft;
                boolean flag = j < 32;
                ResourceLocation resourcelocation = flag ? WorldSelectionList.JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.JOIN_SPRITE;
                ResourceLocation resourcelocation1 = flag ? WorldSelectionList.WARNING_HIGHLIGHTED_SPRITE : WorldSelectionList.WARNING_SPRITE;
                ResourceLocation resourcelocation2 = flag ? WorldSelectionList.ERROR_HIGHLIGHTED_SPRITE : WorldSelectionList.ERROR_SPRITE;
                ResourceLocation resourcelocation3 = flag ? WorldSelectionList.MARKED_JOIN_HIGHLIGHTED_SPRITE : WorldSelectionList.MARKED_JOIN_SPRITE;
                if (this.summary instanceof LevelSummary.SymlinkLevelSummary || this.summary instanceof LevelSummary.CorruptedLevelSummary) {
                    pGuiGraphics.blitSprite(resourcelocation2, pLeft, pTop, 32, 32);
                    pGuiGraphics.blitSprite(resourcelocation3, pLeft, pTop, 32, 32);
                    return;
                }

                if (this.summary.isLocked()) {
                    pGuiGraphics.blitSprite(resourcelocation2, pLeft, pTop, 32, 32);
                    if (flag) {
                        this.screen.setTooltipForNextRenderPass(this.minecraft.font.split(WorldSelectionList.WORLD_LOCKED_TOOLTIP, 175));
                    }
                } else if (this.summary.requiresManualConversion()) {
                    pGuiGraphics.blitSprite(resourcelocation2, pLeft, pTop, 32, 32);
                    if (flag) {
                        this.screen.setTooltipForNextRenderPass(this.minecraft.font.split(WorldSelectionList.WORLD_REQUIRES_CONVERSION, 175));
                    }
                } else if (!this.summary.isCompatible()) {
                    pGuiGraphics.blitSprite(resourcelocation2, pLeft, pTop, 32, 32);
                    if (flag) {
                        this.screen.setTooltipForNextRenderPass(this.minecraft.font.split(WorldSelectionList.INCOMPATIBLE_VERSION_TOOLTIP, 175));
                    }
                } else if (this.summary.shouldBackup()) {
                    pGuiGraphics.blitSprite(resourcelocation3, pLeft, pTop, 32, 32);
                    if (this.summary.isDowngrade()) {
                        pGuiGraphics.blitSprite(resourcelocation2, pLeft, pTop, 32, 32);
                        if (flag) {
                            this.screen
                                .setTooltipForNextRenderPass(
                                    ImmutableList.of(
                                        WorldSelectionList.FROM_NEWER_TOOLTIP_1.getVisualOrderText(),
                                        WorldSelectionList.FROM_NEWER_TOOLTIP_2.getVisualOrderText()
                                    )
                                );
                        }
                    } else if (!SharedConstants.getCurrentVersion().isStable()) {
                        pGuiGraphics.blitSprite(resourcelocation1, pLeft, pTop, 32, 32);
                        if (flag) {
                            this.screen
                                .setTooltipForNextRenderPass(
                                    ImmutableList.of(
                                        WorldSelectionList.SNAPSHOT_TOOLTIP_1.getVisualOrderText(), WorldSelectionList.SNAPSHOT_TOOLTIP_2.getVisualOrderText()
                                    )
                                );
                        }
                    }
                } else {
                    pGuiGraphics.blitSprite(resourcelocation, pLeft, pTop, 32, 32);
                }
            }
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
            if (!this.summary.primaryActionActive()) {
                return true;
            } else {
                WorldSelectionList.this.setSelected((WorldSelectionList.Entry)this);
                if (!(pMouseX - (double)WorldSelectionList.this.getRowLeft() <= 32.0) && Util.getMillis() - this.lastClickTime >= 250L) {
                    this.lastClickTime = Util.getMillis();
                    return true;
                } else {
                    if (this.canJoin()) {
                        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                        this.joinWorld();
                    }

                    return true;
                }
            }
        }

        public boolean canJoin() {
            return this.summary.primaryActionActive();
        }

        public void joinWorld() {
            if (this.summary.primaryActionActive()) {
                if (this.summary instanceof LevelSummary.SymlinkLevelSummary) {
                    this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
                } else {
                    this.minecraft.createWorldOpenFlows().checkForBackupAndLoad(this.summary.getLevelId(), () -> {
                        WorldSelectionList.this.reloadWorldList();
                        this.minecraft.setScreen(this.screen);
                    });
                }
            }
        }

        public void deleteWorld() {
            this.minecraft
                .setScreen(
                    new ConfirmScreen(
                        p_170322_ -> {
                            if (p_170322_) {
                                this.minecraft.setScreen(new ProgressScreen(true));
                                this.doDeleteWorld();
                            }

                            if (this.minecraft.screen instanceof ProgressScreen || this.minecraft.screen instanceof ConfirmScreen) // Neo - fix MC-251068
                            this.minecraft.setScreen(this.screen);
                        },
                        Component.translatable("selectWorld.deleteQuestion"),
                        Component.translatable("selectWorld.deleteWarning", this.summary.getLevelName()),
                        Component.translatable("selectWorld.deleteButton"),
                        CommonComponents.GUI_CANCEL
                    )
                );
        }

        public void doDeleteWorld() {
            LevelStorageSource levelstoragesource = this.minecraft.getLevelSource();
            String s = this.summary.getLevelId();

            try (LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = levelstoragesource.createAccess(s)) {
                levelstoragesource$levelstorageaccess.deleteLevel();
            } catch (IOException ioexception) {
                SystemToast.onWorldDeleteFailure(this.minecraft, s);
                WorldSelectionList.LOGGER.error("Failed to delete world {}", s, ioexception);
            }

            WorldSelectionList.this.reloadWorldList();
        }

        public void editWorld() {
            this.queueLoadScreen();
            String s = this.summary.getLevelId();

            LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess;
            try {
                levelstoragesource$levelstorageaccess = this.minecraft.getLevelSource().validateAndCreateAccess(s);
            } catch (IOException ioexception1) {
                SystemToast.onWorldAccessFailure(this.minecraft, s);
                WorldSelectionList.LOGGER.error("Failed to access level {}", s, ioexception1);
                WorldSelectionList.this.reloadWorldList();
                return;
            } catch (ContentValidationException contentvalidationexception) {
                WorldSelectionList.LOGGER.warn("{}", contentvalidationexception.getMessage());
                this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
                return;
            }

            EditWorldScreen editworldscreen;
            try {
                editworldscreen = EditWorldScreen.create(this.minecraft, levelstoragesource$levelstorageaccess, p_307110_ -> {
                    levelstoragesource$levelstorageaccess.safeClose();
                    if (p_307110_) {
                        WorldSelectionList.this.reloadWorldList();
                    }

                    this.minecraft.setScreen(this.screen);
                });
            } catch (NbtException | ReportedNbtException | IOException ioexception) {
                levelstoragesource$levelstorageaccess.safeClose();
                SystemToast.onWorldAccessFailure(this.minecraft, s);
                WorldSelectionList.LOGGER.error("Failed to load world data {}", s, ioexception);
                WorldSelectionList.this.reloadWorldList();
                return;
            }

            this.minecraft.setScreen(editworldscreen);
        }

        public void recreateWorld() {
            this.queueLoadScreen();

            try (LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.minecraft
                    .getLevelSource()
                    .validateAndCreateAccess(this.summary.getLevelId())) {
                Pair<LevelSettings, WorldCreationContext> pair = this.minecraft.createWorldOpenFlows().recreateWorldData(levelstoragesource$levelstorageaccess);
                LevelSettings levelsettings = pair.getFirst();
                WorldCreationContext worldcreationcontext = pair.getSecond();
                Path path = CreateWorldScreen.createTempDataPackDirFromExistingWorld(
                    levelstoragesource$levelstorageaccess.getLevelPath(LevelResource.DATAPACK_DIR), this.minecraft
                );
                if (worldcreationcontext.options().isOldCustomizedWorld()) {
                    this.minecraft
                        .setScreen(
                            new ConfirmScreen(
                                p_275882_ -> this.minecraft
                                        .setScreen(
                                            (Screen)(p_275882_
                                                ? CreateWorldScreen.createFromExisting(this.minecraft, this.screen, levelsettings, worldcreationcontext, path)
                                                : this.screen)
                                        ),
                                Component.translatable("selectWorld.recreate.customized.title"),
                                Component.translatable("selectWorld.recreate.customized.text"),
                                CommonComponents.GUI_PROCEED,
                                CommonComponents.GUI_CANCEL
                            )
                        );
                } else {
                    this.minecraft.setScreen(CreateWorldScreen.createFromExisting(this.minecraft, this.screen, levelsettings, worldcreationcontext, path));
                }
            } catch (ContentValidationException contentvalidationexception) {
                WorldSelectionList.LOGGER.warn("{}", contentvalidationexception.getMessage());
                this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(this.screen)));
            } catch (Exception exception) {
                WorldSelectionList.LOGGER.error("Unable to recreate world", (Throwable)exception);
                this.minecraft
                    .setScreen(
                        new AlertScreen(
                            () -> this.minecraft.setScreen(this.screen),
                            Component.translatable("selectWorld.recreate.error.title"),
                            Component.translatable("selectWorld.recreate.error.text")
                        )
                    );
            }
        }

        private void queueLoadScreen() {
            this.minecraft.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));
        }

        private void loadIcon() {
            boolean flag = this.iconFile != null && Files.isRegularFile(this.iconFile);
            if (flag) {
                try (InputStream inputstream = Files.newInputStream(this.iconFile)) {
                    this.icon.upload(NativeImage.read(inputstream));
                } catch (Throwable throwable) {
                    WorldSelectionList.LOGGER.error("Invalid icon for world {}", this.summary.getLevelId(), throwable);
                    this.iconFile = null;
                }
            } else {
                this.icon.clear();
            }
        }

        @Override
        public void close() {
            this.icon.close();
        }

        public String getLevelName() {
            return this.summary.getLevelName();
        }

        // FORGE: Patch in experimental warning icon for worlds in the world selection screen
        private void renderExperimentalWarning(GuiGraphics guiGraphics, int mouseX, int mouseY, int top, int left) {
            if (this.summary.isLifecycleExperimental()) {
                int leftStart = left + WorldSelectionList.this.getRowWidth();
                guiGraphics.blit(WorldSelectionList.FORGE_EXPERIMENTAL_WARNING_ICON, leftStart - 36, top, 0.0F, 0.0F, 32, 32, 32, 32);
                if (WorldSelectionList.this.getEntryAtPosition(mouseX, mouseY) == this && mouseX > leftStart - 36 && mouseX < leftStart) {
                    var font = Minecraft.getInstance().font;
                    List<net.minecraft.util.FormattedCharSequence> tooltip = font.split(Component.translatable("neoforge.experimentalsettings.tooltip"), 200);
                    guiGraphics.renderTooltip(font, tooltip, mouseX, mouseY);
                }
            }
        }
    }
}
