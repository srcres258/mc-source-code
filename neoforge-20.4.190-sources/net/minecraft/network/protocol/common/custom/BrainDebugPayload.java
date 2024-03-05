package net.minecraft.network.protocol.common.custom;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public record BrainDebugPayload(BrainDebugPayload.BrainDump brainDump) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation("debug/brain");

    public BrainDebugPayload(FriendlyByteBuf p_295683_) {
        this(new BrainDebugPayload.BrainDump(p_295683_));
    }

    @Override
    public void write(FriendlyByteBuf pBuffer) {
        this.brainDump.write(pBuffer);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static record BrainDump(
        UUID uuid,
        int id,
        String name,
        String profession,
        int xp,
        float health,
        float maxHealth,
        Vec3 pos,
        String inventory,
        @Nullable Path path,
        boolean wantsGolem,
        int angerLevel,
        List<String> activities,
        List<String> behaviors,
        List<String> memories,
        List<String> gossips,
        Set<BlockPos> pois,
        Set<BlockPos> potentialPois
    ) {
        public BrainDump(FriendlyByteBuf p_294290_) {
            this(
                p_294290_.readUUID(),
                p_294290_.readInt(),
                p_294290_.readUtf(),
                p_294290_.readUtf(),
                p_294290_.readInt(),
                p_294290_.readFloat(),
                p_294290_.readFloat(),
                p_294290_.readVec3(),
                p_294290_.readUtf(),
                p_294290_.readNullable(Path::createFromStream),
                p_294290_.readBoolean(),
                p_294290_.readInt(),
                p_294290_.readList(FriendlyByteBuf::readUtf),
                p_294290_.readList(FriendlyByteBuf::readUtf),
                p_294290_.readList(FriendlyByteBuf::readUtf),
                p_294290_.readList(FriendlyByteBuf::readUtf),
                p_294290_.readCollection(HashSet::new, FriendlyByteBuf::readBlockPos),
                p_294290_.readCollection(HashSet::new, FriendlyByteBuf::readBlockPos)
            );
        }

        public void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeUUID(this.uuid);
            pBuffer.writeInt(this.id);
            pBuffer.writeUtf(this.name);
            pBuffer.writeUtf(this.profession);
            pBuffer.writeInt(this.xp);
            pBuffer.writeFloat(this.health);
            pBuffer.writeFloat(this.maxHealth);
            pBuffer.writeVec3(this.pos);
            pBuffer.writeUtf(this.inventory);
            pBuffer.writeNullable(this.path, (p_296121_, p_295181_) -> p_295181_.writeToStream(p_296121_));
            pBuffer.writeBoolean(this.wantsGolem);
            pBuffer.writeInt(this.angerLevel);
            pBuffer.writeCollection(this.activities, FriendlyByteBuf::writeUtf);
            pBuffer.writeCollection(this.behaviors, FriendlyByteBuf::writeUtf);
            pBuffer.writeCollection(this.memories, FriendlyByteBuf::writeUtf);
            pBuffer.writeCollection(this.gossips, FriendlyByteBuf::writeUtf);
            pBuffer.writeCollection(this.pois, FriendlyByteBuf::writeBlockPos);
            pBuffer.writeCollection(this.potentialPois, FriendlyByteBuf::writeBlockPos);
        }

        public boolean hasPoi(BlockPos pPos) {
            return this.pois.contains(pPos);
        }

        public boolean hasPotentialPoi(BlockPos pPos) {
            return this.potentialPois.contains(pPos);
        }
    }
}
