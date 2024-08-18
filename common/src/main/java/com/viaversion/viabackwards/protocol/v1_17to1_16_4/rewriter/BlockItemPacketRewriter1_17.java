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
package com.viaversion.viabackwards.protocol.v1_17to1_16_4.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.LongArrayTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.api.rewriters.MapColorRewriter;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.Protocol1_17To1_16_4;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.data.MapColorMappings1_16_4;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.storage.PingRequests;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.storage.PlayerLastCursorItem;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_16_2;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_17;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ClientboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.CompactArrayUtil;
import com.viaversion.viaversion.util.MathUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public final class BlockItemPacketRewriter1_17 extends BackwardsItemRewriter<ClientboundPackets1_17, ServerboundPackets1_16_2, Protocol1_17To1_16_4> {

    private static final int BEDROCK_BLOCK_STATE = 33;

    public BlockItemPacketRewriter1_17(Protocol1_17To1_16_4 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_SHORT_ARRAY);
    }

    @Override
    protected void registerPackets() {
        BlockRewriter<ClientboundPackets1_17> blockRewriter = BlockRewriter.for1_14(protocol);

        new RecipeRewriter<>(protocol).register(ClientboundPackets1_17.UPDATE_RECIPES);

        registerCooldown(ClientboundPackets1_17.COOLDOWN);
        registerSetContent(ClientboundPackets1_17.CONTAINER_SET_CONTENT);
        registerSetEquipment(ClientboundPackets1_17.SET_EQUIPMENT);
        registerMerchantOffers(ClientboundPackets1_17.MERCHANT_OFFERS);
        registerAdvancements(ClientboundPackets1_17.UPDATE_ADVANCEMENTS);

        blockRewriter.registerBlockBreakAck(ClientboundPackets1_17.BLOCK_BREAK_ACK);
        blockRewriter.registerBlockEvent(ClientboundPackets1_17.BLOCK_EVENT);
        blockRewriter.registerLevelEvent(ClientboundPackets1_17.LEVEL_EVENT, 1010, 2001);


        registerSetCreativeModeSlot(ServerboundPackets1_16_2.SET_CREATIVE_MODE_SLOT);
        protocol.registerServerbound(ServerboundPackets1_16_2.EDIT_BOOK, wrapper -> handleItemToServer(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)));

        // TODO Since the carried and modified items are typically set incorrectly, the server sends unnecessary
        // set slot packets after practically every window click, since it thinks the client and server
        // inventories are desynchronized. Ideally, we want to store a replica of each container state, and update
        // it appropriately for both serverbound and clientbound window packets, then fill in the carried
        // and modified items array as appropriate here. That would be a ton of work and replicated vanilla code,
        // and the hack below mitigates the worst side effects of this issue, which is an incorrect carried item
        // sent to the client when a right/left click drag is started. It works, at least for now...
        protocol.registerServerbound(ServerboundPackets1_16_2.CONTAINER_CLICK, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE);
                handler(wrapper -> {
                    short slot = wrapper.passthrough(Types.SHORT); // Slot
                    byte button = wrapper.passthrough(Types.BYTE); // Button
                    wrapper.read(Types.SHORT); // Action id - removed
                    int mode = wrapper.passthrough(Types.VAR_INT); // Mode
                    Item clicked = handleItemToServer(wrapper.user(), wrapper.read(Types.ITEM1_13_2)); // Clicked item

                    // The 1.17 client would check the entire inventory for changes before -> after a click and send the changed slots here
                    wrapper.write(Types.VAR_INT, 0); // Empty array of slot+item

                    PlayerLastCursorItem state = wrapper.user().get(PlayerLastCursorItem.class);
                    if (mode == 0 && button == 0 && clicked != null) {
                        // Left click PICKUP
                        // Carried item will (usually) be the entire clicked stack
                        state.setLastCursorItem(clicked);
                    } else if (mode == 0 && button == 1 && clicked != null) {
                        // Right click PICKUP
                        // Carried item will (usually) be half of the clicked stack (rounding up)
                        // if the clicked slot is empty, otherwise it will (usually) be the whole clicked stack
                        if (state.isSet()) {
                            state.setLastCursorItem(clicked);
                        } else {
                            state.setLastCursorItem(clicked, (clicked.amount() + 1) / 2);
                        }
                    } else if (mode == 5 && slot == -999 && (button == 0 || button == 4)) {
                        // Start drag (left or right click)
                        // Preserve guessed carried item and forward to server
                        // This mostly fixes the click and drag ghost item issue

                        // no-op
                    } else {
                        // Carried item unknown (TODO)
                        state.setLastCursorItem(null);
                    }

                    Item carried = state.getLastCursorItem();
                    if (carried == null) {
                        // Expected is the carried item after clicking, old clients send the clicked one (*mostly* being the same)
                        wrapper.write(Types.ITEM1_13_2, clicked);
                    } else {
                        wrapper.write(Types.ITEM1_13_2, carried);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.CONTAINER_SET_SLOT, wrapper -> {
            short windowId = wrapper.passthrough(Types.UNSIGNED_BYTE);
            short slot = wrapper.passthrough(Types.SHORT);

            Item carried = wrapper.read(Types.ITEM1_13_2);
            if (carried != null && windowId == -1 && slot == -1) {
                // This is related to the hack to fix click and drag ghost items above.
                // After a completed drag, we have no idea how many items remain on the cursor,
                // and vanilla logic replication would be required to calculate the value.
                // When the click drag complete packet is sent, we will send an incorrect
                // carried item, and the server will helpfully send this packet allowing us
                // to update the internal state. This is necessary for fixing multiple sequential
                // click drag actions without intermittent pickup actions.
                wrapper.user().get(PlayerLastCursorItem.class).setLastCursorItem(carried);
            }

            wrapper.write(Types.ITEM1_13_2, handleItemToClient(wrapper.user(), carried));
        });

        protocol.registerServerbound(ServerboundPackets1_16_2.CONTAINER_ACK, null, wrapper -> {
            wrapper.cancel();
            if (!ViaBackwards.getConfig().handlePingsAsInvAcknowledgements()) {
                return;
            }

            // Handle ping packet replacement
            short inventoryId = wrapper.read(Types.UNSIGNED_BYTE);
            short confirmationId = wrapper.read(Types.SHORT);
            boolean accepted = wrapper.read(Types.BOOLEAN);
            if (inventoryId == 0 && accepted && wrapper.user().get(PingRequests.class).removeId(confirmationId)) {
                PacketWrapper pongPacket = wrapper.create(ServerboundPackets1_17.PONG);
                pongPacket.write(Types.INT, (int) confirmationId);
                pongPacket.sendToServer(Protocol1_17To1_16_4.class);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.LEVEL_PARTICLES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Particle id
                map(Types.BOOLEAN); // Long distance
                map(Types.DOUBLE); // X
                map(Types.DOUBLE); // Y
                map(Types.DOUBLE); // Z
                map(Types.FLOAT); // Offset X
                map(Types.FLOAT); // Offset Y
                map(Types.FLOAT); // Offset Z
                map(Types.FLOAT); // Particle data
                map(Types.INT); // Particle count
                handler(wrapper -> {
                    int id = wrapper.get(Types.INT, 0);
                    if (id == 16) {
                        wrapper.passthrough(Types.FLOAT); // R
                        wrapper.passthrough(Types.FLOAT); // G
                        wrapper.passthrough(Types.FLOAT); // B
                        wrapper.passthrough(Types.FLOAT); // Scale

                        // Dust color transition -> Dust
                        wrapper.read(Types.FLOAT); // R
                        wrapper.read(Types.FLOAT); // G
                        wrapper.read(Types.FLOAT); // B
                    } else if (id == 37) {
                        // Vibration signal - no nice mapping possible without tracking entity positions and doing particle tasks
                        wrapper.set(Types.INT, 0, -1);
                        wrapper.cancel();
                    }
                });
                handler(levelParticlesHandler());
            }
        });

        protocol.mergePacket(ClientboundPackets1_17.SET_BORDER_SIZE, ClientboundPackets1_16_2.SET_BORDER, 0);
        protocol.mergePacket(ClientboundPackets1_17.SET_BORDER_LERP_SIZE, ClientboundPackets1_16_2.SET_BORDER, 1);
        protocol.mergePacket(ClientboundPackets1_17.SET_BORDER_CENTER, ClientboundPackets1_16_2.SET_BORDER, 2);
        protocol.mergePacket(ClientboundPackets1_17.INITIALIZE_BORDER, ClientboundPackets1_16_2.SET_BORDER, 3);
        protocol.mergePacket(ClientboundPackets1_17.SET_BORDER_WARNING_DELAY, ClientboundPackets1_16_2.SET_BORDER, 4);
        protocol.mergePacket(ClientboundPackets1_17.SET_BORDER_WARNING_DISTANCE, ClientboundPackets1_16_2.SET_BORDER, 5);

        // The Great Shrunkening
        // Chunk sections *will* be lost ¯\_(ツ)_/¯
        protocol.registerClientbound(ClientboundPackets1_17.LIGHT_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // X
                map(Types.VAR_INT); // Z
                map(Types.BOOLEAN); // Trust edges
                handler(wrapper -> {
                    EntityTracker tracker = wrapper.user().getEntityTracker(Protocol1_17To1_16_4.class);
                    int startFromSection = Math.max(0, -(tracker.currentMinY() >> 4));

                    long[] skyLightMask = wrapper.read(Types.LONG_ARRAY_PRIMITIVE);
                    long[] blockLightMask = wrapper.read(Types.LONG_ARRAY_PRIMITIVE);
                    int cutSkyLightMask = cutLightMask(skyLightMask, startFromSection);
                    int cutBlockLightMask = cutLightMask(blockLightMask, startFromSection);
                    wrapper.write(Types.VAR_INT, cutSkyLightMask);
                    wrapper.write(Types.VAR_INT, cutBlockLightMask);

                    long[] emptySkyLightMask = wrapper.read(Types.LONG_ARRAY_PRIMITIVE);
                    long[] emptyBlockLightMask = wrapper.read(Types.LONG_ARRAY_PRIMITIVE);
                    wrapper.write(Types.VAR_INT, cutLightMask(emptySkyLightMask, startFromSection));
                    wrapper.write(Types.VAR_INT, cutLightMask(emptyBlockLightMask, startFromSection));

                    writeLightArrays(wrapper, BitSet.valueOf(skyLightMask), cutSkyLightMask, startFromSection, tracker.currentWorldSectionHeight());
                    writeLightArrays(wrapper, BitSet.valueOf(blockLightMask), cutBlockLightMask, startFromSection, tracker.currentWorldSectionHeight());
                });
            }

            private void writeLightArrays(PacketWrapper wrapper, BitSet bitMask, int cutBitMask,
                                          int startFromSection, int sectionHeight) {
                int packetContentsLength = wrapper.read(Types.VAR_INT);
                int read = 0;
                List<byte[]> light = new ArrayList<>();

                // Remove lower bounds
                for (int i = 0; i < startFromSection; i++) {
                    if (bitMask.get(i)) {
                        read++;
                        wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
                    }
                }

                // Add the important 18 sections
                for (int i = 0; i < 18; i++) {
                    if (isSet(cutBitMask, i)) {
                        read++;
                        light.add(wrapper.read(Types.BYTE_ARRAY_PRIMITIVE));
                    }
                }

                // Remove upper bounds
                for (int i = startFromSection + 18; i < sectionHeight + 2; i++) {
                    if (bitMask.get(i)) {
                        read++;
                        wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
                    }
                }

                if (read != packetContentsLength) {
                    // Read the rest if the server for some reason sends more than are actually consumed
                    for (int i = read; i < packetContentsLength; i++) {
                        wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
                    }
                }

                // Aaand we're done
                for (byte[] bytes : light) {
                    wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, bytes);
                }
            }

            private boolean isSet(int mask, int i) {
                return (mask & (1 << i)) != 0;
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.SECTION_BLOCKS_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.LONG); // Chunk pos
                map(Types.BOOLEAN); // Suppress light updates
                handler((wrapper) -> {
                    // Remove sections below y 0 and above 255
                    long chunkPos = wrapper.get(Types.LONG, 0);
                    int chunkY = (int) (chunkPos << 44 >> 44);
                    if (chunkY < 0 || chunkY > 15) {
                        wrapper.cancel();
                        return;
                    }

                    BlockChangeRecord[] records = wrapper.passthrough(Types.VAR_LONG_BLOCK_CHANGE_ARRAY);
                    for (BlockChangeRecord record : records) {
                        if (ViaBackwards.getConfig().bedrockAtY0() && chunkY == 0 && record.getSectionY() == 0) {
                            record.setBlockId(BEDROCK_BLOCK_STATE);
                        } else {
                            record.setBlockId(protocol.getMappingData().getNewBlockStateId(record.getBlockId()));
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.BLOCK_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14);
                map(Types.VAR_INT);
                handler((wrapper) -> {
                    int y = wrapper.get(Types.BLOCK_POSITION1_14, 0).y();
                    if (y < 0 || y > 255) {
                        wrapper.cancel();
                        return;
                    }

                    if (ViaBackwards.getConfig().bedrockAtY0() && y == 0) {
                        wrapper.set(Types.VAR_INT, 0, BEDROCK_BLOCK_STATE);
                    } else {
                        wrapper.set(Types.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(wrapper.get(Types.VAR_INT, 0)));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.LEVEL_CHUNK, wrapper -> {
            EntityTracker tracker = wrapper.user().getEntityTracker(Protocol1_17To1_16_4.class);
            int currentWorldSectionHeight = tracker.currentWorldSectionHeight();

            Chunk chunk = wrapper.read(new ChunkType1_17(currentWorldSectionHeight));
            wrapper.write(ChunkType1_16_2.TYPE, chunk);

            // Cut sections
            int startFromSection = Math.max(0, -(tracker.currentMinY() >> 4));
            chunk.setBiomeData(Arrays.copyOfRange(chunk.getBiomeData(), startFromSection * 64, (startFromSection * 64) + 1024));

            chunk.setBitmask(cutMask(chunk.getChunkMask(), startFromSection, false));
            chunk.setChunkMask(null);

            ChunkSection[] sections = Arrays.copyOfRange(chunk.getSections(), startFromSection, startFromSection + 16);
            chunk.setSections(sections);

            CompoundTag heightMaps = chunk.getHeightMap();
            for (Tag heightMapTag : heightMaps.values()) {
                if (!(heightMapTag instanceof final LongArrayTag heightMap)) {
                    continue; // Client can handle bad data
                }

                int[] heightMapData = new int[256];
                int bitsPerEntry = MathUtil.ceilLog2((currentWorldSectionHeight << 4) + 1);
                // Shift back to 0 based and clamp to normal height with 9 bits
                CompactArrayUtil.iterateCompactArrayWithPadding(bitsPerEntry, heightMapData.length, heightMap.getValue(), (i, v) -> heightMapData[i] = MathUtil.clamp(v + tracker.currentMinY(), 0, 255));
                heightMap.setValue(CompactArrayUtil.createCompactArrayWithPadding(9, heightMapData.length, i -> heightMapData[i]));
            }

            blockRewriter.handleChunk(chunk);

            if (ViaBackwards.getConfig().bedrockAtY0()) {
                final ChunkSection lowestSection = chunk.getSections()[0];
                if (lowestSection != null) {
                    final DataPalette blocks = lowestSection.palette(PaletteType.BLOCKS);
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            blocks.setIdAt(x, 0, z, BEDROCK_BLOCK_STATE);
                        }
                    }
                }
            }

            chunk.getBlockEntities().removeIf(compound -> {
                NumberTag tag = compound.getNumberTag("y");
                if (tag == null) {
                    return false;
                }

                final int y = tag.asInt();
                return y < 0 || y > 255 || (ViaBackwards.getConfig().bedrockAtY0() && y == 0);
            });
        });

        protocol.registerClientbound(ClientboundPackets1_17.BLOCK_ENTITY_DATA, wrapper -> {
            int y = wrapper.passthrough(Types.BLOCK_POSITION1_14).y();
            if (y < 0 || y > 255 || (ViaBackwards.getConfig().bedrockAtY0() && y == 0)) {
                wrapper.cancel();
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.BLOCK_DESTRUCTION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                handler(wrapper -> {
                    int y = wrapper.passthrough(Types.BLOCK_POSITION1_14).y();
                    if (y < 0 || y > 255 || (ViaBackwards.getConfig().bedrockAtY0() && y == 0)) {
                        wrapper.cancel();
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.MAP_ITEM_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Map ID
                map(Types.BYTE); // Scale
                handler(wrapper -> wrapper.write(Types.BOOLEAN, true)); // Tracking position
                map(Types.BOOLEAN); // Locked
                handler(wrapper -> {
                    boolean hasMarkers = wrapper.read(Types.BOOLEAN);
                    if (!hasMarkers) {
                        wrapper.write(Types.VAR_INT, 0); // Array size
                    } else {
                        MapColorRewriter.getRewriteHandler(MapColorMappings1_16_4::getMappedColor).handle(wrapper);
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
