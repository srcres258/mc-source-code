package net.minecraft.advancements;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record AdvancementHolder(ResourceLocation id, Advancement value) {
    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeResourceLocation(this.id);
        this.value.write(pBuffer);
    }

    public static AdvancementHolder read(FriendlyByteBuf pBuffer) {
        return new AdvancementHolder(pBuffer.readResourceLocation(), Advancement.read(pBuffer));
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            if (pOther instanceof AdvancementHolder advancementholder && this.id.equals(advancementholder.id)) {
                return true;
            }

            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}
