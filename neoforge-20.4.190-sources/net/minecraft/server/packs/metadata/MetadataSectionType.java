package net.minecraft.server.packs.metadata;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.Util;

public interface MetadataSectionType<T> extends MetadataSectionSerializer<T> {
    JsonObject toJson(T pData);

    static <T> MetadataSectionType<T> fromCodec(final String pName, final Codec<T> pCodec) {
        return new MetadataSectionType<T>() {
            @Override
            public String getMetadataSectionName() {
                return pName;
            }

            @Override
            public T fromJson(JsonObject p_249450_) {
                return Util.getOrThrow(pCodec.parse(JsonOps.INSTANCE, p_249450_), JsonParseException::new);
            }

            @Override
            public JsonObject toJson(T p_250691_) {
                return Util.getOrThrow(pCodec.encodeStart(JsonOps.INSTANCE, p_250691_), IllegalArgumentException::new).getAsJsonObject();
            }
        };
    }
}
