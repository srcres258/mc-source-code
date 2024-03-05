package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.RecordBuilder.AbstractUniversalBuilder;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongList;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.annotation.Nullable;

public class JavaOps implements DynamicOps<Object> {
    public static final JavaOps INSTANCE = new JavaOps();

    private JavaOps() {
    }

    @Override
    public Object empty() {
        return null;
    }

    @Override
    public Object emptyMap() {
        return Map.of();
    }

    @Override
    public Object emptyList() {
        return List.of();
    }

    @Override
    public <U> U convertTo(DynamicOps<U> pOutOps, Object pInput) {
        if (pInput == null) {
            return pOutOps.empty();
        } else if (pInput instanceof Map) {
            return this.convertMap(pOutOps, pInput);
        } else if (pInput instanceof ByteList bytelist) {
            return pOutOps.createByteList(ByteBuffer.wrap(bytelist.toByteArray()));
        } else if (pInput instanceof IntList intlist) {
            return pOutOps.createIntList(intlist.intStream());
        } else if (pInput instanceof LongList longlist) {
            return pOutOps.createLongList(longlist.longStream());
        } else if (pInput instanceof List) {
            return this.convertList(pOutOps, pInput);
        } else if (pInput instanceof String s) {
            return pOutOps.createString(s);
        } else if (pInput instanceof Boolean obool) {
            return pOutOps.createBoolean(obool);
        } else if (pInput instanceof Byte obyte) {
            return pOutOps.createByte(obyte);
        } else if (pInput instanceof Short oshort) {
            return pOutOps.createShort(oshort);
        } else if (pInput instanceof Integer integer) {
            return pOutOps.createInt(integer);
        } else if (pInput instanceof Long olong) {
            return pOutOps.createLong(olong);
        } else if (pInput instanceof Float f) {
            return pOutOps.createFloat(f);
        } else if (pInput instanceof Double d0) {
            return pOutOps.createDouble(d0);
        } else if (pInput instanceof Number number) {
            return pOutOps.createNumeric(number);
        } else {
            throw new IllegalStateException("Don't know how to convert " + pInput);
        }
    }

    @Override
    public DataResult<Number> getNumberValue(Object pInput) {
        return pInput instanceof Number number ? DataResult.success(number) : DataResult.error(() -> "Not a number: " + pInput);
    }

    @Override
    public Object createNumeric(Number pValue) {
        return pValue;
    }

    @Override
    public Object createByte(byte pValue) {
        return pValue;
    }

    @Override
    public Object createShort(short pValue) {
        return pValue;
    }

    @Override
    public Object createInt(int pValue) {
        return pValue;
    }

    @Override
    public Object createLong(long pValue) {
        return pValue;
    }

    @Override
    public Object createFloat(float pValue) {
        return pValue;
    }

    @Override
    public Object createDouble(double pValue) {
        return pValue;
    }

    @Override
    public DataResult<Boolean> getBooleanValue(Object pInput) {
        return pInput instanceof Boolean obool ? DataResult.success(obool) : DataResult.error(() -> "Not a boolean: " + pInput);
    }

    @Override
    public Object createBoolean(boolean pValue) {
        return pValue;
    }

    @Override
    public DataResult<String> getStringValue(Object pInput) {
        return pInput instanceof String s ? DataResult.success(s) : DataResult.error(() -> "Not a string: " + pInput);
    }

    @Override
    public Object createString(String pValue) {
        return pValue;
    }

    @Override
    public DataResult<Object> mergeToList(Object pList, Object pValue) {
        if (pList == this.empty()) {
            return DataResult.success(List.of(pValue));
        } else if (pList instanceof List list) {
            return list.isEmpty()
                ? DataResult.success(List.of(pValue))
                : DataResult.success(ImmutableList.<Object>builder().addAll(list).add(pValue).build());
        } else {
            return DataResult.error(() -> "Not a list: " + pList);
        }
    }

    @Override
    public DataResult<Object> mergeToList(Object pList, List<Object> pValues) {
        if (pList == this.empty()) {
            return DataResult.success(pValues);
        } else if (pList instanceof List list) {
            return list.isEmpty() ? DataResult.success(pValues) : DataResult.success(ImmutableList.<Object>builder().addAll(list).addAll(pValues).build());
        } else {
            return DataResult.error(() -> "Not a list: " + pList);
        }
    }

    @Override
    public DataResult<Object> mergeToMap(Object pMap, Object pKey, Object pValue) {
        if (pMap == this.empty()) {
            return DataResult.success(Map.of(pKey, pValue));
        } else if (pMap instanceof Map map) {
            if (map.isEmpty()) {
                return DataResult.success(Map.of(pKey, pValue));
            } else {
                Builder<Object, Object> builder = ImmutableMap.builderWithExpectedSize(map.size() + 1);
                builder.putAll(map);
                builder.put(pKey, pValue);
                return DataResult.success(builder.buildKeepingLast());
            }
        } else {
            return DataResult.error(() -> "Not a map: " + pMap);
        }
    }

    @Override
    public DataResult<Object> mergeToMap(Object pMap, Map<Object, Object> pValues) {
        if (pMap == this.empty()) {
            return DataResult.success(pValues);
        } else if (pMap instanceof Map map) {
            if (map.isEmpty()) {
                return DataResult.success(pValues);
            } else {
                Builder<Object, Object> builder = ImmutableMap.builderWithExpectedSize(map.size() + pValues.size());
                builder.putAll(map);
                builder.putAll(pValues);
                return DataResult.success(builder.buildKeepingLast());
            }
        } else {
            return DataResult.error(() -> "Not a map: " + pMap);
        }
    }

