package net.minecraft.network.protocol.status;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.game.ServerPacketListener;
import net.minecraft.network.protocol.game.ServerPingPacketListener;

/**
 * PacketListener for the server side of the STATUS protocol.
 */
public interface ServerStatusPacketListener extends ServerPacketListener, ServerPingPacketListener {
    @Override
    default ConnectionProtocol protocol() {
        return ConnectionProtocol.STATUS;
    }

    void handleStatusRequest(ServerboundStatusRequestPacket pPacket);
}
