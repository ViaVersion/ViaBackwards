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
package com.viaversion.viabackwards.protocol.protocol1_13to1_13_1.packets;

import com.viaversion.viaversion.api.minecraft.BlockFace;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.types.Chunk1_13Type;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import com.viaversion.viaversion.rewriter.BlockRewriter;

public class WorldPackets1_13_1 {

    public static void register(Protocol protocol) {
        BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION);

        protocol.registerClientbound(ClientboundPackets1_13.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        Chunk chunk = wrapper.passthrough(new Chunk1_13Type(clientWorld));

                        for (ChunkSection section : chunk.getSections()) {
                            if (section != null) {
                                for (int i = 0; i < section.getPaletteSize(); i++) {
                                    section.setPaletteEntry(i, protocol.getMappingData().getNewBlockStateId(section.getPaletteEntry(i)));
                                }
                            }
                        }
                    }
                });
            }
        });

        blockRewriter.registerBlockAction(ClientboundPackets1_13.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_13.BLOCK_CHANGE);
        blockRewriter.registerMultiBlockChange(ClientboundPackets1_13.MULTI_BLOCK_CHANGE);
        protocol.registerClientbound(ClientboundPackets1_13.EFFECT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Effect Id
                map(Type.POSITION); // Location
                map(Type.INT); // Data
                handler(wrapper -> {
                    int id = wrapper.get(Type.INT, 0);
                    int data = wrapper.get(Type.INT, 1);
                    if (id == 1010) { // Play record
                        wrapper.set(Type.INT, 1, protocol.getMappingData().getNewItemId(data));
                    } else if (id == 2001) { // Block break + block break sound
                        wrapper.set(Type.INT, 1, protocol.getMappingData().getNewBlockStateId(data));
                    } else if (id == 2000) { // Smoke
                        switch (data) {
                            case 0: // Down
                            case 1: // Up
                                Position pos = wrapper.get(Type.POSITION, 0);
                                BlockFace relative = data == 0 ? BlockFace.BOTTOM : BlockFace.TOP;
                                wrapper.set(Type.POSITION, 0, pos.getRelative(relative)); // Y Offset
                                wrapper.set(Type.INT, 1, 4); // Self
                                break;
                            case 2: // North
                                wrapper.set(Type.INT, 1, 1); // North
                                break;
                            case 3: // South
                                wrapper.set(Type.INT, 1, 7); // South
                                break;
                            case 4: // West
                                wrapper.set(Type.INT, 1, 3); // West
                                break;
                            case 5: // East
                                wrapper.set(Type.INT, 1, 5); // East
                                break;
                        }
                    }
                });
            }
        });

    }
}
