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
package com.viaversion.viabackwards.api.entities.storage;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;

public class PlayerPosRotStorage extends PlayerPositionStorage {
    private float yRot; // yaw
    private float xRot; // pitch

    public float yRot() {
        return yRot;
    }

    public void setYRot(final float yRot) {
        this.yRot = yRot;
    }

    public float xRot() {
        return xRot;
    }

    public void setXRot(final float xRot) {
        this.xRot = xRot;
    }

    public void setRotation(final float yRot, final float xRot) {
        this.yRot = yRot;
        this.xRot = xRot;
    }

    public void setRotFromPacket(final PacketWrapper wrapper) {
        this.yRot = wrapper.passthrough(Types.FLOAT);
        this.xRot = wrapper.passthrough(Types.FLOAT);
    }

    public void setPosRotFromPacket(final PacketWrapper wrapper) {
        this.setPosFromPacket(wrapper);
        this.setRotFromPacket(wrapper);
    }
}
