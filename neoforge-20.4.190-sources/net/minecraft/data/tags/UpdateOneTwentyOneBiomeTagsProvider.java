package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

public class UpdateOneTwentyOneBiomeTagsProvider extends TagsProvider<Biome> {
    public UpdateOneTwentyOneBiomeTagsProvider(
        PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider, CompletableFuture<TagsProvider.TagLookup<Biome>> pParentProvider
    ) {
        super(pOutput, Registries.BIOME, pLookupProvider, pParentProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        this.tag(BiomeTags.HAS_TRIAL_CHAMBERS).addTag(BiomeTags.IS_OVERWORLD);
    }
}
