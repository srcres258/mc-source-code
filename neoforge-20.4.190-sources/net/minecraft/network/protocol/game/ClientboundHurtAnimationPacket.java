package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.LivingEntity;

public record ClientboundHurtAnimationPacket(int id, float yaw) implements Packet<ClientGamePacketListener> {
    public ClientboundHurtAnimationPacket(LivingEntity p_265293_) {
        this(p_265293_.getId(), p_265293_.getHurtDir());
    }

    public ClientboundHurtAnimationPacket(FriendlyByteBuf p_265181_) {
        this(p_265181_.readVarInt(), p_265181_.readFloat());
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.id);
        pBuffer.writeFloat(this.yaw);
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleHurtAnimation(this);
    }
}
