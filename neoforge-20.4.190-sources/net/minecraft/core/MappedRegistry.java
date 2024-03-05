package net.minecraft.core;

import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;

public class MappedRegistry<T> extends net.neoforged.neoforge.registries.BaseMappedRegistry<T> implements WritableRegistry<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    final ResourceKey<? extends Registry<T>> key;
    private final ObjectList<Holder.Reference<T>> byId = new ObjectArrayList<>(256);
    private final Reference2IntMap<T> toId = Util.make(new Reference2IntOpenHashMap<>(), p_304142_ -> p_304142_.defaultReturnValue(-1));
    private final Map<ResourceLocation, Holder.Reference<T>> byLocation = new HashMap<>();
    private final Map<ResourceKey<T>, Holder.Reference<T>> byKey = new HashMap<>();
    private final Map<T, Holder.Reference<T>> byValue = new IdentityHashMap<>();
    private final Map<T, Lifecycle> lifecycles = new IdentityHashMap<>();
    private Lifecycle registryLifecycle;
    private volatile Map<TagKey<T>, HolderSet.Named<T>> tags = new IdentityHashMap<>();
    private boolean frozen;
    @Nullable
    protected Map<T, Holder.Reference<T>> unregisteredIntrusiveHolders;
    @Nullable
    private List<Holder.Reference<T>> holdersInOrder;
    private int nextId;
    private final HolderLookup.RegistryLookup<T> lookup = new HolderLookup.RegistryLookup<T>() {
        @Override
        public ResourceKey<? extends Registry<? extends T>> key() {
            return MappedRegistry.this.key;
        }

        @Override
        public Lifecycle registryLifecycle() {
            return MappedRegistry.this.registryLifecycle();
        }

        @Override
        public Optional<Holder.Reference<T>> get(ResourceKey<T> p_255624_) {
            return MappedRegistry.this.getHolder(p_255624_);
        }

        @Override
        public Stream<Holder.Reference<T>> listElements() {
            return MappedRegistry.this.holders();
        }

        @Override
        public Optional<HolderSet.Named<T>> get(TagKey<T> p_256277_) {
            return MappedRegistry.this.getTag(p_256277_);
        }

        @Override
        public Stream<HolderSet.Named<T>> listTags() {
            return MappedRegistry.this.getTags().map(Pair::getSecond);
        }

        @Override
        @org.jetbrains.annotations.Nullable
        public <A> A getData(net.neoforged.neoforge.registries.datamaps.DataMapType<T, A> type, ResourceKey<T> key) {
            return MappedRegistry.this.getData(type, key);
        }
    };

    public MappedRegistry(ResourceKey<? extends Registry<T>> pKey, Lifecycle pRegistryLifecycle) {
        this(pKey, pRegistryLifecycle, false);
    }

    public MappedRegistry(ResourceKey<? extends Registry<T>> pKey, Lifecycle pRegistryLifecycle, boolean pHasIntrusiveHolders) {
        this.key = pKey;
        this.registryLifecycle = pRegistryLifecycle;
        if (pHasIntrusiveHolders) {
            this.unregisteredIntrusiveHolders = new IdentityHashMap<>();
        }
    }

    @Override
    public ResourceKey<? extends Registry<T>> key() {
        return this.key;
    }

    @Override
    public String toString() {
        return "Registry[" + this.key + " (" + this.registryLifecycle + ")]";
    }

    private List<Holder.Reference<T>> holdersInOrder() {
        if (this.holdersInOrder == null) {
            this.holdersInOrder = this.byId.stream().filter(Objects::nonNull).toList();
        }

        return this.holdersInOrder;
    }

    private void validateWrite() {
        if (this.frozen) {
            throw new IllegalStateException("Registry is already frozen");
        }
    }

    private void validateWrite(ResourceKey<T> pKey) {
        if (this.frozen) {
            throw new IllegalStateException("Registry is already frozen (trying to add key " + pKey + ")");
        }
    }

    public Holder.Reference<T> registerMapping(int pId, ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle) {
        this.validateWrite(pKey);
        Validate.notNull(pKey);
        Validate.notNull(pValue);
        if (pId > this.getMaxId())
            throw new IllegalStateException(String.format(java.util.Locale.ENGLISH, "Invalid id %d - maximum id range of %d exceeded.", pId, this.getMaxId()));

        if (this.byLocation.containsKey(pKey.location())) {
            Util.pauseInIde(new IllegalStateException("Adding duplicate key '" + pKey + "' to registry"));
        }

        if (this.byValue.containsKey(pValue)) {
            Util.pauseInIde(new IllegalStateException("Adding duplicate value '" + pValue + "' to registry"));
        }

        Holder.Reference<T> reference;
        if (this.unregisteredIntrusiveHolders != null) {
            reference = this.unregisteredIntrusiveHolders.remove(pValue);
            if (reference == null) {
                throw new AssertionError("Missing intrusive holder for " + pKey + ":" + pValue);
            }

            reference.bindKey(pKey);
        } else {
            reference = this.byKey.computeIfAbsent(pKey, p_258168_ -> Holder.Reference.createStandAlone(this.holderOwner(), p_258168_));
            // Forge: Bind the value immediately so it can be queried while the registry is not frozen
            reference.bindValue(pValue);
        }

        this.byKey.put(pKey, reference);
        this.byLocation.put(pKey.location(), reference);
        this.byValue.put(pValue, reference);
        while (this.byId.size() < (pId + 1)) this.byId.add(null);
        this.byId.set(pId, reference);
        this.toId.put(pValue, pId);
        if (this.nextId <= pId) {
            this.nextId = pId + 1;
        }

        this.lifecycles.put(pValue, pLifecycle);
        this.registryLifecycle = this.registryLifecycle.add(pLifecycle);
        this.holdersInOrder = null;
        this.addCallbacks.forEach(addCallback -> addCallback.onAdd(this, pId, pKey, pValue));
        return reference;
    }

    @Override
    public Holder.Reference<T> register(ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle) {
        return this.registerMapping(this.nextId, pKey, pValue, pLifecycle);
    }

    /**
     * @return the name used to identify the given object within this registry or {@code null} if the object is not within this registry
     */
    @Nullable
    @Override
    public ResourceLocation getKey(T pValue) {
        Holder.Reference<T> reference = this.byValue.get(pValue);
        return reference != null ? reference.key().location() : null;
    }

    @Override
    public Optional<ResourceKey<T>> getResourceKey(T pValue) {
        return Optional.ofNullable(this.byValue.get(pValue)).map(Holder.Reference::key);
    }

    /**
     * @return the integer ID used to identify the given object
     */
    @Override
    public int getId(@Nullable T pValue) {
        return this.toId.getInt(pValue);
    }

    @Nullable
    @Override
    public T get(@Nullable ResourceKey<T> pKey) {
        return getValueFromNullable(this.byKey.get(resolve(pKey)));
    }

    @Nullable
    @Override
    public T byId(int pId) {
        return pId >= 0 && pId < this.byId.size() ? getValueFromNullable(this.byId.get(pId)) : null;
    }

    @Override
    public Optional<Holder.Reference<T>> getHolder(int pId) {
        return pId >= 0 && pId < this.byId.size() ? Optional.ofNullable(this.byId.get(pId)) : Optional.empty();
    }

    @Override
    public Optional<Holder.Reference<T>> getHolder(ResourceKey<T> pKey) {
        return Optional.ofNullable(this.byKey.get(resolve(pKey)));
    }

    @Override
    public Holder<T> wrapAsHolder(T pValue) {
        Holder.Reference<T> reference = this.byValue.get(pValue);
        return (Holder<T>)(reference != null ? reference : Holder.direct(pValue));
    }

    Holder.Reference<T> getOrCreateHolderOrThrow(ResourceKey<T> pKey) {
        return this.byKey.computeIfAbsent(resolve(pKey), p_258169_ -> {
            if (this.unregisteredIntrusiveHolders != null) {
                throw new IllegalStateException("This registry can't create new holders without value");
            } else {
                this.validateWrite(p_258169_);
                return Holder.Reference.createStandAlone(this.holderOwner(), p_258169_);
            }
        });
    }

    @Override
    public int size() {
        return this.byKey.size();
    }

    @Override
    public Lifecycle lifecycle(T pValue) {
        return this.lifecycles.get(pValue);
    }

    @Override
    public Lifecycle registryLifecycle() {
        return this.registryLifecycle;
    }

    @Override
    public Iterator<T> iterator() {
        return Iterators.transform(this.holdersInOrder().iterator(), Holder::value);
    }

    @Nullable
    @Override
    public T get(@Nullable ResourceLocation pName) {
        Holder.Reference<T> reference = this.byLocation.get(resolve(pName));
        return getValueFromNullable(reference);
    }

    @Nullable
    private static <T> T getValueFromNullable(@Nullable Holder.Reference<T> pHolder) {
        return pHolder != null ? pHolder.value() : null;
    }

    /**
     * @return all keys in this registry
     */
    @Override
    public Set<ResourceLocation> keySet() {
        return Collections.unmodifiableSet(this.byLocation.keySet());
    }

    @Override
    public Set<ResourceKey<T>> registryKeySet() {
        return Collections.unmodifiableSet(this.byKey.keySet());
    }

    @Override
    public Set<Entry<ResourceKey<T>, T>> entrySet() {
        return Collections.unmodifiableSet(Maps.transformValues(this.byKey, Holder::value).entrySet());
    }

    @Override
    public Stream<Holder.Reference<T>> holders() {
        return this.holdersInOrder().stream();
    }

    @Override
    public Stream<Pair<TagKey<T>, HolderSet.Named<T>>> getTags() {
        return this.tags.entrySet().stream().map(p_211060_ -> Pair.of(p_211060_.getKey(), p_211060_.getValue()));
    }

    @Override
    public HolderSet.Named<T> getOrCreateTag(TagKey<T> pKey) {
        HolderSet.Named<T> named = this.tags.get(pKey);
        if (named == null) {
            named = this.createTag(pKey);
            Map<TagKey<T>, HolderSet.Named<T>> map = new IdentityHashMap<>(this.tags);
            map.put(pKey, named);
            this.tags = map;
        }

        return named;
    }

    private HolderSet.Named<T> createTag(TagKey<T> p_211068_) {
        return new HolderSet.Named<>(this.holderOwner(), p_211068_);
    }

    @Override
    public Stream<TagKey<T>> getTagNames() {
        return this.tags.keySet().stream();
    }

    @Override
    public boolean isEmpty() {
        return this.byKey.isEmpty();
    }

    @Override
    public Optional<Holder.Reference<T>> getRandom(RandomSource pRandom) {
        return Util.getRandomSafe(this.holdersInOrder(), pRandom);
    }

    @Override
    public boolean containsKey(ResourceLocation pName) {
        return this.byLocation.containsKey(pName);
    }

    @Override
    public boolean containsKey(ResourceKey<T> pKey) {
        return this.byKey.containsKey(pKey);
    }

    /** @deprecated Forge: For internal use only. Use the Register events when registering values. */
    @Deprecated
    public void unfreeze() {
        this.frozen = false;
    }

    @Override
    public Registry<T> freeze() {
        if (this.frozen) {
            return this;
        } else {
            this.frozen = true;
            List<ResourceLocation> list = this.byKey
                .entrySet()
                .stream()
                .filter(p_211055_ -> !p_211055_.getValue().isBound())
                .map(p_211794_ -> p_211794_.getKey().location())
                .sorted()
                .toList();
            if (!list.isEmpty()) {
                throw new IllegalStateException("Unbound values in registry " + this.key() + ": " + list);
            } else {
                if (this.unregisteredIntrusiveHolders != null) {
                    if (!this.unregisteredIntrusiveHolders.isEmpty()) {
                        throw new IllegalStateException("Some intrusive holders were not registered: " + this.unregisteredIntrusiveHolders.values());
                    }

                    // Neo: We freeze/unfreeze vanilla registries more than once, so we need to keep the unregistered intrusive holders map around.
                    // this.unregisteredIntrusiveHolders = null;
                }
                this.bakeCallbacks.forEach(bakeCallback -> bakeCallback.onBake(this));

                return this;
            }
        }
    }

    @Override
    public Holder.Reference<T> createIntrusiveHolder(T pValue) {
        if (this.unregisteredIntrusiveHolders == null) {
            throw new IllegalStateException("This registry can't create intrusive holders");
        } else {
            this.validateWrite();
            return this.unregisteredIntrusiveHolders.computeIfAbsent(pValue, p_258166_ -> Holder.Reference.createIntrusive(this.asLookup(), p_258166_));
        }
    }

    @Override
    public Optional<HolderSet.Named<T>> getTag(TagKey<T> pKey) {
        return Optional.ofNullable(this.tags.get(pKey));
    }

    @Override
    public void bindTags(Map<TagKey<T>, List<Holder<T>>> pTagMap) {
        Map<Holder.Reference<T>, List<TagKey<T>>> map = new IdentityHashMap<>();
        this.byKey.values().forEach(p_211801_ -> map.put(p_211801_, new ArrayList<>()));
        pTagMap.forEach((p_211806_, p_211807_) -> {
            for(Holder<T> holder : p_211807_) {
                if (!holder.canSerializeIn(this.asLookup())) {
                    throw new IllegalStateException("Can't create named set " + p_211806_ + " containing value " + holder + " from outside registry " + this);
                }

                if (!(holder instanceof Holder.Reference)) {
                    throw new IllegalStateException("Found direct holder " + holder + " value in tag " + p_211806_);
                }

                Holder.Reference<T> reference = (Holder.Reference)holder;
                map.get(reference).add(p_211806_);
            }
        });
        Set<TagKey<T>> set = Sets.difference(this.tags.keySet(), pTagMap.keySet());
        if (!set.isEmpty()) {
            LOGGER.warn(
                "Not all defined tags for registry {} are present in data pack: {}",
                this.key(),
                set.stream().map(p_211811_ -> p_211811_.location().toString()).sorted().collect(Collectors.joining(", "))
            );
        }

        Map<TagKey<T>, HolderSet.Named<T>> map1 = new IdentityHashMap<>(this.tags);
        pTagMap.forEach((p_211797_, p_211798_) -> map1.computeIfAbsent(p_211797_, this::createTag).bind(p_211798_));
        map.forEach(Holder.Reference::bindTags);
        this.tags = map1;
    }

    @Override
    public void resetTags() {
        this.tags.values().forEach(p_211792_ -> p_211792_.bind(List.of()));
        this.byKey.values().forEach(p_211803_ -> p_211803_.bindTags(Set.of()));
    }

    @Override
    public HolderGetter<T> createRegistrationLookup() {
        this.validateWrite();
        return new HolderGetter<T>() {
            @Override
            public Optional<Holder.Reference<T>> get(ResourceKey<T> p_259097_) {
                return Optional.of(this.getOrThrow(p_259097_));
            }

            @Override
            public Holder.Reference<T> getOrThrow(ResourceKey<T> p_259750_) {
                return MappedRegistry.this.getOrCreateHolderOrThrow(p_259750_);
            }

            @Override
            public Optional<HolderSet.Named<T>> get(TagKey<T> p_259486_) {
                return Optional.of(this.getOrThrow(p_259486_));
            }

            @Override
            public HolderSet.Named<T> getOrThrow(TagKey<T> p_260298_) {
                return MappedRegistry.this.getOrCreateTag(p_260298_);
            }
        };
    }

    @Override
    public HolderOwner<T> holderOwner() {
        return this.lookup;
    }

    @Override
    public HolderLookup.RegistryLookup<T> asLookup() {
        return this.lookup;
    }

    @Override
    protected void clear(boolean full) {
        this.validateWrite();
        this.clearCallbacks.forEach(clearCallback -> clearCallback.onClear(this, full));
        super.clear(full);
        this.byId.clear();
        this.toId.clear();
        nextId = 0;
        if (holdersInOrder != null) holdersInOrder = null;
        if (full) {
            this.byLocation.clear();
            this.byKey.clear();
            this.byValue.clear();
            this.tags.clear();
            this.lifecycles.clear();
            if (unregisteredIntrusiveHolders != null) {
                unregisteredIntrusiveHolders.clear();
                unregisteredIntrusiveHolders = null;
            }
        }
    }

    @Override
    protected void registerIdMapping(ResourceKey<T> key, int id) {
        this.validateWrite(key);
        if (id > this.getMaxId())
            throw new IllegalStateException(String.format(java.util.Locale.ENGLISH, "Invalid id %d - maximum id range of %d exceeded.", id, this.getMaxId()));
        if (0 <= id && id < this.byId.size() && this.byId.get(id) != null) { // Don't use byId() method, it will return the default value if the entry is absent
            throw new IllegalStateException("Duplicate id " + id + " for " + key + " and " + this.getKey(this.byId.get(id).value()));
        }
        if (this.nextId <= id) {
            this.nextId = id + 1;
        }
        var holder = byKey.get(key);
        while (this.byId.size() < (id + 1)) this.byId.add(null);
        this.byId.set(id, holder);
        this.toId.put(holder.value(), id);
    }

    @Override
    public int getId(ResourceLocation name) {
        return getId(get(name));
    }

    @Override
    public int getId(ResourceKey<T> key) {
        return getId(get(key));
    }

    @Override
    public boolean containsValue(T value) {
        return byValue.containsKey(value);
    }
}
