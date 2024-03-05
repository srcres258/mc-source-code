package net.minecraft.network.chat.numbers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

public class NumberFormatTypes {
    public static final MapCodec<NumberFormat> MAP_CODEC = BuiltInRegistries.NUMBER_FORMAT_TYPE
        .byNameCodec()
        .dispatchMap(NumberFormat::type, p_313782_ -> p_313782_.mapCodec().codec());
    public static final Codec<NumberFormat> CODEC = MAP_CODEC.codec();

    public static NumberFormatType<?> bootstrap(Registry<NumberFormatType<?>> pRegsitry) {
        NumberFormatType<?> numberformattype = Registry.register(pRegsitry, "blank", BlankFormat.TYPE);
        Registry.register(pRegsitry, "styled", StyledFormat.TYPE);
        Registry.register(pRegsitry, "fixed", FixedFormat.TYPE);
        return numberformattype;
    }

    public static <T extends NumberFormat> void writeToStream(FriendlyByteBuf pBuffer, T pValue) {
        NumberFormatType<T> numberformattype = (NumberFormatType<T>)pValue.type();
        pBuffer.writeId(BuiltInRegistries.NUMBER_FORMAT_TYPE, numberformattype);
        numberformattype.writeToStream(pBuffer, pValue);
    }

    public static NumberFormat readFromStream(FriendlyByteBuf pBuffer) {
        NumberFormatType<?> numberformattype = pBuffer.readById(BuiltInRegistries.NUMBER_FORMAT_TYPE);
        return numberformattype.readFromStream(pBuffer);
    }
}
