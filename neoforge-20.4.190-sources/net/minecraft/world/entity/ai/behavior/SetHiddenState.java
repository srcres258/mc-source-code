package net.minecraft.world.entity.ai.behavior;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.apache.commons.lang3.mutable.MutableInt;

public class SetHiddenState {
    private static final int HIDE_TIMEOUT = 300;

    public static BehaviorControl<LivingEntity> create(int pStayHiddenSeconds, int pCloseEnoughdist) {
        int i = pStayHiddenSeconds * 20;
        MutableInt mutableint = new MutableInt(0);
        return BehaviorBuilder.create(
            p_259055_ -> p_259055_.group(
                        p_259055_.present(MemoryModuleType.HIDING_PLACE), p_259055_.present(MemoryModuleType.HEARD_BELL_TIME)
                    )
                    .apply(p_259055_, (p_260296_, p_260145_) -> (p_313630_, p_313631_, p_313632_) -> {
                            long j = p_259055_.<Long>get(p_260145_);
                            boolean flag = j + 300L <= p_313632_;
                            if (mutableint.getValue() <= i && !flag) {
                                BlockPos blockpos = p_259055_.<GlobalPos>get(p_260296_).pos();
                                if (blockpos.closerThan(p_313631_.blockPosition(), (double)pCloseEnoughdist)) {
                                    mutableint.increment();
                                }
        
                                return true;
                            } else {
                                p_260145_.erase();
                                p_260296_.erase();
                                p_313631_.getBrain().updateActivityFromSchedule(p_313630_.getDayTime(), p_313630_.getGameTime());
                                mutableint.setValue(0);
                                return true;
                            }
                        })
        );
    }
}
