package net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;

public class AppendStatic implements RuleBlockEntityModifier {
    public static final Codec<AppendStatic> CODEC = RecordCodecBuilder.create(
        p_277505_ -> p_277505_.group(CompoundTag.CODEC.fieldOf("data").forGetter(p_278105_ -> p_278105_.tag)).apply(p_277505_, AppendStatic::new)
    );
    private final CompoundTag tag;

    public AppendStatic(CompoundTag p_277900_) {
        this.tag = p_277900_;
    }

    @Override
    public CompoundTag apply(RandomSource pRandom, @Nullable CompoundTag pTag) {
        return pTag == null ? this.tag.copy() : pTag.merge(this.tag);
    }

    @Override
    public RuleBlockEntityModifierType<?> getType() {
        return RuleBlockEntityModifierType.APPEND_STATIC;
    }
}
