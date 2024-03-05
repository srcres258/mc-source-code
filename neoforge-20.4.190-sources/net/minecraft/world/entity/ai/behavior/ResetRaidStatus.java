package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ResetRaidStatus {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(p_259870_ -> p_259870_.point((p_313621_, p_313622_, p_313623_) -> {
                if (p_313621_.random.nextInt(20) != 0) {
                    return false;
                } else {
                    Brain<?> brain = p_313622_.getBrain();
                    Raid raid = p_313621_.getRaidAt(p_313622_.blockPosition());
                    if (raid == null || raid.isStopped() || raid.isLoss()) {
                        brain.setDefaultActivity(Activity.IDLE);
                        brain.updateActivityFromSchedule(p_313621_.getDayTime(), p_313621_.getGameTime());
                    }

                    return true;
                }
            }));
    }
}
