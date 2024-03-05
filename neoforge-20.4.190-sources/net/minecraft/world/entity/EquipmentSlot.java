package net.minecraft.world.entity;

import net.minecraft.util.StringRepresentable;

public enum EquipmentSlot implements StringRepresentable {
    MAINHAND(EquipmentSlot.Type.HAND, 0, 0, "mainhand"),
    OFFHAND(EquipmentSlot.Type.HAND, 1, 5, "offhand"),
    FEET(EquipmentSlot.Type.ARMOR, 0, 1, "feet"),
    LEGS(EquipmentSlot.Type.ARMOR, 1, 2, "legs"),
    CHEST(EquipmentSlot.Type.ARMOR, 2, 3, "chest"),
    HEAD(EquipmentSlot.Type.ARMOR, 3, 4, "head");

    public static final StringRepresentable.EnumCodec<EquipmentSlot> CODEC = StringRepresentable.fromEnum(EquipmentSlot::values);
    private final EquipmentSlot.Type type;
    private final int index;
    private final int filterFlag;
    private final String name;

    private EquipmentSlot(EquipmentSlot.Type pType, int pIndex, int pFilterFlag, String pName) {
        this.type = pType;
        this.index = pIndex;
        this.filterFlag = pFilterFlag;
        this.name = pName;
    }

    public EquipmentSlot.Type getType() {
        return this.type;
    }

    public int getIndex() {
        return this.index;
    }

    public int getIndex(int pBaseIndex) {
        return pBaseIndex + this.index;
    }

    /**
     * Gets the actual slot index.
     */
    public int getFilterFlag() {
        return this.filterFlag;
    }

    public String getName() {
        return this.name;
    }

    public boolean isArmor() {
        return this.type == EquipmentSlot.Type.ARMOR;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public static EquipmentSlot byName(String pTargetName) {
        EquipmentSlot equipmentslot = CODEC.byName(pTargetName);
        if (equipmentslot != null) {
            return equipmentslot;
        } else {
            throw new IllegalArgumentException("Invalid slot '" + pTargetName + "'");
        }
    }

    /**
     * Returns the slot type based on the slot group and the index inside that group.
     */
    public static EquipmentSlot byTypeAndIndex(EquipmentSlot.Type pSlotType, int pSlotIndex) {
        for(EquipmentSlot equipmentslot : values()) {
            if (equipmentslot.getType() == pSlotType && equipmentslot.getIndex() == pSlotIndex) {
                return equipmentslot;
            }
        }

        throw new IllegalArgumentException("Invalid slot '" + pSlotType + "': " + pSlotIndex);
    }

    public static enum Type {
        HAND,
        ARMOR;
    }
}
