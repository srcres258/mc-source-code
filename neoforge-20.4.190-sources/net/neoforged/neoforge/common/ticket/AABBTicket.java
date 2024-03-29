/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforge.common.ticket;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AABBTicket extends SimpleTicket<Vec3> {
    public final AABB axisAlignedBB;

    public AABBTicket(AABB axisAlignedBB) {
        this.axisAlignedBB = axisAlignedBB;
    }

    @Override
    public boolean matches(Vec3 toMatch) {
        return this.axisAlignedBB.contains(toMatch);
    }
}
