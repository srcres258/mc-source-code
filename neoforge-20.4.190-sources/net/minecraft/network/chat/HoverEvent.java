package net.minecraft.network.chat;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class HoverEvent {
    public static final Codec<HoverEvent> CODEC = Codec.either(HoverEvent.TypedHoverEvent.CODEC.codec(), HoverEvent.TypedHoverEvent.LEGACY_CODEC.codec())
        .xmap(
            p_304432_ -> new HoverEvent(
                    p_304432_.map(
                        (Function<? super HoverEvent.TypedHoverEvent<?>, ? extends HoverEvent.TypedHoverEvent<?>>)(p_304707_ -> p_304707_),
                        (Function<? super HoverEvent.TypedHoverEvent<?>, ? extends HoverEvent.TypedHoverEvent<?>>)(p_304549_ -> p_304549_)
                    )
                ),
            p_304867_ -> Either.left(p_304867_.event)
        );
    private final HoverEvent.TypedHoverEvent<?> event;

    public <T> HoverEvent(HoverEvent.Action<T> pAction, T pValue) {
        this(new HoverEvent.TypedHoverEvent<>(pAction, pValue));
    }

    private HoverEvent(HoverEvent.TypedHoverEvent<?> pEvent) {
        this.event = pEvent;
    }

    /**
     * Gets the action to perform when this event is raised.
     */
    public HoverEvent.Action<?> getAction() {
        return this.event.action;
    }

    @Nullable
    public <T> T getValue(HoverEvent.Action<T> pActionType) {
        return this.event.action == pActionType ? pActionType.cast(this.event.value) : null;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return pOther != null && this.getClass() == pOther.getClass() ? ((HoverEvent)pOther).event.equals(this.event) : false;
        }
    }

    @Override
    public String toString() {
        return this.event.toString();
    }

    @Override
    public int hashCode() {
        return this.event.hashCode();
    }

    public static class Action<T> implements StringRepresentable {
        public static final HoverEvent.Action<Component> SHOW_TEXT = new HoverEvent.Action<>(
            "show_text", true, ComponentSerialization.CODEC, DataResult::success
        );
        public static final HoverEvent.Action<HoverEvent.ItemStackInfo> SHOW_ITEM = new HoverEvent.Action<>(
            "show_item", true, HoverEvent.ItemStackInfo.CODEC, HoverEvent.ItemStackInfo::legacyCreate
        );
        public static final HoverEvent.Action<HoverEvent.EntityTooltipInfo> SHOW_ENTITY = new HoverEvent.Action<>(
            "show_entity", true, HoverEvent.EntityTooltipInfo.CODEC, HoverEvent.EntityTooltipInfo::legacyCreate
        );
        public static final Codec<HoverEvent.Action<?>> UNSAFE_CODEC = StringRepresentable.fromValues(
            () -> new HoverEvent.Action[]{SHOW_TEXT, SHOW_ITEM, SHOW_ENTITY}
        );
        public static final Codec<HoverEvent.Action<?>> CODEC = ExtraCodecs.validate(UNSAFE_CODEC, HoverEvent.Action::filterForSerialization);
        private final String name;
        private final boolean allowFromServer;
        final Codec<HoverEvent.TypedHoverEvent<T>> codec;
        final Codec<HoverEvent.TypedHoverEvent<T>> legacyCodec;

        public Action(String pName, boolean pAllowFromServer, Codec<T> pCodec, Function<Component, DataResult<T>> p_130844_) {
            this.name = pName;
            this.allowFromServer = pAllowFromServer;
            this.codec = pCodec.xmap(p_304162_ -> new HoverEvent.TypedHoverEvent<>(this, p_304162_), p_304164_ -> p_304164_.value)
                .fieldOf("contents")
                .codec();
            this.legacyCodec = Codec.of(
                Encoder.error("Can't encode in legacy format"),
                ComponentSerialization.CODEC.flatMap(p_130844_).map(p_304161_ -> new HoverEvent.TypedHoverEvent<>(this, p_304161_))
            );
        }

        /**
         * Indicates whether this event can be run from chat text.
         */
        public boolean isAllowedFromServer() {
            return this.allowFromServer;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        T cast(Object pParameter) {
            return (T)pParameter;
        }

        @Override
        public String toString() {
            return "<action " + this.name + ">";
        }

        private static DataResult<HoverEvent.Action<?>> filterForSerialization(@Nullable HoverEvent.Action<?> p_304784_) {
            if (p_304784_ == null) {
                return DataResult.error(() -> "Unknown action");
            } else {
                return !p_304784_.isAllowedFromServer()
                    ? DataResult.error(() -> "Action not allowed: " + p_304784_)
                    : DataResult.success(p_304784_, Lifecycle.stable());
            }
        }
    }

    public static class EntityTooltipInfo {
        public static final Codec<HoverEvent.EntityTooltipInfo> CODEC = RecordCodecBuilder.create(
            p_304449_ -> p_304449_.group(
                        BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("type").forGetter(p_304417_ -> p_304417_.type),
                        UUIDUtil.LENIENT_CODEC.fieldOf("id").forGetter(p_304877_ -> p_304877_.id),
                        ExtraCodecs.strictOptionalField(ComponentSerialization.CODEC, "name").forGetter(p_304585_ -> p_304585_.name)
                    )
                    .apply(p_304449_, HoverEvent.EntityTooltipInfo::new)
        );
        public final EntityType<?> type;
        public final UUID id;
        public final Optional<Component> name;
        @Nullable
        private List<Component> linesCache;

        public EntityTooltipInfo(EntityType<?> pType, UUID pId, @Nullable Component pName) {
            this(pType, pId, Optional.ofNullable(pName));
        }

        public EntityTooltipInfo(EntityType<?> p_304581_, UUID p_304712_, Optional<Component> p_304973_) {
            this.type = p_304581_;
            this.id = p_304712_;
            this.name = p_304973_;
        }

        public static DataResult<HoverEvent.EntityTooltipInfo> legacyCreate(Component pComponent) {
            try {
                CompoundTag compoundtag = TagParser.parseTag(pComponent.getString());
                Component component = Component.Serializer.fromJson(compoundtag.getString("name"));
                EntityType<?> entitytype = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(compoundtag.getString("type")));
                UUID uuid = UUID.fromString(compoundtag.getString("id"));
                return DataResult.success(new HoverEvent.EntityTooltipInfo(entitytype, uuid, component));
            } catch (Exception exception) {
                return DataResult.error(() -> "Failed to parse tooltip: " + exception.getMessage());
            }
        }

        public List<Component> getTooltipLines() {
            if (this.linesCache == null) {
                this.linesCache = new ArrayList<>();
                this.name.ifPresent(this.linesCache::add);
                this.linesCache.add(Component.translatable("gui.entity_tooltip.type", this.type.getDescription()));
                this.linesCache.add(Component.literal(this.id.toString()));
            }

            return this.linesCache;
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else if (pOther != null && this.getClass() == pOther.getClass()) {
                HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo = (HoverEvent.EntityTooltipInfo)pOther;
                return this.type.equals(hoverevent$entitytooltipinfo.type)
                    && this.id.equals(hoverevent$entitytooltipinfo.id)
                    && this.name.equals(hoverevent$entitytooltipinfo.name);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.type.hashCode();
            i = 31 * i + this.id.hashCode();
            return 31 * i + this.name.hashCode();
        }
    }

    public static class ItemStackInfo {
        public static final Codec<HoverEvent.ItemStackInfo> FULL_CODEC = RecordCodecBuilder.create(
            p_304913_ -> p_304913_.group(
                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(p_304924_ -> p_304924_.item),
                        ExtraCodecs.strictOptionalField(Codec.INT, "count", 1).forGetter(p_304807_ -> p_304807_.count),
                        ExtraCodecs.strictOptionalField(TagParser.AS_CODEC, "tag").forGetter(p_304756_ -> p_304756_.tag)
                    )
                    .apply(p_304913_, HoverEvent.ItemStackInfo::new)
        );
        public static final Codec<HoverEvent.ItemStackInfo> CODEC = Codec.either(BuiltInRegistries.ITEM.byNameCodec(), FULL_CODEC)
            .xmap(p_304599_ -> p_304599_.map(p_304472_ -> new HoverEvent.ItemStackInfo(p_304472_, 1, Optional.empty()), p_304595_ -> p_304595_), Either::right);
        private final Item item;
        private final int count;
        private final Optional<CompoundTag> tag;
        @Nullable
        private ItemStack itemStack;

        ItemStackInfo(Item pItem, int pCount, @Nullable CompoundTag pTag) {
            this(pItem, pCount, Optional.ofNullable(pTag));
        }

        ItemStackInfo(Item p_304523_, int p_304412_, Optional<CompoundTag> p_304459_) {
            this.item = p_304523_;
            this.count = p_304412_;
            this.tag = p_304459_;
        }

        public ItemStackInfo(ItemStack pStack) {
            this(pStack.getItem(), pStack.getCount(), pStack.getTag() != null ? Optional.of(pStack.getTag().copy()) : Optional.empty());
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else if (pOther != null && this.getClass() == pOther.getClass()) {
                HoverEvent.ItemStackInfo hoverevent$itemstackinfo = (HoverEvent.ItemStackInfo)pOther;
                return this.count == hoverevent$itemstackinfo.count
                    && this.item.equals(hoverevent$itemstackinfo.item)
                    && this.tag.equals(hoverevent$itemstackinfo.tag);
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int i = this.item.hashCode();
            i = 31 * i + this.count;
            return 31 * i + this.tag.hashCode();
        }

        public ItemStack getItemStack() {
            if (this.itemStack == null) {
                this.itemStack = new ItemStack(this.item, this.count);
                this.tag.ifPresent(this.itemStack::setTag);
            }

            return this.itemStack;
        }

        private static DataResult<HoverEvent.ItemStackInfo> legacyCreate(Component pComponent) {
            try {
                CompoundTag compoundtag = TagParser.parseTag(pComponent.getString());
                return DataResult.success(new HoverEvent.ItemStackInfo(ItemStack.of(compoundtag)));
            } catch (CommandSyntaxException commandsyntaxexception) {
                return DataResult.error(() -> "Failed to parse item tag: " + commandsyntaxexception.getMessage());
            }
        }
    }

    static record TypedHoverEvent<T>(HoverEvent.Action<T> action, T value) {
        public static final MapCodec<HoverEvent.TypedHoverEvent<?>> CODEC = HoverEvent.Action.CODEC
            .dispatchMap("action", HoverEvent.TypedHoverEvent::action, p_304892_ -> p_304892_.codec);
        public static final MapCodec<HoverEvent.TypedHoverEvent<?>> LEGACY_CODEC = HoverEvent.Action.CODEC
            .dispatchMap("action", HoverEvent.TypedHoverEvent::action, p_304632_ -> p_304632_.legacyCodec);
    }
}
