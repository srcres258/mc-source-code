package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that sets the LootTable and optionally the loot table seed on the stack's {@code BlockEntityTag}. The effect of this is that containers such as chests will receive the given LootTable when placed.
 */
public class SetContainerLootTable extends LootItemConditionalFunction {
    public static final Codec<SetContainerLootTable> CODEC = RecordCodecBuilder.create(
        p_298104_ -> commonFields(p_298104_)
                .and(
                    p_298104_.group(
                        ResourceLocation.CODEC.fieldOf("name").forGetter(p_298106_ -> p_298106_.name),
                        ExtraCodecs.strictOptionalField(Codec.LONG, "seed", 0L).forGetter(p_298105_ -> p_298105_.seed),
                        BuiltInRegistries.BLOCK_ENTITY_TYPE.holderByNameCodec().fieldOf("type").forGetter(p_298107_ -> p_298107_.type)
                    )
                )
                .apply(p_298104_, SetContainerLootTable::new)
    );
    private final ResourceLocation name;
    private final long seed;
    private final Holder<BlockEntityType<?>> type;

    private SetContainerLootTable(List<LootItemCondition> p_298290_, ResourceLocation p_193046_, long p_193047_, Holder<BlockEntityType<?>> p_298416_) {
        super(p_298290_);
        this.name = p_193046_;
        this.seed = p_193047_;
        this.type = p_298416_;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_LOOT_TABLE;
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        if (pStack.isEmpty()) {
            return pStack;
        } else {
            CompoundTag compoundtag = BlockItem.getBlockEntityData(pStack);
            if (compoundtag == null) {
                compoundtag = new CompoundTag();
            }

            compoundtag.putString("LootTable", this.name.toString());
            if (this.seed != 0L) {
                compoundtag.putLong("LootTableSeed", this.seed);
            }

            BlockItem.setBlockEntityData(pStack, this.type.value(), compoundtag);
            return pStack;
        }
    }

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    @Override
    public void validate(ValidationContext pContext) {
        super.validate(pContext);
        LootDataId<LootTable> lootdataid = new LootDataId<>(LootDataType.TABLE, this.name);
        if (pContext.resolver().getElementOptional(lootdataid).isEmpty()) {
            pContext.reportProblem("Missing loot table used for container: " + this.name);
        }
    }

    public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> pType, ResourceLocation pName) {
        return simpleBuilder(p_298114_ -> new SetContainerLootTable(p_298114_, pName, 0L, pType.builtInRegistryHolder()));
    }

    public static LootItemConditionalFunction.Builder<?> withLootTable(BlockEntityType<?> pType, ResourceLocation pName, long pSeed) {
        return simpleBuilder(p_298111_ -> new SetContainerLootTable(p_298111_, pName, pSeed, pType.builtInRegistryHolder()));
    }
}
