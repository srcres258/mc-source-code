package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record NbtPredicate(CompoundTag tag) {
    public static final Codec<NbtPredicate> CODEC = TagParser.AS_CODEC.xmap(NbtPredicate::new, NbtPredicate::tag);

    public boolean matches(ItemStack pStack) {
        return this.matches(pStack.getTag());
    }

    public boolean matches(Entity pEntity) {
        return this.matches(getEntityTagToCompare(pEntity));
    }

    public boolean matches(@Nullable Tag pTag) {
        return pTag != null && NbtUtils.compareNbt(this.tag, pTag, true);
    }

    public static CompoundTag getEntityTagToCompare(Entity pEntity) {
        CompoundTag compoundtag = pEntity.saveWithoutId(new CompoundTag());
        if (pEntity instanceof Player) {
            ItemStack itemstack = ((Player)pEntity).getInventory().getSelected();
            if (!itemstack.isEmpty()) {
                compoundtag.put("SelectedItem", itemstack.save(new CompoundTag()));
            }
        }

        return compoundtag;
    }
}
