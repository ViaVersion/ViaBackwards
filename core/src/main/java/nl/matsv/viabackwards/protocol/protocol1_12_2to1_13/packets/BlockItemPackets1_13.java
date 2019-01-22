/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.minecraft.BlockChangeRecord;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.packets.InventoryPackets;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.types.Chunk1_13Type;
import us.myles.ViaVersion.protocols.protocol1_9_1_2to1_9_3_4.types.Chunk1_9_3_4Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;

public class BlockItemPackets1_13 extends Rewriter<Protocol1_12_2To1_13> {
    public static int toOldId(int oldId) {
        if (oldId < 0) {
            oldId = 0; // Some plugins use negative numbers to clear blocks, remap them to air.
        }
        int newId = BackwardsMappings.blockMappings.getNewBlock(oldId);
        if (newId != -1)
            return newId;

        ViaBackwards.getPlatform().getLogger().warning("Missing block completely " + oldId);
        // Default stone
        return 1 << 4;
    }

    //Basic translation for now. TODO remap new items; should probably use BlockItemRewriter#handleItemToClient/Server, but that needs some rewriting
    public static void toClient(Item item) {
        InventoryPackets.toServer(item);
    }

    public static void toServer(Item item) {
        InventoryPackets.toClient(item);
    }

    @Override
    protected void registerPackets(Protocol1_12_2To1_13 protocol) {

        // Block Action
        protocol.out(State.PLAY, 0x0A, 0x0A, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // Location
                map(Type.UNSIGNED_BYTE); // Action Id
                map(Type.UNSIGNED_BYTE); // Action param
                map(Type.VAR_INT); // Block Id - /!\ NOT BLOCK STATE ID
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int blockId = wrapper.get(Type.VAR_INT, 0);

                        if (blockId == 73)
                            blockId = 25;
                        else if (blockId == 99)
                            blockId = 33;
                        else if (blockId == 92)
                            blockId = 29;
                        else if (blockId == 142)
                            blockId = 54;
                        else if (blockId == 305)
                            blockId = 146;
                        else if (blockId == 249)
                            blockId = 130;
                        else if (blockId == 257)
                            blockId = 138;
                        else if (blockId == 140)
                            blockId = 52;
                        else if (blockId == 472)
                            blockId = 209;
                        else if (blockId >= 483 && blockId <= 498)
                            blockId = blockId - 483 + 219;

                        wrapper.set(Type.VAR_INT, 0, blockId);
                    }
                });
            }
        });

        // Update Block Entity
        protocol.out(State.PLAY, 0x09, 0x09, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // 0 - Position
                map(Type.UNSIGNED_BYTE); // 1 - Action
                map(Type.NBT); // 2 - NBT Data

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        BackwardsBlockEntityProvider provider = Via.getManager().getProviders().get(BackwardsBlockEntityProvider.class);

                        // TODO conduit handling
                        if (wrapper.get(Type.UNSIGNED_BYTE, 0) == 5) {
                            wrapper.cancel();
                        }

                        wrapper.set(Type.NBT, 0,
                                provider.transform(
                                        wrapper.user(),
                                        wrapper.get(Type.POSITION, 0),
                                        wrapper.get(Type.NBT, 0)
                                ));
                    }
                });
            }
        });

        // Block Change
        protocol.out(State.PLAY, 0x0B, 0x0B, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // 0 - Position

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int blockState = wrapper.read(Type.VAR_INT);

                        // Store blocks for
                        BackwardsBlockStorage storage = wrapper.user().get(BackwardsBlockStorage.class);
                        storage.checkAndStore(wrapper.get(Type.POSITION, 0), blockState);

                        wrapper.write(Type.VAR_INT, toOldId(blockState));
                    }
                });
            }
        });

        // Multi Block Change
        protocol.out(State.PLAY, 0x0F, 0x10, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Chunk X
                map(Type.INT); // 1 - Chunk Z
                map(Type.BLOCK_CHANGE_RECORD_ARRAY);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        BackwardsBlockStorage storage = wrapper.user().get(BackwardsBlockStorage.class);

                        for (BlockChangeRecord record : wrapper.get(Type.BLOCK_CHANGE_RECORD_ARRAY, 0)) {
                            int chunkX = wrapper.get(Type.INT, 0);
                            int chunkZ = wrapper.get(Type.INT, 1);
                            int block = record.getBlockId();
                            Position position = new Position(
                                    (long) (record.getHorizontal() >> 4 & 15) + (chunkX * 16),
                                    (long) record.getY(),
                                    (long) (record.getHorizontal() & 15) + (chunkZ * 16));

                            // Store if needed
                            storage.checkAndStore(position, block);

                            // Change to old id
                            record.setBlockId(toOldId(block));
                        }
                    }
                });
            }
        });

        // Windows Items
        protocol.out(State.PLAY, 0x15, 0x14, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE);
                map(Type.FLAT_ITEM_ARRAY, Type.ITEM_ARRAY);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item[] items = wrapper.get(Type.ITEM_ARRAY, 0);
                        for (Item item : items) {
                            toClient(item);
                        }
                    }
                });
            }
        });

        // Set Slot
        protocol.out(State.PLAY, 0x17, 0x16, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE);
                map(Type.SHORT);
                map(Type.FLAT_ITEM, Type.ITEM);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item item = wrapper.get(Type.ITEM, 0);
                        toClient(item);
                    }
                });
            }
        });

        // Chunk packet
        protocol.out(State.PLAY, 0x22, 0x20, new PacketRemapper() {
                    @Override
                    public void registerMap() {
                        handler(new PacketHandler() {
                            @Override
                            public void handle(PacketWrapper wrapper) throws Exception {
                                ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

                                Chunk1_9_3_4Type type_old = new Chunk1_9_3_4Type(clientWorld);
                                Chunk1_13Type type = new Chunk1_13Type(clientWorld);
                                Chunk chunk = wrapper.read(type);


                                // Handle Block Entities before block rewrite
                                BackwardsBlockEntityProvider provider = Via.getManager().getProviders().get(BackwardsBlockEntityProvider.class);
                                BackwardsBlockStorage storage = wrapper.user().get(BackwardsBlockStorage.class);
                                for (CompoundTag tag : chunk.getBlockEntities()) {
                                    if (!tag.contains("id"))
                                        continue;

                                    String id = (String) tag.get("id").getValue();

                                    // Ignore if we don't handle it
                                    if (!provider.isHandled(id))
                                        continue;

                                    int sectionIndex = ((int) tag.get("y").getValue()) >> 4;
                                    ChunkSection section = chunk.getSections()[sectionIndex];

                                    int x = (int) tag.get("x").getValue();
                                    int y = (int) tag.get("y").getValue();
                                    int z = (int) tag.get("z").getValue();
                                    Position position = new Position((long) x, (long) y, (long) z);

                                    int block = section.getFlatBlock(x & 0xF, y & 0xF, z & 0xF);
                                    storage.checkAndStore(position, block);

                                    provider.transform(wrapper.user(), position, tag);
                                }

                                // Rewrite new blocks to old blocks
                                for (int i = 0; i < chunk.getSections().length; i++) {
                                    ChunkSection section = chunk.getSections()[i];
                                    if (section == null) {
                                        continue;
                                    }

                                    for (int p = 0; p < section.getPaletteSize(); p++) {
                                        int old = section.getPaletteEntry(p);
                                        if (old != 0) {
                                            section.setPaletteEntry(p, toOldId(old));
                                        }
                                    }
                                }

                                // Rewrite biome id 255 to plains
                                if (chunk.isBiomeData()) {
                                    for (int i = 0; i < 256; i++) {
                                        chunk.getBiomeData()[i] = 1; // Plains
                                    }
                                }

                                wrapper.write(type_old, chunk);
                            }
                        });
                    }
                }
        );

        // Effect
        protocol.out(State.PLAY, 0x23, 0x21, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Effect Id
                map(Type.POSITION); // Location
                map(Type.INT); // Data
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.INT, 0);
                        int data = wrapper.get(Type.INT, 1);
                        if (id == 1010) { // Play record
                            wrapper.set(Type.INT, 1, data = MappingData.oldToNewItems.inverse().get(data) >> 4);
                        } else if (id == 2001) { // Block break + block break sound
                            data = toOldId(data);
                            int blockId = data >> 4;
                            int blockData = data & 0xF;
                            wrapper.set(Type.INT, 1, data = (blockId & 0xFFF) | (blockData << 12));
                        }
                    }
                });
            }
        });

        // Map
        protocol.out(State.PLAY, 0x26, 0x24, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.BYTE);
                map(Type.BOOLEAN);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int iconCount = wrapper.passthrough(Type.VAR_INT);
                        for (int i = 0; i < iconCount; i++) {
                            int type = wrapper.read(Type.VAR_INT);
                            byte x = wrapper.read(Type.BYTE);
                            byte z = wrapper.read(Type.BYTE);
                            byte direction = wrapper.read(Type.BYTE);
                            if (wrapper.read(Type.BOOLEAN)) {
                                wrapper.read(Type.STRING);
                            }
                            if (type > 9) continue;
                            wrapper.write(Type.BYTE, (byte) ((type << 4) | (direction & 0x0F)));
                            wrapper.write(Type.BYTE, x);
                            wrapper.write(Type.BYTE, z);
                        }
                    }
                });
            }
        });

        // Entity Equipment
        protocol.out(State.PLAY, 0x42, 0x3F, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.VAR_INT);
                map(Type.FLAT_ITEM, Type.ITEM);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item item = wrapper.get(Type.ITEM, 0);
                        toClient(item);
                    }
                });
            }
        });


        // Set Creative Slot
        protocol.in(State.PLAY, 0x24, 0x1B, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.SHORT);
                map(Type.ITEM, Type.FLAT_ITEM);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item item = wrapper.get(Type.FLAT_ITEM, 0);
                        toServer(item);
                    }
                });
            }
        });

        // Click Window
        protocol.in(State.PLAY, 0x08, 0x07, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE);
                map(Type.SHORT);
                map(Type.BYTE);
                map(Type.SHORT);
                map(Type.VAR_INT);
                map(Type.ITEM, Type.FLAT_ITEM);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item item = wrapper.get(Type.FLAT_ITEM, 0);
                        toServer(item);
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {

    }
}
