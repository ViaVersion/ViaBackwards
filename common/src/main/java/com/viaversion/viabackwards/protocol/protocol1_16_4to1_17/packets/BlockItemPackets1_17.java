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
package com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.packets;

import com.viaversion.viabackwards.api.rewriters.ItemRewriter;
import com.viaversion.viabackwards.api.rewriters.MapColorRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.data.MapColorRewrites;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.types.Chunk1_16_2Type;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.data.RecipeRewriter1_16;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ClientboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.types.Chunk1_17Type;
import com.viaversion.viaversion.rewriter.BlockRewriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public final class BlockItemPackets1_17 extends ItemRewriter<Protocol1_16_4To1_17> {

    public BlockItemPackets1_17(Protocol1_16_4To1_17 protocol, TranslatableRewriter translatableRewriter) {
        super(protocol, translatableRewriter);
    }

    @Override
    protected void registerPackets() {
        BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION1_14);

        new RecipeRewriter1_16(protocol).registerDefaultHandler(ClientboundPackets1_17.DECLARE_RECIPES);

        registerSetCooldown(ClientboundPackets1_17.COOLDOWN);
        registerWindowItems(ClientboundPackets1_17.WINDOW_ITEMS, Type.FLAT_VAR_INT_ITEM_ARRAY);
        registerSetSlot(ClientboundPackets1_17.SET_SLOT, Type.FLAT_VAR_INT_ITEM);
        registerEntityEquipmentArray(ClientboundPackets1_17.ENTITY_EQUIPMENT, Type.FLAT_VAR_INT_ITEM);
        registerTradeList(ClientboundPackets1_17.TRADE_LIST, Type.FLAT_VAR_INT_ITEM);
        registerAdvancements(ClientboundPackets1_17.ADVANCEMENTS, Type.FLAT_VAR_INT_ITEM);

        blockRewriter.registerAcknowledgePlayerDigging(ClientboundPackets1_17.ACKNOWLEDGE_PLAYER_DIGGING);
        blockRewriter.registerBlockAction(ClientboundPackets1_17.BLOCK_ACTION);
        blockRewriter.registerEffect(ClientboundPackets1_17.EFFECT, 1010, 2001);


        registerCreativeInvAction(ServerboundPackets1_16_2.CREATIVE_INVENTORY_ACTION, Type.FLAT_VAR_INT_ITEM);
        protocol.registerServerbound(ServerboundPackets1_16_2.EDIT_BOOK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)));
            }
        });

        //TODO This will cause desync issues for players under certain circumstances, but mostly works:tm:
        protocol.registerServerbound(ServerboundPackets1_16_2.CLICK_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // Window Id
                map(Type.SHORT); // Slot
                map(Type.BYTE); // Button
                map(Type.SHORT, Type.NOTHING); // Action id - removed
                map(Type.VAR_INT); // Mode
                handler(wrapper -> {
                    // The 1.17 client would check the entire inventory for changes before -> after a click and send the changed slots here
                    wrapper.write(Type.VAR_INT, 0); // Empty array of slot+item

                    // Expected is the carried item after clicking, old clients send the clicked one (*mostly* being the same)
                    handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));
                });
            }
        });
        protocol.cancelServerbound(ServerboundPackets1_16_2.WINDOW_CONFIRMATION);

        protocol.registerClientbound(ClientboundPackets1_17.SPAWN_PARTICLE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Particle id
                map(Type.BOOLEAN); // Long distance
                map(Type.DOUBLE); // X
                map(Type.DOUBLE); // Y
                map(Type.DOUBLE); // Z
                map(Type.FLOAT); // Offset X
                map(Type.FLOAT); // Offset Y
                map(Type.FLOAT); // Offset Z
                map(Type.FLOAT); // Particle data
                map(Type.INT); // Particle count
                handler(wrapper -> {
                    int id = wrapper.get(Type.INT, 0);
                    if (id == 15) {
                        wrapper.passthrough(Type.FLOAT); // R
                        wrapper.passthrough(Type.FLOAT); // G
                        wrapper.passthrough(Type.FLOAT); // B
                        wrapper.passthrough(Type.FLOAT); // Scale

                        // Dust color transition -> Dust
                        wrapper.read(Type.FLOAT); // R
                        wrapper.read(Type.FLOAT); // G
                        wrapper.read(Type.FLOAT); // B
                    } else if (id == 36) {
                        // Vibration signal - no nice mapping possible without tracking entity positions and doing particle tasks
                        wrapper.cancel();
                    }
                });
                handler(getSpawnParticleHandler(Type.FLAT_VAR_INT_ITEM));
            }
        });

        protocol.mergePacket(ClientboundPackets1_17.WORLD_BORDER_SIZE, ClientboundPackets1_16_2.WORLD_BORDER, 0);
        protocol.mergePacket(ClientboundPackets1_17.WORLD_BORDER_LERP_SIZE, ClientboundPackets1_16_2.WORLD_BORDER, 1);
        protocol.mergePacket(ClientboundPackets1_17.WORLD_BORDER_CENTER, ClientboundPackets1_16_2.WORLD_BORDER, 2);
        protocol.mergePacket(ClientboundPackets1_17.WORLD_BORDER_INIT, ClientboundPackets1_16_2.WORLD_BORDER, 3);
        protocol.mergePacket(ClientboundPackets1_17.WORLD_BORDER_WARNING_DELAY, ClientboundPackets1_16_2.WORLD_BORDER, 4);
        protocol.mergePacket(ClientboundPackets1_17.WORLD_BORDER_WARNING_DISTANCE, ClientboundPackets1_16_2.WORLD_BORDER, 5);

        // The Great Shrunkening
        // Chunk sections *will* be lost ¯\_(ツ)_/¯
        protocol.registerClientbound(ClientboundPackets1_17.UPDATE_LIGHT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // X
                map(Type.VAR_INT); // Z
                map(Type.BOOLEAN); // Trust edges
                handler(wrapper -> {
                    EntityTracker tracker = wrapper.user().getEntityTracker(Protocol1_16_4To1_17.class);
                    int startFromSection = Math.max(0, -(tracker.currentMinY() >> 4));

                    long[] skyLightMask = wrapper.read(Type.LONG_ARRAY_PRIMITIVE);
                    long[] blockLightMask = wrapper.read(Type.LONG_ARRAY_PRIMITIVE);
                    int cutSkyLightMask = cutLightMask(skyLightMask, startFromSection);
                    int cutBlockLightMask = cutLightMask(blockLightMask, startFromSection);
                    wrapper.write(Type.VAR_INT, cutSkyLightMask);
                    wrapper.write(Type.VAR_INT, cutBlockLightMask);

                    long[] emptySkyLightMask = wrapper.read(Type.LONG_ARRAY_PRIMITIVE);
                    long[] emptyBlockLightMask = wrapper.read(Type.LONG_ARRAY_PRIMITIVE);
                    wrapper.write(Type.VAR_INT, cutLightMask(emptySkyLightMask, startFromSection));
                    wrapper.write(Type.VAR_INT, cutLightMask(emptyBlockLightMask, startFromSection));

                    writeLightArrays(wrapper, BitSet.valueOf(skyLightMask), cutSkyLightMask, startFromSection, tracker.currentWorldSectionHeight());
                    writeLightArrays(wrapper, BitSet.valueOf(blockLightMask), cutBlockLightMask, startFromSection, tracker.currentWorldSectionHeight());
                });
            }

            private void writeLightArrays(PacketWrapper wrapper, BitSet bitMask, int cutBitMask,
                                          int startFromSection, int sectionHeight) throws Exception {
                wrapper.read(Type.VAR_INT); // Length - throw it away

                List<byte[]> light = new ArrayList<>();

                // Remove lower bounds
                for (int i = 0; i < startFromSection; i++) {
                    if (bitMask.get(i)) {
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE);
                    }
                }

                // Add the important 18 sections
                for (int i = 0; i < 18; i++) {
                    if (isSet(cutBitMask, i)) {
                        light.add(wrapper.read(Type.BYTE_ARRAY_PRIMITIVE));
                    }
                }

                // Remove upper bounds
                for (int i = startFromSection + 18; i < sectionHeight + 2; i++) {
                    if (bitMask.get(i)) {
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE);
                    }
                }

                // Aaand we're done
                for (byte[] bytes : light) {
                    wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, bytes);
                }
            }

            private boolean isSet(int mask, int i) {
                return (mask & (1 << i)) != 0;
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.MULTI_BLOCK_CHANGE, new PacketRemapper() {
            public void registerMap() {
                map(Type.LONG); // Chunk pos
                map(Type.BOOLEAN); // Suppress light updates
                handler((wrapper) -> {
                    // Remove sections below y 0 and above 255
                    long chunkPos = wrapper.get(Type.LONG, 0);
                    int chunkY = (int) (chunkPos << 44 >> 44);
                    if (chunkY < 0 || chunkY > 15) {
                        wrapper.cancel();
                        return;
                    }

                    BlockChangeRecord[] records = wrapper.passthrough(Type.VAR_LONG_BLOCK_CHANGE_RECORD_ARRAY);
                    for (BlockChangeRecord record : records) {
                        record.setBlockId(protocol.getMappingData().getNewBlockStateId(record.getBlockId()));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.BLOCK_CHANGE, new PacketRemapper() {
            public void registerMap() {
                map(Type.POSITION1_14);
                map(Type.VAR_INT);
                handler((wrapper) -> {
                    int y = wrapper.get(Type.POSITION1_14, 0).getY();
                    if (y < 0 || y > 255) {
                        wrapper.cancel();
                        return;
                    }

                    wrapper.set(Type.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(wrapper.get(Type.VAR_INT, 0)));
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    EntityTracker tracker = wrapper.user().getEntityTracker(Protocol1_16_4To1_17.class);
                    int currentWorldSectionHeight = tracker.currentWorldSectionHeight();

                    Chunk chunk = wrapper.read(new Chunk1_17Type(currentWorldSectionHeight));
                    wrapper.write(new Chunk1_16_2Type(), chunk);

                    // Cut sections
                    int startFromSection = Math.max(0, -(tracker.currentMinY() >> 4));
                    chunk.setBiomeData(Arrays.copyOfRange(chunk.getBiomeData(), startFromSection * 64, (startFromSection * 64) + 1024));

                    chunk.setBitmask(cutMask(chunk.getChunkMask(), startFromSection, false));
                    chunk.setChunkMask(null);

                    ChunkSection[] sections = Arrays.copyOfRange(chunk.getSections(), startFromSection, startFromSection + 16);
                    chunk.setSections(sections);

                    for (int i = 0; i < 16; i++) {
                        ChunkSection section = sections[i];
                        if (section == null) continue;
                        for (int j = 0; j < section.getPaletteSize(); j++) {
                            int old = section.getPaletteEntry(j);
                            section.setPaletteEntry(j, protocol.getMappingData().getNewBlockStateId(old));
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.BLOCK_ENTITY_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int y = wrapper.passthrough(Type.POSITION1_14).getY();
                    if (y < 0 || y > 255) {
                        wrapper.cancel();
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.BLOCK_BREAK_ANIMATION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                handler(wrapper -> {
                    int y = wrapper.passthrough(Type.POSITION1_14).getY();
                    if (y < 0 || y > 255) {
                        wrapper.cancel();
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.MAP_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Map ID
                map(Type.BYTE); // Scale
                handler(wrapper -> wrapper.write(Type.BOOLEAN, true)); // Tracking position
                map(Type.BOOLEAN); // Locked
                handler(wrapper -> {
                    boolean hasMarkers = wrapper.read(Type.BOOLEAN);
                    if (!hasMarkers) {
                        wrapper.write(Type.VAR_INT, 0); // Array size
                    } else {
                        MapColorRewriter.getRewriteHandler(MapColorRewrites::getMappedColor).handle(wrapper);
                    }
                });
            }
        });
    }

    private int cutLightMask(long[] mask, int startFromSection) {
        if (mask.length == 0) return 0;
        return cutMask(BitSet.valueOf(mask), startFromSection, true);
    }

    private int cutMask(BitSet mask, int startFromSection, boolean lightMask) {
        int cutMask = 0;
        // Light masks have a section below and above the 16 main sections
        int to = startFromSection + (lightMask ? 18 : 16);
        for (int i = startFromSection, j = 0; i < to; i++, j++) {
            if (mask.get(i)) {
                cutMask |= (1 << j);
            }
        }
        return cutMask;
    }
}
