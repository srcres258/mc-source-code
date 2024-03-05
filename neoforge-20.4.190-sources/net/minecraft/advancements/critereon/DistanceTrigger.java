package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;

public class DistanceTrigger extends SimpleCriterionTrigger<DistanceTrigger.TriggerInstance> {
    @Override
    public Codec<DistanceTrigger.TriggerInstance> codec() {
        return DistanceTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Vec3 pPosition) {
        Vec3 vec3 = pPlayer.position();
        this.trigger(pPlayer, p_284572_ -> p_284572_.matches(pPlayer.serverLevel(), pPosition, vec3));
    }

    public static record TriggerInstance(
        Optional<ContextAwarePredicate> player, Optional<LocationPredicate> startPosition, Optional<DistancePredicate> distance
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<DistanceTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_311408_ -> p_311408_.group(
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(DistanceTrigger.TriggerInstance::player),
                        ExtraCodecs.strictOptionalField(LocationPredicate.CODEC, "start_position").forGetter(DistanceTrigger.TriggerInstance::startPosition),
                        ExtraCodecs.strictOptionalField(DistancePredicate.CODEC, "distance").forGetter(DistanceTrigger.TriggerInstance::distance)
                    )
                    .apply(p_311408_, DistanceTrigger.TriggerInstance::new)
        );

        public static Criterion<DistanceTrigger.TriggerInstance> fallFromHeight(
            EntityPredicate.Builder pPlayer, DistancePredicate pDistance, LocationPredicate.Builder pStartPosition
        ) {
            return CriteriaTriggers.FALL_FROM_HEIGHT
                .createCriterion(
                    new DistanceTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(pPlayer)), Optional.of(pStartPosition.build()), Optional.of(pDistance))
                );
        }

        public static Criterion<DistanceTrigger.TriggerInstance> rideEntityInLava(EntityPredicate.Builder pPlayer, DistancePredicate pDistance) {
            return CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER
                .createCriterion(new DistanceTrigger.TriggerInstance(Optional.of(EntityPredicate.wrap(pPlayer)), Optional.empty(), Optional.of(pDistance)));
        }

        public static Criterion<DistanceTrigger.TriggerInstance> travelledThroughNether(DistancePredicate pDistance) {
            return CriteriaTriggers.NETHER_TRAVEL
                .createCriterion(new DistanceTrigger.TriggerInstance(Optional.empty(), Optional.empty(), Optional.of(pDistance)));
        }

        public boolean matches(ServerLevel pLevel, Vec3 pStartPosition, Vec3 pCurrentPosition) {
            if (this.startPosition.isPresent() && !this.startPosition.get().matches(pLevel, pStartPosition.x, pStartPosition.y, pStartPosition.z)) {
                return false;
            } else {
                return !this.distance.isPresent() || this.distance.get().matches(pStartPosition.x, pStartPosition.y, pStartPosition.z, pCurrentPosition.x, pCurrentPosition.y, pCurrentPosition.z);
            }
        }
    }
}
