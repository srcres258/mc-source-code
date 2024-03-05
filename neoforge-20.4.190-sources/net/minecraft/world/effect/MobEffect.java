package net.minecraft.world.effect;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class MobEffect implements net.neoforged.neoforge.common.extensions.IMobEffectExtension {
    /**
     * Contains a Map of the AttributeModifiers registered by potions
     */
    private final Map<Attribute, AttributeModifierTemplate> attributeModifiers = Maps.newHashMap();
    private final MobEffectCategory category;
    private final int color;
    @Nullable
    private String descriptionId;
    private Supplier<MobEffectInstance.FactorData> factorDataFactory = () -> null;
    private final Holder.Reference<MobEffect> builtInRegistryHolder = BuiltInRegistries.MOB_EFFECT.createIntrusiveHolder(this);

    protected MobEffect(MobEffectCategory pCategory, int pColor) {
        this.category = pCategory;
        this.color = pColor;
        initClient();
    }

    public Optional<MobEffectInstance.FactorData> createFactorData() {
        return Optional.ofNullable(this.factorDataFactory.get());
    }

    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
    }

    public void applyInstantenousEffect(@Nullable Entity pSource, @Nullable Entity pIndirectSource, LivingEntity pLivingEntity, int pAmplifier, double pHealth) {
        this.applyEffectTick(pLivingEntity, pAmplifier);
    }

    public boolean shouldApplyEffectTickThisTick(int pDuration, int pAmplifier) {
        return false;
    }

    public void onEffectStarted(LivingEntity pLivingEntity, int pAmplifier) {
    }

    /**
     * Returns {@code true} if the potion has an instant effect instead of a continuous one (e.g. Harming)
     */
    public boolean isInstantenous() {
        return false;
    }

    protected String getOrCreateDescriptionId() {
        if (this.descriptionId == null) {
            this.descriptionId = Util.makeDescriptionId("effect", BuiltInRegistries.MOB_EFFECT.getKey(this));
        }

        return this.descriptionId;
    }

    /**
     * Returns the name of the effect.
     */
    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }

    public Component getDisplayName() {
        return Component.translatable(this.getDescriptionId());
    }

    public MobEffectCategory getCategory() {
        return this.category;
    }

    /**
     * Returns the color of the potion liquid.
     */
    public int getColor() {
        return this.color;
    }

    /**
     * Adds an attribute modifier to this effect. This method can be called for more than one attribute. The attributes are applied to an entity when the potion effect is active and removed when it stops.
     */
    public MobEffect addAttributeModifier(Attribute pAttribute, String pUuid, double pAmount, AttributeModifier.Operation pOperation) {
        this.attributeModifiers.put(pAttribute, new MobEffect.MobEffectAttributeModifierTemplate(UUID.fromString(pUuid), pAmount, pOperation));
        return this;
    }

    public MobEffect setFactorDataFactory(Supplier<MobEffectInstance.FactorData> pFactorDataFactory) {
        this.factorDataFactory = pFactorDataFactory;
        return this;
    }

    public Map<Attribute, AttributeModifierTemplate> getAttributeModifiers() {
        return this.attributeModifiers;
    }

    public void removeAttributeModifiers(AttributeMap pAttributeMap) {
        for(Entry<Attribute, AttributeModifierTemplate> entry : this.attributeModifiers.entrySet()) {
            AttributeInstance attributeinstance = pAttributeMap.getInstance(entry.getKey());
            if (attributeinstance != null) {
                attributeinstance.removeModifier(entry.getValue().getAttributeModifierId());
            }
        }
    }

    public void addAttributeModifiers(AttributeMap pAttributeMap, int pAmplifier) {
        for(Entry<Attribute, AttributeModifierTemplate> entry : this.attributeModifiers.entrySet()) {
            AttributeInstance attributeinstance = pAttributeMap.getInstance(entry.getKey());
            if (attributeinstance != null) {
                attributeinstance.removeModifier(entry.getValue().getAttributeModifierId());
                attributeinstance.addPermanentModifier(entry.getValue().create(pAmplifier));
            }
        }
    }

    /**
     * Get if the potion is beneficial to the player. Beneficial potions are shown on the first row of the HUD
     */
    public boolean isBeneficial() {
        return this.category == MobEffectCategory.BENEFICIAL;
    }

    // FORGE START
    private Object effectRenderer;

    /*
        DO NOT CALL, IT WILL DISAPPEAR IN THE FUTURE
        Call RenderProperties.getEffectRenderer instead
     */
    public Object getEffectRendererInternal() {
        return effectRenderer;
    }

    private void initClient() {
        // Minecraft instance isn't available in datagen, so don't call initializeClient if in datagen
        if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT && !net.neoforged.fml.loading.FMLLoader.getLaunchHandler().isData()) {
            initializeClient(properties -> {
                this.effectRenderer = properties;
            });
        }
    }

    public void initializeClient(java.util.function.Consumer<net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions> consumer) {
    }
    // END FORGE


    @Deprecated
    public Holder.Reference<MobEffect> builtInRegistryHolder() {
        return this.builtInRegistryHolder;
    }

    class MobEffectAttributeModifierTemplate implements AttributeModifierTemplate {
        private final UUID id;
        private final double amount;
        private final AttributeModifier.Operation operation;

        public MobEffectAttributeModifierTemplate(UUID pId, double pAmount, AttributeModifier.Operation pOperation) {
            this.id = pId;
            this.amount = pAmount;
            this.operation = pOperation;
        }

        @Override
        public UUID getAttributeModifierId() {
            return this.id;
        }

        @Override
        public AttributeModifier create(int pAmplifier) {
            return new AttributeModifier(this.id, MobEffect.this.getDescriptionId() + " " + pAmplifier, this.amount * (double)(pAmplifier + 1), this.operation);
        }
    }
}
