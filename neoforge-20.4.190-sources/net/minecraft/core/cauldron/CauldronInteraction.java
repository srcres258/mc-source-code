package net.minecraft.core.cauldron;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public interface CauldronInteraction {
    Map<String, CauldronInteraction.InteractionMap> INTERACTIONS = new Object2ObjectArrayMap<>();
    Codec<CauldronInteraction.InteractionMap> CODEC = ExtraCodecs.stringResolverCodec(CauldronInteraction.InteractionMap::name, INTERACTIONS::get);
    CauldronInteraction.InteractionMap EMPTY = newInteractionMap("empty");
    CauldronInteraction.InteractionMap WATER = newInteractionMap("water");
    CauldronInteraction.InteractionMap LAVA = newInteractionMap("lava");
    CauldronInteraction.InteractionMap POWDER_SNOW = newInteractionMap("powder_snow");
    CauldronInteraction FILL_WATER = (p_175683_, p_175684_, p_175685_, p_175686_, p_175687_, p_175688_) -> emptyBucket(
            p_175684_,
            p_175685_,
            p_175686_,
            p_175687_,
            p_175688_,
            Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, Integer.valueOf(3)),
            SoundEvents.BUCKET_EMPTY
        );
    CauldronInteraction FILL_LAVA = (p_175676_, p_175677_, p_175678_, p_175679_, p_175680_, p_175681_) -> emptyBucket(
            p_175677_, p_175678_, p_175679_, p_175680_, p_175681_, Blocks.LAVA_CAULDRON.defaultBlockState(), SoundEvents.BUCKET_EMPTY_LAVA
        );
    CauldronInteraction FILL_POWDER_SNOW = (p_175669_, p_175670_, p_175671_, p_175672_, p_175673_, p_175674_) -> emptyBucket(
            p_175670_,
            p_175671_,
            p_175672_,
            p_175673_,
            p_175674_,
            Blocks.POWDER_SNOW_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, Integer.valueOf(3)),
            SoundEvents.BUCKET_EMPTY_POWDER_SNOW
        );
    CauldronInteraction SHULKER_BOX = (p_175662_, p_175663_, p_175664_, p_175665_, p_175666_, p_175667_) -> {
        Block block = Block.byItem(p_175667_.getItem());
        if (!(block instanceof ShulkerBoxBlock)) {
            return InteractionResult.PASS;
        } else {
            if (!p_175663_.isClientSide) {
                ItemStack itemstack = new ItemStack(Blocks.SHULKER_BOX);
                if (p_175667_.hasTag()) {
                    itemstack.setTag(p_175667_.getTag().copy());
                }
                net.neoforged.neoforge.attachment.AttachmentInternals.copyStackAttachments(p_175667_, itemstack);

                p_175665_.setItemInHand(p_175666_, itemstack);
                p_175665_.awardStat(Stats.CLEAN_SHULKER_BOX);
                LayeredCauldronBlock.lowerFillLevel(p_175662_, p_175663_, p_175664_);
            }

            return InteractionResult.sidedSuccess(p_175663_.isClientSide);
        }
    };
    CauldronInteraction BANNER = (p_278890_, p_278891_, p_278892_, p_278893_, p_278894_, p_278895_) -> {
        if (BannerBlockEntity.getPatternCount(p_278895_) <= 0) {
            return InteractionResult.PASS;
        } else {
            if (!p_278891_.isClientSide) {
                ItemStack itemstack = p_278895_.copyWithCount(1);
                BannerBlockEntity.removeLastPattern(itemstack);
                if (!p_278893_.getAbilities().instabuild) {
                    p_278895_.shrink(1);
                }

                if (p_278895_.isEmpty()) {
                    p_278893_.setItemInHand(p_278894_, itemstack);
                } else if (p_278893_.getInventory().add(itemstack)) {
                    p_278893_.inventoryMenu.sendAllDataToRemote();
                } else {
                    p_278893_.drop(itemstack, false);
                }

                p_278893_.awardStat(Stats.CLEAN_BANNER);
                LayeredCauldronBlock.lowerFillLevel(p_278890_, p_278891_, p_278892_);
            }

            return InteractionResult.sidedSuccess(p_278891_.isClientSide);
        }
    };
    CauldronInteraction DYED_ITEM = (p_175629_, p_175630_, p_175631_, p_175632_, p_175633_, p_175634_) -> {
        Item item = p_175634_.getItem();
        if (!(item instanceof DyeableLeatherItem)) {
            return InteractionResult.PASS;
        } else {
            DyeableLeatherItem dyeableleatheritem = (DyeableLeatherItem)item;
            if (!dyeableleatheritem.hasCustomColor(p_175634_)) {
                return InteractionResult.PASS;
            } else {
                if (!p_175630_.isClientSide) {
                    dyeableleatheritem.clearColor(p_175634_);
                    p_175632_.awardStat(Stats.CLEAN_ARMOR);
                    LayeredCauldronBlock.lowerFillLevel(p_175629_, p_175630_, p_175631_);
                }

                return InteractionResult.sidedSuccess(p_175630_.isClientSide);
            }
        }
    };

    static CauldronInteraction.InteractionMap newInteractionMap(String pName) {
        Object2ObjectOpenHashMap<Item, CauldronInteraction> object2objectopenhashmap = new Object2ObjectOpenHashMap<>();
        object2objectopenhashmap.defaultReturnValue((p_175739_, p_175740_, p_175741_, p_175742_, p_175743_, p_175744_) -> InteractionResult.PASS);
        CauldronInteraction.InteractionMap cauldroninteraction$interactionmap = new CauldronInteraction.InteractionMap(pName, object2objectopenhashmap);
        INTERACTIONS.put(pName, cauldroninteraction$interactionmap);
        return cauldroninteraction$interactionmap;
    }

    InteractionResult interact(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, ItemStack pStack);

    static void bootStrap() {
        Map<Item, CauldronInteraction> map = EMPTY.map();
        addDefaultInteractions(map);
        map.put(Items.POTION, (p_175732_, p_175733_, p_175734_, p_175735_, p_175736_, p_175737_) -> {
            if (PotionUtils.getPotion(p_175737_) != Potions.WATER) {
                return InteractionResult.PASS;
            } else {
                if (!p_175733_.isClientSide) {
                    Item item = p_175737_.getItem();
                    p_175735_.setItemInHand(p_175736_, ItemUtils.createFilledResult(p_175737_, p_175735_, new ItemStack(Items.GLASS_BOTTLE)));
                    p_175735_.awardStat(Stats.USE_CAULDRON);
                    p_175735_.awardStat(Stats.ITEM_USED.get(item));
                    p_175733_.setBlockAndUpdate(p_175734_, Blocks.WATER_CAULDRON.defaultBlockState());
                    p_175733_.playSound(null, p_175734_, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    p_175733_.gameEvent(null, GameEvent.FLUID_PLACE, p_175734_);
                }

                return InteractionResult.sidedSuccess(p_175733_.isClientSide);
            }
        });
        Map<Item, CauldronInteraction> map1 = WATER.map();
        addDefaultInteractions(map1);
        map1.put(
            Items.BUCKET,
            (p_175725_, p_175726_, p_175727_, p_175728_, p_175729_, p_175730_) -> fillBucket(
                    p_175725_,
                    p_175726_,
                    p_175727_,
                    p_175728_,
                    p_175729_,
                    p_175730_,
                    new ItemStack(Items.WATER_BUCKET),
                    p_175660_ -> p_175660_.getValue(LayeredCauldronBlock.LEVEL) == 3,
                    SoundEvents.BUCKET_FILL
                )
        );
        map1.put(
            Items.GLASS_BOTTLE,
            (p_175718_, p_175719_, p_175720_, p_175721_, p_175722_, p_175723_) -> {
                if (!p_175719_.isClientSide) {
                    Item item = p_175723_.getItem();
                    p_175721_.setItemInHand(
                        p_175722_, ItemUtils.createFilledResult(p_175723_, p_175721_, PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER))
                    );
                    p_175721_.awardStat(Stats.USE_CAULDRON);
                    p_175721_.awardStat(Stats.ITEM_USED.get(item));
                    LayeredCauldronBlock.lowerFillLevel(p_175718_, p_175719_, p_175720_);
                    p_175719_.playSound(null, p_175720_, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    p_175719_.gameEvent(null, GameEvent.FLUID_PICKUP, p_175720_);
                }
    
                return InteractionResult.sidedSuccess(p_175719_.isClientSide);
            }
        );
        map1.put(Items.POTION, (p_175704_, p_175705_, p_175706_, p_175707_, p_175708_, p_175709_) -> {
            if (p_175704_.getValue(LayeredCauldronBlock.LEVEL) != 3 && PotionUtils.getPotion(p_175709_) == Potions.WATER) {
                if (!p_175705_.isClientSide) {
                    p_175707_.setItemInHand(p_175708_, ItemUtils.createFilledResult(p_175709_, p_175707_, new ItemStack(Items.GLASS_BOTTLE)));
                    p_175707_.awardStat(Stats.USE_CAULDRON);
                    p_175707_.awardStat(Stats.ITEM_USED.get(p_175709_.getItem()));
                    p_175705_.setBlockAndUpdate(p_175706_, p_175704_.cycle(LayeredCauldronBlock.LEVEL));
                    p_175705_.playSound(null, p_175706_, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    p_175705_.gameEvent(null, GameEvent.FLUID_PLACE, p_175706_);
                }

                return InteractionResult.sidedSuccess(p_175705_.isClientSide);
            } else {
                return InteractionResult.PASS;
            }
        });
        map1.put(Items.LEATHER_BOOTS, DYED_ITEM);
        map1.put(Items.LEATHER_LEGGINGS, DYED_ITEM);
        map1.put(Items.LEATHER_CHESTPLATE, DYED_ITEM);
        map1.put(Items.LEATHER_HELMET, DYED_ITEM);
        map1.put(Items.LEATHER_HORSE_ARMOR, DYED_ITEM);
        map1.put(Items.WHITE_BANNER, BANNER);
        map1.put(Items.GRAY_BANNER, BANNER);
        map1.put(Items.BLACK_BANNER, BANNER);
        map1.put(Items.BLUE_BANNER, BANNER);
        map1.put(Items.BROWN_BANNER, BANNER);
        map1.put(Items.CYAN_BANNER, BANNER);
        map1.put(Items.GREEN_BANNER, BANNER);
        map1.put(Items.LIGHT_BLUE_BANNER, BANNER);
        map1.put(Items.LIGHT_GRAY_BANNER, BANNER);
        map1.put(Items.LIME_BANNER, BANNER);
        map1.put(Items.MAGENTA_BANNER, BANNER);
        map1.put(Items.ORANGE_BANNER, BANNER);
        map1.put(Items.PINK_BANNER, BANNER);
        map1.put(Items.PURPLE_BANNER, BANNER);
        map1.put(Items.RED_BANNER, BANNER);
        map1.put(Items.YELLOW_BANNER, BANNER);
        map1.put(Items.WHITE_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.GRAY_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.BLACK_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.BLUE_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.BROWN_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.CYAN_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.GREEN_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.LIGHT_BLUE_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.LIGHT_GRAY_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.LIME_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.MAGENTA_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.ORANGE_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.PINK_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.PURPLE_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.RED_SHULKER_BOX, SHULKER_BOX);
        map1.put(Items.YELLOW_SHULKER_BOX, SHULKER_BOX);
        Map<Item, CauldronInteraction> map2 = LAVA.map();
        map2.put(
            Items.BUCKET,
            (p_175697_, p_175698_, p_175699_, p_175700_, p_175701_, p_175702_) -> fillBucket(
                    p_175697_,
                    p_175698_,
                    p_175699_,
                    p_175700_,
                    p_175701_,
                    p_175702_,
                    new ItemStack(Items.LAVA_BUCKET),
                    p_175651_ -> true,
                    SoundEvents.BUCKET_FILL_LAVA
                )
        );
        addDefaultInteractions(map2);
        Map<Item, CauldronInteraction> map3 = POWDER_SNOW.map();
        map3.put(
            Items.BUCKET,
            (p_175690_, p_175691_, p_175692_, p_175693_, p_175694_, p_175695_) -> fillBucket(
                    p_175690_,
                    p_175691_,
                    p_175692_,
                    p_175693_,
                    p_175694_,
                    p_175695_,
                    new ItemStack(Items.POWDER_SNOW_BUCKET),
                    p_175627_ -> p_175627_.getValue(LayeredCauldronBlock.LEVEL) == 3,
                    SoundEvents.BUCKET_FILL_POWDER_SNOW
                )
        );
        addDefaultInteractions(map3);
    }

    static void addDefaultInteractions(Map<Item, CauldronInteraction> pInteractionsMap) {
        pInteractionsMap.put(Items.LAVA_BUCKET, FILL_LAVA);
        pInteractionsMap.put(Items.WATER_BUCKET, FILL_WATER);
        pInteractionsMap.put(Items.POWDER_SNOW_BUCKET, FILL_POWDER_SNOW);
    }

    static InteractionResult fillBucket(
        BlockState pBlockState,
        Level pLevel,
        BlockPos pPos,
        Player pPlayer,
        InteractionHand pHand,
        ItemStack pEmptyStack,
        ItemStack pFilledStack,
        Predicate<BlockState> pStatePredicate,
        SoundEvent pFillSound
    ) {
        if (!pStatePredicate.test(pBlockState)) {
            return InteractionResult.PASS;
        } else {
            if (!pLevel.isClientSide) {
                Item item = pEmptyStack.getItem();
                pPlayer.setItemInHand(pHand, ItemUtils.createFilledResult(pEmptyStack, pPlayer, pFilledStack));
                pPlayer.awardStat(Stats.USE_CAULDRON);
                pPlayer.awardStat(Stats.ITEM_USED.get(item));
                pLevel.setBlockAndUpdate(pPos, Blocks.CAULDRON.defaultBlockState());
                pLevel.playSound(null, pPos, pFillSound, SoundSource.BLOCKS, 1.0F, 1.0F);
                pLevel.gameEvent(null, GameEvent.FLUID_PICKUP, pPos);
            }

            return InteractionResult.sidedSuccess(pLevel.isClientSide);
        }
    }

    static InteractionResult emptyBucket(
        Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, ItemStack pFilledStack, BlockState pState, SoundEvent pEmptySound
    ) {
        if (!pLevel.isClientSide) {
            Item item = pFilledStack.getItem();
            pPlayer.setItemInHand(pHand, ItemUtils.createFilledResult(pFilledStack, pPlayer, new ItemStack(Items.BUCKET)));
            pPlayer.awardStat(Stats.FILL_CAULDRON);
            pPlayer.awardStat(Stats.ITEM_USED.get(item));
            pLevel.setBlockAndUpdate(pPos, pState);
            pLevel.playSound(null, pPos, pEmptySound, SoundSource.BLOCKS, 1.0F, 1.0F);
            pLevel.gameEvent(null, GameEvent.FLUID_PLACE, pPos);
        }

        return InteractionResult.sidedSuccess(pLevel.isClientSide);
    }

    public static record InteractionMap(String name, Map<Item, CauldronInteraction> map) {
    }
}
