package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundServerDataPacket implements Packet<ClientGamePacketListener> {
    private final Component motd;
    private final Optional<byte[]> iconBytes;
    private final boolean enforcesSecureChat;

    public ClientboundServerDataPacket(Component pMotd, Optional<byte[]> pIconBytes, boolean pEnforcesSecureChat) {
        this.motd = pMotd;
        this.iconBytes = pIconBytes;
        this.enforcesSecureChat = pEnforcesSecureChat;
    }

    public ClientboundServerDataPacket(FriendlyByteBuf pBuffer) {
        this.motd = pBuffer.readComponentTrusted();
        this.iconBytes = pBuffer.readOptional(FriendlyByteBuf::readByteArray);
        this.enforcesSecureChat = pBuffer.readBoolean();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeComponent(this.motd);
        pBuffer.writeOptional(this.iconBytes, FriendlyByteBuf::writeByteArray);
        pBuffer.writeBoolean(this.enforcesSecureChat);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleServerData(this);
    }

    public Component getMotd() {
        return this.motd;
    }

    public Optional<byte[]> getIconBytes() {
        return this.iconBytes;
    }

    public boolean enforcesSecureChat() {
        return this.enforcesSecureChat;
    }
}
