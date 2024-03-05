package net.minecraft.network.protocol;

/**
 * The direction of packets.
 */
public enum PacketFlow implements net.neoforged.neoforge.common.extensions.IPacketFlowExtension {
    SERVERBOUND,
    CLIENTBOUND;

    public PacketFlow getOpposite() {
        return this == CLIENTBOUND ? SERVERBOUND : CLIENTBOUND;
    }
}
