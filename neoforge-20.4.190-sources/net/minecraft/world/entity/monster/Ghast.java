package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Ghast extends FlyingMob implements Enemy {
    private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING = SynchedEntityData.defineId(Ghast.class, EntityDataSerializers.BOOLEAN);
    private int explosionPower = 1;

    public Ghast(EntityType<? extends Ghast> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.xpReward = 5;
        this.moveControl = new Ghast.GhastMoveControl(this);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new Ghast.RandomFloatAroundGoal(this));
        this.goalSelector.addGoal(7, new Ghast.GhastLookGoal(this));
        this.goalSelector.addGoal(7, new Ghast.GhastShootFireballGoal(this));
        this.targetSelector
            .addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, p_311682_ -> Math.abs(p_311682_.getY() - this.getY()) <= 4.0));
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_IS_CHARGING);
    }

    public void setCharging(boolean pCharging) {
        this.entityData.set(DATA_IS_CHARGING, pCharging);
    }

    public int getExplosionPower() {
        return this.explosionPower;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return true;
    }

    private static boolean isReflectedFireball(DamageSource pDamageSource) {
        return pDamageSource.getDirectEntity() instanceof LargeFireball && pDamageSource.getEntity() instanceof Player;
    }

    /**
     * Returns whether this Entity is invulnerable to the given DamageSource.
     */
    @Override
    public boolean isInvulnerableTo(DamageSource pSource) {
        return !isReflectedFireball(pSource) && super.isInvulnerableTo(pSource);
    }

    /**
     * Called when the entity is attacked.
     */
    @Override
    public boolean hurt(DamageSource pSource, float pAmount) {
        if (isReflectedFireball(pSource)) {
            super.hurt(pSource, 1000.0F);
            return true;
        } else {
            return this.isInvulnerableTo(pSource) ? false : super.hurt(pSource, pAmount);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_CHARGING, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.FOLLOW_RANGE, 100.0);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.GHAST_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.GHAST_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GHAST_DEATH;
    }

    /**
     * Returns the volume for the sounds this mob makes.
     */
    @Override
    protected float getSoundVolume() {
        return 5.0F;
    }

    public static boolean checkGhastSpawnRules(
        EntityType<Ghast> pGhast, LevelAccessor pLevel, MobSpawnType pSpawnType, BlockPos pPos, RandomSource pRandom
    ) {
        return pLevel.getDifficulty() != Difficulty.PEACEFUL
            && pRandom.nextInt(20) == 0
            && checkMobSpawnRules(pGhast, pLevel, pSpawnType, pPos, pRandom);
    }

    /**
     * Will return how many at most can spawn in a chunk at once.
     */
    @Override
    public int getMaxSpawnClusterSize() {
        return 1;
    }

    @Override
    protected Vector3f getPassengerAttachmentPoint(Entity pEntity, EntityDimensions pDimensions, float pScale) {
        return new Vector3f(0.0F, pDimensions.height + 0.0625F * pScale, 0.0F);
    }

    @Override
    protected float ridingOffset(Entity pEntity) {
        return 0.5F;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putByte("ExplosionPower", (byte)this.explosionPower);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        if (pCompound.contains("ExplosionPower", 99)) {
            this.explosionPower = pCompound.getByte("ExplosionPower");
        }
    }

    @Override
    protected float getStandingEyeHeight(Pose pPose, EntityDimensions pSize) {
        return 2.6F;
    }

    static class GhastLookGoal extends Goal {
        private final Ghast ghast;

        public GhastLookGoal(Ghast pGhast) {
            this.ghast = pGhast;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this method as well.
         */
        @Override
        public boolean canUse() {
            return true;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        /**
         * Keep ticking a continuous task that has already been started
         */
        @Override
        public void tick() {
            if (this.ghast.getTarget() == null) {
                Vec3 vec3 = this.ghast.getDeltaMovement();
                this.ghast.setYRot(-((float)Mth.atan2(vec3.x, vec3.z)) * (180.0F / (float)Math.PI));
                this.ghast.yBodyRot = this.ghast.getYRot();
            } else {
                LivingEntity livingentity = this.ghast.getTarget();
                double d0 = 64.0;
                if (livingentity.distanceToSqr(this.ghast) < 4096.0) {
                    double d1 = livingentity.getX() - this.ghast.getX();
                    double d2 = livingentity.getZ() - this.ghast.getZ();
                    this.ghast.setYRot(-((float)Mth.atan2(d1, d2)) * (180.0F / (float)Math.PI));
                    this.ghast.yBodyRot = this.ghast.getYRot();
                }
            }
        }
    }

    static class GhastMoveControl extends MoveControl {
        private final Ghast ghast;
        private int floatDuration;

        public GhastMoveControl(Ghast pGhast) {
            super(pGhast);
            this.ghast = pGhast;
        }

        @Override
        public void tick() {
            if (this.operation == MoveControl.Operation.MOVE_TO) {
                if (this.floatDuration-- <= 0) {
                    this.floatDuration += this.ghast.getRandom().nextInt(5) + 2;
                    Vec3 vec3 = new Vec3(this.wantedX - this.ghast.getX(), this.wantedY - this.ghast.getY(), this.wantedZ - this.ghast.getZ());
                    double d0 = vec3.length();
                    vec3 = vec3.normalize();
                    if (this.canReach(vec3, Mth.ceil(d0))) {
                        this.ghast.setDeltaMovement(this.ghast.getDeltaMovement().add(vec3.scale(0.1)));
                    } else {
                        this.operation = MoveControl.Operation.WAIT;
                    }
                }
            }
        }

        private boolean canReach(Vec3 pPos, int pLength) {
            AABB aabb = this.ghast.getBoundingBox();

            for(int i = 1; i < pLength; ++i) {
                aabb = aabb.move(pPos);
                if (!this.ghast.level().noCollision(this.ghast, aabb)) {
                    return false;
                }
            }

            return true;
        }
    }

    static class GhastShootFireballGoal extends Goal {
        private final Ghast ghast;
        public int chargeTime;

        public GhastShootFireballGoal(Ghast pGhast) {
            this.ghast = pGhast;
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this method as well.
         */
        @Override
        public boolean canUse() {
            return this.ghast.getTarget() != null;
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        @Override
        public void start() {
            this.chargeTime = 0;
        }

        /**
         * Reset the task's internal state. Called when this task is interrupted by another one
         */
        @Override
        public void stop() {
            this.ghast.setCharging(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        /**
         * Keep ticking a continuous task that has already been started
         */
        @Override
        public void tick() {
            LivingEntity livingentity = this.ghast.getTarget();
            if (livingentity != null) {
                double d0 = 64.0;
                if (livingentity.distanceToSqr(this.ghast) < 4096.0 && this.ghast.hasLineOfSight(livingentity)) {
                    Level level = this.ghast.level();
                    ++this.chargeTime;
                    if (this.chargeTime == 10 && !this.ghast.isSilent()) {
                        level.levelEvent(null, 1015, this.ghast.blockPosition(), 0);
                    }

                    if (this.chargeTime == 20) {
                        double d1 = 4.0;
                        Vec3 vec3 = this.ghast.getViewVector(1.0F);
                        double d2 = livingentity.getX() - (this.ghast.getX() + vec3.x * 4.0);
                        double d3 = livingentity.getY(0.5) - (0.5 + this.ghast.getY(0.5));
                        double d4 = livingentity.getZ() - (this.ghast.getZ() + vec3.z * 4.0);
                        if (!this.ghast.isSilent()) {
                            level.levelEvent(null, 1016, this.ghast.blockPosition(), 0);
                        }

                        LargeFireball largefireball = new LargeFireball(level, this.ghast, d2, d3, d4, this.ghast.getExplosionPower());
                        largefireball.setPos(this.ghast.getX() + vec3.x * 4.0, this.ghast.getY(0.5) + 0.5, largefireball.getZ() + vec3.z * 4.0);
                        level.addFreshEntity(largefireball);
                        this.chargeTime = -40;
                    }
                } else if (this.chargeTime > 0) {
                    --this.chargeTime;
                }

                this.ghast.setCharging(this.chargeTime > 10);
            }
        }
    }

    static class RandomFloatAroundGoal extends Goal {
        private final Ghast ghast;

        public RandomFloatAroundGoal(Ghast pGhast) {
            this.ghast = pGhast;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this method as well.
         */
        @Override
        public boolean canUse() {
            MoveControl movecontrol = this.ghast.getMoveControl();
            if (!movecontrol.hasWanted()) {
                return true;
            } else {
                double d0 = movecontrol.getWantedX() - this.ghast.getX();
                double d1 = movecontrol.getWantedY() - this.ghast.getY();
                double d2 = movecontrol.getWantedZ() - this.ghast.getZ();
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                return d3 < 1.0 || d3 > 3600.0;
            }
        }

        /**
         * Returns whether an in-progress EntityAIBase should continue executing
         */
        @Override
        public boolean canContinueToUse() {
            return false;
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        @Override
        public void start() {
            RandomSource randomsource = this.ghast.getRandom();
            double d0 = this.ghast.getX() + (double)((randomsource.nextFloat() * 2.0F - 1.0F) * 16.0F);
            double d1 = this.ghast.getY() + (double)((randomsource.nextFloat() * 2.0F - 1.0F) * 16.0F);
            double d2 = this.ghast.getZ() + (double)((randomsource.nextFloat() * 2.0F - 1.0F) * 16.0F);
            this.ghast.getMoveControl().setWantedPosition(d0, d1, d2, 1.0);
        }
    }
}
