package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public record ClientboundSystemChatPacket(Component content, boolean overlay) implements Packet<ClientGamePacketListener> {
    public ClientboundSystemChatPacket(FriendlyByteBuf p_237852_) {
        this(p_237852_.readComponentTrusted(), p_237852_.readBoolean());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeComponent(this.content);
        pBuffer.writeBoolean(this.overlay);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSystemChat(this);
    }

    /**
     * Whether decoding errors will be ignored for this packet.
     */
    @Override
    public boolean isSkippable() {
        return true;
    }
}
