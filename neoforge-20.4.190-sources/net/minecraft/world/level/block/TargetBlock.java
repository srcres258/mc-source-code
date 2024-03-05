package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class TargetBlock extends Block {
    public static final MapCodec<TargetBlock> CODEC = simpleCodec(TargetBlock::new);
    private static final IntegerProperty OUTPUT_POWER = BlockStateProperties.POWER;
    private static final int ACTIVATION_TICKS_ARROWS = 20;
    private static final int ACTIVATION_TICKS_OTHER = 8;

    @Override
    public MapCodec<TargetBlock> codec() {
        return CODEC;
    }

    public TargetBlock(BlockBehaviour.Properties p_57379_) {
        super(p_57379_);
        this.registerDefaultState(this.stateDefinition.any().setValue(OUTPUT_POWER, Integer.valueOf(0)));
    }

    @Override
    public void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
        int i = updateRedstoneOutput(pLevel, pState, pHit, pProjectile);
        Entity entity = pProjectile.getOwner();
        if (entity instanceof ServerPlayer serverplayer) {
            serverplayer.awardStat(Stats.TARGET_HIT);
            CriteriaTriggers.TARGET_BLOCK_HIT.trigger(serverplayer, pProjectile, pHit.getLocation(), i);
        }
    }

    private static int updateRedstoneOutput(LevelAccessor pLevel, BlockState pState, BlockHitResult pHit, Entity pProjectile) {
        int i = getRedstoneStrength(pHit, pHit.getLocation());
        int j = pProjectile instanceof AbstractArrow ? 20 : 8;
        if (!pLevel.getBlockTicks().hasScheduledTick(pHit.getBlockPos(), pState.getBlock())) {
            setOutputPower(pLevel, pState, i, pHit.getBlockPos(), j);
        }

        return i;
    }

    private static int getRedstoneStrength(BlockHitResult pHit, Vec3 pHitLocation) {
        Direction direction = pHit.getDirection();
        double d0 = Math.abs(Mth.frac(pHitLocation.x) - 0.5);
        double d1 = Math.abs(Mth.frac(pHitLocation.y) - 0.5);
        double d2 = Math.abs(Mth.frac(pHitLocation.z) - 0.5);
        Direction.Axis direction$axis = direction.getAxis();
        double d3;
        if (direction$axis == Direction.Axis.Y) {
            d3 = Math.max(d0, d2);
        } else if (direction$axis == Direction.Axis.Z) {
            d3 = Math.max(d0, d1);
        } else {
            d3 = Math.max(d1, d2);
        }

        return Math.max(1, Mth.ceil(15.0 * Mth.clamp((0.5 - d3) / 0.5, 0.0, 1.0)));
    }

    private static void setOutputPower(LevelAccessor pLevel, BlockState pState, int pPower, BlockPos pPos, int pWaitTime) {
        pLevel.setBlock(pPos, pState.setValue(OUTPUT_POWER, Integer.valueOf(pPower)), 3);
        pLevel.scheduleTick(pPos, pState.getBlock(), pWaitTime);
    }

    @Override
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(OUTPUT_POWER) != 0) {
            pLevel.setBlock(pPos, pState.setValue(OUTPUT_POWER, Integer.valueOf(0)), 3);
        }
    }

    /**
     * Returns the signal this block emits in the given direction.
     *
     * <p>
     * NOTE: directions in redstone signal related methods are backwards, so this method
     * checks for the signal emitted in the <i>opposite</i> direction of the one given.
     *
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#getSignal} whenever possible. Implementing/overriding is fine.
     */
    @Override
    public int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(OUTPUT_POWER);
    }

    /**
     * Returns whether this block is capable of emitting redstone signals.
     *
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#isSignalSource} whenever possible. Implementing/overriding is fine.
     */
    @Override
    public boolean isSignalSource(BlockState pState) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(OUTPUT_POWER);
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pLevel.isClientSide() && !pState.is(pOldState.getBlock())) {
            if (pState.getValue(OUTPUT_POWER) > 0 && !pLevel.getBlockTicks().hasScheduledTick(pPos, this)) {
                pLevel.setBlock(pPos, pState.setValue(OUTPUT_POWER, Integer.valueOf(0)), 18);
            }
        }
    }
}
