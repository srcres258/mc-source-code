package net.minecraft.network.protocol.common.custom;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record StructuresDebugPayload(ResourceKey<Level> dimension, BoundingBox mainBB, List<StructuresDebugPayload.PieceInfo> pieces)
    implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation("debug/structures");

    public StructuresDebugPayload(FriendlyByteBuf p_294983_) {
        this(p_294983_.readResourceKey(Registries.DIMENSION), readBoundingBox(p_294983_), p_294983_.readList(StructuresDebugPayload.PieceInfo::new));
    }

    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeResourceKey(this.dimension);
        writeBoundingBox(pBuffer, this.mainBB);
        pBuffer.writeCollection(this.pieces, (p_294583_, p_296047_) -> p_296047_.write(pBuffer));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }

    static BoundingBox readBoundingBox(FriendlyByteBuf pBuffer) {
        return new BoundingBox(pBuffer.readInt(), pBuffer.readInt(), pBuffer.readInt(), pBuffer.readInt(), pBuffer.readInt(), pBuffer.readInt());
    }

    static void writeBoundingBox(FriendlyByteBuf pBuffer, BoundingBox pBoundingBox) {
        pBuffer.writeInt(pBoundingBox.minX());
        pBuffer.writeInt(pBoundingBox.minY());
        pBuffer.writeInt(pBoundingBox.minZ());
        pBuffer.writeInt(pBoundingBox.maxX());
        pBuffer.writeInt(pBoundingBox.maxY());
        pBuffer.writeInt(pBoundingBox.maxZ());
    }

    public static record PieceInfo(BoundingBox boundingBox, boolean isStart) {
        public PieceInfo(FriendlyByteBuf p_294562_) {
            this(StructuresDebugPayload.readBoundingBox(p_294562_), p_294562_.readBoolean());
        }

        public void write(FriendlyByteBuf pBuffer) {
            StructuresDebugPayload.writeBoundingBox(pBuffer, this.boundingBox);
            pBuffer.writeBoolean(this.isStart);
        }
    }
}
