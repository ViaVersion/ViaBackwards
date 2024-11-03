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
package com.viaversion.viabackwards.protocol.v1_19to1_18_2.storage;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.fastutil.ints.IntArrayList;
import com.viaversion.viaversion.libs.fastutil.ints.IntList;

public final class EntityTracker1_19 extends EntityTrackerBase {

    private final IntList affectedByBlindness = new IntArrayList();
    private final IntList affectedByDarkness = new IntArrayList();

    public EntityTracker1_19(final UserConnection connection) {
        super(connection, EntityTypes1_19.PLAYER);
    }

    @Override
    public void removeEntity(final int id) {
        super.removeEntity(id);
        this.affectedByBlindness.rem(id);
        this.affectedByDarkness.rem(id);
    }

    public IntList getAffectedByBlindness() {
        return affectedByBlindness;
    }

    public IntList getAffectedByDarkness() {
        return affectedByDarkness;
    }
}
