package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.EntityPositionHandler;
import nl.matsv.viabackwards.api.exceptions.RemovedValueException;
import nl.matsv.viabackwards.api.rewriters.LegacyEntityRewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.EntityTypeMapping;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.PaintingMapping;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.ParticleMapping;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.PlayerPositionStorage1_13;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.entities.Entity1_13Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_12;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.Particle;
import us.myles.ViaVersion.api.type.types.version.Types1_12;
import us.myles.ViaVersion.api.type.types.version.Types1_13;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;

import java.util.Optional;

public class EntityPackets1_13 extends LegacyEntityRewriter<Protocol1_12_2To1_13> {

    public EntityPackets1_13(Protocol1_12_2To1_13 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        // Player Position And Look (clientbound)
        protocol.out(State.PLAY, 0x32, 0x2F, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.FLOAT);
                map(Type.FLOAT);
                map(Type.BYTE);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        if (!ViaBackwards.getConfig().isFix1_13FacePlayer()) return;

                        PlayerPositionStorage1_13 playerStorage = wrapper.user().get(PlayerPositionStorage1_13.class);
                        byte bitField = wrapper.get(Type.BYTE, 0);
                        playerStorage.setX(toSet(bitField, 0, playerStorage.getX(), wrapper.get(Type.DOUBLE, 0)));
                        playerStorage.setY(toSet(bitField, 1, playerStorage.getY(), wrapper.get(Type.DOUBLE, 1)));
                        playerStorage.setZ(toSet(bitField, 2, playerStorage.getZ(), wrapper.get(Type.DOUBLE, 2)));
                    }

