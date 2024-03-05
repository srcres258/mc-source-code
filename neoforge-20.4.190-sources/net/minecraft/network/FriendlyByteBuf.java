package net.minecraft.network;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ByteProcessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class FriendlyByteBuf extends ByteBuf implements net.neoforged.neoforge.common.extensions.IFriendlyByteBufExtension {
    public static final int DEFAULT_NBT_QUOTA = 2097152;
    private final ByteBuf source;
    public static final short MAX_STRING_LENGTH = 32767;
    public static final int MAX_COMPONENT_STRING_LENGTH = 262144;
    private static final int PUBLIC_KEY_SIZE = 256;
    private static final int MAX_PUBLIC_KEY_HEADER_SIZE = 256;
    private static final int MAX_PUBLIC_KEY_LENGTH = 512;
    private static final Gson GSON = new Gson();

    public FriendlyByteBuf(ByteBuf pSource) {
        this.source = pSource;
    }

    @Deprecated
    public <T> T readWithCodecTrusted(DynamicOps<Tag> pOps, Codec<T> pCodec) {
        return this.readWithCodec(pOps, pCodec, NbtAccounter.unlimitedHeap());
    }

    @Deprecated
    public <T> T readWithCodec(DynamicOps<Tag> pOps, Codec<T> pCodec, NbtAccounter pNbtAccounter) {
        Tag tag = this.readNbt(pNbtAccounter);
        return Util.getOrThrow(pCodec.parse(pOps, tag), p_261423_ -> new DecoderException("Failed to decode: " + p_261423_ + " " + tag));
    }

    @Deprecated
    public <T> FriendlyByteBuf writeWithCodec(DynamicOps<Tag> pOps, Codec<T> pCodec, T pValue) {
        Tag tag = Util.getOrThrow(
            pCodec.encodeStart(pOps, pValue), p_272384_ -> new EncoderException("Failed to encode: " + p_272384_ + " " + pValue)
        );
        this.writeNbt(tag);
        return this;
    }

    public <T> T readJsonWithCodec(Codec<T> pCodec) {
        JsonElement jsonelement = GsonHelper.fromJson(GSON, this.readUtf(), JsonElement.class);
        DataResult<T> dataresult = pCodec.parse(JsonOps.INSTANCE, jsonelement);
        return Util.getOrThrow(dataresult, p_272382_ -> new DecoderException("Failed to decode json: " + p_272382_));
    }

    public <T> void writeJsonWithCodec(Codec<T> pCodec, T pValue) {
        DataResult<JsonElement> dataresult = pCodec.encodeStart(JsonOps.INSTANCE, pValue);
        this.writeUtf(GSON.toJson(Util.getOrThrow(dataresult, p_261421_ -> new EncoderException("Failed to encode: " + p_261421_ + " " + pValue))));
    }

    public <T> void writeId(IdMap<T> pIdMap, T pValue) {
        int i = pIdMap.getId(pValue);
        if (i == -1) {
            throw new IllegalArgumentException("Can't find id for '" + pValue + "' in map " + pIdMap);
        } else {
            this.writeVarInt(i);
        }
    }

    public <T> void writeId(IdMap<Holder<T>> pIdMap, Holder<T> pValue, FriendlyByteBuf.Writer<T> pWriter) {
        switch(pValue.kind()) {
            case REFERENCE:
                int i = pIdMap.getId(pValue);
                if (i == -1) {
                    throw new IllegalArgumentException("Can't find id for '" + pValue.value() + "' in map " + pIdMap);
                }

                this.writeVarInt(i + 1);
                break;
            case DIRECT:
                this.writeVarInt(0);
                pWriter.accept(this, pValue.value());
        }
    }

    @Nullable
    public <T> T readById(IdMap<T> pIdMap) {
        int i = this.readVarInt();
        return pIdMap.byId(i);
    }

    public <T> Holder<T> readById(IdMap<Holder<T>> pIdMap, FriendlyByteBuf.Reader<T> pReader) {
        int i = this.readVarInt();
        if (i == 0) {
            return Holder.direct(pReader.apply(this));
        } else {
            Holder<T> holder = pIdMap.byId(i - 1);
            if (holder == null) {
                throw new IllegalArgumentException("Can't find element with id " + i);
            } else {
                return holder;
            }
        }
    }

    public static <T> IntFunction<T> limitValue(IntFunction<T> pFunction, int pLimit) {
        return p_182686_ -> {
            if (p_182686_ > pLimit) {
                throw new DecoderException("Value " + p_182686_ + " is larger than limit " + pLimit);
            } else {
                return pFunction.apply(p_182686_);
            }
        };
    }

    /**
     * Read a collection from this buffer. First a new collection is created given the number of elements using {@code collectionFactory}.
     * Then every element is read using {@code elementReader}.
     *
     * @see #writeCollection
     */
    public <T, C extends Collection<T>> C readCollection(IntFunction<C> pCollectionFactory, FriendlyByteBuf.Reader<T> pElementReader) {
        int i = this.readVarInt();
        C c = pCollectionFactory.apply(i);

        for(int j = 0; j < i; ++j) {
            c.add(pElementReader.apply(this));
        }

        return c;
    }

    /**
     * Write a collection to this buffer. Every element is encoded in order using {@code elementWriter}.
     *
     * @see #readCollection
     */
    public <T> void writeCollection(Collection<T> pCollection, FriendlyByteBuf.Writer<T> pElementWriter) {
        this.writeVarInt(pCollection.size());

        for(T t : pCollection) {
            pElementWriter.accept(this, t);
        }
    }

    /**
     * Read a List from this buffer. First a new list is created given the number of elements.
     * Then every element is read using {@code elementReader}.
     *
     * @see #writeCollection
     */
    public <T> List<T> readList(FriendlyByteBuf.Reader<T> pElementReader) {
        return this.readCollection(Lists::newArrayListWithCapacity, pElementReader);
    }

    /**
     * Read an IntList of VarInts from this buffer.
     *
     * @see #writeIntIdList
     */
    public IntList readIntIdList() {
        int i = this.readVarInt();
        IntList intlist = new IntArrayList();

        for(int j = 0; j < i; ++j) {
            intlist.add(this.readVarInt());
        }

        return intlist;
    }

    /**
     * Write an IntList to this buffer. Every element is encoded as a VarInt.
     *
     * @see #readIntIdList
     */
    public void writeIntIdList(IntList pItIdList) {
        this.writeVarInt(pItIdList.size());
        pItIdList.forEach(this::writeVarInt);
    }

    /**
     * Read a Map from this buffer. First a new Map is created given the number of elements using {@code mapFactory}.
     * Then all keys and values are read using the given {@code keyReader} and {@code valueReader}.
     *
     * @see #writeMap
     */
    public <K, V, M extends Map<K, V>> M readMap(IntFunction<M> pMapFactory, FriendlyByteBuf.Reader<K> pKeyReader, FriendlyByteBuf.Reader<V> pValueReader) {
        int i = this.readVarInt();
        M m = pMapFactory.apply(i);

        for(int j = 0; j < i; ++j) {
            K k = pKeyReader.apply(this);
            V v = pValueReader.apply(this);
            m.put(k, v);
        }

        return m;
    }

    /**
     * Read a Map from this buffer. First a new HashMap is created.
     * Then all keys and values are read using the given {@code keyReader} and {@code valueReader}.
     *
     * @see #writeMap
     */
    public <K, V> Map<K, V> readMap(FriendlyByteBuf.Reader<K> pKeyReader, FriendlyByteBuf.Reader<V> pValueReader) {
        return this.readMap(Maps::newHashMapWithExpectedSize, pKeyReader, pValueReader);
    }

    /**
     * Write a Map to this buffer. First the size of the map is written as a VarInt.
     * Then all keys and values are written using the given {@code keyWriter} and {@code valueWriter}.
     *
     * @see #readMap
     */
    public <K, V> void writeMap(Map<K, V> pMap, FriendlyByteBuf.Writer<K> pKeyWriter, FriendlyByteBuf.Writer<V> pValueWriter) {
        this.writeVarInt(pMap.size());
        pMap.forEach((p_236856_, p_236857_) -> {
            pKeyWriter.accept(this, p_236856_);
            pValueWriter.accept(this, p_236857_);
        });
    }

    /**
     * Read a VarInt N from this buffer, then reads N values by calling {@code reader}.
     */
    public void readWithCount(Consumer<FriendlyByteBuf> pReader) {
        int i = this.readVarInt();

        for(int j = 0; j < i; ++j) {
            pReader.accept(this);
        }
    }

    public <E extends Enum<E>> void writeEnumSet(EnumSet<E> pEnumSet, Class<E> pEnumClass) {
        E[] ae = pEnumClass.getEnumConstants();
        BitSet bitset = new BitSet(ae.length);

        for(int i = 0; i < ae.length; ++i) {
            bitset.set(i, pEnumSet.contains(ae[i]));
        }

        this.writeFixedBitSet(bitset, ae.length);
    }

    public <E extends Enum<E>> EnumSet<E> readEnumSet(Class<E> pEnumClass) {
        E[] ae = pEnumClass.getEnumConstants();
        BitSet bitset = this.readFixedBitSet(ae.length);
        EnumSet<E> enumset = EnumSet.noneOf(pEnumClass);

        for(int i = 0; i < ae.length; ++i) {
            if (bitset.get(i)) {
                enumset.add(ae[i]);
            }
        }

        return enumset;
    }

    public <T> void writeOptional(Optional<T> pOptional, FriendlyByteBuf.Writer<T> pWriter) {
        if (pOptional.isPresent()) {
            this.writeBoolean(true);
            pWriter.accept(this, pOptional.get());
        } else {
            this.writeBoolean(false);
        }
    }

    public <T> Optional<T> readOptional(FriendlyByteBuf.Reader<T> pReader) {
        return this.readBoolean() ? Optional.of(pReader.apply(this)) : Optional.empty();
    }

    @Nullable
    public <T> T readNullable(FriendlyByteBuf.Reader<T> pReader) {
        return this.readBoolean() ? pReader.apply(this) : null;
    }

    public <T> void writeNullable(@Nullable T pValue, FriendlyByteBuf.Writer<T> pWriter) {
        if (pValue != null) {
            this.writeBoolean(true);
            pWriter.accept(this, pValue);
        } else {
            this.writeBoolean(false);
        }
    }

    public <L, R> void writeEither(Either<L, R> pValue, FriendlyByteBuf.Writer<L> pLeftWriter, FriendlyByteBuf.Writer<R> pRightWriter) {
        pValue.ifLeft(p_293716_ -> {
            this.writeBoolean(true);
            pLeftWriter.accept(this, p_293716_);
        }).ifRight(p_293718_ -> {
            this.writeBoolean(false);
            pRightWriter.accept(this, p_293718_);
        });
    }

    public <L, R> Either<L, R> readEither(FriendlyByteBuf.Reader<L> pLeftReader, FriendlyByteBuf.Reader<R> pRightReader) {
        return this.readBoolean() ? Either.left(pLeftReader.apply(this)) : Either.right(pRightReader.apply(this));
    }

    public byte[] readByteArray() {
        return this.readByteArray(this.readableBytes());
    }

    public FriendlyByteBuf writeByteArray(byte[] pArray) {
        this.writeVarInt(pArray.length);
        this.writeBytes(pArray);
        return this;
    }

    public byte[] readByteArray(int pMaxLength) {
        int i = this.readVarInt();
        if (i > pMaxLength) {
            throw new DecoderException("ByteArray with size " + i + " is bigger than allowed " + pMaxLength);
        } else {
            byte[] abyte = new byte[i];
            this.readBytes(abyte);
            return abyte;
        }
    }

    /**
     * Writes an array of VarInts to the buffer, prefixed by the length of the array (as a VarInt).
     *
     * @see #readVarIntArray
     */
    public FriendlyByteBuf writeVarIntArray(int[] pArray) {
        this.writeVarInt(pArray.length);

        for(int i : pArray) {
            this.writeVarInt(i);
        }

        return this;
    }

    /**
     * Reads an array of VarInts from this buffer.
     *
     * @see #writeVarIntArray
     */
    public int[] readVarIntArray() {
        return this.readVarIntArray(this.readableBytes());
    }

    /**
     * Reads an array of VarInts with a maximum length from this buffer.
     *
     * @see #writeVarIntArray
     */
    public int[] readVarIntArray(int pMaxLength) {
        int i = this.readVarInt();
        if (i > pMaxLength) {
            throw new DecoderException("VarIntArray with size " + i + " is bigger than allowed " + pMaxLength);
        } else {
            int[] aint = new int[i];

            for(int j = 0; j < aint.length; ++j) {
                aint[j] = this.readVarInt();
            }

            return aint;
        }
    }

    /**
     * Writes an array of longs to the buffer, prefixed by the length of the array (as a VarInt).
     *
     * @see #readLongArray
     */
    public FriendlyByteBuf writeLongArray(long[] pArray) {
        this.writeVarInt(pArray.length);

        for(long i : pArray) {
            this.writeLong(i);
        }

        return this;
    }

    /**
     * Reads a length-prefixed array of longs from the buffer.
     */
    public long[] readLongArray() {
        return this.readLongArray(null);
    }

    /**
     * Reads a length-prefixed array of longs from the buffer.
     * Will try to use the given long[] if possible. Note that if an array with the correct size is given, maxLength is ignored.
     */
    public long[] readLongArray(@Nullable long[] pArray) {
        return this.readLongArray(pArray, this.readableBytes() / 8);
    }

    /**
     * Reads a length-prefixed array of longs with a maximum length from the buffer.
     * Will try to use the given long[] if possible. Note that if an array with the correct size is given, maxLength is ignored.
     */
    public long[] readLongArray(@Nullable long[] pArray, int pMaxLength) {
        int i = this.readVarInt();
        if (pArray == null || pArray.length != i) {
            if (i > pMaxLength) {
                throw new DecoderException("LongArray with size " + i + " is bigger than allowed " + pMaxLength);
            }

            pArray = new long[i];
        }

        for(int j = 0; j < pArray.length; ++j) {
            pArray[j] = this.readLong();
        }

        return pArray;
    }

    /**
     * Reads a BlockPos encoded as a long from the buffer.
     *
     * @see #writeBlockPos
     */
    public BlockPos readBlockPos() {
        return BlockPos.of(this.readLong());
    }

    /**
     * Writes a BlockPos encoded as a long to the buffer.
     *
     * @see #readBlockPos
     */
    public FriendlyByteBuf writeBlockPos(BlockPos pPos) {
        this.writeLong(pPos.asLong());
        return this;
    }

    /**
     * Reads a ChunkPos encoded as a long from the buffer.
     *
     * @see #writeChunkPos
     */
    public ChunkPos readChunkPos() {
        return new ChunkPos(this.readLong());
    }

    /**
     * Writes a ChunkPos encoded as a long to the buffer.
     *
     * @see #readChunkPos
     */
    public FriendlyByteBuf writeChunkPos(ChunkPos pChunkPos) {
        this.writeLong(pChunkPos.toLong());
        return this;
    }

    /**
     * Reads a SectionPos encoded as a long from the buffer.
     *
     * @see #writeSectionPos
     */
    public SectionPos readSectionPos() {
        return SectionPos.of(this.readLong());
    }

    /**
     * Writes a SectionPos encoded as a long to the buffer.
     *
     * @see #readSectionPos
     */
    public FriendlyByteBuf writeSectionPos(SectionPos pSectionPos) {
        this.writeLong(pSectionPos.asLong());
        return this;
    }

    public GlobalPos readGlobalPos() {
        ResourceKey<Level> resourcekey = this.readResourceKey(Registries.DIMENSION);
        BlockPos blockpos = this.readBlockPos();
        return GlobalPos.of(resourcekey, blockpos);
    }

    public void writeGlobalPos(GlobalPos pPos) {
        this.writeResourceKey(pPos.dimension());
        this.writeBlockPos(pPos.pos());
    }

    public Vector3f readVector3f() {
        return new Vector3f(this.readFloat(), this.readFloat(), this.readFloat());
    }

    public void writeVector3f(Vector3f pVector3f) {
        this.writeFloat(pVector3f.x());
        this.writeFloat(pVector3f.y());
        this.writeFloat(pVector3f.z());
    }

    public Quaternionf readQuaternion() {
        return new Quaternionf(this.readFloat(), this.readFloat(), this.readFloat(), this.readFloat());
    }

    public void writeQuaternion(Quaternionf pQuaternion) {
        this.writeFloat(pQuaternion.x);
        this.writeFloat(pQuaternion.y);
        this.writeFloat(pQuaternion.z);
        this.writeFloat(pQuaternion.w);
    }

    public Vec3 readVec3() {
        return new Vec3(this.readDouble(), this.readDouble(), this.readDouble());
    }

    public void writeVec3(Vec3 pVec3) {
        this.writeDouble(pVec3.x());
        this.writeDouble(pVec3.y());
        this.writeDouble(pVec3.z());
    }

    /**
     * Reads a Component encoded as a JSON string from the buffer.
     *
     * @see #writeComponent
     */
    public Component readComponent() {
        return this.readWithCodec(NbtOps.INSTANCE, ComponentSerialization.CODEC, NbtAccounter.create(2097152L));
    }

    public Component readComponentTrusted() {
        return this.readWithCodecTrusted(NbtOps.INSTANCE, ComponentSerialization.CODEC);
    }

    /**
     * Writes a Component encoded as a JSON string to the buffer.
     *
     * @see #readComponent
     */
    public FriendlyByteBuf writeComponent(Component pComponent) {
        return this.writeWithCodec(NbtOps.INSTANCE, ComponentSerialization.CODEC, pComponent);
    }

    /**
     * Reads an enum of the given type T using the ordinal encoded as a VarInt from the buffer.
     *
     * @see #writeEnum
     */
    public <T extends Enum<T>> T readEnum(Class<T> pEnumClass) {
        return pEnumClass.getEnumConstants()[this.readVarInt()];
    }

    /**
     * Writes an enum of the given type T using the ordinal encoded as a VarInt to the buffer.
     *
     * @see #readEnum
     */
    public FriendlyByteBuf writeEnum(Enum<?> pValue) {
        return this.writeVarInt(pValue.ordinal());
    }

    public <T> T readById(IntFunction<T> pIdLookuo) {
        int i = this.readVarInt();
        return pIdLookuo.apply(i);
    }

    public <T> FriendlyByteBuf writeById(ToIntFunction<T> pIdGetter, T pValue) {
        int i = pIdGetter.applyAsInt(pValue);
        return this.writeVarInt(i);
    }

    /**
     * Reads a compressed int from the buffer. To do so it maximally reads 5 byte-sized chunks whose most significant bit dictates whether another byte should be read.
     *
     * @see #writeVarInt
     */
    public int readVarInt() {
        return VarInt.read(this.source);
    }

    /**
     * Reads a compressed long from the buffer. To do so it maximally reads 10 byte-sized chunks whose most significant bit dictates whether another byte should be read.
     *
     * @see #writeVarLong
     */
    public long readVarLong() {
        return VarLong.read(this.source);
    }

    /**
     * Writes a UUID encoded as two longs to this buffer.
     *
     * @see #readUUID
     */
    public FriendlyByteBuf writeUUID(UUID pUuid) {
        this.writeLong(pUuid.getMostSignificantBits());
        this.writeLong(pUuid.getLeastSignificantBits());
        return this;
    }

    /**
     * Reads a UUID encoded as two longs from this buffer.
     *
     * @see #writeUUID
     */
    public UUID readUUID() {
        return new UUID(this.readLong(), this.readLong());
    }

    /**
     * Writes a compressed int to the buffer. The smallest number of bytes to fit the passed int will be written. Of each such byte only 7 bits will be used to describe the actual value since its most significant bit dictates whether the next byte is part of that same int. Micro-optimization for int values that are usually small.
     */
    public FriendlyByteBuf writeVarInt(int p_130131_) {
        VarInt.write(this.source, p_130131_);
        return this;
    }

    /**
     * Writes a compressed long to the buffer. The smallest number of bytes to fit the passed long will be written. Of each such byte only 7 bits will be used to describe the actual value since its most significant bit dictates whether the next byte is part of that same long. Micro-optimization for long values that are usually small.
     */
    public FriendlyByteBuf writeVarLong(long pValue) {
        VarLong.write(this.source, pValue);
        return this;
    }

    public FriendlyByteBuf writeNbt(@Nullable Tag pTag) {
        if (pTag == null) {
            pTag = EndTag.INSTANCE;
        }

        try {
            NbtIo.writeAnyTag(pTag, new ByteBufOutputStream(this));
            return this;
        } catch (IOException ioexception) {
            throw new EncoderException(ioexception);
        }
    }

    /**
     * Reads a NBT CompoundTag from this buffer.
     * {@code null} is a valid value and may be returned.
     *
     * This method will read a maximum of 0x200000 bytes.
     *
     * @see #writeNbt
     * @see #readAnySizeNbt
     * @see #readNbt(NbtAccounter)
     */
    @Nullable
    public CompoundTag readNbt() {
        Tag tag = this.readNbt(NbtAccounter.create(2097152L));
        if (tag != null && !(tag instanceof CompoundTag)) {
            throw new DecoderException("Not a compound tag: " + tag);
        } else {
            return (CompoundTag)tag;
        }
    }

    @Nullable
    public Tag readNbt(NbtAccounter pNbtAccounter) {
        try {
            Tag tag = NbtIo.readAnyTag(new ByteBufInputStream(this), pNbtAccounter);
            return tag.getId() == 0 ? null : tag;
        } catch (IOException ioexception) {
            throw new EncoderException(ioexception);
        }
    }

    /**
     * Writes an ItemStack to this buffer.
     *
     * @see #readItem
     */
    public FriendlyByteBuf writeItem(ItemStack pStack) {
        if (pStack.isEmpty()) {
            this.writeBoolean(false);
        } else {
            this.writeBoolean(true);
            Item item = pStack.getItem();
            this.writeId(BuiltInRegistries.ITEM, item);
            this.writeByte(pStack.getCount());
            CompoundTag compoundtag = null;
            if (item.isDamageable(pStack) || item.shouldOverrideMultiplayerNbt()) {
                compoundtag = pStack.getTag();
            }
            compoundtag = net.neoforged.neoforge.attachment.AttachmentInternals.addAttachmentsToTag(compoundtag, pStack, false);

            this.writeNbt(compoundtag);
        }

        return this;
    }

    /**
     * Reads an ItemStack from this buffer.
     *
     * @see #writeItem
     */
    public ItemStack readItem() {
        if (!this.readBoolean()) {
            return ItemStack.EMPTY;
        } else {
            Item item = this.readById(BuiltInRegistries.ITEM);
            int i = this.readByte();
            return net.neoforged.neoforge.attachment.AttachmentInternals.reconstructItemStack(item, i, this.readNbt());
        }
    }

    /**
     * Reads a String with a maximum length of {@code Short.MAX_VALUE}.
     *
     * @see #readUtf(int)
     * @see #writeUtf
     */
    public String readUtf() {
        return this.readUtf(32767);
    }

    /**
     * Reads a string with a maximum length from this buffer.
     *
     * @see #writeUtf
     */
    public String readUtf(int pMaxLength) {
        return Utf8String.read(this.source, pMaxLength);
    }

    /**
     * Writes a String with a maximum length of {@code Short.MAX_VALUE}.
     *
     * @see #readUtf
     */
    public FriendlyByteBuf writeUtf(String p_130071_) {
        return this.writeUtf(p_130071_, 32767);
    }

    /**
     * Writes a String with a maximum length.
     *
     * @see #readUtf
     */
    public FriendlyByteBuf writeUtf(String pString, int pMaxLength) {
        Utf8String.write(this.source, pString, pMaxLength);
        return this;
    }

    /**
     * Read a ResourceLocation using its String representation.
     *
     * @see #writeResourceLocation
     */
    public ResourceLocation readResourceLocation() {
        return new ResourceLocation(this.readUtf(32767));
    }

    /**
     * Write a ResourceLocation using its String representation.
     *
     * @see #readResourceLocation
     */
    public FriendlyByteBuf writeResourceLocation(ResourceLocation pResourceLocation) {
        this.writeUtf(pResourceLocation.toString());
        return this;
    }

    public <T> ResourceKey<T> readResourceKey(ResourceKey<? extends Registry<T>> pRegistryKey) {
        ResourceLocation resourcelocation = this.readResourceLocation();
        return ResourceKey.create(pRegistryKey, resourcelocation);
    }

    public void writeResourceKey(ResourceKey<?> pResourceKey) {
        this.writeResourceLocation(pResourceKey.location());
    }

    public <T> ResourceKey<? extends Registry<T>> readRegistryKey() {
        ResourceLocation resourcelocation = this.readResourceLocation();
        return ResourceKey.createRegistryKey(resourcelocation);
    }

    /**
     * Read a timestamp as milliseconds since the unix epoch.
     *
     * @see #writeDate
     */
    public Date readDate() {
        return new Date(this.readLong());
    }

    /**
     * Write a timestamp as milliseconds since the unix epoch.
     *
     * @see #readDate
     */
    public FriendlyByteBuf writeDate(Date pTime) {
        this.writeLong(pTime.getTime());
        return this;
    }

    public Instant readInstant() {
        return Instant.ofEpochMilli(this.readLong());
    }

    public void writeInstant(Instant pInstant) {
        this.writeLong(pInstant.toEpochMilli());
    }

    public PublicKey readPublicKey() {
        try {
            return Crypt.byteToPublicKey(this.readByteArray(512));
        } catch (CryptException cryptexception) {
            throw new DecoderException("Malformed public key bytes", cryptexception);
        }
    }

    public FriendlyByteBuf writePublicKey(PublicKey pPublicKey) {
        this.writeByteArray(pPublicKey.getEncoded());
        return this;
    }

    /**
     * Read a BlockHitResult.
     *
     * @see #writeBlockHitResult
     */
    public BlockHitResult readBlockHitResult() {
        BlockPos blockpos = this.readBlockPos();
        Direction direction = this.readEnum(Direction.class);
        float f = this.readFloat();
        float f1 = this.readFloat();
        float f2 = this.readFloat();
        boolean flag = this.readBoolean();
        return new BlockHitResult(
            new Vec3((double)blockpos.getX() + (double)f, (double)blockpos.getY() + (double)f1, (double)blockpos.getZ() + (double)f2),
            direction,
            blockpos,
            flag
        );
    }

    /**
     * Write a BlockHitResult.
     *
     * @see #readBlockHitResult
     */
    public void writeBlockHitResult(BlockHitResult pResult) {
        BlockPos blockpos = pResult.getBlockPos();
        this.writeBlockPos(blockpos);
        this.writeEnum(pResult.getDirection());
        Vec3 vec3 = pResult.getLocation();
        this.writeFloat((float)(vec3.x - (double)blockpos.getX()));
        this.writeFloat((float)(vec3.y - (double)blockpos.getY()));
        this.writeFloat((float)(vec3.z - (double)blockpos.getZ()));
        this.writeBoolean(pResult.isInside());
    }

    /**
     * Read a BitSet as a long[].
     *
     * @see #writeBitSet
     */
    public BitSet readBitSet() {
        return BitSet.valueOf(this.readLongArray());
    }

    /**
     * Write a BitSet as a long[].
     *
     * @see #readBitSet
     */
    public void writeBitSet(BitSet pBitSet) {
        this.writeLongArray(pBitSet.toLongArray());
    }

    public BitSet readFixedBitSet(int pSize) {
        byte[] abyte = new byte[Mth.positiveCeilDiv(pSize, 8)];
        this.readBytes(abyte);
        return BitSet.valueOf(abyte);
    }

    public void writeFixedBitSet(BitSet pBitSet, int pSize) {
        if (pBitSet.length() > pSize) {
            throw new EncoderException("BitSet is larger than expected size (" + pBitSet.length() + ">" + pSize + ")");
        } else {
            byte[] abyte = pBitSet.toByteArray();
            this.writeBytes(Arrays.copyOf(abyte, Mth.positiveCeilDiv(pSize, 8)));
        }
    }

    public GameProfile readGameProfile() {
        UUID uuid = this.readUUID();
        String s = this.readUtf(16);
        GameProfile gameprofile = new GameProfile(uuid, s);
        gameprofile.getProperties().putAll(this.readGameProfileProperties());
        return gameprofile;
    }

    public void writeGameProfile(GameProfile pGameProfile) {
        this.writeUUID(pGameProfile.getId());
        this.writeUtf(pGameProfile.getName());
        this.writeGameProfileProperties(pGameProfile.getProperties());
    }

    public PropertyMap readGameProfileProperties() {
        PropertyMap propertymap = new PropertyMap();
        this.readWithCount(p_293720_ -> {
            Property property = this.readProperty();
            propertymap.put(property.name(), property);
        });
        return propertymap;
    }

    public void writeGameProfileProperties(PropertyMap pGameProfileProperties) {
        this.writeCollection(pGameProfileProperties.values(), FriendlyByteBuf::writeProperty);
    }

    public Property readProperty() {
        String s = this.readUtf();
        String s1 = this.readUtf();
        String s2 = this.readNullable(FriendlyByteBuf::readUtf);
        return new Property(s, s1, s2);
    }

    public void writeProperty(Property p_236806_) {
        this.writeUtf(p_236806_.name());
        this.writeUtf(p_236806_.value());
        this.writeNullable(p_236806_.signature(), FriendlyByteBuf::writeUtf);
    }

    @Override
    public boolean isContiguous() {
        return this.source.isContiguous();
    }

    @Override
    public int maxFastWritableBytes() {
        return this.source.maxFastWritableBytes();
    }

    @Override
    public int capacity() {
        return this.source.capacity();
    }

    public FriendlyByteBuf capacity(int pNewCapacity) {
        this.source.capacity(pNewCapacity);
        return this;
    }

    @Override
    public int maxCapacity() {
        return this.source.maxCapacity();
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.source.alloc();
    }

    @Override
    public ByteOrder order() {
        return this.source.order();
    }

    @Override
    public ByteBuf order(ByteOrder pEndianness) {
        return this.source.order(pEndianness);
    }

    @Override
    public ByteBuf unwrap() {
        return this.source;
    }

    @Override
    public boolean isDirect() {
        return this.source.isDirect();
    }

    @Override
    public boolean isReadOnly() {
        return this.source.isReadOnly();
    }

    @Override
    public ByteBuf asReadOnly() {
        return this.source.asReadOnly();
    }

    @Override
    public int readerIndex() {
        return this.source.readerIndex();
    }

    public FriendlyByteBuf readerIndex(int pReaderIndex) {
        this.source.readerIndex(pReaderIndex);
        return this;
    }

    @Override
    public int writerIndex() {
        return this.source.writerIndex();
    }

    public FriendlyByteBuf writerIndex(int pWriterIndex) {
        this.source.writerIndex(pWriterIndex);
        return this;
    }

    public FriendlyByteBuf setIndex(int pReaderIndex, int pWriterIndex) {
        this.source.setIndex(pReaderIndex, pWriterIndex);
        return this;
    }

    @Override
    public int readableBytes() {
        return this.source.readableBytes();
    }

    @Override
    public int writableBytes() {
        return this.source.writableBytes();
    }

    @Override
    public int maxWritableBytes() {
        return this.source.maxWritableBytes();
    }

    @Override
    public boolean isReadable() {
        return this.source.isReadable();
    }

    @Override
    public boolean isReadable(int pSize) {
        return this.source.isReadable(pSize);
    }

    @Override
    public boolean isWritable() {
        return this.source.isWritable();
    }

    @Override
    public boolean isWritable(int pSize) {
        return this.source.isWritable(pSize);
    }

    public FriendlyByteBuf clear() {
        this.source.clear();
        return this;
    }

    public FriendlyByteBuf markReaderIndex() {
        this.source.markReaderIndex();
        return this;
    }

    public FriendlyByteBuf resetReaderIndex() {
        this.source.resetReaderIndex();
        return this;
    }

    public FriendlyByteBuf markWriterIndex() {
        this.source.markWriterIndex();
        return this;
    }

    public FriendlyByteBuf resetWriterIndex() {
        this.source.resetWriterIndex();
        return this;
    }

    public FriendlyByteBuf discardReadBytes() {
        this.source.discardReadBytes();
        return this;
    }

    public FriendlyByteBuf discardSomeReadBytes() {
        this.source.discardSomeReadBytes();
        return this;
    }

    public FriendlyByteBuf ensureWritable(int pSize) {
        this.source.ensureWritable(pSize);
        return this;
    }

    @Override
    public int ensureWritable(int pSize, boolean pForce) {
        return this.source.ensureWritable(pSize, pForce);
    }

    @Override
    public boolean getBoolean(int pIndex) {
        return this.source.getBoolean(pIndex);
    }

    @Override
    public byte getByte(int pIndex) {
        return this.source.getByte(pIndex);
    }

    @Override
    public short getUnsignedByte(int pIndex) {
        return this.source.getUnsignedByte(pIndex);
    }

    @Override
    public short getShort(int pIndex) {
        return this.source.getShort(pIndex);
    }

    @Override
    public short getShortLE(int pIndex) {
        return this.source.getShortLE(pIndex);
    }

    @Override
    public int getUnsignedShort(int pIndex) {
        return this.source.getUnsignedShort(pIndex);
    }

    @Override
    public int getUnsignedShortLE(int pIndex) {
        return this.source.getUnsignedShortLE(pIndex);
    }

    @Override
    public int getMedium(int pIndex) {
        return this.source.getMedium(pIndex);
    }

    @Override
    public int getMediumLE(int pIndex) {
        return this.source.getMediumLE(pIndex);
    }

    @Override
    public int getUnsignedMedium(int pIndex) {
        return this.source.getUnsignedMedium(pIndex);
    }

    @Override
    public int getUnsignedMediumLE(int pIndex) {
        return this.source.getUnsignedMediumLE(pIndex);
    }

    @Override
    public int getInt(int pIndex) {
        return this.source.getInt(pIndex);
    }

    @Override
    public int getIntLE(int pIndex) {
        return this.source.getIntLE(pIndex);
    }

    @Override
    public long getUnsignedInt(int pIndex) {
        return this.source.getUnsignedInt(pIndex);
    }

    @Override
    public long getUnsignedIntLE(int pIndex) {
        return this.source.getUnsignedIntLE(pIndex);
    }

    @Override
    public long getLong(int pIndex) {
        return this.source.getLong(pIndex);
    }

    @Override
    public long getLongLE(int pIndex) {
        return this.source.getLongLE(pIndex);
    }

    @Override
    public char getChar(int pIndex) {
        return this.source.getChar(pIndex);
    }

    @Override
    public float getFloat(int pIndex) {
        return this.source.getFloat(pIndex);
    }

    @Override
    public double getDouble(int pIndex) {
        return this.source.getDouble(pIndex);
    }

    public FriendlyByteBuf getBytes(int pIndex, ByteBuf pDestination) {
        this.source.getBytes(pIndex, pDestination);
        return this;
    }

    public FriendlyByteBuf getBytes(int pIndex, ByteBuf pDestination, int pLength) {
        this.source.getBytes(pIndex, pDestination, pLength);
        return this;
    }

    public FriendlyByteBuf getBytes(int pIndex, ByteBuf pDestination, int pDestinationIndex, int pLength) {
        this.source.getBytes(pIndex, pDestination, pDestinationIndex, pLength);
        return this;
    }

    public FriendlyByteBuf getBytes(int pIndex, byte[] pDestination) {
        this.source.getBytes(pIndex, pDestination);
        return this;
    }

    public FriendlyByteBuf getBytes(int pIndex, byte[] pDestination, int pDestinationIndex, int pLength) {
        this.source.getBytes(pIndex, pDestination, pDestinationIndex, pLength);
        return this;
    }

    public FriendlyByteBuf getBytes(int pIndex, ByteBuffer pDestination) {
        this.source.getBytes(pIndex, pDestination);
        return this;
    }

    public FriendlyByteBuf getBytes(int pIndex, OutputStream pOut, int pLength) throws IOException {
        this.source.getBytes(pIndex, pOut, pLength);
        return this;
    }

    @Override
    public int getBytes(int pIndex, GatheringByteChannel pOut, int pLength) throws IOException {
        return this.source.getBytes(pIndex, pOut, pLength);
    }

    @Override
    public int getBytes(int pIndex, FileChannel pOut, long pPosition, int pLength) throws IOException {
        return this.source.getBytes(pIndex, pOut, pPosition, pLength);
    }

    @Override
    public CharSequence getCharSequence(int pIndex, int pLength, Charset pCharset) {
        return this.source.getCharSequence(pIndex, pLength, pCharset);
    }

    public FriendlyByteBuf setBoolean(int pIndex, boolean pValue) {
        this.source.setBoolean(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setByte(int pIndex, int pValue) {
        this.source.setByte(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setShort(int pIndex, int pValue) {
        this.source.setShort(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setShortLE(int pIndex, int pValue) {
        this.source.setShortLE(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setMedium(int pIndex, int pValue) {
        this.source.setMedium(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setMediumLE(int pIndex, int pValue) {
        this.source.setMediumLE(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setInt(int pIndex, int pValue) {
        this.source.setInt(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setIntLE(int pIndex, int pValue) {
        this.source.setIntLE(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setLong(int pIndex, long pValue) {
        this.source.setLong(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setLongLE(int pIndex, long pValue) {
        this.source.setLongLE(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setChar(int pIndex, int pValue) {
        this.source.setChar(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setFloat(int pIndex, float pValue) {
        this.source.setFloat(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setDouble(int pIndex, double pValue) {
        this.source.setDouble(pIndex, pValue);
        return this;
    }

    public FriendlyByteBuf setBytes(int pIndex, ByteBuf pSource) {
        this.source.setBytes(pIndex, pSource);
        return this;
    }

    public FriendlyByteBuf setBytes(int pIndex, ByteBuf pSource, int pLength) {
        this.source.setBytes(pIndex, pSource, pLength);
        return this;
    }

    public FriendlyByteBuf setBytes(int pIndex, ByteBuf pSource, int pSourceIndex, int pLength) {
        this.source.setBytes(pIndex, pSource, pSourceIndex, pLength);
        return this;
    }

    public FriendlyByteBuf setBytes(int pIndex, byte[] pSource) {
        this.source.setBytes(pIndex, pSource);
        return this;
    }

    public FriendlyByteBuf setBytes(int pIndex, byte[] pSource, int pSourceIndex, int pLength) {
        this.source.setBytes(pIndex, pSource, pSourceIndex, pLength);
        return this;
    }

    public FriendlyByteBuf setBytes(int pIndex, ByteBuffer pSource) {
        this.source.setBytes(pIndex, pSource);
        return this;
    }

    @Override
    public int setBytes(int pIndex, InputStream pIn, int pLength) throws IOException {
        return this.source.setBytes(pIndex, pIn, pLength);
    }

    @Override
    public int setBytes(int pIndex, ScatteringByteChannel pIn, int pLength) throws IOException {
        return this.source.setBytes(pIndex, pIn, pLength);
    }

    @Override
    public int setBytes(int pIndex, FileChannel pIn, long pPosition, int pLength) throws IOException {
        return this.source.setBytes(pIndex, pIn, pPosition, pLength);
    }

    public FriendlyByteBuf setZero(int pIndex, int pLength) {
        this.source.setZero(pIndex, pLength);
        return this;
    }

    @Override
    public int setCharSequence(int pIndex, CharSequence pCharSequence, Charset pCharset) {
        return this.source.setCharSequence(pIndex, pCharSequence, pCharset);
    }

    @Override
    public boolean readBoolean() {
        return this.source.readBoolean();
    }

    @Override
    public byte readByte() {
        return this.source.readByte();
    }

    @Override
    public short readUnsignedByte() {
        return this.source.readUnsignedByte();
    }

    @Override
    public short readShort() {
        return this.source.readShort();
    }

    @Override
    public short readShortLE() {
        return this.source.readShortLE();
    }

    @Override
    public int readUnsignedShort() {
        return this.source.readUnsignedShort();
    }

    @Override
    public int readUnsignedShortLE() {
        return this.source.readUnsignedShortLE();
    }

    @Override
    public int readMedium() {
        return this.source.readMedium();
    }

    @Override
    public int readMediumLE() {
        return this.source.readMediumLE();
    }

    @Override
    public int readUnsignedMedium() {
        return this.source.readUnsignedMedium();
    }

    @Override
    public int readUnsignedMediumLE() {
        return this.source.readUnsignedMediumLE();
    }

    @Override
    public int readInt() {
        return this.source.readInt();
    }

    @Override
    public int readIntLE() {
        return this.source.readIntLE();
    }

    @Override
    public long readUnsignedInt() {
        return this.source.readUnsignedInt();
    }

    @Override
    public long readUnsignedIntLE() {
        return this.source.readUnsignedIntLE();
    }

    @Override
    public long readLong() {
        return this.source.readLong();
    }

    @Override
    public long readLongLE() {
        return this.source.readLongLE();
    }

    @Override
    public char readChar() {
        return this.source.readChar();
    }

    @Override
    public float readFloat() {
        return this.source.readFloat();
    }

    @Override
    public double readDouble() {
        return this.source.readDouble();
    }

    @Override
    public ByteBuf readBytes(int pLength) {
        return this.source.readBytes(pLength);
    }

    @Override
    public ByteBuf readSlice(int pLength) {
        return this.source.readSlice(pLength);
    }

    @Override
    public ByteBuf readRetainedSlice(int pLength) {
        return this.source.readRetainedSlice(pLength);
    }

    public FriendlyByteBuf readBytes(ByteBuf pDestination) {
        this.source.readBytes(pDestination);
        return this;
    }

    public FriendlyByteBuf readBytes(ByteBuf pDestination, int pLength) {
        this.source.readBytes(pDestination, pLength);
        return this;
    }

    public FriendlyByteBuf readBytes(ByteBuf pDestination, int pDestinationIndex, int pLength) {
        this.source.readBytes(pDestination, pDestinationIndex, pLength);
        return this;
    }

    public FriendlyByteBuf readBytes(byte[] pDestination) {
        this.source.readBytes(pDestination);
        return this;
    }

    public FriendlyByteBuf readBytes(byte[] pDestination, int pDestinationIndex, int pLength) {
        this.source.readBytes(pDestination, pDestinationIndex, pLength);
        return this;
    }

    public FriendlyByteBuf readBytes(ByteBuffer pDestination) {
        this.source.readBytes(pDestination);
        return this;
    }

    public FriendlyByteBuf readBytes(OutputStream pOut, int pLength) throws IOException {
        this.source.readBytes(pOut, pLength);
        return this;
    }

    @Override
    public int readBytes(GatheringByteChannel pOut, int pLength) throws IOException {
        return this.source.readBytes(pOut, pLength);
    }

    @Override
    public CharSequence readCharSequence(int pLength, Charset pCharset) {
        return this.source.readCharSequence(pLength, pCharset);
    }

    @Override
    public int readBytes(FileChannel pOut, long pPosition, int pLength) throws IOException {
        return this.source.readBytes(pOut, pPosition, pLength);
    }

    public FriendlyByteBuf skipBytes(int pLength) {
        this.source.skipBytes(pLength);
        return this;
    }

    public FriendlyByteBuf writeBoolean(boolean pValue) {
        this.source.writeBoolean(pValue);
        return this;
    }

    public FriendlyByteBuf writeByte(int pValue) {
        this.source.writeByte(pValue);
        return this;
    }

    public FriendlyByteBuf writeShort(int pValue) {
        this.source.writeShort(pValue);
        return this;
    }

    public FriendlyByteBuf writeShortLE(int pValue) {
        this.source.writeShortLE(pValue);
        return this;
    }

    public FriendlyByteBuf writeMedium(int pValue) {
        this.source.writeMedium(pValue);
        return this;
    }

    public FriendlyByteBuf writeMediumLE(int pValue) {
        this.source.writeMediumLE(pValue);
        return this;
    }

    public FriendlyByteBuf writeInt(int pValue) {
        this.source.writeInt(pValue);
        return this;
    }

    public FriendlyByteBuf writeIntLE(int pValue) {
        this.source.writeIntLE(pValue);
        return this;
    }

    public FriendlyByteBuf writeLong(long pValue) {
        this.source.writeLong(pValue);
        return this;
    }

    public FriendlyByteBuf writeLongLE(long pValue) {
        this.source.writeLongLE(pValue);
        return this;
    }

    public FriendlyByteBuf writeChar(int pValue) {
        this.source.writeChar(pValue);
        return this;
    }

    public FriendlyByteBuf writeFloat(float pValue) {
        this.source.writeFloat(pValue);
        return this;
    }

    public FriendlyByteBuf writeDouble(double pValue) {
        this.source.writeDouble(pValue);
        return this;
    }

    public FriendlyByteBuf writeBytes(ByteBuf pSource) {
        this.source.writeBytes(pSource);
        return this;
    }

    public FriendlyByteBuf writeBytes(ByteBuf pSource, int pLength) {
        this.source.writeBytes(pSource, pLength);
        return this;
    }

    public FriendlyByteBuf writeBytes(ByteBuf pSource, int pSourceIndex, int pLength) {
        this.source.writeBytes(pSource, pSourceIndex, pLength);
        return this;
    }

    public FriendlyByteBuf writeBytes(byte[] pSource) {
        this.source.writeBytes(pSource);
        return this;
    }

    public FriendlyByteBuf writeBytes(byte[] pSource, int pSourceIndex, int pLength) {
        this.source.writeBytes(pSource, pSourceIndex, pLength);
        return this;
    }

    public FriendlyByteBuf writeBytes(ByteBuffer pSource) {
        this.source.writeBytes(pSource);
        return this;
    }

    @Override
    public int writeBytes(InputStream pIn, int pLength) throws IOException {
        return this.source.writeBytes(pIn, pLength);
    }

    @Override
    public int writeBytes(ScatteringByteChannel pIn, int pLength) throws IOException {
        return this.source.writeBytes(pIn, pLength);
    }

    @Override
    public int writeBytes(FileChannel pIn, long pPosition, int pLength) throws IOException {
        return this.source.writeBytes(pIn, pPosition, pLength);
    }

    public FriendlyByteBuf writeZero(int pLength) {
        this.source.writeZero(pLength);
        return this;
    }

    @Override
    public int writeCharSequence(CharSequence pCharSequence, Charset pCharset) {
        return this.source.writeCharSequence(pCharSequence, pCharset);
    }

    @Override
    public int indexOf(int pFromIndex, int pToIndex, byte pValue) {
        return this.source.indexOf(pFromIndex, pToIndex, pValue);
    }

    @Override
    public int bytesBefore(byte pValue) {
        return this.source.bytesBefore(pValue);
    }

    @Override
    public int bytesBefore(int pLength, byte pValue) {
        return this.source.bytesBefore(pLength, pValue);
    }

    @Override
    public int bytesBefore(int pIndex, int pLength, byte pValue) {
        return this.source.bytesBefore(pIndex, pLength, pValue);
    }

    @Override
    public int forEachByte(ByteProcessor pProcessor) {
        return this.source.forEachByte(pProcessor);
    }

    @Override
    public int forEachByte(int pIndex, int pLength, ByteProcessor pProcessor) {
        return this.source.forEachByte(pIndex, pLength, pProcessor);
    }

    @Override
    public int forEachByteDesc(ByteProcessor pProcessor) {
        return this.source.forEachByteDesc(pProcessor);
    }

    @Override
    public int forEachByteDesc(int pIndex, int pLength, ByteProcessor pProcessor) {
        return this.source.forEachByteDesc(pIndex, pLength, pProcessor);
    }

    @Override
    public ByteBuf copy() {
        return this.source.copy();
    }

    @Override
    public ByteBuf copy(int pIndex, int pLength) {
        return this.source.copy(pIndex, pLength);
    }

    @Override
    public ByteBuf slice() {
        return this.source.slice();
    }

    @Override
    public ByteBuf retainedSlice() {
        return this.source.retainedSlice();
    }

    @Override
    public ByteBuf slice(int pIndex, int pLength) {
        return this.source.slice(pIndex, pLength);
    }

    @Override
    public ByteBuf retainedSlice(int pIndex, int pLength) {
        return this.source.retainedSlice(pIndex, pLength);
    }

    @Override
    public ByteBuf duplicate() {
        return this.source.duplicate();
    }

    @Override
    public ByteBuf retainedDuplicate() {
        return this.source.retainedDuplicate();
    }

    @Override
    public int nioBufferCount() {
        return this.source.nioBufferCount();
    }

    @Override
    public ByteBuffer nioBuffer() {
        return this.source.nioBuffer();
    }

    @Override
    public ByteBuffer nioBuffer(int pIndex, int pLength) {
        return this.source.nioBuffer(pIndex, pLength);
    }

    @Override
    public ByteBuffer internalNioBuffer(int pIndex, int pLength) {
        return this.source.internalNioBuffer(pIndex, pLength);
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return this.source.nioBuffers();
    }

    @Override
    public ByteBuffer[] nioBuffers(int pIndex, int pLength) {
        return this.source.nioBuffers(pIndex, pLength);
    }

    @Override
    public boolean hasArray() {
        return this.source.hasArray();
    }

    @Override
    public byte[] array() {
        return this.source.array();
    }

    @Override
    public int arrayOffset() {
        return this.source.arrayOffset();
    }

    @Override
    public boolean hasMemoryAddress() {
        return this.source.hasMemoryAddress();
    }

    @Override
    public long memoryAddress() {
        return this.source.memoryAddress();
    }

    @Override
    public String toString(Charset pCharset) {
        return this.source.toString(pCharset);
    }

    @Override
    public String toString(int pIndex, int pLength, Charset pCharset) {
        return this.source.toString(pIndex, pLength, pCharset);
    }

    @Override
    public int hashCode() {
        return this.source.hashCode();
    }

    @Override
    public boolean equals(Object pOther) {
        return this.source.equals(pOther);
    }

    @Override
    public int compareTo(ByteBuf pOther) {
        return this.source.compareTo(pOther);
    }

    @Override
    public String toString() {
        return this.source.toString();
    }

    public FriendlyByteBuf retain(int pIncrement) {
        this.source.retain(pIncrement);
        return this;
    }

    public FriendlyByteBuf retain() {
        this.source.retain();
        return this;
    }

    public FriendlyByteBuf touch() {
        this.source.touch();
        return this;
    }

    public FriendlyByteBuf touch(Object pHint) {
        this.source.touch(pHint);
        return this;
    }

    @Override
    public int refCnt() {
        return this.source.refCnt();
    }

    @Override
    public boolean release() {
        return this.source.release();
    }

    @Override
    public boolean release(int pDecrement) {
        return this.source.release(pDecrement);
    }

    @FunctionalInterface
    public interface Reader<T> extends Function<FriendlyByteBuf, T> {
        default FriendlyByteBuf.Reader<Optional<T>> asOptional() {
            return p_236878_ -> p_236878_.readOptional(this);
        }
    }

    @FunctionalInterface
    public interface Writer<T> extends BiConsumer<FriendlyByteBuf, T> {
        default FriendlyByteBuf.Writer<Optional<T>> asOptional() {
            return (p_236881_, p_236882_) -> p_236881_.writeOptional(p_236882_, this);
        }
    }
}
