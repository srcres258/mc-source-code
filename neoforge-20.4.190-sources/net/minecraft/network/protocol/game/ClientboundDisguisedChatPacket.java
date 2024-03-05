package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public record ClientboundDisguisedChatPacket(Component message, ChatType.BoundNetwork chatType) implements Packet<ClientGamePacketListener> {
    public ClientboundDisguisedChatPacket(FriendlyByteBuf p_249018_) {
        this(p_249018_.readComponentTrusted(), new ChatType.BoundNetwork(p_249018_));
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeComponent(this.message);
        this.chatType.write(pBuffer);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleDisguisedChat(this);
    }

    /**
     * Whether decoding errors will be ignored for this packet.
     */
    @Override
    public boolean isSkippable() {
        return true;
    }
}
