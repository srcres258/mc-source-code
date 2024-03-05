package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface CustomPacketPayload {
    void write(FriendlyByteBuf pBuffer);

    ResourceLocation id();
}
