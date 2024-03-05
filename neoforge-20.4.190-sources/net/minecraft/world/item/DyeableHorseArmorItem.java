package net.minecraft.world.item;

/**
 * Constructor for the DyeableHorseArmor
 */
public class DyeableHorseArmorItem extends HorseArmorItem implements DyeableLeatherItem {
    /**
     * @param pProtection the given protection level of the {@code HorseArmorItem}
     * @param pIdentifier the texture path identifier for the {@code
     *                    DyeableHorseArmorItem}, {@link
     *                    net.minecraft.world.item.HorseArmorItem}
     * @param pProperties the item properties
     */
    public DyeableHorseArmorItem(int pProtection, String pIdentifier, Item.Properties pProperties) {
        super(pProtection, pIdentifier, pProperties);
    }
    public DyeableHorseArmorItem(int p_41110_, net.minecraft.resources.ResourceLocation p_41111_, Item.Properties p_41112_) {
        super(p_41110_, p_41111_, p_41112_);
    }
}
