package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class ByteTag extends NumericTag {
    private static final int SELF_SIZE_IN_BYTES = 9;
    public static final TagType<ByteTag> TYPE = new TagType.StaticSize<ByteTag>() {
        public ByteTag load(DataInput p_128297_, NbtAccounter p_128299_) throws IOException {
            return ByteTag.valueOf(readAccounted(p_128297_, p_128299_));
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197438_, StreamTagVisitor p_197439_, NbtAccounter p_302383_) throws IOException {
            return p_197439_.visit(readAccounted(p_197438_, p_302383_));
        }

        private static byte readAccounted(DataInput p_302348_, NbtAccounter p_302326_) throws IOException {
            p_302326_.accountBytes(9L);
            return p_302348_.readByte();
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public String getName() {
            return "BYTE";
        }

        @Override
        public String getPrettyName() {
            return "TAG_Byte";
        }

        @Override
        public boolean isValue() {
            return true;
        }
    };
    public static final ByteTag ZERO = valueOf((byte)0);
    public static final ByteTag ONE = valueOf((byte)1);
    private final byte data;

    ByteTag(byte pData) {
        this.data = pData;
    }

    public static ByteTag valueOf(byte pData) {
        return ByteTag.Cache.cache[128 + pData];
    }

    public static ByteTag valueOf(boolean pData) {
        return pData ? ONE : ZERO;
    }

    @Override
    public void write(DataOutput pOutput) throws IOException {
        pOutput.writeByte(this.data);
    }

    @Override
    public int sizeInBytes() {
        return 9;
    }

    @Override
    public byte getId() {
        return 1;
    }

    @Override
    public TagType<ByteTag> getType() {
        return TYPE;
    }

    /**
     * Creates a deep copy of the value held by this tag. Primitive and string tage will return the same tag instance while all other objects will return a new tag instance with the copied data.
     */
    public ByteTag copy() {
        return this;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return pOther instanceof ByteTag && this.data == ((ByteTag)pOther).data;
        }
    }

    @Override
    public int hashCode() {
        return this.data;
    }

    @Override
    public void accept(TagVisitor pVisitor) {
        pVisitor.visitByte(this);
    }

    @Override
    public long getAsLong() {
        return (long)this.data;
    }

    @Override
    public int getAsInt() {
        return this.data;
    }

    @Override
    public short getAsShort() {
        return (short)this.data;
    }

    @Override
    public byte getAsByte() {
        return this.data;
    }

    @Override
    public double getAsDouble() {
        return (double)this.data;
    }

    @Override
    public float getAsFloat() {
        return (float)this.data;
    }

    @Override
    public Number getAsNumber() {
        return this.data;
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor pVisitor) {
        return pVisitor.visit(this.data);
    }

    static class Cache {
        static final ByteTag[] cache = new ByteTag[256];

        private Cache() {
        }

        static {
            for(int i = 0; i < cache.length; ++i) {
                cache[i] = new ByteTag((byte)(i - 128));
            }
        }
    }
}
