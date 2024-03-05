package net.minecraft.server.packs.repository;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.linkfs.LinkFileSystem;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecraft.world.level.validation.DirectoryValidator;
import net.minecraft.world.level.validation.ForbiddenSymlinkInfo;
import org.slf4j.Logger;

public class FolderRepositorySource implements RepositorySource {
    static final Logger LOGGER = LogUtils.getLogger();
    private final Path folder;
    private final PackType packType;
    private final PackSource packSource;
    private final DirectoryValidator validator;

    public FolderRepositorySource(Path pFolder, PackType pPackType, PackSource pPackSource, DirectoryValidator pValidator) {
        this.folder = pFolder;
        this.packType = pPackType;
        this.packSource = pPackSource;
        this.validator = pValidator;
    }

    private static String nameFromPath(Path pPath) {
        return pPath.getFileName().toString();
    }

    @Override
    public void loadPacks(Consumer<Pack> pOnLoad) {
        try {
            FileUtil.createDirectoriesSafe(this.folder);
            discoverPacks(this.folder, this.validator, false, (p_248243_, p_248244_) -> {
                String s = nameFromPath(p_248243_);
                Pack pack = Pack.readMetaAndCreate("file/" + s, Component.literal(s), false, p_248244_, this.packType, Pack.Position.TOP, this.packSource);
                if (pack != null) {
                    pOnLoad.accept(pack);
                }
            });
        } catch (IOException ioexception) {
            LOGGER.warn("Failed to list packs in {}", this.folder, ioexception);
        }
    }

    public static void discoverPacks(Path pFolder, DirectoryValidator pValidator, boolean pIsBuiltin, BiConsumer<Path, Pack.ResourcesSupplier> pOutput) throws IOException {
        FolderRepositorySource.FolderPackDetector folderrepositorysource$folderpackdetector = new FolderRepositorySource.FolderPackDetector(
            pValidator, pIsBuiltin
        );

        try (DirectoryStream<Path> directorystream = Files.newDirectoryStream(pFolder)) {
            for(Path path : directorystream) {
                try {
                    List<ForbiddenSymlinkInfo> list = new ArrayList<>();
                    Pack.ResourcesSupplier pack$resourcessupplier = folderrepositorysource$folderpackdetector.detectPackResources(path, list);
                    if (!list.isEmpty()) {
                        LOGGER.warn("Ignoring potential pack entry: {}", ContentValidationException.getMessage(path, list));
                    } else if (pack$resourcessupplier != null) {
                        pOutput.accept(path, pack$resourcessupplier);
                    } else {
                        LOGGER.info("Found non-pack entry '{}', ignoring", path);
                    }
                } catch (IOException ioexception) {
                    LOGGER.warn("Failed to read properties of '{}', ignoring", path, ioexception);
                }
            }
        }
    }

    static class FolderPackDetector extends PackDetector<Pack.ResourcesSupplier> {
        private final boolean isBuiltin;

        protected FolderPackDetector(DirectoryValidator pValidator, boolean pIsBuiltin) {
            super(pValidator);
            this.isBuiltin = pIsBuiltin;
        }

        @Nullable
        protected Pack.ResourcesSupplier createZipPack(Path pPath) {
            FileSystem filesystem = pPath.getFileSystem();
            if (filesystem != FileSystems.getDefault() && !(filesystem instanceof LinkFileSystem)) {
                FolderRepositorySource.LOGGER.info("Can't open pack archive at {}", pPath);
                return null;
            } else {
                return new FilePackResources.FileResourcesSupplier(pPath, this.isBuiltin);
            }
        }

        protected Pack.ResourcesSupplier createDirectoryPack(Path pPath) {
            return new PathPackResources.PathResourcesSupplier(pPath, this.isBuiltin);
        }
    }
}
