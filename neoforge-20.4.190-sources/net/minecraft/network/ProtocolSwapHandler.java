package net.minecraft.network;

import io.netty.util.Attribute;
import net.minecraft.network.protocol.Packet;

public interface ProtocolSwapHandler {
    static void swapProtocolIfNeeded(Attribute<ConnectionProtocol.CodecData<?>> pAttribute, Packet<?> pPacket) {
        ConnectionProtocol connectionprotocol = pPacket.nextProtocol();
        if (connectionprotocol != null) {
            ConnectionProtocol.CodecData<?> codecdata = pAttribute.get();
            ConnectionProtocol connectionprotocol1 = codecdata.protocol();
            if (connectionprotocol != connectionprotocol1) {
                ConnectionProtocol.CodecData<?> codecdata1 = connectionprotocol.codec(codecdata.flow());
                pAttribute.set(codecdata1);
            }
        }
    }
}
