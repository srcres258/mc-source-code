package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

/**
 * @param paramA Different for each blockID
 */
public record BlockEventData(BlockPos pos, Block block, int paramA, int paramB) {
}
