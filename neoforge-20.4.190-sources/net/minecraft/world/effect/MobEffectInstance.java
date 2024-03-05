package net.minecraft.world.effect;

import com.google.common.collect.ComparisonChain;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;

public class MobEffectInstance implements Comparable<MobEffectInstance> {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int INFINITE_DURATION = -1;
    private static final String TAG_ID = "id";
    private static final String TAG_AMBIENT = "ambient";
    private static final String TAG_HIDDEN_EFFECT = "hidden_effect";
    private static final String TAG_AMPLIFIER = "amplifier";
    private static final String TAG_DURATION = "duration";
    private static final String TAG_SHOW_PARTICLES = "show_particles";
    private static final String TAG_SHOW_ICON = "show_icon";
    private static final String TAG_FACTOR_CALCULATION_DATA = "factor_calculation_data";
    private final MobEffect effect;
    private int duration;
    private int amplifier;
    private boolean ambient;
    private boolean visible;
    private boolean showIcon;
    /**
     * A hidden effect which is not shown to the player.
     */
    @Nullable
    private MobEffectInstance hiddenEffect;
    private final Optional<MobEffectInstance.FactorData> factorData;

    public MobEffectInstance(MobEffect pEffect) {
        this(pEffect, 0, 0);
    }

    public MobEffectInstance(MobEffect pEffect, int pDuration) {
        this(pEffect, pDuration, 0);
    }

    public MobEffectInstance(MobEffect pEffect, int pDuration, int pAmplifier) {
        this(pEffect, pDuration, pAmplifier, false, true);
    }

    public MobEffectInstance(MobEffect pEffect, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible) {
        this(pEffect, pDuration, pAmplifier, pAmbient, pVisible, pVisible);
    }

    public MobEffectInstance(MobEffect pEffect, int pDuration, int pAmplifier, boolean pAmbient, boolean pVisible, boolean pShowIcon) {
        this(pEffect, pDuration, pAmplifier, pAmbient, pVisible, pShowIcon, null, pEffect.createFactorData());
    }

    public MobEffectInstance(
        MobEffect pEffect,
        int pDuration,
        int pAmplifier,
        boolean pAmbient,
        boolean pVisible,
        boolean pShowIcon,
        @Nullable MobEffectInstance pHiddenEffect,
        Optional<MobEffectInstance.FactorData> pFactorData
    ) {
        this.effect = pEffect;
        this.duration = pDuration;
        this.amplifier = pAmplifier;
        this.ambient = pAmbient;
        this.visible = pVisible;
        this.showIcon = pShowIcon;
        this.hiddenEffect = pHiddenEffect;
        this.factorData = pFactorData;
        this.effect.fillEffectCures(this.cures, this);
    }

    public MobEffectInstance(MobEffectInstance pOther) {
        this.effect = pOther.effect;
        this.factorData = this.effect.createFactorData();
        this.setDetailsFrom(pOther);
    }

    public Optional<MobEffectInstance.FactorData> getFactorData() {
        return this.factorData;
    }

    void setDetailsFrom(MobEffectInstance pEffectInstance) {
        this.duration = pEffectInstance.duration;
        this.amplifier = pEffectInstance.amplifier;
        this.ambient = pEffectInstance.ambient;
        this.visible = pEffectInstance.visible;
        this.showIcon = pEffectInstance.showIcon;
        this.cures.clear();
        this.cures.addAll(pEffectInstance.cures);
    }

    public boolean update(MobEffectInstance pOther) {
        if (this.effect != pOther.effect) {
            LOGGER.warn("This method should only be called for matching effects!");
        }

        boolean flag = false;
        if (pOther.amplifier > this.amplifier) {
            if (pOther.isShorterDurationThan(this)) {
                MobEffectInstance mobeffectinstance = this.hiddenEffect;
                this.hiddenEffect = new MobEffectInstance(this);
                this.hiddenEffect.hiddenEffect = mobeffectinstance;
            }

            this.amplifier = pOther.amplifier;
            this.duration = pOther.duration;
            flag = true;
        } else if (this.isShorterDurationThan(pOther)) {
            if (pOther.amplifier == this.amplifier) {
                this.duration = pOther.duration;
                flag = true;
            } else if (this.hiddenEffect == null) {
                this.hiddenEffect = new MobEffectInstance(pOther);
            } else {
                this.hiddenEffect.update(pOther);
            }
        }

        if (!pOther.ambient && this.ambient || flag) {
            this.ambient = pOther.ambient;
            flag = true;
        }

        if (pOther.visible != this.visible) {
            this.visible = pOther.visible;
            flag = true;
        }

        if (pOther.showIcon != this.showIcon) {
            this.showIcon = pOther.showIcon;
            flag = true;
        }

        return flag;
    }

