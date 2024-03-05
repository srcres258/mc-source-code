package net.minecraft.world.item;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BucketItem extends Item implements DispensibleContainerItem {
    /** Neo: Field accesses are redirected to {@link #getFluid()} with a coremod. */
    private final Fluid content;

    // Forge: Use the other constructor that takes a Supplier
    @Deprecated
    public BucketItem(Fluid pContent, Item.Properties pProperties) {
        super(pProperties);
        this.content = pContent;
        this.fluidSupplier = () -> pContent;
    }

    /**
     * @param supplier A fluid supplier such as {@link net.neoforged.neoforge.registries.DeferredHolder}
     */
    public BucketItem(java.util.function.Supplier<? extends Fluid> supplier, Item.Properties builder) {
        super(builder);
        this.content = null;
        this.fluidSupplier = supplier;
    }

    /**
     * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see {@link #onItemUse}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        BlockHitResult blockhitresult = getPlayerPOVHitResult(
            pLevel, pPlayer, this.content == Fluids.EMPTY ? ClipContext.Fluid.SOURCE_ONLY : ClipContext.Fluid.NONE
        );
        InteractionResultHolder<ItemStack> ret = net.neoforged.neoforge.event.EventHooks.onBucketUse(pPlayer, pLevel, itemstack, blockhitresult);
        if (ret != null) return ret;
        if (blockhitresult.getType() == HitResult.Type.MISS) {
            return InteractionResultHolder.pass(itemstack);
        } else if (blockhitresult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack);
        } else {
            BlockPos blockpos = blockhitresult.getBlockPos();
            Direction direction = blockhitresult.getDirection();
            BlockPos blockpos1 = blockpos.relative(direction);
            if (!pLevel.mayInteract(pPlayer, blockpos) || !pPlayer.mayUseItemAt(blockpos1, direction, itemstack)) {
                return InteractionResultHolder.fail(itemstack);
            } else if (this.content == Fluids.EMPTY) {
                BlockState blockstate1 = pLevel.getBlockState(blockpos);
                Block $$10 = blockstate1.getBlock();
                if ($$10 instanceof BucketPickup bucketpickup) {
                    ItemStack itemstack2 = bucketpickup.pickupBlock(pPlayer, pLevel, blockpos, blockstate1);
                    if (!itemstack2.isEmpty()) {
                        pPlayer.awardStat(Stats.ITEM_USED.get(this));
                        bucketpickup.getPickupSound(blockstate1).ifPresent(p_150709_ -> pPlayer.playSound(p_150709_, 1.0F, 1.0F));
                        pLevel.gameEvent(pPlayer, GameEvent.FLUID_PICKUP, blockpos);
                        ItemStack itemstack1 = ItemUtils.createFilledResult(itemstack, pPlayer, itemstack2);
                        if (!pLevel.isClientSide) {
                            CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayer)pPlayer, itemstack2);
                        }

                        return InteractionResultHolder.sidedSuccess(itemstack1, pLevel.isClientSide());
                    }
                }

                return InteractionResultHolder.fail(itemstack);
            } else {
                BlockState blockstate = pLevel.getBlockState(blockpos);
                BlockPos blockpos2 = canBlockContainFluid(pPlayer, pLevel, blockpos, blockstate) ? blockpos : blockpos1;
                if (this.emptyContents(pPlayer, pLevel, blockpos2, blockhitresult, itemstack)) {
                    this.checkExtraContent(pPlayer, pLevel, itemstack, blockpos2);
                    if (pPlayer instanceof ServerPlayer) {
                        CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer)pPlayer, blockpos2, itemstack);
                    }

                    pPlayer.awardStat(Stats.ITEM_USED.get(this));
                    return InteractionResultHolder.sidedSuccess(getEmptySuccessItem(itemstack, pPlayer), pLevel.isClientSide());
                } else {
                    return InteractionResultHolder.fail(itemstack);
                }
            }
        }
    }

    public static ItemStack getEmptySuccessItem(ItemStack pBucketStack, Player pPlayer) {
        return !pPlayer.getAbilities().instabuild ? new ItemStack(Items.BUCKET) : pBucketStack;
    }

    @Override
    public void checkExtraContent(@Nullable Player pPlayer, Level pLevel, ItemStack pContainerStack, BlockPos pPos) {
    }

    @Override
    @Deprecated //Forge: use the ItemStack sensitive version
    public boolean emptyContents(@Nullable Player pPlayer, Level pLevel, BlockPos pPos, @Nullable BlockHitResult pResult) {
        Fluid $$6 = this.content;
        return this.emptyContents(pPlayer, pLevel, pPos, pResult, null);
    }

    public boolean emptyContents(@Nullable Player p_150716_, Level p_150717_, BlockPos p_150718_, @Nullable BlockHitResult p_150719_, @Nullable ItemStack container) {
        if (!(this.content instanceof FlowingFluid)) {
            return false;
        } else {
            FlowingFluid flowingfluid;
            Block $$7;
            boolean $$8;
            boolean flag2;
            BlockState blockstate;
            label82: {
                flowingfluid = (FlowingFluid)this.content;
                blockstate = p_150717_.getBlockState(p_150718_);
                $$7 = blockstate.getBlock();
                $$8 = blockstate.canBeReplaced(this.content);
                label70:
                if (!blockstate.isAir() && !$$8) {
                    if ($$7 instanceof LiquidBlockContainer liquidblockcontainer
                        && liquidblockcontainer.canPlaceLiquid(p_150716_, p_150717_, p_150718_, blockstate, this.content)) {
                        break label70;
                    }

                    flag2 = false;
                    break label82;
                }

                flag2 = true;
            }

            boolean flag1 = flag2;
            java.util.Optional<net.neoforged.neoforge.fluids.FluidStack> containedFluidStack = java.util.Optional.ofNullable(container).flatMap(net.neoforged.neoforge.fluids.FluidUtil::getFluidContained);
            if (!flag1) {
                return p_150719_ != null && this.emptyContents(p_150716_, p_150717_, p_150719_.getBlockPos().relative(p_150719_.getDirection()), null, container);
            } else if (containedFluidStack.isPresent() && this.content.getFluidType().isVaporizedOnPlacement(p_150717_, p_150718_, containedFluidStack.get())) {
                this.content.getFluidType().onVaporize(p_150716_, p_150717_, p_150718_, containedFluidStack.get());
                return true;
            } else if (p_150717_.dimensionType().ultraWarm() && this.content.is(FluidTags.WATER)) {
                int l = p_150718_.getX();
                int i = p_150718_.getY();
                int j = p_150718_.getZ();
                p_150717_.playSound(
                    p_150716_,
                    p_150718_,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    0.5F,
                    2.6F + (p_150717_.random.nextFloat() - p_150717_.random.nextFloat()) * 0.8F
                );

                for(int k = 0; k < 8; ++k) {
                    p_150717_.addParticle(
                        ParticleTypes.LARGE_SMOKE, (double)l + Math.random(), (double)i + Math.random(), (double)j + Math.random(), 0.0, 0.0, 0.0
                    );
                }

                return true;
            } else {
                if ($$7 instanceof LiquidBlockContainer liquidblockcontainer1 && liquidblockcontainer1.canPlaceLiquid(p_150716_, p_150717_, p_150718_, blockstate,content)) {
                    liquidblockcontainer1.placeLiquid(p_150717_, p_150718_, blockstate, flowingfluid.getSource(false));
                    this.playEmptySound(p_150716_, p_150717_, p_150718_);
                    return true;
                }

                if (!p_150717_.isClientSide && $$8 && !blockstate.liquid()) {
                    p_150717_.destroyBlock(p_150718_, true);
                }

                if (!p_150717_.setBlock(p_150718_, this.content.defaultFluidState().createLegacyBlock(), 11) && !blockstate.getFluidState().isSource()) {
                    return false;
                } else {
                    this.playEmptySound(p_150716_, p_150717_, p_150718_);
                    return true;
                }
            }
        }
    }

    protected void playEmptySound(@Nullable Player pPlayer, LevelAccessor pLevel, BlockPos pPos) {
        SoundEvent soundevent = this.content.getFluidType().getSound(pPlayer, pLevel, pPos, net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY);
        if(soundevent == null) soundevent = this.content.is(FluidTags.LAVA) ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY;
        pLevel.playSound(pPlayer, pPos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
        pLevel.gameEvent(pPlayer, GameEvent.FLUID_PLACE, pPos);
    }

    private final java.util.function.Supplier<? extends Fluid> fluidSupplier;
    public Fluid getFluid() { return fluidSupplier.get(); }

    protected boolean canBlockContainFluid(@Nullable Player player, Level worldIn, BlockPos posIn, BlockState blockstate)
    {
        return blockstate.getBlock() instanceof LiquidBlockContainer && ((LiquidBlockContainer)blockstate.getBlock()).canPlaceLiquid(player, worldIn, posIn, blockstate, this.content);
    }
}
