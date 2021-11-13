/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
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
package com.viaversion.viabackwards.api.entities.storage;

import com.viaversion.viabackwards.api.BackwardsProtocol;

public class EntityObjectData extends EntityData {
    private final int objectData;

    public EntityObjectData(BackwardsProtocol<?, ?, ?, ?> protocol, String key, int id, int replacementId, int objectData) {
        super(protocol, key, id, replacementId);
        this.objectData = objectData;
    }

    @Override
    public boolean isObjectType() {
        return true;
    }

    @Override
    public int objectData() {
        return objectData;
    }
}
