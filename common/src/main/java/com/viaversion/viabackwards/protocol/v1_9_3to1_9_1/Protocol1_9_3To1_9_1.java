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
package com.viaversion.viabackwards.protocol.v1_9_3to1_9_1;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_9_3to1_9_1.data.BlockEntity1_9_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_1;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ClientboundPackets1_9;
import com.viaversion.viaversion.protocols.v1_8to1_9.packet.ServerboundPackets1_9;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;

public class Protocol1_9_3To1_9_1 extends BackwardsProtocol<ClientboundPackets1_9_3, ClientboundPackets1_9, ServerboundPackets1_9_3, ServerboundPackets1_9> {

    public Protocol1_9_3To1_9_1() {
        super(ClientboundPackets1_9_3.class, ClientboundPackets1_9.class, ServerboundPackets1_9_3.class, ServerboundPackets1_9.class);
    }

    @Override
    protected void registerPackets() {
        registerClientbound(ClientboundPackets1_9_3.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_8); //Position
                map(Types.UNSIGNED_BYTE); //Type
                map(Types.NAMED_COMPOUND_TAG); //NBT
                handler(wrapper -> {
                    if (wrapper.get(Types.UNSIGNED_BYTE, 0) == 9) {
                        BlockPosition position = wrapper.get(Types.BLOCK_POSITION1_8, 0);
                        CompoundTag tag = wrapper.get(Types.NAMED_COMPOUND_TAG, 0);

                        wrapper.clearPacket(); //Clear the packet

                        wrapper.setPacketType(ClientboundPackets1_9.UPDATE_SIGN);
                        wrapper.write(Types.BLOCK_POSITION1_8, position); // Position
                        for (int i = 0; i < 4; i++) {
                            // Should technically be written as COMPONENT, but left as String for simplification/to remove redundant wrapping for VR
                            StringTag textTag = tag.getStringTag("Text" + (i + 1));
                            String line = textTag != null ? textTag.getValue() : "";
                            wrapper.write(Types.STRING, line); // Sign line
                        }
                    }
                });
            }
        });

        registerClientbound(ClientboundPackets1_9_3.LEVEL_CHUNK, wrapper -> {
            ClientWorld clientWorld = wrapper.user().getClientWorld(Protocol1_9_3To1_9_1.class);

            ChunkType1_9_3 newType = ChunkType1_9_3.forEnvironment(clientWorld.getEnvironment());
            ChunkType1_9_1 oldType = ChunkType1_9_1.forEnvironment(clientWorld.getEnvironment()); // Get the old type to not write Block Entities

            Chunk chunk = wrapper.read(newType);
            wrapper.write(oldType, chunk);
            BlockEntity1_9_1.handle(chunk.getBlockEntities(), wrapper.user());
        });

        registerClientbound(ClientboundPackets1_9_3.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Entity ID
                map(Types.UNSIGNED_BYTE); // 1 - Gamemode
                map(Types.INT); // 2 - Dimension

                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().getClientWorld(Protocol1_9_3To1_9_1.class);

                    int dimensionId = wrapper.get(Types.INT, 1);
                    clientWorld.setEnvironment(dimensionId);
                });
            }
        });

        registerClientbound(ClientboundPackets1_9_3.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Dimension ID

                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().getClientWorld(Protocol1_9_3To1_9_1.class);

                    int dimensionId = wrapper.get(Types.INT, 0);
                    clientWorld.setEnvironment(dimensionId);
                });
            }
        });

        JsonNBTComponentRewriter<ClientboundPackets1_9_3> componentRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);
        componentRewriter.registerComponentPacket(ClientboundPackets1_9_3.CHAT);
    }

    @Override
    public void init(UserConnection userConnection) {
        userConnection.addClientWorld(this.getClass(), new ClientWorld());
    }
}
