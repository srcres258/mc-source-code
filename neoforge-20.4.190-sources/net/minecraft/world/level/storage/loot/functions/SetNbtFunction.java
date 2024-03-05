package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that merges a given CompoundTag into the stack's NBT tag.
 */
public class SetNbtFunction extends LootItemConditionalFunction {
    public static final Codec<SetNbtFunction> CODEC = RecordCodecBuilder.create(
        p_298156_ -> commonFields(p_298156_).and(TagParser.AS_CODEC.fieldOf("tag").forGetter(p_298157_ -> p_298157_.tag)).apply(p_298156_, SetNbtFunction::new)
    );
    private final CompoundTag tag;

    private SetNbtFunction(List<LootItemCondition> p_298385_, CompoundTag p_81177_) {
        super(p_298385_);
        this.tag = p_81177_;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_NBT;
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        pStack.getOrCreateTag().merge(this.tag);
        return pStack;
    }

    @Deprecated
    public static LootItemConditionalFunction.Builder<?> setTag(CompoundTag pTag) {
        return simpleBuilder(p_298155_ -> new SetNbtFunction(p_298155_, pTag));
    }
}
