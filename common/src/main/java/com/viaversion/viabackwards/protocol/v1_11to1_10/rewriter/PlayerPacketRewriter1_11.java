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

package com.viaversion.viabackwards.protocol.v1_11to1_10.rewriter;

import com.viaversion.viabackwards.protocol.v1_11to1_10.Protocol1_11To1_10;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.remapper.ValueTransformer;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import com.viaversion.viaversion.util.ComponentUtil;

public class PlayerPacketRewriter1_11 {
    private static final ValueTransformer<Short, Float> TO_NEW_FLOAT = new ValueTransformer<>(Types.FLOAT) {
        @Override
        public Float transform(PacketWrapper wrapper, Short inputValue) {
            return inputValue / 16f;
        }
    };

    public static void register(Protocol1_11To1_10 protocol) {
        protocol.registerClientbound(ClientboundPackets1_9_3.SET_TITLES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Action

                handler(wrapper -> {
                    int action = wrapper.get(Types.VAR_INT, 0);

                    if (action == 2) { // Handle the new ActionBar
                        JsonElement message = wrapper.read(Types.COMPONENT);

                        wrapper.clearPacket();
                        wrapper.setPacketType(ClientboundPackets1_9_3.CHAT);

                        // https://bugs.mojang.com/browse/MC-119145
                        String legacy = ComponentUtil.jsonToLegacy(message);
                        message = new JsonObject();
                        message.getAsJsonObject().addProperty("text", legacy);

                        wrapper.write(Types.COMPONENT, message);
                        wrapper.write(Types.BYTE, (byte) 2);
                    } else if (action > 2) {
                        wrapper.set(Types.VAR_INT, 0, action - 1); // Move everything one position down
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.TAKE_ITEM_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Collected entity id
                map(Types.VAR_INT); // 1 - Collector entity id

                handler(wrapper -> wrapper.read(Types.VAR_INT)); // Ignore item pickup count
            }
        });


        protocol.registerServerbound(ServerboundPackets1_9_3.USE_ITEM_ON, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_8); // 0 - Location
                map(Types.VAR_INT); // 1 - Face
                map(Types.VAR_INT); // 2 - Hand

                map(Types.UNSIGNED_BYTE, TO_NEW_FLOAT);
                map(Types.UNSIGNED_BYTE, TO_NEW_FLOAT);
                map(Types.UNSIGNED_BYTE, TO_NEW_FLOAT);
            }
        });
    }
}
