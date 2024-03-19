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
package com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.packets;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.api.rewriters.MapColorRewriter;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.data.MapColorRewrites;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.storage.PingRequests;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.storage.PlayerLastCursorItem;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_16_2;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_17;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.LongArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ClientboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.CompactArrayUtil;
import com.viaversion.viaversion.util.MathUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public final class BlockItemPackets1_17 extends BackwardsItemRewriter<ClientboundPackets1_17, ServerboundPackets1_16_2, Protocol1_16_4To1_17> {

    public BlockItemPackets1_17(Protocol1_16_4To1_17 protocol) {
        super(protocol, Type.ITEM1_13_2, Type.ITEM1_13_2_SHORT_ARRAY);
    }

    @Override
    protected void registerPackets() {
        BlockRewriter<ClientboundPackets1_17> blockRewriter = BlockRewriter.for1_14(protocol);

        new RecipeRewriter<>(protocol).register(ClientboundPackets1_17.DECLARE_RECIPES);

        registerSetCooldown(ClientboundPackets1_17.COOLDOWN);
        registerWindowItems(ClientboundPackets1_17.WINDOW_ITEMS);
        registerEntityEquipmentArray(ClientboundPackets1_17.ENTITY_EQUIPMENT);
        registerTradeList(ClientboundPackets1_17.TRADE_LIST);
        registerAdvancements(ClientboundPackets1_17.ADVANCEMENTS);

        blockRewriter.registerAcknowledgePlayerDigging(ClientboundPackets1_17.ACKNOWLEDGE_PLAYER_DIGGING);
        blockRewriter.registerBlockAction(ClientboundPackets1_17.BLOCK_ACTION);
        blockRewriter.registerEffect(ClientboundPackets1_17.EFFECT, 1010, 2001);


        registerCreativeInvAction(ServerboundPackets1_16_2.CREATIVE_INVENTORY_ACTION);
        protocol.registerServerbound(ServerboundPackets1_16_2.EDIT_BOOK, wrapper -> handleItemToServer(wrapper.passthrough(Type.ITEM1_13_2)));

        // TODO Since the carried and modified items are typically set incorrectly, the server sends unnecessary
        // set slot packets after practically every window click, since it thinks the client and server
        // inventories are desynchronized. Ideally, we want to store a replica of each container state, and update
        // it appropriately for both serverbound and clientbound window packets, then fill in the carried
        // and modified items array as appropriate here. That would be a ton of work and replicated vanilla code,
        // and the hack below mitigates the worst side effects of this issue, which is an incorrect carried item
        // sent to the client when a right/left click drag is started. It works, at least for now...
        protocol.registerServerbound(ServerboundPackets1_16_2.CLICK_WINDOW, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE);
                handler(wrapper -> {
                    short slot = wrapper.passthrough(Type.SHORT); // Slot
                    byte button = wrapper.passthrough(Type.BYTE); // Button
                    wrapper.read(Type.SHORT); // Action id - removed
                    int mode = wrapper.passthrough(Type.VAR_INT); // Mode
                    Item clicked = handleItemToServer(wrapper.read(Type.ITEM1_13_2)); // Clicked item

                    // The 1.17 client would check the entire inventory for changes before -> after a click and send the changed slots here
                    wrapper.write(Type.VAR_INT, 0); // Empty array of slot+item

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
                        wrapper.write(Type.ITEM1_13_2, clicked);
                    } else {
                        wrapper.write(Type.ITEM1_13_2, carried);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.SET_SLOT, wrapper -> {
            short windowId = wrapper.passthrough(Type.UNSIGNED_BYTE);
            short slot = wrapper.passthrough(Type.SHORT);

            Item carried = wrapper.read(Type.ITEM1_13_2);
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

            wrapper.write(Type.ITEM1_13_2, handleItemToClient(carried));
        });

        protocol.registerServerbound(ServerboundPackets1_16_2.WINDOW_CONFIRMATION, null, wrapper -> {
            wrapper.cancel();
            if (!ViaBackwards.getConfig().handlePingsAsInvAcknowledgements()) {
                return;
            }

            // Handle ping packet replacement
            short inventoryId = wrapper.read(Type.UNSIGNED_BYTE);
            short confirmationId = wrapper.read(Type.SHORT);
            boolean accepted = wrapper.read(Type.BOOLEAN);
            if (inventoryId == 0 && accepted && wrapper.user().get(PingRequests.class).removeId(confirmationId)) {
                PacketWrapper pongPacket = wrapper.create(ServerboundPackets1_17.PONG);
                pongPacket.write(Type.INT, (int) confirmationId);
                pongPacket.sendToServer(Protocol1_16_4To1_17.class);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.SPAWN_PARTICLE, new PacketHandlers() {
            @Override
            public void register() {
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
                    if (id == 16) {
                        wrapper.passthrough(Type.FLOAT); // R
                        wrapper.passthrough(Type.FLOAT); // G
                        wrapper.passthrough(Type.FLOAT); // B
                        wrapper.passthrough(Type.FLOAT); // Scale

                        // Dust color transition -> Dust
                        wrapper.read(Type.FLOAT); // R
                        wrapper.read(Type.FLOAT); // G
                        wrapper.read(Type.FLOAT); // B
                    } else if (id == 37) {
                        // Vibration signal - no nice mapping possible without tracking entity positions and doing particle tasks
                        wrapper.set(Type.INT, 0, -1);
                        wrapper.cancel();
                    }
                });
                handler(getSpawnParticleHandler());
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
        protocol.registerClientbound(ClientboundPackets1_17.UPDATE_LIGHT, new PacketHandlers() {
            @Override
            public void register() {
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

        protocol.registerClientbound(ClientboundPackets1_17.MULTI_BLOCK_CHANGE, new PacketHandlers() {
            @Override
            public void register() {
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

        protocol.registerClientbound(ClientboundPackets1_17.BLOCK_CHANGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_14);
                map(Type.VAR_INT);
                handler((wrapper) -> {
                    int y = wrapper.get(Type.POSITION1_14, 0).y();
                    if (y < 0 || y > 255) {
                        wrapper.cancel();
                        return;
                    }

                    wrapper.set(Type.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(wrapper.get(Type.VAR_INT, 0)));
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.CHUNK_DATA, wrapper -> {
            EntityTracker tracker = wrapper.user().getEntityTracker(Protocol1_16_4To1_17.class);
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
                LongArrayTag heightMap = (LongArrayTag) heightMapTag;
                int[] heightMapData = new int[256];
                int bitsPerEntry = MathUtil.ceilLog2((currentWorldSectionHeight << 4) + 1);
                // Shift back to 0 based and clamp to normal height with 9 bits
                CompactArrayUtil.iterateCompactArrayWithPadding(bitsPerEntry, heightMapData.length, heightMap.getValue(), (i, v) -> heightMapData[i] = MathUtil.clamp(v + tracker.currentMinY(), 0, 255));
                heightMap.setValue(CompactArrayUtil.createCompactArrayWithPadding(9, heightMapData.length, i -> heightMapData[i]));
            }

            for (int i = 0; i < 16; i++) {
                ChunkSection section = sections[i];
                if (section == null) {
                    continue;
                }

                DataPalette palette = section.palette(PaletteType.BLOCKS);
                for (int j = 0; j < palette.size(); j++) {
                    int mappedBlockStateId = protocol.getMappingData().getNewBlockStateId(palette.idByIndex(j));
                    palette.setIdByIndex(j, mappedBlockStateId);
                }
            }

            chunk.getBlockEntities().removeIf(compound -> {
                NumberTag tag = compound.getNumberTag("y");
                return tag != null && (tag.asInt() < 0 || tag.asInt() > 255);
            });
        });

        protocol.registerClientbound(ClientboundPackets1_17.BLOCK_ENTITY_DATA, wrapper -> {
            int y = wrapper.passthrough(Type.POSITION1_14).y();
            if (y < 0 || y > 255) {
                wrapper.cancel();
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.BLOCK_BREAK_ANIMATION, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                handler(wrapper -> {
                    int y = wrapper.passthrough(Type.POSITION1_14).y();
                    if (y < 0 || y > 255) {
                        wrapper.cancel();
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.MAP_DATA, new PacketHandlers() {
            @Override
            public void register() {
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
