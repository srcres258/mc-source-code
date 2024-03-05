package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import net.minecraft.Util;

public abstract class NamedEntityWriteReadFix extends DataFix {
    private final String name;
    private final String entityName;
    private final TypeReference type;

    public NamedEntityWriteReadFix(Schema pOutputSchema, boolean pChangesType, String pName, TypeReference pType, String pEntityName) {
        super(pOutputSchema, pChangesType);
        this.name = pName;
        this.type = pType;
        this.entityName = pEntityName;
    }

    @Override
    public TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(this.type);
        Type<?> type1 = this.getInputSchema().getChoiceType(this.type, this.entityName);
        Type<?> type2 = this.getOutputSchema().getType(this.type);
        Type<?> type3 = this.getOutputSchema().getChoiceType(this.type, this.entityName);
        OpticFinder<?> opticfinder = DSL.namedChoice(this.entityName, type1);
        return this.fixTypeEverywhereTyped(
            this.name,
            type,
            type2,
            p_307259_ -> p_307259_.updateTyped(opticfinder, type3, p_311565_ -> Util.writeAndReadTypedOrThrow(p_311565_, type3, this::fix))
        );
    }

    protected abstract <T> Dynamic<T> fix(Dynamic<T> p_307318_);
}
