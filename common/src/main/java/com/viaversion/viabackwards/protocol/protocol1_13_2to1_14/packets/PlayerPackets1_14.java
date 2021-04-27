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
package com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.packets;

import com.viaversion.viabackwards.api.rewriters.Rewriter;
import com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;

public class PlayerPackets1_14 extends Rewriter<Protocol1_13_2To1_14> {

    public PlayerPackets1_14(Protocol1_13_2To1_14 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerOutgoing(ClientboundPackets1_14.SERVER_DIFFICULTY, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE);
                map(Type.BOOLEAN, Type.NOTHING); // Locked
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.OPEN_SIGN_EDITOR, new PacketRemapper() { // c
            @Override
            public void registerMap() {
                map(Type.POSITION1_14, Type.POSITION);
            }
        });
        protocol.registerIncoming(ServerboundPackets1_13.QUERY_BLOCK_NBT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.POSITION, Type.POSITION1_14);
            }
        });
        protocol.registerIncoming(ServerboundPackets1_13.PLAYER_DIGGING, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.POSITION, Type.POSITION1_14);
                map(Type.BYTE);
            }
        });

        protocol.registerIncoming(ServerboundPackets1_13.RECIPE_BOOK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int type = wrapper.get(Type.VAR_INT, 0);
                        if (type == 0) {
                            wrapper.passthrough(Type.STRING);
                        } else if (type == 1) {
                            wrapper.passthrough(Type.BOOLEAN); // Crafting Recipe Book Open
                            wrapper.passthrough(Type.BOOLEAN); // Crafting Recipe Filter Active
                            wrapper.passthrough(Type.BOOLEAN); // Smelting Recipe Book Open
                            wrapper.passthrough(Type.BOOLEAN); // Smelting Recipe Filter Active

                            // Blast furnace/smoker data
                            wrapper.write(Type.BOOLEAN, false);
                            wrapper.write(Type.BOOLEAN, false);
                            wrapper.write(Type.BOOLEAN, false);
                            wrapper.write(Type.BOOLEAN, false);
                        }
                    }
                });
            }
        });

        protocol.registerIncoming(ServerboundPackets1_13.UPDATE_COMMAND_BLOCK, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION, Type.POSITION1_14);
            }
        });
        protocol.registerIncoming(ServerboundPackets1_13.UPDATE_STRUCTURE_BLOCK, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION, Type.POSITION1_14);
            }
        });
        protocol.registerIncoming(ServerboundPackets1_13.UPDATE_SIGN, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION, Type.POSITION1_14);
            }
        });

        protocol.registerIncoming(ServerboundPackets1_13.PLAYER_BLOCK_PLACEMENT, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Position position = wrapper.read(Type.POSITION);
                        int face = wrapper.read(Type.VAR_INT);
                        int hand = wrapper.read(Type.VAR_INT);
                        float x = wrapper.read(Type.FLOAT);
                        float y = wrapper.read(Type.FLOAT);
                        float z = wrapper.read(Type.FLOAT);

                        wrapper.write(Type.VAR_INT, hand);
                        wrapper.write(Type.POSITION1_14, position);
                        wrapper.write(Type.VAR_INT, face);
                        wrapper.write(Type.FLOAT, x);
                        wrapper.write(Type.FLOAT, y);
                        wrapper.write(Type.FLOAT, z);
                        wrapper.write(Type.BOOLEAN, false); // Inside block
                    }
                });
            }
        });
    }
}
