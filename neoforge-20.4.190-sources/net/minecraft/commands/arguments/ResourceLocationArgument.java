package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ResourceLocationArgument implements ArgumentType<ResourceLocation> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ADVANCEMENT = new DynamicCommandExceptionType(
        p_304104_ -> Component.translatableEscape("advancement.advancementNotFound", p_304104_)
    );
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_RECIPE = new DynamicCommandExceptionType(
        p_304105_ -> Component.translatableEscape("recipe.notFound", p_304105_)
    );
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_PREDICATE = new DynamicCommandExceptionType(
        p_304103_ -> Component.translatableEscape("predicate.unknown", p_304103_)
    );
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM_MODIFIER = new DynamicCommandExceptionType(
        p_304106_ -> Component.translatableEscape("item_modifier.unknown", p_304106_)
    );

    public static ResourceLocationArgument id() {
        return new ResourceLocationArgument();
    }

    public static AdvancementHolder getAdvancement(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        ResourceLocation resourcelocation = getId(pContext, pName);
        AdvancementHolder advancementholder = pContext.getSource().getAdvancement(resourcelocation);
        if (advancementholder == null) {
            throw ERROR_UNKNOWN_ADVANCEMENT.create(resourcelocation);
        } else {
            return advancementholder;
        }
    }

    public static RecipeHolder<?> getRecipe(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        RecipeManager recipemanager = pContext.getSource().getRecipeManager();
        ResourceLocation resourcelocation = getId(pContext, pName);
        return recipemanager.byKey(resourcelocation).orElseThrow(() -> ERROR_UNKNOWN_RECIPE.create(resourcelocation));
    }

    public static LootItemCondition getPredicate(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        ResourceLocation resourcelocation = getId(pContext, pName);
        LootDataManager lootdatamanager = pContext.getSource().getServer().getLootData();
        LootItemCondition lootitemcondition = lootdatamanager.getElement(LootDataType.PREDICATE, resourcelocation);
        if (lootitemcondition == null) {
            throw ERROR_UNKNOWN_PREDICATE.create(resourcelocation);
        } else {
            return lootitemcondition;
        }
    }

    public static LootItemFunction getItemModifier(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
        ResourceLocation resourcelocation = getId(pContext, pName);
        LootDataManager lootdatamanager = pContext.getSource().getServer().getLootData();
        LootItemFunction lootitemfunction = lootdatamanager.getElement(LootDataType.MODIFIER, resourcelocation);
        if (lootitemfunction == null) {
            throw ERROR_UNKNOWN_ITEM_MODIFIER.create(resourcelocation);
        } else {
            return lootitemfunction;
        }
    }

    public static ResourceLocation getId(CommandContext<CommandSourceStack> pContext, String pName) {
        return pContext.getArgument(pName, ResourceLocation.class);
    }

    public ResourceLocation parse(StringReader pReader) throws CommandSyntaxException {
        return ResourceLocation.read(pReader);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
