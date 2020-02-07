package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.RecipeRewriter;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.RecipeRewriter1_15;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data.BackwardsMappings;
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
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.types.Chunk1_15Type;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class BlockItemPackets1_16 extends nl.matsv.viabackwards.api.rewriters.ItemRewriter<Protocol1_15_2To1_16> {

    public BlockItemPackets1_16(Protocol1_15_2To1_16 protocol) {
        super(protocol, BlockItemPackets1_16::getOldItemId, BlockItemPackets1_16::getNewItemId, id -> BackwardsMappings.itemMappings.getMappedItem(id));
    }

    @Override
    protected void registerPackets() {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);
        BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION1_14, Protocol1_15_2To1_16::getNewBlockStateId, Protocol1_15_2To1_16::getNewBlockId);

        // Set cooldown
        itemRewriter.registerSetCooldown(0x18, 0x18, BlockItemPackets1_16::getOldItemId);

        // Window items packet
        itemRewriter.registerWindowItems(Type.FLAT_VAR_INT_ITEM_ARRAY, 0x15, 0x15);

        // Set slot packet
        itemRewriter.registerSetSlot(Type.FLAT_VAR_INT_ITEM, 0x17, 0x17);

        // Trade list
        protocol.out(State.PLAY, 0x28, 0x28, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
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
                });
            }
        });

        // Entity Equipment Packet
        itemRewriter.registerEntityEquipment(Type.FLAT_VAR_INT_ITEM, 0x47, 0x47);

        // Declare Recipes
        protocol.registerOutgoing(State.PLAY, 0x5B, 0x5B, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    private final RecipeRewriter recipeHandler = new RecipeRewriter1_15(BlockItemPackets1_16.this);

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

        // Click window packet
        itemRewriter.registerClickWindow(Type.FLAT_VAR_INT_ITEM, 0x09, 0x09);

        // Creative Inventory Action
        itemRewriter.registerCreativeInvAction(Type.FLAT_VAR_INT_ITEM, 0x26, 0x26);

        // Acknowledge player digging
        blockRewriter.registerAcknowledgePlayerDigging(0x08, 0x08);

        // Block Action
        blockRewriter.registerBlockAction(0x0B, 0x0B);

        // Block Change
        blockRewriter.registerBlockChange(0x0C, 0x0C);

        // Multi Block Change
        blockRewriter.registerMultiBlockChange(0x10, 0x10);

        // Chunk
        protocol.registerOutgoing(State.PLAY, 0x22, 0x22, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                    Chunk chunk = wrapper.passthrough(new Chunk1_15Type(clientWorld));
                    for (int i = 0; i < chunk.getSections().length; i++) {
                        ChunkSection section = chunk.getSections()[i];
                        if (section == null) continue;
                        for (int j = 0; j < section.getPaletteSize(); j++) {
                            int old = section.getPaletteEntry(j);
                            section.setPaletteEntry(j, Protocol1_15_2To1_16.getNewBlockStateId(old));
                        }
                    }

                    if (chunk.isBiomeData()) {
                        for (int i = 0; i < 1024; i++) {
                            int biome = chunk.getBiomeData()[i];
                            int newId = -1;
                            switch (biome) {
                                case 170: // new nether biomes
                                case 171:
                                case 172:
                                    newId = 8;
                                    break;
                            }

                            if (newId != -1) {
                                chunk.getBiomeData()[i] = newId;
                            }
                        }
                    }
                });
            }
        });

        // Effect packet
        blockRewriter.registerEffect(0x23, 0x23, 1010, 2001, BlockItemPackets1_16::getOldItemId);

        // Spawn particle
        blockRewriter.registerSpawnParticle(Type.DOUBLE, 0x24, 0x24, 3, 23, 32,
                BlockItemPackets1_16::getNewParticleId, this::handleItemToClient, Type.FLAT_VAR_INT_ITEM);
    }

    public static int getNewParticleId(int id) {
        switch (id) {
            case 27: // soul flame -> flame
                return 26;
            case 63: // ash, crimson spore, warped spore -> mycelium
            case 64:
            case 65:
                return 37;
        }
        if (id > 27) {
            id -= 1;
        }
        return id;
    }

    public static int getNewItemId(int id) {
        Integer newId = MappingData.oldToNewItems.get(id);
        if (newId == null) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.16 item for 1.15 item " + id);
            return 1;
        }
        return newId;
    }


    public static int getOldItemId(int id) {
        Integer oldId = MappingData.oldToNewItems.inverse().get(id);
        if (oldId == null) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.15 item for 1.16 item " + id);
            return 1;
        }
        return oldId;
    }
}
