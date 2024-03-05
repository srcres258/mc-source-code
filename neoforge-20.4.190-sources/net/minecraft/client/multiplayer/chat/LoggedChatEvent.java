package net.minecraft.client.multiplayer.chat;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface LoggedChatEvent {
    Codec<LoggedChatEvent> CODEC = StringRepresentable.fromEnum(LoggedChatEvent.Type::values).dispatch(LoggedChatEvent::type, LoggedChatEvent.Type::codec);

    LoggedChatEvent.Type type();

    @OnlyIn(Dist.CLIENT)
    public static enum Type implements StringRepresentable {
        PLAYER("player", () -> LoggedChatMessage.Player.CODEC),
        SYSTEM("system", () -> LoggedChatMessage.System.CODEC);

        private final String serializedName;
        private final Supplier<Codec<? extends LoggedChatEvent>> codec;

        private Type(String pSerializedName, Supplier<Codec<? extends LoggedChatEvent>> pCodec) {
            this.serializedName = pSerializedName;
            this.codec = pCodec;
        }

        private Codec<? extends LoggedChatEvent> codec() {
            return this.codec.get();
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }
    }
}
