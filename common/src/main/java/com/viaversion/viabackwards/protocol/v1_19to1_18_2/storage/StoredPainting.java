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
package com.viaversion.viabackwards.protocol.v1_19to1_18_2.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import java.util.UUID;

public final class StoredPainting implements StorableObject {
    private final int entityId;
    private final UUID uuid;
    private final BlockPosition position;
    private final byte direction;
    private int type;

    public StoredPainting(int entityId, UUID uuid, BlockPosition position, byte direction) {
        this.entityId = entityId;
        this.uuid = uuid;
        this.position = position;
        this.direction = direction;
    }

    public StoredPainting(final int entityId, final UUID uuid, final BlockPosition position, final int direction) {
        this(entityId, uuid, position, to2dDirection(direction));
    }

    private static byte to2dDirection(int direction) {
        return switch (direction) {
            case 0, 1 -> -1; // No worky
            case 2 -> 2;
            case 3 -> 0;
            case 4 -> 1;
            case 5 -> 3;
            default -> throw new IllegalArgumentException("Invalid direction: " + direction);
        };
    }

    public int entityId() {
        return entityId;
    }

    public UUID uuid() {
        return uuid;
    }

    public BlockPosition position() {
        return position;
    }

    public byte direction() {
        return direction;
    }

    public int type() {
        return type;
    }

    public void setType(final int type) {
        this.type = type;
    }
}
