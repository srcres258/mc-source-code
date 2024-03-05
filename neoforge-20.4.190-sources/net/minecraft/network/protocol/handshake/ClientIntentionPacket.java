package net.minecraft.network.protocol.handshake;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientIntentionPacket(int protocolVersion, String hostName, int port, ClientIntent intention) implements Packet<ServerHandshakePacketListener> {
    private static final int MAX_HOST_LENGTH = 255;

    @Deprecated
    public ClientIntentionPacket(int protocolVersion, String hostName, int port, ClientIntent intention) {
        this.protocolVersion = protocolVersion;
        this.hostName = hostName;
        this.port = port;
        this.intention = intention;
    }

    public ClientIntentionPacket(FriendlyByteBuf p_179801_) {
        this(p_179801_.readVarInt(), p_179801_.readUtf(255), p_179801_.readUnsignedShort(), ClientIntent.byId(p_179801_.readVarInt()));
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.protocolVersion);
        pBuffer.writeUtf(this.hostName);
        pBuffer.writeShort(this.port);
        pBuffer.writeVarInt(this.intention.id());
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerHandshakePacketListener pHandler) {
        pHandler.handleIntention(this);
    }

    @Override
    public ConnectionProtocol nextProtocol() {
        return this.intention.protocol();
    }
}
