package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataId;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A loot pool entry container that generates loot by referencing another loot table.
 */
public class LootTableReference extends LootPoolSingletonContainer {
    public static final Codec<LootTableReference> CODEC = RecordCodecBuilder.create(
        p_298029_ -> p_298029_.group(ResourceLocation.CODEC.fieldOf("name").forGetter(p_298023_ -> p_298023_.name))
                .and(singletonFields(p_298029_))
                .apply(p_298029_, LootTableReference::new)
    );
    private final ResourceLocation name;

    private LootTableReference(ResourceLocation p_79756_, int p_79757_, int p_79758_, List<LootItemCondition> p_298340_, List<LootItemFunction> p_298824_) {
        super(p_79757_, p_79758_, p_298340_, p_298824_);
        this.name = p_79756_;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.REFERENCE;
    }

    /**
     * Generate the loot stacks of this entry.
     * Contrary to the method name this method does not always generate one stack, it can also generate zero or multiple stacks.
     */
    @Override
    public void createItemStack(Consumer<ItemStack> pStackConsumer, LootContext pLootContext) {
        LootTable loottable = pLootContext.getResolver().getLootTable(this.name);
        loottable.getRandomItemsRaw(pLootContext, pStackConsumer);
    }

    @Override
    public void validate(ValidationContext pValidationContext) {
        LootDataId<LootTable> lootdataid = new LootDataId<>(LootDataType.TABLE, this.name);
        if (pValidationContext.hasVisitedElement(lootdataid)) {
            pValidationContext.reportProblem("Table " + this.name + " is recursively called");
        } else {
            super.validate(pValidationContext);
            pValidationContext.resolver()
                .getElementOptional(lootdataid)
                .ifPresentOrElse(
                    p_279078_ -> p_279078_.validate(pValidationContext.enterElement("->{" + this.name + "}", lootdataid)),
                    () -> pValidationContext.reportProblem("Unknown loot table called " + this.name)
                );
        }
    }

    public static LootPoolSingletonContainer.Builder<?> lootTableReference(ResourceLocation pTable) {
        return simpleBuilder((p_298025_, p_298026_, p_298027_, p_298028_) -> new LootTableReference(pTable, p_298025_, p_298026_, p_298027_, p_298028_));
    }
}
