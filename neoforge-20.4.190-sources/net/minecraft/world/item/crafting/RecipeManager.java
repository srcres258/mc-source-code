package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class RecipeManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private Map<RecipeType<?>, Map<ResourceLocation, RecipeHolder<?>>> recipes = ImmutableMap.of();
    private Map<ResourceLocation, RecipeHolder<?>> byName = ImmutableMap.of();
    private boolean hasErrors;

    public RecipeManager() {
        super(GSON, "recipes");
    }

    protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        this.hasErrors = false;
        Map<RecipeType<?>, Builder<ResourceLocation, RecipeHolder<?>>> map = Maps.newHashMap();
        Builder<ResourceLocation, RecipeHolder<?>> builder = ImmutableMap.builder();

        for(Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            if (resourcelocation.getPath().startsWith("_")) continue; //Forge: filter anything beginning with "_" as it's used for metadata.

            try {
                Optional<RecipeHolder<?>> recipeHolderOptional = fromJson(resourcelocation, GsonHelper.convertToJsonObject(entry.getValue(), "top element"), net.neoforged.neoforge.common.conditions.ConditionalOps.create(net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, registryAccess), conditionContext));
                recipeHolderOptional.ifPresentOrElse(recipeholder -> {
                    map.computeIfAbsent(recipeholder.value().getType(), p_44075_ -> ImmutableMap.builder()).put(resourcelocation, recipeholder);
                    builder.put(resourcelocation, recipeholder);
                }, () -> {
                    LOGGER.debug("Skipping loading recipe {} as it's conditions were not met", resourcelocation);
                });
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                LOGGER.error("Parsing error loading recipe {}", resourcelocation, jsonparseexception);
            }
        }

        this.recipes = map.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, p_44033_ -> ((Builder)p_44033_.getValue()).build()));
        this.byName = builder.build();
        LOGGER.info("Loaded {} recipes", map.size());
    }

    public boolean hadErrorsLoading() {
        return this.hasErrors;
    }

    public <C extends Container, T extends Recipe<C>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> pRecipeType, C pInventory, Level pLevel) {
        return this.byType(pRecipeType).values().stream().filter(p_300819_ -> p_300819_.value().matches(pInventory, pLevel)).findFirst();
    }

    public <C extends Container, T extends Recipe<C>> Optional<Pair<ResourceLocation, RecipeHolder<T>>> getRecipeFor(
        RecipeType<T> pRecipeType, C pInventory, Level pLevel, @Nullable ResourceLocation pLastRecipe
    ) {
        Map<ResourceLocation, RecipeHolder<T>> map = this.byType(pRecipeType);
        if (pLastRecipe != null) {
            RecipeHolder<T> recipeholder = map.get(pLastRecipe);
            if (recipeholder != null && recipeholder.value().matches(pInventory, pLevel)) {
                return Optional.of(Pair.of(pLastRecipe, recipeholder));
            }
        }

        return map.entrySet()
            .stream()
            .filter(p_300813_ -> p_300813_.getValue().value().matches(pInventory, pLevel))
            .findFirst()
            .map(p_300816_ -> Pair.of(p_300816_.getKey(), p_300816_.getValue()));
    }

    public <C extends Container, T extends Recipe<C>> List<RecipeHolder<T>> getAllRecipesFor(RecipeType<T> pRecipeType) {
        return List.copyOf(this.byType(pRecipeType).values());
    }

    public <C extends Container, T extends Recipe<C>> List<RecipeHolder<T>> getRecipesFor(RecipeType<T> pRecipeType, C pInventory, Level pLevel) {
        return this.byType(pRecipeType)
            .values()
            .stream()
            .filter(p_300822_ -> p_300822_.value().matches(pInventory, pLevel))
            .sorted(Comparator.comparing(p_300815_ -> p_300815_.value().getResultItem(pLevel.registryAccess()).getDescriptionId()))
            .collect(Collectors.toList());
    }

    private <C extends Container, T extends Recipe<C>> Map<ResourceLocation, RecipeHolder<T>> byType(RecipeType<T> pRecipeType) {
        return (Map<ResourceLocation, RecipeHolder<T>>)(Map<ResourceLocation, ?>)this.recipes.getOrDefault(pRecipeType, Collections.emptyMap());
    }

    public <C extends Container, T extends Recipe<C>> NonNullList<ItemStack> getRemainingItemsFor(RecipeType<T> pRecipeType, C pInventory, Level pLevel) {
        Optional<RecipeHolder<T>> optional = this.getRecipeFor(pRecipeType, pInventory, pLevel);
        if (optional.isPresent()) {
            return optional.get().value().getRemainingItems(pInventory);
        } else {
            NonNullList<ItemStack> nonnulllist = NonNullList.withSize(pInventory.getContainerSize(), ItemStack.EMPTY);

            for(int i = 0; i < nonnulllist.size(); ++i) {
                nonnulllist.set(i, pInventory.getItem(i));
            }

            return nonnulllist;
        }
    }

    public Optional<RecipeHolder<?>> byKey(ResourceLocation pRecipeId) {
        return Optional.ofNullable(this.byName.get(pRecipeId));
    }

    public Collection<RecipeHolder<?>> getRecipes() {
        return this.recipes.values().stream().flatMap(p_220270_ -> p_220270_.values().stream()).collect(Collectors.toSet());
    }

    public Stream<ResourceLocation> getRecipeIds() {
        return this.recipes.values().stream().flatMap(p_220258_ -> p_220258_.keySet().stream());
    }

    /**
 * @deprecated Forge: use {@linkplain #fromJson(ResourceLocation, JsonObject,
 *             com.mojang.serialization.DynamicOps) overload with context}.
 */
    @Deprecated
    protected static RecipeHolder<?> fromJson(ResourceLocation pRecipeId, JsonObject pJson) {
        return fromJson(pRecipeId, pJson, JsonOps.INSTANCE).orElseThrow();
    }

    public static Optional<RecipeHolder<?>> fromJson(ResourceLocation p_44046_, JsonObject p_44047_, com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> jsonElementOps) {
        Optional<? extends Recipe<?>> recipe = net.neoforged.neoforge.common.conditions.ICondition.getWithWithConditionsCodec(Recipe.CONDITIONAL_CODEC, jsonElementOps, p_44047_);
        return recipe.map(r -> new RecipeHolder<>(p_44046_, r));
    }

    public void replaceRecipes(Iterable<RecipeHolder<?>> pRecipes) {
        this.hasErrors = false;
        Map<RecipeType<?>, Map<ResourceLocation, RecipeHolder<?>>> map = Maps.newHashMap();
        Builder<ResourceLocation, RecipeHolder<?>> builder = ImmutableMap.builder();
        pRecipes.forEach(p_300825_ -> {
            Map<ResourceLocation, RecipeHolder<?>> map1 = map.computeIfAbsent(p_300825_.value().getType(), p_220272_ -> Maps.newHashMap());
            ResourceLocation resourcelocation = p_300825_.id();
            RecipeHolder<?> recipeholder = map1.put(resourcelocation, p_300825_);
            builder.put(resourcelocation, p_300825_);
            if (recipeholder != null) {
                throw new IllegalStateException("Duplicate recipe ignored with ID " + resourcelocation);
            }
        });
        this.recipes = ImmutableMap.copyOf(map);
        this.byName = builder.build();
    }

    public static <C extends Container, T extends Recipe<C>> RecipeManager.CachedCheck<C, T> createCheck(final RecipeType<T> pRecipeType) {
        return new RecipeManager.CachedCheck<C, T>() {
            @Nullable
            private ResourceLocation lastRecipe;

            @Override
            public Optional<RecipeHolder<T>> getRecipeFor(C p_220278_, Level p_220279_) {
                RecipeManager recipemanager = p_220279_.getRecipeManager();
                Optional<Pair<ResourceLocation, RecipeHolder<T>>> optional = recipemanager.getRecipeFor(pRecipeType, p_220278_, p_220279_, this.lastRecipe);
                if (optional.isPresent()) {
                    Pair<ResourceLocation, RecipeHolder<T>> pair = optional.get();
                    this.lastRecipe = pair.getFirst();
                    return Optional.of(pair.getSecond());
                } else {
                    return Optional.empty();
                }
            }
        };
    }

    public interface CachedCheck<C extends Container, T extends Recipe<C>> {
        Optional<RecipeHolder<T>> getRecipeFor(C pContainer, Level pLevel);
    }
}
