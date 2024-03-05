package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeCache;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class CrafterBlock extends BaseEntityBlock {
    public static final MapCodec<CrafterBlock> CODEC = simpleCodec(CrafterBlock::new);
    public static final BooleanProperty CRAFTING = BlockStateProperties.CRAFTING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    private static final EnumProperty<FrontAndTop> ORIENTATION = BlockStateProperties.ORIENTATION;
    private static final int MAX_CRAFTING_TICKS = 6;
    private static final int CRAFTING_TICK_DELAY = 4;
    private static final RecipeCache RECIPE_CACHE = new RecipeCache(10);

    public CrafterBlock(BlockBehaviour.Properties p_307674_) {
        super(p_307674_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(ORIENTATION, FrontAndTop.NORTH_UP)
                .setValue(TRIGGERED, Boolean.valueOf(false))
                .setValue(CRAFTING, Boolean.valueOf(false))
        );
    }

    @Override
    protected MapCodec<CrafterBlock> codec() {
        return CODEC;
    }

    /**
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#hasAnalogOutputSignal} whenever possible. Implementing/overriding is fine.
     */
    @Override
    public boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    /**
     * Returns the analog signal this block emits. This is the signal a comparator can read from it.
     *
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#getAnalogOutputSignal} whenever possible. Implementing/overriding is fine.
     */
    @Override
    public int getAnalogOutputSignal(BlockState pState, Level pLevel, BlockPos pPos) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        return blockentity instanceof CrafterBlockEntity crafterblockentity ? crafterblockentity.getRedstoneSignal() : 0;
    }

    @Override
    public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pNeighborBlock, BlockPos pNeighborPos, boolean pMovedByPiston) {
        boolean flag = pLevel.hasNeighborSignal(pPos);
        boolean flag1 = pState.getValue(TRIGGERED);
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (flag && !flag1) {
            pLevel.scheduleTick(pPos, this, 4);
            pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(true)), 2);
            this.setBlockEntityTriggered(blockentity, true);
        } else if (!flag && flag1) {
            pLevel.setBlock(pPos, pState.setValue(TRIGGERED, Boolean.valueOf(false)).setValue(CRAFTING, Boolean.valueOf(false)), 2);
            this.setBlockEntityTriggered(blockentity, false);
        }
    }

    @Override
    public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        this.dispenseFrom(pState, pLevel, pPos);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return pLevel.isClientSide ? null : createTickerHelper(pBlockEntityType, BlockEntityType.CRAFTER, CrafterBlockEntity::serverTick);
    }

    private void setBlockEntityTriggered(@Nullable BlockEntity pBlockEntity, boolean pTriggered) {
        if (pBlockEntity instanceof CrafterBlockEntity crafterblockentity) {
            crafterblockentity.setTriggered(pTriggered);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        CrafterBlockEntity crafterblockentity = new CrafterBlockEntity(pPos, pState);
        crafterblockentity.setTriggered(pState.hasProperty(TRIGGERED) && pState.getValue(TRIGGERED));
        return crafterblockentity;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Direction direction = pContext.getNearestLookingDirection().getOpposite();

        Direction direction1 = switch(direction) {
            case DOWN -> pContext.getHorizontalDirection().getOpposite();
            case UP -> pContext.getHorizontalDirection();
            case NORTH, SOUTH, WEST, EAST -> Direction.UP;
        };
        return this.defaultBlockState()
            .setValue(ORIENTATION, FrontAndTop.fromFrontAndTop(direction, direction1))
            .setValue(TRIGGERED, Boolean.valueOf(pContext.getLevel().hasNeighborSignal(pContext.getClickedPos())));
    }

    /**
     * Called by BlockItem after this block has been placed.
     */
    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
        if (pStack.hasCustomHoverName()) {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity instanceof CrafterBlockEntity crafterblockentity) {
                crafterblockentity.setCustomName(pStack.getHoverName());
            }
        }

        if (pState.getValue(TRIGGERED)) {
            pLevel.scheduleTick(pPos, this, 4);
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        Containers.dropContentsOnDestroy(pState, pNewState, pLevel, pPos);
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }

    @Override
    public InteractionResult use(
        BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit
    ) {
        if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity instanceof CrafterBlockEntity) {
                pPlayer.openMenu((CrafterBlockEntity)blockentity);
            }

            return InteractionResult.CONSUME;
        }
    }

    protected void dispenseFrom(BlockState pState, ServerLevel pLevel, BlockPos pPos) {
        BlockEntity $$5 = pLevel.getBlockEntity(pPos);
        if ($$5 instanceof CrafterBlockEntity crafterblockentity) {
            Optional<CraftingRecipe> optional = getPotentialResults(pLevel, crafterblockentity);
            if (optional.isEmpty()) {
                pLevel.levelEvent(1050, pPos, 0);
            } else {
                crafterblockentity.setCraftingTicksRemaining(6);
                pLevel.setBlock(pPos, pState.setValue(CRAFTING, Boolean.valueOf(true)), 2);
                CraftingRecipe craftingrecipe = optional.get();
                ItemStack itemstack = craftingrecipe.assemble(crafterblockentity, pLevel.registryAccess());
                itemstack.onCraftedBySystem(pLevel);
                this.dispenseItem(pLevel, pPos, crafterblockentity, itemstack, pState);
                craftingrecipe.getRemainingItems(crafterblockentity)
                    .forEach(p_307328_ -> this.dispenseItem(pLevel, pPos, crafterblockentity, p_307328_, pState));
                crafterblockentity.getItems().forEach(p_307295_ -> {
                    if (!p_307295_.isEmpty()) {
                        p_307295_.shrink(1);
                    }
                });
                crafterblockentity.setChanged();
            }
        }
    }

    public static Optional<CraftingRecipe> getPotentialResults(Level pLevel, CraftingContainer pContainer) {
        return RECIPE_CACHE.get(pLevel, pContainer);
    }

    private void dispenseItem(Level pLevel, BlockPos pPos, CrafterBlockEntity pCrafter, ItemStack pItem, BlockState pState) {
        Direction direction = pState.getValue(ORIENTATION).front();
        Container container = HopperBlockEntity.getContainerAt(pLevel, pPos.relative(direction));
        ItemStack itemstack = pItem.copy();
        if (container != null && (container instanceof CrafterBlockEntity || pItem.getCount() > container.getMaxStackSize())) {
            while(!itemstack.isEmpty()) {
                ItemStack itemstack2 = itemstack.copyWithCount(1);
                ItemStack itemstack1 = HopperBlockEntity.addItem(pCrafter, container, itemstack2, direction.getOpposite());
                if (!itemstack1.isEmpty()) {
                    break;
                }

                itemstack.shrink(1);
            }
        } else if (container != null) {
            while(!itemstack.isEmpty()) {
                int i = itemstack.getCount();
                itemstack = HopperBlockEntity.addItem(pCrafter, container, itemstack, direction.getOpposite());
                if (i == itemstack.getCount()) {
                    break;
                }
            }
        }

        if (!itemstack.isEmpty()) {
            Vec3 vec3 = Vec3.atCenterOf(pPos).relative(direction, 0.7);
            DefaultDispenseItemBehavior.spawnItem(pLevel, itemstack, 6, direction, vec3);
            pLevel.levelEvent(1049, pPos, 0);
            pLevel.levelEvent(2010, pPos, direction.get3DDataValue());
        }
    }

    /**
     * The type of render function called. MODEL for mixed tesr and static model, MODELBLOCK_ANIMATED for TESR-only, LIQUID for vanilla liquids, INVISIBLE to skip all rendering
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#getRenderShape} whenever possible. Implementing/overriding is fine.
     */
    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    /**
     * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed blockstate.
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#rotate} whenever possible. Implementing/overriding is fine.
     */
    @Override
    public BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(ORIENTATION, pRotation.rotation().rotate(pState.getValue(ORIENTATION)));
    }

    /**
     * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed blockstate.
     * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase#mirror} whenever possible. Implementing/overriding is fine.
     */
    @Override
    public BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.setValue(ORIENTATION, pMirror.rotation().rotate(pState.getValue(ORIENTATION)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(ORIENTATION, TRIGGERED, CRAFTING);
    }
}
