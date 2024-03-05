package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;

/**
 * The SerializerType for {@link LootItemCondition}.
 */
public record LootItemConditionType(Codec<? extends LootItemCondition> codec) {
}
