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
package nl.matsv.viabackwards.api.entities.storage;

public class EntityObjectData extends EntityData {
    private final boolean isObject;
    private final int objectData;

    public EntityObjectData(int id, boolean isObject, int replacementId, int objectData) {
        super(id, replacementId);
        this.isObject = isObject;
        this.objectData = objectData;
    }

    @Override
    public boolean isObject() {
        return isObject;
    }

    @Override
    public int getObjectData() {
        return objectData;
    }
}
