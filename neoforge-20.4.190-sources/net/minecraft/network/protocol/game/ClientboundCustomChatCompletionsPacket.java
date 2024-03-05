package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundCustomChatCompletionsPacket(ClientboundCustomChatCompletionsPacket.Action action, List<String> entries)
    implements Packet<ClientGamePacketListener> {
    public ClientboundCustomChatCompletionsPacket(FriendlyByteBuf p_243340_) {
        this(p_243340_.readEnum(ClientboundCustomChatCompletionsPacket.Action.class), p_243340_.readList(FriendlyByteBuf::readUtf));
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeEnum(this.action);
        pBuffer.writeCollection(this.entries, FriendlyByteBuf::writeUtf);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleCustomChatCompletions(this);
    }

    public static enum Action {
        ADD,
        REMOVE,
        SET;
    }
}
