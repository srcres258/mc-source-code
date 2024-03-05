package net.minecraft.world.item.crafting;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class RecipeCache {
    private final RecipeCache.Entry[] entries;
    private WeakReference<RecipeManager> cachedRecipeManager = new WeakReference<>(null);

    public RecipeCache(int pSize) {
        this.entries = new RecipeCache.Entry[pSize];
    }

    public Optional<CraftingRecipe> get(Level pLevel, CraftingContainer pContainer) {
        if (pContainer.isEmpty()) {
            return Optional.empty();
        } else {
            this.validateRecipeManager(pLevel);

            for(int i = 0; i < this.entries.length; ++i) {
                RecipeCache.Entry recipecache$entry = this.entries[i];
                if (recipecache$entry != null && recipecache$entry.matches(pContainer.getItems())) {
                    this.moveEntryToFront(i);
                    return Optional.ofNullable(recipecache$entry.value());
                }
            }

            return this.compute(pContainer, pLevel);
        }
    }

    private void validateRecipeManager(Level pLevel) {
        RecipeManager recipemanager = pLevel.getRecipeManager();
        if (recipemanager != this.cachedRecipeManager.get()) {
            this.cachedRecipeManager = new WeakReference<>(recipemanager);
            Arrays.fill(this.entries, null);
        }
    }

    private Optional<CraftingRecipe> compute(CraftingContainer pContainer, Level pLevel) {
        Optional<RecipeHolder<CraftingRecipe>> optional = pLevel.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, pContainer, pLevel);
        this.insert(pContainer.getItems(), optional.map(RecipeHolder::value).orElse(null));
        return optional.map(RecipeHolder::value);
    }

    private void moveEntryToFront(int pIndex) {
        if (pIndex > 0) {
            RecipeCache.Entry recipecache$entry = this.entries[pIndex];
            System.arraycopy(this.entries, 0, this.entries, 1, pIndex);
            this.entries[0] = recipecache$entry;
        }
    }

    private void insert(List<ItemStack> pItems, @Nullable CraftingRecipe pRecipe) {
        NonNullList<ItemStack> nonnulllist = NonNullList.withSize(pItems.size(), ItemStack.EMPTY);

        for(int i = 0; i < pItems.size(); ++i) {
            nonnulllist.set(i, pItems.get(i).copyWithCount(1));
        }

        System.arraycopy(this.entries, 0, this.entries, 1, this.entries.length - 1);
        this.entries[0] = new RecipeCache.Entry(nonnulllist, pRecipe);
    }

    static record Entry(NonNullList<ItemStack> key, @Nullable CraftingRecipe value) {
        public boolean matches(List<ItemStack> pKey) {
            if (this.key.size() != pKey.size()) {
                return false;
            } else {
                for(int i = 0; i < this.key.size(); ++i) {
                    if (!ItemStack.isSameItemSameTags(this.key.get(i), pKey.get(i))) {
                        return false;
                    }
                }

                return true;
            }
        }
    }
}
