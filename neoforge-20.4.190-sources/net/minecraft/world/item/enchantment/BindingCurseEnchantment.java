package net.minecraft.world.item.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BindingCurseEnchantment extends Enchantment {
    public BindingCurseEnchantment(Enchantment.Rarity pRarity, EquipmentSlot... pApplicableSlots) {
        super(pRarity, EnchantmentCategory.WEARABLE, pApplicableSlots);
    }

    /**
     * Returns the minimal value of enchantability needed on the enchantment level passed.
     */
    @Override
    public int getMinCost(int pEnchantmentLevel) {
        return 25;
    }

    @Override
    public int getMaxCost(int pEnchantmentLevel) {
        return 50;
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
     * Checks if the enchantment is considered a curse. These enchantments are treated as debuffs and can not be removed from items under normal circumstances.
     * @return Whether the enchantment is a curse.
     */
    @Override
    public boolean isCurse() {
        return true;
    }

    /**
     * Checks if the enchantment can be applied to a given ItemStack.
     *
     * @param pStack The ItemStack to test.
     */
    @Override
    public boolean canEnchant(ItemStack pStack) {
        return !pStack.is(Items.SHIELD) && super.canEnchant(pStack);
    }
}
