/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.v26_3to26_2.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CushionData implements StorableObject {

    private final UUID uuid;
    private final List<EntityData> entityData = new ArrayList<>();
    private double x;
    private double y;
    private double z;
    private int color;

    public CushionData(final UUID uuid, final double x, final double y, final double z) {
        this.uuid = uuid;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public UUID uuid() {
        return uuid;
    }

    public List<EntityData> entityData() {
        return entityData;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public void setPosition(final double x, final double y, final double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int color() {
        return color;
    }

    public void setColor(final int color) {
        this.color = color;
    }

}
