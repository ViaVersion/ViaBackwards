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
import nl.matsv.viabackwards.api.rewriters.BlockItemRewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.FlowerPotHandler;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.BlockChangeRecord;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.BlockIdData;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.SpawnEggRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.types.Chunk1_13Type;
import us.myles.ViaVersion.protocols.protocol1_9_1_2to1_9_3_4.types.Chunk1_9_3_4Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.viaversion.libs.opennbt.conversion.ConverterRegistry;
import us.myles.viaversion.libs.opennbt.tag.builtin.*;

import java.util.*;

public class BlockItemPackets1_13 extends BlockItemRewriter<Protocol1_12_2To1_13> {

    private static String NBT_TAG_NAME;
    private static final Map<String, String> enchantmentMappings = new HashMap<>();

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

    public static boolean isDamageable(int id) {
        return id >= 256 && id <= 259 // iron shovel, pickaxe, axe, flint and steel
                || id == 261 // bow
                || id >= 267 && id <= 279 // iron sword, wooden+stone+diamond swords, shovels, pickaxes, axes
                || id >= 283 && id <= 286 // gold sword, shovel, pickaxe, axe
                || id >= 290 && id <= 294 // hoes
                || id >= 298 && id <= 317 // armors
                || id == 346 // fishing rod
                || id == 359 // shears
                || id == 398 // carrot on a stick
                || id == 442 // shield
                || id == 443; // elytra
    }

