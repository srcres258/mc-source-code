package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;

public record DamagePredicate(
    MinMaxBounds.Doubles dealtDamage,
    MinMaxBounds.Doubles takenDamage,
    Optional<EntityPredicate> sourceEntity,
    Optional<Boolean> blocked,
    Optional<DamageSourcePredicate> type
) {
    public static final Codec<DamagePredicate> CODEC = RecordCodecBuilder.create(
        p_311407_ -> p_311407_.group(
                    ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "dealt", MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::dealtDamage),
                    ExtraCodecs.strictOptionalField(MinMaxBounds.Doubles.CODEC, "taken", MinMaxBounds.Doubles.ANY).forGetter(DamagePredicate::takenDamage),
                    ExtraCodecs.strictOptionalField(EntityPredicate.CODEC, "source_entity").forGetter(DamagePredicate::sourceEntity),
                    ExtraCodecs.strictOptionalField(Codec.BOOL, "blocked").forGetter(DamagePredicate::blocked),
                    ExtraCodecs.strictOptionalField(DamageSourcePredicate.CODEC, "type").forGetter(DamagePredicate::type)
                )
                .apply(p_311407_, DamagePredicate::new)
    );

    public boolean matches(ServerPlayer pPlayer, DamageSource pSource, float pDealtDamage, float pTakenDamage, boolean pBlocked) {
        if (!this.dealtDamage.matches((double)pDealtDamage)) {
            return false;
        } else if (!this.takenDamage.matches((double)pTakenDamage)) {
            return false;
        } else if (this.sourceEntity.isPresent() && !this.sourceEntity.get().matches(pPlayer, pSource.getEntity())) {
            return false;
        } else if (this.blocked.isPresent() && this.blocked.get() != pBlocked) {
            return false;
        } else {
            return !this.type.isPresent() || this.type.get().matches(pPlayer, pSource);
        }
    }

    public static class Builder {
        private MinMaxBounds.Doubles dealtDamage = MinMaxBounds.Doubles.ANY;
        private MinMaxBounds.Doubles takenDamage = MinMaxBounds.Doubles.ANY;
        private Optional<EntityPredicate> sourceEntity = Optional.empty();
        private Optional<Boolean> blocked = Optional.empty();
        private Optional<DamageSourcePredicate> type = Optional.empty();

        public static DamagePredicate.Builder damageInstance() {
            return new DamagePredicate.Builder();
        }

        public DamagePredicate.Builder dealtDamage(MinMaxBounds.Doubles pDealtDamage) {
            this.dealtDamage = pDealtDamage;
            return this;
        }

        public DamagePredicate.Builder takenDamage(MinMaxBounds.Doubles pTakenDamage) {
            this.takenDamage = pTakenDamage;
            return this;
        }

        public DamagePredicate.Builder sourceEntity(EntityPredicate pSourceEntity) {
            this.sourceEntity = Optional.of(pSourceEntity);
            return this;
        }

        public DamagePredicate.Builder blocked(Boolean pBlocked) {
            this.blocked = Optional.of(pBlocked);
            return this;
        }

        public DamagePredicate.Builder type(DamageSourcePredicate pType) {
            this.type = Optional.of(pType);
            return this;
        }

        public DamagePredicate.Builder type(DamageSourcePredicate.Builder pTypeBuilder) {
            this.type = Optional.of(pTypeBuilder.build());
            return this;
        }

        public DamagePredicate build() {
            return new DamagePredicate(this.dealtDamage, this.takenDamage, this.sourceEntity, this.blocked, this.type);
        }
    }
}
