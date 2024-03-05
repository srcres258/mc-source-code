package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;

public class LootTableTrigger extends SimpleCriterionTrigger<LootTableTrigger.TriggerInstance> {
    @Override
    public Codec<LootTableTrigger.TriggerInstance> codec() {
        return LootTableTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, ResourceLocation pLootTable) {
        this.trigger(pPlayer, p_54606_ -> p_54606_.matches(pLootTable));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, ResourceLocation lootTable) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<LootTableTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_312125_ -> p_312125_.group(
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(LootTableTrigger.TriggerInstance::player),
                        ResourceLocation.CODEC.fieldOf("loot_table").forGetter(LootTableTrigger.TriggerInstance::lootTable)
                    )
                    .apply(p_312125_, LootTableTrigger.TriggerInstance::new)
        );

        public static Criterion<LootTableTrigger.TriggerInstance> lootTableUsed(ResourceLocation pLootTable) {
            return CriteriaTriggers.GENERATE_LOOT.createCriterion(new LootTableTrigger.TriggerInstance(Optional.empty(), pLootTable));
        }

        public boolean matches(ResourceLocation pLootTable) {
            return this.lootTable.equals(pLootTable);
        }
    }
}
