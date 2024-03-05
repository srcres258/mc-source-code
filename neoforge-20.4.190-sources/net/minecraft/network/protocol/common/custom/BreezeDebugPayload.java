package net.minecraft.network.protocol.common.custom;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.DebugEntityNameGenerator;
import net.minecraft.resources.ResourceLocation;

public record BreezeDebugPayload(BreezeDebugPayload.BreezeInfo breezeInfo) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation("debug/breeze");

    public BreezeDebugPayload(FriendlyByteBuf p_312069_) {
        this(new BreezeDebugPayload.BreezeInfo(p_312069_));
    }

    @Override
    public void write(FriendlyByteBuf pBuffer) {
        this.breezeInfo.write(pBuffer);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static record BreezeInfo(UUID uuid, int id, Integer attackTarget, BlockPos jumpTarget) {
        public BreezeInfo(FriendlyByteBuf p_311866_) {
            this(
                p_311866_.readUUID(),
                p_311866_.readInt(),
                p_311866_.readNullable(FriendlyByteBuf::readInt),
                p_311866_.readNullable(FriendlyByteBuf::readBlockPos)
            );
        }

        public void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeUUID(this.uuid);
            pBuffer.writeInt(this.id);
            pBuffer.writeNullable(this.attackTarget, FriendlyByteBuf::writeInt);
            pBuffer.writeNullable(this.jumpTarget, FriendlyByteBuf::writeBlockPos);
        }

        public String generateName() {
            return DebugEntityNameGenerator.getEntityName(this.uuid);
        }

        @Override
        public String toString() {
            return this.generateName();
        }
    }
}
