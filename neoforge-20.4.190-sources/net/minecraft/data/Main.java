package net.minecraft.data;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.WorldVersion;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.advancements.packs.UpdateOneTwentyOneAdvancementProvider;
import net.minecraft.data.advancements.packs.VanillaAdvancementProvider;
import net.minecraft.data.info.BiomeParametersDumpReport;
import net.minecraft.data.info.BlockListReport;
import net.minecraft.data.info.CommandsReport;
import net.minecraft.data.info.RegistryDumpReport;
import net.minecraft.data.loot.packs.TradeRebalanceLootTableProvider;
import net.minecraft.data.loot.packs.UpdateOneTwentyOneLootTableProvider;
import net.minecraft.data.loot.packs.VanillaLootTableProvider;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.data.models.ModelProvider;
import net.minecraft.data.recipes.packs.BundleRecipeProvider;
import net.minecraft.data.recipes.packs.UpdateOneTwentyOneRecipeProvider;
import net.minecraft.data.recipes.packs.VanillaRecipeProvider;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.data.registries.UpdateOneTwentyOneRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.data.structures.NbtToSnbt;
import net.minecraft.data.structures.SnbtToNbt;
import net.minecraft.data.structures.StructureUpdater;
import net.minecraft.data.tags.BannerPatternTagsProvider;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.data.tags.CatVariantTagsProvider;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.data.tags.FlatLevelGeneratorPresetTagsProvider;
import net.minecraft.data.tags.FluidTagsProvider;
import net.minecraft.data.tags.GameEventTagsProvider;
import net.minecraft.data.tags.InstrumentTagsProvider;
import net.minecraft.data.tags.PaintingVariantTagsProvider;
import net.minecraft.data.tags.PoiTypeTagsProvider;
import net.minecraft.data.tags.StructureTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.data.tags.TradeRebalanceStructureTagsProvider;
import net.minecraft.data.tags.UpdateOneTwentyOneBiomeTagsProvider;
import net.minecraft.data.tags.UpdateOneTwentyOneBlockTagsProvider;
import net.minecraft.data.tags.UpdateOneTwentyOneDamageTypeTagsProvider;
import net.minecraft.data.tags.UpdateOneTwentyOneEntityTypeTagsProvider;
import net.minecraft.data.tags.UpdateOneTwentyOneItemTagsProvider;
import net.minecraft.data.tags.VanillaBlockTagsProvider;
import net.minecraft.data.tags.VanillaItemTagsProvider;
import net.minecraft.data.tags.WorldPresetTagsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public class Main {
    @DontObfuscate
    public static void main(String[] pArgs) throws IOException {
        SharedConstants.tryDetectVersion();
        OptionParser optionparser = new OptionParser();
        OptionSpec<Void> optionspec = optionparser.accepts("help", "Show the help menu").forHelp();
        OptionSpec<Void> optionspec1 = optionparser.accepts("server", "Include server generators");
        OptionSpec<Void> optionspec2 = optionparser.accepts("client", "Include client generators");
        OptionSpec<Void> optionspec3 = optionparser.accepts("dev", "Include development tools");
        OptionSpec<Void> optionspec4 = optionparser.accepts("reports", "Include data reports");
        OptionSpec<Void> optionspec5 = optionparser.accepts("validate", "Validate inputs");
        OptionSpec<Void> optionspec6 = optionparser.accepts("all", "Include all generators");
        OptionSpec<String> optionspec7 = optionparser.accepts("output", "Output folder").withRequiredArg().defaultsTo("generated");
        OptionSpec<String> optionspec8 = optionparser.accepts("input", "Input folder").withRequiredArg();
        OptionSpec<String> existing = optionparser.accepts("existing", "Existing resource packs that generated resources can reference").withRequiredArg();
        OptionSpec<String> existingMod = optionparser.accepts("existing-mod", "Existing mods that generated resources can reference the resource packs of").withRequiredArg();
        OptionSpec<java.io.File> gameDir = optionparser.accepts("gameDir").withRequiredArg().ofType(java.io.File.class).defaultsTo(new java.io.File(".")).required(); //Need by modlauncher, so lets just eat it
        OptionSpec<String> mod = optionparser.accepts("mod", "A modid to dump").withRequiredArg().withValuesSeparatedBy(",");
        OptionSpec<Void> flat = optionparser.accepts("flat", "Do not append modid prefix to output directory when generating for multiple mods");
        OptionSpec<String> assetIndex = optionparser.accepts("assetIndex").withRequiredArg();
        OptionSpec<java.io.File> assetsDir = optionparser.accepts("assetsDir").withRequiredArg().ofType(java.io.File.class);
        OptionSet optionset = optionparser.parse(pArgs);
        if (!optionset.has(optionspec) && optionset.hasOptions() && !(optionset.specs().size() == 1 && optionset.has(gameDir))) {
            Path path = Paths.get(optionspec7.value(optionset));
            boolean flag = optionset.has(optionspec6);
            boolean flag1 = flag || optionset.has(optionspec2);
            boolean flag2 = flag || optionset.has(optionspec1);
            boolean flag3 = flag || optionset.has(optionspec3);
            boolean flag4 = flag || optionset.has(optionspec4);
            boolean flag5 = flag || optionset.has(optionspec5);
            Collection<Path> inputs = optionset.valuesOf(optionspec8).stream().map(Paths::get).collect(Collectors.toList());
            Collection<Path> existingPacks = optionset.valuesOf(existing).stream().map(Paths::get).collect(Collectors.toList());
            java.util.Set<String> existingMods = new java.util.HashSet<>(optionset.valuesOf(existingMod));
            java.util.Set<String> mods = new java.util.HashSet<>(optionset.valuesOf(mod));
            boolean isFlat = mods.isEmpty() || optionset.has(flat);
            net.neoforged.neoforge.data.loading.DatagenModLoader.begin(mods, path, inputs, existingPacks, existingMods, flag2, flag1, flag3, flag4, flag5, isFlat, optionset.valueOf(assetIndex), optionset.valueOf(assetsDir));
            if (mods.contains("minecraft") || mods.isEmpty()) {
            DataGenerator datagenerator = createStandardGenerator(
                isFlat ? path : path.resolve("minecraft"),
                inputs,
                flag1,
                flag2,
                flag3,
                flag4,
                flag5,
                SharedConstants.getCurrentVersion(),
                true
            );
            datagenerator.run();
            }
        } else {
            optionparser.printHelpOn(System.out);
        }
    }

    private static <T extends DataProvider> DataProvider.Factory<T> bindRegistries(
        BiFunction<PackOutput, CompletableFuture<HolderLookup.Provider>, T> pTagProviderFactory, CompletableFuture<HolderLookup.Provider> pLookupProvider
    ) {
        return p_255476_ -> pTagProviderFactory.apply(p_255476_, pLookupProvider);
    }

    public static DataGenerator createStandardGenerator(
        Path pOutputFolder,
        Collection<Path> pInputFolders,
        boolean pClient,
        boolean pServer,
        boolean pDev,
        boolean pReports,
        boolean pValidate,
        WorldVersion pVersion,
        boolean pAlwaysGenerate
    ) {
        DataGenerator datagenerator = new DataGenerator(pOutputFolder, pVersion, pAlwaysGenerate);
        DataGenerator.PackGenerator datagenerator$packgenerator = datagenerator.getVanillaPack(pClient || pServer);
        datagenerator$packgenerator.addProvider(p_253388_ -> new SnbtToNbt(p_253388_, pInputFolders).addFilter(new StructureUpdater()));
        CompletableFuture<HolderLookup.Provider> completablefuture2 = CompletableFuture.supplyAsync(VanillaRegistries::createLookup, Util.backgroundExecutor());
        DataGenerator.PackGenerator datagenerator$packgenerator1 = datagenerator.getVanillaPack(pClient);
        datagenerator$packgenerator1.addProvider(ModelProvider::new);
        DataGenerator.PackGenerator datagenerator$packgenerator2 = datagenerator.getVanillaPack(pServer);
        datagenerator$packgenerator2.addProvider(bindRegistries(RegistriesDatapackGenerator::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(VanillaAdvancementProvider::create, completablefuture2));
        datagenerator$packgenerator2.addProvider(VanillaLootTableProvider::create);
        datagenerator$packgenerator2.addProvider(VanillaRecipeProvider::new);
        TagsProvider<Block> tagsprovider3 = datagenerator$packgenerator2.addProvider(bindRegistries(VanillaBlockTagsProvider::new, completablefuture2));
        TagsProvider<Item> tagsprovider = datagenerator$packgenerator2.addProvider(
            p_274753_ -> new VanillaItemTagsProvider(p_274753_, completablefuture2, tagsprovider3.contentsGetter())
        );
        TagsProvider<Biome> tagsprovider1 = datagenerator$packgenerator2.addProvider(bindRegistries(BiomeTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(BannerPatternTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(CatVariantTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(DamageTypeTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(EntityTypeTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(FlatLevelGeneratorPresetTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(FluidTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(GameEventTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(InstrumentTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(PaintingVariantTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(PoiTypeTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(StructureTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(bindRegistries(WorldPresetTagsProvider::new, completablefuture2));
        datagenerator$packgenerator2 = datagenerator.getVanillaPack(pDev);
        datagenerator$packgenerator2.addProvider(p_253386_ -> new NbtToSnbt(p_253386_, pInputFolders));
        datagenerator$packgenerator2 = datagenerator.getVanillaPack(pReports);
        datagenerator$packgenerator2.addProvider(bindRegistries(BiomeParametersDumpReport::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(BlockListReport::new);
        datagenerator$packgenerator2.addProvider(bindRegistries(CommandsReport::new, completablefuture2));
        datagenerator$packgenerator2.addProvider(RegistryDumpReport::new);
        datagenerator$packgenerator2 = datagenerator.getBuiltinDatapack(pServer, "bundle");
        datagenerator$packgenerator2.addProvider(BundleRecipeProvider::new);
        datagenerator$packgenerator2.addProvider(
            p_253392_ -> PackMetadataGenerator.forFeaturePack(
                    p_253392_, Component.translatable("dataPack.bundle.description"), FeatureFlagSet.of(FeatureFlags.BUNDLE)
                )
        );
        datagenerator$packgenerator2 = datagenerator.getBuiltinDatapack(pServer, "trade_rebalance");
        datagenerator$packgenerator2.addProvider(
            p_293698_ -> PackMetadataGenerator.forFeaturePack(
                    p_293698_, Component.translatable("dataPack.trade_rebalance.description"), FeatureFlagSet.of(FeatureFlags.TRADE_REBALANCE)
                )
        );
        datagenerator$packgenerator2.addProvider(TradeRebalanceLootTableProvider::create);
        datagenerator$packgenerator2.addProvider(bindRegistries(TradeRebalanceStructureTagsProvider::new, completablefuture2));
        CompletableFuture<RegistrySetBuilder.PatchedRegistries> completablefuture3 = UpdateOneTwentyOneRegistries.createLookup(completablefuture2);
        CompletableFuture<HolderLookup.Provider> completablefuture = completablefuture3.thenApply(RegistrySetBuilder.PatchedRegistries::full);
        CompletableFuture<HolderLookup.Provider> completablefuture1 = completablefuture3.thenApply(RegistrySetBuilder.PatchedRegistries::patches);
        DataGenerator.PackGenerator datagenerator$packgenerator3 = datagenerator.getBuiltinDatapack(pServer, "update_1_21");
        datagenerator$packgenerator3.addProvider(UpdateOneTwentyOneRecipeProvider::new);
        TagsProvider<Block> tagsprovider2 = datagenerator$packgenerator3.addProvider(
            p_307133_ -> new UpdateOneTwentyOneBlockTagsProvider(p_307133_, completablefuture1, tagsprovider3.contentsGetter())
        );
        datagenerator$packgenerator3.addProvider(
            p_307138_ -> new UpdateOneTwentyOneItemTagsProvider(p_307138_, completablefuture1, tagsprovider.contentsGetter(), tagsprovider2.contentsGetter())
        );
        datagenerator$packgenerator3.addProvider(
            p_311512_ -> new UpdateOneTwentyOneBiomeTagsProvider(p_311512_, completablefuture1, tagsprovider1.contentsGetter())
        );
        datagenerator$packgenerator3.addProvider(UpdateOneTwentyOneLootTableProvider::create);
        datagenerator$packgenerator3.addProvider(bindRegistries(RegistriesDatapackGenerator::new, completablefuture1));
        datagenerator$packgenerator3.addProvider(
            p_307134_ -> PackMetadataGenerator.forFeaturePack(
                    p_307134_, Component.translatable("dataPack.update_1_21.description"), FeatureFlagSet.of(FeatureFlags.UPDATE_1_21)
                )
        );
        datagenerator$packgenerator3.addProvider(bindRegistries(UpdateOneTwentyOneEntityTypeTagsProvider::new, completablefuture));
        datagenerator$packgenerator3.addProvider(bindRegistries(UpdateOneTwentyOneDamageTypeTagsProvider::new, completablefuture));
        datagenerator$packgenerator3.addProvider(bindRegistries(UpdateOneTwentyOneAdvancementProvider::create, completablefuture));
        return datagenerator;
    }
}
