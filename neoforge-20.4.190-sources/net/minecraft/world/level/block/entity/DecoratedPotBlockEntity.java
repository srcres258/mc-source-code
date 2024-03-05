package net.minecraft.world.level.block.entity;

import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.ticks.ContainerSingleItem;

public class DecoratedPotBlockEntity extends BlockEntity implements RandomizableContainer, ContainerSingleItem {
    public static final String TAG_SHERDS = "sherds";
    public static final String TAG_ITEM = "item";
    public static final int EVENT_POT_WOBBLES = 1;
    public long wobbleStartedAtTick;
    @Nullable
    public DecoratedPotBlockEntity.WobbleStyle lastWobbleStyle;
    private DecoratedPotBlockEntity.Decorations decorations;
    private ItemStack item = ItemStack.EMPTY;
    @Nullable
    protected ResourceLocation lootTable;
    protected long lootTableSeed;

    public DecoratedPotBlockEntity(BlockPos pPos, BlockState pState) {
        super(BlockEntityType.DECORATED_POT, pPos, pState);
        this.decorations = DecoratedPotBlockEntity.Decorations.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        this.decorations.save(pTag);
        if (!this.trySaveLootTable(pTag) && !this.item.isEmpty()) {
            pTag.put("item", this.item.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        this.decorations = DecoratedPotBlockEntity.Decorations.load(pTag);
        if (!this.tryLoadLootTable(pTag)) {
            if (pTag.contains("item", 10)) {
                this.item = ItemStack.of(pTag.getCompound("item"));
            } else {
                this.item = ItemStack.EMPTY;
            }
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    public Direction getDirection() {
        return this.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    public DecoratedPotBlockEntity.Decorations getDecorations() {
        return this.decorations;
    }

    public void setFromItem(ItemStack pItem) {
        this.decorations = DecoratedPotBlockEntity.Decorations.load(BlockItem.getBlockEntityData(pItem));
    }

    public ItemStack getPotAsItem() {
        return createDecoratedPotItem(this.decorations);
    }

    public static ItemStack createDecoratedPotItem(DecoratedPotBlockEntity.Decorations pDecorations) {
        ItemStack itemstack = Items.DECORATED_POT.getDefaultInstance();
        CompoundTag compoundtag = pDecorations.save(new CompoundTag());
        BlockItem.setBlockEntityData(itemstack, BlockEntityType.DECORATED_POT, compoundtag);
        return itemstack;
    }

    @Nullable
    @Override
    public ResourceLocation getLootTable() {
        return this.lootTable;
    }

    @Override
    public void setLootTable(@Nullable ResourceLocation pLootTable) {
        this.lootTable = pLootTable;
    }

    @Override
    public long getLootTableSeed() {
        return this.lootTableSeed;
    }

    @Override
    public void setLootTableSeed(long pSeed) {
        this.lootTableSeed = pSeed;
    }

    @Override
    public ItemStack getTheItem() {
        this.unpackLootTable(null);
        return this.item;
    }

    @Override
    public ItemStack splitTheItem(int pAmount) {
        this.unpackLootTable(null);
        ItemStack itemstack = this.item.split(pAmount);
        if (this.item.isEmpty()) {
            this.item = ItemStack.EMPTY;
        }

        return itemstack;
    }

    @Override
    public void setTheItem(ItemStack pItem) {
        this.unpackLootTable(null);
        this.item = pItem;
    }

    @Override
    public BlockEntity getContainerBlockEntity() {
        return this;
    }

    public void wobble(DecoratedPotBlockEntity.WobbleStyle pStyle) {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.blockEvent(this.getBlockPos(), this.getBlockState().getBlock(), 1, pStyle.ordinal());
        }
    }

    @Override
    public boolean triggerEvent(int pId, int pType) {
        if (this.level != null && pId == 1 && pType >= 0 && pType < DecoratedPotBlockEntity.WobbleStyle.values().length) {
            this.wobbleStartedAtTick = this.level.getGameTime();
            this.lastWobbleStyle = DecoratedPotBlockEntity.WobbleStyle.values()[pType];
            return true;
        } else {
            return super.triggerEvent(pId, pType);
        }
    }

    public static record Decorations(Item back, Item left, Item right, Item front) {
        public static final DecoratedPotBlockEntity.Decorations EMPTY = new DecoratedPotBlockEntity.Decorations(
            Items.BRICK, Items.BRICK, Items.BRICK, Items.BRICK
        );

        public CompoundTag save(CompoundTag pTag) {
            if (this.equals(EMPTY)) {
                return pTag;
            } else {
                ListTag listtag = new ListTag();
                this.sorted().forEach(p_285298_ -> listtag.add(StringTag.valueOf(BuiltInRegistries.ITEM.getKey(p_285298_).toString())));
                pTag.put("sherds", listtag);
                return pTag;
            }
        }

        public Stream<Item> sorted() {
            return Stream.of(this.back, this.left, this.right, this.front);
        }

        public static DecoratedPotBlockEntity.Decorations load(@Nullable CompoundTag pTag) {
            if (pTag != null && pTag.contains("sherds", 9)) {
                ListTag listtag = pTag.getList("sherds", 8);
                return new DecoratedPotBlockEntity.Decorations(
                    itemFromTag(listtag, 0), itemFromTag(listtag, 1), itemFromTag(listtag, 2), itemFromTag(listtag, 3)
                );
            } else {
                return EMPTY;
            }
        }

        private static Item itemFromTag(ListTag pTag, int pIndex) {
            if (pIndex >= pTag.size()) {
                return Items.BRICK;
            } else {
                Tag tag = pTag.get(pIndex);
                return BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(tag.getAsString()));
            }
        }
    }

    public static enum WobbleStyle {
        POSITIVE(7),
        NEGATIVE(10);

        public final int duration;

        private WobbleStyle(int pDuration) {
            this.duration = pDuration;
        }
    }
}
