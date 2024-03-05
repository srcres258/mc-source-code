package net.minecraft.world.entity.vehicle;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class MinecartHopper extends AbstractMinecartContainer implements Hopper {
    private boolean enabled = true;

    public MinecartHopper(EntityType<? extends MinecartHopper> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public MinecartHopper(Level pLevel, double pX, double pY, double pZ) {
        super(EntityType.HOPPER_MINECART, pX, pY, pZ, pLevel);
    }

    @Override
    public AbstractMinecart.Type getMinecartType() {
        return AbstractMinecart.Type.HOPPER;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.HOPPER.defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 1;
    }

    /**
     * Returns the number of slots in the inventory.
     */
    @Override
    public int getContainerSize() {
        return 5;
    }

    /**
     * Called every tick the minecart is on an activator rail.
     */
    @Override
    public void activateMinecart(int pX, int pY, int pZ, boolean pReceivingPower) {
        boolean flag = !pReceivingPower;
        if (flag != this.isEnabled()) {
            this.setEnabled(flag);
        }
    }

    /**
     * Get whether this hopper minecart is being blocked by an activator rail.
     */
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Set whether this hopper minecart is being blocked by an activator rail.
     */
    public void setEnabled(boolean pEnabled) {
        this.enabled = pEnabled;
    }

    /**
     * Gets the world X position for this hopper entity.
     */
    @Override
    public double getLevelX() {
        return this.getX();
    }

    /**
     * Gets the world Y position for this hopper entity.
     */
    @Override
    public double getLevelY() {
        return this.getY() + 0.5;
    }

    /**
     * Gets the world Z position for this hopper entity.
     */
    @Override
    public double getLevelZ() {
        return this.getZ();
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.isAlive() && this.isEnabled() && this.suckInItems()) {
            this.setChanged();
        }
    }

    public boolean suckInItems() {
        if (HopperBlockEntity.suckInItems(this.level(), this)) {
            return true;
        } else {
            for(ItemEntity itementity : this.level()
                .getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(0.25, 0.0, 0.25), EntitySelector.ENTITY_STILL_ALIVE)) {
                if (HopperBlockEntity.addItem(this, itementity)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    protected Item getDropItem() {
        return Items.HOPPER_MINECART;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("Enabled", this.enabled);
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.enabled = pCompound.contains("Enabled") ? pCompound.getBoolean("Enabled") : true;
    }

    @Override
    public AbstractContainerMenu createMenu(int pId, Inventory pPlayerInventory) {
        return new HopperMenu(pId, pPlayerInventory, this);
    }
}
