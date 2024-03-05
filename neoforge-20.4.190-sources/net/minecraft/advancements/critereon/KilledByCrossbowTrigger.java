package net.minecraft.advancements.critereon;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootContext;

public class KilledByCrossbowTrigger extends SimpleCriterionTrigger<KilledByCrossbowTrigger.TriggerInstance> {
    @Override
    public Codec<KilledByCrossbowTrigger.TriggerInstance> codec() {
        return KilledByCrossbowTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Collection<Entity> pEntities) {
        List<LootContext> list = Lists.newArrayList();
        Set<EntityType<?>> set = Sets.newHashSet();

        for(Entity entity : pEntities) {
            set.add(entity.getType());
            list.add(EntityPredicate.createContext(pPlayer, entity));
        }

        this.trigger(pPlayer, p_46881_ -> p_46881_.matches(list, set.size()));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, List<ContextAwarePredicate> victims, MinMaxBounds.Ints uniqueEntityTypes)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<KilledByCrossbowTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_312078_ -> p_312078_.group(
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(KilledByCrossbowTrigger.TriggerInstance::player),
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC.listOf(), "victims", List.of())
                            .forGetter(KilledByCrossbowTrigger.TriggerInstance::victims),
                        ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "unique_entity_types", MinMaxBounds.Ints.ANY)
                            .forGetter(KilledByCrossbowTrigger.TriggerInstance::uniqueEntityTypes)
                    )
                    .apply(p_312078_, KilledByCrossbowTrigger.TriggerInstance::new)
        );

        public static Criterion<KilledByCrossbowTrigger.TriggerInstance> crossbowKilled(EntityPredicate.Builder... pVictims) {
            return CriteriaTriggers.KILLED_BY_CROSSBOW
                .createCriterion(new KilledByCrossbowTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(pVictims), MinMaxBounds.Ints.ANY));
        }

        public static Criterion<KilledByCrossbowTrigger.TriggerInstance> crossbowKilled(MinMaxBounds.Ints pUniqueEntityTypes) {
            return CriteriaTriggers.KILLED_BY_CROSSBOW.createCriterion(new KilledByCrossbowTrigger.TriggerInstance(Optional.empty(), List.of(), pUniqueEntityTypes));
        }

        public boolean matches(Collection<LootContext> pContexts, int pBounds) {
            if (!this.victims.isEmpty()) {
                List<LootContext> list = Lists.newArrayList(pContexts);

                for(ContextAwarePredicate contextawarepredicate : this.victims) {
                    boolean flag = false;
                    Iterator<LootContext> iterator = list.iterator();

                    while(iterator.hasNext()) {
                        LootContext lootcontext = iterator.next();
                        if (contextawarepredicate.matches(lootcontext)) {
                            iterator.remove();
                            flag = true;
                            break;
                        }
                    }

                    if (!flag) {
                        return false;
                    }
                }
            }

            return this.uniqueEntityTypes.matches(pBounds);
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            pValidator.validateEntities(this.victims, ".victims");
        }
    }
}
