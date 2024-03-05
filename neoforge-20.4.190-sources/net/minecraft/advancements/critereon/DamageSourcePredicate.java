package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.phys.Vec3;

public record DamageSourcePredicate(List<TagPredicate<DamageType>> tags, Optional<EntityPredicate> directEntity, Optional<EntityPredicate> sourceEntity) {
    public static final Codec<DamageSourcePredicate> CODEC = RecordCodecBuilder.create(
        p_298656_ -> p_298656_.group(
                    ExtraCodecs.strictOptionalField(TagPredicate.codec(Registries.DAMAGE_TYPE).listOf(), "tags", List.of())
                        .forGetter(DamageSourcePredicate::tags),
                    ExtraCodecs.strictOptionalField(EntityPredicate.CODEC, "direct_entity").forGetter(DamageSourcePredicate::directEntity),
                    ExtraCodecs.strictOptionalField(EntityPredicate.CODEC, "source_entity").forGetter(DamageSourcePredicate::sourceEntity)
                )
                .apply(p_298656_, DamageSourcePredicate::new)
    );

    public boolean matches(ServerPlayer pPlayer, DamageSource pSource) {
        return this.matches(pPlayer.serverLevel(), pPlayer.position(), pSource);
    }

    public boolean matches(ServerLevel pLevel, Vec3 pPosition, DamageSource pSource) {
        for(TagPredicate<DamageType> tagpredicate : this.tags) {
            if (!tagpredicate.matches(pSource.typeHolder())) {
                return false;
            }
        }

        if (this.directEntity.isPresent() && !this.directEntity.get().matches(pLevel, pPosition, pSource.getDirectEntity())) {
            return false;
        } else {
            return !this.sourceEntity.isPresent() || this.sourceEntity.get().matches(pLevel, pPosition, pSource.getEntity());
        }
    }

    public static class Builder {
        private final ImmutableList.Builder<TagPredicate<DamageType>> tags = ImmutableList.builder();
        private Optional<EntityPredicate> directEntity = Optional.empty();
        private Optional<EntityPredicate> sourceEntity = Optional.empty();

        public static DamageSourcePredicate.Builder damageType() {
            return new DamageSourcePredicate.Builder();
        }

        public DamageSourcePredicate.Builder tag(TagPredicate<DamageType> pTag) {
            this.tags.add(pTag);
            return this;
        }

        public DamageSourcePredicate.Builder direct(EntityPredicate.Builder pDirectEntity) {
            this.directEntity = Optional.of(pDirectEntity.build());
            return this;
        }

        public DamageSourcePredicate.Builder source(EntityPredicate.Builder pSourceEntity) {
            this.sourceEntity = Optional.of(pSourceEntity.build());
            return this;
        }

        public DamageSourcePredicate build() {
            return new DamageSourcePredicate(this.tags.build(), this.directEntity, this.sourceEntity);
        }
    }
}
