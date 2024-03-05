package net.minecraft.network.protocol.login;

import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.ConnectionProtocol;

/**
 * PacketListener for the client side of the LOGIN protocol.
 */
public interface ClientLoginPacketListener extends ClientboundPacketListener {
    @Override
    default ConnectionProtocol protocol() {
        return ConnectionProtocol.LOGIN;
    }

    void handleHello(ClientboundHelloPacket pPacket);

    void handleGameProfile(ClientboundGameProfilePacket pPacket);

    void handleDisconnect(ClientboundLoginDisconnectPacket pPacket);

    void handleCompression(ClientboundLoginCompressionPacket pPacket);

    void handleCustomQuery(ClientboundCustomQueryPacket pPacket);
}
