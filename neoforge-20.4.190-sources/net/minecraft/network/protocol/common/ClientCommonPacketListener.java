package net.minecraft.network.protocol.common;

import net.minecraft.network.ClientboundPacketListener;

public interface ClientCommonPacketListener extends ClientboundPacketListener, net.neoforged.neoforge.common.extensions.IClientCommonPacketListenerExtension {
    void handleKeepAlive(ClientboundKeepAlivePacket pPacket);

    void handlePing(ClientboundPingPacket pPacket);

    void handleCustomPayload(ClientboundCustomPayloadPacket pPacket);

    void handleDisconnect(ClientboundDisconnectPacket pPacket);

    void handleResourcePackPush(ClientboundResourcePackPushPacket pPacket);

    void handleResourcePackPop(ClientboundResourcePackPopPacket pPacket);

    void handleUpdateTags(ClientboundUpdateTagsPacket pPacket);
}
