package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.phys.Vec3;

public class LevitationTrigger extends SimpleCriterionTrigger<LevitationTrigger.TriggerInstance> {
    @Override
    public Codec<LevitationTrigger.TriggerInstance> codec() {
        return LevitationTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Vec3 pStartPos, int pDuration) {
        this.trigger(pPlayer, p_49124_ -> p_49124_.matches(pPlayer, pStartPos, pDuration));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<DistancePredicate> distance, MinMaxBounds.Ints duration)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<LevitationTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_311430_ -> p_311430_.group(
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(LevitationTrigger.TriggerInstance::player),
                        ExtraCodecs.strictOptionalField(DistancePredicate.CODEC, "distance").forGetter(LevitationTrigger.TriggerInstance::distance),
                        ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "duration", MinMaxBounds.Ints.ANY)
                            .forGetter(LevitationTrigger.TriggerInstance::duration)
                    )
                    .apply(p_311430_, LevitationTrigger.TriggerInstance::new)
        );

        public static Criterion<LevitationTrigger.TriggerInstance> levitated(DistancePredicate pDistance) {
            return CriteriaTriggers.LEVITATION
                .createCriterion(new LevitationTrigger.TriggerInstance(Optional.empty(), Optional.of(pDistance), MinMaxBounds.Ints.ANY));
        }

        public boolean matches(ServerPlayer pPlayer, Vec3 pStartPos, int pDuration) {
            if (this.distance.isPresent()
                && !this.distance.get().matches(pStartPos.x, pStartPos.y, pStartPos.z, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ())) {
                return false;
            } else {
                return this.duration.matches(pDuration);
            }
        }
    }
}
