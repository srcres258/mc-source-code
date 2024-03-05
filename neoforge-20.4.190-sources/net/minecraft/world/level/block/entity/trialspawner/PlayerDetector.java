package net.minecraft.world.level.block.entity.trialspawner;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

public interface PlayerDetector {
    PlayerDetector PLAYERS = (p_311981_, p_312054_, p_312441_) -> p_311981_.getPlayers(
                p_312337_ -> p_312337_.blockPosition().closerThan(p_312054_, (double)p_312441_) && !p_312337_.isCreative() && !p_312337_.isSpectator()
            )
            .stream()
            .map(Entity::getUUID)
            .toList();
    PlayerDetector SHEEP = (p_312029_, p_312529_, p_312727_) -> {
        AABB aabb = new AABB(p_312529_).inflate((double)p_312727_);
        return p_312029_.getEntities(EntityType.SHEEP, aabb, LivingEntity::isAlive).stream().map(Entity::getUUID).toList();
    };

    List<UUID> detect(ServerLevel pLevel, BlockPos pPos, int pMaxDistance);
}
