package net.minecraft.world.level.block.entity.trialspawner;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public enum TrialSpawnerState implements StringRepresentable {
    INACTIVE("inactive", 0, TrialSpawnerState.ParticleEmission.NONE, -1.0, false),
    WAITING_FOR_PLAYERS("waiting_for_players", 4, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, 200.0, true),
    ACTIVE("active", 8, TrialSpawnerState.ParticleEmission.FLAMES_AND_SMOKE, 1000.0, true),
    WAITING_FOR_REWARD_EJECTION("waiting_for_reward_ejection", 8, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, -1.0, false),
    EJECTING_REWARD("ejecting_reward", 8, TrialSpawnerState.ParticleEmission.SMALL_FLAMES, -1.0, false),
    COOLDOWN("cooldown", 0, TrialSpawnerState.ParticleEmission.SMOKE_INSIDE_AND_TOP_FACE, -1.0, false);

    private static final float DELAY_BEFORE_EJECT_AFTER_KILLING_LAST_MOB = 40.0F;
    private static final int TIME_BETWEEN_EACH_EJECTION = Mth.floor(30.0F);
    private final String name;
    private final int lightLevel;
    private final double spinningMobSpeed;
    private final TrialSpawnerState.ParticleEmission particleEmission;
    private final boolean isCapableOfSpawning;

    private TrialSpawnerState(String pName, int pLightLevel, TrialSpawnerState.ParticleEmission pParticleEmission, double pSpinningMobSpeed, boolean pIsCapableOfSpawning) {
        this.name = pName;
        this.lightLevel = pLightLevel;
        this.particleEmission = pParticleEmission;
        this.spinningMobSpeed = pSpinningMobSpeed;
        this.isCapableOfSpawning = pIsCapableOfSpawning;
    }

    TrialSpawnerState tickAndGetNext(BlockPos pPos, TrialSpawner pSpawner, ServerLevel pLevel) {
        TrialSpawnerData trialspawnerdata = pSpawner.getData();
        TrialSpawnerConfig trialspawnerconfig = pSpawner.getConfig();
        PlayerDetector playerdetector = pSpawner.getPlayerDetector();
        TrialSpawnerState trialspawnerstate;
        switch(this) {
            case INACTIVE:
                trialspawnerstate = trialspawnerdata.getOrCreateDisplayEntity(pSpawner, pLevel, WAITING_FOR_PLAYERS) == null ? this : WAITING_FOR_PLAYERS;
                break;
            case WAITING_FOR_PLAYERS:
                if (!trialspawnerdata.hasMobToSpawn()) {
                    trialspawnerstate = INACTIVE;
                } else {
                    trialspawnerdata.tryDetectPlayers(pLevel, pPos, playerdetector, trialspawnerconfig.requiredPlayerRange());
                    trialspawnerstate = trialspawnerdata.detectedPlayers.isEmpty() ? this : ACTIVE;
                }
                break;
            case ACTIVE:
                if (!trialspawnerdata.hasMobToSpawn()) {
                    trialspawnerstate = INACTIVE;
                } else {
                    int i = trialspawnerdata.countAdditionalPlayers(pPos);
                    trialspawnerdata.tryDetectPlayers(pLevel, pPos, playerdetector, trialspawnerconfig.requiredPlayerRange());
                    if (trialspawnerdata.hasFinishedSpawningAllMobs(trialspawnerconfig, i)) {
                        if (trialspawnerdata.haveAllCurrentMobsDied()) {
                            trialspawnerdata.cooldownEndsAt = pLevel.getGameTime() + (long)trialspawnerconfig.targetCooldownLength();
                            trialspawnerdata.totalMobsSpawned = 0;
                            trialspawnerdata.nextMobSpawnsAt = 0L;
                            trialspawnerstate = WAITING_FOR_REWARD_EJECTION;
                            break;
                        }
                    } else if (trialspawnerdata.isReadyToSpawnNextMob(pLevel, trialspawnerconfig, i)) {
                        pSpawner.spawnMob(pLevel, pPos).ifPresent(p_313658_ -> {
                            trialspawnerdata.currentMobs.add(p_313658_);
                            ++trialspawnerdata.totalMobsSpawned;
                            trialspawnerdata.nextMobSpawnsAt = pLevel.getGameTime() + (long)trialspawnerconfig.ticksBetweenSpawn();
                            trialspawnerdata.spawnPotentials.getRandom(pLevel.getRandom()).ifPresent(p_312310_ -> {
                                trialspawnerdata.nextSpawnData = Optional.of(p_312310_.getData());
                                pSpawner.markUpdated();
                            });
                        });
                    }

                    trialspawnerstate = this;
                }
                break;
            case WAITING_FOR_REWARD_EJECTION:
                if (trialspawnerdata.isReadyToOpenShutter(pLevel, trialspawnerconfig, 40.0F)) {
                    pLevel.playSound(null, pPos, SoundEvents.TRIAL_SPAWNER_OPEN_SHUTTER, SoundSource.BLOCKS);
                    trialspawnerstate = EJECTING_REWARD;
                } else {
                    trialspawnerstate = this;
                }
                break;
            case EJECTING_REWARD:
                if (!trialspawnerdata.isReadyToEjectItems(pLevel, trialspawnerconfig, (float)TIME_BETWEEN_EACH_EJECTION)) {
                    trialspawnerstate = this;
                } else if (trialspawnerdata.detectedPlayers.isEmpty()) {
                    pLevel.playSound(null, pPos, SoundEvents.TRIAL_SPAWNER_CLOSE_SHUTTER, SoundSource.BLOCKS);
                    trialspawnerdata.ejectingLootTable = Optional.empty();
                    trialspawnerstate = COOLDOWN;
                } else {
                    if (trialspawnerdata.ejectingLootTable.isEmpty()) {
                        trialspawnerdata.ejectingLootTable = trialspawnerconfig.lootTablesToEject().getRandomValue(pLevel.getRandom());
                    }

                    trialspawnerdata.ejectingLootTable.ifPresent(p_312164_ -> pSpawner.ejectReward(pLevel, pPos, p_312164_));
                    trialspawnerdata.detectedPlayers.remove(trialspawnerdata.detectedPlayers.iterator().next());
                    trialspawnerstate = this;
                }
                break;
            case COOLDOWN:
                if (trialspawnerdata.isCooldownFinished(pLevel)) {
                    trialspawnerdata.cooldownEndsAt = 0L;
                    trialspawnerstate = WAITING_FOR_PLAYERS;
                } else {
                    trialspawnerstate = this;
                }
                break;
            default:
                throw new IncompatibleClassChangeError();
        }

        return trialspawnerstate;
    }

    public int lightLevel() {
        return this.lightLevel;
    }

    public double spinningMobSpeed() {
        return this.spinningMobSpeed;
    }

    public boolean hasSpinningMob() {
        return this.spinningMobSpeed >= 0.0;
    }

    public boolean isCapableOfSpawning() {
        return this.isCapableOfSpawning;
    }

    public void emitParticles(Level pLevel, BlockPos pPos) {
        this.particleEmission.emit(pLevel, pLevel.getRandom(), pPos);
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    static class LightLevel {
        private static final int UNLIT = 0;
        private static final int HALF_LIT = 4;
        private static final int LIT = 8;

        private LightLevel() {
        }
    }

    interface ParticleEmission {
        TrialSpawnerState.ParticleEmission NONE = (p_311998_, p_311983_, p_312351_) -> {
        };
        TrialSpawnerState.ParticleEmission SMALL_FLAMES = (p_312393_, p_312483_, p_312524_) -> {
            if (p_312483_.nextInt(2) == 0) {
                Vec3 vec3 = p_312524_.getCenter().offsetRandom(p_312483_, 0.9F);
                addParticle(ParticleTypes.SMALL_FLAME, vec3, p_312393_);
            }
        };
        TrialSpawnerState.ParticleEmission FLAMES_AND_SMOKE = (p_311792_, p_312092_, p_312591_) -> {
            Vec3 vec3 = p_312591_.getCenter().offsetRandom(p_312092_, 1.0F);
            addParticle(ParticleTypes.SMOKE, vec3, p_311792_);
            addParticle(ParticleTypes.FLAME, vec3, p_311792_);
        };
        TrialSpawnerState.ParticleEmission SMOKE_INSIDE_AND_TOP_FACE = (p_311899_, p_311762_, p_312096_) -> {
            Vec3 vec3 = p_312096_.getCenter().offsetRandom(p_311762_, 0.9F);
            if (p_311762_.nextInt(3) == 0) {
                addParticle(ParticleTypes.SMOKE, vec3, p_311899_);
            }

            if (p_311899_.getGameTime() % 20L == 0L) {
                Vec3 vec31 = p_312096_.getCenter().add(0.0, 0.5, 0.0);
                int i = p_311899_.getRandom().nextInt(4) + 20;

                for(int j = 0; j < i; ++j) {
                    addParticle(ParticleTypes.SMOKE, vec31, p_311899_);
                }
            }
        };

        private static void addParticle(SimpleParticleType pParticleType, Vec3 pPos, Level pLevel) {
            pLevel.addParticle(pParticleType, pPos.x(), pPos.y(), pPos.z(), 0.0, 0.0, 0.0);
        }

        void emit(Level pLevel, RandomSource pRandom, BlockPos pPos);
    }

    static class SpinningMob {
        private static final double NONE = -1.0;
        private static final double SLOW = 200.0;
        private static final double FAST = 1000.0;

        private SpinningMob() {
        }
    }
}
