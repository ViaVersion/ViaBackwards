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
package com.viaversion.viabackwards.protocol.protocol1_9_1_2to1_9_3_4.chunks;

import com.viaversion.viabackwards.protocol.protocol1_9_1_2to1_9_3_4.Protocol1_9_1_2To1_9_3_4;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockEntity {
    private static final Map<String, Integer> TYPES = new HashMap<>();

    static {
        TYPES.put("MobSpawner", 1);
        TYPES.put("Control", 2);
        TYPES.put("Beacon", 3);
        TYPES.put("Skull", 4);
        TYPES.put("FlowerPot", 5);
        TYPES.put("Banner", 6);
        TYPES.put("UNKNOWN", 7);
        TYPES.put("EndGateway", 8);
        TYPES.put("Sign", 9);
    }

    public static void handle(List<CompoundTag> tags, UserConnection connection) throws Exception {
        for (CompoundTag tag : tags) {
            StringTag idTag = tag.getStringTag("id");
            if (idTag == null) {
                continue;
            }

            String id = idTag.getValue();
            if (!TYPES.containsKey(id)) {
                continue;
            }

            int newId = TYPES.get(id);
            if (newId == -1) {
                continue;
            }

            int x = tag.getNumberTag("x").asInt();
            int y = tag.getNumberTag("y").asInt();
            int z = tag.getNumberTag("z").asInt();

            Position pos = new Position(x, (short) y, z);

            updateBlockEntity(pos, (short) newId, tag, connection);
        }
    }

    private static void updateBlockEntity(Position pos, short id, CompoundTag tag, UserConnection connection) throws Exception {
        PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_9_3.BLOCK_ENTITY_DATA, null, connection);
        wrapper.write(Type.POSITION1_8, pos);
        wrapper.write(Type.UNSIGNED_BYTE, id);
        wrapper.write(Type.NAMED_COMPOUND_TAG, tag);
        wrapper.scheduleSend(Protocol1_9_1_2To1_9_3_4.class, false);
    }
}