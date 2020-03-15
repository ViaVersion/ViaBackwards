package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.SoundRewriter;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.packets.BlockItemPackets1_16;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.packets.EntityPackets1_16;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class Protocol1_15_2To1_16 extends BackwardsProtocol {

    private BlockItemPackets1_16 blockItemPackets;

    @Override
    protected void registerPackets() {
        BackwardsMappings.init();
        (blockItemPackets = new BlockItemPackets1_16(this)).register();
        new EntityPackets1_16(this).register();

        TranslatableRewriter translatableRewriter = new TranslatableRewriter(this);
        translatableRewriter.registerBossBar(0x0D, 0x0D);
        translatableRewriter.registerChatMessage(0x0F, 0x0F);
        translatableRewriter.registerCombatEvent(0x33, 0x33);
        translatableRewriter.registerDisconnect(0x1B, 0x1B);
        translatableRewriter.registerOpenWindow(0x2F, 0x2F);
        translatableRewriter.registerPlayerList(0x54, 0x54);
        translatableRewriter.registerTitle(0x50, 0x50);
        translatableRewriter.registerPing();

        SoundRewriter soundRewriter = new SoundRewriter(this, BackwardsMappings.soundMappings);
        soundRewriter.registerSound(0x51, 0x51);
        soundRewriter.registerSound(0x52, 0x52);
        soundRewriter.registerNamedSound(0x1A, 0x1A);

        // Advancements
        registerOutgoing(State.PLAY, 0x58, 0x58, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
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
                });
            }
        });

        // Tags
        registerOutgoing(State.PLAY, 0x5C, 0x5C, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
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
                });
            }
        });
    }

    public static int getNewBlockStateId(int id) {
        int newId = BackwardsMappings.blockStateMappings.getNewId(id);
        if (newId == -1) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.16 blockstate id for 1.15 block " + id);
            return 0;
        }
        return newId;
    }


    public static int getNewBlockId(int id) {
        int newId = BackwardsMappings.blockMappings.getNewId(id);
        if (newId == -1) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.16 block id for 1.15 block " + id);
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

    public BlockItemPackets1_16 getBlockItemPackets() {
        return blockItemPackets;
    }
}
