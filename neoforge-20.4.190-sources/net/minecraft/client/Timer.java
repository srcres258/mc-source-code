package net.minecraft.client;

import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Timer {
    public float partialTick;
    public float tickDelta;
    private long lastMs;
    private final float msPerTick;
    private final FloatUnaryOperator targetMsptProvider;

    public Timer(float pTicksPerSecond, long pLastMs, FloatUnaryOperator pTargetMsptProvider) {
        this.msPerTick = 1000.0F / pTicksPerSecond;
        this.lastMs = pLastMs;
        this.targetMsptProvider = pTargetMsptProvider;
    }

    public int advanceTime(long pGameTime) {
        this.tickDelta = (float)(pGameTime - this.lastMs) / this.targetMsptProvider.apply(this.msPerTick);
        this.lastMs = pGameTime;
        this.partialTick += this.tickDelta;
        int i = (int)this.partialTick;
        this.partialTick -= (float)i;
        return i;
    }
}
