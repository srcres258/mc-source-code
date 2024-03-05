package net.minecraft.util.datafix;

import com.google.gson.JsonObject;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.GsonHelper;

public class ComponentDataFixUtils {
    private static final String EMPTY_CONTENTS = createTextComponentJson("");

    public static <T> Dynamic<T> createPlainTextComponent(DynamicOps<T> pOps, String pText) {
        String s = createTextComponentJson(pText);
        return new Dynamic<>(pOps, pOps.createString(s));
    }

    public static <T> Dynamic<T> createEmptyComponent(DynamicOps<T> pOps) {
        return new Dynamic<>(pOps, pOps.createString(EMPTY_CONTENTS));
    }

    private static String createTextComponentJson(String pText) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("text", pText);
        return GsonHelper.toStableString(jsonobject);
    }

    public static <T> Dynamic<T> createTranslatableComponent(DynamicOps<T> pOps, String pTranslationKey) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("translate", pTranslationKey);
        return new Dynamic<>(pOps, pOps.createString(GsonHelper.toStableString(jsonobject)));
    }

    public static <T> Dynamic<T> wrapLiteralStringAsComponent(Dynamic<T> pDynamic) {
        return DataFixUtils.orElse(pDynamic.asString().map(p_304989_ -> createPlainTextComponent(pDynamic.getOps(), p_304989_)).result(), pDynamic);
    }
}
