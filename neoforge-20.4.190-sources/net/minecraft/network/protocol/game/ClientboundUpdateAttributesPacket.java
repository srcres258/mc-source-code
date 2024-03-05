package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class ClientboundUpdateAttributesPacket implements Packet<ClientGamePacketListener> {
    private final int entityId;
    private final List<ClientboundUpdateAttributesPacket.AttributeSnapshot> attributes;

    public ClientboundUpdateAttributesPacket(int pEntityId, Collection<AttributeInstance> pAttributes) {
        this.entityId = pEntityId;
        this.attributes = Lists.newArrayList();

        for(AttributeInstance attributeinstance : pAttributes) {
            this.attributes
                .add(
                    new ClientboundUpdateAttributesPacket.AttributeSnapshot(
                        attributeinstance.getAttribute(), attributeinstance.getBaseValue(), attributeinstance.getModifiers()
                    )
                );
        }
    }

    public ClientboundUpdateAttributesPacket(FriendlyByteBuf pBuffer) {
        this.entityId = pBuffer.readVarInt();
        this.attributes = pBuffer.readList(
            p_258211_ -> {
                ResourceLocation resourcelocation = p_258211_.readResourceLocation();
                Attribute attribute = BuiltInRegistries.ATTRIBUTE.get(resourcelocation);
                double d0 = p_258211_.readDouble();
                List<AttributeModifier> list = p_258211_.readList(
                    p_179457_ -> new AttributeModifier(
                            p_179457_.readUUID(),
                            "Unknown synced attribute modifier",
                            p_179457_.readDouble(),
                            AttributeModifier.Operation.fromValue(p_179457_.readByte())
                        )
                );
                return new ClientboundUpdateAttributesPacket.AttributeSnapshot(attribute, d0, list);
            }
        );
    }

    /**
     * Writes the raw packet data to the data stream.
     */
    @Override
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.entityId);
        pBuffer.writeCollection(this.attributes, (p_293736_, p_293737_) -> {
            p_293736_.writeResourceLocation(BuiltInRegistries.ATTRIBUTE.getKey(p_293737_.getAttribute()));
            p_293736_.writeDouble(p_293737_.getBase());
            p_293736_.writeCollection(p_293737_.getModifiers(), (p_293738_, p_293739_) -> {
                p_293738_.writeUUID(p_293739_.getId());
                p_293738_.writeDouble(p_293739_.getAmount());
                p_293738_.writeByte(p_293739_.getOperation().toValue());
            });
        });
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleUpdateAttributes(this);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public List<ClientboundUpdateAttributesPacket.AttributeSnapshot> getValues() {
        return this.attributes;
    }

    public static class AttributeSnapshot {
        private final Attribute attribute;
        private final double base;
        private final Collection<AttributeModifier> modifiers;

        public AttributeSnapshot(Attribute pAttribute, double pBase, Collection<AttributeModifier> pModifiers) {
            this.attribute = pAttribute;
            this.base = pBase;
            this.modifiers = pModifiers;
        }

        public Attribute getAttribute() {
            return this.attribute;
        }

        public double getBase() {
            return this.base;
        }

        public Collection<AttributeModifier> getModifiers() {
            return this.modifiers;
        }
    }
}
