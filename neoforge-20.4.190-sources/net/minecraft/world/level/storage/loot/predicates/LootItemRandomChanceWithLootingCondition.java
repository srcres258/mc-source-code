package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A LootItemCondition that does a random chance check with a bonus based on the {@linkplain EnchantmentHelper#getMobLooting looting enchantment}.
 */
public record LootItemRandomChanceWithLootingCondition(float percent, float lootingMultiplier) implements LootItemCondition {
    public static final Codec<LootItemRandomChanceWithLootingCondition> CODEC = RecordCodecBuilder.create(
        p_298193_ -> p_298193_.group(
                    Codec.FLOAT.fieldOf("chance").forGetter(LootItemRandomChanceWithLootingCondition::percent),
                    Codec.FLOAT.fieldOf("looting_multiplier").forGetter(LootItemRandomChanceWithLootingCondition::lootingMultiplier)
                )
                .apply(p_298193_, LootItemRandomChanceWithLootingCondition::new)
    );

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.RANDOM_CHANCE_WITH_LOOTING;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.KILLER_ENTITY);
    }

    public boolean test(LootContext pContext) {
        int i = pContext.getLootingModifier();
        return pContext.getRandom().nextFloat() < this.percent + (float)i * this.lootingMultiplier;
    }

    /**
     * @param pChance            The base chance
     * @param pLootingMultiplier The multiplier for the looting level. The result of
     *                           the multiplication is added to the chance.
     */
    public static LootItemCondition.Builder randomChanceAndLootingBoost(float pChance, float pLootingMultiplier) {
        return () -> new LootItemRandomChanceWithLootingCondition(pChance, pLootingMultiplier);
    }
}
