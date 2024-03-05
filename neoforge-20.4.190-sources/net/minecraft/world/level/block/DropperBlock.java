package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.DropperBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class DropperBlock extends DispenserBlock {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<DropperBlock> CODEC = simpleCodec(DropperBlock::new);
    private static final DispenseItemBehavior DISPENSE_BEHAVIOUR = new DefaultDispenseItemBehavior();

    @Override
    public MapCodec<DropperBlock> codec() {
        return CODEC;
    }

    public DropperBlock(BlockBehaviour.Properties p_52942_) {
        super(p_52942_);
    }

    @Override
    protected DispenseItemBehavior getDispenseMethod(ItemStack pStack) {
        return DISPENSE_BEHAVIOUR;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new DropperBlockEntity(pPos, pState);
    }

    @Override
    protected void dispenseFrom(ServerLevel pLevel, BlockState pState, BlockPos pPos) {
        DispenserBlockEntity dispenserblockentity = pLevel.getBlockEntity(pPos, BlockEntityType.DROPPER).orElse(null);
        if (dispenserblockentity == null) {
            LOGGER.warn("Ignoring dispensing attempt for Dropper without matching block entity at {}", pPos);
        } else {
            BlockSource blocksource = new BlockSource(pLevel, pPos, pState, dispenserblockentity);
            int i = dispenserblockentity.getRandomSlot(pLevel.random);
            if (i < 0) {
                pLevel.levelEvent(1001, pPos, 0);
            } else {
                ItemStack itemstack = dispenserblockentity.getItem(i);
                if (!itemstack.isEmpty() && net.neoforged.neoforge.items.VanillaInventoryCodeHooks.dropperInsertHook(pLevel, pPos, dispenserblockentity, i, itemstack)) {
                    Direction direction = pLevel.getBlockState(pPos).getValue(FACING);
                    Container container = HopperBlockEntity.getContainerAt(pLevel, pPos.relative(direction));
                    ItemStack itemstack1;
                    if (container == null) {
                        itemstack1 = DISPENSE_BEHAVIOUR.dispense(blocksource, itemstack);
                    } else {
                        itemstack1 = HopperBlockEntity.addItem(dispenserblockentity, container, itemstack.copy().split(1), direction.getOpposite());
                        if (itemstack1.isEmpty()) {
                            itemstack1 = itemstack.copy();
                            itemstack1.shrink(1);
                        } else {
                            itemstack1 = itemstack.copy();
                        }
                    }

                    dispenserblockentity.setItem(i, itemstack1);
                }
            }
        }
    }
}
