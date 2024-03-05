package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;
import net.minecraft.Util;

public class EntityHorseSplitFix extends EntityRenameFix {
    public EntityHorseSplitFix(Schema pOutputSchema, boolean pChangesType) {
        super("EntityHorseSplitFix", pOutputSchema, pChangesType);
    }

    @Override
    protected Pair<String, Typed<?>> fix(String pEntityName, Typed<?> pTyped) {
        Dynamic<?> dynamic = pTyped.get(DSL.remainderFinder());
        if (Objects.equals("EntityHorse", pEntityName)) {
            int i = dynamic.get("Type").asInt(0);

            String s = switch(i) {
                case 1 -> "Donkey";
                case 2 -> "Mule";
                case 3 -> "ZombieHorse";
                case 4 -> "SkeletonHorse";
                default -> "Horse";
            };
            dynamic.remove("Type");
            Type<?> type = (Type)this.getOutputSchema().findChoiceType(References.ENTITY).types().get(s);
            return Pair.of(s, Util.writeAndReadTypedOrThrow(pTyped, type, p_311550_ -> p_311550_));
        } else {
            return Pair.of(pEntityName, pTyped);
        }
    }
}
