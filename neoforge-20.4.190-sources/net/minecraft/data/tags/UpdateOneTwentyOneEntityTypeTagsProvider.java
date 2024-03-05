package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;

public class UpdateOneTwentyOneEntityTypeTagsProvider extends IntrinsicHolderTagsProvider<EntityType<?>> {
    public UpdateOneTwentyOneEntityTypeTagsProvider(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider) {
        super(pOutput, Registries.ENTITY_TYPE, pLookupProvider, p_311938_ -> p_311938_.builtInRegistryHolder().key());
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        this.tag(EntityTypeTags.FALL_DAMAGE_IMMUNE).add(EntityType.BREEZE);
        this.tag(EntityTypeTags.DEFLECTS_ARROWS).add(EntityType.BREEZE);
        this.tag(EntityTypeTags.DEFLECTS_TRIDENTS).add(EntityType.BREEZE);
        this.tag(EntityTypeTags.CAN_TURN_IN_BOATS).add(EntityType.BREEZE);
        this.tag(EntityTypeTags.IMPACT_PROJECTILES).add(EntityType.WIND_CHARGE);
    }
}
