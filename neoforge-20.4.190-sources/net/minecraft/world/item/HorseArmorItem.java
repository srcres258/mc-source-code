package net.minecraft.world.item;

import net.minecraft.resources.ResourceLocation;

public class HorseArmorItem extends Item {
    private static final String TEX_FOLDER = "textures/entity/horse/";
    private final int protection;
    private final ResourceLocation texture;

    /**
     * @param pProtection the given protection level of the {@code HorseArmorItem}
     * @param pIdentifier the texture path identifier for the {@code
     *                    DyeableHorseArmorItem}, {@link
     *                    net.minecraft.world.item.HorseArmorItem}
     * @param pProperties the item properties
     */
    public HorseArmorItem(int pProtection, String pIdentifier, Item.Properties pProperties) {
        this(pProtection, new ResourceLocation("textures/entity/horse/armor/horse_armor_" + pIdentifier + ".png"), pProperties);
    }

    public HorseArmorItem(int p_41364_, ResourceLocation p_41365_, Item.Properties p_41366_) {
        super(p_41366_);
        this.protection = p_41364_;
        this.texture = p_41365_;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public int getProtection() {
        return this.protection;
    }
}
