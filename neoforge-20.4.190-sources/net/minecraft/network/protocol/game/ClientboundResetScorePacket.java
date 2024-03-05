package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public record ClientboundResetScorePacket(String owner, @Nullable String objectiveName) implements Packet<ClientGamePacketListener> {
    public ClientboundResetScorePacket(FriendlyByteBuf p_313852_) {
        this(p_313852_.readUtf(), p_313852_.readNullable(FriendlyByteBuf::readUtf));
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUtf(this.owner);
        pBuffer.writeNullable(this.objectiveName, FriendlyByteBuf::writeUtf);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleResetScore(this);
    }
}
