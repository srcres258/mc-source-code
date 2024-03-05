package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;

public record DistancePredicate(
    MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z, MinMaxBounds.Doubles horizontal, MinMaxBounds.Doubles absolute
) {
    public static final Codec<DistancePredicate> CODEC = RecordCodecBuilder.create(
        p_298498_ -> p_298498_.group(
                    ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "x", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::x),
                    ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "y", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::y),
                    ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "z", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::z),
                    ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "horizontal", MinMaxBounds.Doubles.ANY)
                        .forGetter(DistancePredicate::horizontal),
                    ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "absolute", MinMaxBounds.Doubles.ANY).forGetter(DistancePredicate::absolute)
                )
                .apply(p_298498_, DistancePredicate::new)
    );

    public static DistancePredicate horizontal(MinMaxBounds.Doubles pHorizontal) {
        return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, pHorizontal, MinMaxBounds.Doubles.ANY);
    }

    public static DistancePredicate vertical(MinMaxBounds.Doubles pVertical) {
        return new DistancePredicate(MinMaxBounds.Doubles.ANY, pVertical, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY);
    }

    public static DistancePredicate absolute(MinMaxBounds.Doubles pAbsolute) {
        return new DistancePredicate(MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, MinMaxBounds.Doubles.ANY, pAbsolute);
    }

    public boolean matches(double pX1, double pY1, double pZ1, double pX2, double pY2, double pZ2) {
        float f = (float)(pX1 - pX2);
        float f1 = (float)(pY1 - pY2);
        float f2 = (float)(pZ1 - pZ2);
        if (!this.x.matches((double)Mth.abs(f)) || !this.y.matches((double)Mth.abs(f1)) || !this.z.matches((double)Mth.abs(f2))) {
            return false;
        } else if (!this.horizontal.matchesSqr((double)(f * f + f2 * f2))) {
            return false;
        } else {
            return this.absolute.matchesSqr((double)(f * f + f1 * f1 + f2 * f2));
        }
    }
}
