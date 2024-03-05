package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import net.minecraft.network.protocol.Packet;

public record ClientboundSetScorePacket(String owner, String objectiveName, int score, @Nullable Component display, @Nullable NumberFormat numberFormat)
    implements Packet<ClientGamePacketListener> {
    public ClientboundSetScorePacket(FriendlyByteBuf p_179373_) {
        this(
            p_179373_.readUtf(),
            p_179373_.readUtf(),
            p_179373_.readVarInt(),
            p_179373_.readNullable(FriendlyByteBuf::readComponentTrusted),
            p_179373_.readNullable(NumberFormatTypes::readFromStream)
        );
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUtf(this.owner);
        pBuffer.writeUtf(this.objectiveName);
        pBuffer.writeVarInt(this.score);
        pBuffer.writeNullable(this.display, FriendlyByteBuf::writeComponent);
        pBuffer.writeNullable(this.numberFormat, NumberFormatTypes::writeToStream);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetScore(this);
    }
}
