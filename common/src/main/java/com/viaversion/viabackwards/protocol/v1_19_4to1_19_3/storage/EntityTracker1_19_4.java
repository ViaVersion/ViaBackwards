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
package com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.storage;

import com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.Protocol1_19_4To1_19_3;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.TrackedEntity;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_3;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_4;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.fastutil.ints.IntOpenHashSet;
import com.viaversion.viaversion.libs.fastutil.ints.IntSet;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class EntityTracker1_19_4 extends EntityTrackerBase {

    private final IntSet generatedEntities = new IntOpenHashSet(); // Track entities spawned to prevent duplicated entity ids

    public EntityTracker1_19_4(final UserConnection connection) {
        super(connection, EntityTypes1_19_4.PLAYER);
    }

    public int spawnEntity(final EntityTypes1_19_3 entityType, final int data) {
        final int entityId = nextEntityId();

        final PacketWrapper addEntity = PacketWrapper.create(ClientboundPackets1_19_3.ADD_ENTITY, user());
        addEntity.write(Types.VAR_INT, entityId); // Entity id
        addEntity.write(Types.UUID, UUID.randomUUID()); // Entity UUID
        addEntity.write(Types.VAR_INT, entityType.getId()); // Entity type
        addEntity.write(Types.DOUBLE, 0.0); // X
        addEntity.write(Types.DOUBLE, 0.0); // Y
        addEntity.write(Types.DOUBLE, 0.0); // Z
        addEntity.write(Types.BYTE, (byte) 0); // Pitch
        addEntity.write(Types.BYTE, (byte) 0); // Yaw
        addEntity.write(Types.BYTE, (byte) 0); // Head yaw
        addEntity.write(Types.VAR_INT, data); // Data
        addEntity.write(Types.SHORT, (short) 0); // Velocity X
        addEntity.write(Types.SHORT, (short) 0); // Velocity Y
        addEntity.write(Types.SHORT, (short) 0); // Velocity Z

        addEntity.send(Protocol1_19_4To1_19_3.class);

        generatedEntities.add(entityId);
        return entityId;
    }


    @Override
    public void clearEntities() {
        for (final int id : entities.keySet()) {
            clearLinkedEntityStorage(id);
        }
        super.clearEntities();
    }

    @Override
    public void removeEntity(final int id) {
        clearLinkedEntityStorage(id);
        super.removeEntity(id);
    }

    public void clearLinkedEntityStorage(final int id) {
        final TrackedEntity entity = entity(id);
        if (entity == null || !entity.hasData()) {
            return;
        }

        final LinkedEntityStorage storage = entity.data().get(LinkedEntityStorage.class);
        if (storage != null) {
            storage.remove(user());
            generatedEntities.remove(id);
        }
    }

    private int nextEntityId() {
        final int entityId = -ThreadLocalRandom.current().nextInt(10_000);
        if (generatedEntities.contains(entityId)) {
            return nextEntityId();
        }
        return entityId;
    }

}
