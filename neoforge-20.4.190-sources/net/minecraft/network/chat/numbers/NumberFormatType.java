package net.minecraft.network.chat.numbers;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;

public interface NumberFormatType<T extends NumberFormat> {
    MapCodec<T> mapCodec();

    void writeToStream(FriendlyByteBuf pBuffer, T pFormat);

    T readFromStream(FriendlyByteBuf pBuffer);
}
