package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundDisconnectPacket implements Packet<ClientCommonPacketListener> {
    private final Component reason;

    public ClientboundDisconnectPacket(Component pReason) {
        this.reason = pReason;
    }

    public ClientboundDisconnectPacket(FriendlyByteBuf pBuffer) {
        this.reason = pBuffer.readComponentTrusted();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeComponent(this.reason);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientCommonPacketListener pHandler) {
        pHandler.handleDisconnect(this);
    }

    public Component getReason() {
        return this.reason;
    }
}
