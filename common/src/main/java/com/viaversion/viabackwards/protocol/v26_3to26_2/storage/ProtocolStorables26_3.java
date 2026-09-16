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

import com.viaversion.viaversion.connection.ProtocolStorablesBase;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ProtocolStorables26_3 extends ProtocolStorablesBase {

    private Integer currentTeleportId;
    private int brewingStandContainerId = -1;

    public @Nullable Integer currentTeleportId() {
        return currentTeleportId;
    }

    public void setCurrentTeleportId(@Nullable final Integer currentTeleportId) {
        this.currentTeleportId = currentTeleportId;
    }

    public int brewingStandContainerId() {
        return brewingStandContainerId;
    }

    public void setBrewingStandContainerId(final int brewingStandContainerId) {
        this.brewingStandContainerId = brewingStandContainerId;
    }
}
