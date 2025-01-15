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
package com.viaversion.viabackwards.protocol.v1_9_1to1_9;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_9;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;

public class Protocol1_9_1To1_9 extends BackwardsProtocol<ClientboundPackets1_9, ClientboundPackets1_9, ServerboundPackets1_9, ServerboundPackets1_9> {

    public Protocol1_9_1To1_9() {
        super(ClientboundPackets1_9.class, ClientboundPackets1_9.class, ServerboundPackets1_9.class, ServerboundPackets1_9.class);
    }

    @Override
    protected void registerPackets() {
        registerClientbound(ClientboundPackets1_9.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Player ID
                map(Types.UNSIGNED_BYTE); // 1 - Player Gamemode
                // 1.9.1 PRE 2 Changed this
                map(Types.INT, Types.BYTE); // 2 - Player Dimension
                map(Types.UNSIGNED_BYTE); // 3 - World Difficulty
                map(Types.UNSIGNED_BYTE); // 4 - Max Players (Tab)
                map(Types.STRING); // 5 - Level Type
                map(Types.BOOLEAN); // 6 - Reduced Debug info
            }
        });

        registerClientbound(ClientboundPackets1_9.SOUND, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Sound ID

                handler(wrapper -> {
                    int sound = wrapper.get(Types.VAR_INT, 0);

                    if (sound == 415) // Stop the Elytra sound for 1.9 (It's introduced in 1.9.2)
                        wrapper.cancel();
                    else if (sound >= 416) // Act like the Elytra sound never existed
                        wrapper.set(Types.VAR_INT, 0, sound - 1);
                });
            }
        });

        JsonNBTComponentRewriter<ClientboundPackets1_9> componentRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);
        componentRewriter.registerComponentPacket(ClientboundPackets1_9.CHAT);
    }
}
