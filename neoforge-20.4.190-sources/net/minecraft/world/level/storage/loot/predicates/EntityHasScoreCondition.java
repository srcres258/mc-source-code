package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.IntRange;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.Scoreboard;

/**
 * A LootItemCondition that checks if an Entity selected by a {@link LootContext.EntityTarget} has a given set of scores.
 * If one of the given objectives does not exist or the entity does not have a score for that objective, the condition fails.
 */
public record EntityHasScoreCondition(Map<String, IntRange> scores, LootContext.EntityTarget entityTarget) implements LootItemCondition {
    public static final Codec<EntityHasScoreCondition> CODEC = RecordCodecBuilder.create(
        p_298176_ -> p_298176_.group(
                    Codec.unboundedMap(Codec.STRING, IntRange.CODEC).fieldOf("scores").forGetter(EntityHasScoreCondition::scores),
                    LootContext.EntityTarget.CODEC.fieldOf("entity").forGetter(EntityHasScoreCondition::entityTarget)
                )
                .apply(p_298176_, EntityHasScoreCondition::new)
    );

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.ENTITY_SCORES;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Stream.concat(
                Stream.of(this.entityTarget.getParam()), this.scores.values().stream().flatMap(p_165487_ -> p_165487_.getReferencedContextParams().stream())
            )
            .collect(ImmutableSet.toImmutableSet());
    }

    public boolean test(LootContext pContext) {
        Entity entity = pContext.getParamOrNull(this.entityTarget.getParam());
        if (entity == null) {
            return false;
        } else {
            Scoreboard scoreboard = entity.level().getScoreboard();

            for(Entry<String, IntRange> entry : this.scores.entrySet()) {
                if (!this.hasScore(pContext, entity, scoreboard, entry.getKey(), entry.getValue())) {
                    return false;
                }
            }

            return true;
        }
    }

    protected boolean hasScore(LootContext pLootContext, Entity pTargetEntity, Scoreboard pScoreboard, String pObjectiveName, IntRange pScoreRange) {
        Objective objective = pScoreboard.getObjective(pObjectiveName);
        if (objective == null) {
            return false;
        } else {
            ReadOnlyScoreInfo readonlyscoreinfo = pScoreboard.getPlayerScoreInfo(pTargetEntity, objective);
            return readonlyscoreinfo == null ? false : pScoreRange.test(pLootContext, readonlyscoreinfo.value());
        }
    }

    public static EntityHasScoreCondition.Builder hasScores(LootContext.EntityTarget pEntityTarget) {
        return new EntityHasScoreCondition.Builder(pEntityTarget);
    }

    public static class Builder implements LootItemCondition.Builder {
        private final ImmutableMap.Builder<String, IntRange> scores = ImmutableMap.builder();
        private final LootContext.EntityTarget entityTarget;

        public Builder(LootContext.EntityTarget pEntityTarget) {
            this.entityTarget = pEntityTarget;
        }

        /**
         * Add a check that the score for the given {@code objectiveName} is within {@code scoreRange}.
         */
        public EntityHasScoreCondition.Builder withScore(String pObjectiveName, IntRange pScoreRange) {
            this.scores.put(pObjectiveName, pScoreRange);
            return this;
        }

        @Override
        public LootItemCondition build() {
            return new EntityHasScoreCondition(this.scores.build(), this.entityTarget);
        }
    }
}
