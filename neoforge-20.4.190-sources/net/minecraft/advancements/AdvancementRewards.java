package net.minecraft.advancements;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CacheableFunction;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public record AdvancementRewards(int experience, List<ResourceLocation> loot, List<ResourceLocation> recipes, Optional<CacheableFunction> function) {
    public static final Codec<AdvancementRewards> CODEC = RecordCodecBuilder.create(
        p_311395_ -> p_311395_.group(
                    ExtraCodecs.strictOptionalField(Codec.INT, "experience", 0).forGetter(AdvancementRewards::experience),
                    ExtraCodecs.strictOptionalField(ResourceLocation.CODEC.listOf(), "loot", List.of()).forGetter(AdvancementRewards::loot),
                    ExtraCodecs.strictOptionalField(ResourceLocation.CODEC.listOf(), "recipes", List.of()).forGetter(AdvancementRewards::recipes),
                    ExtraCodecs.strictOptionalField(CacheableFunction.CODEC, "function").forGetter(AdvancementRewards::function)
                )
                .apply(p_311395_, AdvancementRewards::new)
    );
    public static final AdvancementRewards EMPTY = new AdvancementRewards(0, List.of(), List.of(), Optional.empty());

    public void grant(ServerPlayer pPlayer) {
        pPlayer.giveExperiencePoints(this.experience);
        LootParams lootparams = new LootParams.Builder(pPlayer.serverLevel())
            .withParameter(LootContextParams.THIS_ENTITY, pPlayer)
            .withParameter(LootContextParams.ORIGIN, pPlayer.position())
            .withLuck(pPlayer.getLuck()) // Forge: Add luck to LootContext
            .create(LootContextParamSets.ADVANCEMENT_REWARD);
        boolean flag = false;

        for(ResourceLocation resourcelocation : this.loot) {
            for(ItemStack itemstack : pPlayer.server.getLootData().getLootTable(resourcelocation).getRandomItems(lootparams)) {
                if (pPlayer.addItem(itemstack)) {
                    pPlayer.level()
                        .playSound(
                            null,
                            pPlayer.getX(),
                            pPlayer.getY(),
                            pPlayer.getZ(),
                            SoundEvents.ITEM_PICKUP,
                            SoundSource.PLAYERS,
                            0.2F,
                            ((pPlayer.getRandom().nextFloat() - pPlayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
                        );
                    flag = true;
                } else {
                    ItemEntity itementity = pPlayer.drop(itemstack, false);
                    if (itementity != null) {
                        itementity.setNoPickUpDelay();
                        itementity.setTarget(pPlayer.getUUID());
                    }
                }
            }
        }

        if (flag) {
            pPlayer.containerMenu.broadcastChanges();
        }

        if (!this.recipes.isEmpty()) {
            pPlayer.awardRecipesByKey(this.recipes);
        }

        MinecraftServer minecraftserver = pPlayer.server;
        this.function
            .flatMap(p_311397_ -> p_311397_.get(minecraftserver.getFunctions()))
            .ifPresent(
                p_311400_ -> minecraftserver.getFunctions().execute(p_311400_, pPlayer.createCommandSourceStack().withSuppressedOutput().withPermission(2))
            );
    }

    public static class Builder {
        private int experience;
        private final ImmutableList.Builder<ResourceLocation> loot = ImmutableList.builder();
        private final ImmutableList.Builder<ResourceLocation> recipes = ImmutableList.builder();
        private Optional<ResourceLocation> function = Optional.empty();

        /**
         * Creates a new builder with the given amount of experience as a reward
         */
        public static AdvancementRewards.Builder experience(int pExperience) {
            return new AdvancementRewards.Builder().addExperience(pExperience);
        }

        /**
         * Adds the given amount of experience. (Not a direct setter)
         */
        public AdvancementRewards.Builder addExperience(int pExperience) {
            this.experience += pExperience;
            return this;
        }

        public static AdvancementRewards.Builder loot(ResourceLocation pLootTableId) {
            return new AdvancementRewards.Builder().addLootTable(pLootTableId);
        }

        public AdvancementRewards.Builder addLootTable(ResourceLocation pLootTableId) {
            this.loot.add(pLootTableId);
            return this;
        }

        /**
         * Creates a new builder with the given recipe as a reward.
         */
        public static AdvancementRewards.Builder recipe(ResourceLocation pRecipeId) {
            return new AdvancementRewards.Builder().addRecipe(pRecipeId);
        }

        /**
         * Adds the given recipe to the rewards.
         */
        public AdvancementRewards.Builder addRecipe(ResourceLocation pRecipeId) {
            this.recipes.add(pRecipeId);
            return this;
        }

        public static AdvancementRewards.Builder function(ResourceLocation pFunctionId) {
            return new AdvancementRewards.Builder().runs(pFunctionId);
        }

        public AdvancementRewards.Builder runs(ResourceLocation pFunctionId) {
            this.function = Optional.of(pFunctionId);
            return this;
        }

        public AdvancementRewards build() {
            return new AdvancementRewards(this.experience, this.loot.build(), this.recipes.build(), this.function.map(CacheableFunction::new));
        }
    }
}
