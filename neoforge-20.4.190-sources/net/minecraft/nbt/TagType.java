package net.minecraft.nbt;

import java.io.DataInput;
import java.io.IOException;

public interface TagType<T extends Tag> {
    T load(DataInput pInput, NbtAccounter pAccounter) throws IOException;

    StreamTagVisitor.ValueResult parse(DataInput pInput, StreamTagVisitor pVisitor, NbtAccounter pAccounter) throws IOException;

    default void parseRoot(DataInput pInput, StreamTagVisitor pVisitor, NbtAccounter pNbtAccounter) throws IOException {
        switch(pVisitor.visitRootEntry(this)) {
            case CONTINUE:
                this.parse(pInput, pVisitor, pNbtAccounter);
            case HALT:
            default:
                break;
            case BREAK:
                this.skip(pInput, pNbtAccounter);
        }
    }

    void skip(DataInput pInput, int pEntries, NbtAccounter pAccounter) throws IOException;

    void skip(DataInput pInput, NbtAccounter pAccounter) throws IOException;

    default boolean isValue() {
        return false;
    }

    String getName();

    String getPrettyName();

    static TagType<EndTag> createInvalid(final int pId) {
        return new TagType<EndTag>() {
            private IOException createException() {
                return new IOException("Invalid tag id: " + pId);
            }

            public EndTag load(DataInput p_129387_, NbtAccounter p_129389_) throws IOException {
                throw this.createException();
            }

            @Override
            public StreamTagVisitor.ValueResult parse(DataInput p_197589_, StreamTagVisitor p_197590_, NbtAccounter p_302377_) throws IOException {
                throw this.createException();
            }

            @Override
            public void skip(DataInput p_197586_, int p_197587_, NbtAccounter p_302342_) throws IOException {
                throw this.createException();
            }

            @Override
            public void skip(DataInput p_197584_, NbtAccounter p_302343_) throws IOException {
                throw this.createException();
            }

            @Override
            public String getName() {
                return "INVALID[" + pId + "]";
            }

            @Override
            public String getPrettyName() {
                return "UNKNOWN_" + pId;
            }
        };
    }

    public interface StaticSize<T extends Tag> extends TagType<T> {
        @Override
        default void skip(DataInput p_197595_, NbtAccounter p_302323_) throws IOException {
            p_197595_.skipBytes(this.size());
        }

        @Override
        default void skip(DataInput p_197597_, int p_197598_, NbtAccounter p_302393_) throws IOException {
            p_197597_.skipBytes(this.size() * p_197598_);
        }

        int size();
    }

    public interface VariableSize<T extends Tag> extends TagType<T> {
        @Override
        default void skip(DataInput p_197600_, int p_197601_, NbtAccounter p_302386_) throws IOException {
            for(int i = 0; i < p_197601_; ++i) {
                this.skip(p_197600_, p_302386_);
            }
        }
    }
}
