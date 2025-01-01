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
package com.viaversion.viabackwards.protocol.v1_14to1_13_2.rewriter;

import com.viaversion.viabackwards.protocol.v1_14to1_13_2.Protocol1_14To1_13_2;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.DifficultyStorage;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.rewriter.RewriterBase;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;

public class PlayerPacketRewriter1_14 extends RewriterBase<Protocol1_14To1_13_2> {

    public PlayerPacketRewriter1_14(Protocol1_14To1_13_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_14.CHANGE_DIFFICULTY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE);
                read(Types.BOOLEAN); // Locked
                handler(wrapper -> {
                    byte difficulty = wrapper.get(Types.UNSIGNED_BYTE, 0).byteValue();
                    wrapper.user().get(DifficultyStorage.class).setDifficulty(difficulty);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.OPEN_SIGN_EDITOR, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14, Types.BLOCK_POSITION1_8);
            }
        });
        protocol.registerServerbound(ServerboundPackets1_13.BLOCK_ENTITY_TAG_QUERY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.BLOCK_POSITION1_8, Types.BLOCK_POSITION1_14);
            }
        });
        protocol.registerServerbound(ServerboundPackets1_13.PLAYER_ACTION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Action
                map(Types.BLOCK_POSITION1_8, Types.BLOCK_POSITION1_14); // Position
            }
        });

        protocol.registerServerbound(ServerboundPackets1_13.RECIPE_BOOK_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                handler(wrapper -> {
                    int type = wrapper.get(Types.VAR_INT, 0);
                    if (type == 0) {
                        wrapper.passthrough(Types.STRING);
                    } else if (type == 1) {
                        wrapper.passthrough(Types.BOOLEAN); // Crafting Recipe Book Open
                        wrapper.passthrough(Types.BOOLEAN); // Crafting Recipe Filter Active
                        wrapper.passthrough(Types.BOOLEAN); // Smelting Recipe Book Open
                        wrapper.passthrough(Types.BOOLEAN); // Smelting Recipe Filter Active

                        // Blast furnace/smoker data
                        wrapper.write(Types.BOOLEAN, false);
                        wrapper.write(Types.BOOLEAN, false);
                        wrapper.write(Types.BOOLEAN, false);
                        wrapper.write(Types.BOOLEAN, false);
                    }
                });
            }
        });

        protocol.registerServerbound(ServerboundPackets1_13.SET_COMMAND_BLOCK, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_8, Types.BLOCK_POSITION1_14);
            }
        });
        protocol.registerServerbound(ServerboundPackets1_13.SET_STRUCTURE_BLOCK, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_8, Types.BLOCK_POSITION1_14);
            }
        });
        protocol.registerServerbound(ServerboundPackets1_13.SIGN_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_8, Types.BLOCK_POSITION1_14);
            }
        });

        protocol.registerServerbound(ServerboundPackets1_13.USE_ITEM_ON, wrapper -> {
            BlockPosition position = wrapper.read(Types.BLOCK_POSITION1_8);
            int face = wrapper.read(Types.VAR_INT);
            int hand = wrapper.read(Types.VAR_INT);
            float x = wrapper.read(Types.FLOAT);
            float y = wrapper.read(Types.FLOAT);
            float z = wrapper.read(Types.FLOAT);

            wrapper.write(Types.VAR_INT, hand);
            wrapper.write(Types.BLOCK_POSITION1_14, position);
            wrapper.write(Types.VAR_INT, face);
            wrapper.write(Types.FLOAT, x);
            wrapper.write(Types.FLOAT, y);
            wrapper.write(Types.FLOAT, z);
            wrapper.write(Types.BOOLEAN, false); // Inside block
        });
    }
}
