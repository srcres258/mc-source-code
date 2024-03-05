package net.minecraft.world.level.block.entity.trialspawner;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class TrialSpawner {
    public static final int DETECT_PLAYER_SPAWN_BUFFER = 40;
    private static final int MAX_MOB_TRACKING_DISTANCE = 47;
    private static final int MAX_MOB_TRACKING_DISTANCE_SQR = Mth.square(47);
    private static final float SPAWNING_AMBIENT_SOUND_CHANCE = 0.02F;
    private final TrialSpawnerConfig config;
    private final TrialSpawnerData data;
    private final TrialSpawner.StateAccessor stateAccessor;
    private PlayerDetector playerDetector;
    private boolean overridePeacefulAndMobSpawnRule;

    public Codec<TrialSpawner> codec() {
        return RecordCodecBuilder.create(
            p_312551_ -> p_312551_.group(
                        TrialSpawnerConfig.MAP_CODEC.forGetter(TrialSpawner::getConfig), TrialSpawnerData.MAP_CODEC.forGetter(TrialSpawner::getData)
                    )
                    .apply(p_312551_, (p_312253_, p_312636_) -> new TrialSpawner(p_312253_, p_312636_, this.stateAccessor, this.playerDetector))
        );
    }

    public TrialSpawner(TrialSpawner.StateAccessor pStateAccessor, PlayerDetector pPlayerDetector) {
        this(TrialSpawnerConfig.DEFAULT, new TrialSpawnerData(), pStateAccessor, pPlayerDetector);
    }

    public TrialSpawner(TrialSpawnerConfig pConfig, TrialSpawnerData pData, TrialSpawner.StateAccessor pStateAccessor, PlayerDetector pPlayerDetector) {
        this.config = pConfig;
        this.data = pData;
        this.data.setSpawnPotentialsFromConfig(pConfig);
        this.stateAccessor = pStateAccessor;
        this.playerDetector = pPlayerDetector;
    }

    public TrialSpawnerConfig getConfig() {
        return this.config;
    }

    public TrialSpawnerData getData() {
        return this.data;
    }

    public TrialSpawnerState getState() {
        return this.stateAccessor.getState();
    }

    public void setState(Level pLevel, TrialSpawnerState pState) {
        this.stateAccessor.setState(pLevel, pState);
    }

    public void markUpdated() {
        this.stateAccessor.markUpdated();
    }

    public PlayerDetector getPlayerDetector() {
        return this.playerDetector;
    }

    public boolean canSpawnInLevel(Level pLevel) {
        if (this.overridePeacefulAndMobSpawnRule) {
            return true;
        } else {
            return pLevel.getDifficulty() == Difficulty.PEACEFUL ? false : pLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        }
    }

    public Optional<UUID> spawnMob(ServerLevel pLevel, BlockPos pPos) {
        RandomSource randomsource = pLevel.getRandom();
        SpawnData spawndata = this.data.getOrCreateNextSpawnData(this, pLevel.getRandom());
        CompoundTag compoundtag = spawndata.entityToSpawn();
        ListTag listtag = compoundtag.getList("Pos", 6);
        Optional<EntityType<?>> optional = EntityType.by(compoundtag);
        if (optional.isEmpty()) {
            return Optional.empty();
        } else {
            int i = listtag.size();
            double d0 = i >= 1
                ? listtag.getDouble(0)
                : (double)pPos.getX() + (randomsource.nextDouble() - randomsource.nextDouble()) * (double)this.config.spawnRange() + 0.5;
            double d1 = i >= 2 ? listtag.getDouble(1) : (double)(pPos.getY() + randomsource.nextInt(3) - 1);
            double d2 = i >= 3
                ? listtag.getDouble(2)
                : (double)pPos.getZ() + (randomsource.nextDouble() - randomsource.nextDouble()) * (double)this.config.spawnRange() + 0.5;
            if (!pLevel.noCollision(optional.get().getAABB(d0, d1, d2))) {
                return Optional.empty();
            } else {
                Vec3 vec3 = new Vec3(d0, d1, d2);
                if (!inLineOfSight(pLevel, pPos.getCenter(), vec3)) {
                    return Optional.empty();
                } else {
                    BlockPos blockpos = BlockPos.containing(vec3);
                    if (!SpawnPlacements.checkSpawnRules(optional.get(), pLevel, MobSpawnType.TRIAL_SPAWNER, blockpos, pLevel.getRandom())) {
                        return Optional.empty();
                    } else {
                        Entity entity = EntityType.loadEntityRecursive(compoundtag, pLevel, p_312375_ -> {
                            p_312375_.moveTo(d0, d1, d2, randomsource.nextFloat() * 360.0F, 0.0F);
                            return p_312375_;
                        });
                        if (entity == null) {
                            return Optional.empty();
                        } else {
                            if (entity instanceof Mob mob) {
                                if (!mob.checkSpawnObstruction(pLevel)) {
                                    return Optional.empty();
                                }

                                if (spawndata.getEntityToSpawn().size() == 1 && spawndata.getEntityToSpawn().contains("id", 8)) {
                                    mob.finalizeSpawn(pLevel, pLevel.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.TRIAL_SPAWNER, null, null);
                                    mob.setPersistenceRequired();
                                }
                            }

                            if (!pLevel.tryAddFreshEntityWithPassengers(entity)) {
                                return Optional.empty();
                            } else {
                                pLevel.levelEvent(3011, pPos, 0);
                                pLevel.levelEvent(3012, blockpos, 0);
                                pLevel.gameEvent(entity, GameEvent.ENTITY_PLACE, blockpos);
                                return Optional.of(entity.getUUID());
                            }
                        }
                    }
                }
            }
        }
    }

    public void ejectReward(ServerLevel pLevel, BlockPos pPos, ResourceLocation pLootTable) {
        LootTable loottable = pLevel.getServer().getLootData().getLootTable(pLootTable);
        LootParams lootparams = new LootParams.Builder(pLevel).create(LootContextParamSets.EMPTY);
        ObjectArrayList<ItemStack> objectarraylist = loottable.getRandomItems(lootparams);
        if (!objectarraylist.isEmpty()) {
            for(ItemStack itemstack : objectarraylist) {
                DefaultDispenseItemBehavior.spawnItem(pLevel, itemstack, 2, Direction.UP, Vec3.atBottomCenterOf(pPos).relative(Direction.UP, 1.2));
            }

            pLevel.levelEvent(3014, pPos, 0);
        }
    }

    public void tickClient(Level pLevel, BlockPos pPos) {
        if (!this.canSpawnInLevel(pLevel)) {
            this.data.oSpin = this.data.spin;
        } else {
            TrialSpawnerState trialspawnerstate = this.getState();
            trialspawnerstate.emitParticles(pLevel, pPos);
            if (trialspawnerstate.hasSpinningMob()) {
                double d0 = (double)Math.max(0L, this.data.nextMobSpawnsAt - pLevel.getGameTime());
                this.data.oSpin = this.data.spin;
                this.data.spin = (this.data.spin + trialspawnerstate.spinningMobSpeed() / (d0 + 200.0)) % 360.0;
            }

            if (trialspawnerstate.isCapableOfSpawning()) {
                RandomSource randomsource = pLevel.getRandom();
                if (randomsource.nextFloat() <= 0.02F) {
                    pLevel.playLocalSound(
                        pPos,
                        SoundEvents.TRIAL_SPAWNER_AMBIENT,
                        SoundSource.BLOCKS,
                        randomsource.nextFloat() * 0.25F + 0.75F,
                        randomsource.nextFloat() + 0.5F,
                        false
                    );
                }
            }
        }
    }

    public void tickServer(ServerLevel pLevel, BlockPos pPos) {
        TrialSpawnerState trialspawnerstate = this.getState();
        if (!this.canSpawnInLevel(pLevel)) {
            if (trialspawnerstate.isCapableOfSpawning()) {
                this.data.reset();
                this.setState(pLevel, TrialSpawnerState.INACTIVE);
            }
        } else {
            if (this.data.currentMobs.removeIf(p_312870_ -> shouldMobBeUntracked(pLevel, pPos, p_312870_))) {
                this.data.nextMobSpawnsAt = pLevel.getGameTime() + (long)this.config.ticksBetweenSpawn();
            }

            TrialSpawnerState trialspawnerstate1 = trialspawnerstate.tickAndGetNext(pPos, this, pLevel);
            if (trialspawnerstate1 != trialspawnerstate) {
                this.setState(pLevel, trialspawnerstate1);
            }
        }
    }

    private static boolean shouldMobBeUntracked(ServerLevel pLevel, BlockPos pPos, UUID pUuid) {
        Entity entity = pLevel.getEntity(pUuid);
        return entity == null
            || !entity.isAlive()
            || !entity.level().dimension().equals(pLevel.dimension())
            || entity.blockPosition().distSqr(pPos) > (double)MAX_MOB_TRACKING_DISTANCE_SQR;
    }

    private static boolean inLineOfSight(Level pLevel, Vec3 pSpawnerPos, Vec3 pMobPos) {
        BlockHitResult blockhitresult = pLevel.clip(
            new ClipContext(pMobPos, pSpawnerPos, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, CollisionContext.empty())
        );
        return blockhitresult.getBlockPos().equals(BlockPos.containing(pSpawnerPos)) || blockhitresult.getType() == HitResult.Type.MISS;
    }

    public static void addSpawnParticles(Level pLevel, BlockPos pPos, RandomSource pRandom) {
        for(int i = 0; i < 20; ++i) {
            double d0 = (double)pPos.getX() + 0.5 + (pRandom.nextDouble() - 0.5) * 2.0;
            double d1 = (double)pPos.getY() + 0.5 + (pRandom.nextDouble() - 0.5) * 2.0;
            double d2 = (double)pPos.getZ() + 0.5 + (pRandom.nextDouble() - 0.5) * 2.0;
            pLevel.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
            pLevel.addParticle(ParticleTypes.FLAME, d0, d1, d2, 0.0, 0.0, 0.0);
        }
    }

    public static void addDetectPlayerParticles(Level pLevel, BlockPos pPos, RandomSource pRandom, int pData) {
        for(int i = 0; i < 30 + Math.min(pData, 10) * 5; ++i) {
            double d0 = (double)(2.0F * pRandom.nextFloat() - 1.0F) * 0.65;
            double d1 = (double)(2.0F * pRandom.nextFloat() - 1.0F) * 0.65;
            double d2 = (double)pPos.getX() + 0.5 + d0;
            double d3 = (double)pPos.getY() + 0.1 + (double)pRandom.nextFloat() * 0.8;
            double d4 = (double)pPos.getZ() + 0.5 + d1;
            pLevel.addParticle(ParticleTypes.TRIAL_SPAWNER_DETECTION, d2, d3, d4, 0.0, 0.0, 0.0);
        }
    }

    public static void addEjectItemParticles(Level pLevel, BlockPos pPos, RandomSource pRandom) {
        for(int i = 0; i < 20; ++i) {
            double d0 = (double)pPos.getX() + 0.4 + pRandom.nextDouble() * 0.2;
            double d1 = (double)pPos.getY() + 0.4 + pRandom.nextDouble() * 0.2;
            double d2 = (double)pPos.getZ() + 0.4 + pRandom.nextDouble() * 0.2;
            double d3 = pRandom.nextGaussian() * 0.02;
            double d4 = pRandom.nextGaussian() * 0.02;
            double d5 = pRandom.nextGaussian() * 0.02;
            pLevel.addParticle(ParticleTypes.SMALL_FLAME, d0, d1, d2, d3, d4, d5 * 0.25);
            pLevel.addParticle(ParticleTypes.SMOKE, d0, d1, d2, d3, d4, d5);
        }
    }

    @Deprecated(
        forRemoval = true
    )
    @VisibleForTesting
    public void setPlayerDetector(PlayerDetector pPlayerDetector) {
        this.playerDetector = pPlayerDetector;
    }

    @Deprecated(
        forRemoval = true
    )
    @VisibleForTesting
    public void overridePeacefulAndMobSpawnRule() {
        this.overridePeacefulAndMobSpawnRule = true;
    }

    public interface StateAccessor {
        void setState(Level pLevel, TrialSpawnerState pState);

        TrialSpawnerState getState();

        void markUpdated();
    }
}
