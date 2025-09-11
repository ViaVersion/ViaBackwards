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

import java.util.UUID;

public final class MannequinData {
    private final UUID uuid;
    private final String name;
    private boolean hasTeam;

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
}
