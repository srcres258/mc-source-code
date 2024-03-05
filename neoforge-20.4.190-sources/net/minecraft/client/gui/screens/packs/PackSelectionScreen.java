package net.minecraft.client.gui.screens.packs;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackDetector;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class PackSelectionScreen extends Screen {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final int LIST_WIDTH = 200;
    private static final Component DRAG_AND_DROP = Component.translatable("pack.dropInfo").withStyle(ChatFormatting.GRAY);
    private static final Component DIRECTORY_BUTTON_TOOLTIP = Component.translatable("pack.folderInfo");
    private static final int RELOAD_COOLDOWN = 20;
    private static final ResourceLocation DEFAULT_ICON = new ResourceLocation("textures/misc/unknown_pack.png");
    private final PackSelectionModel model;
    @Nullable
    private PackSelectionScreen.Watcher watcher;
    private long ticksToReload;
    private TransferableSelectionList availablePackList;
    private TransferableSelectionList selectedPackList;
    private final Path packDir;
    private Button doneButton;
    private final Map<String, ResourceLocation> packIcons = Maps.newHashMap();

    public PackSelectionScreen(PackRepository pRepository, Consumer<PackRepository> pOutput, Path pPackDir, Component pTitle) {
        super(pTitle);
        this.model = new PackSelectionModel(this::populateLists, this::getPackIcon, pRepository, pOutput);
        this.packDir = pPackDir;
        this.watcher = PackSelectionScreen.Watcher.create(pPackDir);
    }

    @Override
    public void onClose() {
        this.model.commit();
        this.closeWatcher();
    }

    private void closeWatcher() {
        if (this.watcher != null) {
            try {
                this.watcher.close();
                this.watcher = null;
            } catch (Exception exception) {
            }
        }
    }

    @Override
    protected void init() {
        this.availablePackList = this.addRenderableWidget(
            new TransferableSelectionList(this.minecraft, this, 200, this.height, Component.translatable("pack.available.title"))
        );
        this.availablePackList.setX(this.width / 2 - 4 - 200);
        this.selectedPackList = this.addRenderableWidget(
            new TransferableSelectionList(this.minecraft, this, 200, this.height, Component.translatable("pack.selected.title"))
        );
        this.selectedPackList.setX(this.width / 2 + 4);
        this.addRenderableWidget(
            Button.builder(Component.translatable("pack.openFolder"), p_100004_ -> Util.getPlatform().openUri(this.packDir.toUri()))
                .bounds(this.width / 2 - 154, this.height - 48, 150, 20)
                .tooltip(Tooltip.create(DIRECTORY_BUTTON_TOOLTIP))
                .build()
        );
        this.doneButton = this.addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE, p_100036_ -> this.onClose()).bounds(this.width / 2 + 4, this.height - 48, 150, 20).build()
        );
        this.reload();
    }

    @Override
    public void tick() {
        if (this.watcher != null) {
            try {
                if (this.watcher.pollForChanges()) {
                    this.ticksToReload = 20L;
                }
            } catch (IOException ioexception) {
                LOGGER.warn("Failed to poll for directory {} changes, stopping", this.packDir);
                this.closeWatcher();
            }
        }

        if (this.ticksToReload > 0L && --this.ticksToReload == 0L) {
            this.reload();
        }
    }

    private void populateLists() {
        this.updateList(this.selectedPackList, this.model.getSelected());
        this.updateList(this.availablePackList, this.model.getUnselected());
        this.doneButton.active = !this.selectedPackList.children().isEmpty();
    }

    private void updateList(TransferableSelectionList pSelection, Stream<PackSelectionModel.Entry> pModels) {
        pSelection.children().clear();
        TransferableSelectionList.PackEntry transferableselectionlist$packentry = pSelection.getSelected();
        String s = transferableselectionlist$packentry == null ? "" : transferableselectionlist$packentry.getPackId();
        pSelection.setSelected(null);
        pModels.forEach(
            p_313437_ -> {
                TransferableSelectionList.PackEntry transferableselectionlist$packentry1 = new TransferableSelectionList.PackEntry(
                    this.minecraft, pSelection, p_313437_
                );
                pSelection.children().add(transferableselectionlist$packentry1);
                if (p_313437_.getId().equals(s)) {
                    pSelection.setSelected(transferableselectionlist$packentry1);
                }
            }
        );
    }

    public void updateFocus(TransferableSelectionList pSelection) {
        TransferableSelectionList transferableselectionlist = this.selectedPackList == pSelection ? this.availablePackList : this.selectedPackList;
        this.changeFocus(ComponentPath.path(transferableselectionlist.getFirstElement(), transferableselectionlist, this));
    }

    public void clearSelected() {
        this.selectedPackList.setSelected(null);
        this.availablePackList.setSelected(null);
    }

    private void reload() {
        this.model.findNewPacks();
        this.populateLists();
        this.ticksToReload = 0L;
        this.packIcons.clear();
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
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 8, 16777215);
        pGuiGraphics.drawCenteredString(this.font, DRAG_AND_DROP, this.width / 2, 20, 16777215);
    }

    @Override
    public void renderBackground(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderDirtBackground(pGuiGraphics);
    }

    protected static void copyPacks(Minecraft pMinecraft, List<Path> pPacks, Path pOutDir) {
        MutableBoolean mutableboolean = new MutableBoolean();
        pPacks.forEach(p_170009_ -> {
            try (Stream<Path> stream = Files.walk(p_170009_)) {
                stream.forEach(p_170005_ -> {
                    try {
                        Util.copyBetweenDirs(p_170009_.getParent(), pOutDir, p_170005_);
                    } catch (IOException ioexception1) {
                        LOGGER.warn("Failed to copy datapack file  from {} to {}", p_170005_, pOutDir, ioexception1);
                        mutableboolean.setTrue();
                    }
                });
            } catch (IOException ioexception) {
                LOGGER.warn("Failed to copy datapack file from {} to {}", p_170009_, pOutDir);
                mutableboolean.setTrue();
            }
        });
        if (mutableboolean.isTrue()) {
            SystemToast.onPackCopyFailure(pMinecraft, pOutDir.toString());
        }
    }

    @Override
    public void onFilesDrop(List<Path> pPacks) {
        String s = extractPackNames(pPacks).collect(Collectors.joining(", "));
        this.minecraft
            .setScreen(
                new ConfirmScreen(
                    p_293606_ -> {
                        if (p_293606_) {
                            List<Path> list = new ArrayList<>(pPacks.size());
                            Set<Path> set = new HashSet<>(pPacks);
                            PackDetector<Path> packdetector = new PackDetector<Path>(this.minecraft.directoryValidator()) {
                                protected Path createZipPack(Path p_294508_) {
                                    return p_294508_;
                                }
            
                                protected Path createDirectoryPack(Path p_296022_) {
                                    return p_296022_;
                                }
                            };
                            List<ForbiddenSymlinkInfo> list1 = new ArrayList<>();
            
                            for(Path path : pPacks) {
                                try {
                                    Path path1 = packdetector.detectPackResources(path, list1);
                                    if (path1 == null) {
                                        LOGGER.warn("Path {} does not seem like pack", path);
                                    } else {
                                        list.add(path1);
                                        set.remove(path1);
                                    }
                                } catch (IOException ioexception) {
                                    LOGGER.warn("Failed to check {} for packs", path, ioexception);
                                }
                            }
            
                            if (!list1.isEmpty()) {
                                this.minecraft.setScreen(NoticeWithLinkScreen.createPackSymlinkWarningScreen(() -> this.minecraft.setScreen(this)));
                                return;
                            }
            
                            if (!list.isEmpty()) {
                                copyPacks(this.minecraft, list, this.packDir);
                                this.reload();
                            }
            
                            if (!set.isEmpty()) {
                                String s1 = extractPackNames(set).collect(Collectors.joining(", "));
                                this.minecraft
                                    .setScreen(
                                        new AlertScreen(
                                            () -> this.minecraft.setScreen(this),
                                            Component.translatable("pack.dropRejected.title"),
                                            Component.translatable("pack.dropRejected.message", s1)
                                        )
                                    );
                                return;
                            }
                        }
            
                        this.minecraft.setScreen(this);
                    },
                    Component.translatable("pack.dropConfirm"),
                    Component.literal(s)
                )
            );
    }

    private static Stream<String> extractPackNames(Collection<Path> pPaths) {
        return pPaths.stream().map(Path::getFileName).map(Path::toString);
    }

    private ResourceLocation loadPackIcon(TextureManager pTextureManager, Pack pPack) {
        try {
            ResourceLocation resourcelocation1;
            try (PackResources packresources = pPack.open()) {
                IoSupplier<InputStream> iosupplier = packresources.getRootResource("pack.png");
                if (iosupplier == null) {
                    return DEFAULT_ICON;
                }

                String s = pPack.getId();
                ResourceLocation resourcelocation = new ResourceLocation(
                    "minecraft", "pack/" + Util.sanitizeName(s, ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(s) + "/icon"
                );

                try (InputStream inputstream = iosupplier.get()) {
                    NativeImage nativeimage = NativeImage.read(inputstream);
                    pTextureManager.register(resourcelocation, new DynamicTexture(nativeimage));
                    resourcelocation1 = resourcelocation;
                }
            }

            return resourcelocation1;
        } catch (Exception exception) {
            LOGGER.warn("Failed to load icon from pack {}", pPack.getId(), exception);
            return DEFAULT_ICON;
        }
    }

    private ResourceLocation getPackIcon(Pack p_99990_) {
        return this.packIcons.computeIfAbsent(p_99990_.getId(), p_280879_ -> this.loadPackIcon(this.minecraft.getTextureManager(), p_99990_));
    }

    @OnlyIn(Dist.CLIENT)
    static class Watcher implements AutoCloseable {
        private final WatchService watcher;
        private final Path packPath;

        public Watcher(Path pPackPath) throws IOException {
            this.packPath = pPackPath;
            this.watcher = pPackPath.getFileSystem().newWatchService();

            try {
                this.watchDir(pPackPath);

                try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(pPackPath)) {
                    for(Path path : directorystream) {
                        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                            this.watchDir(path);
                        }
                    }
                }
            } catch (Exception exception) {
                this.watcher.close();
                throw exception;
            }
        }

        @Nullable
        public static PackSelectionScreen.Watcher create(Path pPackPath) {
            try {
                return new PackSelectionScreen.Watcher(pPackPath);
            } catch (IOException ioexception) {
                PackSelectionScreen.LOGGER.warn("Failed to initialize pack directory {} monitoring", pPackPath, ioexception);
                return null;
            }
        }

        private void watchDir(Path pPath) throws IOException {
            pPath.register(this.watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
        }

        public boolean pollForChanges() throws IOException {
            boolean flag = false;

            WatchKey watchkey;
            while((watchkey = this.watcher.poll()) != null) {
                for(WatchEvent<?> watchevent : watchkey.pollEvents()) {
                    flag = true;
                    if (watchkey.watchable() == this.packPath && watchevent.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                        Path path = this.packPath.resolve((Path)watchevent.context());
                        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                            this.watchDir(path);
                        }
                    }
                }

                watchkey.reset();
            }

            return flag;
        }

        @Override
        public void close() throws IOException {
            this.watcher.close();
        }
    }
}
