package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;

public class UpdateActivityFromSchedule {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(p_259429_ -> p_259429_.point((p_313635_, p_313636_, p_313637_) -> {
                p_313636_.getBrain().updateActivityFromSchedule(p_313635_.getDayTime(), p_313635_.getGameTime());
                return true;
            }));
    }
}
