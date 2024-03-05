package net.minecraft.network.chat.numbers;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class StyledFormat implements NumberFormat {
    public static final NumberFormatType<StyledFormat> TYPE = new NumberFormatType<StyledFormat>() {
        private static final MapCodec<StyledFormat> CODEC = Style.Serializer.MAP_CODEC.xmap(StyledFormat::new, p_313795_ -> p_313795_.style);

        @Override
        public MapCodec<StyledFormat> mapCodec() {
            return CODEC;
        }

        public void writeToStream(FriendlyByteBuf p_313941_, StyledFormat p_313775_) {
            p_313941_.writeWithCodec(NbtOps.INSTANCE, Style.Serializer.CODEC, p_313775_.style);
        }

        public StyledFormat readFromStream(FriendlyByteBuf p_313952_) {
            Style style = p_313952_.readWithCodecTrusted(NbtOps.INSTANCE, Style.Serializer.CODEC);
            return new StyledFormat(style);
        }
    };
    public static final StyledFormat NO_STYLE = new StyledFormat(Style.EMPTY);
    public static final StyledFormat SIDEBAR_DEFAULT = new StyledFormat(Style.EMPTY.withColor(ChatFormatting.RED));
    public static final StyledFormat PLAYER_LIST_DEFAULT = new StyledFormat(Style.EMPTY.withColor(ChatFormatting.YELLOW));
    final Style style;

    public StyledFormat(Style pStyle) {
        this.style = pStyle;
    }

    @Override
    public MutableComponent format(int pNumber) {
        return Component.literal(Integer.toString(pNumber)).withStyle(this.style);
    }

    @Override
    public NumberFormatType<StyledFormat> type() {
        return TYPE;
    }
}
