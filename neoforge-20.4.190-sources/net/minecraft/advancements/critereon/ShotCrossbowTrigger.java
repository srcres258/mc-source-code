package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class ShotCrossbowTrigger extends SimpleCriterionTrigger<ShotCrossbowTrigger.TriggerInstance> {
    @Override
    public Codec<ShotCrossbowTrigger.TriggerInstance> codec() {
        return ShotCrossbowTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer pShooter, ItemStack pStack) {
        this.trigger(pShooter, p_65467_ -> p_65467_.matches(pStack));
    }

    public static record TriggerInstance(Optional<ContextAwarePredicate> player, Optional<ItemPredicate> item) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<ShotCrossbowTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_311435_ -> p_311435_.group(
                        ExtraCodecs.strictOptionalField(EntityPredicate.ADVANCEMENT_CODEC, "player").forGetter(ShotCrossbowTrigger.TriggerInstance::player),
                        ExtraCodecs.strictOptionalField(ItemPredicate.CODEC, "item").forGetter(ShotCrossbowTrigger.TriggerInstance::item)
                    )
                    .apply(p_311435_, ShotCrossbowTrigger.TriggerInstance::new)
        );

        public static Criterion<ShotCrossbowTrigger.TriggerInstance> shotCrossbow(Optional<ItemPredicate> pItem) {
            return CriteriaTriggers.SHOT_CROSSBOW.createCriterion(new ShotCrossbowTrigger.TriggerInstance(Optional.empty(), pItem));
        }

        public static Criterion<ShotCrossbowTrigger.TriggerInstance> shotCrossbow(ItemLike pItem) {
            return CriteriaTriggers.SHOT_CROSSBOW
                .createCriterion(new ShotCrossbowTrigger.TriggerInstance(Optional.empty(), Optional.of(ItemPredicate.Builder.item().of(pItem).build())));
        }

        public boolean matches(ItemStack pItem) {
            return this.item.isEmpty() || this.item.get().matches(pItem);
        }
    }
}
