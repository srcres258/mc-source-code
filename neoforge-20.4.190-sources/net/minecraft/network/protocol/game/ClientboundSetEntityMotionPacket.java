package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class ClientboundSetEntityMotionPacket implements Packet<ClientGamePacketListener> {
    private final int id;
    private final int xa;
    private final int ya;
    private final int za;

    public ClientboundSetEntityMotionPacket(Entity pEntity) {
        this(pEntity.getId(), pEntity.getDeltaMovement());
    }

    public ClientboundSetEntityMotionPacket(int pId, Vec3 pDeltaMovement) {
        this.id = pId;
        double d0 = 3.9;
        double d1 = Mth.clamp(pDeltaMovement.x, -3.9, 3.9);
        double d2 = Mth.clamp(pDeltaMovement.y, -3.9, 3.9);
        double d3 = Mth.clamp(pDeltaMovement.z, -3.9, 3.9);
        this.xa = (int)(d1 * 8000.0);
        this.ya = (int)(d2 * 8000.0);
        this.za = (int)(d3 * 8000.0);
    }

    public ClientboundSetEntityMotionPacket(FriendlyByteBuf pBuffer) {
        this.id = pBuffer.readVarInt();
        this.xa = pBuffer.readShort();
        this.ya = pBuffer.readShort();
        this.za = pBuffer.readShort();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.id);
        pBuffer.writeShort(this.xa);
        pBuffer.writeShort(this.ya);
        pBuffer.writeShort(this.za);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetEntityMotion(this);
    }

    public int getId() {
        return this.id;
    }

    public int getXa() {
        return this.xa;
    }

    public int getYa() {
        return this.ya;
    }

    public int getZa() {
        return this.za;
    }
}
