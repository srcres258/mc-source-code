package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.LongJumpUtil;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LongJump extends Behavior<Breeze> {
    private static final int REQUIRED_AIR_BLOCKS_ABOVE = 4;
    private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 50.0;
    private static final int JUMP_COOLDOWN_TICKS = 10;
    private static final int JUMP_COOLDOWN_WHEN_HURT_TICKS = 2;
    private static final int INHALING_DURATION_TICKS = Math.round(10.0F);
    private static final float MAX_JUMP_VELOCITY = 1.4F;
    private static final ObjectArrayList<Integer> ALLOWED_ANGLES = new ObjectArrayList<>(Lists.newArrayList(40, 55, 60, 75, 80));

    @VisibleForTesting
    public LongJump() {
        super(
            Map.of(
                MemoryModuleType.ATTACK_TARGET,
                MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.BREEZE_JUMP_COOLDOWN,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_JUMP_INHALING,
                MemoryStatus.REGISTERED,
                MemoryModuleType.BREEZE_JUMP_TARGET,
                MemoryStatus.REGISTERED,
                MemoryModuleType.BREEZE_SHOOT,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET,
                MemoryStatus.VALUE_ABSENT
            ),
            200
        );
    }

    protected boolean checkExtraStartConditions(ServerLevel pLevel, Breeze pOwner) {
        if (!pOwner.onGround() && !pOwner.isInWater()) {
            return false;
        } else if (pOwner.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_TARGET, MemoryStatus.VALUE_PRESENT)) {
            return true;
        } else {
            LivingEntity livingentity = pOwner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
            if (livingentity == null) {
                return false;
            } else if (outOfAggroRange(pOwner, livingentity)) {
                pOwner.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                return false;
            } else if (tooCloseForJump(pOwner, livingentity)) {
                return false;
            } else if (!canJumpFromCurrentPosition(pLevel, pOwner)) {
                return false;
            } else {
                BlockPos blockpos = snapToSurface(pOwner, randomPointBehindTarget(livingentity, pOwner.getRandom()));
                if (blockpos == null) {
                    return false;
                } else if (!hasLineOfSight(pOwner, blockpos.getCenter()) && !hasLineOfSight(pOwner, blockpos.above(4).getCenter())) {
                    return false;
                } else {
                    pOwner.getBrain().setMemory(MemoryModuleType.BREEZE_JUMP_TARGET, blockpos);
                    return true;
                }
            }
        }
    }

    protected boolean canStillUse(ServerLevel pLevel, Breeze pEntity, long pGameTime) {
        return pEntity.getPose() != Pose.STANDING && !pEntity.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_JUMP_COOLDOWN);
    }

    protected void start(ServerLevel pLevel, Breeze pEntity, long pGameTime) {
        if (pEntity.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_INHALING, MemoryStatus.VALUE_ABSENT)) {
            pEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_INHALING, Unit.INSTANCE, (long)INHALING_DURATION_TICKS);
        }

        pEntity.setPose(Pose.INHALING);
        pEntity.getBrain()
            .getMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
            .ifPresent(p_312818_ -> pEntity.lookAt(EntityAnchorArgument.Anchor.EYES, p_312818_.getCenter()));
    }

    protected void tick(ServerLevel pLevel, Breeze pOwner, long pGameTime) {
        if (finishedInhaling(pOwner)) {
            Vec3 vec3 = pOwner.getBrain()
                .getMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
                .flatMap(p_311796_ -> calculateOptimalJumpVector(pOwner, pOwner.getRandom(), Vec3.atBottomCenterOf(p_311796_)))
                .orElse(null);
            if (vec3 == null) {
                pOwner.setPose(Pose.STANDING);
                return;
            }

            pOwner.playSound(SoundEvents.BREEZE_JUMP, 1.0F, 1.0F);
            pOwner.setPose(Pose.LONG_JUMPING);
            pOwner.setYRot(pOwner.yBodyRot);
            pOwner.setDiscardFriction(true);
            pOwner.setDeltaMovement(vec3);
        } else if (finishedJumping(pOwner)) {
            pOwner.playSound(SoundEvents.BREEZE_LAND, 1.0F, 1.0F);
            pOwner.setPose(Pose.STANDING);
            pOwner.setDiscardFriction(false);
            boolean flag = pOwner.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
            pOwner.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_COOLDOWN, Unit.INSTANCE, flag ? 2L : 10L);
            pOwner.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, 100L);
        }
    }

    protected void stop(ServerLevel pLevel, Breeze pEntity, long pGameTime) {
        if (pEntity.getPose() == Pose.LONG_JUMPING || pEntity.getPose() == Pose.INHALING) {
            pEntity.setPose(Pose.STANDING);
        }

        pEntity.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_TARGET);
        pEntity.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_INHALING);
    }

    private static boolean finishedInhaling(Breeze pBreeze) {
        return pBreeze.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_INHALING).isEmpty() && pBreeze.getPose() == Pose.INHALING;
    }

    private static boolean finishedJumping(Breeze pBreeze) {
        return pBreeze.getPose() == Pose.LONG_JUMPING && pBreeze.onGround();
    }

    private static Vec3 randomPointBehindTarget(LivingEntity pTarget, RandomSource pRandom) {
        int i = 90;
        float f = pTarget.yHeadRot + 180.0F + (float)pRandom.nextGaussian() * 90.0F / 2.0F;
        float f1 = Mth.lerp(pRandom.nextFloat(), 4.0F, 8.0F);
        Vec3 vec3 = Vec3.directionFromRotation(0.0F, f).scale((double)f1);
        return pTarget.position().add(vec3);
    }

    @Nullable
    private static BlockPos snapToSurface(LivingEntity pOwner, Vec3 pTargetPos) {
        ClipContext clipcontext = new ClipContext(
            pTargetPos, pTargetPos.relative(Direction.DOWN, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pOwner
        );
        HitResult hitresult = pOwner.level().clip(clipcontext);
        if (hitresult.getType() == HitResult.Type.BLOCK) {
            return BlockPos.containing(hitresult.getLocation()).above();
        } else {
            ClipContext clipcontext1 = new ClipContext(
                pTargetPos, pTargetPos.relative(Direction.UP, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pOwner
            );
            HitResult hitresult1 = pOwner.level().clip(clipcontext1);
            return hitresult1.getType() == HitResult.Type.BLOCK ? BlockPos.containing(hitresult.getLocation()).above() : null;
        }
    }

    @VisibleForTesting
    public static boolean hasLineOfSight(Breeze pBreeze, Vec3 pTargetPos) {
        Vec3 vec3 = new Vec3(pBreeze.getX(), pBreeze.getY(), pBreeze.getZ());
        if (pTargetPos.distanceTo(vec3) > 50.0) {
            return false;
        } else {
            return pBreeze.level().clip(new ClipContext(vec3, pTargetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pBreeze)).getType()
                == HitResult.Type.MISS;
        }
    }

    private static boolean outOfAggroRange(Breeze pBreeze, LivingEntity pTarget) {
        return !pTarget.closerThan(pBreeze, 24.0);
    }

    private static boolean tooCloseForJump(Breeze pBreeze, LivingEntity pTarget) {
        return pTarget.distanceTo(pBreeze) - 4.0F <= 0.0F;
    }

    private static boolean canJumpFromCurrentPosition(ServerLevel pLevel, Breeze pBreeze) {
        BlockPos blockpos = pBreeze.blockPosition();

        for(int i = 1; i <= 4; ++i) {
            BlockPos blockpos1 = blockpos.relative(Direction.UP, i);
            if (!pLevel.getBlockState(blockpos1).isAir() && !pLevel.getFluidState(blockpos1).is(FluidTags.WATER)) {
                return false;
            }
        }

        return true;
    }

    private static Optional<Vec3> calculateOptimalJumpVector(Breeze pBreeze, RandomSource pRandom, Vec3 pTarget) {
        for(int i : Util.shuffledCopy(ALLOWED_ANGLES, pRandom)) {
            Optional<Vec3> optional = LongJumpUtil.calculateJumpVectorForAngle(pBreeze, pTarget, 1.4F, i, false);
            if (optional.isPresent()) {
                return optional;
            }
        }

        return Optional.empty();
    }
}
