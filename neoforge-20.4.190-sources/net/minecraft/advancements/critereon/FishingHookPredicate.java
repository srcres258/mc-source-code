package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;

public record FishingHookPredicate(Optional<Boolean> inOpenWater) implements EntitySubPredicate {
    public static final FishingHookPredicate ANY = new FishingHookPredicate(Optional.empty());
    public static final MapCodec<FishingHookPredicate> CODEC = RecordCodecBuilder.mapCodec(
        p_298351_ -> p_298351_.group(ExtraCodecs.strictOptionalField(Codec.BOOL, "in_open_water").forGetter(FishingHookPredicate::inOpenWater))
                .apply(p_298351_, FishingHookPredicate::new)
    );

    public static FishingHookPredicate inOpenWater(boolean pInOpenWater) {
        return new FishingHookPredicate(Optional.of(pInOpenWater));
    }

    @Override
    public EntitySubPredicate.Type type() {
        return EntitySubPredicate.Types.FISHING_HOOK;
    }

    @Override
    public boolean matches(Entity pEntity, ServerLevel pLevel, @Nullable Vec3 pPosition) {
        if (this.inOpenWater.isEmpty()) {
            return true;
        } else if (pEntity instanceof FishingHook fishinghook) {
            return this.inOpenWater.get() == fishinghook.isOpenWaterFishing();
        } else {
            return false;
        }
    }
}