    private boolean isShorterDurationThan(MobEffectInstance pOther) {
        return !this.isInfiniteDuration() && (this.duration < pOther.duration || pOther.isInfiniteDuration());
    }

    public boolean isInfiniteDuration() {
        return this.duration == -1;
    }

    public boolean endsWithin(int pDuration) {
        return !this.isInfiniteDuration() && this.duration <= pDuration;
    }

    public int mapDuration(Int2IntFunction pMapper) {
        return !this.isInfiniteDuration() && this.duration != 0 ? pMapper.applyAsInt(this.duration) : this.duration;
    }

    public MobEffect getEffect() {
        return this.effect;
    }

    public int getDuration() {
        return this.duration;
    }

    public int getAmplifier() {
        return this.amplifier;
    }

    /**
     * Gets whether this potion effect originated from a beacon
     */
    public boolean isAmbient() {
        return this.ambient;
    }

    /**
     * Gets whether this potion effect will show ambient particles or not.
     */
    public boolean isVisible() {
        return this.visible;
    }

    public boolean showIcon() {
        return this.showIcon;
    }

    public boolean tick(LivingEntity pEntity, Runnable pOnExpirationRunnable) {
        if (this.hasRemainingDuration()) {
            int i = this.isInfiniteDuration() ? pEntity.tickCount : this.duration;
            if (this.effect.shouldApplyEffectTickThisTick(i, this.amplifier)) {
                this.effect.applyEffectTick(pEntity, this.amplifier);
            }

            this.tickDownDuration();
            if (this.duration == 0 && this.hiddenEffect != null) {
                this.setDetailsFrom(this.hiddenEffect);
                this.hiddenEffect = this.hiddenEffect.hiddenEffect;
                pOnExpirationRunnable.run();
            }
        }

        this.factorData.ifPresent(p_267917_ -> p_267917_.tick(this));
        return this.hasRemainingDuration();
    }

    private boolean hasRemainingDuration() {
        return this.isInfiniteDuration() || this.duration > 0;
    }

    private int tickDownDuration() {
        if (this.hiddenEffect != null) {
            this.hiddenEffect.tickDownDuration();
        }

        return this.duration = this.mapDuration(p_267916_ -> p_267916_ - 1);
    }

    public void onEffectStarted(LivingEntity pEntity) {
        this.effect.onEffectStarted(pEntity, this.amplifier);
    }

    public String getDescriptionId() {
        return this.effect.getDescriptionId();
    }

    @Override
    public String toString() {
        String s;
        if (this.amplifier > 0) {
            s = this.getDescriptionId() + " x " + (this.amplifier + 1) + ", Duration: " + this.describeDuration();
        } else {
            s = this.getDescriptionId() + ", Duration: " + this.describeDuration();
        }

        if (!this.visible) {
            s = s + ", Particles: false";
        }

        if (!this.showIcon) {
            s = s + ", Show Icon: false";
        }

        return s;
    }

    private String describeDuration() {
        return this.isInfiniteDuration() ? "infinite" : Integer.toString(this.duration);
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else if (!(pOther instanceof MobEffectInstance)) {
            return false;
        } else {
            MobEffectInstance mobeffectinstance = (MobEffectInstance)pOther;
            return this.duration == mobeffectinstance.duration
                && this.amplifier == mobeffectinstance.amplifier
                && this.ambient == mobeffectinstance.ambient
                && this.effect.equals(mobeffectinstance.effect);
        }
    }

    @Override
    public int hashCode() {
        int i = this.effect.hashCode();
        i = 31 * i + this.duration;
        i = 31 * i + this.amplifier;
        return 31 * i + (this.ambient ? 1 : 0);
    }

