package net.minecraft.world.level.storage.loot;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class LootDataManager extends net.neoforged.neoforge.resource.ContextAwareReloadListener implements PreparableReloadListener, LootDataResolver {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().create();
    public static final LootDataId<LootTable> EMPTY_LOOT_TABLE_KEY = new LootDataId<>(LootDataType.TABLE, BuiltInLootTables.EMPTY);
    private Map<LootDataId<?>, ?> elements = Map.of();
    private Multimap<LootDataType<?>, ResourceLocation> typeKeys = ImmutableMultimap.of();

    @Override
    public final CompletableFuture<Void> reload(
        PreparableReloadListener.PreparationBarrier pPreparationBarrier,
        ResourceManager pResourceManager,
        ProfilerFiller pPreparationsProfiler,
        ProfilerFiller pReloadProfiler,
        Executor pBackgroundExecutor,
        Executor pGameExecutor
    ) {
        final var decodeOps = net.neoforged.neoforge.common.conditions.ConditionalOps.create(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, registryAccess), this.conditionContext);
        Map<LootDataType<?>, Map<ResourceLocation, ?>> map = new HashMap<>();
        CompletableFuture<?>[] completablefuture = LootDataType.values()
            .map(p_279242_ -> scheduleElementParse(p_279242_, pResourceManager, pBackgroundExecutor, map, decodeOps))
            .toArray(p_279126_ -> new CompletableFuture[p_279126_]);
        return CompletableFuture.allOf(completablefuture).thenCompose(pPreparationBarrier::wait).thenAcceptAsync(p_279096_ -> this.apply(map), pGameExecutor);
    }

    /**
     * @deprecated Neo: use the variant with a custom codec ops
     */
    @Deprecated
    private static <T> CompletableFuture<?> scheduleElementParse(
        LootDataType<T> pLootDataType, ResourceManager pResourceManager, Executor pBackgroundExecutor, Map<LootDataType<?>, Map<ResourceLocation, ?>> pElementCollector
    ) {
        return scheduleElementParse(pLootDataType, pResourceManager, pBackgroundExecutor, pElementCollector, com.mojang.serialization.JsonOps.INSTANCE);
    }
    private static <T> CompletableFuture<?> scheduleElementParse(
        LootDataType<T> p_279205_, ResourceManager p_279441_, Executor p_279233_, Map<LootDataType<?>, Map<ResourceLocation, ?>> p_279241_,
        com.mojang.serialization.DynamicOps<JsonElement> decodeOps
    ) {
        Map<ResourceLocation, T> map = new HashMap<>();
        p_279241_.put(p_279205_, map);
        return CompletableFuture.runAsync(() -> {
            Map<ResourceLocation, JsonElement> map1 = new HashMap<>();
            SimpleJsonResourceReloadListener.scanDirectory(p_279441_, p_279205_.directory(), GSON, map1);
            map1.forEach((p_279416_, p_279151_) -> p_279205_.deserializeOrDefault(p_279416_,  decodeOps, p_279151_).ifPresent(p_279295_ -> map.put(p_279416_, p_279295_)));
        }, p_279233_);
    }

    private void apply(Map<LootDataType<?>, Map<ResourceLocation, ?>> pCollectedElements) {
        Object object = ((Map)pCollectedElements.get(LootDataType.TABLE)).remove(BuiltInLootTables.EMPTY);
        if (object != null) {
            LOGGER.warn("Datapack tried to redefine {} loot table, ignoring", BuiltInLootTables.EMPTY);
        }

        Builder<LootDataId<?>, Object> builder = ImmutableMap.builder();
        com.google.common.collect.ImmutableMultimap.Builder<LootDataType<?>, ResourceLocation> builder1 = ImmutableMultimap.builder();
        pCollectedElements.forEach((p_279449_, p_279262_) -> p_279262_.forEach((p_279130_, p_279313_) -> {
                builder.put(new LootDataId(p_279449_, p_279130_), p_279313_);
                builder1.put(p_279449_, p_279130_);
            }));
        builder.put(EMPTY_LOOT_TABLE_KEY, LootTable.EMPTY);
        ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
        final Map<LootDataId<?>, ?> map = builder.build();
        ValidationContext validationcontext = new ValidationContext(problemreporter$collector, LootContextParamSets.ALL_PARAMS, new LootDataResolver() {
            @Nullable
            @Override
            public <T> T getElement(LootDataId<T> p_279194_) {
                return (T)map.get(p_279194_);
            }
        });
        map.forEach((p_279387_, p_279087_) -> castAndValidate(validationcontext, p_279387_, p_279087_));
        problemreporter$collector.get()
            .forEach((p_279487_, p_279312_) -> LOGGER.warn("Found loot table element validation problem in {}: {}", p_279487_, p_279312_));
        this.elements = map;
        this.typeKeys = builder1.build();
    }

    private static <T> void castAndValidate(ValidationContext pContext, LootDataId<T> pId, Object pElement) {
        pId.type().runValidation(pContext, pId, (T)pElement);
    }

    @Nullable
    @Override
    public <T> T getElement(LootDataId<T> pId) {
        return (T)this.elements.get(pId);
    }

    public Collection<ResourceLocation> getKeys(LootDataType<?> pType) {
        return this.typeKeys.get(pType);
    }
}
