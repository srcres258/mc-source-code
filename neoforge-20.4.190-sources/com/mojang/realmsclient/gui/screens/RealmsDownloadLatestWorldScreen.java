package com.mojang.realmsclient.gui.screens;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.Unit;
import com.mojang.realmsclient.client.FileDownload;
import com.mojang.realmsclient.dto.WorldDownload;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.realms.RealmsScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsDownloadLatestWorldScreen extends RealmsScreen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ReentrantLock DOWNLOAD_LOCK = new ReentrantLock();
    private static final int BAR_WIDTH = 200;
    private static final int BAR_TOP = 80;
    private static final int BAR_BOTTOM = 95;
    private static final int BAR_BORDER = 1;
    private final Screen lastScreen;
    private final WorldDownload worldDownload;
    private final Component downloadTitle;
    private final RateLimiter narrationRateLimiter;
    private Button cancelButton;
    private final String worldName;
    private final RealmsDownloadLatestWorldScreen.DownloadStatus downloadStatus;
    @Nullable
    private volatile Component errorMessage;
    private volatile Component status = Component.translatable("mco.download.preparing");
    @Nullable
    private volatile String progress;
    private volatile boolean cancelled;
    private volatile boolean showDots = true;
    private volatile boolean finished;
    private volatile boolean extracting;
    @Nullable
    private Long previousWrittenBytes;
    @Nullable
    private Long previousTimeSnapshot;
    private long bytesPersSecond;
    private int animTick;
    private static final String[] DOTS = new String[]{"", ".", ". .", ". . ."};
    private int dotIndex;
    private boolean checked;
    private final BooleanConsumer callback;

    public RealmsDownloadLatestWorldScreen(Screen pLastScreen, WorldDownload pWorldDownload, String pWorldName, BooleanConsumer pCallback) {
        super(GameNarrator.NO_TITLE);
        this.callback = pCallback;
        this.lastScreen = pLastScreen;
        this.worldName = pWorldName;
        this.worldDownload = pWorldDownload;
        this.downloadStatus = new RealmsDownloadLatestWorldScreen.DownloadStatus();
        this.downloadTitle = Component.translatable("mco.download.title");
        this.narrationRateLimiter = RateLimiter.create(0.1F);
    }

    @Override
    public void init() {
        this.cancelButton = this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, p_88642_ -> {
            this.cancelled = true;
            this.backButtonClicked();
        }).bounds((this.width - 200) / 2, this.height - 42, 200, 20).build());
        this.checkDownloadSize();
    }

    private void checkDownloadSize() {
        if (!this.finished) {
            if (!this.checked && this.getContentLength(this.worldDownload.downloadLink) >= 5368709120L) {
                Component component = Component.translatable("mco.download.confirmation.line1", Unit.humanReadable(5368709120L));
                Component component1 = Component.translatable("mco.download.confirmation.line2");
                this.minecraft.setScreen(new RealmsLongConfirmationScreen(p_280727_ -> {
                    this.checked = true;
                    this.minecraft.setScreen(this);
                    this.downloadSave();
                }, RealmsLongConfirmationScreen.Type.WARNING, component, component1, false));
            } else {
                this.downloadSave();
            }
        }
    }

    private long getContentLength(String pUri) {
        FileDownload filedownload = new FileDownload();
        return filedownload.contentLength(pUri);
    }

    @Override
    public void tick() {
        super.tick();
        ++this.animTick;
        if (this.status != null && this.narrationRateLimiter.tryAcquire(1)) {
            Component component = this.createProgressNarrationMessage();
            this.minecraft.getNarrator().sayNow(component);
        }
    }

    private Component createProgressNarrationMessage() {
        List<Component> list = Lists.newArrayList();
        list.add(this.downloadTitle);
        list.add(this.status);
        if (this.progress != null) {
            list.add(Component.translatable("mco.download.percent", this.progress));
            list.add(Component.translatable("mco.download.speed.narration", Unit.humanReadable(this.bytesPersSecond)));
        }

        if (this.errorMessage != null) {
            list.add(this.errorMessage);
        }

        return CommonComponents.joinLines(list);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (pKeyCode == 256) {
            this.cancelled = true;
            this.backButtonClicked();
            return true;
        } else {
            return super.keyPressed(pKeyCode, pScanCode, pModifiers);
        }
    }

    private void backButtonClicked() {
        if (this.finished && this.callback != null && this.errorMessage == null) {
            this.callback.accept(true);
        }

        this.minecraft.setScreen(this.lastScreen);
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
        pGuiGraphics.drawCenteredString(this.font, this.downloadTitle, this.width / 2, 20, 16777215);
        pGuiGraphics.drawCenteredString(this.font, this.status, this.width / 2, 50, 16777215);
        if (this.showDots) {
            this.drawDots(pGuiGraphics);
        }

        if (this.downloadStatus.bytesWritten != 0L && !this.cancelled) {
            this.drawProgressBar(pGuiGraphics);
            this.drawDownloadSpeed(pGuiGraphics);
        }

        if (this.errorMessage != null) {
            pGuiGraphics.drawCenteredString(this.font, this.errorMessage, this.width / 2, 110, 16711680);
        }
    }

    private void drawDots(GuiGraphics pGuiGraphics) {
        int i = this.font.width(this.status);
        if (this.animTick % 10 == 0) {
            ++this.dotIndex;
        }

        pGuiGraphics.drawString(this.font, DOTS[this.dotIndex % DOTS.length], this.width / 2 + i / 2 + 5, 50, 16777215, false);
    }

    private void drawProgressBar(GuiGraphics pGuiGraphics) {
        double d0 = Math.min((double)this.downloadStatus.bytesWritten / (double)this.downloadStatus.totalBytes, 1.0);
        this.progress = String.format(Locale.ROOT, "%.1f", d0 * 100.0);
        int i = (this.width - 200) / 2;
        int j = i + (int)Math.round(200.0 * d0);
        pGuiGraphics.fill(i - 1, 79, j + 1, 96, -2501934);
        pGuiGraphics.fill(i, 80, j, 95, -8355712);
        pGuiGraphics.drawCenteredString(this.font, Component.translatable("mco.download.percent", this.progress), this.width / 2, 84, 16777215);
    }

    private void drawDownloadSpeed(GuiGraphics pGuiGraphics) {
        if (this.animTick % 20 == 0) {
            if (this.previousWrittenBytes != null) {
                long i = Util.getMillis() - this.previousTimeSnapshot;
                if (i == 0L) {
                    i = 1L;
                }

                this.bytesPersSecond = 1000L * (this.downloadStatus.bytesWritten - this.previousWrittenBytes) / i;
                this.drawDownloadSpeed0(pGuiGraphics, this.bytesPersSecond);
            }

            this.previousWrittenBytes = this.downloadStatus.bytesWritten;
            this.previousTimeSnapshot = Util.getMillis();
        } else {
            this.drawDownloadSpeed0(pGuiGraphics, this.bytesPersSecond);
        }
    }

    private void drawDownloadSpeed0(GuiGraphics pGuiGraphics, long pBytesPerSecond) {
        if (pBytesPerSecond > 0L) {
            int i = this.font.width(this.progress);
            pGuiGraphics.drawString(
                this.font, Component.translatable("mco.download.speed", Unit.humanReadable(pBytesPerSecond)), this.width / 2 + i / 2 + 15, 84, 16777215, false
            );
        }
    }

    private void downloadSave() {
        new Thread(() -> {
            try {
                try {
                    if (!DOWNLOAD_LOCK.tryLock(1L, TimeUnit.SECONDS)) {
                        this.status = Component.translatable("mco.download.failed");
                        return;
                    }

                    if (this.cancelled) {
                        this.downloadCancelled();
                        return;
                    }

                    this.status = Component.translatable("mco.download.downloading", this.worldName);
                    FileDownload filedownload = new FileDownload();
                    filedownload.contentLength(this.worldDownload.downloadLink);
                    filedownload.download(this.worldDownload, this.worldName, this.downloadStatus, this.minecraft.getLevelSource());

                    while(!filedownload.isFinished()) {
                        if (filedownload.isError()) {
                            filedownload.cancel();
                            this.errorMessage = Component.translatable("mco.download.failed");
                            this.cancelButton.setMessage(CommonComponents.GUI_DONE);
                            return;
                        }

                        if (filedownload.isExtracting()) {
                            if (!this.extracting) {
                                this.status = Component.translatable("mco.download.extracting");
                            }

                            this.extracting = true;
                        }

                        if (this.cancelled) {
                            filedownload.cancel();
                            this.downloadCancelled();
                            return;
                        }

                        try {
                            Thread.sleep(500L);
                        } catch (InterruptedException interruptedexception) {
                            LOGGER.error("Failed to check Realms backup download status");
                        }
                    }

                    this.finished = true;
                    this.status = Component.translatable("mco.download.done");
                    this.cancelButton.setMessage(CommonComponents.GUI_DONE);
                    return;
                } catch (InterruptedException interruptedexception1) {
                    LOGGER.error("Could not acquire upload lock");
                } catch (Exception exception) {
                    this.errorMessage = Component.translatable("mco.download.failed");
                    LOGGER.info("Exception while downloading world", (Throwable)exception);
                }
            } finally {
                if (!DOWNLOAD_LOCK.isHeldByCurrentThread()) {
                    return;
                } else {
                    DOWNLOAD_LOCK.unlock();
                    this.showDots = false;
                    this.finished = true;
                }
            }
        }).start();
    }

    private void downloadCancelled() {
        this.status = Component.translatable("mco.download.cancelled");
    }

    @OnlyIn(Dist.CLIENT)
    public static class DownloadStatus {
        public volatile long bytesWritten;
        public volatile long totalBytes;
    }
}
