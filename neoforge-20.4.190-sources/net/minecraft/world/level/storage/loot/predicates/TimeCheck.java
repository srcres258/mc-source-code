package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

/**
 * A LootItemCondition that checks the {@linkplain ServerLevel#getDayTime day time} against an {@link IntRange} after applying an optional modulo division.
 */
public record TimeCheck(Optional<Long> period, IntRange value) implements LootItemCondition {
    public static final Codec<TimeCheck> CODEC = RecordCodecBuilder.create(
        p_298496_ -> p_298496_.group(
                    ExtraCodecs.strictOptionalField(Codec.LONG, "period").forGetter(TimeCheck::period),
                    IntRange.CODEC.fieldOf("value").forGetter(TimeCheck::value)
                )
                .apply(p_298496_, TimeCheck::new)
    );

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.TIME_CHECK;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.value.getReferencedContextParams();
    }

    public boolean test(LootContext pContext) {
        ServerLevel serverlevel = pContext.getLevel();
        long i = serverlevel.getDayTime();
        if (this.period.isPresent()) {
            i %= this.period.get();
        }

        return this.value.test(pContext, (int)i);
    }

    public static TimeCheck.Builder time(IntRange pTimeRange) {
        return new TimeCheck.Builder(pTimeRange);
    }

    public static class Builder implements LootItemCondition.Builder {
        private Optional<Long> period = Optional.empty();
        private final IntRange value;

        public Builder(IntRange pTimeRange) {
            this.value = pTimeRange;
        }

        public TimeCheck.Builder setPeriod(long pPeriod) {
            this.period = Optional.of(pPeriod);
            return this;
        }

        public TimeCheck build() {
            return new TimeCheck(this.period, this.value);
        }
    }
}
