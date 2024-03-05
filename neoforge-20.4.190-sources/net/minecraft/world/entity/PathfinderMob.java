package net.minecraft.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.Vec3;

public abstract class PathfinderMob extends Mob {
    protected static final float DEFAULT_WALK_TARGET_VALUE = 0.0F;

    protected PathfinderMob(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public float getWalkTargetValue(BlockPos pPos) {
        return this.getWalkTargetValue(pPos, this.level());
    }

    public float getWalkTargetValue(BlockPos pPos, LevelReader pLevel) {
        return 0.0F;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor pLevel, MobSpawnType pSpawnReason) {
        return this.getWalkTargetValue(this.blockPosition(), pLevel) >= 0.0F;
    }

    /**
     * Returns whether the entity got a {@link net.minecraft.world.level.pathfinder.Path}.
     */
    public boolean isPathFinding() {
        return !this.getNavigation().isDone();
    }

    public boolean isPanicking() {
        return this.brain.hasMemoryValue(MemoryModuleType.IS_PANICKING)
            ? this.brain.getMemory(MemoryModuleType.IS_PANICKING).isPresent()
            : this.goalSelector.getRunningGoals().anyMatch(p_296202_ -> p_296202_.getGoal() instanceof PanicGoal);
    }

    /**
     * Applies logic related to leashes, for example dragging the entity or breaking the leash.
     */
    @Override
    protected void tickLeash() {
        super.tickLeash();
        Entity entity = this.getLeashHolder();
        if (entity != null && entity.level() == this.level()) {
            this.restrictTo(entity.blockPosition(), 5);
            float f = this.distanceTo(entity);
            if (this instanceof TamableAnimal && ((TamableAnimal)this).isInSittingPose()) {
                if (f > 10.0F) {
                    this.dropLeash(true, true);
                }

                return;
            }

            this.onLeashDistance(f);
            if (f > 10.0F) {
                this.dropLeash(true, true);
                this.goalSelector.disableControlFlag(Goal.Flag.MOVE);
            } else if (f > 6.0F) {
                double d0 = (entity.getX() - this.getX()) / (double)f;
                double d1 = (entity.getY() - this.getY()) / (double)f;
                double d2 = (entity.getZ() - this.getZ()) / (double)f;
                this.setDeltaMovement(
                    this.getDeltaMovement().add(Math.copySign(d0 * d0 * 0.4, d0), Math.copySign(d1 * d1 * 0.4, d1), Math.copySign(d2 * d2 * 0.4, d2))
                );
                this.checkSlowFallDistance();
            } else if (this.shouldStayCloseToLeashHolder() && !this.isPanicking()) {
                this.goalSelector.enableControlFlag(Goal.Flag.MOVE);
                float f1 = 2.0F;
                Vec3 vec3 = new Vec3(entity.getX() - this.getX(), entity.getY() - this.getY(), entity.getZ() - this.getZ())
                    .normalize()
                    .scale((double)Math.max(f - 2.0F, 0.0F));
                this.getNavigation().moveTo(this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z, this.followLeashSpeed());
            }
        }
    }

    protected boolean shouldStayCloseToLeashHolder() {
        return true;
    }

    protected double followLeashSpeed() {
        return 1.0;
    }

    protected void onLeashDistance(float pDistance) {
    }
}
