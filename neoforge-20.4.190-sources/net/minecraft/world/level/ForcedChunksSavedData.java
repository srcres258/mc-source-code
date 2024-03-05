package net.minecraft.world.level;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class ForcedChunksSavedData extends SavedData {
    public static final String FILE_ID = "chunks";
    private static final String TAG_FORCED = "Forced";
    private final LongSet chunks;

    public static SavedData.Factory<ForcedChunksSavedData> factory() {
        return new SavedData.Factory<>(ForcedChunksSavedData::new, ForcedChunksSavedData::load, DataFixTypes.SAVED_DATA_FORCED_CHUNKS);
    }

    private ForcedChunksSavedData(LongSet pChunks) {
        this.chunks = pChunks;
    }

    public ForcedChunksSavedData() {
        this(new LongOpenHashSet());
    }

    public static ForcedChunksSavedData load(CompoundTag p_151484_) {
        ForcedChunksSavedData savedData = new ForcedChunksSavedData(new LongOpenHashSet(p_151484_.getLongArray("Forced")));
        net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.readModForcedChunks(p_151484_, savedData.blockForcedChunks, savedData.entityForcedChunks);
        return savedData;
    }

    /**
     * Used to save the {@code SavedData} to a {@code CompoundTag}
     *
     * @param pCompound the {@code CompoundTag} to save the {@code SavedData} to
     */
    @Override
    public CompoundTag save(CompoundTag pCompound) {
        pCompound.putLongArray("Forced", this.chunks.toLongArray());
        net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.writeModForcedChunks(pCompound, this.blockForcedChunks, this.entityForcedChunks);
        return pCompound;
    }

    public LongSet getChunks() {
        return this.chunks;
    }

    /* ======================================== FORGE START =====================================*/
    private final net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<net.minecraft.core.BlockPos> blockForcedChunks = new net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<>();
    private final net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<java.util.UUID> entityForcedChunks = new net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<>();

    public net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<net.minecraft.core.BlockPos> getBlockForcedChunks() {
        return this.blockForcedChunks;
    }

    public net.neoforged.neoforge.common.world.chunk.ForcedChunkManager.TicketTracker<java.util.UUID> getEntityForcedChunks() {
        return this.entityForcedChunks;
    }
}
