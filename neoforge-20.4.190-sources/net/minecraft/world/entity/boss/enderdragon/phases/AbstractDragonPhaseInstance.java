package net.minecraft.world.entity.boss.enderdragon.phases;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractDragonPhaseInstance implements DragonPhaseInstance {
    protected final EnderDragon dragon;

    public AbstractDragonPhaseInstance(EnderDragon pDragon) {
        this.dragon = pDragon;
    }

    @Override
    public boolean isSitting() {
        return false;
    }

    /**
     * Generates particle effects appropriate to the phase (or sometimes sounds).
     * Called by dragon's onLivingUpdate. Only used when worldObj.isRemote.
     */
    @Override
    public void doClientTick() {
    }

    /**
     * Gives the phase a chance to update its status.
     * Called by dragon's onLivingUpdate. Only used when !worldObj.isRemote.
     */
    @Override
    public void doServerTick() {
    }

    @Override
    public void onCrystalDestroyed(EndCrystal pCrystal, BlockPos pPos, DamageSource pDmgSrc, @Nullable Player pPlyr) {
    }

    /**
     * Called when this phase is set to active
     */
    @Override
    public void begin() {
    }

    @Override
    public void end() {
    }

    /**
     * Returns the maximum amount dragon may rise or fall during this phase
     */
    @Override
    public float getFlySpeed() {
        return 0.6F;
    }

    /**
     * Returns the location the dragon is flying toward
     */
    @Nullable
    @Override
    public Vec3 getFlyTargetLocation() {
        return null;
    }

    @Override
    public float onHurt(DamageSource pDamageSource, float pAmount) {
        return pAmount;
    }

    @Override
    public float getTurnSpeed() {
        float f = (float)this.dragon.getDeltaMovement().horizontalDistance() + 1.0F;
        float f1 = Math.min(f, 40.0F);
        return 0.7F / f1 / f;
    }
}
