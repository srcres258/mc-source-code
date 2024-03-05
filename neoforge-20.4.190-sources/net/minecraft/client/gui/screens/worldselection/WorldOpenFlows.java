package net.minecraft.client.gui.screens.worldselection;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.BackupConfirmScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.DatapackLoadFailureScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.NoticeWithLinkScreen;
import net.minecraft.client.gui.screens.RecoverWorldDataScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.commands.Commands;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.util.MemoryReserve;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.LevelDataAndDimensions;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraft.world.level.validation.ContentValidationException;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class WorldOpenFlows { // TODO 1.20.3 PORTING: re-add the autoconfirm code
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final UUID WORLD_PACK_ID = UUID.fromString("640a6a92-b6cb-48a0-b391-831586500359");
    private final Minecraft minecraft;
    private final LevelStorageSource levelSource;

    public WorldOpenFlows(Minecraft pMinecraft, LevelStorageSource pLevelSource) {
        this.minecraft = pMinecraft;
        this.levelSource = pLevelSource;
    }

    public void createFreshLevel(
        String pLevelName, LevelSettings pLevelSettings, WorldOptions pWorldOptions, Function<RegistryAccess, WorldDimensions> pDimensionGetter, Screen pLastScreen
    ) {
        this.minecraft.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));
        LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.createWorldAccess(pLevelName);
        if (levelstoragesource$levelstorageaccess != null) {
            PackRepository packrepository = ServerPacksSource.createPackRepository(levelstoragesource$levelstorageaccess);
            WorldDataConfiguration worlddataconfiguration = pLevelSettings.getDataConfiguration();

            try {
                WorldLoader.PackConfig worldloader$packconfig = new WorldLoader.PackConfig(packrepository, worlddataconfiguration, false, false);
                WorldStem worldstem = this.loadWorldDataBlocking(
                    worldloader$packconfig,
                    p_258145_ -> {
                        WorldDimensions.Complete worlddimensions$complete = pDimensionGetter.apply(p_258145_.datapackWorldgen())
                            .bake(p_258145_.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM));
                        return new WorldLoader.DataLoadOutput<>(
                            new PrimaryLevelData(pLevelSettings, pWorldOptions, worlddimensions$complete.specialWorldProperty(), worlddimensions$complete.lifecycle()),
                            worlddimensions$complete.dimensionsRegistryAccess()
                        );
                    },
                    WorldStem::new
                );
                this.minecraft.doWorldLoad(levelstoragesource$levelstorageaccess, packrepository, worldstem, true);
            } catch (Exception exception) {
                LOGGER.warn("Failed to load datapacks, can't proceed with server load", (Throwable)exception);
                levelstoragesource$levelstorageaccess.safeClose();
                this.minecraft.setScreen(pLastScreen);
            }
        }
    }

    @Nullable
    private LevelStorageSource.LevelStorageAccess createWorldAccess(String pLevelName) {
        try {
            return this.levelSource.validateAndCreateAccess(pLevelName);
        } catch (IOException ioexception) {
            LOGGER.warn("Failed to read level {} data", pLevelName, ioexception);
            SystemToast.onWorldAccessFailure(this.minecraft, pLevelName);
            this.minecraft.setScreen(null);
            return null;
        } catch (ContentValidationException contentvalidationexception) {
            LOGGER.warn("{}", contentvalidationexception.getMessage());
            this.minecraft.setScreen(NoticeWithLinkScreen.createWorldSymlinkWarningScreen(() -> this.minecraft.setScreen(null)));
            return null;
        }
    }

    public void createLevelFromExistingSettings(
        LevelStorageSource.LevelStorageAccess pLevelStorage,
        ReloadableServerResources pResources,
        LayeredRegistryAccess<RegistryLayer> pRegistries,
        WorldData pWorldData
    ) {
        PackRepository packrepository = ServerPacksSource.createPackRepository(pLevelStorage);
        CloseableResourceManager closeableresourcemanager = new WorldLoader.PackConfig(packrepository, pWorldData.getDataConfiguration(), false, false)
            .createResourceManager()
            .getSecond();
        this.minecraft.doWorldLoad(pLevelStorage, packrepository, new WorldStem(closeableresourcemanager, pResources, pRegistries, pWorldData), true);
    }

    public WorldStem loadWorldStem(Dynamic<?> pDynamic, boolean pSafeMode, PackRepository pPackRepository) throws Exception {
        WorldLoader.PackConfig worldloader$packconfig = LevelStorageSource.getPackConfig(pDynamic, pPackRepository, pSafeMode);
        return this.loadWorldDataBlocking(
            worldloader$packconfig,
            p_307082_ -> {
                Registry<LevelStem> registry = p_307082_.datapackDimensions().registryOrThrow(Registries.LEVEL_STEM);
                LevelDataAndDimensions leveldataanddimensions = LevelStorageSource.getLevelDataAndDimensions(
                    pDynamic, p_307082_.dataConfiguration(), registry, p_307082_.datapackWorldgen()
                );
                return new WorldLoader.DataLoadOutput<>(leveldataanddimensions.worldData(), leveldataanddimensions.dimensions().dimensionsRegistryAccess());
            },
            WorldStem::new
        );
    }

    public Pair<LevelSettings, WorldCreationContext> recreateWorldData(LevelStorageSource.LevelStorageAccess pLevelStorage) throws Exception {
        PackRepository packrepository = ServerPacksSource.createPackRepository(pLevelStorage);
        Dynamic<?> dynamic = pLevelStorage.getDataTag();
        WorldLoader.PackConfig worldloader$packconfig = LevelStorageSource.getPackConfig(dynamic, packrepository, false);
        @OnlyIn(Dist.CLIENT)
        record Data(LevelSettings levelSettings, WorldOptions options, Registry<LevelStem> existingDimensions) {
        }
        return this.<Data, Pair<LevelSettings, WorldCreationContext>>loadWorldDataBlocking(
            worldloader$packconfig,
            p_307097_ -> {
                Registry<LevelStem> registry = new MappedRegistry<>(Registries.LEVEL_STEM, Lifecycle.stable()).freeze();
                LevelDataAndDimensions leveldataanddimensions = LevelStorageSource.getLevelDataAndDimensions(
                    dynamic, p_307097_.dataConfiguration(), registry, p_307097_.datapackWorldgen()
                );
                return new WorldLoader.DataLoadOutput<>(
                    new Data(
                        leveldataanddimensions.worldData().getLevelSettings(),
                        leveldataanddimensions.worldData().worldGenOptions(),
                        leveldataanddimensions.dimensions().dimensions()
                    ),
                    p_307097_.datapackDimensions()
                );
            },
            (p_247840_, p_247841_, p_247842_, p_247843_) -> {
                p_247840_.close();
                return Pair.of(
                    p_247843_.levelSettings,
                    new WorldCreationContext(
                        p_247843_.options,
                        new WorldDimensions(p_247843_.existingDimensions),
                        p_247842_,
                        p_247841_,
                        p_247843_.levelSettings.getDataConfiguration()
                    )
                );
            }
        );

        }

    private <D, R> R loadWorldDataBlocking(
        WorldLoader.PackConfig pPackConfig, WorldLoader.WorldDataSupplier<D> pWorldDataSupplier, WorldLoader.ResultFactory<D, R> pResultFactory
    ) throws Exception {
        WorldLoader.InitConfig worldloader$initconfig = new WorldLoader.InitConfig(pPackConfig, Commands.CommandSelection.INTEGRATED, 2);
        CompletableFuture<R> completablefuture = WorldLoader.load(worldloader$initconfig, pWorldDataSupplier, pResultFactory, Util.backgroundExecutor(), this.minecraft);
        this.minecraft.managedBlock(completablefuture::isDone);
        return completablefuture.get();
    }

    private void askForBackup(LevelStorageSource.LevelStorageAccess pLevelStorage, boolean pCustomized, Runnable pLoadLevel, Runnable pOnCancel) {
        Component component;
        Component component1;
        if (pCustomized) {
            component = Component.translatable("selectWorld.backupQuestion.customized");
            component1 = Component.translatable("selectWorld.backupWarning.customized");
        } else {
            component = Component.translatable("selectWorld.backupQuestion.experimental");
            component1 = Component.translatable("selectWorld.backupWarning.experimental");
        }

        this.minecraft.setScreen(new BackupConfirmScreen(pOnCancel, (p_307085_, p_307086_) -> {
            if (p_307085_) {
                EditWorldScreen.makeBackupAndShowToast(pLevelStorage);
            }

            pLoadLevel.run();
        }, component, component1, false));
    }

    public static void confirmWorldCreation(Minecraft pMinecraft, CreateWorldScreen pScreen, Lifecycle pLifecycle, Runnable pLoadWorld, boolean pSkipWarnings) {
        BooleanConsumer booleanconsumer = p_233154_ -> {
            if (p_233154_) {
                pLoadWorld.run();
            } else {
                pMinecraft.setScreen(pScreen);
            }
        };
        if (pSkipWarnings || pLifecycle == Lifecycle.stable()) {
            pLoadWorld.run();
        } else if (pLifecycle == Lifecycle.experimental()) {
            pMinecraft.setScreen(
                new ConfirmScreen(
                    booleanconsumer,
                    Component.translatable("selectWorld.warning.experimental.title"),
                    Component.translatable("selectWorld.warning.experimental.question")
                )
            );
        } else {
            pMinecraft.setScreen(
                new ConfirmScreen(
                    booleanconsumer,
                    Component.translatable("selectWorld.warning.deprecated.title"),
                    Component.translatable("selectWorld.warning.deprecated.question")
                )
            );
        }
    }

    public void checkForBackupAndLoad(String pLevelName, Runnable pCallback) {
        this.minecraft.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));
        LevelStorageSource.LevelStorageAccess levelstoragesource$levelstorageaccess = this.createWorldAccess(pLevelName);
        if (levelstoragesource$levelstorageaccess != null) {
            this.checkForBackupAndLoad(levelstoragesource$levelstorageaccess, pCallback);
        }
    }

    private void checkForBackupAndLoad(LevelStorageSource.LevelStorageAccess pLevelStorage, Runnable pCallback) {
        this.minecraft.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.data_read")));

        Dynamic<?> dynamic;
        LevelSummary levelsummary;
        try {
            dynamic = pLevelStorage.getDataTag();
            levelsummary = pLevelStorage.getSummary(dynamic);
        } catch (NbtException | ReportedNbtException | IOException ioexception) {
            this.minecraft.setScreen(new RecoverWorldDataScreen(this.minecraft, p_307104_ -> {
                if (p_307104_) {
                    this.checkForBackupAndLoad(pLevelStorage, pCallback);
                } else {
                    pLevelStorage.safeClose();
                    pCallback.run();
                }
            }, pLevelStorage));
            return;
        } catch (OutOfMemoryError outofmemoryerror1) {
            MemoryReserve.release();
            System.gc();
            String s = "Ran out of memory trying to read level data of world folder \"" + pLevelStorage.getLevelId() + "\"";
            LOGGER.error(LogUtils.FATAL_MARKER, s);
            OutOfMemoryError outofmemoryerror = new OutOfMemoryError("Ran out of memory reading level data");
            outofmemoryerror.initCause(outofmemoryerror1);
            CrashReport crashreport = CrashReport.forThrowable(outofmemoryerror, s);
            CrashReportCategory crashreportcategory = crashreport.addCategory("World details");
            crashreportcategory.setDetail("World folder", pLevelStorage.getLevelId());
            throw new ReportedException(crashreport);
        }

        if (!levelsummary.isCompatible()) {
            pLevelStorage.safeClose();
            this.minecraft
                .setScreen(
                    new AlertScreen(
                        pCallback,
                        Component.translatable("selectWorld.incompatible.title").withColor(-65536),
                        Component.translatable("selectWorld.incompatible.description", levelsummary.getWorldVersionName())
                    )
                );
        } else {
            LevelSummary.BackupStatus levelsummary$backupstatus = levelsummary.backupStatus();
            if (levelsummary$backupstatus.shouldBackup()) {
                String s1 = "selectWorld.backupQuestion." + levelsummary$backupstatus.getTranslationKey();
                String s2 = "selectWorld.backupWarning." + levelsummary$backupstatus.getTranslationKey();
                MutableComponent mutablecomponent = Component.translatable(s1);
                if (levelsummary$backupstatus.isSevere()) {
                    mutablecomponent.withColor(-2142128);
                }

                Component component = Component.translatable(s2, levelsummary.getWorldVersionName(), SharedConstants.getCurrentVersion().getName());
                this.minecraft.setScreen(new BackupConfirmScreen(() -> {
                    pLevelStorage.safeClose();
                    pCallback.run();
                }, (p_307090_, p_307091_) -> {
                    if (p_307090_) {
                        EditWorldScreen.makeBackupAndShowToast(pLevelStorage);
                    }

                    this.loadLevel(pLevelStorage, dynamic, false, true, pCallback);
                }, mutablecomponent, component, false));
            } else {
                this.loadLevel(pLevelStorage, dynamic, false, true, pCallback);
            }
        }
    }

    public CompletableFuture<Void> loadBundledResourcePack(DownloadedPackSource pPackSource, LevelStorageSource.LevelStorageAccess pLevel) {
        Path path = pLevel.getLevelPath(LevelResource.MAP_RESOURCE_FILE);
        if (Files.exists(path) && !Files.isDirectory(path)) {
            pPackSource.configureForLocalWorld();
            CompletableFuture<Void> completablefuture = pPackSource.waitForPackFeedback(WORLD_PACK_ID);
            pPackSource.pushLocalPack(WORLD_PACK_ID, path);
            return completablefuture;
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }

    private void loadLevel(LevelStorageSource.LevelStorageAccess pLevelStorage, Dynamic<?> pDynamic, boolean pSafeMode, boolean pCheckForBackup, Runnable pCallback) {
        // NEOFORGE: Patch in overload to reduce further patching
        loadLevel(pLevelStorage, pDynamic, pSafeMode, pCheckForBackup, pCallback, false);
    }
    private void loadLevel(LevelStorageSource.LevelStorageAccess p_307362_, Dynamic<?> p_307494_, boolean p_307298_, boolean p_307581_, Runnable p_307336_, boolean confirmExperimentalWarning) {
        this.minecraft.forceSetScreen(new GenericDirtMessageScreen(Component.translatable("selectWorld.resource_load")));
        PackRepository packrepository = ServerPacksSource.createPackRepository(p_307362_);

        WorldStem worldstem;
        try {
            p_307362_.readAdditionalLevelSaveData(false); // Read extra (e.g. modded) data from the world before creating it
            worldstem = this.loadWorldStem(p_307494_, p_307298_, packrepository);
            // Forge: Skip confirmation if it has been done already for this world
            if (confirmExperimentalWarning && worldstem.worldData() instanceof PrimaryLevelData pld) {
                pld.withConfirmedWarning(true);
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to load level data or datapacks, can't proceed with server load", (Throwable)exception);
            if (!p_307298_) {
                this.minecraft.setScreen(new DatapackLoadFailureScreen(() -> {
                    p_307362_.safeClose();
                    p_307336_.run();
                }, () -> this.loadLevel(p_307362_, p_307494_, true, p_307581_, p_307336_)));
            } else {
                p_307362_.safeClose();
                this.minecraft
                    .setScreen(
                        new AlertScreen(
                            p_307336_,
                            Component.translatable("datapackFailure.safeMode.failed.title"),
                            Component.translatable("datapackFailure.safeMode.failed.description"),
                            CommonComponents.GUI_BACK,
                            true
                        )
                    );
            }

            return;
        }

        WorldData worlddata = worldstem.worldData();
        boolean flag = worlddata.worldGenOptions().isOldCustomizedWorld();
        boolean flag1 = worlddata.worldGenSettingsLifecycle() != Lifecycle.stable();
        boolean skipConfirmation = worlddata instanceof PrimaryLevelData pld && pld.hasConfirmedExperimentalWarning();
        if (skipConfirmation || !p_307581_ || !flag && !flag1) {
            DownloadedPackSource downloadedpacksource = this.minecraft.getDownloadedPackSource();
            this.loadBundledResourcePack(downloadedpacksource, p_307362_).thenApply(p_233177_ -> true).exceptionallyComposeAsync(p_233183_ -> {
                LOGGER.warn("Failed to load pack: ", p_233183_);
                return this.promptBundledPackLoadFailure();
            }, this.minecraft).thenAcceptAsync(p_314400_ -> {
                if (p_314400_) {
                    this.minecraft.doWorldLoad(p_307362_, packrepository, worldstem, false);
                } else {
                    worldstem.close();
                    p_307362_.safeClose();
                    downloadedpacksource.popAll();
                    p_307336_.run();
                }
            }, this.minecraft).exceptionally(p_233175_ -> {
                this.minecraft.delayCrash(CrashReport.forThrowable(p_233175_, "Load world"));
                return null;
            });
        } else {
            if (flag) // Forge: For legacy world options, let vanilla handle it.
            this.askForBackup(p_307362_, flag, () -> this.loadLevel(p_307362_, p_307494_, p_307298_, false, p_307336_), () -> {
                p_307362_.safeClose();
                p_307336_.run();
            });
            else net.neoforged.neoforge.client.ClientHooks.createWorldConfirmationScreen(() -> this.loadLevel(p_307362_, p_307494_, p_307298_, false, p_307336_, true));
            worldstem.close();
        }
    }

    private CompletableFuture<Boolean> promptBundledPackLoadFailure() {
        CompletableFuture<Boolean> completablefuture = new CompletableFuture<>();
        this.minecraft
            .setScreen(
                new ConfirmScreen(
                    completablefuture::complete,
                    Component.translatable("multiplayer.texturePrompt.failure.line1"),
                    Component.translatable("multiplayer.texturePrompt.failure.line2"),
                    CommonComponents.GUI_PROCEED,
                    CommonComponents.GUI_CANCEL
                )
            );
        return completablefuture;
    }
}
