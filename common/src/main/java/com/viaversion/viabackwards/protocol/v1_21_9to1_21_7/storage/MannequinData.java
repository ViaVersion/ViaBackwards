/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.storage;

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MannequinData {
    private final UUID uuid;
    private final String name;
    private boolean hasTeam;


    private double x;
    private double y;
    private double z;

    private float yaw;
    private float pitch;

    private int[] passengers;

    private final List<EntityData> entityData = new ArrayList<>();
    private final Map<Byte, Item> itemMap = new HashMap<>();
    private Tag displayName;

    public MannequinData(final UUID uuid, final String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public void setHasTeam(final boolean hasTeam) {
        this.hasTeam = hasTeam;
    }

    public boolean hasTeam() {
        return hasTeam;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setPosition(final double x, final double y, final double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void setRotation(final float yaw, final float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public void setPassengers(final int[] passengers) {
        this.passengers = passengers;
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

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public List<EntityData> entityData() {
        return entityData;
    }

    public int[] passengers() {
        return passengers;
    }

    public void setDisplayName(Tag displayName) {
        this.displayName = displayName;
    }

    public Tag getDisplayName() {
        return displayName;
    }

    public void setEquipment(byte slot, Item item) {
        itemMap.put(slot, item);
    }

    public Map<Byte, Item> getItemMap() {
        return itemMap;
    }
}
