package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

/**
 * Applies a random enchantment to the stack.
 *
 * @see EnchantmentHelper#enchantItem
 */
public class EnchantWithLevelsFunction extends LootItemConditionalFunction {
    public static final Codec<EnchantWithLevelsFunction> CODEC = RecordCodecBuilder.create(
        p_298469_ -> commonFields(p_298469_)
                .and(
                    p_298469_.group(
                        NumberProviders.CODEC.fieldOf("levels").forGetter(p_298844_ -> p_298844_.levels),
                        Codec.BOOL.fieldOf("treasure").orElse(false).forGetter(p_299256_ -> p_299256_.treasure)
                    )
                )
                .apply(p_298469_, EnchantWithLevelsFunction::new)
    );
    private final NumberProvider levels;
    private final boolean treasure;

    EnchantWithLevelsFunction(List<LootItemCondition> p_298580_, NumberProvider p_165194_, boolean p_165195_) {
        super(p_298580_);
        this.levels = p_165194_;
        this.treasure = p_165195_;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.ENCHANT_WITH_LEVELS;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.levels.getReferencedContextParams();
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        RandomSource randomsource = pContext.getRandom();
        return EnchantmentHelper.enchantItem(randomsource, pStack, this.levels.getInt(pContext), this.treasure);
    }

    public static EnchantWithLevelsFunction.Builder enchantWithLevels(NumberProvider pLevels) {
        return new EnchantWithLevelsFunction.Builder(pLevels);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<EnchantWithLevelsFunction.Builder> {
        private final NumberProvider levels;
        private boolean treasure;

        public Builder(NumberProvider pLevels) {
            this.levels = pLevels;
        }

        protected EnchantWithLevelsFunction.Builder getThis() {
            return this;
        }

        public EnchantWithLevelsFunction.Builder allowTreasure() {
            this.treasure = true;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new EnchantWithLevelsFunction(this.getConditions(), this.levels, this.treasure);
        }
    }
}
