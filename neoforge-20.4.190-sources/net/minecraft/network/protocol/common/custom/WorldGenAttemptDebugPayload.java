package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record WorldGenAttemptDebugPayload(BlockPos pos, float scale, float red, float green, float blue, float alpha) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation("debug/worldgen_attempt");

    public WorldGenAttemptDebugPayload(FriendlyByteBuf p_295574_) {
        this(p_295574_.readBlockPos(), p_295574_.readFloat(), p_295574_.readFloat(), p_295574_.readFloat(), p_295574_.readFloat(), p_295574_.readFloat());
    }

    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeBlockPos(this.pos);
        pBuffer.writeFloat(this.scale);
        pBuffer.writeFloat(this.red);
        pBuffer.writeFloat(this.green);
        pBuffer.writeFloat(this.blue);
        pBuffer.writeFloat(this.alpha);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
