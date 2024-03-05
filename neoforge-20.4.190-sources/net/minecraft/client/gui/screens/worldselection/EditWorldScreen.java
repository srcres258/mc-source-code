package net.minecraft.client.gui.screens.worldselection;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class EditWorldScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Component NAME_LABEL = Component.translatable("selectWorld.enterName").withStyle(ChatFormatting.GRAY);
    private static final Component RESET_ICON_BUTTON = Component.translatable("selectWorld.edit.resetIcon");
    private static final Component FOLDER_BUTTON = Component.translatable("selectWorld.edit.openFolder");
    private static final Component BACKUP_BUTTON = Component.translatable("selectWorld.edit.backup");
    private static final Component BACKUP_FOLDER_BUTTON = Component.translatable("selectWorld.edit.backupFolder");
    private static final Component OPTIMIZE_BUTTON = Component.translatable("selectWorld.edit.optimize");
    private static final Component OPTIMIZE_TITLE = Component.translatable("optimizeWorld.confirm.title");
    private static final Component OPTIMIIZE_DESCRIPTION = Component.translatable("optimizeWorld.confirm.description");
    private static final Component SAVE_BUTTON = Component.translatable("selectWorld.edit.save");
    private static final int DEFAULT_WIDTH = 200;
    private static final int VERTICAL_SPACING = 4;
    private static final int HALF_WIDTH = 98;
    private final LinearLayout layout = LinearLayout.vertical().spacing(5);
    private final BooleanConsumer callback;
    private final LevelStorageSource.LevelStorageAccess levelAccess;

    public static EditWorldScreen create(Minecraft pMinecraft, LevelStorageSource.LevelStorageAccess pLevelAccess, BooleanConsumer pCallback) throws IOException {
        LevelSummary levelsummary = pLevelAccess.getSummary(pLevelAccess.getDataTag());
        return new EditWorldScreen(pMinecraft, pLevelAccess, levelsummary.getLevelName(), pCallback);
    }

    private EditWorldScreen(Minecraft pMinecraft, LevelStorageSource.LevelStorageAccess pLevelAccess, String pLevelName, BooleanConsumer pCallback) {
        super(Component.translatable("selectWorld.edit.title"));
        this.callback = pCallback;
        this.levelAccess = pLevelAccess;
        Font font = pMinecraft.font;
        this.layout.addChild(new SpacerElement(200, 20));
        this.layout.addChild(new StringWidget(NAME_LABEL, font));
        EditBox editbox = this.layout.addChild(new EditBox(font, 200, 20, NAME_LABEL));
        editbox.setValue(pLevelName);
        LinearLayout linearlayout = LinearLayout.horizontal().spacing(4);
        Button button = linearlayout.addChild(Button.builder(SAVE_BUTTON, p_307062_ -> this.onRename(editbox.getValue())).width(98).build());
        linearlayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_307073_ -> this.onClose()).width(98).build());
        editbox.setResponder(p_307070_ -> button.active = !Util.isBlank(p_307070_));
        this.layout.addChild(Button.builder(RESET_ICON_BUTTON, p_307072_ -> {
            pLevelAccess.getIconFile().ifPresent(p_182594_ -> FileUtils.deleteQuietly(p_182594_.toFile()));
            p_307072_.active = false;
        }).width(200).build()).active = pLevelAccess.getIconFile().filter(p_182587_ -> Files.isRegularFile(p_182587_)).isPresent();
        this.layout
            .addChild(
                Button.builder(FOLDER_BUTTON, p_307066_ -> Util.getPlatform().openFile(pLevelAccess.getLevelPath(LevelResource.ROOT).toFile())).width(200).build()
            );
        this.layout.addChild(Button.builder(BACKUP_BUTTON, p_307060_ -> {
            boolean flag = makeBackupAndShowToast(pLevelAccess);
            this.callback.accept(!flag);
        }).width(200).build());
        this.layout.addChild(Button.builder(BACKUP_FOLDER_BUTTON, p_307068_ -> {
            LevelStorageSource levelstoragesource = pMinecraft.getLevelSource();
            Path path = levelstoragesource.getBackupPath();

            try {
                FileUtil.createDirectoriesSafe(path);
            } catch (IOException ioexception) {
                throw new RuntimeException(ioexception);
            }

            Util.getPlatform().openFile(path.toFile());
        }).width(200).build());
        this.layout
            .addChild(
                Button.builder(
                        OPTIMIZE_BUTTON, p_307058_ -> pMinecraft.setScreen(new BackupConfirmScreen(() -> pMinecraft.setScreen(this), (p_307053_, p_307054_) -> {
                                if (p_307053_) {
                                    makeBackupAndShowToast(pLevelAccess);
                                }
                
                                pMinecraft.setScreen(OptimizeWorldScreen.create(pMinecraft, this.callback, pMinecraft.getFixerUpper(), pLevelAccess, p_307054_));
                            }, OPTIMIZE_TITLE, OPTIMIIZE_DESCRIPTION, true))
                    )
                    .width(200)
                    .build()
            );
        this.layout.addChild(new SpacerElement(200, 20));
        this.layout.addChild(linearlayout);
        this.setInitialFocus(editbox);
        this.layout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    protected void init() {
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        FrameLayout.centerInRectangle(this.layout, this.getRectangle());
    }

    @Override
    public void onClose() {
        this.callback.accept(false);
    }

    private void onRename(String pSaveName) {
        try {
            this.levelAccess.renameLevel(pSaveName);
        } catch (NbtException | ReportedNbtException | IOException ioexception) {
            LOGGER.error("Failed to access world '{}'", this.levelAccess.getLevelId(), ioexception);
            SystemToast.onWorldAccessFailure(this.minecraft, this.levelAccess.getLevelId());
        }

        this.callback.accept(true);
    }

    public static boolean makeBackupAndShowToast(LevelStorageSource.LevelStorageAccess pLevelAccess) {
        long i = 0L;
        IOException ioexception = null;

        try {
            i = pLevelAccess.makeWorldBackup();
        } catch (IOException ioexception1) {
            ioexception = ioexception1;
        }

        if (ioexception != null) {
            Component component2 = Component.translatable("selectWorld.edit.backupFailed");
            Component component3 = Component.literal(ioexception.getMessage());
            Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToast.SystemToastId.WORLD_BACKUP, component2, component3));
            return false;
        } else {
            Component component = Component.translatable("selectWorld.edit.backupCreated", pLevelAccess.getLevelId());
            Component component1 = Component.translatable("selectWorld.edit.backupSize", Mth.ceil((double)i / 1048576.0));
            Minecraft.getInstance().getToasts().addToast(new SystemToast(SystemToast.SystemToastId.WORLD_BACKUP, component, component1));
            return true;
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
        pGuiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 16777215);
    }
}
