package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;

public class UpdateOneTwentyOneDamageTypeTagsProvider extends TagsProvider<DamageType> {
    public UpdateOneTwentyOneDamageTypeTagsProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider) {
        super(pOutput, Registries.DAMAGE_TYPE, pLookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        this.tag(DamageTypeTags.BREEZE_IMMUNE_TO).add(DamageTypes.ARROW, DamageTypes.TRIDENT);
    }
}
