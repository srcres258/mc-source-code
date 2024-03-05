package net.minecraft.data.registries;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

/**
 * @deprecated Forge: Use {@link net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider} instead
 */
@Deprecated
public class RegistriesDatapackGenerator implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackOutput output;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private final java.util.function.Predicate<String> namespacePredicate;

    @Deprecated
    public RegistriesDatapackGenerator(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
        this(pOutput, pRegistries, null);
    }

    public RegistriesDatapackGenerator(PackOutput p_256643_, CompletableFuture<HolderLookup.Provider> p_255780_, @org.jetbrains.annotations.Nullable java.util.Set<String> modIds) {
        this.namespacePredicate = modIds == null ? namespace -> true : modIds::contains;
        this.registries = p_255780_;
        this.output = p_256643_;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        return this.registries
            .thenCompose(
                p_256533_ -> {
                    DynamicOps<JsonElement> dynamicops = RegistryOps.create(JsonOps.INSTANCE, p_256533_);
                    return CompletableFuture.allOf(
                             net.neoforged.neoforge.registries.DataPackRegistriesHooks.getDataPackRegistriesWithDimensions()
                            .flatMap(p_256552_ -> this.dumpRegistryCap(pOutput, p_256533_, dynamicops, p_256552_).stream())
                            .toArray(p_255809_ -> new CompletableFuture[p_255809_])
                    );
                }
            );
    }

    private <T> Optional<CompletableFuture<?>> dumpRegistryCap(
        CachedOutput pOutput, HolderLookup.Provider pRegistries, DynamicOps<JsonElement> pOps, RegistryDataLoader.RegistryData<T> pRegistryData
    ) {
        ResourceKey<? extends Registry<T>> resourcekey = pRegistryData.key();
        return pRegistries.lookup(resourcekey)
            .map(
                p_255847_ -> {
                    PackOutput.PathProvider packoutput$pathprovider = this.output
                        .createPathProvider(PackOutput.Target.DATA_PACK, net.neoforged.neoforge.common.CommonHooks.prefixNamespace(resourcekey.location()));
                    return CompletableFuture.allOf(
                        p_255847_.listElements()
                            .filter(holder -> this.namespacePredicate.test(holder.key().location().getNamespace()))
                            .map(
                                p_256105_ -> dumpValue(
                                        packoutput$pathprovider.json(p_256105_.key().location()),
                                        pOutput,
                                        pOps,
                                        pRegistryData.elementCodec(),
                                        (T)p_256105_.value()
                                    )
                            )
                            .toArray(p_256279_ -> new CompletableFuture[p_256279_])
                    );
                }
            );
    }

    private static <E> CompletableFuture<?> dumpValue(
        Path pValuePath, CachedOutput pOutput, DynamicOps<JsonElement> pOps, Encoder<E> pEncoder, E pValue
    ) {
        Optional<JsonElement> optional = pEncoder.encodeStart(pOps, pValue)
            .resultOrPartial(p_255999_ -> LOGGER.error("Couldn't serialize element {}: {}", pValuePath, p_255999_));
        return optional.isPresent() ? DataProvider.saveStable(pOutput, optional.get(), pValuePath) : CompletableFuture.completedFuture(null);
    }

    /**
     * Gets a name for this provider, to use in logging.
     */
    @Override
    public String getName() {
        return "Registries";
    }
}
