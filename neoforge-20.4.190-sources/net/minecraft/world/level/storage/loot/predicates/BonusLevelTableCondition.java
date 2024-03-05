package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A LootItemCondition that provides a random chance based on the level of a certain enchantment on the {@linkplain LootContextParams#TOOL tool}.
 * The chances are given as an array of float values that represent the given chance (0..1) for the enchantment level corresponding to the index.
 * {@code [0.2, 0.3, 0.6]} would provide a 20% chance for not enchanted, 30% chance for enchanted at level 1 and 60% chance for enchanted at level 2 or above.
 */
public record BonusLevelTableCondition(Holder<Enchantment> enchantment, List<Float> values) implements LootItemCondition {
    public static final Codec<BonusLevelTableCondition> CODEC = RecordCodecBuilder.create(
        p_298172_ -> p_298172_.group(
                    BuiltInRegistries.ENCHANTMENT.holderByNameCodec().fieldOf("enchantment").forGetter(BonusLevelTableCondition::enchantment),
                    Codec.FLOAT.listOf().fieldOf("chances").forGetter(BonusLevelTableCondition::values)
                )
                .apply(p_298172_, BonusLevelTableCondition::new)
    );

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.TABLE_BONUS;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.TOOL);
    }

    public boolean test(LootContext pContext) {
        ItemStack itemstack = pContext.getParamOrNull(LootContextParams.TOOL);
        int i = itemstack != null ? EnchantmentHelper.getItemEnchantmentLevel(this.enchantment.value(), itemstack) : 0;
        float f = this.values.get(Math.min(i, this.values.size() - 1));
        return pContext.getRandom().nextFloat() < f;
    }

    public static LootItemCondition.Builder bonusLevelFlatChance(Enchantment pEnchantment, float... pChances) {
        List<Float> list = new ArrayList<>(pChances.length);

        for(float f : pChances) {
            list.add(f);
        }

        return () -> new BonusLevelTableCondition(pEnchantment.builtInRegistryHolder(), list);
    }
}