    /**
     * Write a custom potion effect to a potion item's NBT data.
     */
    public CompoundTag save(CompoundTag pNbt) {
        ResourceLocation resourcelocation = BuiltInRegistries.MOB_EFFECT.getKey(this.effect);
        pNbt.putString("id", resourcelocation.toString());
        net.neoforged.neoforge.common.CommonHooks.saveMobEffect(pNbt, "neoforge:id", this.getEffect());
        this.writeDetailsTo(pNbt);
        return pNbt;
    }

    private void writeDetailsTo(CompoundTag pNbt) {
        pNbt.putByte("amplifier", (byte)this.getAmplifier());
        pNbt.putInt("duration", this.getDuration());
        pNbt.putBoolean("ambient", this.isAmbient());
        pNbt.putBoolean("show_particles", this.isVisible());
        pNbt.putBoolean("show_icon", this.showIcon());
        if (this.hiddenEffect != null) {
            CompoundTag compoundtag = new CompoundTag();
            this.hiddenEffect.save(compoundtag);
            pNbt.put("hidden_effect", compoundtag);
        }

        this.factorData
            .ifPresent(
                p_216903_ -> MobEffectInstance.FactorData.CODEC
                        .encodeStart(NbtOps.INSTANCE, p_216903_)
                        .resultOrPartial(LOGGER::error)
                        .ifPresent(p_216906_ -> pNbt.put("factor_calculation_data", p_216906_))
            );

        writeCures(pNbt);
    }

    /**
     * Read a custom potion effect from a potion item's NBT data.
     */
    @Nullable
    public static MobEffectInstance load(CompoundTag pNbt) {
        String s = pNbt.getString("id");
        MobEffect mobeffect = BuiltInRegistries.MOB_EFFECT.get(ResourceLocation.tryParse(s));
        return mobeffect == null ? null : loadSpecifiedEffect(mobeffect, pNbt);
    }

    private static MobEffectInstance loadSpecifiedEffect(MobEffect pEffect, CompoundTag pNbt) {
        int i = pNbt.getByte("amplifier");
        int j = pNbt.getInt("duration");
        boolean flag = pNbt.getBoolean("ambient");
        boolean flag1 = true;
        if (pNbt.contains("show_particles", 1)) {
            flag1 = pNbt.getBoolean("show_particles");
        }

        boolean flag2 = flag1;
        if (pNbt.contains("show_icon", 1)) {
            flag2 = pNbt.getBoolean("show_icon");
        }

        MobEffectInstance mobeffectinstance = null;
        if (pNbt.contains("hidden_effect", 10)) {
            mobeffectinstance = loadSpecifiedEffect(pEffect, pNbt.getCompound("hidden_effect"));
        }

        Optional<MobEffectInstance.FactorData> optional;
        if (pNbt.contains("factor_calculation_data", 10)) {
            optional = MobEffectInstance.FactorData.CODEC
                .parse(new Dynamic<>(NbtOps.INSTANCE, pNbt.getCompound("factor_calculation_data")))
                .resultOrPartial(LOGGER::error);
        } else {
            optional = Optional.empty();
        }

        return new MobEffectInstance(pEffect, j, Math.max(i, 0), flag, flag1, flag2, mobeffectinstance, optional).readCures(pNbt);
    }

    public int compareTo(MobEffectInstance pOther) {
        int i = 32147;
        return (this.getDuration() <= 32147 || pOther.getDuration() <= 32147) && (!this.isAmbient() || !pOther.isAmbient())
            ? ComparisonChain.start()
                .compareFalseFirst(this.isAmbient(), pOther.isAmbient())
                .compareFalseFirst(this.isInfiniteDuration(), pOther.isInfiniteDuration())
                .compare(this.getDuration(), pOther.getDuration())
                .compare(this.getEffect().getSortOrder(this), pOther.getEffect().getSortOrder(pOther))
                .result()
            : ComparisonChain.start()
                .compare(this.isAmbient(), pOther.isAmbient())
                .compare(this.getEffect().getSortOrder(this), pOther.getEffect().getSortOrder(pOther))
                .result();
    }

    private final java.util.Set<net.neoforged.neoforge.common.EffectCure> cures = com.google.common.collect.Sets.newIdentityHashSet();

