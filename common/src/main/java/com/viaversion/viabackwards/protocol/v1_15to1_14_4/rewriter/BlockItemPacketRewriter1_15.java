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
package com.viaversion.viabackwards.protocol.v1_15to1_14_4.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.v1_15to1_14_4.Protocol1_15To1_14_4;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_14;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_15;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ServerboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.packet.ClientboundPackets1_15;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;

public class BlockItemPacketRewriter1_15 extends BackwardsItemRewriter<ClientboundPackets1_15, ServerboundPackets1_14, Protocol1_15To1_14_4> {

    public BlockItemPacketRewriter1_15(Protocol1_15To1_14_4 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_SHORT_ARRAY);
    }

    @Override
    protected void registerPackets() {
        BlockRewriter<ClientboundPackets1_15> blockRewriter = BlockRewriter.for1_14(protocol);

        new RecipeRewriter<>(protocol).register(ClientboundPackets1_15.UPDATE_RECIPES);

        protocol.registerServerbound(ServerboundPackets1_14.EDIT_BOOK, wrapper -> handleItemToServer(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)));

        registerCooldown(ClientboundPackets1_15.COOLDOWN);
        registerSetContent(ClientboundPackets1_15.CONTAINER_SET_CONTENT);
        registerSetSlot(ClientboundPackets1_15.CONTAINER_SET_SLOT);
        registerMerchantOffers(ClientboundPackets1_15.MERCHANT_OFFERS);
        registerSetEquippedItem(ClientboundPackets1_15.SET_EQUIPPED_ITEM);
        registerAdvancements(ClientboundPackets1_15.UPDATE_ADVANCEMENTS);
        registerContainerClick(ServerboundPackets1_14.CONTAINER_CLICK);
        registerSetCreativeModeSlot(ServerboundPackets1_14.SET_CREATIVE_MODE_SLOT);

        blockRewriter.registerBlockBreakAck(ClientboundPackets1_15.BLOCK_BREAK_ACK);
        blockRewriter.registerBlockEvent(ClientboundPackets1_15.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_15.BLOCK_UPDATE);
        blockRewriter.registerChunkBlocksUpdate(ClientboundPackets1_15.CHUNK_BLOCKS_UPDATE);

        protocol.registerClientbound(ClientboundPackets1_15.LEVEL_CHUNK, wrapper -> {
            Chunk chunk = wrapper.read(ChunkType1_15.TYPE);
            wrapper.write(ChunkType1_14.TYPE, chunk);

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

        blockRewriter.registerLevelEvent(ClientboundPackets1_15.LEVEL_EVENT, 1010, 2001);

        protocol.registerClientbound(ClientboundPackets1_15.LEVEL_PARTICLES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Particle ID
                map(Types.BOOLEAN); // 1 - Long Distance
                map(Types.DOUBLE, Types.FLOAT); // 2 - X
                map(Types.DOUBLE, Types.FLOAT); // 3 - Y
                map(Types.DOUBLE, Types.FLOAT); // 4 - Z
                map(Types.FLOAT); // 5 - Offset X
                map(Types.FLOAT); // 6 - Offset Y
                map(Types.FLOAT); // 7 - Offset Z
                map(Types.FLOAT); // 8 - Particle Data
                map(Types.INT); // 9 - Particle Count
                handler(wrapper -> {
                    int id = wrapper.get(Types.INT, 0);
                    if (id == 3 || id == 23) {
                        int data = wrapper.passthrough(Types.VAR_INT);
                        wrapper.set(Types.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(data));
                    } else if (id == 32) {
                        Item item = handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_13_2));
                        wrapper.write(Types.ITEM1_13_2, item);
                    }

                    int mappedId = protocol.getMappingData().getNewParticleId(id);
                    if (id != mappedId) {
                        wrapper.set(Types.INT, 0, mappedId);
                    }
                });
            }
        });
    }
}
