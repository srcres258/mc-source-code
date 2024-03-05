package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;

public class ClientboundUpdateAdvancementsPacket implements Packet<ClientGamePacketListener> {
    private final boolean reset;
    private final List<AdvancementHolder> added;
    private final Set<ResourceLocation> removed;
    private final Map<ResourceLocation, AdvancementProgress> progress;

    public ClientboundUpdateAdvancementsPacket(
        boolean pReset, Collection<AdvancementHolder> pAdded, Set<ResourceLocation> pRemoved, Map<ResourceLocation, AdvancementProgress> pProgress
    ) {
        this.reset = pReset;
        this.added = List.copyOf(pAdded);
        this.removed = Set.copyOf(pRemoved);
        this.progress = Map.copyOf(pProgress);
    }

    public ClientboundUpdateAdvancementsPacket(FriendlyByteBuf pBuffer) {
        this.reset = pBuffer.readBoolean();
        this.added = pBuffer.readList(AdvancementHolder::read);
        this.removed = pBuffer.readCollection(Sets::newLinkedHashSetWithExpectedSize, FriendlyByteBuf::readResourceLocation);
        this.progress = pBuffer.readMap(FriendlyByteBuf::readResourceLocation, AdvancementProgress::fromNetwork);
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeBoolean(this.reset);
        pBuffer.writeCollection(this.added, (p_300719_, p_300720_) -> p_300720_.write(p_300719_));
        pBuffer.writeCollection(this.removed, FriendlyByteBuf::writeResourceLocation);
        pBuffer.writeMap(this.progress, FriendlyByteBuf::writeResourceLocation, (p_179444_, p_179445_) -> p_179445_.serializeToNetwork(p_179444_));
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleUpdateAdvancementsPacket(this);
    }

    public List<AdvancementHolder> getAdded() {
        return this.added;
    }

    public Set<ResourceLocation> getRemoved() {
        return this.removed;
    }

    public Map<ResourceLocation, AdvancementProgress> getProgress() {
        return this.progress;
    }

    public boolean shouldReset() {
        return this.reset;
    }
}
