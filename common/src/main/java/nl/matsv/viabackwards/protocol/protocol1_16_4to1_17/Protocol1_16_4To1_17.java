package nl.matsv.viabackwards.protocol.protocol1_16_4to1_17;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.BackwardsMappings;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.SoundRewriter;
import nl.matsv.viabackwards.protocol.protocol1_16_4to1_17.packets.BlockItemPackets1_17;
import nl.matsv.viabackwards.protocol.protocol1_16_4to1_17.packets.EntityPackets1_17;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.IdRewriteFunction;
import us.myles.ViaVersion.api.rewriters.RegistryType;
import us.myles.ViaVersion.api.rewriters.StatisticsRewriter;
import us.myles.ViaVersion.api.rewriters.TagRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2;
import us.myles.ViaVersion.protocols.protocol1_17to1_16_4.ClientboundPackets1_17;
import us.myles.ViaVersion.protocols.protocol1_17to1_16_4.Protocol1_17To1_16_4;
import us.myles.viaversion.libs.fastutil.ints.IntArrayList;
import us.myles.viaversion.libs.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Protocol1_16_4To1_17 extends BackwardsProtocol<ClientboundPackets1_17, ClientboundPackets1_16_2, ServerboundPackets1_16_2, ServerboundPackets1_16_2> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.17", "1.16.2", Protocol1_17To1_16_4.class, true);
    private static final int[] EMPTY_ARRAY = {};
    private BlockItemPackets1_17 blockItemPackets;

    public Protocol1_16_4To1_17() {
        super(ClientboundPackets1_17.class, ClientboundPackets1_16_2.class, ServerboundPackets1_16_2.class, ServerboundPackets1_16_2.class);
    }

    @Override
    protected void registerPackets() {
        executeAsyncAfterLoaded(Protocol1_17To1_16_4.class, MAPPINGS::load);

        blockItemPackets = new BlockItemPackets1_17(this, null);
        blockItemPackets.register();

        new EntityPackets1_17(this).register();

        SoundRewriter soundRewriter = new SoundRewriter(this);
        soundRewriter.registerSound(ClientboundPackets1_17.SOUND);
        soundRewriter.registerSound(ClientboundPackets1_17.ENTITY_SOUND);
        soundRewriter.registerNamedSound(ClientboundPackets1_17.NAMED_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_17.STOP_SOUND);

        TagRewriter tagRewriter = new TagRewriter(this, null);
        registerOutgoing(ClientboundPackets1_17.TAGS, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    Map<String, List<TagRewriter.TagData>> tags = new HashMap<>();

                    int length = wrapper.read(Type.VAR_INT);
                    for (int i = 0; i < length; i++) {
                        String resourceKey = wrapper.read(Type.STRING);
                        if (resourceKey.startsWith("minecraft:")) {
                            resourceKey = resourceKey.substring(10);
                        }

                        List<TagRewriter.TagData> tagList = new ArrayList<>();
                        tags.put(resourceKey, tagList);

                        int tagLength = wrapper.read(Type.VAR_INT);
                        for (int j = 0; j < tagLength; j++) {
                            String identifier = wrapper.read(Type.STRING);
                            int[] entries = wrapper.read(Type.VAR_INT_ARRAY_PRIMITIVE);
                            tagList.add(new TagRewriter.TagData(identifier, entries));
                        }
                    }

                    // Put them into the hardcoded order of Vanilla tags (and only those), rewrite ids
                    for (RegistryType type : RegistryType.getValues()) {
                        List<TagRewriter.TagData> tagList = tags.get(type.getResourceLocation());
                        IdRewriteFunction rewriter = tagRewriter.getRewriter(type);

                        wrapper.write(Type.VAR_INT, tagList.size());
                        for (TagRewriter.TagData tagData : tagList) {
                            int[] entries = tagData.getEntries();
                            if (rewriter != null) {
                                // Handle id rewriting now
                                IntList idList = new IntArrayList(entries.length);
                                for (int id : entries) {
                                    int mappedId = rewriter.rewrite(id);
                                    if (mappedId != -1) {
                                        idList.add(mappedId);
                                    }
                                }
                                entries = idList.toArray(EMPTY_ARRAY);
                            }

                            wrapper.write(Type.STRING, tagData.getIdentifier());
                            wrapper.write(Type.VAR_INT_ARRAY_PRIMITIVE, entries);
                        }

                        // Stop after the entity types
                        if (type == RegistryType.ENTITY) {
                            break;
                        }
                    }
                });
            }
        });

        new StatisticsRewriter(this, null).register(ClientboundPackets1_17.STATISTICS);

        registerOutgoing(ClientboundPackets1_17.RESOURCE_PACK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.passthrough(Type.STRING);
                    wrapper.passthrough(Type.STRING);
                    wrapper.read(Type.BOOLEAN); // Required
                });
            }
        });

        registerOutgoing(ClientboundPackets1_17.MAP_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.passthrough(Type.VAR_INT);
                    wrapper.passthrough(Type.BYTE);
                    wrapper.write(Type.BOOLEAN, true); // Tracking position
                    wrapper.passthrough(Type.BOOLEAN);

                    boolean hasMarkers = wrapper.read(Type.BOOLEAN);
                    if (!hasMarkers) {
                        wrapper.write(Type.VAR_INT, 0); // Array size
                    }
                });
            }
        });

        registerIncoming(ServerboundPackets1_16_2.CLIENT_SETTINGS, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Locale
                map(Type.BYTE); // View distance
                map(Type.VAR_INT); // Chat mode
                map(Type.BOOLEAN); // Chat colors
                map(Type.UNSIGNED_BYTE); // Chat flags
                map(Type.VAR_INT); // Main hand
                handler(wrapper -> {
                    wrapper.write(Type.BOOLEAN, false); // Text filtering
                });
            }
        });

        cancelOutgoing(ClientboundPackets1_17.ADD_VIBRATION_SIGNAL);
    }

    @Override
    public void init(UserConnection user) {
        if (!user.has(EntityTracker.class)) {
            user.put(new EntityTracker(user));
        }
        user.get(EntityTracker.class).initProtocol(this);
    }

    public BlockItemPackets1_17 getBlockItemPackets() {
        return blockItemPackets;
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }
}
