package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.packets;

import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.ParticleMapping;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data.BackwardsMappings;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.entities.Entity1_15Types;
import us.myles.ViaVersion.api.entities.Entity1_16Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_14;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueTransformer;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.Particle;
import us.myles.ViaVersion.api.type.types.version.Types1_14;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class EntityPackets1_16 extends EntityRewriter<Protocol1_15_2To1_16> {

    private final ValueTransformer<String, Integer> dimensionTransformer = new ValueTransformer<String, Integer>(Type.STRING, Type.INT) {
        @Override
        public Integer transform(PacketWrapper wrapper, String input) throws Exception {
            switch (input) {
                case "minecraft:the_nether":
                    return -1;
                default:
                case "minecraft:overworld":
                    return 0;
                case "minecraft:the_end":
                    return 1;
            }
        }
    };

    public EntityPackets1_16(Protocol1_15_2To1_16 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        // Spawn Object
        registerSpawnTrackerWithData(0x00, 0x00, Entity1_16Types.EntityType.FALLING_BLOCK, Protocol1_15_2To1_16::getNewBlockStateId);

        // Spawn mob packet
        registerSpawnTracker(0x02, 0x03);

        // Respawn
        protocol.registerOutgoing(State.PLAY, 0x3A, 0x3B, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(dimensionTransformer); // Dimension Type
                map(Type.STRING, Type.NOTHING); // Dimension
                map(Type.LONG);
                map(Type.UNSIGNED_BYTE);
                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                    clientWorld.setEnvironment(wrapper.get(Type.INT, 0));

                    wrapper.write(Type.STRING, "default"); // Level type
                    wrapper.read(Type.BOOLEAN); // Debug
                    if (wrapper.read(Type.BOOLEAN)) {
                        wrapper.set(Type.STRING, 0, "flat");
                    }
                    wrapper.read(Type.BOOLEAN); // Keep all playerdata
                });
            }
        });

        // Join Game
        protocol.registerOutgoing(State.PLAY, 0x25, 0x26, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); //  Entity ID
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.STRING_ARRAY, Type.NOTHING); // World list
                map(Type.NBT, Type.NOTHING); // whatever this is
                map(dimensionTransformer); // Dimension Type
                map(Type.STRING, Type.NOTHING); // Dimension
                map(Type.LONG); // Seed
                map(Type.UNSIGNED_BYTE); // Max players
                handler(wrapper -> {
                    ClientWorld clientChunks = wrapper.user().get(ClientWorld.class);
                    clientChunks.setEnvironment(wrapper.get(Type.INT, 1));
                    getEntityTracker(wrapper.user()).trackEntityType(wrapper.get(Type.INT, 0), Entity1_16Types.EntityType.PLAYER);

                    wrapper.write(Type.STRING, "default"); // Level type

                    wrapper.passthrough(Type.VAR_INT); // View distance
                    wrapper.passthrough(Type.BOOLEAN); // Reduced debug info
                    wrapper.passthrough(Type.BOOLEAN); // Show death screen

                    wrapper.read(Type.BOOLEAN); // Debug
                    if (wrapper.read(Type.BOOLEAN)) {
                        wrapper.set(Type.STRING, 0, "flat");
                    }
                });
            }
        });

        // Spawn Experience Orb
        registerExtraTracker(0x01, Entity1_16Types.EntityType.EXPERIENCE_ORB);

        // F Spawn Global Object, it is no longer with us :(

        // Spawn painting
        registerExtraTracker(0x03, 0x04, Entity1_16Types.EntityType.PAINTING);

        // Spawn player packet
        registerExtraTracker(0x04, 0x05, Entity1_16Types.EntityType.PLAYER);

        // Destroy entities
        registerEntityDestroy(0x37, 0x38);

        // Entity Metadata packet
        registerMetadataRewriter(0x44, 0x44, Types1_14.METADATA_LIST);

        // Entity Properties
        protocol.out(State.PLAY, 0x58, 0x59, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.passthrough(Type.VAR_INT);
                    int size = wrapper.passthrough(Type.INT);
                    for (int i = 0; i < size; i++) {
                        String attributeIdentifier = wrapper.read(Type.STRING);
                        String oldKey = BackwardsMappings.attributeMappings.get(attributeIdentifier);
                        wrapper.write(Type.STRING, oldKey != null ? oldKey : attributeIdentifier.replace("minecraft:", ""));

                        wrapper.passthrough(Type.DOUBLE);
                        int modifierSize = wrapper.passthrough(Type.VAR_INT);
                        for (int j = 0; j < modifierSize; j++) {
                            wrapper.passthrough(Type.UUID);
                            wrapper.passthrough(Type.DOUBLE);
                            wrapper.passthrough(Type.BYTE);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            MetaType type = meta.getMetaType();
            if (type == MetaType1_14.Slot) {
                meta.setValue(protocol.getBlockItemPackets().handleItemToClient((Item) meta.getValue()));
            } else if (type == MetaType1_14.BlockID) {
                meta.setValue(Protocol1_15_2To1_16.getNewBlockStateId((int) meta.getValue()));
            } else if (type == MetaType1_14.PARTICLE) {
                Particle particle = (Particle) meta.getValue();
                particle.setId(ParticleMapping.getOldId(particle.getId()));
            } else if (type == MetaType1_14.OptChat) {
                String text = meta.getCastedValue();
                if (text != null) {
                    meta.setValue(protocol.getTranslatableRewriter().processText(text));
                }
            }
            return meta;
        });

        mapEntityDirect(Entity1_16Types.EntityType.ZOMBIFIED_PIGLIN, Entity1_15Types.EntityType.ZOMBIE_PIGMAN);
        mapTypes(Entity1_16Types.EntityType.values(), Entity1_15Types.EntityType.class);

        mapEntity(Entity1_16Types.EntityType.HOGLIN, Entity1_16Types.EntityType.COW).jsonName("Hoglin");
        mapEntity(Entity1_16Types.EntityType.ZOGLIN, Entity1_16Types.EntityType.COW).jsonName("Zoglin");
        mapEntity(Entity1_16Types.EntityType.PIGLIN, Entity1_16Types.EntityType.ZOMBIFIED_PIGLIN).jsonName("Piglin");
        mapEntity(Entity1_16Types.EntityType.STRIDER, Entity1_16Types.EntityType.MAGMA_CUBE).jsonName("Strider");

        registerMetaHandler().filter(Entity1_16Types.EntityType.ZOGLIN, 16).removed();
        registerMetaHandler().filter(Entity1_16Types.EntityType.HOGLIN, 15).removed();

        registerMetaHandler().filter(Entity1_16Types.EntityType.PIGLIN, 16).removed();
        registerMetaHandler().filter(Entity1_16Types.EntityType.PIGLIN, 17).removed();
        registerMetaHandler().filter(Entity1_16Types.EntityType.PIGLIN, 18).removed();

        registerMetaHandler().filter(Entity1_16Types.EntityType.STRIDER, 15).handle(meta -> {
            boolean baby = meta.getData().getCastedValue();
            meta.getData().setValue(baby ? 1 : 3);
            meta.getData().setMetaType(MetaType1_14.VarInt);
            return meta.getData();
        });
        registerMetaHandler().filter(Entity1_16Types.EntityType.STRIDER, 16).removed();
        registerMetaHandler().filter(Entity1_16Types.EntityType.STRIDER, 17).removed();
        registerMetaHandler().filter(Entity1_16Types.EntityType.STRIDER, 18).removed();

        registerMetaHandler().filter(Entity1_16Types.EntityType.FISHING_BOBBER, 8).removed();

        registerMetaHandler().filter(Entity1_16Types.EntityType.ABSTRACT_ARROW, true, 8).removed();
        registerMetaHandler().filter(Entity1_16Types.EntityType.ABSTRACT_ARROW, true).handle(meta -> {
            if (meta.getIndex() >= 8) {
                meta.getData().setId(meta.getIndex() + 1);
            }
            return meta.getData();
        });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_16Types.getTypeFromId(typeId);
    }
}
