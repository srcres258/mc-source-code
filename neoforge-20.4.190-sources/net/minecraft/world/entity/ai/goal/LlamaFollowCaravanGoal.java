package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.phys.Vec3;

public class LlamaFollowCaravanGoal extends Goal {
    public final Llama llama;
    private double speedModifier;
    private static final int CARAVAN_LIMIT = 8;
    private int distCheckCounter;

    public LlamaFollowCaravanGoal(Llama pLlama, double pSpeedModifier) {
        this.llama = pLlama;
        this.speedModifier = pSpeedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this method as well.
     */
    @Override
    public boolean canUse() {
        if (!this.llama.isLeashed() && !this.llama.inCaravan()) {
            List<Entity> list = this.llama.level().getEntities(this.llama, this.llama.getBoundingBox().inflate(9.0, 4.0, 9.0), p_25505_ -> {
                EntityType<?> entitytype = p_25505_.getType();
                return entitytype == EntityType.LLAMA || entitytype == EntityType.TRADER_LLAMA;
            });
            Llama llama = null;
            double d0 = Double.MAX_VALUE;

            for(Entity entity : list) {
                Llama llama1 = (Llama)entity;
                if (llama1.inCaravan() && !llama1.hasCaravanTail()) {
                    double d1 = this.llama.distanceToSqr(llama1);
                    if (!(d1 > d0)) {
                        d0 = d1;
                        llama = llama1;
                    }
                }
            }

            if (llama == null) {
                for(Entity entity1 : list) {
                    Llama llama2 = (Llama)entity1;
                    if (llama2.isLeashed() && !llama2.hasCaravanTail()) {
                        double d2 = this.llama.distanceToSqr(llama2);
                        if (!(d2 > d0)) {
                            d0 = d2;
                            llama = llama2;
                        }
                    }
                }
            }

            if (llama == null) {
                return false;
            } else if (d0 < 4.0) {
                return false;
            } else if (!llama.isLeashed() && !this.firstIsLeashed(llama, 1)) {
                return false;
            } else {
                this.llama.joinCaravan(llama);
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean canContinueToUse() {
        if (this.llama.inCaravan() && this.llama.getCaravanHead().isAlive() && this.firstIsLeashed(this.llama, 0)) {
            double d0 = this.llama.distanceToSqr(this.llama.getCaravanHead());
            if (d0 > 676.0) {
                if (this.speedModifier <= 3.0) {
                    this.speedModifier *= 1.2;
                    this.distCheckCounter = reducedTickDelay(40);
                    return true;
                }

                if (this.distCheckCounter == 0) {
                    return false;
                }
            }

            if (this.distCheckCounter > 0) {
                --this.distCheckCounter;
            }

            return true;
        } else {
            return false;
        }
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    @Override
    public void stop() {
        this.llama.leaveCaravan();
        this.speedModifier = 2.1;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    @Override
    public void tick() {
        if (this.llama.inCaravan()) {
            if (!(this.llama.getLeashHolder() instanceof LeashFenceKnotEntity)) {
                Llama llama = this.llama.getCaravanHead();
                double d0 = (double)this.llama.distanceTo(llama);
                float f = 2.0F;
                Vec3 vec3 = new Vec3(llama.getX() - this.llama.getX(), llama.getY() - this.llama.getY(), llama.getZ() - this.llama.getZ())
                    .normalize()
                    .scale(Math.max(d0 - 2.0, 0.0));
                this.llama.getNavigation().moveTo(this.llama.getX() + vec3.x, this.llama.getY() + vec3.y, this.llama.getZ() + vec3.z, this.speedModifier);
            }
        }
    }

    private boolean firstIsLeashed(Llama pLlama, int pLeashedQueuePosition) {
        if (pLeashedQueuePosition > 8) {
            return false;
        } else if (pLlama.inCaravan()) {
            return pLlama.getCaravanHead().isLeashed() ? true : this.firstIsLeashed(pLlama.getCaravanHead(), ++pLeashedQueuePosition);
        } else {
            return false;
        }
    }
}
