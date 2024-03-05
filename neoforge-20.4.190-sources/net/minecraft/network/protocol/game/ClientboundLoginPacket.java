package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * @param showDeathScreen Set to false when the doImmediateRespawn gamerule is true
 */
public record ClientboundLoginPacket(
    int playerId,
    boolean hardcore,
    Set<ResourceKey<Level>> levels,
    int maxPlayers,
    int chunkRadius,
    int simulationDistance,
    boolean reducedDebugInfo,
    boolean showDeathScreen,
    boolean doLimitedCrafting,
    CommonPlayerSpawnInfo commonPlayerSpawnInfo
) implements Packet<ClientGamePacketListener> {
    public ClientboundLoginPacket(FriendlyByteBuf p_178960_) {
        this(
            p_178960_.readInt(),
            p_178960_.readBoolean(),
            p_178960_.readCollection(Sets::newHashSetWithExpectedSize, p_258210_ -> p_258210_.readResourceKey(Registries.DIMENSION)),
            p_178960_.readVarInt(),
            p_178960_.readVarInt(),
            p_178960_.readVarInt(),
            p_178960_.readBoolean(),
            p_178960_.readBoolean(),
            p_178960_.readBoolean(),
            new CommonPlayerSpawnInfo(p_178960_)
        );
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeInt(this.playerId);
        pBuffer.writeBoolean(this.hardcore);
        pBuffer.writeCollection(this.levels, FriendlyByteBuf::writeResourceKey);
        pBuffer.writeVarInt(this.maxPlayers);
        pBuffer.writeVarInt(this.chunkRadius);
        pBuffer.writeVarInt(this.simulationDistance);
        pBuffer.writeBoolean(this.reducedDebugInfo);
        pBuffer.writeBoolean(this.showDeathScreen);
        pBuffer.writeBoolean(this.doLimitedCrafting);
        this.commonPlayerSpawnInfo.write(pBuffer);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleLogin(this);
    }
}
