package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.packets;

import com.google.common.collect.ImmutableSet;
import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.BlockItemRewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets.BlockItemPackets1_13;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.storage.ChunkLightStorage;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.entities.Entity1_14Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.BlockChangeRecord;
import us.myles.ViaVersion.api.minecraft.Environment;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_13_2;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_13;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.types.Chunk1_13Type;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.Protocol1_14To1_13_2;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.types.Chunk1_14Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.viaversion.libs.opennbt.conversion.ConverterRegistry;
import us.myles.viaversion.libs.opennbt.tag.builtin.*;

import java.util.*;

public class BlockItemPackets1_14 extends BlockItemRewriter<Protocol1_13_2To1_14> {

    private static final String NBT_TAG_NAME = "ViaBackwards|" + Protocol1_13_2To1_14.class.getSimpleName();
    private final Map<String, String> enchantmentMappings = new HashMap<>();

    @Override
    protected void registerPackets(Protocol1_13_2To1_14 protocol) {
        // Open window
        protocol.registerOutgoing(State.PLAY, 0x2E, 0x14, new PacketRemapper() {
            @Override
            public void registerMap() { // c
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.read(Type.VAR_INT);
                        wrapper.write(Type.UNSIGNED_BYTE, (short) id);
                        int type = wrapper.read(Type.VAR_INT);
                        String stringType = null;
                        String title = null;
                        int slotSize = 0;
                        if (type < 6) {
                            if (type == 2) title = "Barrel";
                            stringType = "minecraft:container";
                            slotSize = (type + 1) * 9;
                        } else
                            switch (type) {
                                case 11:
                                    stringType = "minecraft:crafting_table";
                                    break;
                                case 9: //blast furnace
                                case 20: //smoker
                                case 13: //furnace
                                case 14: //grindstone
                                    if (type == 9) title = "Blast Furnace";
                                    if (type == 20) title = "Smoker";
                                    if (type == 14) title = "Grindstone";
                                    stringType = "minecraft:furnace";
                                    slotSize = 3;
                                    break;
                                case 6:
                                    stringType = "minecraft:dropper";
                                    slotSize = 9;
                                    break;
                                case 12:
                                    stringType = "minecraft:enchanting_table";
                                    break;
                                case 10:
                                    stringType = "minecraft:brewing_stand";
                                    slotSize = 5;
                                    break;
                                case 18:
                                    stringType = "minecraft:villager";
                                    break;
                                case 8:
                                    stringType = "minecraft:beacon";
                                    slotSize = 1;
                                    break;
                                case 21: //cartography_table
                                case 7:
                                    if (type == 21) title = "Cartography Table";
                                    stringType = "minecraft:anvil";
                                    break;
                                case 15:
                                    stringType = "minecraft:hopper";
                                    slotSize = 5;
                                    break;
                                case 19:
                                    stringType = "minecraft:shulker_box";
                                    slotSize = 27;
                                    break;
                            }

                        if (stringType == null) {
                            ViaBackwards.getPlatform().getLogger().warning("Can't open inventory for 1.13 player! Type: " + type);
                            wrapper.cancel();
                            return;
                        }

                        wrapper.write(Type.STRING, stringType);
                        String t = wrapper.read(Type.STRING);
                        if (title != null) t = ChatRewriter.legacyTextToJson(title);
                        wrapper.write(Type.STRING, t);
                        wrapper.write(Type.UNSIGNED_BYTE, (short) slotSize);
                    }
                });
            }
        });

        // Horse window
        protocol.registerOutgoing(State.PLAY, 0x1F, 0x14, new PacketRemapper() {
            @Override
            public void registerMap() { // c
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.passthrough(Type.UNSIGNED_BYTE); // Window id
                        wrapper.write(Type.STRING, "EntityHorse"); // Type
                        wrapper.write(Type.STRING, "{\"translate\":\"minecraft.horse\"}"); // Title
                        wrapper.write(Type.UNSIGNED_BYTE, wrapper.read(Type.VAR_INT).shortValue()); // Number of slots
                        wrapper.passthrough(Type.INT); // Entity id
                    }
                });
            }
        });

        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);

        // Window items packet
        itemRewriter.registerWindowItems(Type.FLAT_VAR_INT_ITEM_ARRAY, 0x14, 0x15);

        // Set slot packet
        itemRewriter.registerSetSlot(Type.FLAT_VAR_INT_ITEM, 0x16, 0x17);

        // Trade list
        protocol.out(State.PLAY, 0x27, 0x19, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.write(Type.STRING, "minecraft:trader_list");

                        int windowId = wrapper.read(Type.VAR_INT);
                        wrapper.write(Type.INT, windowId);

                        int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                        for (int i = 0; i < size; i++) {
                            // Input Item
                            Item input = wrapper.read(Type.FLAT_VAR_INT_ITEM);
                            input = handleItemToClient(input);
                            wrapper.write(Type.FLAT_VAR_INT_ITEM, input);


                            // Output Item
                            Item output = wrapper.read(Type.FLAT_VAR_INT_ITEM);
                            output = handleItemToClient(output);
                            wrapper.write(Type.FLAT_VAR_INT_ITEM, output);

                            boolean secondItem = wrapper.passthrough(Type.BOOLEAN); // Has second item
                            if (secondItem) {
                                // Second Item
                                Item second = wrapper.read(Type.FLAT_VAR_INT_ITEM);
                                second = handleItemToClient(second);
                                wrapper.write(Type.FLAT_VAR_INT_ITEM, second);
                            }

                            wrapper.passthrough(Type.BOOLEAN); // Trade disabled
                            wrapper.passthrough(Type.INT); // Number of tools uses
                            wrapper.passthrough(Type.INT); // Maximum number of trade uses

                            wrapper.read(Type.INT);
                            wrapper.read(Type.INT);
                            wrapper.read(Type.FLOAT);
                        }
                        wrapper.read(Type.VAR_INT);
                        wrapper.read(Type.VAR_INT);
                        wrapper.read(Type.BOOLEAN);
                    }
                });
            }
        });

        // Book open
        protocol.registerOutgoing(State.PLAY, 0x2D, 0x19, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.write(Type.STRING, "minecraft:book_open");
                        wrapper.passthrough(Type.VAR_INT);
                    }
                });
            }
        });

        // Entity Equipment Packet
        protocol.registerOutgoing(State.PLAY, 0x46, 0x42, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.VAR_INT); // 1 - Slot ID
                map(Type.FLAT_VAR_INT_ITEM); // 2 - Item

                handler(itemRewriter.itemToClientHandler(Type.FLAT_VAR_INT_ITEM));

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);
                        EntityType entityType = wrapper.user().get(EntityTracker.class).get(getProtocol()).getEntityType(entityId);
                        if (entityType == null) return; // TODO: Check why there might (?) be an untracked entity

                        if (entityType.isOrHasParent(Entity1_14Types.EntityType.ABSTRACT_HORSE)) {
                            wrapper.setId(0x3F);
                            wrapper.resetReader();
                            wrapper.passthrough(Type.VAR_INT);
                            wrapper.read(Type.VAR_INT);
                            Item item = wrapper.read(Type.FLAT_VAR_INT_ITEM);
                            int armorType = item == null || item.getIdentifier() == 0 ? 0 : item.getIdentifier() - 726;
                            if (armorType < 0 || armorType > 3) {
                                ViaBackwards.getPlatform().getLogger().warning("Received invalid horse armor: " + item);
                                wrapper.cancel();
                                return;
                            }
                            List<Metadata> metadataList = new ArrayList<>();
                            metadataList.add(new Metadata(16, MetaType1_13_2.VarInt, armorType));
                            wrapper.write(Types1_13.METADATA_LIST, metadataList);
                        }
                    }
                });
            }
        });

        Set<String> removedTypes = ImmutableSet.of("crafting_special_suspiciousstew", "blasting", "smoking", "campfire_cooking", "stonecutting");

        // Declare Recipes
        protocol.registerOutgoing(State.PLAY, 0x5A, 0x54, new PacketRemapper() { // c
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int size = wrapper.passthrough(Type.VAR_INT);
                        int deleted = 0;
                        for (int i = 0; i < size; i++) {
                            String type = wrapper.read(Type.STRING);
                            String id = wrapper.read(Type.STRING); // Recipe Identifier
                            type = type.replace("minecraft:", "");
                            if (removedTypes.contains(type)) {
                                switch (type) {
                                    case "blasting":
                                    case "smoking":
                                    case "campfire_cooking":
                                        wrapper.read(Type.STRING); // Group
                                        wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                        wrapper.read(Type.FLAT_VAR_INT_ITEM);
                                        wrapper.read(Type.FLOAT); // EXP
                                        wrapper.read(Type.VAR_INT); // Cooking time
                                        break;
                                    case "stonecutting":
                                        wrapper.read(Type.STRING); // Group?
                                        wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                        wrapper.read(Type.FLAT_VAR_INT_ITEM); // Result
                                        break;
                                }
                                deleted++;
                                continue;
                            }
                            wrapper.write(Type.STRING, id);
                            wrapper.write(Type.STRING, type);

                            if (type.equals("crafting_shapeless")) {
                                wrapper.passthrough(Type.STRING); // Group
                                int ingredientsNo = wrapper.passthrough(Type.VAR_INT);
                                for (int j = 0; j < ingredientsNo; j++) {
                                    Item[] items = wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                    for (int k = 0; k < items.length; k++) {
                                        items[k] = handleItemToClient(items[k]);
                                    }
                                    wrapper.write(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT, items);
                                }
                                Item result = handleItemToClient(wrapper.read(Type.FLAT_VAR_INT_ITEM));// Result
                                wrapper.write(Type.FLAT_VAR_INT_ITEM, result);
                            } else if (type.equals("crafting_shaped")) {
                                int ingredientsNo = wrapper.passthrough(Type.VAR_INT) * wrapper.passthrough(Type.VAR_INT);
                                wrapper.passthrough(Type.STRING); // Group
                                for (int j = 0; j < ingredientsNo; j++) {
                                    Item[] items = wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                    for (int k = 0; k < items.length; k++) {
                                        items[k] = handleItemToClient(items[k]);
                                    }
                                    wrapper.write(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT, items);
                                }
                                Item result = handleItemToClient(wrapper.read(Type.FLAT_VAR_INT_ITEM));// Result
                                wrapper.write(Type.FLAT_VAR_INT_ITEM, result);
                            } else if (type.equals("smelting")) {
                                wrapper.passthrough(Type.STRING); // Group
                                Item[] items = wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                for (int k = 0; k < items.length; k++) {
                                    items[k] = handleItemToClient(items[k]);
                                }
                                wrapper.write(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT, items);

                                Item result = handleItemToClient(wrapper.read(Type.FLAT_VAR_INT_ITEM));// Result
                                wrapper.write(Type.FLAT_VAR_INT_ITEM, result);

                                wrapper.passthrough(Type.FLOAT); // EXP
                                wrapper.passthrough(Type.VAR_INT); // Cooking time
                            }
                        }
                        wrapper.set(Type.VAR_INT, 0, size - deleted);
                    }
                });
            }
        });


        /*
            Incoming packets
         */

        // Click window packet
        itemRewriter.registerClickWindow(Type.FLAT_VAR_INT_ITEM, 0x09, 0x08);

        // Creative Inventory Action
        itemRewriter.registerCreativeInvAction(Type.FLAT_VAR_INT_ITEM, 0x26, 0x24);

        // Block break animation
        protocol.registerOutgoing(State.PLAY, 0x08, 0x08, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.POSITION1_14, Type.POSITION);
                map(Type.BYTE);
            }
        });

        // Update block entity
        protocol.registerOutgoing(State.PLAY, 0x09, 0x09, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14, Type.POSITION);
            }
        });

        // Block Action
        protocol.registerOutgoing(State.PLAY, 0x0A, 0x0A, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14, Type.POSITION); // Location
                map(Type.UNSIGNED_BYTE); // Action id
                map(Type.UNSIGNED_BYTE); // Action param
                map(Type.VAR_INT); // Block id - /!\ NOT BLOCK STATE
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(Type.VAR_INT, 0, Protocol1_13_2To1_14.getNewBlockStateId(wrapper.get(Type.VAR_INT, 0))); //TODO proper block id
                    }
                });
            }
        });

        // Block Change
        protocol.registerOutgoing(State.PLAY, 0xB, 0xB, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14, Type.POSITION);
                map(Type.VAR_INT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.VAR_INT, 0);

                        wrapper.set(Type.VAR_INT, 0, Protocol1_13_2To1_14.getNewBlockStateId(id));
                    }
                });
            }
        });

        // Multi Block Change
        protocol.registerOutgoing(State.PLAY, 0xF, 0xF, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Chunk X
                map(Type.INT); // 1 - Chunk Z
                map(Type.BLOCK_CHANGE_RECORD_ARRAY); // 2 - Records
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        // Convert ids
                        for (BlockChangeRecord record : wrapper.get(Type.BLOCK_CHANGE_RECORD_ARRAY, 0)) {
                            int id = record.getBlockId();
                            record.setBlockId(Protocol1_13_2To1_14.getNewBlockStateId(id));
                        }
                    }
                });
            }
        });

        //Explosion
        protocol.registerOutgoing(State.PLAY, 0x1C, 0x1E, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.FLOAT); // X
                map(Type.FLOAT); // Y
                map(Type.FLOAT); // Z
                map(Type.FLOAT); // Radius
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        for (int i = 0; i < 3; i++) {
                            float coord = wrapper.get(Type.FLOAT, i);

                            if (coord < 0f) {
                                coord = (float) Math.floor(coord);
                                wrapper.set(Type.FLOAT, i, coord);
                            }
                        }
                    }
                });
            }
        });

        //Chunk
        protocol.registerOutgoing(State.PLAY, 0x21, 0x22, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        Chunk chunk = wrapper.read(new Chunk1_14Type(clientWorld));
                        wrapper.write(new Chunk1_13Type(clientWorld), chunk);

                        ChunkLightStorage.ChunkLight chunkLight = wrapper.user().get(ChunkLightStorage.class).getStoredLight(chunk.getX(), chunk.getZ());
                        for (int i = 0; i < chunk.getSections().length; i++) {
                            ChunkSection section = chunk.getSections()[i];
                            if (section == null) continue;

                            if (chunkLight == null) {
                                section.setBlockLight(ChunkLightStorage.FULL_LIGHT);
                                if (clientWorld.getEnvironment() == Environment.NORMAL) {
                                    section.setSkyLight(ChunkLightStorage.FULL_LIGHT);
                                }
                            } else {
                                final byte[] blockLight = chunkLight.getBlockLight()[i];
                                section.setBlockLight(blockLight != null ? blockLight : ChunkLightStorage.FULL_LIGHT);
                                if (clientWorld.getEnvironment() == Environment.NORMAL) {
                                    final byte[] skyLight = chunkLight.getSkyLight()[i];
                                    section.setSkyLight(skyLight != null ? skyLight : ChunkLightStorage.FULL_LIGHT);
                                }
                            }

                            if (Via.getConfig().isNonFullBlockLightFix() && section.getNonAirBlocksCount() != 0 && section.hasBlockLight()) {
                                for (int x = 0; x < 16; x++) {
                                    for (int y = 0; y < 16; y++) {
                                        for (int z = 0; z < 16; z++) {
                                            int id = section.getFlatBlock(x, y, z);
                                            if (MappingData.nonFullBlocks.contains(id)) {
                                                section.getBlockLightNibbleArray().set(x, y, z, 0);
                                            }
                                        }
                                    }
                                }
                            }

                            for (int j = 0; j < section.getPaletteSize(); j++) {
                                int old = section.getPaletteEntry(j);
                                int newId = Protocol1_13_2To1_14.getNewBlockStateId(old);
                                section.setPaletteEntry(j, newId);
                            }
                        }
                    }
                });
            }
        });

        // Unload chunk
        protocol.registerOutgoing(State.PLAY, 0x1D, 0x1F, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int x = wrapper.passthrough(Type.INT);
                        int z = wrapper.passthrough(Type.INT);
                        wrapper.user().get(ChunkLightStorage.class).unloadChunk(x, z);
                    }
                });
            }
        });

        // Effect packet
        protocol.registerOutgoing(State.PLAY, 0x22, 0x23, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Effect Id
                map(Type.POSITION1_14, Type.POSITION); // Location
                map(Type.INT); // Data
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.INT, 0);
                        int data = wrapper.get(Type.INT, 1);
                        if (id == 1010) { // Play record
                            wrapper.set(Type.INT, 1, data = BlockItemPackets1_14.getNewItemId(data));
                        } else if (id == 2001) { // Block break + block break sound
                            wrapper.set(Type.INT, 1, data = Protocol1_14To1_13_2.getNewBlockStateId(data));
                        }
                    }
                });
            }
        });

        //spawn particle
        protocol.registerOutgoing(State.PLAY, 0x23, 0x24, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Particle ID
                map(Type.BOOLEAN); // 1 - Long Distance
                map(Type.FLOAT); // 2 - X
                map(Type.FLOAT); // 3 - Y
                map(Type.FLOAT); // 4 - Z
                map(Type.FLOAT); // 5 - Offset X
                map(Type.FLOAT); // 6 - Offset Y
                map(Type.FLOAT); // 7 - Offset Z
                map(Type.FLOAT); // 8 - Particle Data
                map(Type.INT); // 9 - Particle Count
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.INT, 0);
                        id = EntityPackets1_14.getOldParticleId(id);
                        if (id == 3 || id == 20) {
                            int data = wrapper.passthrough(Type.VAR_INT);
                            wrapper.set(Type.VAR_INT, 0, Protocol1_13_2To1_14.getNewBlockStateId(data));
                        } else if (id == 27) {
                            Item item = handleItemToClient(wrapper.read(Type.FLAT_VAR_INT_ITEM));
                            wrapper.write(Type.FLAT_VAR_INT_ITEM, item);
                        }
                        wrapper.set(Type.INT, 0, id);
                    }
                });
            }
        });


        //Map Data
        protocol.registerOutgoing(State.PLAY, 0x26, 0x26, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.BYTE);
                map(Type.BOOLEAN);
                map(Type.BOOLEAN, Type.NOTHING); // Locked
            }
        });

        // Spawn position
        protocol.registerOutgoing(State.PLAY, 0x4D, 0x49, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14, Type.POSITION);
            }
        });
    }

    @Override
    protected void registerRewrites() {
        rewrite(247).repItem(new Item((short) 245, (byte) 1, (short) -1, getNamedTag("1.14 Brick Wall")));
        rewrite(248).repItem(new Item((short) 245, (byte) 1, (short) -1, getNamedTag("1.14 Prismarine Wall")));
        rewrite(249).repItem(new Item((short) 245, (byte) 1, (short) -1, getNamedTag("1.14 Red Sandstone Wall")));
        rewrite(250).repItem(new Item((short) 246, (byte) 1, (short) -1, getNamedTag("1.14 Mossy Stone Brick Wall")));
        rewrite(251).repItem(new Item((short) 245, (byte) 1, (short) -1, getNamedTag("1.14 Granite Wall")));
        rewrite(252).repItem(new Item((short) 245, (byte) 1, (short) -1, getNamedTag("1.14 Stone Brick Wall")));
        rewrite(253).repItem(new Item((short) 245, (byte) 1, (short) -1, getNamedTag("1.14 Nether Brick Wall")));
        rewrite(254).repItem(new Item((short) 245, (byte) 1, (short) -1, getNamedTag("1.14 Andesite Wall")));
        rewrite(255).repItem(new Item((short) 245, (byte) 1, (short) -1, getNamedTag("1.14 Red Nether Brick Wall")));
        rewrite(256).repItem(new Item((short) 245, (byte) 1, (short) -1, getNamedTag("1.14 Sandstone Wall")));
        rewrite(257).repItem(new Item((short) 245, (byte) 1, (short) -1, getNamedTag("1.14 End Stone Brick Wall")));
        rewrite(258).repItem(new Item((short) 245, (byte) 1, (short) -1, getNamedTag("1.14 Diorite Wall")));

        rewrite(121).repItem(new Item((short) 126, (byte) 1, (short) -1, getNamedTag("1.14 Stone Slab")));
        rewrite(124).repItem(new Item((short) 123, (byte) 1, (short) -1, getNamedTag("1.14 Cut Sandstone Slab")));
        rewrite(132).repItem(new Item((short) 131, (byte) 1, (short) -1, getNamedTag("1.14 Cut Red Sandstone Slab")));
        rewrite(492).repItem(new Item((short) 126, (byte) 1, (short) -1, getNamedTag("1.14 Polished Granite Slab")));
        rewrite(493).repItem(new Item((short) 131, (byte) 1, (short) -1, getNamedTag("1.14 Smooth Red Sandstone Slab")));
        rewrite(494).repItem(new Item((short) 126, (byte) 1, (short) -1, getNamedTag("1.14 Mossy Stone Brick Slab")));
        rewrite(495).repItem(new Item((short) 126, (byte) 1, (short) -1, getNamedTag("1.14 Polished Diorite Slab")));
        rewrite(496).repItem(new Item((short) 126, (byte) 1, (short) -1, getNamedTag("1.14 Mossy Cobblestone Slab")));
        rewrite(497).repItem(new Item((short) 123, (byte) 1, (short) -1, getNamedTag("1.14 End Stone Brick Slab")));
        rewrite(498).repItem(new Item((short) 123, (byte) 1, (short) -1, getNamedTag("1.14 Smooth Cut Sandstone Slab")));
        rewrite(499).repItem(new Item((short) 130, (byte) 1, (short) -1, getNamedTag("1.14 Smooth Quartz Slab")));
        rewrite(500).repItem(new Item((short) 126, (byte) 1, (short) -1, getNamedTag("1.14 Granite Slab")));
        rewrite(501).repItem(new Item((short) 126, (byte) 1, (short) -1, getNamedTag("1.14 Andesite Slab")));
        rewrite(502).repItem(new Item((short) 129, (byte) 1, (short) -1, getNamedTag("1.14 Red Nether Brick Slab")));
        rewrite(503).repItem(new Item((short) 126, (byte) 1, (short) -1, getNamedTag("1.14 Polished Andesite Slab")));
        rewrite(504).repItem(new Item((short) 126, (byte) 1, (short) -1, getNamedTag("1.14 Diorite Slab")));

        rewrite(478).repItem(new Item((short) 163, (byte) 1, (short) -1, getNamedTag("1.14 Polished Granite Stairs")));
        rewrite(479).repItem(new Item((short) 371, (byte) 1, (short) -1, getNamedTag("1.14 Smooth Red Sandstone Stairs")));
        rewrite(480).repItem(new Item((short) 163, (byte) 1, (short) -1, getNamedTag("1.14 Mossy Stone Brick Stairs")));
        rewrite(481).repItem(new Item((short) 163, (byte) 1, (short) -1, getNamedTag("1.14 Polished Diorite Stairs")));
        rewrite(482).repItem(new Item((short) 163, (byte) 1, (short) -1, getNamedTag("1.14 Mossy Cobblestone Stairs")));
        rewrite(483).repItem(new Item((short) 235, (byte) 1, (short) -1, getNamedTag("1.14 End Stone Brick Stairs")));
        rewrite(484).repItem(new Item((short) 163, (byte) 1, (short) -1, getNamedTag("1.14 Stone Stairs")));
        rewrite(485).repItem(new Item((short) 235, (byte) 1, (short) -1, getNamedTag("1.14 Smooth Sandstone Stairs")));
        rewrite(486).repItem(new Item((short) 278, (byte) 1, (short) -1, getNamedTag("1.14 Smooth Quartz Stairs")));
        rewrite(487).repItem(new Item((short) 163, (byte) 1, (short) -1, getNamedTag("1.14 Granite Stairs")));
        rewrite(488).repItem(new Item((short) 163, (byte) 1, (short) -1, getNamedTag("1.14 Andesite Stairs")));
        rewrite(489).repItem(new Item((short) 228, (byte) 1, (short) -1, getNamedTag("1.14 Red Nether Brick Stairs")));
        rewrite(490).repItem(new Item((short) 163, (byte) 1, (short) -1, getNamedTag("1.14 Polished Andesite Stairs")));
        rewrite(491).repItem(new Item((short) 163, (byte) 1, (short) -1, getNamedTag("1.14 Diorite Stairs")));

        rewrite(108).repItem(new Item((short) 111, (byte) 1, (short) -1, getNamedTag("1.14 Cornflower")));
        rewrite(109).repItem(new Item((short) 105, (byte) 1, (short) -1, getNamedTag("1.14 Lily of the Valley")));
        rewrite(110).repItem(new Item((short) 100, (byte) 1, (short) -1, getNamedTag("1.14 Wither Rose")));

        rewrite(614).repItem(new Item((short) 611, (byte) 1, (short) -1, getNamedTag("1.14 Bamboo")));
        rewrite(857).repItem(new Item((short) 547, (byte) 1, (short) -1, getNamedTag("1.14 Suspicious Stew")));
        rewrite(795).repItem(new Item((short) 793, (byte) 1, (short) -1, getNamedTag("1.14 Leather Horse Armor")));

        rewrite(647).repItem(new Item((short) 635, (byte) 1, (short) -1, getNamedTag("1.14 Blue Dye")));
        rewrite(648).repItem(new Item((short) 634, (byte) 1, (short) -1, getNamedTag("1.14 Brown Dye")));
        rewrite(649).repItem(new Item((short) 631, (byte) 1, (short) -1, getNamedTag("1.14 Black Dye")));
        rewrite(650).repItem(new Item((short) 646, (byte) 1, (short) -1, getNamedTag("1.14 White Dye")));

        rewrite(505).repItem(new Item((short) 299, (byte) 1, (short) -1, getNamedTag("1.14 Scaffolding")));
        rewrite(516).repItem(new Item((short) 515, (byte) 1, (short) -1, getNamedTag("1.14 Jigsaw Block")));
        rewrite(517).repItem(new Item((short) 694, (byte) 1, (short) -1, getNamedTag("1.14 Composter")));

        rewrite(864).repItem(new Item((short) 155, (byte) 1, (short) -1, getNamedTag("1.14 Barrel")));
        rewrite(858).repItem(new Item((short) 158, (byte) 1, (short) -1, getNamedTag("1.14 Loom")));
        rewrite(865).repItem(new Item((short) 160, (byte) 1, (short) -1, getNamedTag("1.14 Smoker")));
        rewrite(866).repItem(new Item((short) 160, (byte) 1, (short) -1, getNamedTag("1.14 Blast Furnace")));
        rewrite(867).repItem(new Item((short) 158, (byte) 1, (short) -1, getNamedTag("1.14 Cartography Table")));
        rewrite(868).repItem(new Item((short) 158, (byte) 1, (short) -1, getNamedTag("1.14 Fletching Table")));
        rewrite(869).repItem(new Item((short) 265, (byte) 1, (short) -1, getNamedTag("1.14 Grindstone")));
        rewrite(870).repItem(new Item((short) 143, (byte) 1, (short) -1, getNamedTag("1.14 Lectern")));
        rewrite(871).repItem(new Item((short) 158, (byte) 1, (short) -1, getNamedTag("1.14 Smithing Table")));
        rewrite(872).repItem(new Item((short) 158, (byte) 1, (short) -1, getNamedTag("1.14 Stonecutter")));

        rewrite(859).repItem(new Item((short) 615, (byte) 1, (short) -1, getNamedTag("1.14 Flower Banner Pattern")));
        rewrite(860).repItem(new Item((short) 615, (byte) 1, (short) -1, getNamedTag("1.14 Creeper Banner Pattern")));
        rewrite(861).repItem(new Item((short) 615, (byte) 1, (short) -1, getNamedTag("1.14 Skull Banner Pattern")));
        rewrite(862).repItem(new Item((short) 615, (byte) 1, (short) -1, getNamedTag("1.14 Mojang Banner Pattern")));
        rewrite(863).repItem(new Item((short) 615, (byte) 1, (short) -1, getNamedTag("1.14 Globe Banner Pattern")));

        rewrite(873).repItem(new Item((short) 113, (byte) 1, (short) -1, getNamedTag("1.14 Bell")));
        rewrite(874).repItem(new Item((short) 234, (byte) 1, (short) -1, getNamedTag("1.14 Lantern")));
        rewrite(875).repItem(new Item((short) 820, (byte) 1, (short) -1, getNamedTag("1.14 Sweet Berries")));
        rewrite(876).repItem(new Item((short) 146, (byte) 1, (short) -1, getNamedTag("1.14 Campfire")));

        rewrite(590).repItem(new Item((short) 589, (byte) 1, (short) -1, getNamedTag("1.14 Spruce Sign")));
        rewrite(591).repItem(new Item((short) 589, (byte) 1, (short) -1, getNamedTag("1.14 Birch Sign")));
        rewrite(592).repItem(new Item((short) 589, (byte) 1, (short) -1, getNamedTag("1.14 Jungle Sign")));
        rewrite(593).repItem(new Item((short) 589, (byte) 1, (short) -1, getNamedTag("1.14 Acacia Sign")));
        rewrite(594).repItem(new Item((short) 589, (byte) 1, (short) -1, getNamedTag("1.14 Dark Oak Sign")));

        rewrite(856).repItem(new Item((short) 525, (byte) 1, (short) -1, getNamedTag("1.14 Crossbow")));

        rewrite(699).repItem(new Item((short) 721, (byte) 1, (short) -1, getNamedTag("1.14 Cat Spawn Egg")));
        rewrite(712).repItem(new Item((short) 725, (byte) 1, (short) -1, getNamedTag("1.14 Fox Spawn Egg")));
        rewrite(722).repItem(new Item((short) 735, (byte) 1, (short) -1, getNamedTag("1.14 Panda Spawn Egg")));
        rewrite(726).repItem(new Item((short) 754, (byte) 1, (short) -1, getNamedTag("1.14 Pillager Spawn Egg")));
        rewrite(730).repItem(new Item((short) 734, (byte) 1, (short) -1, getNamedTag("1.14 Ravager Spawn Egg")));
        rewrite(741).repItem(new Item((short) 698, (byte) 1, (short) -1, getNamedTag("1.14 Trader Llama Spawn Egg")));
        rewrite(747).repItem(new Item((short) 739, (byte) 1, (short) -1, getNamedTag("1.14 Wandering Trader Spawn Egg")));

        enchantmentMappings.put("minecraft:multishot", "§7Multishot");
        enchantmentMappings.put("minecraft:quick_charge", "§7Quick Charge");
        enchantmentMappings.put("minecraft:piercing", "§7Piercing");
    }

    @Override
    public Item handleItemToClient(Item i) {
        Item item = super.handleItemToClient(i);
        if (item == null) return null;
        item.setIdentifier(getOldItemId(item.getIdentifier()));

        CompoundTag tag;
        if ((tag = item.getTag()) != null) {
            // Display Name now uses JSON
            if (tag.get("display") instanceof CompoundTag) {
                CompoundTag display = tag.get("display");
                if (((CompoundTag) tag.get("display")).get("Lore") instanceof ListTag) {
                    ListTag lore = display.get("Lore");
                    ListTag via = display.get(NBT_TAG_NAME + "|Lore");
                    if (via != null) {
                        display.put(ConverterRegistry.convertToTag("Lore", ConverterRegistry.convertToValue(via)));
                    } else {
                        for (Tag loreEntry : lore) {
                            if (loreEntry instanceof StringTag) {
                                ((StringTag) loreEntry).setValue(
                                        ChatRewriter.jsonTextToLegacy(
                                                ((StringTag) loreEntry).getValue()
                                        )
                                );
                            }
                        }
                    }
                    display.remove(NBT_TAG_NAME + "|Lore");
                }
            }

            if (tag.get("Enchantments") instanceof ListTag) {
                rewriteEnchantmentsToClient(tag, false);
            }
            if (tag.get("StoredEnchantments") instanceof ListTag) {
                rewriteEnchantmentsToClient(tag, true);
            }
        }
        return item;
    }

    private void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "Enchantments";
        ListTag enchantments = tag.get(key);
        ListTag noMapped = new ListTag(NBT_TAG_NAME + "|" + key, CompoundTag.class);
        List<Tag> lore = new ArrayList<>();
        for (Tag enchantmentEntry : enchantments.clone()) {
            String newId = (String) ((CompoundTag) enchantmentEntry).get("id").getValue();
            String enchantmentName = enchantmentMappings.get(newId);
            if (enchantmentName != null) {
                enchantments.remove(enchantmentEntry);
                lore.add(new StringTag("", enchantmentMappings.get(newId) + " " + BlockItemPackets1_13.getRomanNumber((Short) ((CompoundTag) enchantmentEntry).get("lvl").getValue())));
                noMapped.add(enchantmentEntry);
            }
        }
        if (!lore.isEmpty()) {
            if (!storedEnchant && enchantments.size() == 0) {
                CompoundTag dummyEnchantment = new CompoundTag("");
                dummyEnchantment.put(new StringTag("id", ""));
                dummyEnchantment.put(new ShortTag("lvl", (short) 0));
                enchantments.add(dummyEnchantment);

                tag.put(new ByteTag(NBT_TAG_NAME + "|dummyEnchant"));
            }

            tag.put(noMapped);

            CompoundTag display = tag.get("display");
            if (display == null) {
                tag.put(display = new CompoundTag("display"));
            }
            ListTag loreTag = display.get("Lore");
            if (loreTag == null) {
                display.put(loreTag = new ListTag("Lore", StringTag.class));
            }

            lore.addAll(loreTag.getValue());
            loreTag.setValue(lore);
        }
    }

    @Override
    public Item handleItemToServer(Item item) {
        if (item == null) return null;
        item.setIdentifier(getNewItemId(item.getIdentifier()));
        item = super.handleItemToServer(item);

        CompoundTag tag;
        if ((tag = item.getTag()) != null) {
            // Display Lore now uses JSON
            if (tag.get("display") instanceof CompoundTag) {
                CompoundTag display = tag.get("display");
                if (display.get("Lore") instanceof ListTag) {
                    ListTag lore = display.get("Lore");
                    display.put(ConverterRegistry.convertToTag(NBT_TAG_NAME + "|Lore", ConverterRegistry.convertToValue(lore)));
                    for (Tag loreEntry : lore) {
                        if (loreEntry instanceof StringTag) {
                            ((StringTag) loreEntry).setValue(
                                    ChatRewriter.legacyTextToJson(
                                            ((StringTag) loreEntry).getValue()
                                    )
                            );
                        }
                    }
                }
            }

            if (tag.contains(NBT_TAG_NAME + "|Enchantments")) {
                rewriteEnchantmentsToServer(tag, false);
            }
            if (tag.contains(NBT_TAG_NAME + "|StoredEnchantments")) {
                rewriteEnchantmentsToServer(tag, true);
            }
        }
        return item;
    }

    private void rewriteEnchantmentsToServer(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "Enchantments";
        ListTag newEnchantments = tag.get(NBT_TAG_NAME + "|" + key);
        ListTag enchantments = tag.contains(key) ? tag.get(key) : new ListTag(key, CompoundTag.class);
        if (!storedEnchant && tag.contains(NBT_TAG_NAME + "|dummyEnchant")) {
            tag.remove(NBT_TAG_NAME + "|dummyEnchant");
            for (Tag enchantment : enchantments.clone()) {
                String id = (String) ((CompoundTag) enchantment).get("id").getValue();
                if (id.isEmpty())
                    enchantments.remove(enchantment);
            }
        }

        CompoundTag display = tag.get("display");
        // A few null checks just to be safe, though they shouldn't actually be
        ListTag lore = display != null ? display.get("Lore") : null;
        for (Tag enchantment : newEnchantments.clone()) {
            enchantments.add(enchantment);
            if (lore != null && lore.size() != 0)
                lore.remove(lore.get(0));
        }
        if (lore != null && lore.size() == 0) {
            display.remove("Lore");
            if (display.isEmpty())
                tag.remove("display");
        }
        tag.put(enchantments);
        tag.remove(newEnchantments.getName());
    }

    @Override
    protected CompoundTag getNamedTag(String text) {
        CompoundTag tag = new CompoundTag("");
        tag.put(new CompoundTag("display"));
        ((CompoundTag) tag.get("display")).put(new StringTag("Name", ChatRewriter.legacyTextToJson(text)));
        return tag;
    }


    public static int getNewItemId(int id) {
        Integer newId = MappingData.oldToNewItems.get(id);
        if (newId == null) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.14 item for 1.13.2 item " + id);
            return 1;
        }
        return newId;
    }


    public static int getOldItemId(int id) {
        Integer oldId = MappingData.oldToNewItems.inverse().get(id);
        if (oldId == null) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.13.2 item for 1.14 item " + id);
            return 1;
        }
        return oldId;
    }
}
