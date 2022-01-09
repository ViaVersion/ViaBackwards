/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_16_3to1_16_4;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.protocol.protocol1_16_3to1_16_4.storage.PlayerHandStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2;

public class Protocol1_16_3To1_16_4 extends BackwardsProtocol<ClientboundPackets1_16_2, ClientboundPackets1_16_2, ServerboundPackets1_16_2, ServerboundPackets1_16_2> {

    public Protocol1_16_3To1_16_4() {
        super(ClientboundPackets1_16_2.class, ClientboundPackets1_16_2.class, ServerboundPackets1_16_2.class, ServerboundPackets1_16_2.class);
    }

    @Override
    protected void registerPackets() {
        registerServerbound(ServerboundPackets1_16_2.EDIT_BOOK, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.FLAT_VAR_INT_ITEM);
                map(Type.BOOLEAN);
                handler(wrapper -> {
                    int slot = wrapper.read(Type.VAR_INT);
                    if (slot == 1) {
                        wrapper.write(Type.VAR_INT, 40); // offhand
                    } else {
                        wrapper.write(Type.VAR_INT, wrapper.user().get(PlayerHandStorage.class).getCurrentHand());
                    }
                });
            }
        });

        registerServerbound(ServerboundPackets1_16_2.HELD_ITEM_CHANGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    short slot = wrapper.passthrough(Type.SHORT);
                    wrapper.user().get(PlayerHandStorage.class).setCurrentHand(slot);
                });
            }
        });
    }

    @Override
    public void init(UserConnection user) {
        user.put(new PlayerHandStorage());
    }
}
