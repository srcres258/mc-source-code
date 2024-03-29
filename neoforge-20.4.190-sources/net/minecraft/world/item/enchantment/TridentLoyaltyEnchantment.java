package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;

public class TridentLoyaltyEnchantment extends Enchantment {
    public TridentLoyaltyEnchantment(Enchantment.Rarity pRarity, EquipmentSlot... pApplicableSlots) {
        super(pRarity, EnchantmentCategory.TRIDENT, pApplicableSlots);
    }

    /**
     * Returns the minimal value of enchantability needed on the enchantment level passed.
     */
    @Override
    public int getMinCost(int pEnchantmentLevel) {
        return 5 + pEnchantmentLevel * 7;
    }

    @Override
    public int getMaxCost(int pEnchantmentLevel) {
        return 50;
    }

    /**
     * Returns the maximum level that the enchantment can have.
     */
    @Override
    public int getMaxLevel() {
        return 3;
    }
}
