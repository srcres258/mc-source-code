package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that sets a stack's lore tag, optionally replacing any previously present lore.
 * The Components for the lore tag are optionally resolved relative to a given {@link LootContext.EntityTarget} for entity-sensitive component data such as scoreboard scores.
 */
public class SetLoreFunction extends LootItemConditionalFunction {
    public static final Codec<SetLoreFunction> CODEC = RecordCodecBuilder.create(
        p_304382_ -> commonFields(p_304382_)
                .and(
                    p_304382_.group(
                        Codec.BOOL.fieldOf("replace").orElse(false).forGetter(p_299000_ -> p_299000_.replace),
                        ComponentSerialization.CODEC.listOf().fieldOf("lore").forGetter(p_299022_ -> p_299022_.lore),
                        ExtraCodecs.strictOptionalField(LootContext.EntityTarget.CODEC, "entity").forGetter(p_298481_ -> p_298481_.resolutionContext)
                    )
                )
                .apply(p_304382_, SetLoreFunction::new)
    );
    private final boolean replace;
    private final List<Component> lore;
    private final Optional<LootContext.EntityTarget> resolutionContext;

    public SetLoreFunction(List<LootItemCondition> p_81085_, boolean p_81084_, List<Component> p_298633_, Optional<LootContext.EntityTarget> p_298623_) {
        super(p_81085_);
        this.replace = p_81084_;
        this.lore = List.copyOf(p_298633_);
        this.resolutionContext = p_298623_;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_LORE;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.resolutionContext.<Set<LootContextParam<?>>>map(p_298664_ -> Set.of(p_298664_.getParam())).orElseGet(Set::of);
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        ListTag listtag = this.getLoreTag(pStack, !this.lore.isEmpty());
        if (listtag != null) {
            if (this.replace) {
                listtag.clear();
            }

            UnaryOperator<Component> unaryoperator = SetNameFunction.createResolver(pContext, this.resolutionContext.orElse(null));
            this.lore.stream().map(unaryoperator).map(Component.Serializer::toJson).map(StringTag::valueOf).forEach(listtag::add);
        }

        return pStack;
    }

    @Nullable
    private ListTag getLoreTag(ItemStack pStack, boolean pCreateIfMissing) {
        CompoundTag compoundtag;
        if (pStack.hasTag()) {
            compoundtag = pStack.getTag();
        } else {
            if (!pCreateIfMissing) {
                return null;
            }

            compoundtag = new CompoundTag();
            pStack.setTag(compoundtag);
        }

        CompoundTag compoundtag1;
        if (compoundtag.contains("display", 10)) {
            compoundtag1 = compoundtag.getCompound("display");
        } else {
            if (!pCreateIfMissing) {
                return null;
            }

            compoundtag1 = new CompoundTag();
            compoundtag.put("display", compoundtag1);
        }

        if (compoundtag1.contains("Lore", 9)) {
            return compoundtag1.getList("Lore", 8);
        } else if (pCreateIfMissing) {
            ListTag listtag = new ListTag();
            compoundtag1.put("Lore", listtag);
            return listtag;
        } else {
            return null;
        }
    }

    public static SetLoreFunction.Builder setLore() {
        return new SetLoreFunction.Builder();
    }

    public static class Builder extends LootItemConditionalFunction.Builder<SetLoreFunction.Builder> {
        private boolean replace;
        private Optional<LootContext.EntityTarget> resolutionContext = Optional.empty();
        private final ImmutableList.Builder<Component> lore = ImmutableList.builder();

        public SetLoreFunction.Builder setReplace(boolean pReplace) {
            this.replace = pReplace;
            return this;
        }

        public SetLoreFunction.Builder setResolutionContext(LootContext.EntityTarget pResolutionContext) {
            this.resolutionContext = Optional.of(pResolutionContext);
            return this;
        }

        public SetLoreFunction.Builder addLine(Component pLine) {
            this.lore.add(pLine);
            return this;
        }

        protected SetLoreFunction.Builder getThis() {
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetLoreFunction(this.getConditions(), this.replace, this.lore.build(), this.resolutionContext);
        }
    }
}
