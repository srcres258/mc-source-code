package net.minecraft.network.chat.numbers;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class BlankFormat implements NumberFormat {
    public static final BlankFormat INSTANCE = new BlankFormat();
    public static final NumberFormatType<BlankFormat> TYPE = new NumberFormatType<BlankFormat>() {
        private static final MapCodec<BlankFormat> CODEC = MapCodec.unit(BlankFormat.INSTANCE);

        @Override
        public MapCodec<BlankFormat> mapCodec() {
            return CODEC;
        }

        public void writeToStream(FriendlyByteBuf p_313784_, BlankFormat p_313756_) {
        }

        public BlankFormat readFromStream(FriendlyByteBuf p_313888_) {
            return BlankFormat.INSTANCE;
        }
    };

    @Override
    public MutableComponent format(int p_313861_) {
        return Component.empty();
    }

    @Override
    public NumberFormatType<BlankFormat> type() {
        return TYPE;
    }
}
