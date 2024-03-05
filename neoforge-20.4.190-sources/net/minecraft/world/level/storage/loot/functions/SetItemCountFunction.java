package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

/**
 * LootItemFunction that sets the stack's count based on a {@link NumberProvider}, optionally adding to any existing count.
 */
public class SetItemCountFunction extends LootItemConditionalFunction {
    public static final Codec<SetItemCountFunction> CODEC = RecordCodecBuilder.create(
        p_298131_ -> commonFields(p_298131_)
                .and(
                    p_298131_.group(
                        NumberProviders.CODEC.fieldOf("count").forGetter(p_298132_ -> p_298132_.value),
                        Codec.BOOL.fieldOf("add").orElse(false).forGetter(p_298133_ -> p_298133_.add)
                    )
                )
                .apply(p_298131_, SetItemCountFunction::new)
    );
    private final NumberProvider value;
    private final boolean add;

    private SetItemCountFunction(List<LootItemCondition> p_299158_, NumberProvider p_165410_, boolean p_165411_) {
        super(p_299158_);
        this.value = p_165410_;
        this.add = p_165411_;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_COUNT;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.value.getReferencedContextParams();
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        int i = this.add ? pStack.getCount() : 0;
        pStack.setCount(Mth.clamp(i + this.value.getInt(pContext), 0, pStack.getMaxStackSize()));
        return pStack;
    }

    public static LootItemConditionalFunction.Builder<?> setCount(NumberProvider pCountValue) {
        return simpleBuilder(p_298130_ -> new SetItemCountFunction(p_298130_, pCountValue, false));
    }

    public static LootItemConditionalFunction.Builder<?> setCount(NumberProvider pCountValue, boolean pAdd) {
        return simpleBuilder(p_298128_ -> new SetItemCountFunction(p_298128_, pCountValue, pAdd));
    }
}
