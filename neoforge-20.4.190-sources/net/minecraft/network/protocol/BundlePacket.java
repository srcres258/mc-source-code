package net.minecraft.network.protocol;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;

public abstract class BundlePacket<T extends PacketListener> implements Packet<T> {
    private final Iterable<Packet<? super T>> packets;

    protected BundlePacket(Iterable<Packet<? super T>> pPackets) {
        this.packets = net.neoforged.neoforge.network.bundle.BundlePacketUtils.flatten(pPackets);
    }

    public final Iterable<Packet<? super T>> subPackets() {
        return this.packets;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public final void write(FriendlyByteBuf pBuffer) {
    }
}
