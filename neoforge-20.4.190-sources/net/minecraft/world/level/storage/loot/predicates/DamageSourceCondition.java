package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.critereon.DamageSourcePredicate;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * A LootItemCondition which checks {@link LootContextParams#ORIGIN} and {@link LootContextParams#DAMAGE_SOURCE} against a {@link DamageSourcePredicate}.
 */
public record DamageSourceCondition(Optional<DamageSourcePredicate> predicate) implements LootItemCondition {
    public static final Codec<DamageSourceCondition> CODEC = RecordCodecBuilder.create(
        p_298174_ -> p_298174_.group(ExtraCodecs.strictOptionalField(DamageSourcePredicate.CODEC, "predicate").forGetter(DamageSourceCondition::predicate))
                .apply(p_298174_, DamageSourceCondition::new)
    );

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.DAMAGE_SOURCE_PROPERTIES;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.ORIGIN, LootContextParams.DAMAGE_SOURCE);
    }

    public boolean test(LootContext pContext) {
        DamageSource damagesource = pContext.getParamOrNull(LootContextParams.DAMAGE_SOURCE);
        Vec3 vec3 = pContext.getParamOrNull(LootContextParams.ORIGIN);
        if (vec3 != null && damagesource != null) {
            return this.predicate.isEmpty() || this.predicate.get().matches(pContext.getLevel(), vec3, damagesource);
        } else {
            return false;
        }
    }

    public static LootItemCondition.Builder hasDamageSource(DamageSourcePredicate.Builder pBuilder) {
        return () -> new DamageSourceCondition(Optional.of(pBuilder.build()));
    }
}
