package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundTabListPacket implements Packet<ClientGamePacketListener> {
    private final Component header;
    private final Component footer;

    public ClientboundTabListPacket(Component pHeader, Component pFooter) {
        this.header = pHeader;
        this.footer = pFooter;
    }

    public ClientboundTabListPacket(FriendlyByteBuf pBuffer) {
        this.header = pBuffer.readComponentTrusted();
        this.footer = pBuffer.readComponentTrusted();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeComponent(this.header);
        pBuffer.writeComponent(this.footer);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleTabListCustomisation(this);
    }

    public Component getHeader() {
        return this.header;
    }

    public Component getFooter() {
        return this.footer;
    }
}
