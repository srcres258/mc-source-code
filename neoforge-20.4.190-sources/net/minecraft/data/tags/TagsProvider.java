package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import org.slf4j.Logger;

public abstract class TagsProvider<T> implements DataProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final PackOutput.PathProvider pathProvider;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;
    private final CompletableFuture<Void> contentsDone = new CompletableFuture<>();
    private final CompletableFuture<TagsProvider.TagLookup<T>> parentProvider;
    protected final ResourceKey<? extends Registry<T>> registryKey;
    protected final Map<ResourceLocation, TagBuilder> builders = Maps.newLinkedHashMap();
    protected final String modId;
    @org.jetbrains.annotations.Nullable
    protected final net.neoforged.neoforge.common.data.ExistingFileHelper existingFileHelper;
    private final net.neoforged.neoforge.common.data.ExistingFileHelper.IResourceType resourceType;
    private final net.neoforged.neoforge.common.data.ExistingFileHelper.IResourceType elementResourceType; // FORGE: Resource type for validating required references to datapack registry elements.

    /**
     * @deprecated Forge: Use the {@linkplain #TagsProvider(PackOutput, ResourceKey,
     *             CompletableFuture, String,
     *             net.neoforged.neoforge.common.data.ExistingFileHelper) mod id
     *             variant}
     */
    protected TagsProvider(PackOutput pOutput, ResourceKey<? extends Registry<T>> pRegistryKey, CompletableFuture<HolderLookup.Provider> pLookupProvider) {
        this(pOutput, pRegistryKey, pLookupProvider, "vanilla", null);
    }
    protected TagsProvider(PackOutput p_256596_, ResourceKey<? extends Registry<T>> p_255886_, CompletableFuture<HolderLookup.Provider> p_256513_, String modId, @org.jetbrains.annotations.Nullable net.neoforged.neoforge.common.data.ExistingFileHelper existingFileHelper) {
        this(p_256596_, p_255886_, p_256513_, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()), modId, existingFileHelper);
    }

    /**
     * @deprecated Forge: Use the {@linkplain #TagsProvider(PackOutput, ResourceKey,
     *             CompletableFuture, CompletableFuture, String,
     *             net.neoforged.neoforge.common.data.ExistingFileHelper) mod id
     *             variant}
     */
    @Deprecated
    protected TagsProvider(
        PackOutput pOutput,
        ResourceKey<? extends Registry<T>> pRegistryKey,
        CompletableFuture<HolderLookup.Provider> pLookupProvider,
        CompletableFuture<TagsProvider.TagLookup<T>> pParentProvider
    ) {
        this(pOutput, pRegistryKey, pLookupProvider, pParentProvider, "vanilla", null);
    }
    protected TagsProvider(PackOutput p_275432_, ResourceKey<? extends Registry<T>> p_275476_, CompletableFuture<HolderLookup.Provider> p_275222_, CompletableFuture<TagsProvider.TagLookup<T>> p_275565_, String modId, @org.jetbrains.annotations.Nullable net.neoforged.neoforge.common.data.ExistingFileHelper existingFileHelper) {
        this.pathProvider = p_275432_.createPathProvider(PackOutput.Target.DATA_PACK, TagManager.getTagDir(p_275476_));
        this.registryKey = p_275476_;
        this.parentProvider = p_275565_;
        this.lookupProvider = p_275222_;
        this.modId = modId;
        this.existingFileHelper = existingFileHelper;
        this.resourceType = new net.neoforged.neoforge.common.data.ExistingFileHelper.ResourceType(net.minecraft.server.packs.PackType.SERVER_DATA, ".json", TagManager.getTagDir(p_275476_));
        this.elementResourceType = new net.neoforged.neoforge.common.data.ExistingFileHelper.ResourceType(net.minecraft.server.packs.PackType.SERVER_DATA, ".json", net.neoforged.neoforge.common.CommonHooks.prefixNamespace(p_275476_.location()));
    }

    // Forge: Allow customizing the path for a given tag or returning null
    @org.jetbrains.annotations.Nullable
    protected Path getPath(ResourceLocation id) {
        return this.pathProvider.json(id);
    }

    /**
     * Gets a name for this provider, to use in logging.
     */
    @Override
    public String getName() {
        return "Tags for " + this.registryKey.location() + " mod id " + this.modId;
    }

    protected abstract void addTags(HolderLookup.Provider pProvider);

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        record CombinedData<T>(HolderLookup.Provider contents, TagsProvider.TagLookup<T> parent) {
        }
        return this.createContentsProvider()
            .thenApply(p_275895_ -> {
                this.contentsDone.complete(null);
                return p_275895_;
            })
            .thenCombineAsync(this.parentProvider, (p_274778_, p_274779_) -> new CombinedData<>(p_274778_, p_274779_))
            .thenCompose(
                p_274774_ -> {
                    HolderLookup.RegistryLookup<T> registrylookup = p_274774_.contents.lookupOrThrow(this.registryKey);
                    Predicate<ResourceLocation> predicate = p_255496_ -> registrylookup.get(ResourceKey.create(this.registryKey, p_255496_)).isPresent();
                    Predicate<ResourceLocation> predicate1 = p_274776_ -> this.builders.containsKey(p_274776_)
                            || p_274774_.parent.contains(TagKey.create(this.registryKey, p_274776_));
                    return CompletableFuture.allOf(
                        this.builders
                            .entrySet()
                            .stream()
                            .map(
                                p_255499_ -> {
                                    ResourceLocation resourcelocation = p_255499_.getKey();
                                    TagBuilder tagbuilder = p_255499_.getValue();
                                    List<TagEntry> list = tagbuilder.build();
                                    List<TagEntry> list1 = java.util.stream.Stream.concat(list.stream(), tagbuilder.getRemoveEntries())
                                              .filter((p_274771_) -> !p_274771_.verifyIfPresent(predicate, predicate1))
                                              .filter(this::missing)
                                              .toList();
                                    if (!list1.isEmpty()) {
                                        throw new IllegalArgumentException(
                                            String.format(
                                                Locale.ROOT,
                                                "Couldn't define tag %s as it is missing following references: %s",
                                                resourcelocation,
                                                list1.stream().map(Objects::toString).collect(Collectors.joining(","))
                                            )
                                        );
                                    } else {
                                        var removed = tagbuilder.getRemoveEntries().toList();
                                        JsonElement jsonelement = TagFile.CODEC.encodeStart(JsonOps.INSTANCE, new TagFile(list, tagbuilder.isReplace(), removed)).getOrThrow(false, LOGGER::error);
                                        Path path = this.getPath(resourcelocation);
                                        if (path == null) return CompletableFuture.completedFuture(null); // Forge: Allow running this data provider without writing it. Recipe provider needs valid tags.
                                        return DataProvider.saveStable(pOutput, jsonelement, path);
                                    }
                                }
                            )
                            .toArray(p_253442_ -> new CompletableFuture[p_253442_])
                    );
                }
            );


    }

    private boolean missing(TagEntry reference) {
        // Optional tags should not be validated

        if (reference.isRequired()) {
            return existingFileHelper == null || !existingFileHelper.exists(reference.getId(), reference.isTag() ? resourceType : elementResourceType);
        }
        return false;
    }

    protected TagsProvider.TagAppender<T> tag(TagKey<T> pTag) {
        TagBuilder tagbuilder = this.getOrCreateRawBuilder(pTag);
        return new TagsProvider.TagAppender<>(tagbuilder, modId);
    }

    protected TagBuilder getOrCreateRawBuilder(TagKey<T> pTag) {
        if (existingFileHelper != null) {
            existingFileHelper.trackGenerated(pTag.location(), resourceType);
        }
        return this.builders.computeIfAbsent(pTag.location(), p_236442_ -> TagBuilder.create());
    }

    public CompletableFuture<TagsProvider.TagLookup<T>> contentsGetter() {
        return this.contentsDone.thenApply(p_276016_ -> p_274772_ -> Optional.ofNullable(this.builders.get(p_274772_.location())));
    }

    protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
        return this.lookupProvider.thenApply(p_274768_ -> {
            this.builders.clear();
            this.addTags(p_274768_);
            return p_274768_;
        });
    }

    public static class TagAppender<T> implements net.neoforged.neoforge.common.extensions.ITagAppenderExtension<T> {
        private final TagBuilder builder;
        private final String modId;

        protected TagAppender(TagBuilder p_236454_, String modId) {
            this.builder = p_236454_;
            this.modId = modId;
        }

        public final TagsProvider.TagAppender<T> add(ResourceKey<T> pKey) {
            this.builder.addElement(pKey.location());
            return this;
        }

        @SafeVarargs
        public final TagsProvider.TagAppender<T> add(ResourceKey<T>... pToAdd) {
            for(ResourceKey<T> resourcekey : pToAdd) {
                this.builder.addElement(resourcekey.location());
            }

            return this;
        }

        public TagsProvider.TagAppender<T> addOptional(ResourceLocation pLocation) {
            this.builder.addOptionalElement(pLocation);
            return this;
        }

        public TagsProvider.TagAppender<T> addTag(TagKey<T> pTag) {
            this.builder.addTag(pTag.location());
            return this;
        }

        public TagsProvider.TagAppender<T> addOptionalTag(ResourceLocation pLocation) {
            this.builder.addOptionalTag(pLocation);
            return this;
        }

        public TagsProvider.TagAppender<T> add(TagEntry tag) {
             builder.add(tag);
             return this;
        }

        public TagBuilder getInternalBuilder() {
             return builder;
        }

        public String getModID() {
             return modId;
        }
    }

    @FunctionalInterface
    public interface TagLookup<T> extends Function<TagKey<T>, Optional<TagBuilder>> {
        static <T> TagsProvider.TagLookup<T> empty() {
            return p_275247_ -> Optional.empty();
        }

        default boolean contains(TagKey<T> pKey) {
            return this.apply(pKey).isPresent();
        }
    }
}
