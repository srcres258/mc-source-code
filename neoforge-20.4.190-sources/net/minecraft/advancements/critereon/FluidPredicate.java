package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public record FluidPredicate(Optional<TagKey<Fluid>> tag, Optional<Holder<Fluid>> fluid, Optional<StatePropertiesPredicate> properties) {
    public static final Codec<FluidPredicate> CODEC = RecordCodecBuilder.create(
        p_299184_ -> p_299184_.group(
                    ExtraCodecs.strictOptionalField(TagKey.codec(Registries.FLUID), "tag").forGetter(FluidPredicate::tag),
                    ExtraCodecs.strictOptionalField(BuiltInRegistries.FLUID.holderByNameCodec(), "fluid").forGetter(FluidPredicate::fluid),
                    ExtraCodecs.strictOptionalField(StatePropertiesPredicate.CODEC, "state").forGetter(FluidPredicate::properties)
                )
                .apply(p_299184_, FluidPredicate::new)
    );

    public boolean matches(ServerLevel pLevel, BlockPos pPos) {
        if (!pLevel.isLoaded(pPos)) {
            return false;
        } else {
            FluidState fluidstate = pLevel.getFluidState(pPos);
            if (this.tag.isPresent() && !fluidstate.is(this.tag.get())) {
                return false;
            } else if (this.fluid.isPresent() && !fluidstate.is(this.fluid.get().value())) {
                return false;
            } else {
                return !this.properties.isPresent() || this.properties.get().matches(fluidstate);
            }
        }
    }

    public static class Builder {
        private Optional<Holder<Fluid>> fluid = Optional.empty();
        private Optional<TagKey<Fluid>> fluids = Optional.empty();
        private Optional<StatePropertiesPredicate> properties = Optional.empty();

        private Builder() {
        }

        public static FluidPredicate.Builder fluid() {
            return new FluidPredicate.Builder();
        }

        public FluidPredicate.Builder of(Fluid pFluid) {
            this.fluid = Optional.of(pFluid.builtInRegistryHolder());
            return this;
        }

        public FluidPredicate.Builder of(TagKey<Fluid> pFluids) {
            this.fluids = Optional.of(pFluids);
            return this;
        }

        public FluidPredicate.Builder setProperties(StatePropertiesPredicate pProperties) {
            this.properties = Optional.of(pProperties);
            return this;
        }

        public FluidPredicate build() {
            return new FluidPredicate(this.fluids, this.fluid, this.properties);
        }
    }
}
