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
package com.viaversion.viabackwards.protocol.protocol1_14_4to1_15.packets;

import com.viaversion.viabackwards.protocol.protocol1_14_4to1_15.Protocol1_14_4To1_15;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_14;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_15;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.ServerboundPackets1_14;
import com.viaversion.viaversion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;

public class BlockItemPackets1_15 extends com.viaversion.viabackwards.api.rewriters.ItemRewriter<ClientboundPackets1_15, ServerboundPackets1_14, Protocol1_14_4To1_15> {

    public BlockItemPackets1_15(Protocol1_14_4To1_15 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        BlockRewriter<ClientboundPackets1_15> blockRewriter = BlockRewriter.for1_14(protocol);

        new RecipeRewriter<>(protocol).register(ClientboundPackets1_15.DECLARE_RECIPES);

        protocol.registerServerbound(ServerboundPackets1_14.EDIT_BOOK, wrapper -> handleItemToServer(wrapper.passthrough(Type.ITEM1_13_2)));

        registerSetCooldown(ClientboundPackets1_15.COOLDOWN);
        registerWindowItems(ClientboundPackets1_15.WINDOW_ITEMS, Type.ITEM1_13_2_SHORT_ARRAY);
        registerSetSlot(ClientboundPackets1_15.SET_SLOT, Type.ITEM1_13_2);
        registerTradeList(ClientboundPackets1_15.TRADE_LIST);
        registerEntityEquipment(ClientboundPackets1_15.ENTITY_EQUIPMENT, Type.ITEM1_13_2);
        registerAdvancements(ClientboundPackets1_15.ADVANCEMENTS, Type.ITEM1_13_2);
        registerClickWindow(ServerboundPackets1_14.CLICK_WINDOW, Type.ITEM1_13_2);
        registerCreativeInvAction(ServerboundPackets1_14.CREATIVE_INVENTORY_ACTION, Type.ITEM1_13_2);

        blockRewriter.registerAcknowledgePlayerDigging(ClientboundPackets1_15.ACKNOWLEDGE_PLAYER_DIGGING);
        blockRewriter.registerBlockAction(ClientboundPackets1_15.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_15.BLOCK_CHANGE);
        blockRewriter.registerMultiBlockChange(ClientboundPackets1_15.MULTI_BLOCK_CHANGE);

        protocol.registerClientbound(ClientboundPackets1_15.CHUNK_DATA, wrapper -> {
            Chunk chunk = wrapper.read(new ChunkType1_15());
            wrapper.write(new ChunkType1_14(), chunk);

            if (chunk.isFullChunk()) {
                int[] biomeData = chunk.getBiomeData();
                int[] newBiomeData = new int[256];
                for (int i = 0; i < 4; ++i) {
                    for (int j = 0; j < 4; ++j) {
                        int x = j << 2;
                        int z = i << 2;
                        int newIndex = z << 4 | x;
                        int oldIndex = i << 2 | j;

                        int biome = biomeData[oldIndex];
                        for (int k = 0; k < 4; k++) {
                            int offX = newIndex + (k << 4);
                            for (int l = 0; l < 4; l++) {
                                newBiomeData[offX + l] = biome;
                            }
                        }
                    }
                }

                chunk.setBiomeData(newBiomeData);
            }

            for (int i = 0; i < chunk.getSections().length; i++) {
                ChunkSection section = chunk.getSections()[i];
                if (section == null) {
                    continue;
                }

                DataPalette palette = section.palette(PaletteType.BLOCKS);
                for (int j = 0; j < palette.size(); j++) {
                    int mappedBlockStateId = protocol.getMappingData().getNewBlockStateId(palette.idByIndex(j));
                    palette.setIdByIndex(j, mappedBlockStateId);
                }
            }
        });

        blockRewriter.registerEffect(ClientboundPackets1_15.EFFECT, 1010, 2001);

        protocol.registerClientbound(ClientboundPackets1_15.SPAWN_PARTICLE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // 0 - Particle ID
                map(Type.BOOLEAN); // 1 - Long Distance
                map(Type.DOUBLE, Type.FLOAT); // 2 - X
                map(Type.DOUBLE, Type.FLOAT); // 3 - Y
                map(Type.DOUBLE, Type.FLOAT); // 4 - Z
                map(Type.FLOAT); // 5 - Offset X
                map(Type.FLOAT); // 6 - Offset Y
                map(Type.FLOAT); // 7 - Offset Z
                map(Type.FLOAT); // 8 - Particle Data
                map(Type.INT); // 9 - Particle Count
                handler(wrapper -> {
                    int id = wrapper.get(Type.INT, 0);
                    if (id == 3 || id == 23) {
                        int data = wrapper.passthrough(Type.VAR_INT);
                        wrapper.set(Type.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(data));
                    } else if (id == 32) {
                        Item item = handleItemToClient(wrapper.read(Type.ITEM1_13_2));
                        wrapper.write(Type.ITEM1_13_2, item);
                    }

                    int mappedId = protocol.getMappingData().getNewParticleId(id);
                    if (id != mappedId) {
                        wrapper.set(Type.INT, 0, mappedId);
                    }
                });
            }
        });
    }
}
