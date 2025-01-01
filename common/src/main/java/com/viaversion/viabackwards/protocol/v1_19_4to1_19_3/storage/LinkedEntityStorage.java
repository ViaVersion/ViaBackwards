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
package com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.storage;

import com.viaversion.viabackwards.api.entities.storage.EntityPositionStorage;
import com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.Protocol1_19_4To1_19_3;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;

public class LinkedEntityStorage extends EntityPositionStorage implements StorableObject {

    private int[] entities;

    public int[] entities() {
        return entities;
    }

    public void setEntities(final int... entities) {
        this.entities = entities;
    }

    public void remove(final UserConnection connection) {
        final PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_19_3.REMOVE_ENTITIES, connection);
        wrapper.write(Types.VAR_INT_ARRAY_PRIMITIVE, entities);

        wrapper.send(Protocol1_19_4To1_19_3.class);
    }
}
