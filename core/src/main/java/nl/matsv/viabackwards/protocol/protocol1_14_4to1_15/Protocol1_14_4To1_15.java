package nl.matsv.viabackwards.protocol.protocol1_14_4to1_15;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.EntityTypeMapping;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.packets.BlockItemPackets1_15;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.packets.EntityPackets1_15;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class Protocol1_14_4To1_15 extends BackwardsProtocol {

    private static final Integer[] A = new Integer[0];
    private BlockItemPackets1_15 blockItemPackets;

    @Override
    protected void registerPackets() {
        BackwardsMappings.init();
        blockItemPackets = new BlockItemPackets1_15();
        blockItemPackets.register(this);
        new EntityPackets1_15().register(this);

        // Entity Sound Effect
        registerOutgoing(State.PLAY, 0x51, 0x50, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Sound Id
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int newId = BackwardsMappings.soundMappings.getNewId(wrapper.get(Type.VAR_INT, 0));
                        if (newId == -1) {
                            wrapper.cancel();
                        } else {
                            wrapper.set(Type.VAR_INT, 0, newId);
                        }
                    }
                });
            }
        });

        // Sound Effect
        registerOutgoing(State.PLAY, 0x52, 0x51, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Sound Id
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int newId = BackwardsMappings.soundMappings.getNewId(wrapper.get(Type.VAR_INT, 0));
                        if (newId == -1) {
                            wrapper.cancel();
                        } else {
                            wrapper.set(Type.VAR_INT, 0, newId);
                        }
                    }
                });
            }
        });

        // Advancements
        registerOutgoing(State.PLAY, 0x58, 0x57, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.passthrough(Type.BOOLEAN); // Reset/clear
                        int size = wrapper.passthrough(Type.VAR_INT);
                        for (int i = 0; i < size; i++) {
                            wrapper.passthrough(Type.STRING); // Identifier
                            // Parent
                            if (wrapper.passthrough(Type.BOOLEAN)) {
                                wrapper.passthrough(Type.STRING);
                            }
                            // Display data
                            if (wrapper.passthrough(Type.BOOLEAN)) {
                                wrapper.passthrough(Type.STRING); // Title
                                wrapper.passthrough(Type.STRING); // Description
                                blockItemPackets.handleItemToClient(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)); // Icon
                                wrapper.passthrough(Type.VAR_INT); // Frame type
                                int flags = wrapper.passthrough(Type.INT); // Flags
                                if ((flags & 1) != 0) {
                                    wrapper.passthrough(Type.STRING); // Background texture
                                }
                                wrapper.passthrough(Type.FLOAT); // X
                                wrapper.passthrough(Type.FLOAT); // Y
                            }

                            wrapper.passthrough(Type.STRING_ARRAY); // Criteria
                            int arrayLength = wrapper.passthrough(Type.VAR_INT);
                            for (int array = 0; array < arrayLength; array++) {
                                wrapper.passthrough(Type.STRING_ARRAY); // String array
                            }
                        }
                    }
                });
            }
        });

        // Tags
        registerOutgoing(State.PLAY, 0x5C, 0x5B, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int blockTagsSize = wrapper.passthrough(Type.VAR_INT);
                        for (int i = 0; i < blockTagsSize; i++) {
                            wrapper.passthrough(Type.STRING);
                            int[] blockIds = wrapper.passthrough(Type.VAR_INT_ARRAY_PRIMITIVE);
                            for (int j = 0; j < blockIds.length; j++) {
                                int id = blockIds[j];
                                blockIds[j] = BackwardsMappings.blockMappings.getNewId(id);
                            }
                        }

                        int itemTagsSize = wrapper.passthrough(Type.VAR_INT);
                        for (int i = 0; i < itemTagsSize; i++) {
                            wrapper.passthrough(Type.STRING);
                            int[] itemIds = wrapper.passthrough(Type.VAR_INT_ARRAY_PRIMITIVE);
                            for (int j = 0; j < itemIds.length; j++) {
                                Integer oldId = MappingData.oldToNewItems.inverse().get(itemIds[j]);
                                itemIds[j] = oldId != null ? oldId : -1;
                            }
                        }

                        int fluidTagsSize = wrapper.passthrough(Type.VAR_INT); // fluid tags
                        for (int i = 0; i < fluidTagsSize; i++) {
                            wrapper.passthrough(Type.STRING);
                            wrapper.passthrough(Type.VAR_INT_ARRAY_PRIMITIVE);
                        }

                        int entityTagsSize = wrapper.passthrough(Type.VAR_INT);
                        for (int i = 0; i < entityTagsSize; i++) {
                            wrapper.passthrough(Type.STRING);
                            int[] entityIds = wrapper.passthrough(Type.VAR_INT_ARRAY_PRIMITIVE);
                            for (int j = 0; j < entityIds.length; j++) {
                                entityIds[j] = EntityTypeMapping.getOldEntityId(entityIds[j]);
                            }
                        }
                    }
                });
            }
        });

        registerOutgoing(State.PLAY, 0x09, 0x08);
        registerOutgoing(State.PLAY, 0x0A, 0x09);
        registerOutgoing(State.PLAY, 0x0D, 0x0C);
        registerOutgoing(State.PLAY, 0x0E, 0x0D);
        registerOutgoing(State.PLAY, 0x0F, 0x0E);
        registerOutgoing(State.PLAY, 0x11, 0x10);
        registerOutgoing(State.PLAY, 0x12, 0x11);
        registerOutgoing(State.PLAY, 0x13, 0x12);
        registerOutgoing(State.PLAY, 0x14, 0x13);
        registerOutgoing(State.PLAY, 0x16, 0x15);
        registerOutgoing(State.PLAY, 0x19, 0x18);
        registerOutgoing(State.PLAY, 0x1A, 0x19);
        registerOutgoing(State.PLAY, 0x1B, 0x1A);
        registerOutgoing(State.PLAY, 0x1C, 0x1B);
        registerOutgoing(State.PLAY, 0x1D, 0x1C);
        registerOutgoing(State.PLAY, 0x1E, 0x1D);
        registerOutgoing(State.PLAY, 0x1F, 0x1E);
        registerOutgoing(State.PLAY, 0x20, 0x1F);
        registerOutgoing(State.PLAY, 0x21, 0x20);
        registerOutgoing(State.PLAY, 0x25, 0x24);
        registerOutgoing(State.PLAY, 0x27, 0x26);
        registerOutgoing(State.PLAY, 0x29, 0x28);
        registerOutgoing(State.PLAY, 0x2A, 0x29);
        registerOutgoing(State.PLAY, 0x2B, 0x2A);
        registerOutgoing(State.PLAY, 0x2C, 0x2B);
        registerOutgoing(State.PLAY, 0x2D, 0x2C);
        registerOutgoing(State.PLAY, 0x2E, 0x2D);
        registerOutgoing(State.PLAY, 0x2F, 0x2E);
        registerOutgoing(State.PLAY, 0x30, 0x2F);
        registerOutgoing(State.PLAY, 0x31, 0x30);
        registerOutgoing(State.PLAY, 0x32, 0x31);
        registerOutgoing(State.PLAY, 0x33, 0x32);
        registerOutgoing(State.PLAY, 0x34, 0x33);
        registerOutgoing(State.PLAY, 0x35, 0x34);
        registerOutgoing(State.PLAY, 0x36, 0x35);
        registerOutgoing(State.PLAY, 0x37, 0x36);
        registerOutgoing(State.PLAY, 0x39, 0x38);
        registerOutgoing(State.PLAY, 0x3A, 0x39);
        registerOutgoing(State.PLAY, 0x3C, 0x3B);
        registerOutgoing(State.PLAY, 0x3D, 0x3C);
        registerOutgoing(State.PLAY, 0x3E, 0x3D);
        registerOutgoing(State.PLAY, 0x3F, 0x3E);
        registerOutgoing(State.PLAY, 0x40, 0x3F);
        registerOutgoing(State.PLAY, 0x41, 0x40);
        registerOutgoing(State.PLAY, 0x42, 0x41);
        registerOutgoing(State.PLAY, 0x43, 0x42);
        registerOutgoing(State.PLAY, 0x45, 0x44);
        registerOutgoing(State.PLAY, 0x46, 0x45);
        registerOutgoing(State.PLAY, 0x48, 0x47);
        registerOutgoing(State.PLAY, 0x49, 0x48);
        registerOutgoing(State.PLAY, 0x4A, 0x49);
        registerOutgoing(State.PLAY, 0x4B, 0x4A);
        registerOutgoing(State.PLAY, 0x4C, 0x4B);
        registerOutgoing(State.PLAY, 0x4D, 0x4C);
        registerOutgoing(State.PLAY, 0x4E, 0x4D);
        registerOutgoing(State.PLAY, 0x4F, 0x4E);
        registerOutgoing(State.PLAY, 0x50, 0x4F);
        registerOutgoing(State.PLAY, 0x53, 0x52);
        registerOutgoing(State.PLAY, 0x54, 0x53);
        registerOutgoing(State.PLAY, 0x55, 0x54);
        registerOutgoing(State.PLAY, 0x56, 0x55);
        registerOutgoing(State.PLAY, 0x57, 0x56);
        registerOutgoing(State.PLAY, 0x5A, 0x59);
    }

    public static int getNewBlockStateId(int id) {
        int newId = BackwardsMappings.blockStateMappings.getNewId(id);
        if (newId == -1) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.15 blockstate id for 1.14.4 block " + id);
            return 0;
        }
        return newId;
    }


    public static int getNewBlockId(int id) {
        int newId = BackwardsMappings.blockMappings.getNewId(id);
        if (newId == -1) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.15 block id for 1.14.4 block " + id);
            return id;
        }
        return newId;
    }

    @Override
    public void init(UserConnection user) {
        if (!user.has(ClientWorld.class))
            user.put(new ClientWorld(user));
        if (!user.has(EntityTracker.class))
            user.put(new EntityTracker(user));
        user.get(EntityTracker.class).initProtocol(this);
    }

    public BlockItemPackets1_15 getBlockItemPackets() {
        return blockItemPackets;
    }
}
