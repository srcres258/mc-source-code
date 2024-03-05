package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.serialization.Lifecycle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import org.apache.commons.lang3.mutable.MutableObject;

public class RegistrySetBuilder {
    private final List<RegistrySetBuilder.RegistryStub<?>> entries = new ArrayList<>();

    static <T> HolderGetter<T> wrapContextLookup(final HolderLookup.RegistryLookup<T> pOwner) {
        return new RegistrySetBuilder.EmptyTagLookup<T>(pOwner) {
            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> p_255765_) {
                return pOwner.get(p_255765_);
            }
        };
    }

    static <T> HolderLookup.RegistryLookup<T> lookupFromMap(
        final ResourceKey<? extends Registry<? extends T>> pRegistryKey, final Lifecycle pLifecycle, final Map<ResourceKey<T>, Holder.Reference<T>> p_312180_
    ) {
        return new HolderLookup.RegistryLookup<T>() {
            @Override
            public ResourceKey<? extends Registry<? extends T>> key() {
                return pRegistryKey;
            }

            @Override
            public Lifecycle registryLifecycle() {
                return pLifecycle;
            }

            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> p_312659_) {
                return Optional.ofNullable(p_312180_.get(p_312659_));
            }

            @Override
            public Stream<Holder.Reference<T>> listElements() {
                return p_312180_.values().stream();
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> p_312111_) {
                return Optional.empty();
            }

            @Override
            public Stream<HolderSet.Named<T>> listTags() {
                return Stream.empty();
            }
        };
    }

    public <T> RegistrySetBuilder add(ResourceKey<? extends Registry<T>> pKey, Lifecycle pLifecycle, RegistrySetBuilder.RegistryBootstrap<T> pBootstrap) {
        this.entries.add(new RegistrySetBuilder.RegistryStub<>(pKey, pLifecycle, pBootstrap));
        return this;
    }

    public <T> RegistrySetBuilder add(ResourceKey<? extends Registry<T>> pKey, RegistrySetBuilder.RegistryBootstrap<T> pBootstrap) {
        return this.add(pKey, Lifecycle.stable(), pBootstrap);
    }

    public List<? extends ResourceKey<? extends Registry<?>>> getEntryKeys() {
        return this.entries.stream().map(RegistrySetBuilder.RegistryStub::key).toList();
    }

    private RegistrySetBuilder.BuildState createState(RegistryAccess pRegistryAccess) {
        RegistrySetBuilder.BuildState registrysetbuilder$buildstate = RegistrySetBuilder.BuildState.create(
            pRegistryAccess, this.entries.stream().map(RegistrySetBuilder.RegistryStub::key)
        );
        this.entries.forEach(p_255629_ -> p_255629_.apply(registrysetbuilder$buildstate));
        return registrysetbuilder$buildstate;
    }

    private static HolderLookup.Provider buildProviderWithContext(RegistryAccess pRegistry, Stream<HolderLookup.RegistryLookup<?>> pLookups) {
        Stream<HolderLookup.RegistryLookup<?>> stream = pRegistry.registries().map(p_258195_ -> p_258195_.value().asLookup());
        return HolderLookup.Provider.create(Stream.concat(stream, pLookups));
    }

    public HolderLookup.Provider build(RegistryAccess pRegistryAccess) {
        RegistrySetBuilder.BuildState registrysetbuilder$buildstate = this.createState(pRegistryAccess);
        Stream<HolderLookup.RegistryLookup<?>> stream = this.entries
            .stream()
            .map(p_311487_ -> p_311487_.collectRegisteredValues(registrysetbuilder$buildstate).buildAsLookup(registrysetbuilder$buildstate.owner));
        HolderLookup.Provider holderlookup$provider = buildProviderWithContext(pRegistryAccess, stream);
        registrysetbuilder$buildstate.reportNotCollectedHolders();
        registrysetbuilder$buildstate.reportUnclaimedRegisteredValues();
        registrysetbuilder$buildstate.throwOnError();
        return holderlookup$provider;
    }

    private HolderLookup.Provider createLazyFullPatchedRegistries(
        RegistryAccess pRegistry,
        HolderLookup.Provider pLookupProvider,
        Cloner.Factory pClonerFactory,
        Map<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.RegistryContents<?>> pRegistries,
        HolderLookup.Provider pRegistryLookupProvider
    ) {
        RegistrySetBuilder.CompositeOwner registrysetbuilder$compositeowner = new RegistrySetBuilder.CompositeOwner();
        MutableObject<HolderLookup.Provider> mutableobject = new MutableObject<>();
        List<HolderLookup.RegistryLookup<?>> list = pRegistries.keySet()
            .stream()
            .map(
                p_311471_ -> this.createLazyFullPatchedRegistries(registrysetbuilder$compositeowner, pClonerFactory, p_311471_, pRegistryLookupProvider, pLookupProvider, mutableobject)
            )
            .peek(registrysetbuilder$compositeowner::add)
            .collect(Collectors.toUnmodifiableList());
        HolderLookup.Provider holderlookup$provider = buildProviderWithContext(pRegistry, list.stream());
        mutableobject.setValue(holderlookup$provider);
        return holderlookup$provider;
    }

    private <T> HolderLookup.RegistryLookup<T> createLazyFullPatchedRegistries(
        HolderOwner<T> pOwner,
        Cloner.Factory pClonerFactory,
        ResourceKey<? extends Registry<? extends T>> pRegistryKey,
        HolderLookup.Provider pRegistryLookupProvider,
        HolderLookup.Provider pLookupProvider,
        MutableObject<HolderLookup.Provider> p_311797_
    ) {
        Cloner<T> cloner = pClonerFactory.cloner(pRegistryKey);
        if (cloner == null) {
            throw new NullPointerException("No cloner for " + pRegistryKey.location());
        } else {
            Map<ResourceKey<T>, Holder.Reference<T>> map = new HashMap<>();
            HolderLookup.RegistryLookup<T> registrylookup = pRegistryLookupProvider.lookupOrThrow(pRegistryKey);
            registrylookup.listElements().forEach(p_311483_ -> {
                ResourceKey<T> resourcekey = p_311483_.key();
                RegistrySetBuilder.LazyHolder<T> lazyholder = new RegistrySetBuilder.LazyHolder<>(pOwner, resourcekey);
                lazyholder.supplier = () -> cloner.clone((T)p_311483_.value(), pRegistryLookupProvider, p_311797_.getValue());
                map.put(resourcekey, lazyholder);
            });
            Optional<HolderLookup.RegistryLookup<T>> lookup = pLookupProvider.lookup(pRegistryKey);
            Lifecycle lifecycle;
            if (lookup.isPresent()) {
                HolderLookup.RegistryLookup<T> registrylookup1 = lookup.get();
                registrylookup1.listElements().forEach(p_311506_ -> {
                    ResourceKey<T> resourcekey = p_311506_.key();
                    map.computeIfAbsent(resourcekey, p_311494_ -> {
                        RegistrySetBuilder.LazyHolder<T> lazyholder = new RegistrySetBuilder.LazyHolder<>(pOwner, resourcekey);
                        lazyholder.supplier = () -> cloner.clone((T) p_311506_.value(), pLookupProvider, p_311797_.getValue());
                        return lazyholder;
                    });
                });
                lifecycle = registrylookup.registryLifecycle().add(registrylookup1.registryLifecycle());
            } else {
                lifecycle = registrylookup.registryLifecycle();
            }
            return lookupFromMap(pRegistryKey, lifecycle, map);
        }
    }

    public RegistrySetBuilder.PatchedRegistries buildPatch(RegistryAccess pRegistry, HolderLookup.Provider pLookupProvider, Cloner.Factory pClonerFactory) {
        RegistrySetBuilder.BuildState registrysetbuilder$buildstate = this.createState(pRegistry);
        Map<ResourceKey<? extends Registry<?>>, RegistrySetBuilder.RegistryContents<?>> map = new HashMap<>();
        this.entries
            .stream()
            .map(p_311508_ -> p_311508_.collectRegisteredValues(registrysetbuilder$buildstate))
            .forEach(p_272339_ -> map.put(p_272339_.key, p_272339_));
        Set<ResourceKey<? extends Registry<?>>> set = pRegistry.listRegistries().collect(Collectors.toUnmodifiableSet());
        pLookupProvider.listRegistries()
            .filter(p_311485_ -> !set.contains(p_311485_))
            .forEach(p_311473_ -> map.putIfAbsent(p_311473_, new RegistrySetBuilder.RegistryContents<>(p_311473_, Lifecycle.stable(), Map.of())));
        Stream<HolderLookup.RegistryLookup<?>> stream = map.values().stream().map(p_311500_ -> p_311500_.buildAsLookup(registrysetbuilder$buildstate.owner));
        HolderLookup.Provider holderlookup$provider = buildProviderWithContext(pRegistry, stream);
        registrysetbuilder$buildstate.reportUnclaimedRegisteredValues();
        registrysetbuilder$buildstate.throwOnError();
        HolderLookup.Provider holderlookup$provider1 = this.createLazyFullPatchedRegistries(pRegistry, pLookupProvider, pClonerFactory, map, holderlookup$provider);
        return new RegistrySetBuilder.PatchedRegistries(holderlookup$provider1, holderlookup$provider);
    }

    static record BuildState(
        RegistrySetBuilder.CompositeOwner owner,
        RegistrySetBuilder.UniversalLookup lookup,
        Map<ResourceLocation, HolderGetter<?>> registries,
        Map<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>> registeredValues,
        List<RuntimeException> errors
    ) {
        public static RegistrySetBuilder.BuildState create(RegistryAccess pRegistryAccess, Stream<ResourceKey<? extends Registry<?>>> pRegistries) {
            RegistrySetBuilder.CompositeOwner registrysetbuilder$compositeowner = new RegistrySetBuilder.CompositeOwner();
            List<RuntimeException> list = new ArrayList<>();
            RegistrySetBuilder.UniversalLookup registrysetbuilder$universallookup = new RegistrySetBuilder.UniversalLookup(registrysetbuilder$compositeowner);
            Builder<ResourceLocation, HolderGetter<?>> builder = ImmutableMap.builder();
            pRegistryAccess.registries()
                .forEach(p_258197_ -> builder.put(p_258197_.key().location(), net.neoforged.neoforge.common.CommonHooks.wrapRegistryLookup(p_258197_.value().asLookup())));
            pRegistries.forEach(p_256603_ -> builder.put(p_256603_.location(), registrysetbuilder$universallookup));
            return new RegistrySetBuilder.BuildState(
                registrysetbuilder$compositeowner, registrysetbuilder$universallookup, builder.build(), new HashMap<>(), list
            );
        }

        public <T> BootstapContext<T> bootstapContext() {
            return new BootstapContext<T>() {
                @Override
                public Holder.Reference<T> register(ResourceKey<T> p_256176_, T p_256422_, Lifecycle p_255924_) {
                    RegistrySetBuilder.RegisteredValue<?> registeredvalue = BuildState.this.registeredValues
                        .put(p_256176_, new RegistrySetBuilder.RegisteredValue(p_256422_, p_255924_));
                    if (registeredvalue != null) {
                        BuildState.this.errors
                            .add(new IllegalStateException("Duplicate registration for " + p_256176_ + ", new=" + p_256422_ + ", old=" + registeredvalue.value));
                    }

                    return BuildState.this.lookup.getOrCreate(p_256176_);
                }

                @Override
                public <S> HolderGetter<S> lookup(ResourceKey<? extends Registry<? extends S>> p_255961_) {
                    return (HolderGetter<S>)BuildState.this.registries.getOrDefault(p_255961_.location(), BuildState.this.lookup);
                }

                @Override
                public <S> Optional<HolderLookup.RegistryLookup<S>> registryLookup(ResourceKey<? extends Registry<? extends S>> registry) {
                    return Optional.ofNullable((HolderLookup.RegistryLookup<S>) BuildState.this.registries.get(registry.location()));
                }
            };
        }

        public void reportUnclaimedRegisteredValues() {
            this.registeredValues
                .forEach((p_256143_, p_256662_) -> this.errors.add(new IllegalStateException("Orpaned value " + p_256662_.value + " for key " + p_256143_)));
        }

        public void reportNotCollectedHolders() {
            for(ResourceKey<Object> resourcekey : this.lookup.holders.keySet()) {
                this.errors.add(new IllegalStateException("Unreferenced key: " + resourcekey));
            }
        }

        public void throwOnError() {
            if (!this.errors.isEmpty()) {
                IllegalStateException illegalstateexception = new IllegalStateException("Errors during registry creation");

                for(RuntimeException runtimeexception : this.errors) {
                    illegalstateexception.addSuppressed(runtimeexception);
                }

                throw illegalstateexception;
            }
        }
    }

    static class CompositeOwner implements HolderOwner<Object> {
        private final Set<HolderOwner<?>> owners = Sets.newIdentityHashSet();

        @Override
        public boolean canSerializeIn(HolderOwner<Object> pOwner) {
            return this.owners.contains(pOwner);
        }

        public void add(HolderOwner<?> pOwner) {
            this.owners.add(pOwner);
        }

        public <T> HolderOwner<T> cast() {
            return (HolderOwner<T>)this;
        }
    }

    abstract static class EmptyTagLookup<T> implements HolderGetter<T> {
        protected final HolderOwner<T> owner;

        protected EmptyTagLookup(HolderOwner<T> pOwner) {
            this.owner = pOwner;
        }

        @Override
        public Optional<HolderSet.Named<T>> get(TagKey<T> pTagKey) {
            return Optional.of(HolderSet.emptyNamed(this.owner, pTagKey));
        }
    }

    static class LazyHolder<T> extends Holder.Reference<T> {
        @Nullable
        Supplier<T> supplier;

        protected LazyHolder(HolderOwner<T> pOwner, @Nullable ResourceKey<T> pKey) {
            super(Holder.Reference.Type.STAND_ALONE, pOwner, pKey, (T)null);
        }

        @Override
        public void bindValue(T pValue) {
            super.bindValue(pValue);
            this.supplier = null;
        }

        @Override
        public T value() {
            if (this.supplier != null) {
                this.bindValue(this.supplier.get());
            }

            return super.value();
        }
    }

    public static record PatchedRegistries(HolderLookup.Provider full, HolderLookup.Provider patches) {
    }

    static record RegisteredValue<T>(T value, Lifecycle lifecycle) {
    }

    @FunctionalInterface
    public interface RegistryBootstrap<T> {
        void run(BootstapContext<T> pContext);
    }

    static record RegistryContents<T>(
        ResourceKey<? extends Registry<? extends T>> key, Lifecycle lifecycle, Map<ResourceKey<T>, RegistrySetBuilder.ValueAndHolder<T>> values
    ) {
        public HolderLookup.RegistryLookup<T> buildAsLookup(RegistrySetBuilder.CompositeOwner pOwner) {
            Map<ResourceKey<T>, Holder.Reference<T>> map = this.values
                .entrySet()
                .stream()
                .collect(
                    Collectors.toUnmodifiableMap(
                        Entry::getKey,
                        p_311927_ -> {
                            RegistrySetBuilder.ValueAndHolder<T> valueandholder = (RegistrySetBuilder.ValueAndHolder)p_311927_.getValue();
                            Holder.Reference<T> reference = valueandholder.holder()
                                .orElseGet(() -> Holder.Reference.createStandAlone(pOwner.cast(), (ResourceKey<T>)p_311927_.getKey()));
                            reference.bindValue(valueandholder.value().value());
                            return reference;
                        }
                    )
                );
            HolderLookup.RegistryLookup<T> registrylookup = RegistrySetBuilder.lookupFromMap(this.key, this.lifecycle, map);
            pOwner.add(registrylookup);
            return registrylookup;
        }
    }

    static record RegistryStub<T>(ResourceKey<? extends Registry<T>> key, Lifecycle lifecycle, RegistrySetBuilder.RegistryBootstrap<T> bootstrap) {
        void apply(RegistrySetBuilder.BuildState pState) {
            this.bootstrap.run(pState.bootstapContext());
        }

        public RegistrySetBuilder.RegistryContents<T> collectRegisteredValues(RegistrySetBuilder.BuildState pBuildState) {
            Map<ResourceKey<T>, RegistrySetBuilder.ValueAndHolder<T>> map = new HashMap<>();
            Iterator<Entry<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>>> iterator = pBuildState.registeredValues.entrySet().iterator();

            while(iterator.hasNext()) {
                Entry<ResourceKey<?>, RegistrySetBuilder.RegisteredValue<?>> entry = iterator.next();
                ResourceKey<?> resourcekey = entry.getKey();
                if (resourcekey.isFor(this.key)) {
                    RegistrySetBuilder.RegisteredValue<T> registeredvalue = (RegistrySetBuilder.RegisteredValue)entry.getValue();
                    Holder.Reference<T> reference = (Holder.Reference<T>)pBuildState.lookup.holders.remove(resourcekey);
                    map.put((ResourceKey<T>)resourcekey, new RegistrySetBuilder.ValueAndHolder<>(registeredvalue, Optional.ofNullable(reference)));
                    iterator.remove();
                }
            }

            return new RegistrySetBuilder.RegistryContents<>(this.key, this.lifecycle, map);
        }
    }

    static class UniversalLookup extends RegistrySetBuilder.EmptyTagLookup<Object> {
        final Map<ResourceKey<Object>, Holder.Reference<Object>> holders = new HashMap<>();

        public UniversalLookup(HolderOwner<Object> pOwner) {
            super(pOwner);
        }

        @Override
        public Optional<Holder.Reference<Object>> get(ResourceKey<Object> pResourceKey) {
            return Optional.of(this.getOrCreate(pResourceKey));
        }

        <T> Holder.Reference<T> getOrCreate(ResourceKey<T> pKey) {
            return (Holder.Reference<T>)this.holders.computeIfAbsent((ResourceKey<Object>)pKey, p_256154_ -> Holder.Reference.createStandAlone(this.owner, p_256154_));
        }
    }

    static record ValueAndHolder<T>(RegistrySetBuilder.RegisteredValue<T> value, Optional<Holder.Reference<T>> holder) {
    }
}
