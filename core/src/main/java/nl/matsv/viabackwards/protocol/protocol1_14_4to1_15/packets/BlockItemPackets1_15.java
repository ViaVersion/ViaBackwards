package nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.BlockItemRewriter;
import nl.matsv.viabackwards.api.rewriters.RecipeRewriter;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.Protocol1_14_4To1_15;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.RecipeRewriter1_15;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.BlockChangeRecord;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.types.Chunk1_14Type;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.types.Chunk1_15Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class BlockItemPackets1_15 extends BlockItemRewriter<Protocol1_14_4To1_15> {

    @Override
    protected void registerPackets(Protocol1_14_4To1_15 protocol) {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);

        // Set cooldown
        itemRewriter.registerSetCooldown(0x18, 0x17, BlockItemPackets1_15::getOldItemId);

        // Window items packet
        itemRewriter.registerWindowItems(Type.FLAT_VAR_INT_ITEM_ARRAY, 0x15, 0x14);

        // Set slot packet
        itemRewriter.registerSetSlot(Type.FLAT_VAR_INT_ITEM, 0x17, 0x16);

        // Trade list
        protocol.out(State.PLAY, 0x28, 0x27, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.passthrough(Type.VAR_INT);
                        int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                        for (int i = 0; i < size; i++) {
                            Item input = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                            handleItemToClient(input);

                            Item output = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                            handleItemToClient(output);

                            if (wrapper.passthrough(Type.BOOLEAN)) { // Has second item
                                // Second Item
                                Item second = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                                handleItemToClient(second);
                            }

                            wrapper.passthrough(Type.BOOLEAN);
                            wrapper.passthrough(Type.INT);
                            wrapper.passthrough(Type.INT);

                            wrapper.passthrough(Type.INT);
                            wrapper.passthrough(Type.INT);
                            wrapper.passthrough(Type.FLOAT);
                        }

                        wrapper.passthrough(Type.VAR_INT);
                        wrapper.passthrough(Type.VAR_INT);
                        wrapper.passthrough(Type.BOOLEAN);
                    }
                });
            }
        });

        // Entity Equipment Packet
        itemRewriter.registerEntityEquipment(Type.FLAT_VAR_INT_ITEM, 0x47, 0x46);

        // Declare Recipes
        protocol.registerOutgoing(State.PLAY, 0x5B, 0x5A, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    private final RecipeRewriter recipeHandler = new RecipeRewriter1_15(BlockItemPackets1_15.this);

                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int size = wrapper.passthrough(Type.VAR_INT);
                        for (int i = 0; i < size; i++) {
                            String type = wrapper.passthrough(Type.STRING).replace("minecraft:", "");
                            String id = wrapper.passthrough(Type.STRING); // Recipe Identifier
                            recipeHandler.handle(wrapper, type);
                        }
                    }
                });
            }
        });

        // Incoming packets

        // Click window packet
        itemRewriter.registerClickWindow(Type.FLAT_VAR_INT_ITEM, 0x09, 0x09);

        // Creative Inventory Action
        itemRewriter.registerCreativeInvAction(Type.FLAT_VAR_INT_ITEM, 0x26, 0x26);

        // Block Action
        protocol.registerOutgoing(State.PLAY, 0x0B, 0x0A, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14); // Location
                map(Type.UNSIGNED_BYTE); // Action id
                map(Type.UNSIGNED_BYTE); // Action param
                map(Type.VAR_INT); // Block id - /!\ NOT BLOCK STATE
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.set(Type.VAR_INT, 0, Protocol1_14_4To1_15.getNewBlockId(wrapper.get(Type.VAR_INT, 0)));
                    }
                });
            }
        });

        // Block Change
        protocol.registerOutgoing(State.PLAY, 0x0C, 0x0B, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14);
                map(Type.VAR_INT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.VAR_INT, 0);
                        wrapper.set(Type.VAR_INT, 0, Protocol1_14_4To1_15.getNewBlockStateId(id));
                    }
                });
            }
        });

        // Multi Block Change
        protocol.registerOutgoing(State.PLAY, 0x10, 0x0F, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Chunk X
                map(Type.INT); // 1 - Chunk Z
                map(Type.BLOCK_CHANGE_RECORD_ARRAY); // 2 - Records
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        for (BlockChangeRecord record : wrapper.get(Type.BLOCK_CHANGE_RECORD_ARRAY, 0)) {
                            int id = record.getBlockId();
                            record.setBlockId(Protocol1_14_4To1_15.getNewBlockStateId(id));
                        }
                    }
                });
            }
        });

        // Chunk
        protocol.registerOutgoing(State.PLAY, 0x22, 0x21, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        Chunk chunk = wrapper.read(new Chunk1_15Type(clientWorld));
                        wrapper.write(new Chunk1_14Type(clientWorld), chunk);

                        if (chunk.isGroundUp()) {
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
                            if (section == null) continue;
                            for (int j = 0; j < section.getPaletteSize(); j++) {
                                int old = section.getPaletteEntry(j);
                                int newId = Protocol1_14_4To1_15.getNewBlockStateId(old);
                                section.setPaletteEntry(j, newId);
                            }
                        }
                    }
                });
            }
        });

        // Effect packet
        protocol.registerOutgoing(State.PLAY, 0x23, 0x22, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Effect Id
                map(Type.POSITION1_14); // Location
                map(Type.INT); // Data
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.INT, 0);
                        int data = wrapper.get(Type.INT, 1);
                        if (id == 1010) { // Play record
                            wrapper.set(Type.INT, 1, BlockItemPackets1_15.getOldItemId(data));
                        } else if (id == 2001) { // Block break + block break sound
                            wrapper.set(Type.INT, 1, Protocol1_14_4To1_15.getNewBlockStateId(data));
                        }
                    }
                });
            }
        });

        // Spawn particle
        protocol.registerOutgoing(State.PLAY, 0x24, 0x23, new PacketRemapper() {
            @Override
            public void registerMap() {
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
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.INT, 0);
                        if (id == 3 || id == 23) {
                            int data = wrapper.passthrough(Type.VAR_INT);
                            wrapper.set(Type.VAR_INT, 0, Protocol1_14_4To1_15.getNewBlockStateId(data));
                        } else if (id == 32) {
                            Item item = handleItemToClient(wrapper.read(Type.FLAT_VAR_INT_ITEM));
                            wrapper.write(Type.FLAT_VAR_INT_ITEM, item);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        rewrite(881).repItem(new Item(824, (byte) 1, (short) -1, getNamedTag("1.15 Honey Bottle")));
        rewrite(878).repItem(new Item(323, (byte) 1, (short) -1, getNamedTag("1.15 Honeycomb")));
        rewrite(883).repItem(new Item(455, (byte) 1, (short) -1, getNamedTag("1.15 Honeycomb Block")));
        rewrite(879).repItem(new Item(385, (byte) 1, (short) -1, getNamedTag("1.15 Bee Nest")));
        rewrite(880).repItem(new Item(865, (byte) 1, (short) -1, getNamedTag("1.15 Beehive")));
        rewrite(882).repItem(new Item(321, (byte) 1, (short) -1, getNamedTag("1.15 Honey Block")));
        rewrite(698).repItem(new Item(722, (byte) 1, (short) -1, getNamedTag("1.15 Bee Spawn Egg")));
    }

    @Override
    public Item handleItemToClient(Item i) {
        Item item = super.handleItemToClient(i);
        if (item == null) return null;
        item.setIdentifier(getOldItemId(item.getIdentifier()));
        return item;
    }

    @Override
    public Item handleItemToServer(Item item) {
        if (item == null) return null;
        item.setIdentifier(getNewItemId(item.getIdentifier()));
        return super.handleItemToServer(item);
    }


    public static int getNewItemId(int id) {
        Integer newId = MappingData.oldToNewItems.get(id);
        if (newId == null) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.15 item for 1.14.4 item " + id);
            return 1;
        }
        return newId;
    }


    public static int getOldItemId(int id) {
        Integer oldId = MappingData.oldToNewItems.inverse().get(id);
        if (oldId == null) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.14.4 item for 1.15 item " + id);
            return 1;
        }
        return oldId;
    }
}
