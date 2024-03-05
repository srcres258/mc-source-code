package net.minecraft.data.advancements.packs;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.advancements.AdvancementProvider;

public class UpdateOneTwentyOneAdvancementProvider {
    public static AdvancementProvider create(PackOutput pPackOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider) {
        return new AdvancementProvider(pPackOutput, pLookupProvider, List.of(new UpdateOneTwentyOneAdventureAdvancements()));
    }
}
