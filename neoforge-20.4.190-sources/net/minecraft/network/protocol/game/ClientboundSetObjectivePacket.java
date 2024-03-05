package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.NumberFormatTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class ClientboundSetObjectivePacket implements Packet<ClientGamePacketListener> {
    public static final int METHOD_ADD = 0;
    public static final int METHOD_REMOVE = 1;
    public static final int METHOD_CHANGE = 2;
    private final String objectiveName;
    private final Component displayName;
    private final ObjectiveCriteria.RenderType renderType;
    @Nullable
    private final NumberFormat numberFormat;
    private final int method;

    public ClientboundSetObjectivePacket(Objective pObjective, int pMethod) {
        this.objectiveName = pObjective.getName();
        this.displayName = pObjective.getDisplayName();
        this.renderType = pObjective.getRenderType();
        this.numberFormat = pObjective.numberFormat();
        this.method = pMethod;
    }

    public ClientboundSetObjectivePacket(FriendlyByteBuf pBuffer) {
        this.objectiveName = pBuffer.readUtf();
        this.method = pBuffer.readByte();
        if (this.method != 0 && this.method != 2) {
            this.displayName = CommonComponents.EMPTY;
            this.renderType = ObjectiveCriteria.RenderType.INTEGER;
            this.numberFormat = null;
        } else {
            this.displayName = pBuffer.readComponentTrusted();
            this.renderType = pBuffer.readEnum(ObjectiveCriteria.RenderType.class);
            this.numberFormat = pBuffer.readNullable(NumberFormatTypes::readFromStream);
        }
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUtf(this.objectiveName);
        pBuffer.writeByte(this.method);
        if (this.method == 0 || this.method == 2) {
            pBuffer.writeComponent(this.displayName);
            pBuffer.writeEnum(this.renderType);
            pBuffer.writeNullable(this.numberFormat, NumberFormatTypes::writeToStream);
        }
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleAddObjective(this);
    }

    public String getObjectiveName() {
        return this.objectiveName;
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public int getMethod() {
        return this.method;
    }

    public ObjectiveCriteria.RenderType getRenderType() {
        return this.renderType;
    }

    @Nullable
    public NumberFormat getNumberFormat() {
        return this.numberFormat;
    }
}
