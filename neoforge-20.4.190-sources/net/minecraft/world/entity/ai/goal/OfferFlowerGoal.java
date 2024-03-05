package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;

public class OfferFlowerGoal extends Goal {
    private static final TargetingConditions OFFER_TARGER_CONTEXT = TargetingConditions.forNonCombat().range(6.0);
    public static final int OFFER_TICKS = 400;
    private final IronGolem golem;
    private Villager villager;
    private int tick;

    public OfferFlowerGoal(IronGolem pGolem) {
        this.golem = pGolem;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this method as well.
     */
    @Override
    public boolean canUse() {
        if (!this.golem.level().isDay()) {
            return false;
        } else if (this.golem.getRandom().nextInt(8000) != 0) {
            return false;
        } else {
            this.villager = this.golem
                .level()
                .getNearestEntity(
                    Villager.class,
                    OFFER_TARGER_CONTEXT,
                    this.golem,
                    this.golem.getX(),
                    this.golem.getY(),
                    this.golem.getZ(),
                    this.golem.getBoundingBox().inflate(6.0, 2.0, 6.0)
                );
            return this.villager != null;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean canContinueToUse() {
        return this.tick > 0;
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
    @Override
    public void start() {
        this.tick = this.adjustedTickDelay(400);
        this.golem.offerFlower(true);
    }

    /**
     * Reset the task's internal state. Called when this task is interrupted by another one
     */
    @Override
    public void stop() {
        this.golem.offerFlower(false);
        this.villager = null;
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    @Override
    public void tick() {
        this.golem.getLookControl().setLookAt(this.villager, 30.0F, 30.0F);
        --this.tick;
    }
}
