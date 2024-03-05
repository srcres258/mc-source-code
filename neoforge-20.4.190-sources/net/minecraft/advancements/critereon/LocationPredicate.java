package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.levelgen.structure.Structure;

public record LocationPredicate(
    Optional<LocationPredicate.PositionPredicate> position,
    Optional<ResourceKey<Biome>> biome,
    Optional<ResourceKey<Structure>> structure,
    Optional<ResourceKey<Level>> dimension,
    Optional<Boolean> smokey,
    Optional<LightPredicate> light,
    Optional<BlockPredicate> block,
    Optional<FluidPredicate> fluid
) {
    public static final Codec<LocationPredicate> CODEC = RecordCodecBuilder.create(
        p_297907_ -> p_297907_.group(
                    ExtraCodecs.strictOptionalField(LocationPredicate.PositionPredicate.CODEC, "position").forGetter(LocationPredicate::position),
                    ExtraCodecs.strictOptionalField(ResourceKey.codec(Registries.BIOME), "biome").forGetter(LocationPredicate::biome),
                    ExtraCodecs.strictOptionalField(ResourceKey.codec(Registries.STRUCTURE), "structure").forGetter(LocationPredicate::structure),
                    ExtraCodecs.strictOptionalField(ResourceKey.codec(Registries.DIMENSION), "dimension").forGetter(LocationPredicate::dimension),
                    ExtraCodecs.strictOptionalField(Codec.BOOL, "smokey").forGetter(LocationPredicate::smokey),
                    ExtraCodecs.strictOptionalField(LightPredicate.CODEC, "light").forGetter(LocationPredicate::light),
                    ExtraCodecs.strictOptionalField(BlockPredicate.CODEC, "block").forGetter(LocationPredicate::block),
                    ExtraCodecs.strictOptionalField(FluidPredicate.CODEC, "fluid").forGetter(LocationPredicate::fluid)
                )
                .apply(p_297907_, LocationPredicate::new)
    );

    private static Optional<LocationPredicate> of(
        Optional<LocationPredicate.PositionPredicate> pPostition,
        Optional<ResourceKey<Biome>> pBiome,
        Optional<ResourceKey<Structure>> pStructure,
        Optional<ResourceKey<Level>> pDimension,
        Optional<Boolean> pSmokey,
        Optional<LightPredicate> pLight,
        Optional<BlockPredicate> pBlock,
        Optional<FluidPredicate> pFluid
    ) {
        return pPostition.isEmpty()
                && pBiome.isEmpty()
                && pStructure.isEmpty()
                && pDimension.isEmpty()
                && pSmokey.isEmpty()
                && pLight.isEmpty()
                && pBlock.isEmpty()
                && pFluid.isEmpty()
            ? Optional.empty()
            : Optional.of(new LocationPredicate(pPostition, pBiome, pStructure, pDimension, pSmokey, pLight, pBlock, pFluid));
    }

    public boolean matches(ServerLevel pLevel, double pX, double pY, double pZ) {
        if (this.position.isPresent() && !this.position.get().matches(pX, pY, pZ)) {
            return false;
        } else if (this.dimension.isPresent() && this.dimension.get() != pLevel.dimension()) {
            return false;
        } else {
            BlockPos blockpos = BlockPos.containing(pX, pY, pZ);
            boolean flag = pLevel.isLoaded(blockpos);
            if (!this.biome.isPresent() || flag && pLevel.getBiome(blockpos).is(this.biome.get())) {
                if (!this.structure.isPresent() || flag && pLevel.structureManager().getStructureWithPieceAt(blockpos, this.structure.get()).isValid()) {
                    if (!this.smokey.isPresent() || flag && this.smokey.get() == CampfireBlock.isSmokeyPos(pLevel, blockpos)) {
                        if (this.light.isPresent() && !this.light.get().matches(pLevel, blockpos)) {
                            return false;
                        } else if (this.block.isPresent() && !this.block.get().matches(pLevel, blockpos)) {
                            return false;
                        } else {
                            return !this.fluid.isPresent() || this.fluid.get().matches(pLevel, blockpos);
                        }
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }

    public static class Builder {
        private MinMaxBounds.Doubles x = MinMaxBounds.Doubles.ANY;
        private MinMaxBounds.Doubles y = MinMaxBounds.Doubles.ANY;
        private MinMaxBounds.Doubles z = MinMaxBounds.Doubles.ANY;
        private Optional<ResourceKey<Biome>> biome = Optional.empty();
        private Optional<ResourceKey<Structure>> structure = Optional.empty();
        private Optional<ResourceKey<Level>> dimension = Optional.empty();
        private Optional<Boolean> smokey = Optional.empty();
        private Optional<LightPredicate> light = Optional.empty();
        private Optional<BlockPredicate> block = Optional.empty();
        private Optional<FluidPredicate> fluid = Optional.empty();

        public static LocationPredicate.Builder location() {
            return new LocationPredicate.Builder();
        }

        public static LocationPredicate.Builder inBiome(ResourceKey<Biome> pBiome) {
            return location().setBiome(pBiome);
        }

        public static LocationPredicate.Builder inDimension(ResourceKey<Level> pDimension) {
            return location().setDimension(pDimension);
        }

        public static LocationPredicate.Builder inStructure(ResourceKey<Structure> pStucture) {
            return location().setStructure(pStucture);
        }

        public static LocationPredicate.Builder atYLocation(MinMaxBounds.Doubles pY) {
            return location().setY(pY);
        }

        public LocationPredicate.Builder setX(MinMaxBounds.Doubles pX) {
            this.x = pX;
            return this;
        }

        public LocationPredicate.Builder setY(MinMaxBounds.Doubles pY) {
            this.y = pY;
            return this;
        }

        public LocationPredicate.Builder setZ(MinMaxBounds.Doubles pZ) {
            this.z = pZ;
            return this;
        }

        public LocationPredicate.Builder setBiome(ResourceKey<Biome> pBiome) {
            this.biome = Optional.of(pBiome);
            return this;
        }

        public LocationPredicate.Builder setStructure(ResourceKey<Structure> pStructure) {
            this.structure = Optional.of(pStructure);
            return this;
        }

        public LocationPredicate.Builder setDimension(ResourceKey<Level> pDimension) {
            this.dimension = Optional.of(pDimension);
            return this;
        }

        public LocationPredicate.Builder setLight(LightPredicate.Builder pLight) {
            this.light = Optional.of(pLight.build());
            return this;
        }

        public LocationPredicate.Builder setBlock(BlockPredicate.Builder pBlock) {
            this.block = Optional.of(pBlock.build());
            return this;
        }

        public LocationPredicate.Builder setFluid(FluidPredicate.Builder pFluid) {
            this.fluid = Optional.of(pFluid.build());
            return this;
        }

        public LocationPredicate.Builder setSmokey(boolean pSmokey) {
            this.smokey = Optional.of(pSmokey);
            return this;
        }

        public LocationPredicate build() {
            Optional<LocationPredicate.PositionPredicate> optional = LocationPredicate.PositionPredicate.of(this.x, this.y, this.z);
            return new LocationPredicate(optional, this.biome, this.structure, this.dimension, this.smokey, this.light, this.block, this.fluid);
        }
    }

    static record PositionPredicate(MinMaxBounds.Doubles x, MinMaxBounds.Doubles y, MinMaxBounds.Doubles z) {
        public static final Codec<LocationPredicate.PositionPredicate> CODEC = RecordCodecBuilder.create(
            p_299107_ -> p_299107_.group(
                        ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "x", MinMaxBounds.Doubles.ANY)
                            .forGetter(LocationPredicate.PositionPredicate::x),
                        ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "y", MinMaxBounds.Doubles.ANY)
                            .forGetter(LocationPredicate.PositionPredicate::y),
                        ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "z", MinMaxBounds.Doubles.ANY)
                            .forGetter(LocationPredicate.PositionPredicate::z)
                    )
                    .apply(p_299107_, LocationPredicate.PositionPredicate::new)
        );

        static Optional<LocationPredicate.PositionPredicate> of(MinMaxBounds.Doubles pX, MinMaxBounds.Doubles pY, MinMaxBounds.Doubles pZ) {
            return pX.isAny() && pY.isAny() && pZ.isAny()
                ? Optional.empty()
                : Optional.of(new LocationPredicate.PositionPredicate(pX, pY, pZ));
        }

        public boolean matches(double pX, double pY, double pZ) {
            return this.x.matches(pX) && this.y.matches(pY) && this.z.matches(pZ);
        }
    }
}
