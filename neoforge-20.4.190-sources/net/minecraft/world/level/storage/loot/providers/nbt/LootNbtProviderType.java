package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.serialization.Codec;

/**
 * The SerializerType for {@link NbtProvider}.
 */
public record LootNbtProviderType(Codec<? extends NbtProvider> codec) {
}