    @Override
    protected void registerPackets(Protocol1_12_2To1_13 protocol) {
        NBT_TAG_NAME = "ViaBackwards|" + protocol.getClass().getSimpleName() + "|Part2";
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
                        Position position = wrapper.get(Type.POSITION, 0);

                        // Store blocks
                        BackwardsBlockStorage storage = wrapper.user().get(BackwardsBlockStorage.class);
                        storage.checkAndStore(position, blockState);

                        wrapper.write(Type.VAR_INT, toOldId(blockState));

                        // Flower pot special treatment
                        flowerPotSpecialTreatment(wrapper.user(), blockState, position);
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

                            // Flower pot special treatment
                            flowerPotSpecialTreatment(wrapper.user(), block, position);

                            // Change to old id
                            record.setBlockId(toOldId(block));
                        }
                    }
                });
            }
        });

        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);

        // Windows Items
        protocol.out(State.PLAY, 0x15, 0x14, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE);
                map(Type.FLAT_ITEM_ARRAY, Type.ITEM_ARRAY);

                handler(itemRewriter.itemArrayHandler(Type.ITEM_ARRAY));
            }
        });

        // Set Slot
        protocol.out(State.PLAY, 0x17, 0x16, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE);
                map(Type.SHORT);
                map(Type.FLAT_ITEM, Type.ITEM);

                handler(itemRewriter.itemToClientHandler(Type.ITEM));
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

                                    // Flower pots require a special treatment, they are no longer block entities :(
                                    for (int y = 0; y < 16; y++) {
                                        for (int z = 0; z < 16; z++) {
                                            for (int x = 0; x < 16; x++) {
                                                int block = section.getFlatBlock(x, y, z);

                                                // Check if the block is a flower
                                                if (FlowerPotHandler.isFlowah(block)) {
                                                    Position pos = new Position(
                                                            (long) (x + (chunk.getX() << 4)),
                                                            (long) (y + (i << 4)),
                                                            (long) (z + (chunk.getZ() << 4))
                                                    );
                                                    // Store block
                                                    storage.checkAndStore(pos, block);

                                                    CompoundTag nbt = provider.transform(wrapper.user(), pos, "minecraft:flower_pot");

                                                    chunk.getBlockEntities().add(nbt);
                                                }
                                            }
                                        }
                                    }

                                    for (int p = 0; p < section.getPaletteSize(); p++) {
                                        int old = section.getPaletteEntry(p);
                                        if (old != 0) {
                                            int oldId = toOldId(old);
                                            section.setPaletteEntry(p, oldId);
                                        }
                                    }
                                }


                                if (chunk.isBiomeData()) {
                                    for (int i = 0; i < 256; i++) {
                                        int biome = chunk.getBiomeData()[i];
                                        int newId = -1;
                                        switch (biome) {
                                            case 40: // end biomes
                                            case 41:
                                            case 42:
                                            case 43:
                                                newId = 9;
                                                break;
                                            case 47: // deep ocean biomes
                                            case 48:
                                            case 49:
                                                newId = 24;
                                                break;
                                            case 50: // deep frozen... let's just pick the frozen variant
                                                newId = 10;
                                                break;
                                            case 44: // the other new ocean biomes
                                            case 45:
                                            case 46:
                                                newId = 0;
                                                break;
                                        }

                                        if (newId != -1) {
                                            chunk.getBiomeData()[i] = newId;
                                        }
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
                            if (type > 9) {
                                wrapper.set(Type.VAR_INT, 1, wrapper.get(Type.VAR_INT, 1) - 1);
                                continue;
                            }
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

                handler(itemRewriter.itemToClientHandler(Type.ITEM));
            }
        });


        // Set Creative Slot
        protocol.in(State.PLAY, 0x24, 0x1B, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.SHORT);
                map(Type.ITEM, Type.FLAT_ITEM);

                handler(itemRewriter.itemToServerHandler(Type.FLAT_ITEM));
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

                handler(itemRewriter.itemToServerHandler(Type.FLAT_ITEM));
            }
        });
    }

    @Override
    protected void registerRewrites() {
        rewrite(245).repItem(new Item((short) 241, (byte) 1, (short) -1, getNamedTag("1.13 Acacia Button")));
        rewrite(243).repItem(new Item((short) 241, (byte) 1, (short) -1, getNamedTag("1.13 Birch Button")));
        rewrite(242).repItem(new Item((short) 241, (byte) 1, (short) -1, getNamedTag("1.13 Spruce Button")));
        rewrite(244).repItem(new Item((short) 241, (byte) 1, (short) -1, getNamedTag("1.13 Jungle Button")));
        rewrite(246).repItem(new Item((short) 241, (byte) 1, (short) -1, getNamedTag("1.13 Dark Oak Button")));

        rewrite(191).repItem(new Item((short) 187, (byte) 1, (short) -1, getNamedTag("1.13 Acacia Trapdoor")));
        rewrite(189).repItem(new Item((short) 187, (byte) 1, (short) -1, getNamedTag("1.13 Birch Trapdoor")));
        rewrite(188).repItem(new Item((short) 187, (byte) 1, (short) -1, getNamedTag("1.13 Spruce Trapdoor")));
        rewrite(190).repItem(new Item((short) 187, (byte) 1, (short) -1, getNamedTag("1.13 Jungle Trapdoor")));
        rewrite(192).repItem(new Item((short) 187, (byte) 1, (short) -1, getNamedTag("1.13 Dark Oak Trapdoor")));

        rewrite(164).repItem(new Item((short) 187, (byte) 1, (short) -1, getNamedTag("1.13 Acacia Pressure Plate")));
        rewrite(162).repItem(new Item((short) 187, (byte) 1, (short) -1, getNamedTag("1.13 Birch Pressure Plate")));
        rewrite(161).repItem(new Item((short) 187, (byte) 1, (short) -1, getNamedTag("1.13 Spruce Pressure Plate")));
        rewrite(163).repItem(new Item((short) 187, (byte) 1, (short) -1, getNamedTag("1.13 Jungle Pressure Plate")));
        rewrite(165).repItem(new Item((short) 187, (byte) 1, (short) -1, getNamedTag("1.13 Dark Oak Pressure Plate")));

        rewrite(762).repItem(new Item((short) 544, (byte) 1, (short) -1, getNamedTag("1.13 Acacia Boat")));
        rewrite(760).repItem(new Item((short) 544, (byte) 1, (short) -1, getNamedTag("1.13 Birch Boat")));
        rewrite(759).repItem(new Item((short) 544, (byte) 1, (short) -1, getNamedTag("1.13 Spruce Boat")));
        rewrite(761).repItem(new Item((short) 544, (byte) 1, (short) -1, getNamedTag("1.13 Jungle Boat")));
        rewrite(763).repItem(new Item((short) 544, (byte) 1, (short) -1, getNamedTag("1.13 Dark Oak Boat")));

        rewrite(453).repItem(new Item((short) 300, (byte) 1, (short) -1, getNamedTag("1.13 Blue Ice")));

        rewrite(547).repItem(new Item((short) 538, (byte) 1, (short) -1, getNamedTag("1.13 Bucket of Pufferfish")));
        rewrite(548).repItem(new Item((short) 538, (byte) 1, (short) -1, getNamedTag("1.13 Bucket of Salmon")));
        rewrite(549).repItem(new Item((short) 538, (byte) 1, (short) -1, getNamedTag("1.13 Bucket of Cod")));
        rewrite(550).repItem(new Item((short) 538, (byte) 1, (short) -1, getNamedTag("1.13 Bucket of Tropical Fish")));

        rewrite(784).repItem(new Item((short) 543, (byte) 1, (short) -1, getNamedTag("1.13 Heart of the Sea")));
        rewrite(783).repItem(new Item((short) 587, (byte) 1, (short) -1, getNamedTag("1.13 Nautilus Shell")));
        rewrite(782).repItem(new Item((short) 545, (byte) 1, (short) -1, getNamedTag("1.13 Phantom Membrane")));
        rewrite(465).repItem(new Item((short) 510, (byte) 1, (short) -1, getNamedTag("1.13 Turtle Shell")));
        rewrite(427).repItem(new Item((short) 561, (byte) 1, (short) -1, getNamedTag("1.13 Turtle Egg")));
        rewrite(466).repItem(new Item((short) 582, (byte) 1, (short) -1, getNamedTag("1.13 Scute")));
        rewrite(781).repItem(new Item((short) 488, (byte) 1, (short) -1, getNamedTag("1.13 Trident")));
        rewrite(80).repItem(new Item((short) 561, (byte) 1, (short) -1, getNamedTag("1.13 Sea Pickle")));
        rewrite(79).repItem(new Item((short) 76, (byte) 1, (short) -1, getNamedTag("1.13 Seagrass")));
        rewrite(454).repItem(new Item((short) 238, (byte) 1, (short) -1, getNamedTag("1.13 Conduit")));

        rewrite(554).repItem(new Item((short) 76, (byte) 1, (short) -1, getNamedTag("1.13 Kelp")));
        rewrite(611).repItem(new Item((short) 508, (byte) 1, (short) -1, getNamedTag("1.13 Dried Kelp")));
        rewrite(555).repItem(new Item((short) 281, (byte) 1, (short) -1, getNamedTag("1.13 Dried Kelp Block")));

        rewrite(38).repItem(new Item((short) 32, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Oak Log")));
        rewrite(42).repItem(new Item((short) 36, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Acacia Log")));
        rewrite(40).repItem(new Item((short) 34, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Birch Log")));
        rewrite(39).repItem(new Item((short) 33, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Spruce Log")));
        rewrite(41).repItem(new Item((short) 35, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Jungle Log")));
        rewrite(43).repItem(new Item((short) 37, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Dark Oak Log")));

        rewrite(44).repItem(new Item((short) 32, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Oak Wood")));
        rewrite(48).repItem(new Item((short) 36, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Acacia Wood")));
        rewrite(46).repItem(new Item((short) 34, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Birch Wood")));
        rewrite(45).repItem(new Item((short) 33, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Spruce Wood")));
        rewrite(47).repItem(new Item((short) 35, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Jungle Wood")));
        rewrite(49).repItem(new Item((short) 37, (byte) 1, (short) -1, getNamedTag("1.13 Stripped Dark Oak Wood")));

        rewrite(50).repItem(new Item((short) 32, (byte) 1, (short) -1, getNamedTag("1.13 Oak Wood")));
        rewrite(51).repItem(new Item((short) 33, (byte) 1, (short) -1, getNamedTag("1.13 Spruce Wood")));
        rewrite(52).repItem(new Item((short) 34, (byte) 1, (short) -1, getNamedTag("1.13 Birch Wood")));
        rewrite(53).repItem(new Item((short) 35, (byte) 1, (short) -1, getNamedTag("1.13 Jungle Wood")));
        rewrite(54).repItem(new Item((short) 36, (byte) 1, (short) -1, getNamedTag("1.13 Acacia Wood")));
        rewrite(55).repItem(new Item((short) 37, (byte) 1, (short) -1, getNamedTag("1.13 Dark Oak Wood")));

        rewrite(128).repItem(new Item((short) 121, (byte) 1, (short) -1, getNamedTag("1.13 Prismarine Slab")));
        rewrite(129).repItem(new Item((short) 122, (byte) 1, (short) -1, getNamedTag("1.13 Prismarine Brick Slab")));
        rewrite(130).repItem(new Item((short) 123, (byte) 1, (short) -1, getNamedTag("1.13 Dark Prismarine Slab")));
        rewrite(346).repItem(new Item((short) 157, (byte) 1, (short) -1, getNamedTag("1.13 Prismarine Stairs")));
        rewrite(347).repItem(new Item((short) 216, (byte) 1, (short) -1, getNamedTag("1.13 Prismarine Brick Stairs")));
        rewrite(348).repItem(new Item((short) 217, (byte) 1, (short) -1, getNamedTag("1.13 Dark Prismarine Brick Stairs")));

        //Spawn Eggs:
        rewrite(643).repItem(new Item((short) 662, (byte) 1, (short) -1, getNamedTag("1.13 Drowned Spawn Egg")));
        rewrite(658).repItem(new Item((short) 662, (byte) 1, (short) -1, getNamedTag("1.13 Phantom Spawn Egg")));
        rewrite(641).repItem(new Item((short) 662, (byte) 1, (short) -1, getNamedTag("1.13 Dolphin Spawn Egg")));
        rewrite(674).repItem(new Item((short) 662, (byte) 1, (short) -1, getNamedTag("1.13 Turtle Spawn Egg")));
        rewrite(638).repItem(new Item((short) 662, (byte) 1, (short) -1, getNamedTag("1.13 Cod Spawn Egg")));
        rewrite(663).repItem(new Item((short) 662, (byte) 1, (short) -1, getNamedTag("1.13 Salmon Spawn Egg")));
        rewrite(661).repItem(new Item((short) 662, (byte) 1, (short) -1, getNamedTag("1.13 Pufferfish Spawn Egg")));
        rewrite(673).repItem(new Item((short) 662, (byte) 1, (short) -1, getNamedTag("1.13 Tropical Fish Spawn Egg")));

        //Corals
        rewrite(438).repItem(new Item((short) 100, (byte) 1, (short) -1, getNamedTag("1.13 Tube Coral")));
        rewrite(439).repItem(new Item((short) 106, (byte) 1, (short) -1, getNamedTag("1.13 Brain Coral")));
        rewrite(440).repItem(new Item((short) 101, (byte) 1, (short) -1, getNamedTag("1.13 Bubble Coral")));
        rewrite(441).repItem(new Item((short) 103, (byte) 1, (short) -1, getNamedTag("1.13 Fire Coral")));
        rewrite(442).repItem(new Item((short) 98, (byte) 1, (short) -1, getNamedTag("1.13 Horn Coral")));

        rewrite(443).repItem(new Item((short) 100, (byte) 1, (short) -1, getNamedTag("1.13 Tube Coral Fan")));
        rewrite(444).repItem(new Item((short) 106, (byte) 1, (short) -1, getNamedTag("1.13 Brain Coral Fan")));
        rewrite(445).repItem(new Item((short) 101, (byte) 1, (short) -1, getNamedTag("1.13 Bubble Coral Fan")));
        rewrite(446).repItem(new Item((short) 103, (byte) 1, (short) -1, getNamedTag("1.13 Fire Coral Fan")));
        rewrite(447).repItem(new Item((short) 98, (byte) 1, (short) -1, getNamedTag("1.13 Horn Coral Fan")));

        rewrite(448).repItem(new Item((short) 78, (byte) 1, (short) -1, getNamedTag("1.13 Dead Tube Coral Fan")));
        rewrite(449).repItem(new Item((short) 78, (byte) 1, (short) -1, getNamedTag("1.13 Dead Brain Coral Fan")));
        rewrite(450).repItem(new Item((short) 78, (byte) 1, (short) -1, getNamedTag("1.13 Dead Bubble Coral Fan")));
        rewrite(451).repItem(new Item((short) 78, (byte) 1, (short) -1, getNamedTag("1.13 Dead Fire Coral Fan")));
        rewrite(452).repItem(new Item((short) 78, (byte) 1, (short) -1, getNamedTag("1.13 Dead Horn Coral Fan")));

        rewrite(428).repItem(new Item((short) 90, (byte) 1, (short) -1, getNamedTag("1.13 Dead Tube Coral Block")));
        rewrite(429).repItem(new Item((short) 90, (byte) 1, (short) -1, getNamedTag("1.13 Dead Brain Coral Block")));
        rewrite(430).repItem(new Item((short) 90, (byte) 1, (short) -1, getNamedTag("1.13 Dead Bubble Coral Block")));
        rewrite(431).repItem(new Item((short) 90, (byte) 1, (short) -1, getNamedTag("1.13 Dead Fire Coral Block")));
        rewrite(432).repItem(new Item((short) 90, (byte) 1, (short) -1, getNamedTag("1.13 Dead Horn Coral Block")));

        rewrite(433).repItem(new Item((short) 93, (byte) 1, (short) -1, getNamedTag("1.13 Tube Coral Block")));
        rewrite(434).repItem(new Item((short) 88, (byte) 1, (short) -1, getNamedTag("1.13 Brain Coral Block")));
        rewrite(435).repItem(new Item((short) 92, (byte) 1, (short) -1, getNamedTag("1.13 Bubble Coral Block")));
        rewrite(436).repItem(new Item((short) 96, (byte) 1, (short) -1, getNamedTag("1.13 Fire Coral Block")));
        rewrite(437).repItem(new Item((short) 86, (byte) 1, (short) -1, getNamedTag("1.13 Horn Coral Block")));
        // Coral End

        rewrite(131).repItem(new Item((short) 711, (byte) 1, (short) -1, getNamedTag("1.13 Smooth Quartz")));
        rewrite(132).repItem(new Item((short) 350, (byte) 1, (short) -1, getNamedTag("1.13 Smooth Red Sandstone")));
        rewrite(133).repItem(new Item((short) 68, (byte) 1, (short) -1, getNamedTag("1.13 Smooth Sandstone")));
        rewrite(134).repItem(new Item((short) 118, (byte) 1, (short) -1, getNamedTag("1.13 Smooth Stone")));

        rewrite(181).repItem(new Item((short) 182, (byte) 1, (short) -1, getNamedTag("1.13 Carved Pumpkin")));

        rewrite(205).repItem(new Item((short) 90, (byte) 1, (short) -1, getNamedTag("1.13 Mushroom Stem")));


        enchantmentMappings.put("minecraft:loyalty", "§7Loyalty");
        enchantmentMappings.put("minecraft:impaling", "§7Impaling");
        enchantmentMappings.put("minecraft:riptide", "§7Riptide");
        enchantmentMappings.put("minecraft:channeling", "§7Channeling");
    }

    @Override
    protected CompoundTag getNamedTag(String text) {
        CompoundTag tag = new CompoundTag("");
        tag.put(new CompoundTag("display"));
        ((CompoundTag) tag.get("display")).put(new StringTag("Name", ChatRewriter.legacyTextToJson(text)));
        return tag;
    }

    @Override
    public Item handleItemToClient(Item item) {
        if (item == null) return null;
        item = super.handleItemToClient(item);

        Integer rawId = null;
        boolean gotRawIdFromTag = false;

        CompoundTag tag = item.getTag();

        // Use tag to get original ID and data
        if (tag != null) {
            // Check for valid tag
            if (tag.get(NBT_TAG_NAME) instanceof IntTag) {
                rawId = (Integer) tag.get(NBT_TAG_NAME).getValue();
                // Remove the tag
                tag.remove(NBT_TAG_NAME);
                gotRawIdFromTag = true;
            }
        }

        if (rawId == null) {
            Integer oldId = MappingData.oldToNewItems.inverse().get(item.getIdentifier());
            if (oldId != null) {
                // Handle spawn eggs
                Optional<String> eggEntityId = SpawnEggRewriter.getEntityId(oldId);
                if (eggEntityId.isPresent()) {
                    rawId = 383 << 16;
                    if (tag == null)
                        item.setTag(tag = new CompoundTag("tag"));
                    if (!tag.contains("EntityTag")) {
                        CompoundTag entityTag = new CompoundTag("EntityTag");
                        entityTag.put(new StringTag("id", eggEntityId.get()));
                        tag.put(entityTag);
                    }
                } else {
                    rawId = (oldId >> 4) << 16 | oldId & 0xF;
                }
            } else if (item.getIdentifier() == 362) { // base/colorless shulker box
                rawId = 0xe50000; // purple shulker box
            }
        }

        if (rawId == null) {
            if (!Via.getConfig().isSuppress1_13ConversionErrors() || Via.getManager().isDebug()) {
                ViaBackwards.getPlatform().getLogger().warning("Failed to get 1.12 item for " + item.getIdentifier());
            }
            rawId = 0x10000; // Stone
        }

        item.setIdentifier(rawId >> 16);
        item.setData((short) (rawId & 0xFFFF));

        // NBT changes
        if (tag != null) {
            if (isDamageable(item.getIdentifier())) {
                if (tag.get("Damage") instanceof IntTag) {
                    if (!gotRawIdFromTag)
                        item.setData((short) (int) tag.get("Damage").getValue());
                    tag.remove("Damage");
                }
            }

            if (item.getIdentifier() == 358) { // map
                if (tag.get("map") instanceof IntTag) {
                    if (!gotRawIdFromTag)
                        item.setData((short) (int) tag.get("map").getValue());
                    tag.remove("map");
                }
            }

            if (item.getIdentifier() == 442 || item.getIdentifier() == 425) { // shield / banner
                if (tag.get("BlockEntityTag") instanceof CompoundTag) {
                    CompoundTag blockEntityTag = tag.get("BlockEntityTag");
                    if (blockEntityTag.get("Base") instanceof IntTag) {
                        IntTag base = blockEntityTag.get("Base");
                        base.setValue(15 - base.getValue()); // invert color id
                    }
                    if (blockEntityTag.get("Patterns") instanceof ListTag) {
                        for (Tag pattern : (ListTag) blockEntityTag.get("Patterns")) {
                            if (pattern instanceof CompoundTag) {
                                IntTag c = ((CompoundTag) pattern).get("Color");
                                c.setValue(15 - c.getValue()); // Invert color id
                            }
                        }
                    }
                }
            }
            // Display Name now uses JSON
            if (tag.get("display") instanceof CompoundTag) {
                CompoundTag display = tag.get("display");
                if (((CompoundTag) tag.get("display")).get("Name") instanceof StringTag) {
                    StringTag name = display.get("Name");
                    StringTag via = display.get(NBT_TAG_NAME + "|Name");
                    name.setValue(via != null ? via.getValue() : ChatRewriter.jsonTextToLegacy(name.getValue()));
                    display.remove(NBT_TAG_NAME + "|Name");
                }
            }

            // ench is now Enchantments and now uses identifiers
            if (tag.get("Enchantments") instanceof ListTag) {
                rewriteEnchantmentsToClient(tag, false);
            }
            if (tag.get("StoredEnchantments") instanceof ListTag) {
                rewriteEnchantmentsToClient(tag, true);
            }

            if (tag.get(NBT_TAG_NAME + "|CanPlaceOn") instanceof ListTag) {
                tag.put(ConverterRegistry.convertToTag(
                        "CanPlaceOn",
                        ConverterRegistry.convertToValue(tag.get(NBT_TAG_NAME + "|CanPlaceOn"))
                ));
                tag.remove(NBT_TAG_NAME + "|CanPlaceOn");
            } else if (tag.get("CanPlaceOn") instanceof ListTag) {
                ListTag old = tag.get("CanPlaceOn");
                ListTag newCanPlaceOn = new ListTag("CanPlaceOn", StringTag.class);
                for (Tag oldTag : old) {
                    Object value = oldTag.getValue();
                    String[] newValues = BlockIdData.fallbackReverseMapping.get(value instanceof String
                            ? ((String) value).replace("minecraft:", "")
                            : null);
                    if (newValues != null) {
                        for (String newValue : newValues) {
                            newCanPlaceOn.add(new StringTag("", newValue));
                        }
                    } else {
                        newCanPlaceOn.add(oldTag);
                    }
                }
                tag.put(newCanPlaceOn);
            }
            if (tag.get(NBT_TAG_NAME + "|CanDestroy") instanceof ListTag) {
                tag.put(ConverterRegistry.convertToTag(
                        "CanDestroy",
                        ConverterRegistry.convertToValue(tag.get(NBT_TAG_NAME + "|CanDestroy"))
                ));
                tag.remove(NBT_TAG_NAME + "|CanDestroy");
            } else if (tag.get("CanDestroy") instanceof ListTag) {
                ListTag old = tag.get("CanDestroy");
                ListTag newCanDestroy = new ListTag("CanDestroy", StringTag.class);
                for (Tag oldTag : old) {
                    Object value = oldTag.getValue();
                    String[] newValues = BlockIdData.fallbackReverseMapping.get(value instanceof String
                            ? ((String) value).replace("minecraft:", "")
                            : null);
                    if (newValues != null) {
                        for (String newValue : newValues) {
                            newCanDestroy.add(new StringTag("", newValue));
                        }
                    } else {
                        newCanDestroy.add(oldTag);
                    }
                }
                tag.put(newCanDestroy);
            }
        }
        return item;
    }

    private void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnch) {
        String key = storedEnch ? "StoredEnchantments" : "Enchantments";
        ListTag enchantments = tag.get(key);
        ListTag noMapped = new ListTag(NBT_TAG_NAME + "|" + key, CompoundTag.class);
        ListTag newEnchantments = new ListTag(storedEnch ? key : "ench", CompoundTag.class);
        List<Tag> lore = new ArrayList<>();
        boolean hasValidEnchants = false;
        for (Tag enchantmentEntry : enchantments.clone()) {
            CompoundTag enchEntry = new CompoundTag("");
            String newId = (String) ((CompoundTag) enchantmentEntry).get("id").getValue();
            if (enchantmentMappings.containsKey(newId)) {
                lore.add(new StringTag("", enchantmentMappings.get(newId) + " "
                        + getRomanNumber((Short) ((CompoundTag) enchantmentEntry).get("lvl").getValue())));
                noMapped.add(enchantmentEntry);
            } else if (!newId.isEmpty()) {
                Short oldId = MappingData.oldEnchantmentsIds.inverse().get(newId);
                if (oldId == null) {
                    if (!newId.startsWith("viaversion:legacy/")) {
                        // Custom enchant (?)
                        noMapped.add(enchantmentEntry);

                        // Some custom-enchant plugins write it into the lore manually, which would double its entry
                        if (ViaBackwards.getConfig().addCustomEnchantsToLore()) {
                            String name = newId;
                            if (name.contains(":"))
                                name = name.split(":")[1];
                            name = "§7" + Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase(Locale.ENGLISH);

                            lore.add(new StringTag("", name + " "
                                    + getRomanNumber((Short) ((CompoundTag) enchantmentEntry).get("lvl").getValue())));
                        }

                        if (Via.getManager().isDebug())
                            ViaBackwards.getPlatform().getLogger().warning("Found unknown enchant: " + newId);
                        continue;
                    } else {
                        oldId = Short.valueOf(newId.substring(18));
                    }
                }

                Short level = (Short) ((CompoundTag) enchantmentEntry).get("lvl").getValue();
                if (level != 0)
                    hasValidEnchants = true;
                enchEntry.put(new ShortTag("id", oldId));
                enchEntry.put(new ShortTag("lvl", level));
                newEnchantments.add(enchEntry);
            }
        }

        // Put here to hide empty enchantment from 1.14 rewrites
        if (!storedEnch && !hasValidEnchants) {
            IntTag hideFlags = tag.get("HideFlags");
            if (hideFlags == null) {
                hideFlags = new IntTag("HideFlags");
                tag.put(new ByteTag(NBT_TAG_NAME + "|DummyEnchant"));
            } else {
                tag.put(new IntTag(NBT_TAG_NAME + "|OldHideFlags", hideFlags.getValue()));
            }

            if (newEnchantments.size() == 0) {
                CompoundTag enchEntry = new CompoundTag("");
                enchEntry.put(new ShortTag("id", (short) 0));
                enchEntry.put(new ShortTag("lvl", (short) 0));
                newEnchantments.add(enchEntry);
            }

            int value = hideFlags.getValue() | 1;
            hideFlags.setValue(value);
            tag.put(hideFlags);
        }

        if (noMapped.size() != 0) {
            tag.put(noMapped);

            if (!lore.isEmpty()) {
                CompoundTag display = tag.get("display");
                if (display == null) {
                    tag.put(display = new CompoundTag("display"));
                }

                ListTag loreTag = display.get("Lore");
                if (loreTag == null) {
                    display.put(loreTag = new ListTag("Lore", StringTag.class));
                    tag.put(new ByteTag(NBT_TAG_NAME + "|DummyLore"));
                } else if (loreTag.size() != 0) {
                    ListTag oldLore = new ListTag(NBT_TAG_NAME + "|OldLore", StringTag.class);
                    for (Tag value : loreTag) {
                        oldLore.add(value.clone());
                    }
                    tag.put(oldLore);

                    lore.addAll(loreTag.getValue());
                }

                loreTag.setValue(lore);
            }
        }

        tag.remove("Enchantments");
        tag.put(newEnchantments);
    }

    @Override
    protected Item handleItemToServer(Item item) {
        if (item == null) return null;
        CompoundTag tag = item.getTag();

        // Save original id
        int originalId = (item.getIdentifier() << 16 | item.getData() & 0xFFFF);

        int rawId = (item.getIdentifier() << 4 | item.getData() & 0xF);

        // NBT Additions
        if (isDamageable(item.getIdentifier())) {
            if (tag == null) item.setTag(tag = new CompoundTag("tag"));
            tag.put(new IntTag("Damage", item.getData()));
        }
        if (item.getIdentifier() == 358) { // map
            if (tag == null) item.setTag(tag = new CompoundTag("tag"));
            tag.put(new IntTag("map", item.getData()));
        }

        // NBT Changes
        if (tag != null) {
            // Invert shield color id
            if (item.getIdentifier() == 442 || item.getIdentifier() == 425) {
                if (tag.get("BlockEntityTag") instanceof CompoundTag) {
                    CompoundTag blockEntityTag = tag.get("BlockEntityTag");
                    if (blockEntityTag.get("Base") instanceof IntTag) {
                        IntTag base = blockEntityTag.get("Base");
                        base.setValue(15 - base.getValue());
                    }
                    if (blockEntityTag.get("Patterns") instanceof ListTag) {
                        for (Tag pattern : (ListTag) blockEntityTag.get("Patterns")) {
                            if (pattern instanceof CompoundTag) {
                                IntTag c = ((CompoundTag) pattern).get("Color");
                                c.setValue(15 - c.getValue()); // Invert color id
                            }
                        }
                    }
                }
            }
            // Display Name now uses JSON
            Tag display = tag.get("display");
            if (display instanceof CompoundTag) {
                CompoundTag displayTag = (CompoundTag) display;
                StringTag name = displayTag.get("Name");
                if (name instanceof StringTag) {
                    displayTag.put(new StringTag(NBT_TAG_NAME + "|Name", name.getValue()));
                    name.setValue(
                            ChatRewriter.legacyTextToJson(
                                    name.getValue()
                            )
                    );
                }
            }

            // ench is now Enchantments and now uses identifiers
            if (tag.get("ench") instanceof ListTag) {
                rewriteEnchantmentsToServer(tag, false);
            }
            if (tag.get("StoredEnchantments") instanceof ListTag) {
                rewriteEnchantmentsToServer(tag, true);
            }

            // Handle SpawnEggs
            if (item.getIdentifier() == 383) {
                if (tag.get("EntityTag") instanceof CompoundTag) {
                    CompoundTag entityTag = tag.get("EntityTag");
                    if (entityTag.get("id") instanceof StringTag) {
                        StringTag identifier = entityTag.get("id");
                        rawId = SpawnEggRewriter.getSpawnEggId(identifier.getValue());
                        if (rawId == -1) {
                            rawId = 25100288; // Bat fallback
                        } else {
                            entityTag.remove("id");
                            if (entityTag.isEmpty())
                                tag.remove("EntityTag");
                        }
                    } else {
                        // Fallback to bat
                        rawId = 25100288;
                    }
                } else {
                    // Fallback to bat
                    rawId = 25100288;
                }
            }
            if (tag.isEmpty()) {
                item.setTag(tag = null);
            }
        }

        int newId = -1;
        if (!MappingData.oldToNewItems.containsKey(rawId)) {
            if (!isDamageable(item.getIdentifier()) && item.getIdentifier() != 358) { // Map
                if (tag == null) item.setTag(tag = new CompoundTag("tag"));
                tag.put(new IntTag(NBT_TAG_NAME, originalId)); // Data will be lost, saving original id
            }

            if (item.getIdentifier() == 229) { // purple shulker box
                newId = 362; // directly set the new id -> base/colorless shulker box
            } else if (item.getIdentifier() == 31 && item.getData() == 0) { // Shrub was removed
                rawId = 32 << 4; // Dead Bush
            } else if (MappingData.oldToNewItems.containsKey(rawId & ~0xF)) {
                rawId &= ~0xF; // Remove data
            } else {
                if (!Via.getConfig().isSuppress1_13ConversionErrors() || Via.getManager().isDebug()) {
                    ViaBackwards.getPlatform().getLogger().warning("Failed to get 1.13 item for " + item.getIdentifier());
                }
                rawId = 16; // Stone
            }
        }

        if (newId == -1) {
            newId = MappingData.oldToNewItems.get(rawId);
        }

        item.setIdentifier(newId);
        item.setData((short) 0);
        item = super.handleItemToServer(item);
        return item;
    }

    private void rewriteEnchantmentsToServer(CompoundTag tag, boolean storedEnch) {
        String key = storedEnch ? "StoredEnchantments" : "Enchantments";
        ListTag enchantments = tag.get(storedEnch ? key : "ench");
        ListTag newEnchantments = new ListTag(key, CompoundTag.class);

        boolean dummyEnchant = false;
        if (!storedEnch) {
            IntTag hideFlags = tag.get(NBT_TAG_NAME + "|OldHideFlags");
            if (hideFlags != null) {
                tag.put(new IntTag("HideFlags", hideFlags.getValue()));
                dummyEnchant = true;
                tag.remove(NBT_TAG_NAME + "|OldHideFlags");
            } else if (tag.contains(NBT_TAG_NAME + "|DummyEnchant")) {
                tag.remove("HideFlags");
                dummyEnchant = true;
                tag.remove(NBT_TAG_NAME + "|DummyEnchant");
            }
        }

        for (Tag enchEntry : enchantments) {
            CompoundTag enchantmentEntry = new CompoundTag("");
            short oldId = ((Number) ((CompoundTag) enchEntry).get("id").getValue()).shortValue();
            short level = ((Number) ((CompoundTag) enchEntry).get("lvl").getValue()).shortValue();
            if (dummyEnchant && oldId == 0 && level == 0) {
                continue; //Skip dummy enchatment
            }
            String newId = MappingData.oldEnchantmentsIds.get(oldId);
            if (newId == null) {
                newId = "viaversion:legacy/" + oldId;
            }
            enchantmentEntry.put(new StringTag("id", newId));

            enchantmentEntry.put(new ShortTag("lvl", level));
            newEnchantments.add(enchantmentEntry);
        }

        ListTag noMapped = tag.get(NBT_TAG_NAME + "|Enchantments");
        if (noMapped != null) {
            for (Tag value : noMapped) {
                newEnchantments.add(value);
            }
            tag.remove(NBT_TAG_NAME + "|Enchantments");
        }

        CompoundTag display = tag.get("display");
        if (display == null) {
            tag.put(display = new CompoundTag("display"));
        }

        ListTag oldLore = tag.get(NBT_TAG_NAME + "|OldLore");
        if (oldLore != null) {
            ListTag lore = display.get("Lore");
            if (lore == null) {
                tag.put(lore = new ListTag("Lore"));
            }
            lore.setValue(oldLore.getValue());
            tag.remove(NBT_TAG_NAME + "|OldLore");
        } else if (tag.contains(NBT_TAG_NAME + "|DummyLore")) {
            display.remove("Lore");
            if (display.isEmpty())
                tag.remove("display");
            tag.remove(NBT_TAG_NAME + "|DummyLore");
        }

        if (!storedEnch)
            tag.remove("ench");
        tag.put(newEnchantments);
    }

    // TODO find a less hacky way to do this (https://bugs.mojang.com/browse/MC-74231)
    private static void flowerPotSpecialTreatment(UserConnection user, int blockState, Position position) throws Exception {
        if (FlowerPotHandler.isFlowah(blockState)) {
            BackwardsBlockEntityProvider beProvider = Via.getManager().getProviders().get(BackwardsBlockEntityProvider.class);

            CompoundTag nbt = beProvider.transform(user, position, "minecraft:flower_pot");

            // Remove the flowerpot
            PacketWrapper blockUpdateRemove = new PacketWrapper(0x0B, null, user);
            blockUpdateRemove.write(Type.POSITION, position);
            blockUpdateRemove.write(Type.VAR_INT, 0);
            blockUpdateRemove.send(Protocol1_12_2To1_13.class, true);

            // Create the flowerpot
            PacketWrapper blockCreate = new PacketWrapper(0x0B, null, user);
            blockCreate.write(Type.POSITION, position);
            blockCreate.write(Type.VAR_INT, toOldId(blockState));
            blockCreate.send(Protocol1_12_2To1_13.class, true);

            // Send a block entity update
            PacketWrapper wrapper = new PacketWrapper(0x09, null, user);
            wrapper.write(Type.POSITION, position);
            wrapper.write(Type.UNSIGNED_BYTE, (short) 5);
            wrapper.write(Type.NBT, nbt);
            wrapper.send(Protocol1_12_2To1_13.class, true);

        }
    }

    public static String getRomanNumber(int number) {
        switch (number) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            case 6:
                return "VI";
            case 7:
                return "VII";
            case 8:
                return "VIII";
            case 9:
                return "IX";
            case 10:
                return "X";
            default:
                return Integer.toString(number);
        }
    }
}
