package net.minecraft.world.item.crafting;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class Ingredient implements Predicate<ItemStack> {
    public static final Ingredient EMPTY = new Ingredient(Stream.empty());
    public final Ingredient.Value[] values;
    @Nullable
    private ItemStack[] itemStacks;
    @Nullable
    private IntList stackingIds;
    private final java.util.function.Supplier<? extends net.neoforged.neoforge.common.crafting.IngredientType<?>> type;

    public static final Codec<Ingredient> VANILLA_CODEC = codec(true);
    public static final Codec<Ingredient> VANILLA_CODEC_NONEMPTY = codec(false);
    public static final Codec<Ingredient> CODEC = net.neoforged.neoforge.common.crafting.CraftingHelper.makeIngredientCodec(true, VANILLA_CODEC);
    public static final Codec<Ingredient> CODEC_NONEMPTY = net.neoforged.neoforge.common.crafting.CraftingHelper.makeIngredientCodec(false, VANILLA_CODEC_NONEMPTY);
    public static final Codec<List<Ingredient>> LIST_CODEC = CODEC.listOf();
    public static final Codec<List<Ingredient>> LIST_CODEC_NONEMPTY = CODEC_NONEMPTY.listOf();

    protected Ingredient(Stream<? extends Ingredient.Value> pValues) {
        this(pValues, net.neoforged.neoforge.common.NeoForgeMod.VANILLA_INGREDIENT_TYPE);
    }

    private Ingredient(Ingredient.Value[] p_301044_) {
        this(p_301044_, net.neoforged.neoforge.common.NeoForgeMod.VANILLA_INGREDIENT_TYPE);
    }

    protected Ingredient(Stream<? extends Ingredient.Value> p_43907_, java.util.function.Supplier<? extends net.neoforged.neoforge.common.crafting.IngredientType<?>> type) {
        this.values = p_43907_.toArray(Value[]::new);
        this.type = type;
    }

    private Ingredient(Ingredient.Value[] p_301044_, java.util.function.Supplier<? extends net.neoforged.neoforge.common.crafting.IngredientType<?>> type) {
        this.values = p_301044_;
        this.type = type;
    }

    public net.neoforged.neoforge.common.crafting.IngredientType<?> getType() {
        return type.get();
    }

    public ItemStack[] getItems() {
        if (this.itemStacks == null) {
            this.itemStacks = Arrays.stream(this.values)
                .flatMap(p_43916_ -> p_43916_.getItems().stream())
                .distinct()
                .toArray(p_43910_ -> new ItemStack[p_43910_]);
        }

        return this.itemStacks;
    }

    public boolean test(@Nullable ItemStack pStack) {
        if (pStack == null) {
            return false;
        } else if (this.isEmpty()) {
            return pStack.isEmpty();
        } else {
            for(ItemStack itemstack : this.getItems()) {
                if (areStacksEqual(itemstack, pStack)) {
                    return true;
                }
            }

            return false;
        }
    }

    protected boolean areStacksEqual(ItemStack left, ItemStack right) {
        return left.is(right.getItem());
    }

    public IntList getStackingIds() {
        if (this.stackingIds == null) {
            ItemStack[] aitemstack = this.getItems();
            this.stackingIds = new IntArrayList(aitemstack.length);

            for(ItemStack itemstack : aitemstack) {
                this.stackingIds.add(StackedContents.getStackingIndex(itemstack));
            }

            this.stackingIds.sort(IntComparators.NATURAL_COMPARATOR);
        }

        return this.stackingIds;
    }

    public final void toNetwork(FriendlyByteBuf pBuffer) {
        if (synchronizeWithContents()) {
            pBuffer.writeCollection(Arrays.asList(this.getItems()), FriendlyByteBuf::writeItem);
        }
        else {
            pBuffer.writeVarInt(-1);
            pBuffer.writeWithCodec(net.minecraft.nbt.NbtOps.INSTANCE, CODEC, this);
        }
    }

    /**
     * {@return if {@code true}, this ingredient will be synchronized using its contents, as in vanilla, otherwise it will be synchronized via the {@link #codec(boolean) codec}}
     */
    public boolean synchronizeWithContents() {
        return true;
    }

    public boolean isEmpty() {
        return this.values.length == 0 || Arrays.stream(getItems()).allMatch(ItemStack::isEmpty);
    }

    @Override
    public boolean equals(Object pOther) {
        return pOther instanceof Ingredient ingredient ? Arrays.equals((Object[])this.values, (Object[])ingredient.values) : false;
    }

    public boolean isSimple() {
        return true;
    }

    public static Ingredient fromValues(Stream<? extends Ingredient.Value> pStream) {
        Ingredient ingredient = new Ingredient(pStream);
        return ingredient.isEmpty() ? EMPTY : ingredient;
    }

    public static Ingredient of() {
        return EMPTY;
    }

    public static Ingredient of(ItemLike... pItems) {
        return of(Arrays.stream(pItems).map(ItemStack::new));
    }

    public static Ingredient of(ItemStack... pStacks) {
        return of(Arrays.stream(pStacks));
    }

    public static Ingredient of(Stream<ItemStack> pStacks) {
        return fromValues(pStacks.filter(p_43944_ -> !p_43944_.isEmpty()).map(Ingredient.ItemValue::new));
    }

    public static Ingredient of(TagKey<Item> pTag) {
        return fromValues(Stream.of(new Ingredient.TagValue(pTag)));
    }

    public static Ingredient fromJson(com.google.gson.JsonElement element, boolean nonEmpty) {
        Codec<Ingredient> codec = nonEmpty ? CODEC : CODEC_NONEMPTY;
        return net.minecraft.Util.getOrThrow(codec.parse(com.mojang.serialization.JsonOps.INSTANCE, element), IllegalStateException::new);
    }

    public static Ingredient fromNetwork(FriendlyByteBuf pBuffer) {
        var size = pBuffer.readVarInt();
        if (size == -1) {
            return pBuffer.readWithCodecTrusted(net.minecraft.nbt.NbtOps.INSTANCE, CODEC);
        }
        return new Ingredient(Stream.generate(() -> new Ingredient.ItemValue(pBuffer.readItem())).limit(size));
    }

    private static Codec<Ingredient> codec(boolean pAllowEmpty) {
        //PATCH: 1.20.2: Wrap in dispatch codec with fallback.
        Codec<Ingredient.Value[]> codec = Codec.list(Ingredient.Value.CODEC)
            .comapFlatMap(
                p_300810_ -> !pAllowEmpty && p_300810_.size() < 1
                        ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined")
                        : DataResult.success(p_300810_.toArray(new Ingredient.Value[0])),
                List::of
            );
        return ExtraCodecs.either(codec, Ingredient.Value.CODEC)
            .flatComapMap(
                p_300805_ -> p_300805_.map(Ingredient::new, p_300806_ -> new Ingredient(new Ingredient.Value[]{p_300806_})),
                p_300808_ -> {
                    if (p_300808_.values.length == 1) {
                        return DataResult.success(Either.right(p_300808_.values[0]));
                    } else {
                        return p_300808_.values.length == 0 && !pAllowEmpty
                            ? DataResult.error(() -> "Item array cannot be empty, at least one item must be defined")
                            : DataResult.success(Either.left(p_300808_.values));
                    }
                }
            );
    }

    public static record ItemValue(ItemStack item, java.util.function.BiFunction<net.minecraft.world.item.ItemStack, net.minecraft.world.item.ItemStack, Boolean> comparator) implements Ingredient.Value {
        public ItemValue(ItemStack item) {
            this(item, ItemValue::areStacksEqual);
        }

        static final Codec<Ingredient.ItemValue> CODEC = RecordCodecBuilder.create(
            p_311727_ -> p_311727_.group(ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(p_300919_ -> p_300919_.item))
                    .apply(p_311727_, Ingredient.ItemValue::new)
        );

        @Override
        public boolean equals(Object pOther) {
            if (!(pOther instanceof Ingredient.ItemValue)) {
                return false;
            } else {
                Ingredient.ItemValue ingredient$itemvalue = (Ingredient.ItemValue)pOther;
                return comparator().apply(item(), ingredient$itemvalue.item());
            }
        }

        @Override
        public Collection<ItemStack> getItems() {
            return Collections.singleton(this.item);
        }

        private static boolean areStacksEqual(ItemStack left, ItemStack right) {
            return left.getItem().equals(right.getItem())
                    && left.getCount() == right.getCount();
        }
    }

    public static record TagValue(TagKey<Item> tag) implements Ingredient.Value {
        static final Codec<Ingredient.TagValue> CODEC = RecordCodecBuilder.create(
            p_301118_ -> p_301118_.group(TagKey.codec(Registries.ITEM).fieldOf("tag").forGetter(p_301154_ -> p_301154_.tag))
                    .apply(p_301118_, Ingredient.TagValue::new)
        );

        @Override
        public boolean equals(Object pOther) {
            return pOther instanceof Ingredient.TagValue ingredient$tagvalue ? ingredient$tagvalue.tag.location().equals(this.tag.location()) : false;
        }

        @Override
        public Collection<ItemStack> getItems() {
            List<ItemStack> list = Lists.newArrayList();

            for(Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(this.tag)) {
                list.add(new ItemStack(holder));
            }

            if (list.size() == 0) {
                list.add(new ItemStack(net.minecraft.world.level.block.Blocks.BARRIER).setHoverName(net.minecraft.network.chat.Component.literal("Empty Tag: " + this.tag.location())));
            }
            return list;
        }
    }

    public interface Value {
        Codec<Ingredient.Value> CODEC = ExtraCodecs.xor(Ingredient.ItemValue.CODEC, Ingredient.TagValue.CODEC)
            .xmap(p_300956_ -> p_300956_.map(p_300932_ -> p_300932_, p_301313_ -> p_301313_), p_301304_ -> {
                if (p_301304_ instanceof Ingredient.TagValue ingredient$tagvalue) {
                    return Either.right(ingredient$tagvalue);
                } else if (p_301304_ instanceof Ingredient.ItemValue ingredient$itemvalue) {
                    return Either.left(ingredient$itemvalue);
                } else {
                    throw new UnsupportedOperationException("This is neither an item value nor a tag value.");
                }
            });

        Collection<ItemStack> getItems();
    }

    public final Value[] getValues() {
        return values;
    }
}
