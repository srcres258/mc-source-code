package net.minecraft.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.JavaOps;

public class Cloner<T> {
    private final Codec<T> directCodec;

    Cloner(Codec<T> pDirectCodec) {
        this.directCodec = pDirectCodec;
    }

    public T clone(T pObject, HolderLookup.Provider pLookupProvider1, HolderLookup.Provider pLookupProvider2) {
        DynamicOps<Object> dynamicops = RegistryOps.create(JavaOps.INSTANCE, pLookupProvider1);
        DynamicOps<Object> dynamicops1 = RegistryOps.create(JavaOps.INSTANCE, pLookupProvider2);
        Object object = Util.getOrThrow(
            this.directCodec.encodeStart(dynamicops, pObject), p_312200_ -> new IllegalStateException("Failed to encode: " + p_312200_)
        );
        return Util.getOrThrow(this.directCodec.parse(dynamicops1, object), p_312832_ -> new IllegalStateException("Failed to decode: " + p_312832_));
    }

    public static class Factory {
        private final Map<ResourceKey<? extends Registry<?>>, Cloner<?>> codecs = new HashMap<>();

        public <T> Cloner.Factory addCodec(ResourceKey<? extends Registry<? extends T>> pRegistryKey, Codec<T> pCodec) {
            this.codecs.put(pRegistryKey, new Cloner<>(pCodec));
            return this;
        }

        @Nullable
        public <T> Cloner<T> cloner(ResourceKey<? extends Registry<? extends T>> pRegistryKey) {
            return (Cloner<T>)this.codecs.get(pRegistryKey);
        }
    }
}
