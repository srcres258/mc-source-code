package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;

public class EnvironmentScanPlacement extends PlacementModifier {
    private final Direction directionOfSearch;
    private final BlockPredicate targetCondition;
    private final BlockPredicate allowedSearchCondition;
    private final int maxSteps;
    public static final Codec<EnvironmentScanPlacement> CODEC = RecordCodecBuilder.create(
        p_191650_ -> p_191650_.group(
                    // NeoForge: Allow any direction, not just vertical. The code already handles it fine.
                    Direction.CODEC.fieldOf("direction_of_search").forGetter(p_191672_ -> p_191672_.directionOfSearch),
                    BlockPredicate.CODEC.fieldOf("target_condition").forGetter(p_191670_ -> p_191670_.targetCondition),
                    BlockPredicate.CODEC
                        .optionalFieldOf("allowed_search_condition", BlockPredicate.alwaysTrue())
                        .forGetter(p_191668_ -> p_191668_.allowedSearchCondition),
                    Codec.intRange(1, 32).fieldOf("max_steps").forGetter(p_191652_ -> p_191652_.maxSteps)
                )
                .apply(p_191650_, EnvironmentScanPlacement::new)
    );

    private EnvironmentScanPlacement(Direction p_191645_, BlockPredicate p_191646_, BlockPredicate p_191647_, int p_191648_) {
        this.directionOfSearch = p_191645_;
        this.targetCondition = p_191646_;
        this.allowedSearchCondition = p_191647_;
        this.maxSteps = p_191648_;
    }

    public static EnvironmentScanPlacement scanningFor(Direction pDirectionOfSearch, BlockPredicate pTargetCondition, BlockPredicate pAllowedSearchCondition, int pMaxSteps) {
        return new EnvironmentScanPlacement(pDirectionOfSearch, pTargetCondition, pAllowedSearchCondition, pMaxSteps);
    }

    public static EnvironmentScanPlacement scanningFor(Direction pDirectionOfSearch, BlockPredicate pTargetCondition, int pMaxSteps) {
        return scanningFor(pDirectionOfSearch, pTargetCondition, BlockPredicate.alwaysTrue(), pMaxSteps);
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext pContext, RandomSource pRandom, BlockPos pPos) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();
        WorldGenLevel worldgenlevel = pContext.getLevel();
        if (!this.allowedSearchCondition.test(worldgenlevel, blockpos$mutableblockpos)) {
            return Stream.of();
        } else {
            for(int i = 0; i < this.maxSteps; ++i) {
                if (this.targetCondition.test(worldgenlevel, blockpos$mutableblockpos)) {
                    return Stream.of(blockpos$mutableblockpos);
                }

                blockpos$mutableblockpos.move(this.directionOfSearch);
                if (worldgenlevel.isOutsideBuildHeight(blockpos$mutableblockpos.getY())) {
                    return Stream.of();
                }

                if (!this.allowedSearchCondition.test(worldgenlevel, blockpos$mutableblockpos)) {
                    break;
                }
            }

            return this.targetCondition.test(worldgenlevel, blockpos$mutableblockpos) ? Stream.of(blockpos$mutableblockpos) : Stream.of();
        }
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.ENVIRONMENT_SCAN;
    }
}
