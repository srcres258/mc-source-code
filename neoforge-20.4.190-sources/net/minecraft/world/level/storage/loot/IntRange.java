package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

/**
 * A possibly unbounded range of integers based on {@link LootContext}. Minimum and maximum are given in the form of {@link NumberProvider}s.
 * Minimum and maximum are both optional. If given, they are both inclusive.
 */
public class IntRange {
    private static final Codec<IntRange> RECORD_CODEC = RecordCodecBuilder.create(
        p_297986_ -> p_297986_.group(
                    ExtraCodecs.strictOptionalField(NumberProviders.CODEC, "min").forGetter(p_297985_ -> Optional.ofNullable(p_297985_.min)),
                    ExtraCodecs.strictOptionalField(NumberProviders.CODEC, "max").forGetter(p_297984_ -> Optional.ofNullable(p_297984_.max))
                )
                .apply(p_297986_, IntRange::new)
    );
    public static final Codec<IntRange> CODEC = Codec.either(Codec.INT, RECORD_CODEC)
        .xmap(p_297983_ -> p_297983_.map(IntRange::exact, Function.identity()), p_297982_ -> {
            OptionalInt optionalint = p_297982_.unpackExact();
            return optionalint.isPresent() ? Either.left(optionalint.getAsInt()) : Either.right(p_297982_);
        });
    @Nullable
    private final NumberProvider min;
    @Nullable
    private final NumberProvider max;
    private final IntRange.IntLimiter limiter;
    private final IntRange.IntChecker predicate;

    /**
     * The LootContextParams required for this IntRange.
     */
    public Set<LootContextParam<?>> getReferencedContextParams() {
        Builder<LootContextParam<?>> builder = ImmutableSet.builder();
        if (this.min != null) {
            builder.addAll(this.min.getReferencedContextParams());
        }

        if (this.max != null) {
            builder.addAll(this.max.getReferencedContextParams());
        }

        return builder.build();
    }

    private IntRange(Optional<NumberProvider> p_299273_, Optional<NumberProvider> p_298799_) {
        this(p_299273_.orElse(null), p_298799_.orElse(null));
    }

    private IntRange(@Nullable NumberProvider pMin, @Nullable NumberProvider pMax) {
        this.min = pMin;
        this.max = pMax;
        if (pMin == null) {
            if (pMax == null) {
                this.limiter = (p_165050_, p_165051_) -> p_165051_;
                this.predicate = (p_165043_, p_165044_) -> true;
            } else {
                this.limiter = (p_165054_, p_165055_) -> Math.min(pMax.getInt(p_165054_), p_165055_);
                this.predicate = (p_165047_, p_165048_) -> p_165048_ <= pMax.getInt(p_165047_);
            }
        } else if (pMax == null) {
            this.limiter = (p_165033_, p_165034_) -> Math.max(pMin.getInt(p_165033_), p_165034_);
            this.predicate = (p_165019_, p_165020_) -> p_165020_ >= pMin.getInt(p_165019_);
        } else {
            this.limiter = (p_165038_, p_165039_) -> Mth.clamp(p_165039_, pMin.getInt(p_165038_), pMax.getInt(p_165038_));
            this.predicate = (p_165024_, p_165025_) -> p_165025_ >= pMin.getInt(p_165024_) && p_165025_ <= pMax.getInt(p_165024_);
        }
    }

    /**
     * Create an IntRange that contains only exactly the given value.
     */
    public static IntRange exact(int p_165010_) {
        ConstantValue constantvalue = ConstantValue.exactly((float)p_165010_);
        return new IntRange(Optional.of(constantvalue), Optional.of(constantvalue));
    }

    /**
     * Create an IntRange that ranges from {@code min} to {@code max}, both inclusive.
     */
    public static IntRange range(int pMin, int pMax) {
        return new IntRange(Optional.of(ConstantValue.exactly((float)pMin)), Optional.of(ConstantValue.exactly((float)pMax)));
    }

    /**
     * Create an IntRange with the given minimum (inclusive) and no upper bound.
     */
    public static IntRange lowerBound(int pMin) {
        return new IntRange(Optional.of(ConstantValue.exactly((float)pMin)), Optional.empty());
    }

    /**
     * Create an IntRange with the given maximum (inclusive) and no lower bound.
     */
    public static IntRange upperBound(int pMax) {
        return new IntRange(Optional.empty(), Optional.of(ConstantValue.exactly((float)pMax)));
    }

    /**
     * Clamp the given value so that it falls within this IntRange.
     */
    public int clamp(LootContext pLootContext, int pValue) {
        return this.limiter.apply(pLootContext, pValue);
    }

    /**
     * Check whether the given value falls within this IntRange.
     */
    public boolean test(LootContext pLootContext, int pValue) {
        return this.predicate.test(pLootContext, pValue);
    }

    private OptionalInt unpackExact() {
        if (Objects.equals(this.min, this.max)) {
            NumberProvider numberprovider = this.min;
            if (numberprovider instanceof ConstantValue constantvalue && Math.floor((double)constantvalue.value()) == (double)constantvalue.value()) {
                return OptionalInt.of((int)constantvalue.value());
            }
        }

        return OptionalInt.empty();
    }

    @FunctionalInterface
    interface IntChecker {
        boolean test(LootContext pLootContext, int pValue);
    }

    @FunctionalInterface
    interface IntLimiter {
        int apply(LootContext pLootContext, int pValue);
    }
}
