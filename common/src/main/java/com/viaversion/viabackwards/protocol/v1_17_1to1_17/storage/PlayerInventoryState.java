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
package com.viaversion.viabackwards.protocol.v1_17_1to1_17.storage;

import com.viaversion.viaversion.api.connection.StorableObject;

public final class PlayerInventoryState implements StorableObject {

    private short selectedHotbarSlot = 0;
    private int openContainerId = 0; // 0 = none open (the server never assigns 0 to a screen)

    public short selectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    public void setSelectedHotbarSlot(final short slot) {
        this.selectedHotbarSlot = slot;
    }

    public int openContainerId() {
        return openContainerId;
    }

    public void setOpenContainerId(final int containerId) {
        this.openContainerId = containerId;
    }
}