    private static Map<Object, Object> mapLikeToMap(MapLike<Object> pMapLike) {
        return pMapLike.entries().collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    public DataResult<Object> mergeToMap(Object pMap, MapLike<Object> pValues) {
        if (pMap == this.empty()) {
            return DataResult.success(mapLikeToMap(pValues));
        } else if (pMap instanceof Map map) {
            if (map.isEmpty()) {
                return DataResult.success(mapLikeToMap(pValues));
            } else {
                Builder<Object, Object> builder = ImmutableMap.builderWithExpectedSize(map.size());
                builder.putAll(map);
                pValues.entries().forEach(p_304552_ -> builder.put(p_304552_.getFirst(), p_304552_.getSecond()));
                return DataResult.success(builder.buildKeepingLast());
            }
        } else {
            return DataResult.error(() -> "Not a map: " + pMap);
        }
    }

    static Stream<Pair<Object, Object>> getMapEntries(Map<?, ?> pInput) {
        return pInput.entrySet().stream().map(p_304506_ -> Pair.of(p_304506_.getKey(), p_304506_.getValue()));
    }

    @Override
    public DataResult<Stream<Pair<Object, Object>>> getMapValues(Object pInput) {
        return pInput instanceof Map map ? DataResult.success(getMapEntries(map)) : DataResult.error(() -> "Not a map: " + pInput);
    }

    @Override
    public DataResult<Consumer<BiConsumer<Object, Object>>> getMapEntries(Object pInput) {
        return pInput instanceof Map map ? DataResult.success(map::forEach) : DataResult.error(() -> "Not a map: " + pInput);
    }

    @Override
    public Object createMap(Stream<Pair<Object, Object>> pMap) {
        return pMap.collect(ImmutableMap.toImmutableMap(Pair::getFirst, Pair::getSecond));
    }

    @Override
    public DataResult<MapLike<Object>> getMap(Object pInput) {
        return pInput instanceof Map map ? DataResult.success(new MapLike<Object>() {
            @Nullable
            @Override
            public Object get(Object p_304705_) {
                return map.get(p_304705_);
            }

            @Nullable
            @Override
            public Object get(String p_304715_) {
                return map.get(p_304715_);
            }

            @Override
            public Stream<Pair<Object, Object>> entries() {
                return JavaOps.getMapEntries(map);
            }

            @Override
            public String toString() {
                return "MapLike[" + map + "]";
            }
        }) : DataResult.error(() -> "Not a map: " + pInput);
    }

    @Override
    public Object createMap(Map<Object, Object> pInput) {
        return pInput;
    }

    @Override
    public DataResult<Stream<Object>> getStream(Object pInput) {
        return pInput instanceof List list
            ? DataResult.success(list.stream().map(p_304639_ -> p_304639_))
            : DataResult.error(() -> "Not an list: " + pInput);
    }

    @Override
    public DataResult<Consumer<Consumer<Object>>> getList(Object pInput) {
        return pInput instanceof List list ? DataResult.success(list::forEach) : DataResult.error(() -> "Not an list: " + pInput);
    }

    @Override
    public Object createList(Stream<Object> pInput) {
        return pInput.toList();
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(Object pInput) {
        return pInput instanceof ByteList bytelist
            ? DataResult.success(ByteBuffer.wrap(bytelist.toByteArray()))
            : DataResult.error(() -> "Not a byte list: " + pInput);
    }

    @Override
    public Object createByteList(ByteBuffer pValue) {
        ByteBuffer bytebuffer = pValue.duplicate().clear();
        ByteArrayList bytearraylist = new ByteArrayList();
        bytearraylist.size(bytebuffer.capacity());
        bytebuffer.get(0, bytearraylist.elements(), 0, bytearraylist.size());
        return bytearraylist;
    }

    @Override
    public DataResult<IntStream> getIntStream(Object pInput) {
        return pInput instanceof IntList intlist ? DataResult.success(intlist.intStream()) : DataResult.error(() -> "Not an int list: " + pInput);
    }

    @Override
    public Object createIntList(IntStream pValue) {
        return IntArrayList.toList(pValue);
    }

    @Override
    public DataResult<LongStream> getLongStream(Object pInput) {
        return pInput instanceof LongList longlist ? DataResult.success(longlist.longStream()) : DataResult.error(() -> "Not a long list: " + pInput);
    }

    @Override
    public Object createLongList(LongStream pValue) {
        return LongArrayList.toList(pValue);
    }

    @Override
    public Object remove(Object pInput, String pKey) {
        if (pInput instanceof Map map) {
            Map<Object, Object> map1 = new LinkedHashMap<>(map);
            map1.remove(pKey);
            return DataResult.success(Map.copyOf(map1));
        } else {
            return DataResult.error(() -> "Not a map: " + pInput);
        }
    }

    @Override
    public RecordBuilder<Object> mapBuilder() {
        return new JavaOps.FixedMapBuilder<>(this);
    }

    @Override
    public String toString() {
        return "Java";
    }

    static final class FixedMapBuilder<T> extends AbstractUniversalBuilder<T, Builder<T, T>> {
        public FixedMapBuilder(DynamicOps<T> pOps) {
            super(pOps);
        }

        protected Builder<T, T> initBuilder() {
            return ImmutableMap.builder();
        }

        protected Builder<T, T> append(T pKey, T pValue, Builder<T, T> pBuilder) {
            return pBuilder.put(pKey, pValue);
        }

        protected DataResult<T> build(Builder<T, T> pBuilder, T pPrefix) {
            ImmutableMap<T, T> immutablemap;
            try {
                immutablemap = pBuilder.buildOrThrow();
            } catch (IllegalArgumentException illegalargumentexception) {
                return DataResult.error(() -> "Can't build map: " + illegalargumentexception.getMessage());
            }

            return this.ops().mergeToMap(pPrefix, immutablemap);
        }
    }
}
