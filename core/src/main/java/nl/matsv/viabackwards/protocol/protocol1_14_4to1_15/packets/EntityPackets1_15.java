package nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.packets;

import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.Protocol1_14_4To1_15;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.EntityTypeMapping;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.ImmediateRespawn;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.ParticleMapping;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.entities.Entity1_15Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_14;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.Particle;
import us.myles.ViaVersion.api.type.types.version.Types1_14;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;

import java.util.ArrayList;

public class EntityPackets1_15 extends EntityRewriter<Protocol1_14_4To1_15> {

    public EntityPackets1_15(Protocol1_14_4To1_15 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerOutgoing(ClientboundPackets1_15.UPDATE_HEALTH, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    float health = wrapper.passthrough(Type.FLOAT);
                    if (health > 0) return;
                    if (!wrapper.user().get(ImmediateRespawn.class).isImmediateRespawn()) return;

                    // Instantly request respawn when 1.15 gamerule is set
                    PacketWrapper statusPacket = wrapper.create(0x04);
                    statusPacket.write(Type.VAR_INT, 0);
                    statusPacket.sendToServer(Protocol1_14_4To1_15.class);
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_15.GAME_EVENT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE);
                map(Type.FLOAT);
                handler(wrapper -> {
                    if (wrapper.get(Type.UNSIGNED_BYTE, 0) == 11) {
                        wrapper.user().get(ImmediateRespawn.class).setImmediateRespawn(wrapper.get(Type.FLOAT, 0) == 1);
                    }
                });
            }
        });

        registerSpawnTrackerWithData(ClientboundPackets1_15.SPAWN_ENTITY, Entity1_15Types.EntityType.FALLING_BLOCK, Protocol1_14_4To1_15::getNewBlockStateId);

        protocol.registerOutgoing(ClientboundPackets1_15.SPAWN_MOB, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Entity UUID
                map(Type.VAR_INT); // 2 - Entity Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Yaw
                map(Type.BYTE); // 7 - Pitch
                map(Type.BYTE); // 8 - Head Pitch
                map(Type.SHORT); // 9 - Velocity X
                map(Type.SHORT); // 10 - Velocity Y
                map(Type.SHORT); // 11 - Velocity Z
                create(wrapper -> wrapper.write(Types1_14.METADATA_LIST, new ArrayList<>())); // Metadata is no longer sent in 1.15, so we have to send an empty one

                handler(wrapper -> {
                    int type = wrapper.get(Type.VAR_INT, 1);
                    Entity1_15Types.EntityType entityType = Entity1_15Types.getTypeFromId(type);
                    addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), entityType);
                    wrapper.set(Type.VAR_INT, 1, EntityTypeMapping.getOldEntityId(type));
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_15.RESPAWN, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT);
                map(Type.LONG, Type.NOTHING); // Seed
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_15.JOIN_GAME, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                map(Type.LONG, Type.NOTHING); // Seed

                map(Type.UNSIGNED_BYTE); // 3 - Max Players
                map(Type.STRING); // 4 - Level Type
                map(Type.VAR_INT); // 5 - View Distance
                map(Type.BOOLEAN); // 6 - Reduce Debug Info

                handler(getTrackerHandler(Entity1_15Types.EntityType.PLAYER, Type.INT));

                handler(wrapper -> wrapper.user().get(ImmediateRespawn.class).setImmediateRespawn(wrapper.read(Type.BOOLEAN)));
            }
        });

        registerExtraTracker(ClientboundPackets1_15.SPAWN_EXPERIENCE_ORB, Entity1_15Types.EntityType.EXPERIENCE_ORB);
        registerExtraTracker(ClientboundPackets1_15.SPAWN_GLOBAL_ENTITY, Entity1_15Types.EntityType.LIGHTNING_BOLT);
        registerExtraTracker(ClientboundPackets1_15.SPAWN_PAINTING, Entity1_15Types.EntityType.PAINTING);

        protocol.registerOutgoing(ClientboundPackets1_15.SPAWN_PLAYER, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Player UUID
                map(Type.DOUBLE); // 2 - X
                map(Type.DOUBLE); // 3 - Y
                map(Type.DOUBLE); // 4 - Z
                map(Type.BYTE); // 5 - Yaw
                map(Type.BYTE); // 6 - Pitch
                create(wrapper -> wrapper.write(Types1_14.METADATA_LIST, new ArrayList<>())); // Metadata is no longer sent in 1.15, so we have to send an empty one

                handler(getTrackerHandler(Entity1_15Types.EntityType.PLAYER, Type.VAR_INT));
            }
        });

        registerEntityDestroy(ClientboundPackets1_15.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_15.ENTITY_METADATA, Types1_14.METADATA_LIST);

        // Attributes (get rid of generic.flyingSpeed for the Bee remap)
        protocol.registerOutgoing(ClientboundPackets1_15.ENTITY_PROPERTIES, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.INT);
                handler(wrapper -> {
                    int entityId = wrapper.get(Type.VAR_INT, 0);
                    EntityType entityType = getEntityType(wrapper.user(), entityId);
                    if (entityType != Entity1_15Types.EntityType.BEE) return;

                    int size = wrapper.get(Type.INT, 0);
                    int newSize = size;
                    for (int i = 0; i < size; i++) {
                        String key = wrapper.read(Type.STRING);
                        if (key.equals("generic.flyingSpeed")) {
                            newSize--;
                            wrapper.read(Type.DOUBLE);
                            int modSize = wrapper.read(Type.VAR_INT);
                            for (int j = 0; j < modSize; j++) {
                                wrapper.read(Type.UUID);
                                wrapper.read(Type.DOUBLE);
                                wrapper.read(Type.BYTE);
                            }
                        } else {
                            wrapper.write(Type.STRING, key);
                            wrapper.passthrough(Type.DOUBLE);
                            int modSize = wrapper.passthrough(Type.VAR_INT);
                            for (int j = 0; j < modSize; j++) {
                                wrapper.passthrough(Type.UUID);
                                wrapper.passthrough(Type.DOUBLE);
                                wrapper.passthrough(Type.BYTE);
                            }
                        }
                    }

                    if (newSize != size) {
                        wrapper.set(Type.INT, 0, newSize);
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
                Item item = (Item) meta.getValue();
                meta.setValue(protocol.getBlockItemPackets().handleItemToClient(item));
            } else if (type == MetaType1_14.BlockID) {
                int blockstate = (int) meta.getValue();
                meta.setValue(Protocol1_14_4To1_15.getNewBlockStateId(blockstate));
            } else if (type == MetaType1_14.PARTICLE) {
                Particle particle = (Particle) meta.getValue();
                particle.setId(ParticleMapping.getOldId(particle.getId()));
            }
            return meta;
        });

        registerMetaHandler().filter(Entity1_15Types.EntityType.LIVINGENTITY, true).handle(e -> {
            int index = e.getIndex();
            if (index == 12) {
                throw RemovedValueException.EX;
            } else if (index > 12) {
                e.getData().setId(index - 1);
            }
            return e.getData();
        });

        registerMetaHandler().filter(Entity1_15Types.EntityType.BEE, 15).removed();
        registerMetaHandler().filter(Entity1_15Types.EntityType.BEE, 16).removed();

        mapEntity(Entity1_15Types.EntityType.BEE, Entity1_15Types.EntityType.PUFFERFISH).jsonName("Bee").spawnMetadata(storage -> {
            storage.add(new Metadata(14, MetaType1_14.Boolean, false));
            storage.add(new Metadata(15, MetaType1_14.VarInt, 2));
        });

        registerMetaHandler().filter(Entity1_15Types.EntityType.ENDERMAN, 16).removed();
        registerMetaHandler().filter(Entity1_15Types.EntityType.TRIDENT, 10).removed();

        registerMetaHandler().filter(Entity1_15Types.EntityType.WOLF).handle(e -> {
            int index = e.getIndex();
            if (index >= 17) {
                e.getData().setId(index + 1); // redundant health removed in 1.15
            }
            return e.getData();
        });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_15Types.getTypeFromId(typeId);
    }

    @Override
    public int getOldEntityId(final int newId) {
        return EntityTypeMapping.getOldEntityId(newId);
    }
}
