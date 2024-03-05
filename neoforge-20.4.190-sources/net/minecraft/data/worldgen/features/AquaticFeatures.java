package net.minecraft.data.worldgen.features;

import net.minecraft.core.HolderSet;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.CountConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ProbabilityFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleRandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AquaticFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> SEAGRASS_SHORT = FeatureUtils.createKey("seagrass_short");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SEAGRASS_SLIGHTLY_LESS_SHORT = FeatureUtils.createKey("seagrass_slightly_less_short");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SEAGRASS_MID = FeatureUtils.createKey("seagrass_mid");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SEAGRASS_TALL = FeatureUtils.createKey("seagrass_tall");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SEA_PICKLE = FeatureUtils.createKey("sea_pickle");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SEAGRASS_SIMPLE = FeatureUtils.createKey("seagrass_simple");
    public static final ResourceKey<ConfiguredFeature<?, ?>> KELP = FeatureUtils.createKey("kelp");
    public static final ResourceKey<ConfiguredFeature<?, ?>> WARM_OCEAN_VEGETATION = FeatureUtils.createKey("warm_ocean_vegetation");

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> pContext) {
        FeatureUtils.register(pContext, SEAGRASS_SHORT, Feature.SEAGRASS, new ProbabilityFeatureConfiguration(0.3F));
        FeatureUtils.register(pContext, SEAGRASS_SLIGHTLY_LESS_SHORT, Feature.SEAGRASS, new ProbabilityFeatureConfiguration(0.4F));
        FeatureUtils.register(pContext, SEAGRASS_MID, Feature.SEAGRASS, new ProbabilityFeatureConfiguration(0.6F));
        FeatureUtils.register(pContext, SEAGRASS_TALL, Feature.SEAGRASS, new ProbabilityFeatureConfiguration(0.8F));
        FeatureUtils.register(pContext, SEA_PICKLE, Feature.SEA_PICKLE, new CountConfiguration(20));
        FeatureUtils.register(pContext, SEAGRASS_SIMPLE, Feature.SIMPLE_BLOCK, new SimpleBlockConfiguration(BlockStateProvider.simple(Blocks.SEAGRASS)));
        FeatureUtils.register(pContext, KELP, Feature.KELP);
        FeatureUtils.register(
            pContext,
            WARM_OCEAN_VEGETATION,
            Feature.SIMPLE_RANDOM_SELECTOR,
            new SimpleRandomFeatureConfiguration(
                HolderSet.direct(
                    PlacementUtils.inlinePlaced(Feature.CORAL_TREE, FeatureConfiguration.NONE),
                    PlacementUtils.inlinePlaced(Feature.CORAL_CLAW, FeatureConfiguration.NONE),
                    PlacementUtils.inlinePlaced(Feature.CORAL_MUSHROOM, FeatureConfiguration.NONE)
                )
            )
        );
    }
}
