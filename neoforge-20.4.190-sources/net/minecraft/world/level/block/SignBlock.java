package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class SignBlock extends BaseEntityBlock implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    protected static final float AABB_OFFSET = 4.0F;
    protected static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private final WoodType type;

    protected SignBlock(WoodType pType, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.type = pType;
    }

    @Override
    protected abstract MapCodec<? extends SignBlock> codec();

    /**
     * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
     * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately returns its solidified counterpart.
     * Note that this method should ideally consider only the specific direction passed in.
     */
    @Override
    public BlockState updateShape(BlockState pState, Direction pFacing, BlockState pFacingState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pFacingPos) {
        if (pState.getValue(WATERLOGGED)) {
            pLevel.scheduleTick(pCurrentPos, Fluids.WATER, Fluids.WATER.getTickDelay(pLevel));
        }

        return super.updateShape(pState, pFacing, pFacingState, pLevel, pCurrentPos, pFacingPos);
    }

    @Override
    public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public boolean isPossibleToRespawnInThis(BlockState pState) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new SignBlockEntity(pPos, pState);
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        Item item = itemstack.getItem();
        Item $$11 = itemstack.getItem();
        SignApplicator signapplicator = $$11 instanceof SignApplicator signapplicator1 ? signapplicator1 : null;
        boolean flag1 = signapplicator != null && pPlayer.mayBuild();
        BlockEntity $$12 = pLevel.getBlockEntity(pPos);
        if ($$12 instanceof SignBlockEntity signblockentity) {
            if (!pLevel.isClientSide) {
                boolean flag2 = signblockentity.isFacingFrontText(pPlayer);
                SignText signtext = signblockentity.getText(flag2);
                boolean flag = signblockentity.executeClickCommandsIfPresent(pPlayer, pLevel, pPos, flag2);
                if (signblockentity.isWaxed()) {
                    pLevel.playSound(null, signblockentity.getBlockPos(), signblockentity.getSignInteractionFailedSoundEvent(), SoundSource.BLOCKS);
                    return this.getInteractionResult(flag1);
                } else if (flag1
                    && !this.otherPlayerIsEditingSign(pPlayer, signblockentity)
                    && signapplicator.canApplyToSign(signtext, pPlayer)
                    && signapplicator.tryApplyToSign(pLevel, signblockentity, flag2, pPlayer)) {
                    if (!pPlayer.isCreative()) {
                        itemstack.shrink(1);
                    }

                    pLevel.gameEvent(GameEvent.BLOCK_CHANGE, signblockentity.getBlockPos(), GameEvent.Context.of(pPlayer, signblockentity.getBlockState()));
                    pPlayer.awardStat(Stats.ITEM_USED.get(item));
                    return InteractionResult.SUCCESS;
                } else if (flag) {
                    return InteractionResult.SUCCESS;
                } else if (!this.otherPlayerIsEditingSign(pPlayer, signblockentity)
                    && pPlayer.mayBuild()
                    && this.hasEditableText(pPlayer, signblockentity, flag2)) {
                    this.openTextEdit(pPlayer, signblockentity, flag2);
                    return this.getInteractionResult(flag1);
                } else {
                    return InteractionResult.PASS;
                }
            } else {
                return !flag1 && !signblockentity.isWaxed() ? InteractionResult.CONSUME : InteractionResult.SUCCESS;
            }
        } else {
            return InteractionResult.PASS;
        }
    }

    private InteractionResult getInteractionResult(boolean pCanBuild) {
        return pCanBuild ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    private boolean hasEditableText(Player pPlayer, SignBlockEntity pSignEntity, boolean pIsFrontText) {
        SignText signtext = pSignEntity.getText(pIsFrontText);
        return Arrays.stream(signtext.getMessages(pPlayer.isTextFilteringEnabled()))
            .allMatch(p_304376_ -> p_304376_.equals(CommonComponents.EMPTY) || p_304376_.getContents() instanceof PlainTextContents);
    }

    public abstract float getYRotationDegrees(BlockState pState);

    public Vec3 getSignHitboxCenterPosition(BlockState pState) {
        return new Vec3(0.5, 0.5, 0.5);
    }

    @Override
    public FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    public WoodType type() {
        return this.type;
    }

    public static WoodType getWoodType(Block pBlock) {
        WoodType woodtype;
        if (pBlock instanceof SignBlock) {
            woodtype = ((SignBlock)pBlock).type();
        } else {
            woodtype = WoodType.OAK;
        }

        return woodtype;
    }

    public void openTextEdit(Player pPlayer, SignBlockEntity pSignEntity, boolean pIsFrontText) {
        pSignEntity.setAllowedPlayerEditor(pPlayer.getUUID());
        pPlayer.openTextEdit(pSignEntity, pIsFrontText);
    }

    private boolean otherPlayerIsEditingSign(Player pPlayer, SignBlockEntity pSignEntity) {
        UUID uuid = pSignEntity.getPlayerWhoMayEdit();
        return uuid != null && !uuid.equals(pPlayer.getUUID());
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, BlockEntityType.SIGN, SignBlockEntity::tick);
    }
}
