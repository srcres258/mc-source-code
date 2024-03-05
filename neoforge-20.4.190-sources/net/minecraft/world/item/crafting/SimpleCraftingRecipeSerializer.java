package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.network.FriendlyByteBuf;

public class SimpleCraftingRecipeSerializer<T extends CraftingRecipe> implements RecipeSerializer<T> {
    private final SimpleCraftingRecipeSerializer.Factory<T> constructor;
    private final Codec<T> codec;

    public SimpleCraftingRecipeSerializer(SimpleCraftingRecipeSerializer.Factory<T> pConstructor) {
        this.constructor = pConstructor;
        this.codec = RecordCodecBuilder.create(
            p_311736_ -> p_311736_.group(CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(CraftingRecipe::category))
                    .apply(p_311736_, pConstructor::create)
        );
    }

    @Override
    public Codec<T> codec() {
        return this.codec;
    }

    public T fromNetwork(FriendlyByteBuf pBuffer) {
        CraftingBookCategory craftingbookcategory = pBuffer.readEnum(CraftingBookCategory.class);
        return this.constructor.create(craftingbookcategory);
    }

    public void toNetwork(FriendlyByteBuf pBuffer, T pRecipe) {
        pBuffer.writeEnum(pRecipe.category());
    }

    @FunctionalInterface
    public interface Factory<T extends CraftingRecipe> {
        T create(CraftingBookCategory pCategory);
    }
}
