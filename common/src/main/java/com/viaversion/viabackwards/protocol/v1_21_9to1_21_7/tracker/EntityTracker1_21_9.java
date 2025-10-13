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
package com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.tracker;

import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.Protocol1_21_9To1_21_7;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.storage.MannequinData;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPackets1_21_6;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class EntityTracker1_21_9 extends EntityTrackerBase {

    public EntityTracker1_21_9(final UserConnection connection, @Nullable final EntityType playerType) {
        super(connection, playerType);
    }

    @Override
    public void removeEntity(final int id) {
        sendRemovePacket(id);
        super.removeEntity(id);
    }

    public void sendRemovePacket(final int id) {
        final StoredEntityData entityData = entityDataIfPresent(id);
        final MannequinData trackedEntityId;
        if (entityData != null && (trackedEntityId = entityData.get(MannequinData.class)) != null) {
            final PacketWrapper playerInfoRemove = PacketWrapper.create(ClientboundPackets1_21_6.PLAYER_INFO_REMOVE, user());
            playerInfoRemove.write(Types.UUID_ARRAY, new UUID[]{trackedEntityId.uuid()});
            playerInfoRemove.send(Protocol1_21_9To1_21_7.class);

            final PacketWrapper removePlayerTeam = PacketWrapper.create(ClientboundPackets1_21_6.SET_PLAYER_TEAM, user());
            removePlayerTeam.write(Types.STRING, trackedEntityId.name());
            removePlayerTeam.write(Types.BYTE, (byte) 1); // Method
            removePlayerTeam.send(Protocol1_21_9To1_21_7.class);
        }
    }
}
