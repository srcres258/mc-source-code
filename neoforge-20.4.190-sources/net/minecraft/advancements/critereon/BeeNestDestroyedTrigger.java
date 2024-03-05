package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BeeNestDestroyedTrigger extends SimpleCriterionTrigger<BeeNestDestroyedTrigger.TriggerInstance> {
    @Override
    public Codec<BeeNestDestroyedTrigger.TriggerInstance> codec() {
        return BeeNestDestroyedTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pPlayer, BlockState pState, ItemStack pStack, int pNumBees) {
        this.trigger(pPlayer, p_146660_ -> p_146660_.matches(pState, pStack, pNumBees));
    }

    public static record TriggerInstance(
        Optional<ContextAwarePredicate> player, Optional<Holder<Block>> block, Optional<ItemPredicate> item, MinMaxBounds.Ints beesInside
    ) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<BeeNestDestroyedTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_311401_ -> p_311401_.group(
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(BeeNestDestroyedTrigger.TriggerInstance::player),
                        ExtraCodecs.strictOptionalField(BuiltInRegistries.BLOCK.holderByNameCodec(), "block")
                            .forGetter(BeeNestDestroyedTrigger.TriggerInstance::block),
                        ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "item").forGetter(BeeNestDestroyedTrigger.TriggerInstance::item),
                        ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "num_bees_inside", MinMaxBounds.Ints.ANY)
                            .forGetter(BeeNestDestroyedTrigger.TriggerInstance::beesInside)
                    )
                    .apply(p_311401_, BeeNestDestroyedTrigger.TriggerInstance::new)
        );

        public static Criterion<BeeNestDestroyedTrigger.TriggerInstance> destroyedBeeNest(
            Block pBlock, ItemPredicate.Builder pItem, MinMaxBounds.Ints pNumBees
        ) {
            return CriteriaTriggers.BEE_NEST_DESTROYED
                .createCriterion(
                    new BeeNestDestroyedTrigger.TriggerInstance(
                        Optional.empty(), Optional.of(pBlock.builtInRegistryHolder()), Optional.of(pItem.build()), pNumBees
                    )
                );
        }

        public boolean matches(BlockState pState, ItemStack pStack, int pNumBees) {
            if (this.block.isPresent() && !pState.is(this.block.get())) {
                return false;
            } else {
                return this.item.isPresent() && !this.item.get().matches(pStack) ? false : this.beesInside.matches(pNumBees);
            }
        }
    }
}
