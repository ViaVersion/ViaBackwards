package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.SoundRewriter;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.chat.TranslatableRewriter1_16;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.packets.BlockItemPackets1_16;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.packets.EntityPackets1_16;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.TagRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.Protocol1_16To1_15_2;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

import java.util.UUID;

public class Protocol1_15_2To1_16 extends BackwardsProtocol {

    private BlockItemPackets1_16 blockItemPackets;

    @Override
    protected void registerPackets() {
        executeAsyncAfterLoaded(Protocol1_16To1_15_2.class, BackwardsMappings::init);

        (blockItemPackets = new BlockItemPackets1_16(this)).register();
        EntityPackets1_16 entityPackets = new EntityPackets1_16(this);
        entityPackets.register();

        TranslatableRewriter translatableRewriter = new TranslatableRewriter1_16(this);
        translatableRewriter.registerBossBar(0x0D, 0x0D);
        translatableRewriter.registerChatMessage(0x0F, 0x0F);
        translatableRewriter.registerCombatEvent(0x33, 0x33);
        translatableRewriter.registerDisconnect(0x1B, 0x1B);
        translatableRewriter.registerOpenWindow(0x2F, 0x2F);
        translatableRewriter.registerPlayerList(0x54, 0x54);
        translatableRewriter.registerTitle(0x50, 0x50);
        translatableRewriter.registerPing();

        SoundRewriter soundRewriter = new SoundRewriter(this,
                id -> BackwardsMappings.soundMappings.getNewId(id), stringId -> BackwardsMappings.soundMappings.getNewId(stringId));
        soundRewriter.registerSound(0x51, 0x51);
        soundRewriter.registerSound(0x52, 0x52);
        soundRewriter.registerNamedSound(0x1A, 0x1A);
        soundRewriter.registerStopSound(0x53, 0x53);

        // Login success
        registerOutgoing(State.LOGIN, 0x02, 0x02, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    // Transform int array to plain string
                    UUID uuid = wrapper.read(Type.UUID_INT_ARRAY);
                    wrapper.write(Type.STRING, uuid.toString());
                });
            }
        });

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
        new TagRewriter(this, id -> BackwardsMappings.blockMappings.getNewId(id), id -> {
            Integer oldId = MappingData.oldToNewItems.inverse().get(id);
            return oldId != null ? oldId : -1;
        }, entityPackets::getOldEntityId).register(0x5C, 0x5C);

        registerOutgoing(State.PLAY, 0x43, 0x4E);
        registerOutgoing(State.PLAY, 0x44, 0x43);

        registerOutgoing(State.PLAY, 0x46, 0x45);
        registerOutgoing(State.PLAY, 0x47, 0x46);

        registerOutgoing(State.PLAY, 0x49, 0x48);
        registerOutgoing(State.PLAY, 0x4A, 0x49);
        registerOutgoing(State.PLAY, 0x4B, 0x4A);
        registerOutgoing(State.PLAY, 0x4C, 0x4B);
        registerOutgoing(State.PLAY, 0x4D, 0x4C);
        registerOutgoing(State.PLAY, 0x4E, 0x4D);


        cancelIncoming(State.PLAY, 0x27); // Set jigsaw
        registerIncoming(State.PLAY, 0x10, 0x0F);
        registerIncoming(State.PLAY, 0x11, 0x10);
        registerIncoming(State.PLAY, 0x12, 0x11);
        registerIncoming(State.PLAY, 0x13, 0x12);
        registerIncoming(State.PLAY, 0x14, 0x13);
        registerIncoming(State.PLAY, 0x15, 0x14);
        registerIncoming(State.PLAY, 0x16, 0x15);
        registerIncoming(State.PLAY, 0x17, 0x16);
        registerIncoming(State.PLAY, 0x18, 0x17);
        registerIncoming(State.PLAY, 0x19, 0x18);
        registerIncoming(State.PLAY, 0x1A, 0x19);
        registerIncoming(State.PLAY, 0x1B, 0x1A);
        registerIncoming(State.PLAY, 0x1C, 0x1B);
        registerIncoming(State.PLAY, 0x1D, 0x1C);
        registerIncoming(State.PLAY, 0x1E, 0x1D);
        registerIncoming(State.PLAY, 0x1F, 0x1E);
        registerIncoming(State.PLAY, 0x20, 0x1F);
        registerIncoming(State.PLAY, 0x21, 0x20);
        registerIncoming(State.PLAY, 0x22, 0x21);
        registerIncoming(State.PLAY, 0x23, 0x22);
        registerIncoming(State.PLAY, 0x24, 0x23);
        registerIncoming(State.PLAY, 0x25, 0x24);
        registerIncoming(State.PLAY, 0x26, 0x25);

        registerIncoming(State.PLAY, 0x29, 0x28);
        registerIncoming(State.PLAY, 0x2A, 0x29);
        registerIncoming(State.PLAY, 0x2B, 0x2A);
        registerIncoming(State.PLAY, 0x2C, 0x2B);
        registerIncoming(State.PLAY, 0x2D, 0x2C);
        registerIncoming(State.PLAY, 0x2E, 0x2D);
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
