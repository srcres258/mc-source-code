package net.minecraft.data.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class UpdateOneTwentyOnePools {
    public static final ResourceKey<StructureTemplatePool> EMPTY = createKey("empty");

    public static ResourceKey<StructureTemplatePool> createKey(String pLocation) {
        return ResourceKey.create(Registries.TEMPLATE_POOL, new ResourceLocation(pLocation));
    }

    public static void register(BootstapContext<StructureTemplatePool> pContext, String pName, StructureTemplatePool pValue) {
        Pools.register(pContext, pName, pValue);
    }

    public static void bootstrap(BootstapContext<StructureTemplatePool> pContext) {
        TrialChambersStructurePools.bootstrap(pContext);
    }
}
