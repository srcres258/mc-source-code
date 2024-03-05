package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class OcelotAttackGoal extends Goal {
    private final Mob mob;
    private LivingEntity target;
    private int attackTime;

    public OcelotAttackGoal(Mob pMob) {
        this.mob = pMob;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this method as well.
     */
    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.mob.getTarget();
        if (livingentity == null) {
            return false;
        } else {
            this.target = livingentity;
            return true;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean canContinueToUse() {
        if (!this.target.isAlive()) {
            return false;
        } else if (this.mob.distanceToSqr(this.target) > 225.0) {
            return false;
        } else {
            return !this.mob.getNavigation().isDone() || this.canUse();
        }
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    @Override
    public void stop() {
        this.target = null;
        this.mob.getNavigation().stop();
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
        this.mob.getLookControl().setLookAt(this.target, 30.0F, 30.0F);
        double d0 = (double)(this.mob.getBbWidth() * 2.0F * this.mob.getBbWidth() * 2.0F);
        double d1 = this.mob.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
        double d2 = 0.8;
        if (d1 > d0 && d1 < 16.0) {
            d2 = 1.33;
        } else if (d1 < 225.0) {
            d2 = 0.6;
        }

        this.mob.getNavigation().moveTo(this.target, d2);
        this.attackTime = Math.max(this.attackTime - 1, 0);
        if (!(d1 > d0)) {
            if (this.attackTime <= 0) {
                this.attackTime = 20;
                this.mob.doHurtTarget(this.target);
            }
        }
    }
}
