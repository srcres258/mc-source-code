package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.schedule.Activity;

public class ReactToBell {
    public static BehaviorControl<LivingEntity> create() {
        return BehaviorBuilder.create(
            p_259349_ -> p_259349_.group(p_259349_.present(MemoryModuleType.HEARD_BELL_TIME))
                    .apply(p_259349_, p_259472_ -> (p_311634_, p_311635_, p_311636_) -> {
                            Raid raid = p_311634_.getRaidAt(p_311635_.blockPosition());
                            if (raid == null) {
                                p_311635_.getBrain().setActiveActivityIfPossible(Activity.HIDE);
                            }
        
                            return true;
                        })
        );
    }
}