                    private double toSet(int field, int bitIndex, double origin, double packetValue) {
                        // If bit is set, coordinate is relative
                        return (field & (1 << bitIndex)) != 0 ? origin + packetValue : packetValue;
                    }
                });
            }
        });

        // Spawn Object
        protocol.out(State.PLAY, 0x00, 0x00, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.UUID);
                map(Type.BYTE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.BYTE);
                map(Type.BYTE);
                map(Type.INT);

                handler(getObjectTrackerHandler());

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Optional<Entity1_13Types.ObjectType> optionalType = Entity1_13Types.ObjectType.findById(wrapper.get(Type.BYTE, 0));
                        if (!optionalType.isPresent()) return;

                        final Entity1_13Types.ObjectType type = optionalType.get();
                        if (type == Entity1_13Types.ObjectType.FALLING_BLOCK) {
                            int blockState = wrapper.get(Type.INT, 0);
                            int combined = BlockItemPackets1_13.toOldId(blockState);
                            combined = ((combined >> 4) & 0xFFF) | ((combined & 0xF) << 12);
                            wrapper.set(Type.INT, 0, combined);
                        } else if (type == Entity1_13Types.ObjectType.ITEM_FRAME) {
                            int data = wrapper.get(Type.INT, 0);
                            switch (data) {
                                case 3:
                                    data = 0;
                                    break;
                                case 4:
                                    data = 1;
                                    break;
                                case 5:
                                    data = 3;
                                    break;
                            }
                            wrapper.set(Type.INT, 0, data);
                        } else if (type == Entity1_13Types.ObjectType.TRIDENT) {
                            wrapper.set(Type.BYTE, 0, (byte) Entity1_13Types.ObjectType.TIPPED_ARROW.getId());
                        }
                    }
                });
            }
        });

        // Spawn Experience Orb
        registerExtraTracker(0x01, Entity1_13Types.EntityType.EXPERIENCE_ORB);

        // Spawn Global Entity
        registerExtraTracker(0x02, Entity1_13Types.EntityType.LIGHTNING_BOLT);

        //Spawn Mob
        protocol.out(State.PLAY, 0x03, 0x03, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.UUID);
                map(Type.VAR_INT);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.BYTE);
                map(Type.BYTE);
                map(Type.BYTE);
                map(Type.SHORT);
                map(Type.SHORT);
                map(Type.SHORT);
                map(Types1_13.METADATA_LIST, Types1_12.METADATA_LIST);

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int type = wrapper.get(Type.VAR_INT, 1);
                        EntityType entityType = Entity1_13Types.getTypeFromId(type, false);
                        addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), entityType);

                        Optional<Integer> oldId = EntityTypeMapping.getOldId(type);
                        if (!oldId.isPresent()) {
                            if (!hasData(entityType))
                                ViaBackwards.getPlatform().getLogger().warning("Could not find 1.12 entity type for 1.13 entity type " + type + "/" + entityType);
                        } else {
                            wrapper.set(Type.VAR_INT, 1, oldId.get());
                        }
                    }
                });

                // Rewrite entity type / metadata
                handler(getMobSpawnRewriter(Types1_12.METADATA_LIST));
            }
        });

        // Spawn Player
        protocol.out(State.PLAY, 0x05, 0x05, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.UUID);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.BYTE);
                map(Type.BYTE);
                map(Types1_13.METADATA_LIST, Types1_12.METADATA_LIST);

                handler(getTrackerAndMetaHandler(Types1_12.METADATA_LIST, Entity1_13Types.EntityType.PLAYER));
            }
        });

        // Spawn Painting
        protocol.out(State.PLAY, 0x04, 0x04, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.UUID);

                handler(getTrackerHandler(Entity1_13Types.EntityType.PAINTING, Type.VAR_INT));
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int motive = wrapper.read(Type.VAR_INT);
                        String title = PaintingMapping.getStringId(motive);
                        wrapper.write(Type.STRING, title);
                    }
                });
            }
        });

        // Join game
        protocol.out(State.PLAY, 0x25, 0x23, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                handler(getTrackerHandler(Entity1_13Types.EntityType.PLAYER, Type.INT));
                handler(getDimensionHandler(1));
            }
        });

        // Respawn Packet (save dimension id)
        protocol.registerOutgoing(State.PLAY, 0x38, 0x35, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Dimension ID

                handler(getDimensionHandler(0));
                handler(wrapper -> wrapper.user().get(BackwardsBlockStorage.class).clear());
            }
        });

        // Destroy Entities Packet
        registerEntityDestroy(0x35, 0x32);

        // Entity Metadata packet
        registerMetadataRewriter(0x3F, 0x3C, Types1_13.METADATA_LIST, Types1_12.METADATA_LIST);

        // Face Player (new packet)
        protocol.out(State.PLAY, 0x31, -1, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.cancel();

                        if (!ViaBackwards.getConfig().isFix1_13FacePlayer()) return;

                        // We will just accept a possible, very minor mismatch between server and client position,
                        // and will take the server's one in both cases, else we would have to cache all entities' positions.
                        final int anchor = wrapper.read(Type.VAR_INT); // feet/eyes enum
                        final double x = wrapper.read(Type.DOUBLE);
                        final double y = wrapper.read(Type.DOUBLE);
                        final double z = wrapper.read(Type.DOUBLE);

                        PlayerPositionStorage1_13 positionStorage = wrapper.user().get(PlayerPositionStorage1_13.class);

                        // Send teleport packet to client
                        PacketWrapper positionAndLook = wrapper.create(0x2F);
                        positionAndLook.write(Type.DOUBLE, 0D);
                        positionAndLook.write(Type.DOUBLE, 0D);
                        positionAndLook.write(Type.DOUBLE, 0D);

                        //TODO properly cache and calculate head position
                        EntityPositionHandler.writeFacingDegrees(positionAndLook, positionStorage.getX(),
                                anchor == 1 ? positionStorage.getY() + 1.62 : positionStorage.getY(),
                                positionStorage.getZ(), x, y, z);

                        positionAndLook.write(Type.BYTE, (byte) 7); // bitfield, 0=absolute, 1=relative - x,y,z relative, yaw,pitch absolute
                        positionAndLook.write(Type.VAR_INT, -1);
                        positionAndLook.send(Protocol1_12_2To1_13.class, true, true);
                    }
                });
            }
        });

        if (ViaBackwards.getConfig().isFix1_13FacePlayer()) {
            PacketRemapper movementRemapper = new PacketRemapper() {
                @Override
                public void registerMap() {
                    map(Type.DOUBLE);
                    map(Type.DOUBLE);
                    map(Type.DOUBLE);
                    handler(wrapper -> wrapper.user().get(PlayerPositionStorage1_13.class).setCoordinates(wrapper, false));
                }
            };
            protocol.in(State.PLAY, 0x10, 0x0D, movementRemapper); // Player Position
            protocol.in(State.PLAY, 0x11, 0x0E, movementRemapper); // Player Position And Look (serverbound)
            protocol.in(State.PLAY, 0x13, 0x10, movementRemapper); // Vehicle Move (serverbound)
        } else {
            protocol.in(State.PLAY, 0x10, 0x0D); // Player Position
            protocol.in(State.PLAY, 0x11, 0x0E); // Player Position And Look (serverbound)
            protocol.in(State.PLAY, 0x13, 0x10); // Vehicle Move (serverbound)
        }
    }

    @Override
    protected void registerRewrites() {
        // Rewrite new Entity 'drowned'
        mapEntity(Entity1_13Types.EntityType.DROWNED, Entity1_13Types.EntityType.ZOMBIE_VILLAGER).mobName("Drowned");

        // Fishy
        mapEntity(Entity1_13Types.EntityType.COD_MOB, Entity1_13Types.EntityType.SQUID).mobName("Cod");
        mapEntity(Entity1_13Types.EntityType.SALMON, Entity1_13Types.EntityType.SQUID).mobName("Salmon");
        mapEntity(Entity1_13Types.EntityType.PUFFERFISH, Entity1_13Types.EntityType.SQUID).mobName("Puffer Fish");
        mapEntity(Entity1_13Types.EntityType.TROPICAL_FISH, Entity1_13Types.EntityType.SQUID).mobName("Tropical Fish");

        // Phantom
        mapEntity(Entity1_13Types.EntityType.PHANTOM, Entity1_13Types.EntityType.PARROT).mobName("Phantom").spawnMetadata(storage -> {
            // The phantom is grey/blue so let's do yellow/blue
            storage.add(new Metadata(15, MetaType1_12.VarInt, 3));
        });

        // Dolphin
        mapEntity(Entity1_13Types.EntityType.DOLPHIN, Entity1_13Types.EntityType.SQUID).mobName("Dolphin");

        // Turtle
        mapEntity(Entity1_13Types.EntityType.TURTLE, Entity1_13Types.EntityType.OCELOT).mobName("Turtle");

        // Rewrite Meta types
        registerMetaHandler().handle(e -> {
            Metadata meta = e.getData();
            int typeId = meta.getMetaType().getTypeID();

            // Rewrite optional chat to chat
            if (typeId == 5) {
                meta.setMetaType(MetaType1_12.String);

                if (meta.getValue() == null) {
                    meta.setValue("");
                }
            }

            // Rewrite items
            else if (typeId == 6) {
                meta.setMetaType(MetaType1_12.Slot);
                Item item = (Item) meta.getValue();
                meta.setValue(protocol.getBlockItemPackets().handleItemToClient(item));
            }

            // Discontinue particles
            else if (typeId == 15) {
                meta.setMetaType(MetaType1_12.Discontinued);
            }

            // Rewrite to 1.12 ids
            else if (typeId > 5) {
                meta.setMetaType(MetaType1_12.byId(
                        typeId - 1
                ));
            }

            return meta;
        });

        // Rewrite Custom Name from Chat to String
        registerMetaHandler().filter(Entity1_13Types.EntityType.ENTITY, true, 2).handle(e -> {
            Metadata meta = e.getData();
            String value = (String) meta.getValue();
            if (value.isEmpty()) return meta;
            meta.setValue(ChatRewriter.jsonTextToLegacy(value));
            return meta;
        });

        // Handle zombie metadata
        registerMetaHandler().filter(Entity1_13Types.EntityType.ZOMBIE, true, 15).removed();
        registerMetaHandler().filter(Entity1_13Types.EntityType.ZOMBIE, true).handle(e -> {
            Metadata meta = e.getData();

            if (meta.getId() > 15) {
                meta.setId(meta.getId() - 1);
            }

            return meta;
        });

        // Handle turtle metadata (Remove them all for now)
        registerMetaHandler().filter(Entity1_13Types.EntityType.TURTLE, 13).removed(); // Home pos
        registerMetaHandler().filter(Entity1_13Types.EntityType.TURTLE, 14).removed(); // Has egg
        registerMetaHandler().filter(Entity1_13Types.EntityType.TURTLE, 15).removed(); // Laying egg
        registerMetaHandler().filter(Entity1_13Types.EntityType.TURTLE, 16).removed(); // Travel pos
        registerMetaHandler().filter(Entity1_13Types.EntityType.TURTLE, 17).removed(); // Going home
        registerMetaHandler().filter(Entity1_13Types.EntityType.TURTLE, 18).removed(); // Traveling

        // Remove additional fish meta
        registerMetaHandler().filter(Entity1_13Types.EntityType.ABSTRACT_FISHES, true, 12).removed();
        registerMetaHandler().filter(Entity1_13Types.EntityType.ABSTRACT_FISHES, true, 13).removed();

        // Remove phantom size
        registerMetaHandler().filter(Entity1_13Types.EntityType.PHANTOM, 12).removed();

        // Remove boat splash timer
        registerMetaHandler().filter(Entity1_13Types.EntityType.BOAT, 12).removed();

        // Remove Trident special loyalty level
        registerMetaHandler().filter(Entity1_13Types.EntityType.TRIDENT, 7).removed();

        // Handle new wolf colors
        registerMetaHandler().filter(Entity1_13Types.EntityType.WOLF, 17).handle(e -> {
            Metadata meta = e.getData();

            meta.setValue(15 - (int) meta.getValue());

            return meta;
        });

        // Rewrite AreaEffectCloud
        registerMetaHandler().filter(Entity1_13Types.EntityType.AREA_EFFECT_CLOUD, 9).handle(e -> {
            Metadata meta = e.getData();
            Particle particle = (Particle) meta.getValue();

            ParticleMapping.ParticleData data = ParticleMapping.getMapping(particle.getId());

            int firstArg = 0;
            int secondArg = 0;
            int[] particleArgs = data.rewriteMeta(protocol, particle.getArguments());
            if (particleArgs != null && particleArgs.length != 0) {
                firstArg = particleArgs[0];
                secondArg = particleArgs.length == 2 ? particleArgs[1] : 0;
            }

            e.createMeta(new Metadata(9, MetaType1_12.VarInt, data.getHistoryId()));
            e.createMeta(new Metadata(10, MetaType1_12.VarInt, firstArg));
            e.createMeta(new Metadata(11, MetaType1_12.VarInt, secondArg));

            throw RemovedValueException.EX;
        });
    }

    @Override
    protected EntityType getTypeFromId(int typeId) {
        return Entity1_13Types.getTypeFromId(typeId, false);
    }

    @Override
    protected EntityType getObjectTypeFromId(final int typeId) {
        return Entity1_13Types.getTypeFromId(typeId, true);
    }

    @Override
    public int getOldEntityId(final int newId) {
        return EntityTypeMapping.getOldId(newId).orElse(newId);
    }
}
