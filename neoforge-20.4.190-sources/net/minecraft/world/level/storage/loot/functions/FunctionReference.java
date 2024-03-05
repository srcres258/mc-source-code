package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class FunctionReference extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<FunctionReference> CODEC = RecordCodecBuilder.create(
        p_298089_ -> commonFields(p_298089_)
                .and(ResourceLocation.CODEC.fieldOf("name").forGetter(p_298088_ -> p_298088_.name))
                .apply(p_298089_, FunctionReference::new)
    );
    private final ResourceLocation name;

    private FunctionReference(List<LootItemCondition> p_298661_, ResourceLocation p_279246_) {
        super(p_298661_);
        this.name = p_279246_;
    }

    @Override
    public LootItemFunctionType getType() {
        return LootItemFunctions.REFERENCE;
    }

    /**
     * Validate that this object is used correctly according to the given ValidationContext.
     */
    @Override
    public void validate(ValidationContext pContext) {
        LootDataId<LootItemFunction> lootdataid = new LootDataId<>(LootDataType.MODIFIER, this.name);
        if (pContext.hasVisitedElement(lootdataid)) {
            pContext.reportProblem("Function " + this.name + " is recursively called");
        } else {
            super.validate(pContext);
            pContext.resolver()
                .getElementOptional(lootdataid)
                .ifPresentOrElse(
                    p_279367_ -> p_279367_.validate(pContext.enterElement(".{" + this.name + "}", lootdataid)),
                    () -> pContext.reportProblem("Unknown function table called " + this.name)
                );
        }
    }

    /**
     * Called to perform the actual action of this function, after conditions have been checked.
     */
    @Override
    protected ItemStack run(ItemStack pStack, LootContext pContext) {
        LootItemFunction lootitemfunction = pContext.getResolver().getElement(LootDataType.MODIFIER, this.name);
        if (lootitemfunction == null) {
            LOGGER.warn("Unknown function: {}", this.name);
            return pStack;
        } else {
            LootContext.VisitedEntry<?> visitedentry = LootContext.createVisitedEntry(lootitemfunction);
            if (pContext.pushVisitedElement(visitedentry)) {
                ItemStack itemstack;
                try {
                    itemstack = lootitemfunction.apply(pStack, pContext);
                } finally {
                    pContext.popVisitedElement(visitedentry);
                }

                return itemstack;
            } else {
                LOGGER.warn("Detected infinite loop in loot tables");
                return pStack;
            }
        }
    }

    public static LootItemConditionalFunction.Builder<?> functionReference(ResourceLocation pName) {
        return simpleBuilder(p_298091_ -> new FunctionReference(p_298091_, pName));
    }
}
