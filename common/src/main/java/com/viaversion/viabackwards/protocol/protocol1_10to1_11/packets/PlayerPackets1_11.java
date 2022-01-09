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

package com.viaversion.viabackwards.protocol.protocol1_10to1_11.packets;

import com.viaversion.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.protocol.remapper.ValueTransformer;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import com.viaversion.viaversion.libs.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;

public class PlayerPackets1_11 {
    private static final ValueTransformer<Short, Float> TO_NEW_FLOAT = new ValueTransformer<Short, Float>(Type.FLOAT) {
        @Override
        public Float transform(PacketWrapper wrapper, Short inputValue) throws Exception {
            return inputValue / 15f;
        }
    };

    public void register(Protocol1_10To1_11 protocol) {
        protocol.registerClientbound(ClientboundPackets1_9_3.TITLE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Action

                handler(wrapper -> {
                    int action = wrapper.get(Type.VAR_INT, 0);

                    if (action == 2) { // Handle the new ActionBar
                        JsonElement message = wrapper.read(Type.COMPONENT);

                        wrapper.clearPacket();
                        wrapper.setId(ClientboundPackets1_9_3.CHAT_MESSAGE.ordinal());

                        // https://bugs.mojang.com/browse/MC-119145to
                        String legacy = LegacyComponentSerializer.legacySection().serialize(GsonComponentSerializer.gson().deserialize(message.toString()));
                        message = new JsonObject();
                        message.getAsJsonObject().addProperty("text", legacy);

                        wrapper.write(Type.COMPONENT, message);
                        wrapper.write(Type.BYTE, (byte) 2);
                    } else if (action > 2) {
                        wrapper.set(Type.VAR_INT, 0, action - 1); // Move everything one position down
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.COLLECT_ITEM, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Collected entity id
                map(Type.VAR_INT); // 1 - Collector entity id

                handler(wrapper -> wrapper.read(Type.VAR_INT)); // Ignore item pickup count
            }
        });


        protocol.registerServerbound(ServerboundPackets1_9_3.PLAYER_BLOCK_PLACEMENT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // 0 - Location
                map(Type.VAR_INT); // 1 - Face
                map(Type.VAR_INT); // 2 - Hand

                map(Type.UNSIGNED_BYTE, TO_NEW_FLOAT);
                map(Type.UNSIGNED_BYTE, TO_NEW_FLOAT);
                map(Type.UNSIGNED_BYTE, TO_NEW_FLOAT);
            }
        });
    }
}
