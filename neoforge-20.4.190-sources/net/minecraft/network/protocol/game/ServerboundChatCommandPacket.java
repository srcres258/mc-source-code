package net.minecraft.network.protocol.game;

import java.time.Instant;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.protocol.Packet;

public record ServerboundChatCommandPacket(
    String command, Instant timeStamp, long salt, ArgumentSignatures argumentSignatures, LastSeenMessages.Update lastSeenMessages
) implements Packet<ServerGamePacketListener> {
    public ServerboundChatCommandPacket(FriendlyByteBuf p_237932_) {
        this(p_237932_.readUtf(256), p_237932_.readInstant(), p_237932_.readLong(), new ArgumentSignatures(p_237932_), new LastSeenMessages.Update(p_237932_));
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUtf(this.command, 256);
        pBuffer.writeInstant(this.timeStamp);
        pBuffer.writeLong(this.salt);
        this.argumentSignatures.write(pBuffer);
        this.lastSeenMessages.write(pBuffer);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleChatCommand(this);
    }
}
