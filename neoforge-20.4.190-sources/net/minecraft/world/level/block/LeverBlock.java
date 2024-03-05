package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LeverBlock extends FaceAttachedHorizontalDirectionalBlock {
    public static final MapCodec<LeverBlock> CODEC = simpleCodec(LeverBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    protected static final int DEPTH = 6;
    protected static final int WIDTH = 6;
    protected static final int HEIGHT = 8;
    protected static final VoxelShape NORTH_AABB = Block.box(5.0, 4.0, 10.0, 11.0, 12.0, 16.0);
    protected static final VoxelShape SOUTH_AABB = Block.box(5.0, 4.0, 0.0, 11.0, 12.0, 6.0);
    protected static final VoxelShape WEST_AABB = Block.box(10.0, 4.0, 5.0, 16.0, 12.0, 11.0);
    protected static final VoxelShape EAST_AABB = Block.box(0.0, 4.0, 5.0, 6.0, 12.0, 11.0);
    protected static final VoxelShape UP_AABB_Z = Block.box(5.0, 0.0, 4.0, 11.0, 6.0, 12.0);
    protected static final VoxelShape UP_AABB_X = Block.box(4.0, 0.0, 5.0, 12.0, 6.0, 11.0);
    protected static final VoxelShape DOWN_AABB_Z = Block.box(5.0, 10.0, 4.0, 11.0, 16.0, 12.0);
    protected static final VoxelShape DOWN_AABB_X = Block.box(4.0, 10.0, 5.0, 12.0, 16.0, 11.0);

    @Override
    public MapCodec<LeverBlock> codec() {
        return CODEC;
    }

    public LeverBlock(BlockBehaviour.Properties p_54633_) {
        super(p_54633_);
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, Boolean.valueOf(false)).setValue(FACE, AttachFace.WALL)
        );
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        switch((AttachFace)pState.getValue(FACE)) {
            case FLOOR:
                switch(pState.getValue(FACING).getAxis()) {
                    case X:
                        return UP_AABB_X;
                    case Z:
                    default:
                        return UP_AABB_Z;
                }
            case WALL:
                switch((Direction)pState.getValue(FACING)) {
                    case EAST:
                        return EAST_AABB;
                    case WEST:
                        return WEST_AABB;
                    case SOUTH:
                        return SOUTH_AABB;
                    case NORTH:
                    default:
                        return NORTH_AABB;
                }
            case CEILING:
            default:
                switch(pState.getValue(FACING).getAxis()) {
                    case X:
                        return DOWN_AABB_X;
                    case Z:
                    default:
                        return DOWN_AABB_Z;
                }
        }
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (pLevel.isClientSide) {
            BlockState blockstate1 = pState.cycle(POWERED);
            if (blockstate1.getValue(POWERED)) {
                makeParticle(blockstate1, pLevel, pPos, 1.0F);
            }

            return InteractionResult.SUCCESS;
        } else {
            BlockState blockstate = this.pull(pState, pLevel, pPos);
            float f = blockstate.getValue(POWERED) ? 0.6F : 0.5F;
            pLevel.playSound(null, pPos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, f);
            pLevel.gameEvent(pPlayer, blockstate.getValue(POWERED) ? GameEvent.BLOCK_ACTIVATE : GameEvent.BLOCK_DEACTIVATE, pPos);
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public void onExplosionHit(BlockState pState, Level pLevel, BlockPos pPos, Explosion pExplosion, BiConsumer<ItemStack, BlockPos> pDropConsumer) {
        if (pExplosion.getBlockInteraction() == Explosion.BlockInteraction.TRIGGER_BLOCK && !pLevel.isClientSide()) {
            this.pull(pState, pLevel, pPos);
        }

        super.onExplosionHit(pState, pLevel, pPos, pExplosion, pDropConsumer);
    }

    public BlockState pull(BlockState pState, Level pLevel, BlockPos pPos) {
        pState = pState.cycle(POWERED);
        pLevel.setBlock(pPos, pState, 3);
        this.updateNeighbours(pState, pLevel, pPos);
        return pState;
    }

    private static void makeParticle(BlockState pState, LevelAccessor pLevel, BlockPos pPos, float pAlpha) {
        Direction direction = pState.getValue(FACING).getOpposite();
        Direction direction1 = getConnectedDirection(pState).getOpposite();
        double d0 = (double)pPos.getX() + 0.5 + 0.1 * (double)direction.getStepX() + 0.2 * (double)direction1.getStepX();
        double d1 = (double)pPos.getY() + 0.5 + 0.1 * (double)direction.getStepY() + 0.2 * (double)direction1.getStepY();
        double d2 = (double)pPos.getZ() + 0.5 + 0.1 * (double)direction.getStepZ() + 0.2 * (double)direction1.getStepZ();
        pLevel.addParticle(new DustParticleOptions(DustParticleOptions.REDSTONE_PARTICLE_COLOR, pAlpha), d0, d1, d2, 0.0, 0.0, 0.0);
    }

    /**
     * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
     */
    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(POWERED) && pRandom.nextFloat() < 0.25F) {
            makeParticle(pState, pLevel, pPos, 0.5F);
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pIsMoving && !pState.is(pNewState.getBlock())) {
            if (pState.getValue(POWERED)) {
                this.updateNeighbours(pState, pLevel, pPos);
            }

            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
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
        return pBlockState.getValue(POWERED) ? 15 : 0;
    }

    /**
     * Returns the direct signal this block emits in the given direction.
     *
     * <p>
     * NOTE: directions in redstone signal related methods are backwards, so this method
     * checks for the signal emitted in the <i>opposite</i> direction of the one given.
     *
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#getDirectSignal} whenever possible. Implementing/overriding is fine.
     */
    @Override
    public int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(POWERED) && getConnectedDirection(pBlockState) == pSide ? 15 : 0;
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

    private void updateNeighbours(BlockState pState, Level pLevel, BlockPos pPos) {
        pLevel.updateNeighborsAt(pPos, this);
        pLevel.updateNeighborsAt(pPos.relative(getConnectedDirection(pState).getOpposite()), this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACE, FACING, POWERED);
    }
}
