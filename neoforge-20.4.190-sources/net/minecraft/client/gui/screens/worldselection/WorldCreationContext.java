package net.minecraft.client.gui.screens.worldselection;

import java.util.function.BiFunction;
import java.util.function.UnaryOperator;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record WorldCreationContext(
    WorldOptions options,
    Registry<LevelStem> datapackDimensions,
    WorldDimensions selectedDimensions,
    LayeredRegistryAccess<RegistryLayer> worldgenRegistries,
    ReloadableServerResources dataPackResources,
    WorldDataConfiguration dataConfiguration
) {
    public WorldCreationContext(
        WorldGenSettings p_249130_, LayeredRegistryAccess<RegistryLayer> p_248513_, ReloadableServerResources p_251786_, WorldDataConfiguration p_248593_
    ) {
        this(p_249130_.options(), p_249130_.dimensions(), p_248513_, p_251786_, p_248593_);
    }

    public WorldCreationContext(
        WorldOptions p_249836_,
        WorldDimensions p_250641_,
        LayeredRegistryAccess<RegistryLayer> p_251794_,
        ReloadableServerResources p_250560_,
        WorldDataConfiguration p_248539_
    ) {
        this(
            p_249836_,
            p_251794_.getLayer(RegistryLayer.DIMENSIONS).registryOrThrow(Registries.LEVEL_STEM),
            p_250641_,
            p_251794_.replaceFrom(RegistryLayer.DIMENSIONS),
            p_250560_,
            p_248539_
        );
    }

    public WorldCreationContext withSettings(WorldOptions pOptions, WorldDimensions pSelectedDimensions) {
        return new WorldCreationContext(pOptions, this.datapackDimensions, pSelectedDimensions, this.worldgenRegistries, this.dataPackResources, this.dataConfiguration);
    }

    public WorldCreationContext withOptions(WorldCreationContext.OptionsModifier pOptionsModifier) {
        return new WorldCreationContext(
            pOptionsModifier.apply(this.options),
            this.datapackDimensions,
            this.selectedDimensions,
            this.worldgenRegistries,
            this.dataPackResources,
            this.dataConfiguration
        );
    }

    public WorldCreationContext withDimensions(WorldCreationContext.DimensionsUpdater pDimensionsUpdater) {
        return new WorldCreationContext(
            this.options,
            this.datapackDimensions,
            pDimensionsUpdater.apply(this.worldgenLoadContext(), this.selectedDimensions),
            this.worldgenRegistries,
            this.dataPackResources,
            this.dataConfiguration
        );
    }

    public WorldCreationContext withDataConfiguration(WorldDataConfiguration dataConfiguration) {
        return new WorldCreationContext(this.options, this.datapackDimensions, this.selectedDimensions, this.worldgenRegistries, this.dataPackResources, dataConfiguration);
    }

    public RegistryAccess.Frozen worldgenLoadContext() {
        return this.worldgenRegistries.compositeAccess();
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface DimensionsUpdater extends BiFunction<RegistryAccess.Frozen, WorldDimensions, WorldDimensions> {
    }

    @OnlyIn(Dist.CLIENT)
    public interface OptionsModifier extends UnaryOperator<WorldOptions> {
    }
}
