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

package nl.matsv.viabackwards.protocol.protocol1_12_1to1_12_2;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_12_1to1_12.ClientboundPackets1_12_1;
import us.myles.ViaVersion.protocols.protocol1_12_1to1_12.ServerboundPackets1_12_1;

public class Protocol1_12_1To1_12_2 extends BackwardsProtocol<ClientboundPackets1_12_1, ClientboundPackets1_12_1, ServerboundPackets1_12_1, ServerboundPackets1_12_1> {

    public Protocol1_12_1To1_12_2() {
        super(ClientboundPackets1_12_1.class, ClientboundPackets1_12_1.class, ServerboundPackets1_12_1.class, ServerboundPackets1_12_1.class);
    }

    @Override
    protected void registerPackets() {
        registerOutgoing(ClientboundPackets1_12_1.KEEP_ALIVE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper packetWrapper) throws Exception {
                        Long keepAlive = packetWrapper.read(Type.LONG);
                        packetWrapper.user().get(KeepAliveTracker.class).setKeepAlive(keepAlive);
                        packetWrapper.write(Type.VAR_INT, keepAlive.hashCode());
                    }
                });
            }
        });

        registerIncoming(ServerboundPackets1_12_1.KEEP_ALIVE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper packetWrapper) throws Exception {
                        int keepAlive = packetWrapper.read(Type.VAR_INT);
                        long realKeepAlive = packetWrapper.user().get(KeepAliveTracker.class).getKeepAlive();
                        if (keepAlive != Long.hashCode(realKeepAlive)) {
                            packetWrapper.cancel(); // Wrong data, cancel packet
                            return;
                        }
                        packetWrapper.write(Type.LONG, realKeepAlive);
                        // Reset KeepAliveTracker (to prevent sending same valid value in a row causing a timeout)
                        packetWrapper.user().get(KeepAliveTracker.class).setKeepAlive(Integer.MAX_VALUE);
                    }
                });
            }
        });
    }

    @Override
    public void init(UserConnection userConnection) {
        userConnection.put(new KeepAliveTracker(userConnection));
    }
}
