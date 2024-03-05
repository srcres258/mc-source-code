package net.minecraft.world.level.storage.loot.providers.number;

import com.mojang.serialization.Codec;

/**
 * The SerializerType for {@link NumberProvider}.
 */
public record LootNumberProviderType(Codec<? extends NumberProvider> codec) {
}
