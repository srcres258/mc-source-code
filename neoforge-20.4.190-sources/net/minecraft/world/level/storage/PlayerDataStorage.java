package net.minecraft.world.level.storage;

import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

public class PlayerDataStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final File playerDir;
    protected final DataFixer fixerUpper;

    public PlayerDataStorage(LevelStorageSource.LevelStorageAccess pLevelStorageAccess, DataFixer pFixerUpper) {
        this.fixerUpper = pFixerUpper;
        this.playerDir = pLevelStorageAccess.getLevelPath(LevelResource.PLAYER_DATA_DIR).toFile();
        this.playerDir.mkdirs();
    }

    public void save(Player pPlayer) {
        try {
            CompoundTag compoundtag = pPlayer.saveWithoutId(new CompoundTag());
            Path path = this.playerDir.toPath();
            Path path1 = Files.createTempFile(path, pPlayer.getStringUUID() + "-", ".dat");
            NbtIo.writeCompressed(compoundtag, path1);
            Path path2 = path.resolve(pPlayer.getStringUUID() + ".dat");
            Path path3 = path.resolve(pPlayer.getStringUUID() + ".dat_old");
            Util.safeReplaceFile(path2, path1, path3);
            net.neoforged.neoforge.event.EventHooks.firePlayerSavingEvent(pPlayer, playerDir, pPlayer.getStringUUID());
        } catch (Exception exception) {
            LOGGER.warn("Failed to save player data for {}", pPlayer.getName().getString());
        }
    }

    @Nullable
    public CompoundTag load(Player pPlayer) {
        CompoundTag compoundtag = null;

        try {
            File file1 = new File(this.playerDir, pPlayer.getStringUUID() + ".dat");
            if (file1.exists() && file1.isFile()) {
                compoundtag = NbtIo.readCompressed(file1.toPath(), NbtAccounter.unlimitedHeap());
            }
        } catch (Exception exception) {
            LOGGER.warn("Failed to load player data for {}", pPlayer.getName().getString());
        }

        if (compoundtag != null) {
            int i = NbtUtils.getDataVersion(compoundtag, -1);
            compoundtag = DataFixTypes.PLAYER.updateToCurrentVersion(this.fixerUpper, compoundtag, i);
            pPlayer.load(compoundtag);
        }
        net.neoforged.neoforge.event.EventHooks.firePlayerLoadingEvent(pPlayer, playerDir, pPlayer.getStringUUID());

        return compoundtag;
    }

    public String[] getSeenPlayers() {
        String[] astring = this.playerDir.list();
        if (astring == null) {
            astring = new String[0];
        }

        for(int i = 0; i < astring.length; ++i) {
            if (astring[i].endsWith(".dat")) {
                astring[i] = astring[i].substring(0, astring[i].length() - 4);
            }
        }

        return astring;
    }

    public File getPlayerDataFolder() {
        return playerDir;
    }
}
