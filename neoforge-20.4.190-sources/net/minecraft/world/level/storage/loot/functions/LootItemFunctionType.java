package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;

/**
 * The SerializerType for {@link LootItemFunction}.
 */
public record LootItemFunctionType(Codec<? extends LootItemFunction> codec) {
}
