package net.minecraft.network.protocol.game;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record ClientboundDamageEventPacket(int entityId, int sourceTypeId, int sourceCauseId, int sourceDirectId, Optional<Vec3> sourcePosition)
    implements Packet<ClientGamePacketListener> {
    public ClientboundDamageEventPacket(Entity p_270474_, DamageSource p_270781_) {
        this(
            p_270474_.getId(),
            p_270474_.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getId(p_270781_.type()),
            p_270781_.getEntity() != null ? p_270781_.getEntity().getId() : -1,
            p_270781_.getDirectEntity() != null ? p_270781_.getDirectEntity().getId() : -1,
            Optional.ofNullable(p_270781_.sourcePositionRaw())
        );
    }

    public ClientboundDamageEventPacket(FriendlyByteBuf p_270722_) {
        this(
            p_270722_.readVarInt(),
            p_270722_.readVarInt(),
            readOptionalEntityId(p_270722_),
            readOptionalEntityId(p_270722_),
            p_270722_.readOptional(p_270813_ -> new Vec3(p_270813_.readDouble(), p_270813_.readDouble(), p_270813_.readDouble()))
        );
    }

    private static void writeOptionalEntityId(FriendlyByteBuf pBuffer, int pOptionalEntityId) {
        pBuffer.writeVarInt(pOptionalEntityId + 1);
    }

    private static int readOptionalEntityId(FriendlyByteBuf pBuffer) {
        return pBuffer.readVarInt() - 1;
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.entityId);
        pBuffer.writeVarInt(this.sourceTypeId);
        writeOptionalEntityId(pBuffer, this.sourceCauseId);
        writeOptionalEntityId(pBuffer, this.sourceDirectId);
        pBuffer.writeOptional(this.sourcePosition, (p_293723_, p_293724_) -> {
            p_293723_.writeDouble(p_293724_.x());
            p_293723_.writeDouble(p_293724_.y());
            p_293723_.writeDouble(p_293724_.z());
        });
    }

    /**
     * Passes this Packet on to the PacketListener for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleDamageEvent(this);
    }

    public DamageSource getSource(Level pLevel) {
        Holder<DamageType> holder = pLevel.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolder(this.sourceTypeId).get();
        if (this.sourcePosition.isPresent()) {
            return new DamageSource(holder, this.sourcePosition.get());
        } else {
            Entity entity = pLevel.getEntity(this.sourceCauseId);
            Entity entity1 = pLevel.getEntity(this.sourceDirectId);
            return new DamageSource(holder, entity1, entity);
        }
    }
}
