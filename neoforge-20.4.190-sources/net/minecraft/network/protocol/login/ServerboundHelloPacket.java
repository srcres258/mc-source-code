package net.minecraft.network.protocol.login;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ServerboundHelloPacket(String name, UUID profileId) implements Packet<ServerLoginPacketListener> {
    public ServerboundHelloPacket(FriendlyByteBuf p_179827_) {
        this(p_179827_.readUtf(16), p_179827_.readUUID());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUtf(this.name, 16);
        pBuffer.writeUUID(this.profileId);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerLoginPacketListener pHandler) {
        pHandler.handleHello(this);
    }
}
