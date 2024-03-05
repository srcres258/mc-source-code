package net.minecraft.world.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.DigDurabilityEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.slf4j.Logger;

public final class ItemStack extends net.neoforged.neoforge.attachment.AttachmentHolder implements net.neoforged.neoforge.common.extensions.IItemStackExtension {
    public static final Codec<ItemStack> CODEC = RecordCodecBuilder.create(
        p_311716_ -> p_311716_.group(
                    BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("id").forGetter(ItemStack::getItemHolder),
                    Codec.INT.fieldOf("Count").forGetter(ItemStack::getCount),
                    CompoundTag.CODEC.optionalFieldOf("tag").forGetter(p_281115_ -> Optional.ofNullable(p_281115_.getTag())),
                    ExtraCodecs.strictOptionalField(CompoundTag.CODEC, ATTACHMENTS_NBT_KEY).forGetter(s -> Optional.ofNullable(s.serializeAttachments()))
                )
                .apply(p_311716_, ItemStack::new)
    );
    private static final Codec<Item> ITEM_NON_AIR_CODEC = ExtraCodecs.validate(
        BuiltInRegistries.ITEM.byNameCodec(),
        p_311719_ -> p_311719_ == Items.AIR ? DataResult.error(() -> "Item must not be minecraft:air") : DataResult.success(p_311719_)
    );
    public static final Codec<ItemStack> ADVANCEMENT_ICON_CODEC = RecordCodecBuilder.create(
        p_311717_ -> p_311717_.group(
                    BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(ItemStack::getItemHolder),
                    ExtraCodecs.strictOptionalField(TagParser.AS_CODEC, "nbt").forGetter(p_311718_ -> Optional.ofNullable(p_311718_.getTag())),
                    ExtraCodecs.strictOptionalField(TagParser.AS_CODEC, ATTACHMENTS_NBT_KEY).forGetter(s -> Optional.ofNullable(s.serializeAttachments()))
                )
                .apply(p_311717_, (p_311722_, p_311723_, attachments) -> new ItemStack(p_311722_, 1, p_311723_, attachments))
    );
    public static final Codec<ItemStack> ITEM_WITH_COUNT_CODEC = RecordCodecBuilder.create(
        p_311720_ -> p_311720_.group(
                    ITEM_NON_AIR_CODEC.fieldOf("item").forGetter(ItemStack::getItem),
                    ExtraCodecs.strictOptionalField(ExtraCodecs.POSITIVE_INT, "count", 1).forGetter(ItemStack::getCount),
                    ExtraCodecs.strictOptionalField(net.neoforged.neoforge.common.crafting.CraftingHelper.TAG_CODEC, "nbt").forGetter(stack -> java.util.Optional.ofNullable(net.neoforged.neoforge.common.crafting.CraftingHelper.getTagForWriting(stack))),
                    ExtraCodecs.strictOptionalField(net.neoforged.neoforge.common.crafting.CraftingHelper.TAG_CODEC, ATTACHMENTS_NBT_KEY).forGetter(s -> Optional.ofNullable(s.serializeAttachments()))
                )
                .apply(p_311720_, ItemStack::new)
    );
    public static final Codec<ItemStack> SINGLE_ITEM_CODEC = ITEM_NON_AIR_CODEC.xmap(ItemStack::new, ItemStack::getItem);
    public static final MapCodec<ItemStack> RESULT_CODEC = RecordCodecBuilder.mapCodec(
        p_311721_ -> p_311721_.group(
                    BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(ItemStack::getItem),
                    Codec.INT.fieldOf("count").forGetter(ItemStack::getCount)
                )
                .apply(p_311721_, ItemStack::new)
    );
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ItemStack EMPTY = new ItemStack((Void)null);
    public static final DecimalFormat ATTRIBUTE_MODIFIER_FORMAT = Util.make(
        new DecimalFormat("#.##"), p_41704_ -> p_41704_.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT))
    );
    public static final String TAG_ENCH = "Enchantments";
    public static final String TAG_DISPLAY = "display";
    public static final String TAG_DISPLAY_NAME = "Name";
    public static final String TAG_LORE = "Lore";
    public static final String TAG_DAMAGE = "Damage";
    public static final String TAG_COLOR = "color";
    private static final String TAG_UNBREAKABLE = "Unbreakable";
    private static final String TAG_REPAIR_COST = "RepairCost";
    private static final String TAG_CAN_DESTROY_BLOCK_LIST = "CanDestroy";
    private static final String TAG_CAN_PLACE_ON_BLOCK_LIST = "CanPlaceOn";
    private static final String TAG_HIDE_FLAGS = "HideFlags";
    private static final Component DISABLED_ITEM_TOOLTIP = Component.translatable("item.disabled").withStyle(ChatFormatting.RED);
    private static final int DONT_HIDE_TOOLTIP = 0;
    private static final Style LORE_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE).withItalic(true);
    private int count;
    private int popTime;
    @Deprecated
    @Nullable
    private final Item item;
    @Nullable
    private CompoundTag tag;
    /**
     * The entity the item is attached to, like an Item Frame.
     */
    @Nullable
    private Entity entityRepresentation;
    @Nullable
    private AdventureModeCheck adventureBreakCheck;
    @Nullable
    private AdventureModeCheck adventurePlaceCheck;

    public Optional<TooltipComponent> getTooltipImage() {
        return this.getItem().getTooltipImage(this);
    }

    public ItemStack(ItemLike p_41599_) {
        this(p_41599_, 1);
    }

    public ItemStack(Holder<Item> pTag) {
        this(pTag.value(), 1);
    }

    public ItemStack(Holder<Item> p_312081_, int p_41605_, Optional<CompoundTag> p_41606_) {
        this(p_312081_.value(), p_41605_, p_41606_);
    }
    public ItemStack(ItemLike p_312081_, int p_41605_, Optional<CompoundTag> p_41606_) {
        this(p_312081_, p_41605_);
        p_41606_.ifPresent(this::setTag);
    }
    private ItemStack(Holder<Item> p_312081_, int p_41605_, Optional<CompoundTag> p_41606_, Optional<CompoundTag> attachmentsNbt) {
        this(p_312081_.value(), p_41605_, p_41606_, attachmentsNbt);
    }
    private ItemStack(ItemLike p_312081_, int p_41605_, Optional<CompoundTag> p_41606_, Optional<CompoundTag> attachmentsNbt) {
        this(p_312081_, p_41605_, attachmentsNbt.orElse(null));
        p_41606_.ifPresent(this::setTag);
    }

    public ItemStack(Holder<Item> pItem, int pCount) {
        this(pItem.value(), pCount);
    }

    public ItemStack(ItemLike p_41601_, int p_41602_) { this(p_41601_, p_41602_, (CompoundTag) null); }
    public ItemStack(ItemLike p_41601_, int p_41602_, @Nullable CompoundTag attachmentsNbt) {
        this.item = p_41601_.asItem();
        this.count = p_41602_;
        if (attachmentsNbt != null) deserializeAttachments(attachmentsNbt);
        if (this.item.isDamageable(this)) {
            this.setDamageValue(this.getDamageValue());
        }
    }

    private ItemStack(@Nullable Void p_282703_) {
        this.item = null;
    }

    private ItemStack(CompoundTag pCompoundTag) {
        this.item = BuiltInRegistries.ITEM.get(new ResourceLocation(pCompoundTag.getString("id")));
        this.count = pCompoundTag.getByte("Count");
        if (pCompoundTag.contains("tag", 10)) {
            this.tag = pCompoundTag.getCompound("tag").copy();
            if (this.tag.contains(ATTACHMENTS_NBT_KEY, Tag.TAG_COMPOUND)) { // Neo: Read contained attachments
                deserializeAttachments(this.tag.getCompound(ATTACHMENTS_NBT_KEY));
                this.tag = net.neoforged.neoforge.attachment.AttachmentInternals.cleanTag(this.tag);
            }
            if (this.tag != null)
            this.getItem().verifyTagAfterLoad(this.tag);
        }

        if (this.getItem().isDamageable(this)) {
            this.setDamageValue(this.getDamageValue());
        }
    }

    public static ItemStack of(CompoundTag pCompoundTag) {
        try {
            return new ItemStack(pCompoundTag);
        } catch (RuntimeException runtimeexception) {
            LOGGER.debug("Tried to load invalid item: {}", pCompoundTag, runtimeexception);
            return EMPTY;
        }
    }

    public boolean isEmpty() {
        return this == EMPTY || this.item == Items.AIR || this.count <= 0;
    }

    public boolean isItemEnabled(FeatureFlagSet pEnabledFlags) {
        return this.isEmpty() || this.getItem().isEnabled(pEnabledFlags);
    }

    /**
     * Splits off a stack of the given amount of this stack and reduces this stack by the amount.
     */
    public ItemStack split(int pAmount) {
        int i = Math.min(pAmount, this.getCount());
        ItemStack itemstack = this.copyWithCount(i);
        this.shrink(i);
        return itemstack;
    }

    public ItemStack copyAndClear() {
        if (this.isEmpty()) {
            return EMPTY;
        } else {
            ItemStack itemstack = this.copy();
            this.setCount(0);
            return itemstack;
        }
    }

    /**
     * Returns the object corresponding to the stack.
     */
    public Item getItem() {
        return this.isEmpty() ? Items.AIR : this.item;
    }

    public Holder<Item> getItemHolder() {
        return this.getItem().builtInRegistryHolder();
    }

    public boolean is(TagKey<Item> pTag) {
        return this.getItem().builtInRegistryHolder().is(pTag);
    }

    public boolean is(Item pItem) {
        return this.getItem() == pItem;
    }

    public boolean is(Predicate<Holder<Item>> pItem) {
        return pItem.test(this.getItem().builtInRegistryHolder());
    }

    public boolean is(Holder<Item> pItem) {
        return is(pItem.value()); // Neo: Fix comparing for custom holders such as DeferredHolders
    }

    public boolean is(HolderSet<Item> pItem) {
        return pItem.contains(this.getItemHolder());
    }

    public Stream<TagKey<Item>> getTags() {
        return this.getItem().builtInRegistryHolder().tags();
    }

    public InteractionResult useOn(UseOnContext pContext) {
        if (pContext.getPlayer() != null) { // TODO 1.20.5: Make event accept nullable player, and remove this check.
            var e = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent(pContext, net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK));
            if (e.isCanceled()) return e.getCancellationResult();
        }
        if (!pContext.getLevel().isClientSide) return net.neoforged.neoforge.common.CommonHooks.onPlaceItemIntoWorld(pContext);
        return onItemUse(pContext, (c) -> getItem().useOn(pContext));
    }

    public InteractionResult onItemUseFirst(UseOnContext p_41662_) {
        if (p_41662_.getPlayer() != null) { // TODO 1.20.5: Make event accept nullable player, and remove this check.
            var e = net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent(p_41662_, net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent.UsePhase.ITEM_BEFORE_BLOCK));
            if (e.isCanceled()) return e.getCancellationResult();
        }
        return onItemUse(p_41662_, (c) -> getItem().onItemUseFirst(this, p_41662_));
    }

    private InteractionResult onItemUse(UseOnContext p_41662_, java.util.function.Function<UseOnContext, InteractionResult> callback) {
        Player player = p_41662_.getPlayer();
        BlockPos blockpos = p_41662_.getClickedPos();
        BlockInWorld blockinworld = new BlockInWorld(p_41662_.getLevel(), blockpos, false);
        if (player != null
            && !player.getAbilities().mayBuild
            && !this.hasAdventureModePlaceTagForBlock(p_41662_.getLevel().registryAccess().registryOrThrow(Registries.BLOCK), blockinworld)) {
            return InteractionResult.PASS;
        } else {
            Item item = this.getItem();
            InteractionResult interactionresult = callback.apply(p_41662_);
            if (player != null && interactionresult.shouldAwardStats()) {
                player.awardStat(Stats.ITEM_USED.get(item));
            }

            return interactionresult;
        }
    }

    public float getDestroySpeed(BlockState pState) {
        return this.getItem().getDestroySpeed(this, pState);
    }

    /**
     * Called when the {@code ItemStack} is equipped and right-clicked. Replaces the {@code ItemStack} with the return value.
     */
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        return this.getItem().use(pLevel, pPlayer, pUsedHand);
    }

    /**
     * Called when the item in use count reach 0, e.g. item food eaten. Return the new ItemStack. Args : world, entity
     */
    public ItemStack finishUsingItem(Level pLevel, LivingEntity pLivingEntity) {
        return this.getItem().finishUsingItem(this, pLevel, pLivingEntity);
    }

    /**
     * Write the stack fields to a NBT object. Return the new NBT object.
     */
    public CompoundTag save(CompoundTag pCompoundTag) {
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(this.getItem());
        pCompoundTag.putString("id", resourcelocation == null ? "minecraft:air" : resourcelocation.toString());
        pCompoundTag.putByte("Count", (byte)this.count);
        var tag = net.neoforged.neoforge.attachment.AttachmentInternals.addAttachmentsToTag(this.tag, this, true);
        if (tag != null) {
            pCompoundTag.put("tag", tag);
        }

        return pCompoundTag;
    }

    /**
     * Returns maximum size of the stack.
     */
    public int getMaxStackSize() {
        return this.getItem().getMaxStackSize(this);
    }

    /**
     * Returns {@code true} if the {@code ItemStack} can hold 2 or more units of the item.
     */
    public boolean isStackable() {
        return this.getMaxStackSize() > 1 && (!this.isDamageableItem() || !this.isDamaged());
    }

    /**
     * Returns {@code true} if this {@code ItemStack} is damageable.
     */
    public boolean isDamageableItem() {
        if (!this.isEmpty() && this.getItem().isDamageable(this)) {
            CompoundTag compoundtag = this.getTag();
            return compoundtag == null || !compoundtag.getBoolean("Unbreakable");
        } else {
            return false;
        }
    }

    /**
     * Returns {@code true} when a damageable item is damaged.
     */
    public boolean isDamaged() {
        return this.isDamageableItem() && getItem().isDamaged(this);
    }

    public int getDamageValue() {
        return this.getItem().getDamage(this);
    }

    public void setDamageValue(int pDamage) {
        this.getItem().setDamage(this, pDamage);
    }

    /**
     * Returns the max damage an item in the stack can take.
     */
    public int getMaxDamage() {
        return this.getItem().getMaxDamage(this);
    }

    /**
     * Attempts to damage the {@code ItemStack} with {@code amount} damage, If the {@code ItemStack} has the Unbreaking enchantment there is a chance for each point of damage to be negated. Returns {@code true} if it takes more damage than {@code getMaxDamage()}. Returns {@code false} otherwise or if the {@code ItemStack} can't be damaged or if all points of damage are negated.
     */
    public boolean hurt(int pAmount, RandomSource pRandom, @Nullable ServerPlayer pUser) {
        if (!this.isDamageableItem()) {
            return false;
        } else {
            if (pAmount > 0) {
                int i = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, this);
                int j = 0;

                for(int k = 0; i > 0 && k < pAmount; ++k) {
                    if (DigDurabilityEnchantment.shouldIgnoreDurabilityDrop(this, i, pRandom)) {
                        ++j;
                    }
                }

                pAmount -= j;
                if (pAmount <= 0) {
                    return false;
                }
            }

            if (pUser != null && pAmount != 0) {
                CriteriaTriggers.ITEM_DURABILITY_CHANGED.trigger(pUser, this, this.getDamageValue() + pAmount);
            }

            int l = this.getDamageValue() + pAmount;
            this.setDamageValue(l);
            return l >= this.getMaxDamage();
        }
    }

    public <T extends LivingEntity> void hurtAndBreak(int pAmount, T pEntity, Consumer<T> pOnBroken) {
        if (!pEntity.level().isClientSide && (!(pEntity instanceof Player) || !((Player)pEntity).getAbilities().instabuild)) {
            if (this.isDamageableItem()) {
                pAmount = this.getItem().damageItem(this, pAmount, pEntity, pOnBroken);
                if (this.hurt(pAmount, pEntity.getRandom(), pEntity instanceof ServerPlayer ? (ServerPlayer)pEntity : null)) {
                    pOnBroken.accept(pEntity);
                    Item item = this.getItem();
                    this.shrink(1);
                    if (pEntity instanceof Player) {
                        ((Player)pEntity).awardStat(Stats.ITEM_BROKEN.get(item));
                    }

                    this.setDamageValue(0);
                }
            }
        }
    }

    public boolean isBarVisible() {
        return this.getItem().isBarVisible(this);
    }

    public int getBarWidth() {
        return this.getItem().getBarWidth(this);
    }

    public int getBarColor() {
        return this.getItem().getBarColor(this);
    }

    public boolean overrideStackedOnOther(Slot pSlot, ClickAction pAction, Player pPlayer) {
        return this.getItem().overrideStackedOnOther(this, pSlot, pAction, pPlayer);
    }

    public boolean overrideOtherStackedOnMe(ItemStack pStack, Slot pSlot, ClickAction pAction, Player pPlayer, SlotAccess pAccess) {
        return this.getItem().overrideOtherStackedOnMe(this, pStack, pSlot, pAction, pPlayer, pAccess);
    }

    /**
     * Calls the delegated method to the Item to damage the incoming Entity, and if necessary, triggers a stats increase.
     */
    public void hurtEnemy(LivingEntity pEntity, Player pPlayer) {
        Item item = this.getItem();
        if (item.hurtEnemy(this, pEntity, pPlayer)) {
            pPlayer.awardStat(Stats.ITEM_USED.get(item));
        }
    }

    /**
     * Called when a Block is destroyed using this ItemStack
     */
    public void mineBlock(Level pLevel, BlockState pState, BlockPos pPos, Player pPlayer) {
        Item item = this.getItem();
        if (item.mineBlock(this, pLevel, pState, pPos, pPlayer)) {
            pPlayer.awardStat(Stats.ITEM_USED.get(item));
        }
    }

    /**
     * Check whether the given Block can be harvested using this ItemStack.
     */
    public boolean isCorrectToolForDrops(BlockState pState) {
        return this.getItem().isCorrectToolForDrops(this, pState);
    }

    public InteractionResult interactLivingEntity(Player pPlayer, LivingEntity pEntity, InteractionHand pUsedHand) {
        return this.getItem().interactLivingEntity(this, pPlayer, pEntity, pUsedHand);
    }

    /**
     * Returns a new stack with the same properties.
     */
    public ItemStack copy() {
        if (this.isEmpty()) {
            return EMPTY;
        } else {
            ItemStack itemstack = new ItemStack(this.getItem(), this.count);
            net.neoforged.neoforge.attachment.AttachmentInternals.copyStackAttachments(this, itemstack);
            itemstack.setPopTime(this.getPopTime());
            if (this.tag != null) {
                itemstack.tag = this.tag.copy();
            }

            return itemstack;
        }
    }

    public ItemStack copyWithCount(int pCount) {
        if (this.isEmpty()) {
            return EMPTY;
        } else {
            ItemStack itemstack = this.copy();
            itemstack.setCount(pCount);
            return itemstack;
        }
    }

    /**
     * Compares both {@code ItemStacks}, returns {@code true} if both {@code ItemStacks} are equal.
     */
    public static boolean matches(ItemStack pStack, ItemStack pOther) {
        if (pStack == pOther) {
            return true;
        } else {
            return pStack.getCount() != pOther.getCount() ? false : isSameItemSameTags(pStack, pOther);
        }
    }

    public static boolean isSameItem(ItemStack pStack, ItemStack pOther) {
        return pStack.is(pOther.getItem());
    }

    public static boolean isSameItemSameTags(ItemStack pStack, ItemStack pOther) {
        if (!pStack.is(pOther.getItem())) {
            return false;
        } else {
            return pStack.isEmpty() && pOther.isEmpty() ? true : Objects.equals(pStack.tag, pOther.tag) && pStack.areAttachmentsCompatible(pOther);
        }
    }

    /**
     * Neo: Check if the attachments of this stack and another stack are compatible.
     * If not, they cannot be stacked.
     * @see #areAttachmentsCompatible(net.neoforged.neoforge.attachment.AttachmentHolder, net.neoforged.neoforge.attachment.AttachmentHolder)
     */
    public boolean areAttachmentsCompatible(ItemStack other) {
        return areAttachmentsCompatible(this, other);
    }

    public String getDescriptionId() {
        return this.getItem().getDescriptionId(this);
    }

    @Override
    public String toString() {
        return this.getCount() + " " + this.getItem();
    }

    /**
     * Called each tick as long the {@code ItemStack} in in player's inventory. Used to progress the pickup animation and update maps.
     */
    public void inventoryTick(Level pLevel, Entity pEntity, int pInventorySlot, boolean pIsCurrentItem) {
        if (this.popTime > 0) {
            --this.popTime;
        }

        if (this.getItem() != null) {
            this.getItem().inventoryTick(this, pLevel, pEntity, pInventorySlot, pIsCurrentItem);
        }
    }

    public void onCraftedBy(Level pLevel, Player pPlayer, int pAmount) {
        pPlayer.awardStat(Stats.ITEM_CRAFTED.get(this.getItem()), pAmount);
        this.getItem().onCraftedBy(this, pLevel, pPlayer);
    }

    public void onCraftedBySystem(Level pLevel) {
        this.getItem().onCraftedPostProcess(this, pLevel);
    }

    public int getUseDuration() {
        return this.getItem().getUseDuration(this);
    }

    public UseAnim getUseAnimation() {
        return this.getItem().getUseAnimation(this);
    }

    /**
     * Called when the player releases the use item button.
     */
    public void releaseUsing(Level pLevel, LivingEntity pLivingEntity, int pTimeLeft) {
        this.getItem().releaseUsing(this, pLevel, pLivingEntity, pTimeLeft);
    }

    public boolean useOnRelease() {
        return this.getItem().useOnRelease(this);
    }

    /**
     * Returns {@code true} if the ItemStack has an NBTTagCompound. Currently used to store enchantments.
     */
    public boolean hasTag() {
        return !this.isEmpty() && this.tag != null && !this.tag.isEmpty();
    }

    @Nullable
    public CompoundTag getTag() {
        return this.tag;
    }

    public CompoundTag getOrCreateTag() {
        if (this.tag == null) {
            this.setTag(new CompoundTag());
        }

        return this.tag;
    }

    public CompoundTag getOrCreateTagElement(String pKey) {
        if (this.tag != null && this.tag.contains(pKey, 10)) {
            return this.tag.getCompound(pKey);
        } else {
            CompoundTag compoundtag = new CompoundTag();
            this.addTagElement(pKey, compoundtag);
            return compoundtag;
        }
    }

    /**
     * Get an NBTTagCompound from this stack's NBT data.
     */
    @Nullable
    public CompoundTag getTagElement(String pKey) {
        return this.tag != null && this.tag.contains(pKey, 10) ? this.tag.getCompound(pKey) : null;
    }

    public void removeTagKey(String pKey) {
        if (this.tag != null && this.tag.contains(pKey)) {
            this.tag.remove(pKey);
            if (this.tag.isEmpty()) {
                this.tag = null;
            }
        }
    }

    public ListTag getEnchantmentTags() {
        return this.tag != null ? this.tag.getList("Enchantments", 10) : new ListTag();
    }

    /**
     * Assigns a NBTTagCompound to the ItemStack, minecraft validates that only non-stackable items can have it.
     */
    public void setTag(@Nullable CompoundTag p_41752_) {
        this.tag = p_41752_;
        if (this.getItem().isDamageable(this)) {
            this.setDamageValue(this.getDamageValue());
        }

        if (p_41752_ != null) {
            this.getItem().verifyTagAfterLoad(p_41752_);
        }
    }

    public Component getHoverName() {
        CompoundTag compoundtag = this.getTagElement("display");
        if (compoundtag != null && compoundtag.contains("Name", 8)) {
            try {
                Component component = Component.Serializer.fromJson(compoundtag.getString("Name"));
                if (component != null) {
                    return component;
                }

                compoundtag.remove("Name");
            } catch (Exception exception) {
                compoundtag.remove("Name");
            }
        }

        return this.getItem().getName(this);
    }

    public ItemStack setHoverName(@Nullable Component pNameComponent) {
        CompoundTag compoundtag = this.getOrCreateTagElement("display");
        if (pNameComponent != null) {
            compoundtag.putString("Name", Component.Serializer.toJson(pNameComponent));
        } else {
            compoundtag.remove("Name");
        }

        return this;
    }

    /**
     * Clear any custom name set for this ItemStack
     */
    public void resetHoverName() {
        CompoundTag compoundtag = this.getTagElement("display");
        if (compoundtag != null) {
            compoundtag.remove("Name");
            if (compoundtag.isEmpty()) {
                this.removeTagKey("display");
            }
        }

        if (this.tag != null && this.tag.isEmpty()) {
            this.tag = null;
        }
    }

    /**
     * Returns {@code true} if the itemstack has a display name
     */
    public boolean hasCustomHoverName() {
        CompoundTag compoundtag = this.getTagElement("display");
        return compoundtag != null && compoundtag.contains("Name", 8);
    }

    /**
     * Return a list of strings containing information about the item
     */
    public List<Component> getTooltipLines(@Nullable Player pPlayer, TooltipFlag pIsAdvanced) {
        List<Component> list = Lists.newArrayList();
        MutableComponent mutablecomponent = Component.empty().append(this.getHoverName()).withStyle(this.getRarity().getStyleModifier());
        if (this.hasCustomHoverName()) {
            mutablecomponent.withStyle(ChatFormatting.ITALIC);
        }

        list.add(mutablecomponent);
        if (!pIsAdvanced.isAdvanced() && !this.hasCustomHoverName() && this.is(Items.FILLED_MAP)) {
            Integer integer = MapItem.getMapId(this);
            if (integer != null) {
                list.add(MapItem.getTooltipForId(this));
            }
        }

        int j = this.getHideFlags();
        if (shouldShowInTooltip(j, ItemStack.TooltipPart.ADDITIONAL)) {
            this.getItem().appendHoverText(this, pPlayer == null ? null : pPlayer.level(), list, pIsAdvanced);
        }

        if (this.hasTag()) {
            if (shouldShowInTooltip(j, ItemStack.TooltipPart.UPGRADES) && pPlayer != null) {
                ArmorTrim.appendUpgradeHoverText(this, pPlayer.level().registryAccess(), list);
            }

            if (shouldShowInTooltip(j, ItemStack.TooltipPart.ENCHANTMENTS)) {
                appendEnchantmentNames(list, this.getEnchantmentTags());
            }

            if (this.tag.contains("display", 10)) {
                CompoundTag compoundtag = this.tag.getCompound("display");
                if (shouldShowInTooltip(j, ItemStack.TooltipPart.DYE) && compoundtag.contains("color", 99)) {
                    if (pIsAdvanced.isAdvanced()) {
                        list.add(
                            Component.translatable("item.color", String.format(Locale.ROOT, "#%06X", compoundtag.getInt("color")))
                                .withStyle(ChatFormatting.GRAY)
                        );
                    } else {
                        list.add(Component.translatable("item.dyed").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                    }
                }

                if (compoundtag.getTagType("Lore") == 9) {
                    ListTag listtag = compoundtag.getList("Lore", 8);

                    for(int i = 0; i < listtag.size(); ++i) {
                        String s = listtag.getString(i);

                        try {
                            MutableComponent mutablecomponent1 = Component.Serializer.fromJson(s);
                            if (mutablecomponent1 != null) {
                                list.add(ComponentUtils.mergeStyles(mutablecomponent1, LORE_STYLE));
                            }
                        } catch (Exception exception) {
                            compoundtag.remove("Lore");
                        }
                    }
                }
            }
        }

        if (shouldShowInTooltip(j, ItemStack.TooltipPart.MODIFIERS)) {
            for(EquipmentSlot equipmentslot : EquipmentSlot.values()) {
                Multimap<Attribute, AttributeModifier> multimap = this.getAttributeModifiers(equipmentslot);
                if (!multimap.isEmpty()) {
                    list.add(CommonComponents.EMPTY);
                    list.add(Component.translatable("item.modifiers." + equipmentslot.getName()).withStyle(ChatFormatting.GRAY));

                    for(Entry<Attribute, AttributeModifier> entry : multimap.entries()) {
                        AttributeModifier attributemodifier = entry.getValue();
                        double d0 = attributemodifier.getAmount();
                        boolean flag = false;
                        if (pPlayer != null) {
                            if (attributemodifier.getId() == Item.BASE_ATTACK_DAMAGE_UUID) {
                                d0 += pPlayer.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);
                                d0 += (double)EnchantmentHelper.getDamageBonus(this, MobType.UNDEFINED);
                                flag = true;
                            } else if (attributemodifier.getId() == Item.BASE_ATTACK_SPEED_UUID) {
                                d0 += pPlayer.getAttributeBaseValue(Attributes.ATTACK_SPEED);
                                flag = true;
                            }
                        }

                        double d1;
                        if (attributemodifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE
                            || attributemodifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                            d1 = d0 * 100.0;
                        } else if (entry.getKey().equals(Attributes.KNOCKBACK_RESISTANCE)) {
                            d1 = d0 * 10.0;
                        } else {
                            d1 = d0;
                        }

                        if (flag) {
                            list.add(
                                CommonComponents.space()
                                    .append(
                                        Component.translatable(
                                            "attribute.modifier.equals." + attributemodifier.getOperation().toValue(),
                                            ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                                            Component.translatable(entry.getKey().getDescriptionId())
                                        )
                                    )
                                    .withStyle(ChatFormatting.DARK_GREEN)
                            );
                        } else if (d0 > 0.0) {
                            list.add(
                                Component.translatable(
                                        "attribute.modifier.plus." + attributemodifier.getOperation().toValue(),
                                        ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                                        Component.translatable(entry.getKey().getDescriptionId())
                                    )
                                    .withStyle(ChatFormatting.BLUE)
                            );
                        } else if (d0 < 0.0) {
                            d1 *= -1.0;
                            list.add(
                                Component.translatable(
                                        "attribute.modifier.take." + attributemodifier.getOperation().toValue(),
                                        ATTRIBUTE_MODIFIER_FORMAT.format(d1),
                                        Component.translatable(entry.getKey().getDescriptionId())
                                    )
                                    .withStyle(ChatFormatting.RED)
                            );
                        }
                    }
                }
            }
        }

        if (this.hasTag()) {
            if (shouldShowInTooltip(j, ItemStack.TooltipPart.UNBREAKABLE) && this.tag.getBoolean("Unbreakable")) {
                list.add(Component.translatable("item.unbreakable").withStyle(ChatFormatting.BLUE));
            }

            if (shouldShowInTooltip(j, ItemStack.TooltipPart.CAN_DESTROY) && this.tag.contains("CanDestroy", 9)) {
                ListTag listtag1 = this.tag.getList("CanDestroy", 8);
                if (!listtag1.isEmpty()) {
                    list.add(CommonComponents.EMPTY);
                    list.add(Component.translatable("item.canBreak").withStyle(ChatFormatting.GRAY));

                    for(int k = 0; k < listtag1.size(); ++k) {
                        list.addAll(expandBlockState(listtag1.getString(k)));
                    }
                }
            }

            if (shouldShowInTooltip(j, ItemStack.TooltipPart.CAN_PLACE) && this.tag.contains("CanPlaceOn", 9)) {
                ListTag listtag2 = this.tag.getList("CanPlaceOn", 8);
                if (!listtag2.isEmpty()) {
                    list.add(CommonComponents.EMPTY);
                    list.add(Component.translatable("item.canPlace").withStyle(ChatFormatting.GRAY));

                    for(int l = 0; l < listtag2.size(); ++l) {
                        list.addAll(expandBlockState(listtag2.getString(l)));
                    }
                }
            }
        }

        if (pIsAdvanced.isAdvanced()) {
            if (this.isDamaged()) {
                list.add(Component.translatable("item.durability", this.getMaxDamage() - this.getDamageValue(), this.getMaxDamage()));
            }

            list.add(Component.literal(BuiltInRegistries.ITEM.getKey(this.getItem()).toString()).withStyle(ChatFormatting.DARK_GRAY));
            if (this.hasTag()) {
                list.add(Component.translatable("item.nbt_tags", this.tag.getAllKeys().size()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        if (pPlayer != null && !this.getItem().isEnabled(pPlayer.level().enabledFeatures())) {
            list.add(DISABLED_ITEM_TOOLTIP);
        }

        net.neoforged.neoforge.event.EventHooks.onItemTooltip(this, pPlayer, list, pIsAdvanced);
        return list;
    }

    private static boolean shouldShowInTooltip(int pHideFlags, ItemStack.TooltipPart pPart) {
        return (pHideFlags & pPart.getMask()) == 0;
    }

    private int getHideFlags() {
        return this.hasTag() && this.tag.contains("HideFlags", 99) ? this.tag.getInt("HideFlags") : this.getItem().getDefaultTooltipHideFlags(this);
    }

    public void hideTooltipPart(ItemStack.TooltipPart pPart) {
        CompoundTag compoundtag = this.getOrCreateTag();
        compoundtag.putInt("HideFlags", compoundtag.getInt("HideFlags") | pPart.getMask());
    }

    public static void appendEnchantmentNames(List<Component> pTooltipComponents, ListTag pStoredEnchantments) {
        for(int i = 0; i < pStoredEnchantments.size(); ++i) {
            CompoundTag compoundtag = pStoredEnchantments.getCompound(i);
            BuiltInRegistries.ENCHANTMENT
                .getOptional(EnchantmentHelper.getEnchantmentId(compoundtag))
                .ifPresent(p_41708_ -> pTooltipComponents.add(p_41708_.getFullname(EnchantmentHelper.getEnchantmentLevel(compoundtag))));
        }
    }

    private static Collection<Component> expandBlockState(String pStateString) {
        try {
            return BlockStateParser.parseForTesting(BuiltInRegistries.BLOCK.asLookup(), pStateString, true)
                .map(
                    p_220162_ -> Lists.newArrayList(p_220162_.blockState().getBlock().getName().withStyle(ChatFormatting.DARK_GRAY)),
                    p_220164_ -> p_220164_.tag()
                            .stream()
                            .map(p_220172_ -> p_220172_.value().getName().withStyle(ChatFormatting.DARK_GRAY))
                            .collect(Collectors.toList())
                );
        } catch (CommandSyntaxException commandsyntaxexception) {
            return Lists.newArrayList(Component.literal("missingno").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    public boolean hasFoil() {
        return this.getItem().isFoil(this);
    }

    public Rarity getRarity() {
        return this.getItem().getRarity(this);
    }

    /**
     * True if it is a tool and has no enchantments to begin with
     */
    public boolean isEnchantable() {
        if (!this.getItem().isEnchantable(this)) {
            return false;
        } else {
            return !this.isEnchanted();
        }
    }

    /**
     * Adds an enchantment with a desired level on the ItemStack.
     */
    public void enchant(Enchantment pEnchantment, int pLevel) {
        this.getOrCreateTag();
        if (!this.tag.contains("Enchantments", 9)) {
            this.tag.put("Enchantments", new ListTag());
        }

        ListTag listtag = this.tag.getList("Enchantments", 10);
        listtag.add(EnchantmentHelper.storeEnchantment(EnchantmentHelper.getEnchantmentId(pEnchantment), (byte)pLevel));
    }

    /**
     * True if the item has enchantment data
     */
    public boolean isEnchanted() {
        if (this.tag != null && this.tag.contains("Enchantments", 9)) {
            return !this.tag.getList("Enchantments", 10).isEmpty();
        } else {
            return false;
        }
    }

    public void addTagElement(String pKey, Tag pTag) {
        this.getOrCreateTag().put(pKey, pTag);
    }

    /**
     * Return whether this stack is on an item frame.
     */
    public boolean isFramed() {
        return this.entityRepresentation instanceof ItemFrame;
    }

    public void setEntityRepresentation(@Nullable Entity pEntity) {
        this.entityRepresentation = pEntity;
    }

    /**
     * Return the item frame this stack is on. Returns null if not on an item frame.
     */
    @Nullable
    public ItemFrame getFrame() {
        return this.entityRepresentation instanceof ItemFrame ? (ItemFrame)this.getEntityRepresentation() : null;
    }

    /**
     * For example, it'll return an {@code ItemFrameEntity} if it is in an itemframe.
     */
    @Nullable
    public Entity getEntityRepresentation() {
        return !this.isEmpty() ? this.entityRepresentation : null;
    }

    /**
     * Get this stack's repair cost, or 0 if no repair cost is defined.
     */
    public int getBaseRepairCost() {
        return this.hasTag() && this.tag.contains("RepairCost", 3) ? this.tag.getInt("RepairCost") : 0;
    }

    /**
     * Set this stack's repair cost.
     */
    public void setRepairCost(int pCost) {
        if (pCost > 0) {
            this.getOrCreateTag().putInt("RepairCost", pCost);
        } else {
            this.removeTagKey("RepairCost");
        }
    }

    /**
     * Gets the attribute modifiers for this ItemStack.
     * Will check for an NBT tag list containing modifiers for the stack.
     */
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot pSlot) {
        Multimap<Attribute, AttributeModifier> multimap;
        if (this.hasTag() && this.tag.contains("AttributeModifiers", 9)) {
            multimap = HashMultimap.create();
            ListTag listtag = this.tag.getList("AttributeModifiers", 10);

            for(int i = 0; i < listtag.size(); ++i) {
                CompoundTag compoundtag = listtag.getCompound(i);
                if (!compoundtag.contains("Slot", 8) || compoundtag.getString("Slot").equals(pSlot.getName())) {
                    Optional<Attribute> optional = BuiltInRegistries.ATTRIBUTE.getOptional(ResourceLocation.tryParse(compoundtag.getString("AttributeName")));
                    if (!optional.isEmpty()) {
                        AttributeModifier attributemodifier = AttributeModifier.load(compoundtag);
                        if (attributemodifier != null
                            && attributemodifier.getId().getLeastSignificantBits() != 0L
                            && attributemodifier.getId().getMostSignificantBits() != 0L) {
                            multimap.put(optional.get(), attributemodifier);
                        }
                    }
                }
            }
        } else {
            multimap = this.getItem().getAttributeModifiers(pSlot, this);
        }

        multimap = net.neoforged.neoforge.common.CommonHooks.getAttributeModifiers(this, pSlot, multimap);
        return multimap;
    }

    public void addAttributeModifier(Attribute pAttribute, AttributeModifier pModifier, @Nullable EquipmentSlot pSlot) {
        this.getOrCreateTag();
        if (!this.tag.contains("AttributeModifiers", 9)) {
            this.tag.put("AttributeModifiers", new ListTag());
        }

        ListTag listtag = this.tag.getList("AttributeModifiers", 10);
        CompoundTag compoundtag = pModifier.save();
        compoundtag.putString("AttributeName", BuiltInRegistries.ATTRIBUTE.getKey(pAttribute).toString());
        if (pSlot != null) {
            compoundtag.putString("Slot", pSlot.getName());
        }

        listtag.add(compoundtag);
    }

    /**
     * Get a ChatComponent for this Item's display name that shows this Item on hover
     */
    public Component getDisplayName() {
        MutableComponent mutablecomponent = Component.empty().append(this.getHoverName());
        if (this.hasCustomHoverName()) {
            mutablecomponent.withStyle(ChatFormatting.ITALIC);
        }

        MutableComponent mutablecomponent1 = ComponentUtils.wrapInSquareBrackets(mutablecomponent);
        if (!this.isEmpty()) {
            mutablecomponent1.withStyle(this.getRarity().getStyleModifier())
                .withStyle(p_220170_ -> p_220170_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(this))));
        }

        return mutablecomponent1;
    }

    public boolean hasAdventureModePlaceTagForBlock(Registry<Block> pBlockRegistry, BlockInWorld pBlock) {
        if (this.adventurePlaceCheck == null) {
            this.adventurePlaceCheck = new AdventureModeCheck("CanPlaceOn");
        }

        return this.adventurePlaceCheck.test(this, pBlockRegistry, pBlock);
    }

    public boolean hasAdventureModeBreakTagForBlock(Registry<Block> pBlockRegistry, BlockInWorld pBlock) {
        if (this.adventureBreakCheck == null) {
            this.adventureBreakCheck = new AdventureModeCheck("CanDestroy");
        }

        return this.adventureBreakCheck.test(this, pBlockRegistry, pBlock);
    }

    public int getPopTime() {
        return this.popTime;
    }

    public void setPopTime(int pPopTime) {
        this.popTime = pPopTime;
    }

    public int getCount() {
        return this.isEmpty() ? 0 : this.count;
    }

    public void setCount(int pCount) {
        this.count = pCount;
    }

    public void grow(int pIncrement) {
        this.setCount(this.getCount() + pIncrement);
    }

    public void shrink(int pDecrement) {
        this.grow(-pDecrement);
    }

    /**
     * Called as the stack is being used by an entity.
     */
    public void onUseTick(Level pLevel, LivingEntity pLivingEntity, int pCount) {
        this.getItem().onUseTick(pLevel, pLivingEntity, this, pCount);
    }

    /**
 * @deprecated Forge: Use {@linkplain IItemStackExtension#onDestroyed(ItemEntity,
 *             net.minecraft.world.damagesource.DamageSource) damage source
 *             sensitive version}
 */
    @Deprecated
    public void onDestroyed(ItemEntity pItemEntity) {
        this.getItem().onDestroyed(pItemEntity);
    }

    public boolean isEdible() {
        return this.getItem().isEdible();
    }

    public SoundEvent getDrinkingSound() {
        return this.getItem().getDrinkingSound();
    }

    public SoundEvent getEatingSound() {
        return this.getItem().getEatingSound();
    }

    public static enum TooltipPart {
        ENCHANTMENTS,
        MODIFIERS,
        UNBREAKABLE,
        CAN_DESTROY,
        CAN_PLACE,
        ADDITIONAL,
        DYE,
        UPGRADES;

        private final int mask = 1 << this.ordinal();

        public int getMask() {
            return this.mask;
        }
    }
}
