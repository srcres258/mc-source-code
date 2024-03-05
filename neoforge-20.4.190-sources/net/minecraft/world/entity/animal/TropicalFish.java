package net.minecraft.world.entity.animal;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

public class TropicalFish extends AbstractSchoolingFish implements VariantHolder<TropicalFish.Pattern> {
    public static final String BUCKET_VARIANT_TAG = "BucketVariantTag";
    private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(TropicalFish.class, EntityDataSerializers.INT);
    public static final List<TropicalFish.Variant> COMMON_VARIANTS = List.of(
        new TropicalFish.Variant(TropicalFish.Pattern.STRIPEY, DyeColor.ORANGE, DyeColor.GRAY),
        new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.GRAY),
        new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.BLUE),
        new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.GRAY),
        new TropicalFish.Variant(TropicalFish.Pattern.SUNSTREAK, DyeColor.BLUE, DyeColor.GRAY),
        new TropicalFish.Variant(TropicalFish.Pattern.KOB, DyeColor.ORANGE, DyeColor.WHITE),
        new TropicalFish.Variant(TropicalFish.Pattern.SPOTTY, DyeColor.PINK, DyeColor.LIGHT_BLUE),
        new TropicalFish.Variant(TropicalFish.Pattern.BLOCKFISH, DyeColor.PURPLE, DyeColor.YELLOW),
        new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.RED),
        new TropicalFish.Variant(TropicalFish.Pattern.SPOTTY, DyeColor.WHITE, DyeColor.YELLOW),
        new TropicalFish.Variant(TropicalFish.Pattern.GLITTER, DyeColor.WHITE, DyeColor.GRAY),
        new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.ORANGE),
        new TropicalFish.Variant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.PINK),
        new TropicalFish.Variant(TropicalFish.Pattern.BRINELY, DyeColor.LIME, DyeColor.LIGHT_BLUE),
        new TropicalFish.Variant(TropicalFish.Pattern.BETTY, DyeColor.RED, DyeColor.WHITE),
        new TropicalFish.Variant(TropicalFish.Pattern.SNOOPER, DyeColor.GRAY, DyeColor.RED),
        new TropicalFish.Variant(TropicalFish.Pattern.BLOCKFISH, DyeColor.RED, DyeColor.WHITE),
        new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.WHITE, DyeColor.YELLOW),
        new TropicalFish.Variant(TropicalFish.Pattern.KOB, DyeColor.RED, DyeColor.WHITE),
        new TropicalFish.Variant(TropicalFish.Pattern.SUNSTREAK, DyeColor.GRAY, DyeColor.WHITE),
        new TropicalFish.Variant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.YELLOW),
        new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.YELLOW, DyeColor.YELLOW)
    );
    private boolean isSchool = true;

    public TropicalFish(EntityType<? extends TropicalFish> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public static String getPredefinedName(int pVariantId) {
        return "entity.minecraft.tropical_fish.predefined." + pVariantId;
    }

    static int packVariant(TropicalFish.Pattern pPattern, DyeColor pBaseColor, DyeColor pPatternColor) {
        return pPattern.getPackedId() & 65535 | (pBaseColor.getId() & 0xFF) << 16 | (pPatternColor.getId() & 0xFF) << 24;
    }

    public static DyeColor getBaseColor(int pVariantId) {
        return DyeColor.byId(pVariantId >> 16 & 0xFF);
    }

    public static DyeColor getPatternColor(int pVariantId) {
        return DyeColor.byId(pVariantId >> 24 & 0xFF);
    }

    public static TropicalFish.Pattern getPattern(int pVariantId) {
        return TropicalFish.Pattern.byId(pVariantId & 65535);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_ID_TYPE_VARIANT, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Variant", this.getPackedVariant());
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setPackedVariant(pCompound.getInt("Variant"));
    }

    private void setPackedVariant(int pPackedVariant) {
        this.entityData.set(DATA_ID_TYPE_VARIANT, pPackedVariant);
    }

    @Override
    public boolean isMaxGroupSizeReached(int pSize) {
        return !this.isSchool;
    }

    private int getPackedVariant() {
        return this.entityData.get(DATA_ID_TYPE_VARIANT);
    }

    public DyeColor getBaseColor() {
        return getBaseColor(this.getPackedVariant());
    }

    public DyeColor getPatternColor() {
        return getPatternColor(this.getPackedVariant());
    }

    public TropicalFish.Pattern getVariant() {
        return getPattern(this.getPackedVariant());
    }

    public void setVariant(TropicalFish.Pattern pVariant) {
        int i = this.getPackedVariant();
        DyeColor dyecolor = getBaseColor(i);
        DyeColor dyecolor1 = getPatternColor(i);
        this.setPackedVariant(packVariant(pVariant, dyecolor, dyecolor1));
    }

    @Override
    public void saveToBucketTag(ItemStack pStack) {
        super.saveToBucketTag(pStack);
        CompoundTag compoundtag = pStack.getOrCreateTag();
        compoundtag.putInt("BucketVariantTag", this.getPackedVariant());
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.TROPICAL_FISH_BUCKET);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TROPICAL_FISH_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.TROPICAL_FISH_FLOP;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(
        ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag
    ) {
        pSpawnData = super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
        if (pReason == MobSpawnType.BUCKET && pDataTag != null && pDataTag.contains("BucketVariantTag", 3)) {
            this.setPackedVariant(pDataTag.getInt("BucketVariantTag"));
            return pSpawnData;
        } else {
            RandomSource randomsource = pLevel.getRandom();
            TropicalFish.Variant tropicalfish$variant;
            if (pSpawnData instanceof TropicalFish.TropicalFishGroupData tropicalfish$tropicalfishgroupdata) {
                tropicalfish$variant = tropicalfish$tropicalfishgroupdata.variant;
            } else if ((double)randomsource.nextFloat() < 0.9) {
                tropicalfish$variant = Util.getRandom(COMMON_VARIANTS, randomsource);
                pSpawnData = new TropicalFish.TropicalFishGroupData(this, tropicalfish$variant);
            } else {
                this.isSchool = false;
                TropicalFish.Pattern[] atropicalfish$pattern = TropicalFish.Pattern.values();
                DyeColor[] adyecolor = DyeColor.values();
                TropicalFish.Pattern tropicalfish$pattern = Util.getRandom(atropicalfish$pattern, randomsource);
                DyeColor dyecolor = Util.getRandom(adyecolor, randomsource);
                DyeColor dyecolor1 = Util.getRandom(adyecolor, randomsource);
                tropicalfish$variant = new TropicalFish.Variant(tropicalfish$pattern, dyecolor, dyecolor1);
            }

            this.setPackedVariant(tropicalfish$variant.getPackedId());
            return pSpawnData;
        }
    }

    public static boolean checkTropicalFishSpawnRules(
        EntityType<TropicalFish> pTropicalFish, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom
    ) {
        return pLevel.getFluidState(pPos.below()).is(FluidTags.WATER)
            && pLevel.getBlockState(pPos.above()).is(Blocks.WATER)
            && (
                pLevel.getBiome(pPos).is(BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT)
                    || WaterAnimal.checkSurfaceWaterAnimalSpawnRules(pTropicalFish, pLevel, pSpawnType, pPos, pRandom)
            );
    }

    public static enum Base {
        SMALL(0),
        LARGE(1);

        final int id;

        private Base(int pId) {
            this.id = pId;
        }
    }

    public static enum Pattern implements StringRepresentable {
        KOB("kob", TropicalFish.Base.SMALL, 0),
        SUNSTREAK("sunstreak", TropicalFish.Base.SMALL, 1),
        SNOOPER("snooper", TropicalFish.Base.SMALL, 2),
        DASHER("dasher", TropicalFish.Base.SMALL, 3),
        BRINELY("brinely", TropicalFish.Base.SMALL, 4),
        SPOTTY("spotty", TropicalFish.Base.SMALL, 5),
        FLOPPER("flopper", TropicalFish.Base.LARGE, 0),
        STRIPEY("stripey", TropicalFish.Base.LARGE, 1),
        GLITTER("glitter", TropicalFish.Base.LARGE, 2),
        BLOCKFISH("blockfish", TropicalFish.Base.LARGE, 3),
        BETTY("betty", TropicalFish.Base.LARGE, 4),
        CLAYFISH("clayfish", TropicalFish.Base.LARGE, 5);

        public static final Codec<TropicalFish.Pattern> CODEC = StringRepresentable.fromEnum(TropicalFish.Pattern::values);
        private static final IntFunction<TropicalFish.Pattern> BY_ID = ByIdMap.sparse(TropicalFish.Pattern::getPackedId, values(), KOB);
        private final String name;
        private final Component displayName;
        private final TropicalFish.Base base;
        private final int packedId;

        private Pattern(String pName, TropicalFish.Base pBase, int pId) {
            this.name = pName;
            this.base = pBase;
            this.packedId = pBase.id | pId << 8;
            this.displayName = Component.translatable("entity.minecraft.tropical_fish.type." + this.name);
        }

        public static TropicalFish.Pattern byId(int pPackedId) {
            return BY_ID.apply(pPackedId);
        }

        public TropicalFish.Base base() {
            return this.base;
        }

        public int getPackedId() {
            return this.packedId;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public Component displayName() {
            return this.displayName;
        }
    }

    static class TropicalFishGroupData extends AbstractSchoolingFish.SchoolSpawnGroupData {
        final TropicalFish.Variant variant;

        TropicalFishGroupData(TropicalFish pLeader, TropicalFish.Variant pVariant) {
            super(pLeader);
            this.variant = pVariant;
        }
    }

    public static record Variant(TropicalFish.Pattern pattern, DyeColor baseColor, DyeColor patternColor) {
        public int getPackedId() {
            return TropicalFish.packVariant(this.pattern, this.baseColor, this.patternColor);
        }
    }
}
