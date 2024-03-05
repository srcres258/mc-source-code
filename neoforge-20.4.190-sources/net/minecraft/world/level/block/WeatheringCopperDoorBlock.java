package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class WeatheringCopperDoorBlock extends DoorBlock implements WeatheringCopper {
    public static final MapCodec<WeatheringCopperDoorBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_309083_ -> p_309083_.group(
                    BlockSetType.CODEC.fieldOf("block_set_type").forGetter(DoorBlock::type),
                    WeatheringCopper.WeatherState.CODEC.fieldOf("weathering_state").forGetter(WeatheringCopperDoorBlock::getAge),
                    propertiesCodec()
                )
                .apply(p_309083_, WeatheringCopperDoorBlock::new)
    );
    private final WeatheringCopper.WeatherState weatherState;

    @Override
    public MapCodec<WeatheringCopperDoorBlock> codec() {
        return CODEC;
    }

    protected WeatheringCopperDoorBlock(BlockSetType p_309051_, WeatheringCopper.WeatherState p_308937_, BlockBehaviour.Properties p_309122_) {
        super(p_309051_, p_309122_);
        this.weatherState = p_308937_;
    }

    /**
     * Performs a random tick on a block.
     */
    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
            this.changeOverTime(pState, pLevel, pPos, pRandom);
        }
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
