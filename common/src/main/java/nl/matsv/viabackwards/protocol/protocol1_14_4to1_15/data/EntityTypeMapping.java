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
package nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data;

import us.myles.ViaVersion.api.entities.Entity1_14Types;

public class EntityTypeMapping {

    // There's only the bee, so not much to do here
    public static int getOldEntityId(int entityId) {
        if (entityId == 4) return Entity1_14Types.PUFFERFISH.getId(); // Flying pufferfish!
        return entityId >= 5 ? entityId - 1 : entityId;
    }
}
