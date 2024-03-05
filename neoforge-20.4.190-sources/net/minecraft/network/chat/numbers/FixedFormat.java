package net.minecraft.network.chat.numbers;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;

public class FixedFormat implements NumberFormat {
    public static final NumberFormatType<FixedFormat> TYPE = new NumberFormatType<FixedFormat>() {
        private static final MapCodec<FixedFormat> CODEC = ComponentSerialization.CODEC.fieldOf("value").xmap(FixedFormat::new, p_313699_ -> p_313699_.value);

        @Override
        public MapCodec<FixedFormat> mapCodec() {
            return CODEC;
        }

        public void writeToStream(FriendlyByteBuf p_313925_, FixedFormat p_313884_) {
            p_313925_.writeComponent(p_313884_.value);
        }

        public FixedFormat readFromStream(FriendlyByteBuf p_313701_) {
            Component component = p_313701_.readComponentTrusted();
            return new FixedFormat(component);
        }
    };
    final Component value;

    public FixedFormat(Component pValue) {
        this.value = pValue;
    }

    @Override
    public MutableComponent format(int pNumber) {
        return this.value.copy();
    }

    @Override
    public NumberFormatType<FixedFormat> type() {
        return TYPE;
    }
}
