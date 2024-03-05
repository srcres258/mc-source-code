package net.minecraft.world.level.storage.loot.predicates;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.Set;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * A LootItemCondition that checks the {@linkplain LootContextParams#TOOL tool} against an {@link ItemPredicate}.
 */
public record MatchTool(Optional<ItemPredicate> predicate) implements LootItemCondition {
    public static final Codec<MatchTool> CODEC = RecordCodecBuilder.create(
        p_298194_ -> p_298194_.group(ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "predicate").forGetter(MatchTool::predicate))
                .apply(p_298194_, MatchTool::new)
    );

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.MATCH_TOOL;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.TOOL);
    }

    public boolean test(LootContext pContext) {
        ItemStack itemstack = pContext.getParamOrNull(LootContextParams.TOOL);
        return itemstack != null && (this.predicate.isEmpty() || this.predicate.get().matches(itemstack));
    }

    public static LootItemCondition.Builder toolMatches(ItemPredicate.Builder pToolPredicateBuilder) {
        return () -> new MatchTool(Optional.of(pToolPredicateBuilder.build()));
    }
}
