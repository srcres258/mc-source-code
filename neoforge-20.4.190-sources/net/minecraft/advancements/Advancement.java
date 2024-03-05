package net.minecraft.advancements;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.critereon.CriterionValidator;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootDataResolver;

public record Advancement(
    Optional<ResourceLocation> parent,
    Optional<DisplayInfo> display,
    AdvancementRewards rewards,
    Map<String, Criterion<?>> criteria,
    AdvancementRequirements requirements,
    boolean sendsTelemetryEvent,
    Optional<Component> name
) {
    private static final Codec<Map<String, Criterion<?>>> CRITERIA_CODEC = ExtraCodecs.validate(
        Codec.unboundedMap(Codec.STRING, Criterion.CODEC),
        p_311380_ -> p_311380_.isEmpty() ? DataResult.error(() -> "Advancement criteria cannot be empty") : DataResult.success(p_311380_)
    );
    public static final Codec<Advancement> CODEC = ExtraCodecs.validate(
        RecordCodecBuilder.create(
            p_311387_ -> p_311387_.group(
                        ExtraCodecs.strictOptionalField(ResourceLocation.CODEC, "parent").forGetter(Advancement::parent),
                        ExtraCodecs.strictOptionalField(DisplayInfo.CODEC, "display").forGetter(Advancement::display),
                        ExtraCodecs.strictOptionalField(AdvancementRewards.CODEC, "rewards", AdvancementRewards.EMPTY).forGetter(Advancement::rewards),
                        CRITERIA_CODEC.fieldOf("criteria").forGetter(Advancement::criteria),
                        ExtraCodecs.strictOptionalField(AdvancementRequirements.CODEC, "requirements")
                            .forGetter(p_311389_ -> Optional.of(p_311389_.requirements())),
                        ExtraCodecs.strictOptionalField(Codec.BOOL, "sends_telemetry_event", false).forGetter(Advancement::sendsTelemetryEvent)
                    )
                    .apply(p_311387_, (p_311374_, p_311375_, p_311376_, p_311377_, p_311378_, p_311379_) -> {
                        AdvancementRequirements advancementrequirements = p_311378_.orElseGet(() -> AdvancementRequirements.allOf(p_311377_.keySet()));
                        return new Advancement(p_311374_, p_311375_, p_311376_, p_311377_, advancementrequirements, p_311379_);
                    })
        ),
        Advancement::validate
    );
    public static final Codec<Optional<net.neoforged.neoforge.common.conditions.WithConditions<Advancement>>> CONDITIONAL_CODEC = net.neoforged.neoforge.common.conditions.ConditionalOps.createConditionalCodecWithConditions(CODEC);

    public Advancement(
        Optional<ResourceLocation> p_300893_,
        Optional<DisplayInfo> p_301147_,
        AdvancementRewards p_286389_,
        Map<String, Criterion<?>> p_286635_,
        AdvancementRequirements p_301002_,
        boolean p_286478_
    ) {
        this(p_300893_, p_301147_, p_286389_, Map.copyOf(p_286635_), p_301002_, p_286478_, p_301147_.map(Advancement::decorateName));
    }

    private static DataResult<Advancement> validate(Advancement p_312433_) {
        return p_312433_.requirements().validate(p_312433_.criteria().keySet()).map(p_311382_ -> p_312433_);
    }

    private static Component decorateName(DisplayInfo p_301019_) {
        Component component = p_301019_.getTitle();
        ChatFormatting chatformatting = p_301019_.getType().getChatColor();
        Component component1 = ComponentUtils.mergeStyles(component.copy(), Style.EMPTY.withColor(chatformatting))
            .append("\n")
            .append(p_301019_.getDescription());
        Component component2 = component.copy().withStyle(p_138316_ -> p_138316_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component1)));
        return ComponentUtils.wrapInSquareBrackets(component2).withStyle(chatformatting);
    }

    public static Component name(AdvancementHolder pAdvancement) {
        return pAdvancement.value().name().orElseGet(() -> Component.literal(pAdvancement.id().toString()));
    }

    public void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeOptional(this.parent, FriendlyByteBuf::writeResourceLocation);
        pBuffer.writeOptional(this.display, (p_300647_, p_300648_) -> p_300648_.serializeToNetwork(p_300647_));
        this.requirements.write(pBuffer);
        pBuffer.writeBoolean(this.sendsTelemetryEvent);
    }

    public static Advancement read(FriendlyByteBuf pBuffer) {
        return new Advancement(
            pBuffer.readOptional(FriendlyByteBuf::readResourceLocation),
            pBuffer.readOptional(DisplayInfo::fromNetwork),
            AdvancementRewards.EMPTY,
            Map.of(),
            new AdvancementRequirements(pBuffer),
            pBuffer.readBoolean()
        );
    }

    public boolean isRoot() {
        return this.parent.isEmpty();
    }

    public void validate(ProblemReporter pProblemReporter, LootDataResolver pLootDataResolver) {
        this.criteria.forEach((p_311385_, p_311386_) -> {
            CriterionValidator criterionvalidator = new CriterionValidator(pProblemReporter.forChild(p_311385_), pLootDataResolver);
            p_311386_.triggerInstance().validate(criterionvalidator);
        });
    }

    public static class Builder implements net.neoforged.neoforge.common.extensions.IAdvancementBuilderExtension {
        private Optional<ResourceLocation> parent = Optional.empty();
        private Optional<DisplayInfo> display = Optional.empty();
        private AdvancementRewards rewards = AdvancementRewards.EMPTY;
        private final ImmutableMap.Builder<String, Criterion<?>> criteria = ImmutableMap.builder();
        private Optional<AdvancementRequirements> requirements = Optional.empty();
        private AdvancementRequirements.Strategy requirementsStrategy = AdvancementRequirements.Strategy.AND;
        private boolean sendsTelemetryEvent;

        public static Advancement.Builder advancement() {
            return new Advancement.Builder().sendsTelemetryEvent();
        }

        public static Advancement.Builder recipeAdvancement() {
            return new Advancement.Builder();
        }

        public Advancement.Builder parent(AdvancementHolder pParent) {
            this.parent = Optional.of(pParent.id());
            return this;
        }

        @Deprecated(
            forRemoval = true
        )
        public Advancement.Builder parent(ResourceLocation pParentId) {
            this.parent = Optional.of(pParentId);
            return this;
        }

        public Advancement.Builder display(
            ItemStack pIcon,
            Component pTitle,
            Component pDescription,
            @Nullable ResourceLocation pBackground,
            AdvancementType pType,
            boolean pShowToast,
            boolean pAnnounceChat,
            boolean pHidden
        ) {
            return this.display(new DisplayInfo(pIcon, pTitle, pDescription, Optional.ofNullable(pBackground), pType, pShowToast, pAnnounceChat, pHidden));
        }

        public Advancement.Builder display(
            ItemLike pIcon,
            Component pTitle,
            Component pDescription,
            @Nullable ResourceLocation pBackground,
            AdvancementType pType,
            boolean pShowToast,
            boolean pAnnounceChat,
            boolean pHidden
        ) {
            return this.display(
                new DisplayInfo(
                    new ItemStack(pIcon.asItem()), pTitle, pDescription, Optional.ofNullable(pBackground), pType, pShowToast, pAnnounceChat, pHidden
                )
            );
        }

        public Advancement.Builder display(DisplayInfo pDisplay) {
            this.display = Optional.of(pDisplay);
            return this;
        }

        public Advancement.Builder rewards(AdvancementRewards.Builder pRewardsBuilder) {
            return this.rewards(pRewardsBuilder.build());
        }

        public Advancement.Builder rewards(AdvancementRewards pRewards) {
            this.rewards = pRewards;
            return this;
        }

        public Advancement.Builder addCriterion(String pKey, Criterion<?> pCriterion) {
            this.criteria.put(pKey, pCriterion);
            return this;
        }

        public Advancement.Builder requirements(AdvancementRequirements.Strategy pRequirementsStrategy) {
            this.requirementsStrategy = pRequirementsStrategy;
            return this;
        }

        public Advancement.Builder requirements(AdvancementRequirements pRequirements) {
            this.requirements = Optional.of(pRequirements);
            return this;
        }

        public Advancement.Builder sendsTelemetryEvent() {
            this.sendsTelemetryEvent = true;
            return this;
        }

        public AdvancementHolder build(ResourceLocation pId) {
            Map<String, Criterion<?>> map = this.criteria.buildOrThrow();
            AdvancementRequirements advancementrequirements = this.requirements.orElseGet(() -> this.requirementsStrategy.create(map.keySet()));
            return new AdvancementHolder(
                pId, new Advancement(this.parent, this.display, this.rewards, map, advancementrequirements, this.sendsTelemetryEvent)
            );
        }

        public AdvancementHolder save(Consumer<AdvancementHolder> pOutput, String pId) {
            AdvancementHolder advancementholder = this.build(new ResourceLocation(pId));
            pOutput.accept(advancementholder);
            return advancementholder;
        }
    }
}
