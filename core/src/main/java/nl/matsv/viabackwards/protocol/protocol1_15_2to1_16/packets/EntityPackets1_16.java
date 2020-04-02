package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.packets;

import nl.matsv.viabackwards.api.rewriters.EntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.ParticleMapping;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data.BackwardsMappings;
import us.myles.ViaVersion.api.entities.Entity1_15Types;
import us.myles.ViaVersion.api.entities.Entity1_16Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.MetaType;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_14;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.Particle;
import us.myles.ViaVersion.api.type.types.version.Types1_14;
import us.myles.ViaVersion.packets.State;

public class EntityPackets1_16 extends EntityRewriter<Protocol1_15_2To1_16> {

    public EntityPackets1_16(Protocol1_15_2To1_16 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        // Spawn Object
        registerSpawnTrackerWithData(0x00, 0x00, Entity1_16Types.EntityType.FALLING_BLOCK, Protocol1_15_2To1_16::getNewBlockStateId);

        // Spawn mob packet
        registerSpawnTracker(0x03, 0x03);

        // Respawn
        registerRespawn(0x3B, 0x3B);

        // Join Game
        registerJoinGame(0x26, 0x26, Entity1_16Types.EntityType.PLAYER);

        // Spawn Experience Orb
        registerExtraTracker(0x01, Entity1_16Types.EntityType.EXPERIENCE_ORB);

        // Spawn Global Object
        registerExtraTracker(0x02, Entity1_16Types.EntityType.LIGHTNING_BOLT);

        // Spawn painting
        registerExtraTracker(0x04, Entity1_16Types.EntityType.PAINTING);

        // Spawn player packet
        registerExtraTracker(0x05, Entity1_16Types.EntityType.PLAYER);

        // Destroy entities
        registerEntityDestroy(0x38, 0x38);

        // Entity Metadata packet
        registerMetadataRewriter(0x45, 0x44, Types1_14.METADATA_LIST);

        // Entity Properties
        protocol.out(State.PLAY, 0x59, 0x59, new PacketRemapper() {
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
            }
            return meta;
        });

        mapEntity(Entity1_16Types.EntityType.HOGLIN, Entity1_16Types.EntityType.COW).jsonName("Hoglin");
        mapEntity(Entity1_16Types.EntityType.ZOGLIN, Entity1_16Types.EntityType.COW).jsonName("Zoglin");
        mapEntity(Entity1_16Types.EntityType.PIGLIN, Entity1_16Types.EntityType.ZOMBIFIED_PIGLIN).jsonName("Piglin");
        mapEntity(Entity1_16Types.EntityType.STRIDER, Entity1_16Types.EntityType.MAGMA_CUBE)
                .spawnMetadata(storage -> storage.add(new Metadata(15, MetaType1_14.VarInt, 3))).jsonName("Strider");

        registerMetaHandler().filter(Entity1_16Types.EntityType.ZOGLIN, 16).removed();
        registerMetaHandler().filter(Entity1_16Types.EntityType.HOGLIN, 15).removed();

        registerMetaHandler().filter(Entity1_16Types.EntityType.PIGLIN, 16).removed(); // charging crossbow
        registerMetaHandler().filter(Entity1_16Types.EntityType.PIGLIN, 17).removed();

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

    @Override
    protected int getOldEntityId(int newId) {
        if (newId == Entity1_16Types.EntityType.HOGLIN.getId() || newId == Entity1_16Types.EntityType.ZOGLIN.getId()) {
            return Entity1_15Types.EntityType.COW.getId();
        } else if (newId == Entity1_16Types.EntityType.PIGLIN.getId()) {
            return Entity1_15Types.EntityType.ZOMBIE_PIGMAN.getId();
        } else if (newId == Entity1_16Types.EntityType.STRIDER.getId()) {
            return Entity1_15Types.EntityType.MAGMA_CUBE.getId();
        }
        return Protocol1_15_2To1_16.getNewEntityId(newId);
    }
}
