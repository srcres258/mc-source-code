package net.minecraft.network.protocol.game;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;

public class ClientboundSetDisplayObjectivePacket implements Packet<ClientGamePacketListener> {
    private final DisplaySlot slot;
    private final String objectiveName;

    public ClientboundSetDisplayObjectivePacket(DisplaySlot p_294785_, @Nullable Objective p_133132_) {
        this.slot = p_294785_;
        if (p_133132_ == null) {
            this.objectiveName = "";
        } else {
            this.objectiveName = p_133132_.getName();
        }
    }

    public ClientboundSetDisplayObjectivePacket(FriendlyByteBuf pBuffer) {
        this.slot = pBuffer.readById(DisplaySlot.BY_ID);
        this.objectiveName = pBuffer.readUtf();
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeById(DisplaySlot::id, this.slot);
        pBuffer.writeUtf(this.objectiveName);
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetDisplayObjective(this);
    }

    public DisplaySlot getSlot() {
        return this.slot;
    }

    @Nullable
    public String getObjectiveName() {
        return Objects.equals(this.objectiveName, "") ? null : this.objectiveName;
    }
}
