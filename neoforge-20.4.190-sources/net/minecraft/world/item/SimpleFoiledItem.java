package net.minecraft.world.item;

public class SimpleFoiledItem extends Item {
    public SimpleFoiledItem(Item.Properties pProperties) {
        super(pProperties);
    }

    /**
     * Returns {@code true} if this item has an enchantment glint. By default, this returns <code>stack.isItemEnchanted()</code>, but other items can override it (for instance, written books always return true).
     *
     * Note that if you override this method, you generally want to also call the super version (on {@link Item}) to get the glint for enchanted items. Of course, that is unnecessary if the overwritten version always returns true.
     */
    @Override
    public boolean isFoil(ItemStack pStack) {
        return true;
    }
}
