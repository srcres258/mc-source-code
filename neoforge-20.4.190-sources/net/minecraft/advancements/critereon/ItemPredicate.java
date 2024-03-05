package net.minecraft.advancements.critereon;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ItemLike;

public record ItemPredicate(
    Optional<TagKey<Item>> tag,
    Optional<HolderSet<Item>> items,
    MinMaxBounds.Ints count,
    MinMaxBounds.Ints durability,
    List<EnchantmentPredicate> enchantments,
    List<EnchantmentPredicate> storedEnchantments,
    Optional<Holder<Potion>> potion,
    Optional<NbtPredicate> nbt,
    Optional<net.neoforged.neoforge.common.advancements.critereon.ICustomItemPredicate> customLogic
) {
    private static final Codec<HolderSet<Item>> ITEMS_CODEC = BuiltInRegistries.ITEM
        .holderByNameCodec()
        .listOf()
        .xmap(HolderSet::direct, p_297903_ -> p_297903_.stream().toList());
    public static final Codec<ItemPredicate> VANILLA_CODEC = RecordCodecBuilder.create(
        p_297904_ -> p_297904_.group(
                    ExtraCodecs.strictOptionalField(TagKey.codec(Registries.ITEM), "tag").forGetter(ItemPredicate::tag),
                    ExtraCodecs.strictOptionalField(ITEMS_CODEC, "items").forGetter(ItemPredicate::items),
                    ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "count", MinMaxBounds.Ints.ANY).forGetter(ItemPredicate::count),
                    ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "durability", MinMaxBounds.Ints.ANY).forGetter(ItemPredicate::durability),
                    ExtraCodecs.strictOptionalField(EnchantmentPredicate.CODEC.listOf(), "enchantments", List.of()).forGetter(ItemPredicate::enchantments),
                    ExtraCodecs.strictOptionalField(EnchantmentPredicate.CODEC.listOf(), "stored_enchantments", List.of())
                        .forGetter(ItemPredicate::storedEnchantments),
                    ExtraCodecs.strictOptionalField(BuiltInRegistries.POTION.holderByNameCodec(), "potion").forGetter(ItemPredicate::potion),
                    ExtraCodecs.strictOptionalField(NbtPredicate.CODEC, "nbt").forGetter(ItemPredicate::nbt)
                )
                .apply(p_297904_, ItemPredicate::new)
    );
    //TODO - 1.20.5: Make this final
    public static Codec<ItemPredicate> CODEC = ExtraCodecs.<net.neoforged.neoforge.common.advancements.critereon.ICustomItemPredicate, ItemPredicate>either(
            net.neoforged.neoforge.registries.NeoForgeRegistries.ITEM_PREDICATE_SERIALIZERS.byNameCodec()
                    .dispatch(
                            net.neoforged.neoforge.common.advancements.critereon.ICustomItemPredicate::codec,
                            java.util.function.Function.identity()),
            VANILLA_CODEC
        ).xmap(either -> either.map(ItemPredicate::new, p -> p), predicate -> {
            // Serialize using dispatch codec if custom logic is present, otherwise use vanilla codec
            if (predicate.customLogic.isPresent()) {
                return com.mojang.datafixers.util.Either.left(predicate.customLogic.get());
            } else {
                return com.mojang.datafixers.util.Either.right(predicate);
            }
        });

    public ItemPredicate(Optional<TagKey<Item>> tag, Optional<HolderSet<Item>> items, MinMaxBounds.Ints count, MinMaxBounds.Ints durability, List<EnchantmentPredicate> enchantments, List<EnchantmentPredicate> storedEnchantments, Optional<Holder<Potion>> potion, Optional<NbtPredicate> nbt) {
        this(tag, items, count, durability, enchantments, storedEnchantments, potion, nbt, Optional.empty());
    }

    public ItemPredicate(net.neoforged.neoforge.common.advancements.critereon.ICustomItemPredicate customLogic) {
        this(Optional.empty(), Optional.empty(), MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, List.of(), List.of(), Optional.empty(), Optional.empty(), Optional.of(customLogic));
    }

    public boolean matches(ItemStack pItem) {
        if (this.customLogic.isPresent()) {
            return this.customLogic.get().test(pItem);
        }
        if (this.tag.isPresent() && !pItem.is(this.tag.get())) {
            return false;
        } else if (this.items.isPresent() && !pItem.is(this.items.get())) {
            return false;
        } else if (!this.count.matches(pItem.getCount())) {
            return false;
        } else if (!this.durability.isAny() && !pItem.isDamageableItem()) {
            return false;
        } else if (!this.durability.matches(pItem.getMaxDamage() - pItem.getDamageValue())) {
            return false;
        } else if (this.nbt.isPresent() && !this.nbt.get().matches(pItem)) {
            return false;
        } else {
            if (!this.enchantments.isEmpty()) {
                Map<Enchantment, Integer> map = pItem.getAllEnchantments();

                for(EnchantmentPredicate enchantmentpredicate : this.enchantments) {
                    if (!enchantmentpredicate.containedIn(map)) {
                        return false;
                    }
                }
            }

            if (!this.storedEnchantments.isEmpty()) {
                Map<Enchantment, Integer> map1 = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(pItem));

                for(EnchantmentPredicate enchantmentpredicate1 : this.storedEnchantments) {
                    if (!enchantmentpredicate1.containedIn(map1)) {
                        return false;
                    }
                }
            }

            return !this.potion.isPresent() || this.potion.get().value() == PotionUtils.getPotion(pItem);
        }
    }

    public static class Builder {
        private final ImmutableList.Builder<EnchantmentPredicate> enchantments = ImmutableList.builder();
        private final ImmutableList.Builder<EnchantmentPredicate> storedEnchantments = ImmutableList.builder();
        private Optional<HolderSet<Item>> items = Optional.empty();
        private Optional<TagKey<Item>> tag = Optional.empty();
        private MinMaxBounds.Ints count = MinMaxBounds.Ints.ANY;
        private MinMaxBounds.Ints durability = MinMaxBounds.Ints.ANY;
        private Optional<Holder<Potion>> potion = Optional.empty();
        private Optional<NbtPredicate> nbt = Optional.empty();

        private Builder() {
        }

        public static ItemPredicate.Builder item() {
            return new ItemPredicate.Builder();
        }

        public ItemPredicate.Builder of(ItemLike... pItems) {
            this.items = Optional.of(HolderSet.direct(p_298756_ -> p_298756_.asItem().builtInRegistryHolder(), pItems));
            return this;
        }

        public ItemPredicate.Builder of(TagKey<Item> pTag) {
            this.tag = Optional.of(pTag);
            return this;
        }

        public ItemPredicate.Builder withCount(MinMaxBounds.Ints pCount) {
            this.count = pCount;
            return this;
        }

        public ItemPredicate.Builder hasDurability(MinMaxBounds.Ints pDurability) {
            this.durability = pDurability;
            return this;
        }

        public ItemPredicate.Builder isPotion(Potion pPotion) {
            this.potion = Optional.of(pPotion.builtInRegistryHolder());
            return this;
        }

        public ItemPredicate.Builder hasNbt(CompoundTag pNbt) {
            this.nbt = Optional.of(new NbtPredicate(pNbt));
            return this;
        }

        public ItemPredicate.Builder hasEnchantment(EnchantmentPredicate pEnchantment) {
            this.enchantments.add(pEnchantment);
            return this;
        }

        public ItemPredicate.Builder hasStoredEnchantment(EnchantmentPredicate pStoredEnchantment) {
            this.storedEnchantments.add(pStoredEnchantment);
            return this;
        }

        public ItemPredicate build() {
            List<EnchantmentPredicate> list = this.enchantments.build();
            List<EnchantmentPredicate> list1 = this.storedEnchantments.build();
            return new ItemPredicate(this.tag, this.items, this.count, this.durability, list, list1, this.potion, this.nbt);
        }
    }
}
