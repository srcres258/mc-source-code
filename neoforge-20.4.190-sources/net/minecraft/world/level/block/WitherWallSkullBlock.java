package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WitherWallSkullBlock extends WallSkullBlock {
    public static final MapCodec<WitherWallSkullBlock> CODEC = simpleCodec(WitherWallSkullBlock::new);

    @Override
    public MapCodec<WitherWallSkullBlock> codec() {
        return CODEC;
    }

    public WitherWallSkullBlock(BlockBehaviour.Properties p_58276_) {
        super(SkullBlock.Types.WITHER_SKELETON, p_58276_);
    }

    /**
     * Called by ItemBlocks after a block is set in the world, to allow post-place logic
     */
    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, @Nullable LivingEntity pPlacer, ItemStack pStack) {
        Blocks.WITHER_SKELETON_SKULL.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
    }
}
