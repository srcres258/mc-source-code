package net.minecraft.network.protocol.configuration;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.RegistryOps;

public record ClientboundRegistryDataPacket(RegistryAccess.Frozen registryHolder) implements Packet<ClientConfigurationPacketListener> {
    private static final RegistryOps<Tag> BUILTIN_CONTEXT_OPS = RegistryOps.create(
        NbtOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
    );

    public ClientboundRegistryDataPacket(FriendlyByteBuf p_295827_) {
        this(p_295827_.readWithCodecTrusted(BUILTIN_CONTEXT_OPS, RegistrySynchronization.NETWORK_CODEC).freeze());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeWithCodec(BUILTIN_CONTEXT_OPS, RegistrySynchronization.NETWORK_CODEC, this.registryHolder);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientConfigurationPacketListener pHandler) {
        pHandler.handleRegistryData(this);
    }
}
