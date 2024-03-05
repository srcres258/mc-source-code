package net.minecraft.world.level.storage.loot.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;

/**
 * A LootItemCondition that inverts the output of another one.
 */
public record InvertedLootItemCondition(LootItemCondition term) implements LootItemCondition {
    public static final Codec<InvertedLootItemCondition> CODEC = RecordCodecBuilder.create(
        p_298177_ -> p_298177_.group(LootItemConditions.CODEC.fieldOf("term").forGetter(InvertedLootItemCondition::term))
                .apply(p_298177_, InvertedLootItemCondition::new)
    );

    @Override
    public LootItemConditionType getType() {
        return LootItemConditions.INVERTED;
    }

    public boolean test(LootContext pContext) {
        return !this.term.test(pContext);
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.term.getReferencedContextParams();
    }

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    @Override
    public void validate(ValidationContext pContext) {
        LootItemCondition.super.validate(pContext);
        this.term.validate(pContext);
    }

    public static LootItemCondition.Builder invert(LootItemCondition.Builder pToInvert) {
        InvertedLootItemCondition invertedlootitemcondition = new InvertedLootItemCondition(pToInvert.build());
        return () -> invertedlootitemcondition;
    }
}
