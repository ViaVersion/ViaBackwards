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
package com.viaversion.viabackwards.protocol.v1_14_4to1_14_3;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ServerboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_14_3to1_14_4.packet.ClientboundPackets1_14_4;

public class Protocol1_14_4To1_14_3 extends BackwardsProtocol<ClientboundPackets1_14_4, ClientboundPackets1_14, ServerboundPackets1_14, ServerboundPackets1_14> {

    public Protocol1_14_4To1_14_3() {
        super(ClientboundPackets1_14_4.class, ClientboundPackets1_14.class, ServerboundPackets1_14.class, ServerboundPackets1_14.class);
    }

    @Override
    protected void registerPackets() {
        // Acknowledge Player Digging - added in pre4
        registerClientbound(ClientboundPackets1_14_4.BLOCK_BREAK_ACK, ClientboundPackets1_14.BLOCK_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14);
                map(Types.VAR_INT);
                handler(wrapper -> {
                    int status = wrapper.read(Types.VAR_INT);
                    boolean allGood = wrapper.read(Types.BOOLEAN);
                    if (allGood && status == 0) {
                        wrapper.cancel();
                    }
                });
            }
        });

        registerClientbound(ClientboundPackets1_14_4.MERCHANT_OFFERS, wrapper -> {
            wrapper.passthrough(Types.VAR_INT);
            int size = wrapper.passthrough(Types.UNSIGNED_BYTE);
            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Types.ITEM1_13_2);
                wrapper.passthrough(Types.ITEM1_13_2);
                if (wrapper.passthrough(Types.BOOLEAN)) {
                    wrapper.passthrough(Types.ITEM1_13_2);
                }
                wrapper.passthrough(Types.BOOLEAN);
                wrapper.passthrough(Types.INT);
                wrapper.passthrough(Types.INT);
                wrapper.passthrough(Types.INT);
                wrapper.passthrough(Types.INT);
                wrapper.passthrough(Types.FLOAT);
                wrapper.read(Types.INT); // demand value added in pre-5
            }
        });
    }
}
