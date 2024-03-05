package net.minecraft.world.entity.ai.attributes;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import org.slf4j.Logger;

public class AttributeModifier {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<AttributeModifier> CODEC = RecordCodecBuilder.create(
        p_304341_ -> p_304341_.group(
                    UUIDUtil.CODEC.fieldOf("UUID").forGetter(AttributeModifier::getId),
                    Codec.STRING.fieldOf("Name").forGetter(p_304342_ -> p_304342_.name),
                    Codec.DOUBLE.fieldOf("Amount").forGetter(AttributeModifier::getAmount),
                    AttributeModifier.Operation.CODEC.fieldOf("Operation").forGetter(AttributeModifier::getOperation)
                )
                .apply(p_304341_, AttributeModifier::new)
    );
    private final double amount;
    private final AttributeModifier.Operation operation;
    private final String name;
    private final UUID id;

    public AttributeModifier(String pName, double pAmount, AttributeModifier.Operation pOperation) {
        this(Mth.createInsecureUUID(RandomSource.createNewThreadLocalInstance()), pName, pAmount, pOperation);
    }

    public AttributeModifier(UUID p_22200_, String p_22201_, double p_22202_, AttributeModifier.Operation p_22203_) {
        this.id = p_22200_;
        this.name = p_22201_;
        this.amount = p_22202_;
        this.operation = p_22203_;
    }

    public UUID getId() {
        return this.id;
    }

    public AttributeModifier.Operation getOperation() {
        return this.operation;
    }

    public double getAmount() {
        return this.amount;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else if (pOther != null && this.getClass() == pOther.getClass()) {
            AttributeModifier attributemodifier = (AttributeModifier)pOther;
            return Objects.equals(this.id, attributemodifier.id);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "AttributeModifier{amount=" + this.amount + ", operation=" + this.operation + ", name='" + this.name + "', id=" + this.id + "}";
    }

    public CompoundTag save() {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.putString("Name", this.name);
        compoundtag.putDouble("Amount", this.amount);
        compoundtag.putInt("Operation", this.operation.toValue());
        compoundtag.putUUID("UUID", this.id);
        return compoundtag;
    }

    @Nullable
    public static AttributeModifier load(CompoundTag pNbt) {
        try {
            UUID uuid = pNbt.getUUID("UUID");
            AttributeModifier.Operation attributemodifier$operation = AttributeModifier.Operation.fromValue(pNbt.getInt("Operation"));
            return new AttributeModifier(uuid, pNbt.getString("Name"), pNbt.getDouble("Amount"), attributemodifier$operation);
        } catch (Exception exception) {
            LOGGER.warn("Unable to create attribute: {}", exception.getMessage());
            return null;
        }
    }

    public static enum Operation implements StringRepresentable {
        ADDITION("addition", 0),
        MULTIPLY_BASE("multiply_base", 1),
        MULTIPLY_TOTAL("multiply_total", 2);

        private static final AttributeModifier.Operation[] OPERATIONS = new AttributeModifier.Operation[]{ADDITION, MULTIPLY_BASE, MULTIPLY_TOTAL};
        public static final Codec<AttributeModifier.Operation> CODEC = StringRepresentable.fromEnum(AttributeModifier.Operation::values);
        private final String name;
        private final int value;

        private Operation(String pName, int pValue) {
            this.name = pName;
            this.value = pValue;
        }

        public int toValue() {
            return this.value;
        }

        public static AttributeModifier.Operation fromValue(int pId) {
            if (pId >= 0 && pId < OPERATIONS.length) {
                return OPERATIONS[pId];
            } else {
                throw new IllegalArgumentException("No operation with value " + pId);
            }
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
