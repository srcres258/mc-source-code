package net.minecraft.network.protocol.game;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.protocol.Packet;

public record ClientboundPlayerChatPacket(
    UUID sender,
    int index,
    @Nullable MessageSignature signature,
    SignedMessageBody.Packed body,
    @Nullable Component unsignedContent,
    FilterMask filterMask,
    ChatType.BoundNetwork chatType
) implements Packet<ClientGamePacketListener> {
    public ClientboundPlayerChatPacket(FriendlyByteBuf p_237741_) {
        this(
            p_237741_.readUUID(),
            p_237741_.readVarInt(),
            p_237741_.readNullable(MessageSignature::read),
            new SignedMessageBody.Packed(p_237741_),
            p_237741_.readNullable(FriendlyByteBuf::readComponentTrusted),
            FilterMask.read(p_237741_),
            new ChatType.BoundNetwork(p_237741_)
        );
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUUID(this.sender);
        pBuffer.writeVarInt(this.index);
        pBuffer.writeNullable(this.signature, MessageSignature::write);
        this.body.write(pBuffer);
        pBuffer.writeNullable(this.unsignedContent, FriendlyByteBuf::writeComponent);
        FilterMask.write(pBuffer, this.filterMask);
        this.chatType.write(pBuffer);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handlePlayerChat(this);
    }

    /**
     * Whether decoding errors will be ignored for this packet.
     */
    @Override
    public boolean isSkippable() {
        return true;
    }
}
