package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that modifies the stack's count based on an enchantment level on the {@linkplain LootContextParams#TOOL tool} using various formulas.
 */
public class ApplyBonusCount extends LootItemConditionalFunction {
    private static final Map<ResourceLocation, ApplyBonusCount.FormulaType> FORMULAS = Stream.of(
            ApplyBonusCount.BinomialWithBonusCount.TYPE, ApplyBonusCount.OreDrops.TYPE, ApplyBonusCount.UniformBonusCount.TYPE
        )
        .collect(Collectors.toMap(ApplyBonusCount.FormulaType::id, Function.identity()));
    private static final Codec<ApplyBonusCount.FormulaType> FORMULA_TYPE_CODEC = ResourceLocation.CODEC
        .comapFlatMap(
            p_298052_ -> {
                ApplyBonusCount.FormulaType applybonuscount$formulatype = FORMULAS.get(p_298052_);
                return applybonuscount$formulatype != null
                    ? DataResult.success(applybonuscount$formulatype)
                    : DataResult.error(() -> "No formula type with id: '" + p_298052_ + "'");
            },
            ApplyBonusCount.FormulaType::id
        );
    private static final MapCodec<ApplyBonusCount.Formula> FORMULA_CODEC = ExtraCodecs.dispatchOptionalValue(
        "formula", "parameters", FORMULA_TYPE_CODEC, ApplyBonusCount.Formula::getType, ApplyBonusCount.FormulaType::codec
    );
    public static final Codec<ApplyBonusCount> CODEC = RecordCodecBuilder.create(
        p_298054_ -> commonFields(p_298054_)
                .and(
                    p_298054_.group(
                        BuiltInRegistries.ENCHANTMENT.holderByNameCodec().fieldOf("enchantment").forGetter(p_298051_ -> p_298051_.enchantment),
                        FORMULA_CODEC.forGetter(p_298061_ -> p_298061_.formula)
                    )
                )
                .apply(p_298054_, ApplyBonusCount::new)
    );
    private final Holder<Enchantment> enchantment;
    private final ApplyBonusCount.Formula formula;

    private ApplyBonusCount(List<LootItemCondition> p_298532_, Holder<Enchantment> p_298797_, ApplyBonusCount.Formula p_79905_) {
        super(p_298532_);
        this.enchantment = p_298797_;
        this.formula = p_79905_;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.APPLY_BONUS;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return ImmutableSet.of(LootContextParams.TOOL);
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        ItemStack itemstack = pContext.getParamOrNull(LootContextParams.TOOL);
        if (itemstack != null) {
            int i = EnchantmentHelper.getItemEnchantmentLevel(this.enchantment.value(), itemstack);
            int j = this.formula.calculateNewCount(pContext.getRandom(), pStack.getCount(), i);
            pStack.setCount(j);
        }

        return pStack;
    }

    public static LootItemConditionalFunction.Builder<?> addBonusBinomialDistributionCount(Enchantment pEnchantment, float pProbability, int pExtraRounds) {
        return simpleBuilder(
            p_298058_ -> new ApplyBonusCount(p_298058_, pEnchantment.builtInRegistryHolder(), new ApplyBonusCount.BinomialWithBonusCount(pExtraRounds, pProbability))
        );
    }

    public static LootItemConditionalFunction.Builder<?> addOreBonusCount(Enchantment pEnchantment) {
        return simpleBuilder(p_298047_ -> new ApplyBonusCount(p_298047_, pEnchantment.builtInRegistryHolder(), new ApplyBonusCount.OreDrops()));
    }

    public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Enchantment pEnchantment) {
        return simpleBuilder(p_298060_ -> new ApplyBonusCount(p_298060_, pEnchantment.builtInRegistryHolder(), new ApplyBonusCount.UniformBonusCount(1)));
    }

    public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Enchantment pEnchantment, int pBonusMultiplier) {
        return simpleBuilder(p_298050_ -> new ApplyBonusCount(p_298050_, pEnchantment.builtInRegistryHolder(), new ApplyBonusCount.UniformBonusCount(pBonusMultiplier)));
    }

    /**
     * Applies a bonus based on a binomial distribution with {@code n = enchantmentLevel + extraRounds} and {@code p = probability}.
     */
    static record BinomialWithBonusCount(int extraRounds, float probability) implements ApplyBonusCount.Formula {
        private static final Codec<ApplyBonusCount.BinomialWithBonusCount> CODEC = RecordCodecBuilder.create(
            p_298226_ -> p_298226_.group(
                        Codec.INT.fieldOf("extra").forGetter(ApplyBonusCount.BinomialWithBonusCount::extraRounds),
                        Codec.FLOAT.fieldOf("probability").forGetter(ApplyBonusCount.BinomialWithBonusCount::probability)
                    )
                    .apply(p_298226_, ApplyBonusCount.BinomialWithBonusCount::new)
        );
        public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(new ResourceLocation("binomial_with_bonus_count"), CODEC);

        @Override
        public int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel) {
            for(int i = 0; i < pEnchantmentLevel + this.extraRounds; ++i) {
                if (pRandom.nextFloat() < this.probability) {
                    ++pOriginalCount;
                }
            }

            return pOriginalCount;
        }

        @Override
        public ApplyBonusCount.FormulaType getType() {
            return TYPE;
        }
    }

    interface Formula {
        int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel);

        ApplyBonusCount.FormulaType getType();
    }

    static record FormulaType(ResourceLocation id, Codec<? extends ApplyBonusCount.Formula> codec) {
    }

    /**
     * Applies a bonus count with a special formula used for fortune ore drops.
     */
    static record OreDrops() implements ApplyBonusCount.Formula {
        public static final Codec<ApplyBonusCount.OreDrops> CODEC = Codec.unit(ApplyBonusCount.OreDrops::new);
        public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(new ResourceLocation("ore_drops"), CODEC);

        @Override
        public int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel) {
            if (pEnchantmentLevel > 0) {
                int i = pRandom.nextInt(pEnchantmentLevel + 2) - 1;
                if (i < 0) {
                    i = 0;
                }

                return pOriginalCount * (i + 1);
            } else {
                return pOriginalCount;
            }
        }

        @Override
        public ApplyBonusCount.FormulaType getType() {
            return TYPE;
        }
    }

    /**
     * Adds a bonus count based on the enchantment level scaled by a constant multiplier.
     */
    static record UniformBonusCount(int bonusMultiplier) implements ApplyBonusCount.Formula {
        public static final Codec<ApplyBonusCount.UniformBonusCount> CODEC = RecordCodecBuilder.create(
            p_298501_ -> p_298501_.group(Codec.INT.fieldOf("bonusMultiplier").forGetter(ApplyBonusCount.UniformBonusCount::bonusMultiplier))
                    .apply(p_298501_, ApplyBonusCount.UniformBonusCount::new)
        );
        public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(new ResourceLocation("uniform_bonus_count"), CODEC);

        @Override
        public int calculateNewCount(RandomSource pRandom, int pOriginalCount, int pEnchantmentLevel) {
            return pOriginalCount + pRandom.nextInt(this.bonusMultiplier * pEnchantmentLevel + 1);
        }

        @Override
        public ApplyBonusCount.FormulaType getType() {
            return TYPE;
        }
    }
}
