package net.minecraft.network;

import io.netty.buffer.ByteBuf;

public class VarInt {
    private static final int MAX_VARINT_SIZE = 5;
    private static final int DATA_BITS_MASK = 127;
    private static final int CONTINUATION_BIT_MASK = 128;
    private static final int DATA_BITS_PER_BYTE = 7;

    public static int getByteSize(int pData) {
        for(int i = 1; i < 5; ++i) {
            if ((pData & -1 << i * 7) == 0) {
                return i;
            }
        }

        return 5;
    }

    public static boolean hasContinuationBit(byte pData) {
        return (pData & 128) == 128;
    }

    public static int read(ByteBuf pBuffer) {
        int i = 0;
        int j = 0;

        byte b0;
        do {
            b0 = pBuffer.readByte();
            i |= (b0 & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while(hasContinuationBit(b0));

        return i;
    }

    public static ByteBuf write(ByteBuf pBuffer, int pValue) {
        while((pValue & -128) != 0) {
            pBuffer.writeByte(pValue & 127 | 128);
            pValue >>>= 7;
        }

        pBuffer.writeByte(pValue);
        return pBuffer;
    }
}
