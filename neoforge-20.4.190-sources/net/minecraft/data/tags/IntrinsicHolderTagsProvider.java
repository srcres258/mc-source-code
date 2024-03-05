package net.minecraft.data.tags;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;

public abstract class IntrinsicHolderTagsProvider<T> extends TagsProvider<T> {
    private final Function<T, ResourceKey<T>> keyExtractor;

    @Deprecated
    public IntrinsicHolderTagsProvider(
        PackOutput pOutput,
        ResourceKey<? extends Registry<T>> pRegistryKey,
        CompletableFuture<HolderLookup.Provider> pLookupProvider,
        Function<T, ResourceKey<T>> pKeyExtractor
    ) {
        this(pOutput, pRegistryKey, pLookupProvider, pKeyExtractor, "vanilla", null);
    }

    @Deprecated
    public IntrinsicHolderTagsProvider(
        PackOutput pOutput,
        ResourceKey<? extends Registry<T>> pRegistryKey,
        CompletableFuture<HolderLookup.Provider> pLookupProvider,
        CompletableFuture<TagsProvider.TagLookup<T>> pParentProvider,
        Function<T, ResourceKey<T>> pKeyExtractor
    ) {
        this(pOutput, pRegistryKey, pLookupProvider, pParentProvider, pKeyExtractor, "vanilla", null);
    }

    public IntrinsicHolderTagsProvider(
        PackOutput p_256164_,
        ResourceKey<? extends Registry<T>> p_256155_,
        CompletableFuture<HolderLookup.Provider> p_256488_,
        Function<T, ResourceKey<T>> p_256168_,
        String modId,
        @javax.annotation.Nullable net.neoforged.neoforge.common.data.ExistingFileHelper existingFileHelper
    ) {
        super(p_256164_, p_256155_, p_256488_, modId, existingFileHelper);
        this.keyExtractor = p_256168_;
    }

    public IntrinsicHolderTagsProvider(
        PackOutput p_275304_,
        ResourceKey<? extends Registry<T>> p_275709_,
        CompletableFuture<HolderLookup.Provider> p_275227_,
        CompletableFuture<TagsProvider.TagLookup<T>> p_275311_,
        Function<T, ResourceKey<T>> p_275566_,
        String modId,
        @javax.annotation.Nullable net.neoforged.neoforge.common.data.ExistingFileHelper existingFileHelper
    ) {
        super(p_275304_, p_275709_, p_275227_, p_275311_, modId, existingFileHelper);
        this.keyExtractor = p_275566_;
    }



    protected IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> tag(TagKey<T> pTag) {
        TagBuilder tagbuilder = this.getOrCreateRawBuilder(pTag);
        return new IntrinsicHolderTagsProvider.IntrinsicTagAppender<>(tagbuilder, this.keyExtractor, this.modId);
    }

    public static class IntrinsicTagAppender<T> extends TagsProvider.TagAppender<T> implements net.neoforged.neoforge.common.extensions.IIntrinsicHolderTagAppenderExtension<T> {
        private final Function<T, ResourceKey<T>> keyExtractor;

        IntrinsicTagAppender(TagBuilder p_256108_, Function<T, ResourceKey<T>> p_256433_, String modId) {
            super(p_256108_, modId);
            this.keyExtractor = p_256433_;
        }

        public IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> addTag(TagKey<T> pTag) {
            super.addTag(pTag);
            return this;
        }

        public final IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> add(T pValue) {
            this.add(this.keyExtractor.apply(pValue));
            return this;
        }

        @SafeVarargs
        public final IntrinsicHolderTagsProvider.IntrinsicTagAppender<T> add(T... pValues) {
            Stream.<T>of(pValues).map(this.keyExtractor).forEach(this::add);
            return this;
        }

        @Override
        public final ResourceKey<T> getKey(T value) {
            return this.keyExtractor.apply(value);
        }
    }
}
