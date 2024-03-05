package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class WeatheringCopperStairBlock extends StairBlock implements WeatheringCopper {
    public static final MapCodec<WeatheringCopperStairBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_308852_ -> p_308852_.group(
                    WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(ChangeOverTimeBlock::getAge),
                    BlockState.CODEC.fieldOf("base_state").forGetter(StairBlock::getModelState),
                    propertiesCodec()
                )
                .apply(p_308852_, WeatheringCopperStairBlock::new)
    );
    private final WeatheringCopper.WeatherState weatherState;

    @Override
    public MapCodec<WeatheringCopperStairBlock> codec() {
        return CODEC;
    }

    public WeatheringCopperStairBlock(WeatheringCopper.WeatherState p_154951_, BlockState p_154952_, BlockBehaviour.Properties p_154953_) {
        super(p_154952_, p_154953_);
        this.weatherState = p_154951_;
    }

    /**
     * Performs a random tick on a block.
     */
    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        this.changeOverTime(pState, pLevel, pPos, pRandom);
    }

    /**
     * @return whether this block needs random ticking.
     */
    @Override
    public boolean isRandomlyTicking(BlockState pState) {
        return WeatheringCopper.getNext(pState.getBlock()).isPresent();
    }

    public WeatheringCopper.WeatherState getAge() {
        return this.weatherState;
    }
}
