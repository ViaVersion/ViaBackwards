package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.packets;

import com.google.common.collect.ImmutableSet;
import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.BlockItemRewriter;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets.InventoryPackets1_13_1;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.BlockChangeRecord;
import us.myles.ViaVersion.api.minecraft.Environment;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueCreator;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.types.Chunk1_13Type;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.Protocol1_14To1_13_2;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.types.Chunk1_14Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.viaversion.libs.opennbt.conversion.ConverterRegistry;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

import java.util.Set;

public class BlockItemPackets1_14 extends BlockItemRewriter<Protocol1_13_2To1_14> {
    private static String NBT_TAG_NAME = "ViaBackwards|" + Protocol1_13_2To1_14.class.getSimpleName();

    @Override
    protected void registerPackets(Protocol1_13_2To1_14 protocol) {
        // Open window
        protocol.registerOutgoing(State.PLAY, 0x58, 0x14, new PacketRemapper() {
            @Override
            public void registerMap() { // c
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.read(Type.VAR_INT);
                        wrapper.write(Type.UNSIGNED_BYTE, (short) id);
                        int type = wrapper.read(Type.VAR_INT);
                        String stringType = null;
                        int slotSize = 0;
                        switch (type) {
                            case 0:
                                stringType = "minecraft:container";
                                slotSize = 27;
                                break;
                            case 1:
                                stringType = "minecraft:container";
                                slotSize = 54;
                                break;
                            case 7:
                                stringType = "minecraft:crafting_table";
                                break;
                            case 9:
                                stringType = "minecraft:furnace";
                                break;
                            case 2:
                                stringType = "minecraft:dropper";
                                break;
                            case 8:
                                stringType = "minecraft:enchanting_table";
                                break;
                            case 6:
                                stringType = "minecraft:brewing_stand";
                                break;
                            case 14:
                                stringType = "minecraft:villager";
                                break;
                            case 4:
                                stringType = "minecraft:beacon";
                                break;
                            case 3:
                                stringType = "minecraft:anvil";
                                break;
                            case 11:
                                stringType = "minecraft:hopper";
                                break;
                            case 15:
                                stringType = "minecraft:shulker_box";
                                break;
                        }

                        if (stringType == null) {
                            ViaBackwards.getPlatform().getLogger().warning("Can't open inventory for 1.13 player! Type: " + type);
                            wrapper.cancel();
                            return;
                        }

                        wrapper.write(Type.STRING, stringType);
                        wrapper.passthrough(Type.STRING);
                        wrapper.write(Type.UNSIGNED_BYTE, (short) slotSize);
                    }
                });
            }
        });

        // Horse window
        protocol.registerOutgoing(State.PLAY, 0x14, 0x14, new PacketRemapper() {
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

        // Window items packet
        protocol.registerOutgoing(State.PLAY, 0x15, 0x15, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.FLAT_VAR_INT_ITEM_ARRAY); // 1 - Window Values

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item[] stacks = wrapper.get(Type.FLAT_VAR_INT_ITEM_ARRAY, 0);
                        for (Item stack : stacks) handleItemToClient(stack);
                    }
                });
            }
        });

        // Set slot packet
        protocol.registerOutgoing(State.PLAY, 0x17, 0x17, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE); // 0 - Window ID
                map(Type.SHORT); // 1 - Slot ID
                map(Type.FLAT_VAR_INT_ITEM); // 2 - Slot Value

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        handleItemToClient(wrapper.get(Type.FLAT_VAR_INT_ITEM, 0));
                    }
                });
            }
        });

        // Trade list
        protocol.out(State.PLAY, 0x59, 0x19, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.write(Type.STRING, "minecraft:trader_list");
                        wrapper.read(Type.STRING); // Remove channel

                        int windowId = wrapper.read(Type.INT);
                        wrapper.write(Type.VAR_INT, windowId);

                        int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                        for (int i = 0; i < size; i++) {
                            // Input Item
                            handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));
                            // Output Item
                            handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));

                            boolean secondItem = wrapper.passthrough(Type.BOOLEAN); // Has second item
                            if (secondItem) {
                                // Second Item
                                handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));
                            }

                            wrapper.passthrough(Type.BOOLEAN); // Trade disabled
                            wrapper.passthrough(Type.INT); // Number of tools uses
                            wrapper.passthrough(Type.INT); // Maximum number of trade uses
                        }
                    }
                });
            }
        });

        // Book open
        protocol.registerOutgoing(State.PLAY, 0x2C, 0x19, new PacketRemapper() {
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
        protocol.registerOutgoing(State.PLAY, 0x42, 0x42, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.VAR_INT); // 1 - Slot ID
                map(Type.FLAT_VAR_INT_ITEM); // 2 - Item

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        handleItemToClient(wrapper.get(Type.FLAT_VAR_INT_ITEM, 0));
                    }
                });
            }
        });

        Set<String> removedTypes = ImmutableSet.of("crafting_special_suspiciousstew", "blasting", "smoking", "campfire_cooking");

        // Declare Recipes
        protocol.registerOutgoing(State.PLAY, 0x55, 0x54, new PacketRemapper() { // c
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
                                if (!type.equals("crafting_special_suspiciousstew")) {
                                    wrapper.read(Type.STRING); // Group
                                    Item[] items = wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                    wrapper.read(Type.FLAT_VAR_INT_ITEM);
                                    wrapper.read(Type.FLOAT); // EXP
                                    wrapper.read(Type.VAR_INT); // Cooking time
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
                                    Item[] items = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                    for (Item item : items) handleItemToClient(item);
                                }
                                handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // Result
                            } else if (type.equals("crafting_shaped")) {
                                int ingredientsNo = wrapper.passthrough(Type.VAR_INT) * wrapper.passthrough(Type.VAR_INT);
                                wrapper.passthrough(Type.STRING); // Group
                                for (int j = 0; j < ingredientsNo; j++) {
                                    Item[] items = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                    for (Item item : items) handleItemToClient(item);
                                }
                                handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // Result
                            } else if (type.equals("smelting")) {
                                wrapper.passthrough(Type.STRING); // Group
                                Item[] items = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                for (Item item : items) handleItemToClient(item);
                                handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));
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
        protocol.registerIncoming(State.PLAY, 0x08, 0x08, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.SHORT); // 1 - Slot
                map(Type.BYTE); // 2 - Button
                map(Type.SHORT); // 3 - Action number
                map(Type.VAR_INT); // 4 - Mode
                map(Type.FLAT_VAR_INT_ITEM); // 5 - Clicked Item

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        handleItemToServer(wrapper.get(Type.FLAT_VAR_INT_ITEM, 0));
                    }
                });
            }
        });

        // Creative Inventory Action
        protocol.registerIncoming(State.PLAY, 0x24, 0x24, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.SHORT); // 0 - Slot
                map(Type.FLAT_VAR_INT_ITEM); // 1 - Clicked Item

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        handleItemToServer(wrapper.get(Type.FLAT_VAR_INT_ITEM, 0));
                    }
                });
            }
        });

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
                map(Type.POSITION, Type.POSITION1_14); // Location
                map(Type.UNSIGNED_BYTE); // Action id
                map(Type.UNSIGNED_BYTE); // Action param
                map(Type.VAR_INT); // Block id - /!\ NOT BLOCK STATE
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(Type.VAR_INT, 0, Protocol1_13_2To1_14.getNewBlockStateId(wrapper.get(Type.VAR_INT, 0)));
                    }
                });
            }
        });

        // Block Change
        protocol.registerOutgoing(State.PLAY, 0xB, 0xB, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION, Type.POSITION1_14);
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

        //Chunk
        protocol.registerOutgoing(State.PLAY, 0x22, 0x22, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        Chunk chunk = wrapper.read(new Chunk1_14Type(clientWorld));
                        wrapper.write(new Chunk1_13Type(clientWorld), chunk);

                        for (ChunkSection section : chunk.getSections()) {
                            if (section == null) continue;
                            section.setBlockLight(new byte[2048]);
                            if (clientWorld.getEnvironment() == Environment.NORMAL) {
                                section.setSkyLight(new byte[2048]);
                            }
                            for (int i = 0; i < section.getPaletteSize(); i++) {
                                int old = section.getPaletteEntry(i);
                                int newId = Protocol1_13_2To1_14.getNewBlockStateId(old);
                                section.setPaletteEntry(i, newId);
                            }
                        }
                    }
                });
            }
        });

        // Effect packet
        protocol.registerOutgoing(State.PLAY, 0x23, 0x23, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Effect Id
                map(Type.POSITION, Type.POSITION1_14); // Location
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
        protocol.registerOutgoing(State.PLAY, 0x24, 0x24, new PacketRemapper() {
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
                        if (id == 3 || id == 20) {
                            int data = wrapper.passthrough(Type.VAR_INT);
                            wrapper.set(Type.VAR_INT, 0, Protocol1_14To1_13_2.getNewBlockStateId(data));
                        } else if (id == 27) {
                            handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));
                        }
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

        //respawn
        protocol.registerOutgoing(State.PLAY, 0x38, 0x38, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Dimension ID
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        int dimensionId = wrapper.get(Type.INT, 0);
                        clientWorld.setEnvironment(dimensionId);
                    }
                });
            }
        });

        // Spawn position
        protocol.registerOutgoing(State.PLAY, 0x49, 0x49, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION, Type.POSITION1_14);
            }
        });
    }

    @Override
    protected void registerRewrites() {

    }

    @Override
    public Item handleItemToClient(Item i) {
        Item item = super.handleItemToClient(i);
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
        }
        return item;
    }

    @Override
    public Item handleItemToServer(Item i) {
        Item item = super.handleItemToServer(i);
        item.setIdentifier(getNewItemId(item.getIdentifier()));

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
        }
        return item;
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
        return oldId != null ? oldId : 1;
    }
}
