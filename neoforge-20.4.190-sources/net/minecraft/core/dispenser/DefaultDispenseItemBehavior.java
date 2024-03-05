package net.minecraft.core.dispenser;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;

public class DefaultDispenseItemBehavior implements DispenseItemBehavior {
    @Override
    public final ItemStack dispense(BlockSource pBlockSource, ItemStack pItem) {
        ItemStack itemstack = this.execute(pBlockSource, pItem);
        this.playSound(pBlockSource);
        this.playAnimation(pBlockSource, pBlockSource.state().getValue(DispenserBlock.FACING));
        return itemstack;
    }

    protected ItemStack execute(BlockSource pBlockSource, ItemStack pItem) {
        Direction direction = pBlockSource.state().getValue(DispenserBlock.FACING);
        Position position = DispenserBlock.getDispensePosition(pBlockSource);
        ItemStack itemstack = pItem.split(1);
        spawnItem(pBlockSource.level(), itemstack, 6, direction, position);
        return pItem;
    }

    public static void spawnItem(Level pLevel, ItemStack pStack, int pSpeed, Direction pFacing, Position pPosition) {
        double d0 = pPosition.x();
        double d1 = pPosition.y();
        double d2 = pPosition.z();
        if (pFacing.getAxis() == Direction.Axis.Y) {
            d1 -= 0.125;
        } else {
            d1 -= 0.15625;
        }

        ItemEntity itementity = new ItemEntity(pLevel, d0, d1, d2, pStack);
        double d3 = pLevel.random.nextDouble() * 0.1 + 0.2;
        itementity.setDeltaMovement(
            pLevel.random.triangle((double)pFacing.getStepX() * d3, 0.0172275 * (double)pSpeed),
            pLevel.random.triangle(0.2, 0.0172275 * (double)pSpeed),
            pLevel.random.triangle((double)pFacing.getStepZ() * d3, 0.0172275 * (double)pSpeed)
        );
        pLevel.addFreshEntity(itementity);
    }

    protected void playSound(BlockSource pBlockSource) {
        pBlockSource.level().levelEvent(1000, pBlockSource.pos(), 0);
    }

    protected void playAnimation(BlockSource pBlockSource, Direction pDirection) {
        pBlockSource.level().levelEvent(2000, pBlockSource.pos(), pDirection.get3DDataValue());
    }
}
