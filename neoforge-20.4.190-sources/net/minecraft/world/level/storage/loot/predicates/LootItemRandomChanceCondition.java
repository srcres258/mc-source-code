package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * A LootItemCondition that succeeds with a given probability.
 */
public record LootItemRandomChanceCondition(float probability) implements LootItemCondition {
    public static final Codec<LootItemRandomChanceCondition> CODEC = RecordCodecBuilder.create(
        p_298192_ -> p_298192_.group(Codec.FLOAT.fieldOf("chance").forGetter(LootItemRandomChanceCondition::probability))
                .apply(p_298192_, LootItemRandomChanceCondition::new)
    );

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.RANDOM_CHANCE;
    }

    public boolean test(LootContext pContext) {
        return pContext.getRandom().nextFloat() < this.probability;
    }

    public static LootItemCondition.Builder randomChance(float pProbability) {
        return () -> new LootItemRandomChanceCondition(pProbability);
    }
}
