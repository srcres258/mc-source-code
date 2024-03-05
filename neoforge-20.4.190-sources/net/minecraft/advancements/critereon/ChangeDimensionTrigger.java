package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.Level;

public class ChangeDimensionTrigger extends SimpleCriterionTrigger<ChangeDimensionTrigger.TriggerInstance> {
    @Override
    public Codec<ChangeDimensionTrigger.TriggerInstance> codec() {
        return ChangeDimensionTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, ResourceKey<Level> pFromLevel, ResourceKey<Level> pToLevel) {
        this.trigger(pPlayer, p_19768_ -> p_19768_.matches(pFromLevel, pToLevel));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ResourceKey<Level>> from, Optional<ResourceKey<Level>> to)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<ChangeDimensionTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_312097_ -> p_312097_.group(
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(ChangeDimensionTrigger.TriggerInstance::player),
                        ExtraCodecs.strictOptionalField(ResourceKey.codec(Registries.DIMENSION), "from")
                            .forGetter(ChangeDimensionTrigger.TriggerInstance::from),
                        ExtraCodecs.strictOptionalField(ResourceKey.codec(Registries.DIMENSION), "to").forGetter(ChangeDimensionTrigger.TriggerInstance::to)
                    )
                    .apply(p_312097_, ChangeDimensionTrigger.TriggerInstance::new)
        );

        public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimension() {
            return CriteriaTriggers.CHANGED_DIMENSION
                .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.empty()));
        }

        public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimension(ResourceKey<Level> pFrom, ResourceKey<Level> pTo) {
            return CriteriaTriggers.CHANGED_DIMENSION
                .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.of(pFrom), Optional.of(pTo)));
        }

        public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimensionTo(ResourceKey<Level> pTo) {
            return CriteriaTriggers.CHANGED_DIMENSION
                .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(pTo)));
        }

        public static Criterion<ChangeDimensionTrigger.TriggerInstance> changedDimensionFrom(ResourceKey<Level> pFrom) {
            return CriteriaTriggers.CHANGED_DIMENSION
                .createCriterion(new ChangeDimensionTrigger.TriggerInstance(Optional.empty(), Optional.of(pFrom), Optional.empty()));
        }

        public boolean matches(ResourceKey<Level> pFromLevel, ResourceKey<Level> pToLevel) {
            if (this.from.isPresent() && this.from.get() != pFromLevel) {
                return false;
            } else {
                return !this.to.isPresent() || this.to.get() == pToLevel;
            }
        }
    }
}
