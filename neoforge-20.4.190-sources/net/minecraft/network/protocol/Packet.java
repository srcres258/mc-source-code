package net.minecraft.network.protocol;

import javax.annotation.Nullable;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;

public interface Packet<T extends PacketListener> {
    /**
     * Writes the raw packet data to the data stream.
     */
    void write(FriendlyByteBuf pBuffer);

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    void handle(T pHandler);

    /**
     * Whether decoding errors will be ignored for this packet.
     */
    default boolean isSkippable() {
        return false;
    }

    @Nullable
    default ConnectionProtocol nextProtocol() {
        return null;
    }
}
