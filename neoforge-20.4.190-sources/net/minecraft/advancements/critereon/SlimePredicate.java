package net.minecraft.advancements.critereon;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.phys.Vec3;

public record SlimePredicate(MinMaxBounds.Ints size) implements EntitySubPredicate {
    public static final MapCodec<SlimePredicate> CODEC = RecordCodecBuilder.mapCodec(
        p_298436_ -> p_298436_.group(ExtraCodecs.strictOptionalField(MinMaxBounds.Ints.CODEC, "size", MinMaxBounds.Ints.ANY).forGetter(SlimePredicate::size))
                .apply(p_298436_, SlimePredicate::new)
    );

    public static SlimePredicate sized(MinMaxBounds.Ints pSize) {
        return new SlimePredicate(pSize);
    }

    @Override
    public boolean matches(Entity pEntity, ServerLevel pLevel, @Nullable Vec3 pPosition) {
        return pEntity instanceof Slime slime ? this.size.matches(slime.getSize()) : false;
    }

    @Override
    public EntitySubPredicate.Type type() {
        return EntitySubPredicate.Types.SLIME;
    }
}
