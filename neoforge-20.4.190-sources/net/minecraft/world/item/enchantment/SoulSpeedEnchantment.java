package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class SoulSpeedEnchantment extends Enchantment {
    public SoulSpeedEnchantment(Enchantment.Rarity pRarity, EquipmentSlot... pApplicableSlots) {
        super(pRarity, EnchantmentCategory.ARMOR_FEET, pApplicableSlots);
    }

    /**
     * Returns the minimal value of enchantability needed on the enchantment level passed.
     */
    @Override
    public int getMinCost(int pEnchantmentLevel) {
        return pEnchantmentLevel * 10;
    }

    @Override
    public int getMaxCost(int pEnchantmentLevel) {
        return this.getMinCost(pEnchantmentLevel) + 15;
    }

    /**
     * Checks if the enchantment should be considered a treasure enchantment. These enchantments can not be obtained using the enchantment table. The mending enchantment is an example of a treasure enchantment.
     * @return Whether the enchantment is a treasure enchantment.
     */
    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    /**
     * Checks if the enchantment can be sold by villagers in their trades.
     */
    @Override
    public boolean isTradeable() {
        return false;
    }

    /**
     * Checks if the enchantment can be applied to loot table drops.
     */
    @Override
    public boolean isDiscoverable() {
        return false;
    }

    /**
     * Returns the maximum level that the enchantment can have.
     */
    @Override
    public int getMaxLevel() {
        return 3;
    }
}
