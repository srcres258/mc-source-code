package net.minecraft.network.protocol.login;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryAnswerPayload;

public record ServerboundCustomQueryAnswerPacket(int transactionId, @Nullable CustomQueryAnswerPayload payload) implements Packet<ServerLoginPacketListener> {
    private static final int MAX_PAYLOAD_SIZE = 1048576;

    public static ServerboundCustomQueryAnswerPacket read(FriendlyByteBuf pBuffer) {
        int i = pBuffer.readVarInt();
        return new ServerboundCustomQueryAnswerPacket(i, readPayload(i, pBuffer));
    }

    private static CustomQueryAnswerPayload readPayload(int pTransactionId, FriendlyByteBuf pBuffer) {
        return readUnknownPayload(pBuffer);
    }

    private static CustomQueryAnswerPayload readUnknownPayload(FriendlyByteBuf pBuffer) {
        int i = pBuffer.readableBytes();
        if (i >= 0 && i <= 1048576) {
            pBuffer.skipBytes(i);
            return DiscardedQueryAnswerPayload.INSTANCE;
        } else {
            throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.transactionId);
        pBuffer.writeNullable(this.payload, (p_295443_, p_295588_) -> p_295588_.write(p_295443_));
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ServerLoginPacketListener pHandler) {
        pHandler.handleCustomQueryPacket(this);
    }
}
