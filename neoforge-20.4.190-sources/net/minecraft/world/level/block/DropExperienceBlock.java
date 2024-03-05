package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class DropExperienceBlock extends Block {
    public static final MapCodec<DropExperienceBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308822_ -> p_308822_.group(IntProvider.codec(0, 10).fieldOf("experience").forGetter(p_304879_ -> p_304879_.xpRange), propertiesCodec())
                .apply(p_308822_, DropExperienceBlock::new)
    );
    private final IntProvider xpRange;

    @Override
    public MapCodec<? extends DropExperienceBlock> codec() {
        return CODEC;
    }

    public DropExperienceBlock(IntProvider p_221084_, BlockBehaviour.Properties p_221083_) {
        super(p_221083_);
        this.xpRange = p_221084_;
    }

    /**
     * Perform side-effects from block dropping, such as creating silverfish
     */
    @Override
    public void spawnAfterBreak(BlockState pState, ServerLevel pLevel, BlockPos pPos, ItemStack pStack, boolean pDropExperience) {
        super.spawnAfterBreak(pState, pLevel, pPos, pStack, pDropExperience);

    }

    @Override
    public int getExpDrop(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.util.RandomSource randomSource, BlockPos pos, int fortuneLevel, int silkTouchLevel) {
        return silkTouchLevel == 0 ? this.xpRange.sample(randomSource) : 0;
    }
}
