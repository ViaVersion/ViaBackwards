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
package com.viaversion.viabackwards.protocol.v1_21_2to1_21.task;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.PlayerStorage;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.connection.StorableObjectTask;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.storage.ClientVehicleStorage;
import java.util.logging.Level;

public final class PlayerPacketsTickTask extends StorableObjectTask<PlayerStorage> {

    public PlayerPacketsTickTask() {
        super(PlayerStorage.class);
    }

    @Override
    public void run(final UserConnection connection, final PlayerStorage storableObject) {
        final ProtocolInfo protocolInfo = connection.getProtocolInfo();
        if (protocolInfo.getClientState() != State.PLAY || protocolInfo.getServerState() != State.PLAY) {
            return;
        }

        final EntityTracker entityTracker = connection.getEntityTracker(Protocol1_21_2To1_21.class);
        if (!entityTracker.hasClientEntityId()) {
            return;
        }

        try {
            if (!connection.has(ClientVehicleStorage.class)) {
                storableObject.tick(connection);
            }
        } catch (final Throwable t) {
            ViaBackwards.getPlatform().getLogger().log(Level.SEVERE, "Error while sending player input packet.", t);
        }

        try {
            final PacketWrapper clientTickEndPacket = PacketWrapper.create(ServerboundPackets1_21_2.CLIENT_TICK_END, connection);
            clientTickEndPacket.sendToServer(Protocol1_21_2To1_21.class);
        } catch (final Throwable t) {
            ViaBackwards.getPlatform().getLogger().log(Level.SEVERE, "Error while sending client tick end packet.", t);
        }
    }
}
