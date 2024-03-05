package net.minecraft.world.level.levelgen.structure;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

public class StructureFeatureIndexSavedData extends SavedData {
    private static final String TAG_REMAINING_INDEXES = "Remaining";
    private static final String TAG_All_INDEXES = "All";
    private final LongSet all;
    private final LongSet remaining;

    public static SavedData.Factory<StructureFeatureIndexSavedData> factory() {
        return new SavedData.Factory<>(
            StructureFeatureIndexSavedData::new, StructureFeatureIndexSavedData::load, DataFixTypes.SAVED_DATA_STRUCTURE_FEATURE_INDICES
        );
    }

    private StructureFeatureIndexSavedData(LongSet pAll, LongSet pRemaining) {
        this.all = pAll;
        this.remaining = pRemaining;
    }

    public StructureFeatureIndexSavedData() {
        this(new LongOpenHashSet(), new LongOpenHashSet());
    }

    public static StructureFeatureIndexSavedData load(CompoundTag p_163535_) {
        return new StructureFeatureIndexSavedData(new LongOpenHashSet(p_163535_.getLongArray("All")), new LongOpenHashSet(p_163535_.getLongArray("Remaining")));
    }

    /**
     * Used to save the {@code SavedData} to a {@code CompoundTag}
     *
     * @param pCompoundTag the {@code CompoundTag} to save the {@code SavedData} to
     */
    @Override
    public CompoundTag save(CompoundTag pCompoundTag) {
        pCompoundTag.putLongArray("All", this.all.toLongArray());
        pCompoundTag.putLongArray("Remaining", this.remaining.toLongArray());
        return pCompoundTag;
    }

    public void addIndex(long pIndex) {
        this.all.add(pIndex);
        this.remaining.add(pIndex);
    }

    public boolean hasStartIndex(long pIndex) {
        return this.all.contains(pIndex);
    }

    public boolean hasUnhandledIndex(long pIndex) {
        return this.remaining.contains(pIndex);
    }

    public void removeIndex(long pIndex) {
        this.remaining.remove(pIndex);
    }

    public LongSet getAll() {
        return this.all;
    }
}
