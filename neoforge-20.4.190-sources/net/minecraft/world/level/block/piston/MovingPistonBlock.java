package net.minecraft.world.level.block.piston;

import com.mojang.serialization.MapCodec;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.PistonType;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MovingPistonBlock extends BaseEntityBlock {
    public static final MapCodec<MovingPistonBlock> CODEC = simpleCodec(MovingPistonBlock::new);
    public static final DirectionProperty FACING = PistonHeadBlock.FACING;
    public static final EnumProperty<PistonType> TYPE = PistonHeadBlock.TYPE;

    @Override
    public MapCodec<MovingPistonBlock> codec() {
        return CODEC;
    }

    public MovingPistonBlock(BlockBehaviour.Properties p_60050_) {
        super(p_60050_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TYPE, PistonType.DEFAULT));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return null;
    }

    public static BlockEntity newMovingBlockEntity(
        BlockPos pPos, BlockState pBlockState, BlockState pMovedState, Direction pDirection, boolean pExtending, boolean pIsSourcePiston
    ) {
        return new PistonMovingBlockEntity(pPos, pBlockState, pMovedState, pDirection, pExtending, pIsSourcePiston);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, BlockEntityType.PISTON, PistonMovingBlockEntity::tick);
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (!pState.is(pNewState.getBlock())) {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity instanceof PistonMovingBlockEntity) {
                ((PistonMovingBlockEntity)blockentity).finalTick();
            }
        }
    }

    /**
     * Called after this block has been removed by a player.
     */
    @Override
    public void destroy(LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
        BlockPos blockpos = pPos.relative(pState.getValue(FACING).getOpposite());
        BlockState blockstate = pLevel.getBlockState(blockpos);
        if (blockstate.getBlock() instanceof PistonBaseBlock && blockstate.getValue(PistonBaseBlock.EXTENDED)) {
            pLevel.removeBlock(blockpos, false);
        }
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide && pLevel.getBlockEntity(pPos) == null) {
            pLevel.removeBlock(pPos, false);
            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState pState, LootParams.Builder pParams) {
        PistonMovingBlockEntity pistonmovingblockentity = this.getBlockEntity(
            pParams.getLevel(), BlockPos.containing(pParams.getParameter(LootContextParams.ORIGIN))
        );
        return pistonmovingblockentity == null ? Collections.emptyList() : pistonmovingblockentity.getMovedState().getDrops(pParams);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        PistonMovingBlockEntity pistonmovingblockentity = this.getBlockEntity(pLevel, pPos);
        return pistonmovingblockentity != null ? pistonmovingblockentity.getCollisionShape(pLevel, pPos) : Shapes.empty();
    }

    @Nullable
    private PistonMovingBlockEntity getBlockEntity(BlockGetter pBlockReader, BlockPos pPos) {
        BlockEntity blockentity = pBlockReader.getBlockEntity(pPos);
        return blockentity instanceof PistonMovingBlockEntity ? (PistonMovingBlockEntity)blockentity : null;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return ItemStack.EMPTY;
    }

    /**
     * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed blockstate.
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#rotate} whenever possible. Implementing/overriding is fine.
     */
    @Override
    public BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
    }

    /**
     * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed blockstate.
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#mirror} whenever possible. Implementing/overriding is fine.
     */
    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, TYPE);
    }

    @Override
    public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
        return false;
    }
}
