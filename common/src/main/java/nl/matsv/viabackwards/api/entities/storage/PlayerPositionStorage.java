/*
 * ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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
package nl.matsv.viabackwards.api.entities.storage;

import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.StoredObject;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.type.Type;

public abstract class PlayerPositionStorage extends StoredObject {
    private double x;
    private double y;
    private double z;

    protected PlayerPositionStorage(UserConnection user) {
        super(user);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public void setX(final double x) {
        this.x = x;
    }

    public void setY(final double y) {
        this.y = y;
    }

    public void setZ(final double z) {
        this.z = z;
    }

    public void setCoordinates(PacketWrapper wrapper, boolean relative) throws Exception {
        setCoordinates(wrapper.get(Type.DOUBLE, 0), wrapper.get(Type.DOUBLE, 1), wrapper.get(Type.DOUBLE, 2), relative);
    }

    public void setCoordinates(double x, double y, double z, boolean relative) {
        if (relative) {
            this.x += x;
            this.y += y;
            this.z += z;
        } else {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
