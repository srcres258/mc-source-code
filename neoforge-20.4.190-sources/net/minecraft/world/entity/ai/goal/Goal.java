package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.util.Mth;

public abstract class Goal {
    private final EnumSet<Goal.Flag> flags = EnumSet.noneOf(Goal.Flag.class);

    /**
     * Returns whether execution should begin. You can also read and cache any state necessary for execution in this method as well.
     */
    public abstract boolean canUse();

    /**
     * @return whether the goal should continue executing
     */
    public boolean canContinueToUse() {
        return this.canUse();
    }

    public boolean isInterruptable() {
        return true;
    }

    /**
     * Called when the goal is about to start executing
     */
    public void start() {
    }

    /**
     * Called when the goal stops executing, usually to reset the mob's state.
     */
    public void stop() {
    }

    public boolean requiresUpdateEveryTick() {
        return false;
    }

    /**
     * Called every tick to update a goal that is in progress.
     */
    public void tick() {
    }

    public void setFlags(EnumSet<Goal.Flag> pFlagSet) {
        this.flags.clear();
        this.flags.addAll(pFlagSet);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public EnumSet<Goal.Flag> getFlags() {
        return this.flags;
    }

    protected int adjustedTickDelay(int pAdjustment) {
        return this.requiresUpdateEveryTick() ? pAdjustment : reducedTickDelay(pAdjustment);
    }

    protected static int reducedTickDelay(int pReduction) {
        return Mth.positiveCeilDiv(pReduction, 2);
    }

    public static enum Flag {
        MOVE,
        LOOK,
        JUMP,
        TARGET;
    }
}
