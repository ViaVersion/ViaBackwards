/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_9_1_2to1_9_3_4;

import com.viaversion.viabackwards.protocol.protocol1_9_1_2to1_9_3_4.chunks.BlockEntity;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_1;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.protocol1_9to1_8.ServerboundPackets1_9;

public class Protocol1_9_1_2To1_9_3_4 extends AbstractProtocol<ClientboundPackets1_9_3, ClientboundPackets1_9, ServerboundPackets1_9_3, ServerboundPackets1_9> {

    public Protocol1_9_1_2To1_9_3_4() {
        super(ClientboundPackets1_9_3.class, ClientboundPackets1_9.class, ServerboundPackets1_9_3.class, ServerboundPackets1_9.class);
    }

    @Override
    protected void registerPackets() {
        registerClientbound(ClientboundPackets1_9_3.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_8); //Position
                map(Type.UNSIGNED_BYTE); //Type
                map(Type.NAMED_COMPOUND_TAG); //NBT
                handler(wrapper -> {
                    if (wrapper.get(Type.UNSIGNED_BYTE, 0) == 9) {
                        Position position = wrapper.get(Type.POSITION1_8, 0);
                        CompoundTag tag = wrapper.get(Type.NAMED_COMPOUND_TAG, 0);

                        wrapper.clearPacket(); //Clear the packet

                        wrapper.setPacketType(ClientboundPackets1_9.UPDATE_SIGN);
                        wrapper.write(Type.POSITION1_8, position); // Position
                        for (int i = 1; i < 5; i++) {
                            // Should technically be written as COMPONENT, but left as String for simplification/to remove redundant wrapping for VR
                            Tag textTag = tag.get("Text" + i);
                            String line = textTag instanceof StringTag ? ((StringTag) textTag).getValue() : "";
                            wrapper.write(Type.STRING, line); // Sign line
                        }
                    }
                });
            }
        });

        registerClientbound(ClientboundPackets1_9_3.CHUNK_DATA, wrapper -> {
            ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

            ChunkType1_9_3 newType = new ChunkType1_9_3(clientWorld);
            ChunkType1_9_1 oldType = new ChunkType1_9_1(clientWorld); // Get the old type to not write Block Entities

            Chunk chunk = wrapper.read(newType);
            wrapper.write(oldType, chunk);
            BlockEntity.handle(chunk.getBlockEntities(), wrapper.user());
        });

        registerClientbound(ClientboundPackets1_9_3.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                handler(wrapper -> {
                    ClientWorld clientChunks = wrapper.user().get(ClientWorld.class);

                    int dimensionId = wrapper.get(Type.INT, 1);
                    clientChunks.setEnvironment(dimensionId);
                });
            }
        });

        registerClientbound(ClientboundPackets1_9_3.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // 0 - Dimension ID

                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

                    int dimensionId = wrapper.get(Type.INT, 0);
                    clientWorld.setEnvironment(dimensionId);
                });
            }
        });
    }

    @Override
    public void init(UserConnection userConnection) {
        if (!userConnection.has(ClientWorld.class)) {
            userConnection.put(new ClientWorld(userConnection));
        }
    }
}