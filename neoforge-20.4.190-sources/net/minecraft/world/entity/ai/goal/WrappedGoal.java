package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import javax.annotation.Nullable;

/**
 * This is an internal object used by the GoalSelector to choose between Goals.
 * In most cases, it should not be constructed directly.
 *
 * For information on how individual methods work, see the javadocs for Goal:
 * {@link net.minecraft.entity.ai.goal.Goal}
 */
public class WrappedGoal extends Goal {
    private final Goal goal;
    private final int priority;
    private boolean isRunning;

    public WrappedGoal(int pPriority, Goal pGoal) {
        this.priority = pPriority;
        this.goal = pGoal;
    }

    public boolean canBeReplacedBy(WrappedGoal pOther) {
        return this.isInterruptable() && pOther.getPriority() < this.getPriority();
    }

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this method as well.
     */
    @Override
    public boolean canUse() {
        return this.goal.canUse();
    }

    /**
     * @return whether the goal should continue executing
     */
    @Override
    public boolean canContinueToUse() {
        return this.goal.canContinueToUse();
    }

    @Override
    public boolean isInterruptable() {
        return this.goal.isInterruptable();
    }

    /**
     * Called when the goal is about to start executing
     */
    @Override
    public void start() {
        if (!this.isRunning) {
            this.isRunning = true;
            this.goal.start();
        }
    }

    /**
     * Called when the goal stops executing, usually to reset the mob's state.
     */
    @Override
    public void stop() {
        if (this.isRunning) {
            this.isRunning = false;
            this.goal.stop();
        }
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return this.goal.requiresUpdateEveryTick();
    }

    @Override
    protected int adjustedTickDelay(int pAdjustment) {
        return this.goal.adjustedTickDelay(pAdjustment);
    }

    /**
     * Called every tick to update a goal that is in progress.
     */
    @Override
    public void tick() {
        this.goal.tick();
    }

    @Override
    public void setFlags(EnumSet<Goal.Flag> pFlagSet) {
        this.goal.setFlags(pFlagSet);
    }

    @Override
    public EnumSet<Goal.Flag> getFlags() {
        return this.goal.getFlags();
    }

    public boolean isRunning() {
        return this.isRunning;
    }

    public int getPriority() {
        return this.priority;
    }

    /**
     * Gets the private goal enclosed by this WrappedGoal.
     */
    public Goal getGoal() {
        return this.goal;
    }

    @Override
    public boolean equals(@Nullable Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return pOther != null && this.getClass() == pOther.getClass() ? this.goal.equals(((WrappedGoal)pOther).goal) : false;
        }
    }

    @Override
    public int hashCode() {
        return this.goal.hashCode();
    }
}
