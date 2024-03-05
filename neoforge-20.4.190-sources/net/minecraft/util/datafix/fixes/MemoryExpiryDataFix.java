package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;

public class MemoryExpiryDataFix extends NamedEntityFix {
    public MemoryExpiryDataFix(Schema pOutputSchema, String pEntityName) {
        super(pOutputSchema, false, "Memory expiry data fix (" + pEntityName + ")", References.ENTITY, pEntityName);
    }

    @Override
    protected Typed<?> fix(Typed<?> pTyped) {
        return pTyped.update(DSL.remainderFinder(), this::fixTag);
    }

    public Dynamic<?> fixTag(Dynamic<?> p_16412_) {
        return p_16412_.update("Brain", this::updateBrain);
    }

    private Dynamic<?> updateBrain(Dynamic<?> p_16414_) {
        return p_16414_.update("memories", this::updateMemories);
    }

    private Dynamic<?> updateMemories(Dynamic<?> p_16416_) {
        return p_16416_.updateMapValues(this::updateMemoryEntry);
    }

    private Pair<Dynamic<?>, Dynamic<?>> updateMemoryEntry(Pair<Dynamic<?>, Dynamic<?>> p_16410_) {
        return p_16410_.mapSecond(this::wrapMemoryValue);
    }

    private Dynamic<?> wrapMemoryValue(Dynamic<?> p_16418_) {
        return p_16418_.createMap(ImmutableMap.of(p_16418_.createString("value"), p_16418_));
    }
}
