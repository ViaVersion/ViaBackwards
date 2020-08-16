package nl.matsv.viabackwards.protocol.protocol1_16_1to1_16_2;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.BackwardsMappings;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.SoundRewriter;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import nl.matsv.viabackwards.protocol.protocol1_16_1to1_16_2.packets.BlockItemPackets1_16_2;
import nl.matsv.viabackwards.protocol.protocol1_16_1to1_16_2.packets.EntityPackets1_16_2;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.StatisticsRewriter;
import us.myles.ViaVersion.api.rewriters.TagRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.Protocol1_16_2To1_16_1;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.ClientboundPackets1_16;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.ServerboundPackets1_16;

public class Protocol1_16_1To1_16_2 extends BackwardsProtocol<ClientboundPackets1_16_2, ClientboundPackets1_16, ServerboundPackets1_16_2, ServerboundPackets1_16> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.16.2", "1.16", Protocol1_16_2To1_16_1.class, true);
    private BlockItemPackets1_16_2 blockItemPackets;
    private TranslatableRewriter translatableRewriter;

    public Protocol1_16_1To1_16_2() {
        super(ClientboundPackets1_16_2.class, ClientboundPackets1_16.class, ServerboundPackets1_16_2.class, ServerboundPackets1_16.class);
    }

    @Override
    protected void registerPackets() {
        executeAsyncAfterLoaded(Protocol1_16_2To1_16_1.class, MAPPINGS::loadVBMappings);

        translatableRewriter = new TranslatableRewriter(this);
        translatableRewriter.registerBossBar(ClientboundPackets1_16_2.BOSSBAR);
        translatableRewriter.registerCombatEvent(ClientboundPackets1_16_2.COMBAT_EVENT);
        translatableRewriter.registerDisconnect(ClientboundPackets1_16_2.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_16_2.TAB_LIST);
        translatableRewriter.registerTitle(ClientboundPackets1_16_2.TITLE);
        translatableRewriter.registerChatMessage(ClientboundPackets1_16_2.CHAT_MESSAGE);
        translatableRewriter.registerOpenWindow(ClientboundPackets1_16_2.OPEN_WINDOW);
        translatableRewriter.registerPing();

        (blockItemPackets = new BlockItemPackets1_16_2(this, translatableRewriter)).register();
        EntityPackets1_16_2 entityPackets = new EntityPackets1_16_2(this);
        entityPackets.register();

        SoundRewriter soundRewriter = new SoundRewriter(this);
        soundRewriter.registerSound(ClientboundPackets1_16_2.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_16_2.ENTITY_SOUND);
        soundRewriter.registerNamedSound(ClientboundPackets1_16_2.NAMED_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_16_2.STOP_SOUND);

        // Recipe book data has been split into 2 separate packets
        registerIncoming(ServerboundPackets1_16.RECIPE_BOOK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int type = wrapper.read(Type.VAR_INT);
                        if (type == 0) {
                            // Shown, change to its own packet
                            wrapper.passthrough(Type.STRING); // Recipe
                            wrapper.setId(ServerboundPackets1_16_2.SEEN_RECIPE.ordinal());
                        } else {
                            wrapper.cancel();

                            // Settings
                            for (int i = 0; i < 3; i++) {
                                sendSeenRecipePacket(i, wrapper);
                            }
                        }
                    }

                    private void sendSeenRecipePacket(int recipeType, PacketWrapper wrapper) throws Exception {
                        boolean open = wrapper.read(Type.BOOLEAN);
                        boolean filter = wrapper.read(Type.BOOLEAN);

                        PacketWrapper newPacket = wrapper.create(ServerboundPackets1_16_2.RECIPE_BOOK_DATA.ordinal());
                        newPacket.write(Type.VAR_INT, recipeType);
                        newPacket.write(Type.BOOLEAN, open);
                        newPacket.write(Type.BOOLEAN, filter);
                        newPacket.sendToServer(Protocol1_16_1To1_16_2.class);
                    }
                });
            }
        });

        new TagRewriter(this, entityPackets::getOldEntityId).register(ClientboundPackets1_16_2.TAGS);

        new StatisticsRewriter(this, entityPackets::getOldEntityId).register(ClientboundPackets1_16_2.STATISTICS);
    }

    @Override
    public void init(UserConnection user) {
        if (!user.has(EntityTracker.class)) {
            user.put(new EntityTracker(user));
        }
        user.get(EntityTracker.class).initProtocol(this);
    }

    public BlockItemPackets1_16_2 getBlockItemPackets() {
        return blockItemPackets;
    }

    public TranslatableRewriter getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }
}
