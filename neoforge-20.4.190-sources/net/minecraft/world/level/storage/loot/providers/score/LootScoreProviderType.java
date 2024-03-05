package net.minecraft.world.level.storage.loot.providers.score;

import com.mojang.serialization.Codec;

/**
 * The SerializerType for {@link ScoreboardNameProvider}.
 */
public record LootScoreProviderType(Codec<? extends ScoreboardNameProvider> codec) {
}
