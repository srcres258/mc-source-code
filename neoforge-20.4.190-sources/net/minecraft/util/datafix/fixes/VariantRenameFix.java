package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Map;

public class VariantRenameFix extends NamedEntityFix {
    private final Map<String, String> renames;

    public VariantRenameFix(Schema pOutputSchema, String pName, TypeReference pType, String pEntityName, Map<String, String> pRenames) {
        super(pOutputSchema, false, pName, pType, pEntityName);
        this.renames = pRenames;
    }

    @Override
    protected Typed<?> fix(Typed<?> pTyped) {
        return pTyped.update(
            DSL.remainderFinder(),
            p_216750_ -> p_216750_.update(
                    "variant",
                    p_216755_ -> DataFixUtils.orElse(
                            p_216755_.asString().map(p_216753_ -> p_216755_.createString(this.renames.getOrDefault(p_216753_, p_216753_))).result(), p_216755_
                        )
                )
        );
    }
}
