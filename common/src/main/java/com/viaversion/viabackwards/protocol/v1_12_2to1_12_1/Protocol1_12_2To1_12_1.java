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

package com.viaversion.viabackwards.protocol.v1_12_2to1_12_1;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.protocol.v1_12_2to1_12_1.storage.KeepAliveTracker;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ServerboundPackets1_12_1;

public class Protocol1_12_2To1_12_1 extends BackwardsProtocol<ClientboundPackets1_12_1, ClientboundPackets1_12_1, ServerboundPackets1_12_1, ServerboundPackets1_12_1> {

    public Protocol1_12_2To1_12_1() {
        super(ClientboundPackets1_12_1.class, ClientboundPackets1_12_1.class, ServerboundPackets1_12_1.class, ServerboundPackets1_12_1.class);
    }

    @Override
    protected void registerPackets() {
        registerClientbound(ClientboundPackets1_12_1.KEEP_ALIVE, new PacketHandlers() {
            @Override
            public void register() {
                handler(packetWrapper -> {
                    Long keepAlive = packetWrapper.read(Types.LONG);
                    packetWrapper.user().get(KeepAliveTracker.class).setKeepAlive(keepAlive);
                    packetWrapper.write(Types.VAR_INT, keepAlive.hashCode());
                });
            }
        });

        registerServerbound(ServerboundPackets1_12_1.KEEP_ALIVE, new PacketHandlers() {
            @Override
            public void register() {
                handler(packetWrapper -> {
                    int keepAlive = packetWrapper.read(Types.VAR_INT);
                    long realKeepAlive = packetWrapper.user().get(KeepAliveTracker.class).getKeepAlive();
                    if (keepAlive != Long.hashCode(realKeepAlive)) {
                        packetWrapper.cancel(); // Wrong data, cancel packet
                        return;
                    }
                    packetWrapper.write(Types.LONG, realKeepAlive);
                    // Reset KeepAliveTracker (to prevent sending same valid value in a row causing a timeout)
                    packetWrapper.user().get(KeepAliveTracker.class).setKeepAlive(Integer.MAX_VALUE);
                });
            }
        });
    }

    @Override
    public void init(UserConnection userConnection) {
        userConnection.put(new KeepAliveTracker());
    }
}
