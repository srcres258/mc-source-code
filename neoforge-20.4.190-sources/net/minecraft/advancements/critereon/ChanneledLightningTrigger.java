package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class ChanneledLightningTrigger extends SimpleCriterionTrigger<ChanneledLightningTrigger.TriggerInstance> {
    @Override
    public Codec<ChanneledLightningTrigger.TriggerInstance> codec() {
        return ChanneledLightningTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Collection<? extends Entity> pEntityTriggered) {
        List<LootContext> list = pEntityTriggered.stream().map(p_21720_ -> EntityPredicate.createContext(pPlayer, p_21720_)).collect(Collectors.toList());
        this.trigger(pPlayer, p_21730_ -> p_21730_.matches(list));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, List<ContextAwarePredicate> victims)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<ChanneledLightningTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_312247_ -> p_312247_.group(
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player")
                            .forGetter(ChanneledLightningTrigger.TriggerInstance::player),
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC.listOf(), "victims", List.of())
                            .forGetter(ChanneledLightningTrigger.TriggerInstance::victims)
                    )
                    .apply(p_312247_, ChanneledLightningTrigger.TriggerInstance::new)
        );

        public static Criterion<ChanneledLightningTrigger.TriggerInstance> channeledLightning(EntityPredicate.Builder... pVictims) {
            return CriteriaTriggers.CHANNELED_LIGHTNING
                .createCriterion(new ChanneledLightningTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pVictims)));
        }

        public boolean matches(Collection<? extends LootContext> pVictims) {
            for(ContextAwarePredicate contextawarepredicate : this.victims) {
                boolean flag = false;

                for(LootContext lootcontext : pVictims) {
                    if (contextawarepredicate.matches(lootcontext)) {
                        flag = true;
                        break;
                    }
                }

                if (!flag) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            pValidator.validateEntities(this.victims, ".victims");
        }
    }
}
