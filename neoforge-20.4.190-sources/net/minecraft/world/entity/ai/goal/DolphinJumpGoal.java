package net.minecraft.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

public class DolphinJumpGoal extends JumpGoal {
    private static final int[] STEPS_TO_CHECK = new int[]{0, 1, 4, 5, 6, 7};
    private final Dolphin dolphin;
    private final int interval;
    private boolean breached;

    public DolphinJumpGoal(Dolphin pDolphin, int pInterval) {
        this.dolphin = pDolphin;
        this.interval = reducedTickDelay(pInterval);
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this method as well.
     */
    @Override
    public boolean canUse() {
        if (this.dolphin.getRandom().nextInt(this.interval) != 0) {
            return false;
        } else {
            Direction direction = this.dolphin.getMotionDirection();
            int i = direction.getStepX();
            int j = direction.getStepZ();
            BlockPos blockpos = this.dolphin.blockPosition();

            for(int k : STEPS_TO_CHECK) {
                if (!this.waterIsClear(blockpos, i, j, k) || !this.surfaceIsClear(blockpos, i, j, k)) {
                    return false;
                }
            }

            return true;
        }
    }

    private boolean waterIsClear(BlockPos pPos, int pDx, int pDz, int pScale) {
        BlockPos blockpos = pPos.offset(pDx * pScale, 0, pDz * pScale);
        return this.dolphin.level().getFluidState(blockpos).is(FluidTags.WATER) && !this.dolphin.level().getBlockState(blockpos).blocksMotion();
    }

    private boolean surfaceIsClear(BlockPos pPos, int pDx, int pDz, int pScale) {
        return this.dolphin.level().getBlockState(pPos.offset(pDx * pScale, 1, pDz * pScale)).isAir()
            && this.dolphin.level().getBlockState(pPos.offset(pDx * pScale, 2, pDz * pScale)).isAir();
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean canContinueToUse() {
        double d0 = this.dolphin.getDeltaMovement().y;
        return (!(d0 * d0 < 0.03F) || this.dolphin.getXRot() == 0.0F || !(Math.abs(this.dolphin.getXRot()) < 10.0F) || !this.dolphin.isInWater())
            && !this.dolphin.onGround();
    }

    @Override
    public boolean isInterruptable() {
        return false;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void start() {
        Direction direction = this.dolphin.getMotionDirection();
        this.dolphin.setDeltaMovement(this.dolphin.getDeltaMovement().add((double)direction.getStepX() * 0.6, 0.7, (double)direction.getStepZ() * 0.6));
        this.dolphin.getNavigation().stop();
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    @Override
    public void stop() {
        this.dolphin.setXRot(0.0F);
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    @Override
    public void tick() {
        boolean flag = this.breached;
        if (!flag) {
            FluidState fluidstate = this.dolphin.level().getFluidState(this.dolphin.blockPosition());
            this.breached = fluidstate.is(FluidTags.WATER);
        }

        if (this.breached && !flag) {
            this.dolphin.playSound(SoundEvents.DOLPHIN_JUMP, 1.0F, 1.0F);
        }

        Vec3 vec3 = this.dolphin.getDeltaMovement();
        if (vec3.y * vec3.y < 0.03F && this.dolphin.getXRot() != 0.0F) {
            this.dolphin.setXRot(Mth.rotLerp(0.2F, this.dolphin.getXRot(), 0.0F));
        } else if (vec3.length() > 1.0E-5F) {
            double d0 = vec3.horizontalDistance();
            double d1 = Math.atan2(-vec3.y, d0) * 180.0F / (float)Math.PI;
            this.dolphin.setXRot((float)d1);
        }
    }
}
