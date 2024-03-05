package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A LootItemCondition that matches if the last damage to an entity was done by a player.
 *
 * @see LootContextParams#LAST_DAMAGE_PLAYER
 */
public class LootItemKilledByPlayerCondition implements LootItemCondition {
    private static final LootItemKilledByPlayerCondition INSTANCE = new LootItemKilledByPlayerCondition();
    public static final Codec<LootItemKilledByPlayerCondition> CODEC = Codec.unit(INSTANCE);

    private LootItemKilledByPlayerCondition() {
    }

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.KILLED_BY_PLAYER;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.LAST_DAMAGE_PLAYER);
    }

    public boolean test(LootContext pContext) {
        return pContext.hasParam(LootContextParams.LAST_DAMAGE_PLAYER);
    }

    public static LootItemCondition.Builder killedByPlayer() {
        return () -> INSTANCE;
    }
}