    /**
     * {@return the {@link net.neoforged.neoforge.common.EffectCure}s which can cure the {@link MobEffect} held by this {@link MobEffectInstance}}
     */
    public java.util.Set<net.neoforged.neoforge.common.EffectCure> getCures() {
        return cures;
    }

    private MobEffectInstance readCures(CompoundTag tag) {
        cures.clear(); // Overwrite cures with ones stored in NBT, if any
        if (tag.contains("neoforge:cures", Tag.TAG_LIST)) {
            net.minecraft.nbt.ListTag list = tag.getList("neoforge:cures", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                cures.add(net.neoforged.neoforge.common.EffectCure.get(list.getString(i)));
            }
        }
        return this;
    }

    private void writeCures(CompoundTag tag) {
        if (!cures.isEmpty()) {
            net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
            for (net.neoforged.neoforge.common.EffectCure cure : cures) {
                list.add(net.minecraft.nbt.StringTag.valueOf(cure.name()));
            }
            tag.put("neoforge:cures", list);
        }
    }

    public static class FactorData {
        public static final Codec<MobEffectInstance.FactorData> CODEC = RecordCodecBuilder.create(
            p_216933_ -> p_216933_.group(
                        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("padding_duration").forGetter(p_216945_ -> p_216945_.paddingDuration),
                        Codec.FLOAT.fieldOf("factor_start").orElse(0.0F).forGetter(p_216943_ -> p_216943_.factorStart),
                        Codec.FLOAT.fieldOf("factor_target").orElse(1.0F).forGetter(p_216941_ -> p_216941_.factorTarget),
                        Codec.FLOAT.fieldOf("factor_current").orElse(0.0F).forGetter(p_216939_ -> p_216939_.factorCurrent),
                        ExtraCodecs.NON_NEGATIVE_INT.fieldOf("ticks_active").orElse(0).forGetter(p_267918_ -> p_267918_.ticksActive),
                        Codec.FLOAT.fieldOf("factor_previous_frame").orElse(0.0F).forGetter(p_216935_ -> p_216935_.factorPreviousFrame),
                        Codec.BOOL.fieldOf("had_effect_last_tick").orElse(false).forGetter(p_216929_ -> p_216929_.hadEffectLastTick)
                    )
                    .apply(p_216933_, MobEffectInstance.FactorData::new)
        );
        private final int paddingDuration;
        private float factorStart;
        private float factorTarget;
        private float factorCurrent;
        private int ticksActive;
        private float factorPreviousFrame;
        private boolean hadEffectLastTick;

        public FactorData(int p_216919_, float p_216920_, float p_216921_, float p_216922_, int p_216923_, float p_216924_, boolean p_216925_) {
            this.paddingDuration = p_216919_;
            this.factorStart = p_216920_;
            this.factorTarget = p_216921_;
            this.factorCurrent = p_216922_;
            this.ticksActive = p_216923_;
            this.factorPreviousFrame = p_216924_;
            this.hadEffectLastTick = p_216925_;
        }

        public FactorData(int pPaddingDuration) {
            this(pPaddingDuration, 0.0F, 1.0F, 0.0F, 0, 0.0F, false);
        }

        public void tick(MobEffectInstance pEffect) {
            this.factorPreviousFrame = this.factorCurrent;
            boolean flag = !pEffect.endsWithin(this.paddingDuration);
            ++this.ticksActive;
            if (this.hadEffectLastTick != flag) {
                this.hadEffectLastTick = flag;
                this.ticksActive = 0;
                this.factorStart = this.factorCurrent;
                this.factorTarget = flag ? 1.0F : 0.0F;
            }

            float f = Mth.clamp((float)this.ticksActive / (float)this.paddingDuration, 0.0F, 1.0F);
            this.factorCurrent = Mth.lerp(f, this.factorStart, this.factorTarget);
        }

        public float getFactor(LivingEntity pEntity, float pPartialTick) {
            if (pEntity.isRemoved()) {
                this.factorPreviousFrame = this.factorCurrent;
            }

            return Mth.lerp(pPartialTick, this.factorPreviousFrame, this.factorCurrent);
        }
    }
}
