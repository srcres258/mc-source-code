package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class InventoryChangeTrigger extends SimpleCriterionTrigger<InventoryChangeTrigger.TriggerInstance> {
    @Override
    public Codec<InventoryChangeTrigger.TriggerInstance> codec() {
        return InventoryChangeTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, Inventory pInventory, ItemStack pStack) {
        int i = 0;
        int j = 0;
        int k = 0;

        for(int l = 0; l < pInventory.getContainerSize(); ++l) {
            ItemStack itemstack = pInventory.getItem(l);
            if (itemstack.isEmpty()) {
                ++j;
            } else {
                ++k;
                if (itemstack.getCount() >= itemstack.getMaxStackSize()) {
                    ++i;
                }
            }
        }

        this.trigger(pPlayer, pInventory, pStack, i, j, k);
    }

    private void trigger(ServerPlayer pPlayer, Inventory pInventory, ItemStack pStack, int pFull, int pEmpty, int pOccupied) {
        this.trigger(pPlayer, p_43166_ -> p_43166_.matches(pInventory, pStack, pFull, pEmpty, pOccupied));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, InventoryChangeTrigger.TriggerInstance.Slots slots, List<ItemPredicate> items)
        implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<InventoryChangeTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_311424_ -> p_311424_.group(
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(InventoryChangeTrigger.TriggerInstance::player),
                        ExtraCodecs.strictOptionalField(
                                InventoryChangeTrigger.TriggerInstance.Slots.CODEC, "slots", InventoryChangeTrigger.TriggerInstance.Slots.ANY
                            )
                            .forGetter(InventoryChangeTrigger.TriggerInstance::slots),
                        ExtraCodecs.strictOptionalField(ItemPredicate.CODEC.listOf(), "items", List.of())
                            .forGetter(InventoryChangeTrigger.TriggerInstance::items)
                    )
                    .apply(p_311424_, InventoryChangeTrigger.TriggerInstance::new)
        );

        public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemPredicate.Builder... pItems) {
            return hasItems(Stream.of(pItems).map(ItemPredicate.Builder::build).<ItemPredicate>toArray(p_297901_ -> new ItemPredicate[p_297901_]));
        }

        public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemPredicate... pItems) {
            return CriteriaTriggers.INVENTORY_CHANGED
                .createCriterion(
                    new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(pItems))
                );
        }

        public static Criterion<InventoryChangeTrigger.TriggerInstance> hasItems(ItemLike... pItems) {
            ItemPredicate[] aitempredicate = new ItemPredicate[pItems.length];

            for(int i = 0; i < pItems.length; ++i) {
                aitempredicate[i] = new ItemPredicate(
                    Optional.empty(),
                    Optional.of(HolderSet.direct(pItems[i].asItem().builtInRegistryHolder())),
                    MinMaxBounds.Ints.ANY,
                    MinMaxBounds.Ints.ANY,
                    List.of(),
                    List.of(),
                    Optional.empty(),
                    Optional.empty()
                );
            }

            return hasItems(aitempredicate);
        }

        public boolean matches(Inventory pInventory, ItemStack pStack, int pFull, int pEmpty, int pOccupied) {
            if (!this.slots.matches(pFull, pEmpty, pOccupied)) {
                return false;
            } else if (this.items.isEmpty()) {
                return true;
            } else if (this.items.size() != 1) {
                List<ItemPredicate> list = new ObjectArrayList<>(this.items);
                int i = pInventory.getContainerSize();

                for(int j = 0; j < i; ++j) {
                    if (list.isEmpty()) {
                        return true;
                    }

                    ItemStack itemstack = pInventory.getItem(j);
                    if (!itemstack.isEmpty()) {
                        list.removeIf(p_43194_ -> p_43194_.matches(itemstack));
                    }
                }

                return list.isEmpty();
            } else {
                return !pStack.isEmpty() && this.items.get(0).matches(pStack);
            }
        }

        public static record Slots(MinMaxBounds.Ints occupied, MinMaxBounds.Ints full, MinMaxBounds.Ints empty) {
            public static final Codec<InventoryChangeTrigger.TriggerInstance.Slots> CODEC = RecordCodecBuilder.create(
                p_312770_ -> p_312770_.group(
                            ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "occupied", MinMaxBounds.Ints.ANY)
                                .forGetter(InventoryChangeTrigger.TriggerInstance.Slots::occupied),
                            ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "full", MinMaxBounds.Ints.ANY)
                                .forGetter(InventoryChangeTrigger.TriggerInstance.Slots::full),
                            ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "empty", MinMaxBounds.Ints.ANY)
                                .forGetter(InventoryChangeTrigger.TriggerInstance.Slots::empty)
                        )
                        .apply(p_312770_, InventoryChangeTrigger.TriggerInstance.Slots::new)
            );
            public static final InventoryChangeTrigger.TriggerInstance.Slots ANY = new InventoryChangeTrigger.TriggerInstance.Slots(
                MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY, MinMaxBounds.Ints.ANY
            );

            public boolean matches(int pFull, int pEmpty, int pOccupied) {
                if (!this.full.matches(pFull)) {
                    return false;
                } else if (!this.empty.matches(pEmpty)) {
                    return false;
                } else {
                    return this.occupied.matches(pOccupied);
                }
            }
        }
    }
}
