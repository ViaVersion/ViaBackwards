/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage;

import com.viaversion.viabackwards.api.entities.storage.PlayerPositionStorage;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;

public final class PlayerStorage extends PlayerPositionStorage {

    private float yaw;
    private float pitch;

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public void setPosition(PacketWrapper wrapper) {
        setX(wrapper.get(Types.DOUBLE, 0));
        setY(wrapper.get(Types.DOUBLE, 1));
        setZ(wrapper.get(Types.DOUBLE, 2));
    }

    public void setRotation(final PacketWrapper wrapper) {
        this.yaw = wrapper.get(Types.FLOAT, 0);
        this.pitch = wrapper.get(Types.FLOAT, 1);
    }

}
