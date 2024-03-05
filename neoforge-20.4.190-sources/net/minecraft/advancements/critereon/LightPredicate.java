package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;

public record LightPredicate(MinMaxBounds.Ints composite) {
    public static final Codec<LightPredicate> CODEC = RecordCodecBuilder.create(
        p_298348_ -> p_298348_.group(
                    ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "light", MinMaxBounds.Ints.ANY).forGetter(LightPredicate::composite)
                )
                .apply(p_298348_, LightPredicate::new)
    );

    public boolean matches(ServerLevel pLevel, BlockPos pPos) {
        if (!pLevel.isLoaded(pPos)) {
            return false;
        } else {
            return this.composite.matches(pLevel.getMaxLocalRawBrightness(pPos));
        }
    }

    public static class Builder {
        private MinMaxBounds.Ints composite = MinMaxBounds.Ints.ANY;

        public static LightPredicate.Builder light() {
            return new LightPredicate.Builder();
        }

        public LightPredicate.Builder setComposite(MinMaxBounds.Ints pComposite) {
            this.composite = pComposite;
            return this;
        }

        public LightPredicate build() {
            return new LightPredicate(this.composite);
        }
    }
}
