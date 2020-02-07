package nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.Protocol1_14_4To1_15;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.ParticleMapping;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.RecipeRewriter1_15;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.BlockRewriter;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.types.Chunk1_14Type;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.types.Chunk1_15Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class BlockItemPackets1_15 extends nl.matsv.viabackwards.api.rewriters.ItemRewriter<Protocol1_14_4To1_15> {

    public BlockItemPackets1_15(Protocol1_14_4To1_15 protocol) {
        super(protocol, BlockItemPackets1_15::getOldItemId, BlockItemPackets1_15::getNewItemId, id -> BackwardsMappings.itemMappings.getMappedItem(id));
    }

    @Override
    protected void registerPackets() {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);
        BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION1_14, Protocol1_14_4To1_15::getNewBlockStateId, Protocol1_14_4To1_15::getNewBlockId);

        // Declare Recipes
        new RecipeRewriter1_15(this).registerDefaultHandler(0x5B, 0x5A);

        // Edit Book
        protocol.registerIncoming(State.PLAY, 0x0C, 0x0C, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)));
            }
        });

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
                            wrapper.passthrough(Type.INT);
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


        // Click window packet
        itemRewriter.registerClickWindow(Type.FLAT_VAR_INT_ITEM, 0x09, 0x09);

        // Creative Inventory Action
        itemRewriter.registerCreativeInvAction(Type.FLAT_VAR_INT_ITEM, 0x26, 0x26);

        // Acknowledge player digging
        blockRewriter.registerAcknowledgePlayerDigging(0x08, 0x5C);

        // Block Action
        blockRewriter.registerBlockAction(0x0B, 0x0A);

        // Block Change
        blockRewriter.registerBlockChange(0x0C, 0x0B);

        // Multi Block Change
        blockRewriter.registerMultiBlockChange(0x10, 0x0F);

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
        blockRewriter.registerEffect(0x23, 0x22, 1010, 2001, BlockItemPackets1_15::getOldItemId);

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
                        int mappedId = ParticleMapping.getOldId(id);
                        if (id != mappedId) {
                            wrapper.set(Type.INT, 0, mappedId);
                        }

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
