package net.minecraft.network.protocol.configuration;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public record ClientboundUpdateEnabledFeaturesPacket(Set<ResourceLocation> features) implements Packet<ClientConfigurationPacketListener> {
    public ClientboundUpdateEnabledFeaturesPacket(FriendlyByteBuf p_295459_) {
        this(p_295459_.<ResourceLocation, Set<ResourceLocation>>readCollection(HashSet::new, FriendlyByteBuf::readResourceLocation));
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeCollection(this.features, FriendlyByteBuf::writeResourceLocation);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientConfigurationPacketListener pHandler) {
        pHandler.handleEnabledFeatures(this);
    }
}
