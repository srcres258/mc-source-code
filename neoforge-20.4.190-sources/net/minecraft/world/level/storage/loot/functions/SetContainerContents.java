package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntries;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntry;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that sets the contents of a container such as a chest by setting the {@code BlocKEntityTag} of the stacks.
 * The contents are based on a list of loot pools.
 */
public class SetContainerContents extends LootItemConditionalFunction {
    public static final Codec<SetContainerContents> CODEC = RecordCodecBuilder.create(
        p_298101_ -> commonFields(p_298101_)
                .and(
                    p_298101_.group(
                        BuiltInRegistries.BLOCK_ENTITY_TYPE.holderByNameCodec().fieldOf("type").forGetter(p_298102_ -> p_298102_.type),
                        LootPoolEntries.CODEC.listOf().fieldOf("entries").forGetter(p_298103_ -> p_298103_.entries)
                    )
                )
                .apply(p_298101_, SetContainerContents::new)
    );
    private final Holder<BlockEntityType<?>> type;
    private final List<LootPoolEntryContainer> entries;

    SetContainerContents(List<LootItemCondition> p_193035_, Holder<BlockEntityType<?>> p_298738_, List<LootPoolEntryContainer> p_298300_) {
        super(p_193035_);
        this.type = p_298738_;
        this.entries = List.copyOf(p_298300_);
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_CONTENTS;
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        if (pStack.isEmpty()) {
            return pStack;
        } else {
            NonNullList<ItemStack> nonnulllist = NonNullList.create();
            this.entries
                .forEach(
                    p_80916_ -> p_80916_.expand(
                            pContext, p_287573_ -> p_287573_.createItemStack(LootTable.createStackSplitter(pContext.getLevel(), nonnulllist::add), pContext)
                        )
                );
            CompoundTag compoundtag = new CompoundTag();
            ContainerHelper.saveAllItems(compoundtag, nonnulllist);
            CompoundTag compoundtag1 = BlockItem.getBlockEntityData(pStack);
            if (compoundtag1 == null) {
                compoundtag1 = compoundtag;
            } else {
                compoundtag1.merge(compoundtag);
            }

            BlockItem.setBlockEntityData(pStack, this.type.value(), compoundtag1);
            return pStack;
        }
    }

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    @Override
    public void validate(ValidationContext pContext) {
        super.validate(pContext);

        for(int i = 0; i < this.entries.size(); ++i) {
            this.entries.get(i).validate(pContext.forChild(".entry[" + i + "]"));
        }
    }

    public static SetContainerContents.Builder setContents(BlockEntityType<?> pType) {
        return new SetContainerContents.Builder(pType);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<SetContainerContents.Builder> {
        private final ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();
        private final BlockEntityType<?> type;

        public Builder(BlockEntityType<?> pType) {
            this.type = pType;
        }

        protected SetContainerContents.Builder getThis() {
            return this;
        }

        public SetContainerContents.Builder withEntry(LootPoolEntryContainer.Builder<?> pLootEntryBuilder) {
            this.entries.add(pLootEntryBuilder.build());
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetContainerContents(this.getConditions(), this.type.builtInRegistryHolder(), this.entries.build());
        }
    }
}
