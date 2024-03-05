package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

public class PlayerHeadItem extends StandingAndWallBlockItem {
    public static final String TAG_SKULL_OWNER = "SkullOwner";

    public PlayerHeadItem(Block pBlock, Block pWallBlock, Item.Properties pProperties) {
        super(pBlock, pWallBlock, pProperties, Direction.DOWN);
    }

    @Override
    public Component getName(ItemStack pStack) {
        if (pStack.is(Items.PLAYER_HEAD) && pStack.hasTag()) {
            String s = null;
            CompoundTag compoundtag = pStack.getTag();
            if (compoundtag.contains("SkullOwner", 8)) {
                s = compoundtag.getString("SkullOwner");
            } else if (compoundtag.contains("SkullOwner", 10)) {
                CompoundTag compoundtag1 = compoundtag.getCompound("SkullOwner");
                if (compoundtag1.contains("Name", 8)) {
                    s = compoundtag1.getString("Name");
                }
            }

            if (s != null) {
                return Component.translatable(this.getDescriptionId() + ".named", s);
            }
        }

        return super.getName(pStack);
    }

    @Override
    public void verifyTagAfterLoad(CompoundTag pTag) {
        super.verifyTagAfterLoad(pTag);
        SkullBlockEntity.resolveGameProfile(pTag);
    }
}
