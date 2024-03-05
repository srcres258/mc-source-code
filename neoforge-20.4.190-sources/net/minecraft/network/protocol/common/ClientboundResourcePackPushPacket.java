package net.minecraft.network.protocol.common;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public record ClientboundResourcePackPushPacket(UUID id, String url, String hash, boolean required, @Nullable Component prompt)
    implements Packet<ClientCommonPacketListener> {
    public static final int MAX_HASH_LENGTH = 40;

    public ClientboundResourcePackPushPacket(UUID id, String url, String hash, boolean required, @Nullable Component prompt) {
        if (hash.length() > 40) {
            throw new IllegalArgumentException("Hash is too long (max 40, was " + hash.length() + ")");
        } else {
            this.id = id;
            this.url = url;
            this.hash = hash;
            this.required = required;
            this.prompt = prompt;
        }
    }

    public ClientboundResourcePackPushPacket(FriendlyByteBuf p_314581_) {
        this(
            p_314581_.readUUID(),
            p_314581_.readUtf(),
            p_314581_.readUtf(40),
            p_314581_.readBoolean(),
            p_314581_.readNullable(FriendlyByteBuf::readComponentTrusted)
        );
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUUID(this.id);
        pBuffer.writeUtf(this.url);
        pBuffer.writeUtf(this.hash);
        pBuffer.writeBoolean(this.required);
        pBuffer.writeNullable(this.prompt, FriendlyByteBuf::writeComponent);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientCommonPacketListener pHandler) {
        pHandler.handleResourcePackPush(this);
    }
}
