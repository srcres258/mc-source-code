package net.minecraft.world.level.storage.loot.functions;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

/**
 * LootItemFunction that sets a stack's name.
 * The Component for the name is optionally resolved relative to a given {@link LootContext.EntityTarget} for entity-sensitive component data such as scoreboard scores.
 */
public class SetNameFunction extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<SetNameFunction> CODEC = RecordCodecBuilder.create(
        p_304383_ -> commonFields(p_304383_)
                .and(
                    p_304383_.group(
                        ExtraCodecs.strictOptionalField(ComponentSerialization.CODEC, "name").forGetter(p_298144_ -> p_298144_.name),
                        ExtraCodecs.strictOptionalField(LootContext.EntityTarget.CODEC, "entity").forGetter(p_298153_ -> p_298153_.resolutionContext)
                    )
                )
                .apply(p_304383_, SetNameFunction::new)
    );
    private final Optional<Component> name;
    private final Optional<LootContext.EntityTarget> resolutionContext;

    private SetNameFunction(List<LootItemCondition> p_299241_, Optional<Component> p_298804_, Optional<LootContext.EntityTarget> p_298545_) {
        super(p_299241_);
        this.name = p_298804_;
        this.resolutionContext = p_298545_;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.SET_NAME;
    }

    /**
     * Get the parameters used by this object.
     */
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return this.resolutionContext.<Set<LootContextParam<?>>>map(p_298146_ -> Set.of(p_298146_.getParam())).orElse(Set.of());
    }

    /**
     * Create a UnaryOperator that resolves Components based on the given LootContext and EntityTarget.
     * This will replace for example score components.
     */
    public static UnaryOperator<Component> createResolver(LootContext pLootContext, @Nullable LootContext.EntityTarget pResolutionContext) {
        if (pResolutionContext != null) {
            Entity entity = pLootContext.getParamOrNull(pResolutionContext.getParam());
            if (entity != null) {
                CommandSourceStack commandsourcestack = entity.createCommandSourceStack().withPermission(2);
                return p_81147_ -> {
                    try {
                        return ComponentUtils.updateForEntity(commandsourcestack, p_81147_, entity, 0);
                    } catch (CommandSyntaxException commandsyntaxexception) {
                        LOGGER.warn("Failed to resolve text component", (Throwable)commandsyntaxexception);
                        return p_81147_;
                    }
                };
            }
        }

        return p_81152_ -> p_81152_;
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        this.name.ifPresent(p_298149_ -> pStack.setHoverName(createResolver(pContext, this.resolutionContext.orElse(null)).apply(p_298149_)));
        return pStack;
    }

    public static LootItemConditionalFunction.Builder<?> setName(Component pName) {
        return simpleBuilder(p_298143_ -> new SetNameFunction(p_298143_, Optional.of(pName), Optional.empty()));
    }

    public static LootItemConditionalFunction.Builder<?> setName(Component pName, LootContext.EntityTarget pResolutionContext) {
        return simpleBuilder(p_298152_ -> new SetNameFunction(p_298152_, Optional.of(pName), Optional.of(pResolutionContext)));
    }
}
