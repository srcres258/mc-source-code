/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.util;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;

public class ItemStackMap {
    public static <V> Map<ItemStack, V> createTypeAndTagLinkedMap() {
        return new Object2ObjectLinkedOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG);
    }

    public static <V> Map<ItemStack, V> createTypeAndTagMap() {
        return new Object2ObjectOpenCustomHashMap<>(ItemStackLinkedSet.TYPE_AND_TAG);
    }
}
