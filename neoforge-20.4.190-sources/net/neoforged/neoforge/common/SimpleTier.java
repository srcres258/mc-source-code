/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common;

import java.util.function.Supplier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * Helper class to define a custom tier
 */
public final class SimpleTier implements Tier {
    private final int level;
    private final int uses;
    private final float speed;
    private final float attackDamageBonus;
    private final int enchantmentValue;

    private final TagKey<Block> tag;

    private final Supplier<Ingredient> repairIngredient;

    public SimpleTier(int level, int uses, float speed, float attackDamageBonus, int enchantmentValue,
            TagKey<Block> tag, Supplier<Ingredient> repairIngredient) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.attackDamageBonus = attackDamageBonus;
        this.enchantmentValue = enchantmentValue;
        this.tag = tag;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return this.speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.attackDamageBonus;
    }

    @Override
    public int getLevel() {
        return this.level;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    public TagKey<Block> getTag() {
        return this.tag;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String toString() {
        return "SimpleTier[" +
                "level=" + level + ", " +
                "uses=" + uses + ", " +
                "speed=" + speed + ", " +
                "attackDamageBonus=" + attackDamageBonus + ", " +
                "enchantmentValue=" + enchantmentValue + ", " +
                "tag=" + tag + ", " +
                "repairIngredient=" + repairIngredient + ']';
    }
}
