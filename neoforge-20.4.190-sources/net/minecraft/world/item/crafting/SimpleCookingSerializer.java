package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

public class SimpleCookingSerializer<T extends AbstractCookingRecipe> implements RecipeSerializer<T> {
    private final AbstractCookingRecipe.Factory<T> factory;
    private final Codec<T> codec;

    public SimpleCookingSerializer(AbstractCookingRecipe.Factory<T> pFactory, int pCookingTime) {
        this.factory = pFactory;
        this.codec = RecordCodecBuilder.create(
            p_300831_ -> p_300831_.group(
                        ExtraCodecs.strictOptionalField(Codec.STRING, "group", "").forGetter(p_300832_ -> p_300832_.group),
                        CookingBookCategory.CODEC.fieldOf("category").orElse(CookingBookCategory.MISC).forGetter(p_300828_ -> p_300828_.category),
                        Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(p_300833_ -> p_300833_.ingredient),
                        net.neoforged.neoforge.common.crafting.CraftingHelper.smeltingResultCodec().fieldOf("result").forGetter(p_300827_ -> p_300827_.result),
                        Codec.FLOAT.fieldOf("experience").orElse(0.0F).forGetter(p_300826_ -> p_300826_.experience),
                        Codec.INT.fieldOf("cookingtime").orElse(pCookingTime).forGetter(p_300834_ -> p_300834_.cookingTime)
                    )
                    .apply(p_300831_, pFactory::create)
        );
    }

    @Override
    public Codec<T> codec() {
        return this.codec;
    }

    public T fromNetwork(FriendlyByteBuf pBuffer) {
        String s = pBuffer.readUtf();
        CookingBookCategory cookingbookcategory = pBuffer.readEnum(CookingBookCategory.class);
        Ingredient ingredient = Ingredient.fromNetwork(pBuffer);
        ItemStack itemstack = pBuffer.readItem();
        float f = pBuffer.readFloat();
        int i = pBuffer.readVarInt();
        return this.factory.create(s, cookingbookcategory, ingredient, itemstack, f, i);
    }

    public void toNetwork(FriendlyByteBuf pBuffer, T pRecipe) {
        pBuffer.writeUtf(pRecipe.group);
        pBuffer.writeEnum(pRecipe.category());
        pRecipe.ingredient.toNetwork(pBuffer);
        pBuffer.writeItem(pRecipe.result);
        pBuffer.writeFloat(pRecipe.experience);
        pBuffer.writeVarInt(pRecipe.cookingTime);
    }

    public AbstractCookingRecipe create(
        String pGroup, CookingBookCategory pCategory, Ingredient pIngredient, ItemStack pResult, float pExperience, int pCookingTime
    ) {
        return this.factory.create(pGroup, pCategory, pIngredient, pResult, pExperience, pCookingTime);
    }
}
