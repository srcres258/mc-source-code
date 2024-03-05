package net.minecraft.world;

import net.minecraft.util.TimeUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class TickRateManager {
    public static final float MIN_TICKRATE = 1.0F;
    protected float tickrate = 20.0F;
    protected long nanosecondsPerTick = TimeUtil.NANOSECONDS_PER_SECOND / 20L;
    protected int frozenTicksToRun = 0;
    protected boolean runGameElements = true;
    protected boolean isFrozen = false;

    public void setTickRate(float pTickRate) {
        this.tickrate = Math.max(pTickRate, 1.0F);
        this.nanosecondsPerTick = (long)((double)TimeUtil.NANOSECONDS_PER_SECOND / (double)this.tickrate);
    }

    public float tickrate() {
        return this.tickrate;
    }

    public float millisecondsPerTick() {
        return (float)this.nanosecondsPerTick / (float)TimeUtil.NANOSECONDS_PER_MILLISECOND;
    }

    public long nanosecondsPerTick() {
        return this.nanosecondsPerTick;
    }

    public boolean runsNormally() {
        return this.runGameElements;
    }

    public boolean isSteppingForward() {
        return this.frozenTicksToRun > 0;
    }

    public void setFrozenTicksToRun(int pFrozenTicksToRun) {
        this.frozenTicksToRun = pFrozenTicksToRun;
    }

    public int frozenTicksToRun() {
        return this.frozenTicksToRun;
    }

    public void setFrozen(boolean pFrozen) {
        this.isFrozen = pFrozen;
    }

    public boolean isFrozen() {
        return this.isFrozen;
    }

    public void tick() {
        this.runGameElements = !this.isFrozen || this.frozenTicksToRun > 0;
        if (this.frozenTicksToRun > 0) {
            --this.frozenTicksToRun;
        }
    }

    public boolean isEntityFrozen(Entity pEntity) {
        return !this.runsNormally() && !(pEntity instanceof Player) && pEntity.countPlayerPassengers() <= 0;
    }
}
