package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * A LootItemCondition that checks a given {@link EntityPredicate} against a given {@link LootContext.EntityTarget}.
 */
public record LootItemEntityPropertyCondition(Optional<EntityPredicate> predicate, LootContext.EntityTarget entityTarget) implements LootItemCondition {
    public static final Codec<LootItemEntityPropertyCondition> CODEC = RecordCodecBuilder.create(
        p_298191_ -> p_298191_.group(
                    ExtraCodecs.strictOptionalField(EntityPredicate.CODEC, "predicate").forGetter(LootItemEntityPropertyCondition::predicate),
                    LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(LootItemEntityPropertyCondition::entityTarget)
                )
                .apply(p_298191_, LootItemEntityPropertyCondition::new)
    );

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.ENTITY_PROPERTIES;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.ORIGIN, this.entityTarget.getParam());
    }

    public boolean test(LootContext pContext) {
        Entity entity = pContext.getParamOrNull(this.entityTarget.getParam());
        Vec3 vec3 = pContext.getParamOrNull(LootContextParams.ORIGIN);
        return this.predicate.isEmpty() || this.predicate.get().matches(pContext.getLevel(), vec3, entity);
    }

    public static LootItemCondition.Builder entityPresent(LootContext.EntityTarget pTarget) {
        return hasProperties(pTarget, EntityPredicate.Builder.entity());
    }

    public static LootItemCondition.Builder hasProperties(LootContext.EntityTarget pTarget, EntityPredicate.Builder pPredicateBuilder) {
        return () -> new LootItemEntityPropertyCondition(Optional.of(pPredicateBuilder.build()), pTarget);
    }

    public static LootItemCondition.Builder hasProperties(LootContext.EntityTarget pTarget, EntityPredicate pEntityPredicate) {
        return () -> new LootItemEntityPropertyCondition(Optional.of(pEntityPredicate), pTarget);
    }
}
