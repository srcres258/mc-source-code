package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.Codec;

/**
 * The SerializerType for {@link LootPoolEntryContainer}.
 */
public record LootPoolEntryType(Codec<? extends LootPoolEntryContainer> codec) {
}
