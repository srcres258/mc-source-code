package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;

public class OptionsAddTextBackgroundFix extends DataFix {
    public OptionsAddTextBackgroundFix(Schema pOutputSchema, boolean pChangesType) {
        super(pOutputSchema, pChangesType);
    }

    @Override
    public TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped(
            "OptionsAddTextBackgroundFix",
            this.getInputSchema().getType(References.OPTIONS),
            p_16610_ -> p_16610_.update(
                    DSL.remainderFinder(),
                    p_145567_ -> DataFixUtils.orElse(
                            p_145567_.get("chatOpacity")
                                .asString()
                                .map(p_145570_ -> p_145567_.set("textBackgroundOpacity", p_145567_.createDouble(this.calculateBackground(p_145570_))))
                                .result(),
                            p_145567_
                        )
                )
        );
    }

    private double calculateBackground(String pOldBackground) {
        try {
            double d0 = 0.9 * Double.parseDouble(pOldBackground) + 0.1;
            return d0 / 2.0;
        } catch (NumberFormatException numberformatexception) {
            return 0.5;
        }
    }
}
