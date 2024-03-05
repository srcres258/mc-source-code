package net.minecraft.world.entity.animal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.VariantHolder;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SuspiciousStewItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public class MushroomCow extends Cow implements Shearable, VariantHolder<MushroomCow.MushroomType> {
    private static final EntityDataAccessor<String> DATA_TYPE = SynchedEntityData.defineId(MushroomCow.class, EntityDataSerializers.STRING);
    private static final int MUTATE_CHANCE = 1024;
    private static final String TAG_STEW_EFFECTS = "stew_effects";
    @Nullable
    private List<SuspiciousEffectHolder.EffectEntry> stewEffects;
    /**
     * Stores the UUID of the most recent lightning bolt to strike
     */
    @Nullable
    private UUID lastLightningBoltUUID;

    public MushroomCow(EntityType<? extends MushroomCow> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Override
    public float getWalkTargetValue(BlockPos pPos, LevelReader pLevel) {
        return pLevel.getBlockState(pPos.below()).is(Blocks.MYCELIUM) ? 10.0F : pLevel.getPathfindingCostFromLightLevels(pPos);
    }

    public static boolean checkMushroomSpawnRules(
        EntityType<MushroomCow> pMushroomCow, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandomSource
    ) {
        return pLevel.getBlockState(pPos.below()).is(BlockTags.MOOSHROOMS_SPAWNABLE_ON) && isBrightEnoughToSpawn(pLevel, pPos);
    }

    @Override
    public void thunderHit(ServerLevel pLevel, LightningBolt pLightning) {
        UUID uuid = pLightning.getUUID();
        if (!uuid.equals(this.lastLightningBoltUUID)) {
            this.setVariant(this.getVariant() == MushroomCow.MushroomType.RED ? MushroomCow.MushroomType.BROWN : MushroomCow.MushroomType.RED);
            this.lastLightningBoltUUID = uuid;
            this.playSound(SoundEvents.MOOSHROOM_CONVERT, 2.0F, 1.0F);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_TYPE, MushroomCow.MushroomType.RED.type);
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (itemstack.is(Items.BOWL) && !this.isBaby()) {
            boolean flag = false;
            ItemStack itemstack2;
            if (this.stewEffects != null) {
                flag = true;
                itemstack2 = new ItemStack(Items.SUSPICIOUS_STEW);
                SuspiciousStewItem.saveMobEffects(itemstack2, this.stewEffects);
                this.stewEffects = null;
            } else {
                itemstack2 = new ItemStack(Items.MUSHROOM_STEW);
            }

            ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, pPlayer, itemstack2, false);
            pPlayer.setItemInHand(pHand, itemstack1);
            SoundEvent soundevent;
            if (flag) {
                soundevent = SoundEvents.MOOSHROOM_MILK_SUSPICIOUSLY;
            } else {
                soundevent = SoundEvents.MOOSHROOM_MILK;
            }

            this.playSound(soundevent, 1.0F, 1.0F);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else if (false && itemstack.getItem() == Items.SHEARS && this.readyForShearing()) { //Forge: Moved to onSheared
            this.shear(SoundSource.PLAYERS);
            this.gameEvent(GameEvent.SHEAR, pPlayer);
            if (!this.level().isClientSide) {
                itemstack.hurtAndBreak(1, pPlayer, p_28927_ -> p_28927_.broadcastBreakEvent(pHand));
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else if (this.getVariant() == MushroomCow.MushroomType.BROWN && itemstack.is(ItemTags.SMALL_FLOWERS)) {
            if (this.stewEffects != null) {
                for(int i = 0; i < 2; ++i) {
                    this.level()
                        .addParticle(
                            ParticleTypes.SMOKE,
                            this.getX() + this.random.nextDouble() / 2.0,
                            this.getY(0.5),
                            this.getZ() + this.random.nextDouble() / 2.0,
                            0.0,
                            this.random.nextDouble() / 5.0,
                            0.0
                        );
                }
            } else {
                Optional<List<SuspiciousEffectHolder.EffectEntry>> optional = this.getEffectsFromItemStack(itemstack);
                if (optional.isEmpty()) {
                    return InteractionResult.PASS;
                }

                if (!pPlayer.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }

                for(int j = 0; j < 4; ++j) {
                    this.level()
                        .addParticle(
                            ParticleTypes.EFFECT,
                            this.getX() + this.random.nextDouble() / 2.0,
                            this.getY(0.5),
                            this.getZ() + this.random.nextDouble() / 2.0,
                            0.0,
                            this.random.nextDouble() / 5.0,
                            0.0
                        );
                }

                this.stewEffects = optional.get();
                this.playSound(SoundEvents.MOOSHROOM_EAT, 2.0F, 1.0F);
            }

            return InteractionResult.sidedSuccess(this.level().isClientSide);
        } else {
            return super.mobInteract(pPlayer, pHand);
        }
    }

    @Override
    public void shear(SoundSource pCategory) {
        this.level().playSound(null, this, SoundEvents.MOOSHROOM_SHEAR, pCategory, 1.0F, 1.0F);
        if (!this.level().isClientSide()) {
            if (!net.neoforged.neoforge.event.EventHooks.canLivingConvert(this, EntityType.COW, (timer) -> {})) return;
            Cow cow = EntityType.COW.create(this.level());
            if (cow != null) {
                net.neoforged.neoforge.event.EventHooks.onLivingConvert(this, cow);
                ((ServerLevel)this.level()).sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
                this.discard();
                cow.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
                cow.setHealth(this.getHealth());
                cow.yBodyRot = this.yBodyRot;
                if (this.hasCustomName()) {
                    cow.setCustomName(this.getCustomName());
                    cow.setCustomNameVisible(this.isCustomNameVisible());
                }

                if (this.isPersistenceRequired()) {
                    cow.setPersistenceRequired();
                }

                cow.setInvulnerable(this.isInvulnerable());
                this.level().addFreshEntity(cow);

                for(int i = 0; i < 5; ++i) {
                    //Neo: Change from addFreshEntity to spawnAtLocation to ensure captureDrops can capture this, we also need to unset the default pickup delay from the item
                    ItemEntity item = spawnAtLocation(new ItemStack(this.getVariant().blockState.getBlock()), getBbHeight());
                    if (item != null) item.setNoPickUpDelay();
                }
            }
        }
    }

    @Override
    public boolean readyForShearing() {
        return this.isAlive() && !this.isBaby();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putString("Type", this.getVariant().getSerializedName());
        if (this.stewEffects != null) {
            SuspiciousEffectHolder.EffectEntry.LIST_CODEC
                .encodeStart(NbtOps.INSTANCE, this.stewEffects)
                .result()
                .ifPresent(p_297973_ -> pCompound.put("stew_effects", p_297973_));
        }
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setVariant(MushroomCow.MushroomType.byType(pCompound.getString("Type")));
        if (pCompound.contains("stew_effects", 9)) {
            SuspiciousEffectHolder.EffectEntry.LIST_CODEC
                .parse(NbtOps.INSTANCE, pCompound.get("stew_effects"))
                .result()
                .ifPresent(p_297971_ -> this.stewEffects = p_297971_);
        }
    }

    private Optional<List<SuspiciousEffectHolder.EffectEntry>> getEffectsFromItemStack(ItemStack pStack) {
        SuspiciousEffectHolder suspiciouseffectholder = SuspiciousEffectHolder.tryGet(pStack.getItem());
        return suspiciouseffectholder != null ? Optional.of(suspiciouseffectholder.getSuspiciousEffects()) : Optional.empty();
    }

    public void setVariant(MushroomCow.MushroomType pVariant) {
        this.entityData.set(DATA_TYPE, pVariant.type);
    }

    public MushroomCow.MushroomType getVariant() {
        return MushroomCow.MushroomType.byType(this.entityData.get(DATA_TYPE));
    }

    @Nullable
    public MushroomCow getBreedOffspring(ServerLevel pLevel, AgeableMob pOtherParent) {
        MushroomCow mushroomcow = EntityType.MOOSHROOM.create(pLevel);
        if (mushroomcow != null) {
            mushroomcow.setVariant(this.getOffspringType((MushroomCow)pOtherParent));
        }

        return mushroomcow;
    }

    private MushroomCow.MushroomType getOffspringType(MushroomCow pMate) {
        MushroomCow.MushroomType mushroomcow$mushroomtype = this.getVariant();
        MushroomCow.MushroomType mushroomcow$mushroomtype1 = pMate.getVariant();
        MushroomCow.MushroomType mushroomcow$mushroomtype2;
        if (mushroomcow$mushroomtype == mushroomcow$mushroomtype1 && this.random.nextInt(1024) == 0) {
            mushroomcow$mushroomtype2 = mushroomcow$mushroomtype == MushroomCow.MushroomType.BROWN
                ? MushroomCow.MushroomType.RED
                : MushroomCow.MushroomType.BROWN;
        } else {
            mushroomcow$mushroomtype2 = this.random.nextBoolean() ? mushroomcow$mushroomtype : mushroomcow$mushroomtype1;
        }

        return mushroomcow$mushroomtype2;
    }

    public static enum MushroomType implements StringRepresentable {
        RED("red", Blocks.RED_MUSHROOM.defaultBlockState()),
        BROWN("brown", Blocks.BROWN_MUSHROOM.defaultBlockState());

        public static final StringRepresentable.EnumCodec<MushroomCow.MushroomType> CODEC = StringRepresentable.fromEnum(MushroomCow.MushroomType::values);
        final String type;
        final BlockState blockState;

        private MushroomType(String pType, BlockState pBlockState) {
            this.type = pType;
            this.blockState = pBlockState;
        }

        /**
         * A block state that is rendered on the back of the mooshroom.
         */
        public BlockState getBlockState() {
            return this.blockState;
        }

        @Override
        public String getSerializedName() {
            return this.type;
        }

        static MushroomCow.MushroomType byType(String pName) {
            return CODEC.byName(pName, RED);
        }
    }
}
