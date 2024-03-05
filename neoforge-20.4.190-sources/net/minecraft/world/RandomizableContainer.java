package net.minecraft.world;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public interface RandomizableContainer extends Container {
    String LOOT_TABLE_TAG = "LootTable";
    String LOOT_TABLE_SEED_TAG = "LootTableSeed";

    @Nullable
    ResourceLocation getLootTable();

    void setLootTable(@Nullable ResourceLocation pLootTable);

    default void setLootTable(ResourceLocation pLootTable, long pSeed) {
        this.setLootTable(pLootTable);
        this.setLootTableSeed(pSeed);
    }

    long getLootTableSeed();

    void setLootTableSeed(long pSeed);

    BlockPos getBlockPos();

    @Nullable
    Level getLevel();

    static void setBlockEntityLootTable(BlockGetter pLevel, RandomSource pRandom, BlockPos pPos, ResourceLocation pLootTable) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        if (blockentity instanceof RandomizableContainer randomizablecontainer) {
            randomizablecontainer.setLootTable(pLootTable, pRandom.nextLong());
        }
    }

    default boolean tryLoadLootTable(CompoundTag pTag) {
        if (pTag.contains("LootTable", 8)) {
            this.setLootTable(new ResourceLocation(pTag.getString("LootTable")));
            this.setLootTableSeed(pTag.getLong("LootTableSeed"));
            return true;
        } else {
            return false;
        }
    }

    default boolean trySaveLootTable(CompoundTag pTag) {
        ResourceLocation resourcelocation = this.getLootTable();
        if (resourcelocation == null) {
            return false;
        } else {
            pTag.putString("LootTable", resourcelocation.toString());
            long i = this.getLootTableSeed();
            if (i != 0L) {
                pTag.putLong("LootTableSeed", i);
            }

            return true;
        }
    }

    default void unpackLootTable(@Nullable Player pPlayer) {
        Level level = this.getLevel();
        BlockPos blockpos = this.getBlockPos();
        ResourceLocation resourcelocation = this.getLootTable();
        if (resourcelocation != null && level != null && level.getServer() != null) {
            LootTable loottable = level.getServer().getLootData().getLootTable(resourcelocation);
            if (pPlayer instanceof ServerPlayer) {
                CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer)pPlayer, resourcelocation);
            }

            this.setLootTable(null);
            LootParams.Builder lootparams$builder = new LootParams.Builder((ServerLevel)level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockpos));
            if (pPlayer != null) {
                lootparams$builder.withLuck(pPlayer.getLuck()).withParameter(LootContextParams.THIS_ENTITY, pPlayer);
            }

            loottable.fill(this, lootparams$builder.create(LootContextParamSets.CHEST), this.getLootTableSeed());
        }
    }
}
