package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.storage.loot.LootContext;

public class BredAnimalsTrigger extends SimpleCriterionTrigger<BredAnimalsTrigger.TriggerInstance> {
    @Override
    public Codec<BredAnimalsTrigger.TriggerInstance> codec() {
        return BredAnimalsTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Animal pParent, Animal pPartner, @Nullable AgeableMob pChild) {
        LootContext lootcontext = EntityPredicate.createContext(pPlayer, pParent);
        LootContext lootcontext1 = EntityPredicate.createContext(pPlayer, pPartner);
        LootContext lootcontext2 = pChild != null ? EntityPredicate.createContext(pPlayer, pChild) : null;
        this.trigger(pPlayer, p_18653_ -> p_18653_.matches(lootcontext, lootcontext1, lootcontext2));
    }

    public static record TriggerInstance(
        Optional<ContextAwarePredicate> player,
        Optional<ContextAwarePredicate> parent,
        Optional<ContextAwarePredicate> partner,
        Optional<ContextAwarePredicate> child
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<BredAnimalsTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_311402_ -> p_311402_.group(
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(BredAnimalsTrigger.TriggerInstance::player),
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "parent").forGetter(BredAnimalsTrigger.TriggerInstance::parent),
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "partner").forGetter(BredAnimalsTrigger.TriggerInstance::partner),
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "child").forGetter(BredAnimalsTrigger.TriggerInstance::child)
                    )
                    .apply(p_311402_, BredAnimalsTrigger.TriggerInstance::new)
        );

        public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals() {
            return CriteriaTriggers.BRED_ANIMALS
                .createCriterion(new BredAnimalsTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()));
        }

        public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals(EntityPredicate.Builder pChild) {
            return CriteriaTriggers.BRED_ANIMALS
                .createCriterion(
                    new BredAnimalsTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(EntityPredicate.wrap(pChild)))
                );
        }

        public static Criterion<BredAnimalsTrigger.TriggerInstance> bredAnimals(
            Optional<EntityPredicate> pParent, Optional<EntityPredicate> pPartner, Optional<EntityPredicate> pChild
        ) {
            return CriteriaTriggers.BRED_ANIMALS
                .createCriterion(
                    new BredAnimalsTrigger.TriggerInstance(
                        Optional.empty(), EntityPredicate.wrap(pParent), EntityPredicate.wrap(pPartner), EntityPredicate.wrap(pChild)
                    )
                );
        }

        public boolean matches(LootContext pParentContext, LootContext pPartnerContext, @Nullable LootContext pChildContext) {
            if (!this.child.isPresent() || pChildContext != null && this.child.get().matches(pChildContext)) {
                return matches(this.parent, pParentContext) && matches(this.partner, pPartnerContext) || matches(this.parent, pPartnerContext) && matches(this.partner, pParentContext);
            } else {
                return false;
            }
        }

        private static boolean matches(Optional<ContextAwarePredicate> pPredicate, LootContext pContext) {
            return pPredicate.isEmpty() || pPredicate.get().matches(pContext);
        }

        @Override
        public void validate(CriterionValidator pValidator) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(pValidator);
            pValidator.validateEntity(this.parent, ".parent");
            pValidator.validateEntity(this.partner, ".partner");
            pValidator.validateEntity(this.child, ".child");
        }
    }
}
