package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;

public class ServerboundSetJigsawBlockPacket implements Packet<ServerGamePacketListener> {
    private final BlockPos pos;
    private final ResourceLocation name;
    private final ResourceLocation target;
    private final ResourceLocation pool;
    private final String finalState;
    private final JigsawBlockEntity.JointType joint;
    private final int selectionPriority;
    private final int placementPriority;

    public ServerboundSetJigsawBlockPacket(
        BlockPos pPos,
        ResourceLocation pName,
        ResourceLocation pTarget,
        ResourceLocation pPool,
        String pFinalState,
        JigsawBlockEntity.JointType pJoint,
        int pSelectionPriority,
        int pPlacementPriority
    ) {
        this.pos = pPos;
        this.name = pName;
        this.target = pTarget;
        this.pool = pPool;
        this.finalState = pFinalState;
        this.joint = pJoint;
        this.selectionPriority = pSelectionPriority;
        this.placementPriority = pPlacementPriority;
    }

    public ServerboundSetJigsawBlockPacket(FriendlyByteBuf pBuffer) {
        this.pos = pBuffer.readBlockPos();
        this.name = pBuffer.readResourceLocation();
        this.target = pBuffer.readResourceLocation();
        this.pool = pBuffer.readResourceLocation();
        this.finalState = pBuffer.readUtf();
        this.joint = JigsawBlockEntity.JointType.byName(pBuffer.readUtf()).orElse(JigsawBlockEntity.JointType.ALIGNED);
        this.selectionPriority = pBuffer.readVarInt();
        this.placementPriority = pBuffer.readVarInt();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeBlockPos(this.pos);
        pBuffer.writeResourceLocation(this.name);
        pBuffer.writeResourceLocation(this.target);
        pBuffer.writeResourceLocation(this.pool);
        pBuffer.writeUtf(this.finalState);
        pBuffer.writeUtf(this.joint.getSerializedName());
        pBuffer.writeVarInt(this.selectionPriority);
        pBuffer.writeVarInt(this.placementPriority);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleSetJigsawBlock(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public ResourceLocation getName() {
        return this.name;
    }

    public ResourceLocation getTarget() {
        return this.target;
    }

    public ResourceLocation getPool() {
        return this.pool;
    }

    public String getFinalState() {
        return this.finalState;
    }

    public JigsawBlockEntity.JointType getJoint() {
        return this.joint;
    }

    public int getSelectionPriority() {
        return this.selectionPriority;
    }

    public int getPlacementPriority() {
        return this.placementPriority;
    }
}
