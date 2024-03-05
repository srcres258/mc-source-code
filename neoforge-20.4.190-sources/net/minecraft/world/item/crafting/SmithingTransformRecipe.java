package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.stream.Stream;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SmithingTransformRecipe implements SmithingRecipe {
    final Ingredient template;
    final Ingredient base;
    final Ingredient addition;
    final ItemStack result;

    public SmithingTransformRecipe(Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, ItemStack pResult) {
        this.template = pTemplate;
        this.base = pBase;
        this.addition = pAddition;
        this.result = pResult;
    }

    /**
     * Used to check if a recipe matches current crafting inventory
     */
    @Override
    public boolean matches(Container pContainer, Level pLevel) {
        return this.template.test(pContainer.getItem(0)) && this.base.test(pContainer.getItem(1)) && this.addition.test(pContainer.getItem(2));
    }

    @Override
    public ItemStack assemble(Container pContainer, RegistryAccess pRegistryAccess) {
        ItemStack itemstack = this.result.copy();
        CompoundTag compoundtag = pContainer.getItem(1).getTag();
        if (compoundtag != null) {
            itemstack.setTag(compoundtag.copy());
        }
        net.neoforged.neoforge.attachment.AttachmentInternals.copyStackAttachments(pContainer.getItem(1), itemstack);

        return itemstack;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess pRegistryAccess) {
        return this.result;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack pStack) {
        return this.template.test(pStack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack pStack) {
        return this.base.test(pStack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack pStack) {
        return this.addition.test(pStack);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return RecipeSerializer.SMITHING_TRANSFORM;
    }

    @Override
    public boolean isIncomplete() {
        return Stream.of(this.template, this.base, this.addition).anyMatch(net.neoforged.neoforge.common.CommonHooks::hasNoElements);
    }

    public static class Serializer implements RecipeSerializer<SmithingTransformRecipe> {
        private static final Codec<SmithingTransformRecipe> CODEC = RecordCodecBuilder.create(
            p_311739_ -> p_311739_.group(
                        Ingredient.CODEC.fieldOf("template").forGetter(p_301310_ -> p_301310_.template),
                        Ingredient.CODEC.fieldOf("base").forGetter(p_300938_ -> p_300938_.base),
                        Ingredient.CODEC.fieldOf("addition").forGetter(p_301153_ -> p_301153_.addition),
                        ItemStack.ITEM_WITH_COUNT_CODEC.fieldOf("result").forGetter(p_300935_ -> p_300935_.result)
                    )
                    .apply(p_311739_, SmithingTransformRecipe::new)
        );

        @Override
        public Codec<SmithingTransformRecipe> codec() {
            return CODEC;
        }

        public SmithingTransformRecipe fromNetwork(FriendlyByteBuf p_267139_) {
            Ingredient ingredient = Ingredient.fromNetwork(p_267139_);
            Ingredient ingredient1 = Ingredient.fromNetwork(p_267139_);
            Ingredient ingredient2 = Ingredient.fromNetwork(p_267139_);
            ItemStack itemstack = p_267139_.readItem();
            return new SmithingTransformRecipe(ingredient, ingredient1, ingredient2, itemstack);
        }

        public void toNetwork(FriendlyByteBuf p_266746_, SmithingTransformRecipe p_266927_) {
            p_266927_.template.toNetwork(p_266746_);
            p_266927_.base.toNetwork(p_266746_);
            p_266927_.addition.toNetwork(p_266746_);
            p_266746_.writeItem(p_266927_.result);
        }
    }
}
