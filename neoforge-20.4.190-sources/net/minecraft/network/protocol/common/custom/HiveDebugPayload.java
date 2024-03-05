package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record HiveDebugPayload(HiveDebugPayload.HiveInfo hiveInfo) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation("debug/hive");

    public HiveDebugPayload(FriendlyByteBuf p_296486_) {
        this(new HiveDebugPayload.HiveInfo(p_296486_));
    }

    @Override
    public void write(FriendlyByteBuf pBuffer) {
        this.hiveInfo.write(pBuffer);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    public static record HiveInfo(BlockPos pos, String hiveType, int occupantCount, int honeyLevel, boolean sedated) {
        public HiveInfo(FriendlyByteBuf p_295182_) {
            this(p_295182_.readBlockPos(), p_295182_.readUtf(), p_295182_.readInt(), p_295182_.readInt(), p_295182_.readBoolean());
        }

        public void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeBlockPos(this.pos);
            pBuffer.writeUtf(this.hiveType);
            pBuffer.writeInt(this.occupantCount);
            pBuffer.writeInt(this.honeyLevel);
            pBuffer.writeBoolean(this.sedated);
        }
    }
}
