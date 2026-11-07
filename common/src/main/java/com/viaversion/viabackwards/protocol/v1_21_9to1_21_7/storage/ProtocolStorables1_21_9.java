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
package com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.storage;

import com.viaversion.viaversion.connection.ProtocolStorablesBase;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.storage.BundleStateTracker;

public final class ProtocolStorables1_21_9 extends ProtocolStorablesBase {

    private final PlayerRotationStorage playerRotationStorage = new PlayerRotationStorage();
    private final DimensionScaleStorage dimensionScaleStorage = new DimensionScaleStorage();
    private final BundleStateTracker bundleStateTracker = new BundleStateTracker();
    private final MannequinTeamStorage mannequinTeamStorage = new MannequinTeamStorage();

    public PlayerRotationStorage playerRotationStorage() {
        return playerRotationStorage;
    }

    public DimensionScaleStorage dimensionScaleStorage() {
        return dimensionScaleStorage;
    }

    public BundleStateTracker bundleStateTracker() {
        return bundleStateTracker;
    }

    public MannequinTeamStorage mannequinTeamStorage() {
        return mannequinTeamStorage;
    }
}
