/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.packets;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.entities.storage.EntityPositionHandler;
import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.data.EntityTypeMapping;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.data.PaintingMapping;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.data.ParticleMapping;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage.PlayerPositionStorage1_13;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_13Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_12;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.Particle;
import com.viaversion.viaversion.api.type.types.version.Types1_12;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ServerboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ChatRewriter;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;

import java.util.Optional;

public class EntityPackets1_13 extends LegacyEntityRewriter<Protocol1_12_2To1_13> {

    public EntityPackets1_13(Protocol1_12_2To1_13 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_13.PLAYER_POSITION, new PacketRemapper() {
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

        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_ENTITY, new PacketRemapper() {
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

                        Entity1_13Types.ObjectType type = optionalType.get();
                        if (type == Entity1_13Types.ObjectType.FALLING_BLOCK) {
                            int blockState = wrapper.get(Type.INT, 0);
                            int combined = Protocol1_12_2To1_13.MAPPINGS.getNewBlockStateId(blockState);
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

        registerTracker(ClientboundPackets1_13.SPAWN_EXPERIENCE_ORB, Entity1_13Types.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_13.SPAWN_GLOBAL_ENTITY, Entity1_13Types.EntityType.LIGHTNING_BOLT);

        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_MOB, new PacketRemapper() {
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
                        tracker(wrapper.user()).addEntity(wrapper.get(Type.VAR_INT, 0), entityType);

                        int oldId = EntityTypeMapping.getOldId(type);
                        if (oldId == -1) {
                            if (!hasData(entityType)) {
                                ViaBackwards.getPlatform().getLogger().warning("Could not find 1.12 entity type for 1.13 entity type " + type + "/" + entityType);
                            }
                        } else {
                            wrapper.set(Type.VAR_INT, 1, oldId);
                        }
                    }
                });

                // Rewrite entity type / metadata
                handler(getMobSpawnRewriter(Types1_12.METADATA_LIST));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_PLAYER, new PacketRemapper() {
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

        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_PAINTING, new PacketRemapper() {
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

        registerJoinGame(ClientboundPackets1_13.JOIN_GAME, Entity1_13Types.EntityType.PLAYER);

        protocol.registerClientbound(ClientboundPackets1_13.RESPAWN, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Dimension ID

                handler(getDimensionHandler(0));
                handler(wrapper -> wrapper.user().get(BackwardsBlockStorage.class).clear());
            }
        });

        registerRemoveEntities(ClientboundPackets1_13.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_13.ENTITY_METADATA, Types1_13.METADATA_LIST, Types1_12.METADATA_LIST);

        // Face Player (new packet)
        protocol.registerClientbound(ClientboundPackets1_13.FACE_PLAYER, null, new PacketRemapper() {
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
                        PacketWrapper positionAndLook = wrapper.create(ClientboundPackets1_12_1.PLAYER_POSITION);
                        positionAndLook.write(Type.DOUBLE, 0D);
                        positionAndLook.write(Type.DOUBLE, 0D);
                        positionAndLook.write(Type.DOUBLE, 0D);

                        //TODO properly cache and calculate head position?
                        EntityPositionHandler.writeFacingDegrees(positionAndLook, positionStorage.getX(),
                                anchor == 1 ? positionStorage.getY() + 1.62 : positionStorage.getY(),
                                positionStorage.getZ(), x, y, z);

                        positionAndLook.write(Type.BYTE, (byte) 7); // bitfield, 0=absolute, 1=relative - x,y,z relative, yaw,pitch absolute
                        positionAndLook.write(Type.VAR_INT, -1);
                        positionAndLook.send(Protocol1_12_2To1_13.class);
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
            protocol.registerServerbound(ServerboundPackets1_12_1.PLAYER_POSITION, movementRemapper); // Player Position
            protocol.registerServerbound(ServerboundPackets1_12_1.PLAYER_POSITION_AND_ROTATION, movementRemapper); // Player Position And Look (serverbound)
            protocol.registerServerbound(ServerboundPackets1_12_1.VEHICLE_MOVE, movementRemapper); // Vehicle Move (serverbound)
        }
    }

    @Override
    protected void registerRewrites() {
        // Rewrite new Entity 'drowned'
        mapEntityTypeWithData(Entity1_13Types.EntityType.DROWNED, Entity1_13Types.EntityType.ZOMBIE_VILLAGER).mobName("Drowned");

        // Fishy
        mapEntityTypeWithData(Entity1_13Types.EntityType.COD, Entity1_13Types.EntityType.SQUID).mobName("Cod");
        mapEntityTypeWithData(Entity1_13Types.EntityType.SALMON, Entity1_13Types.EntityType.SQUID).mobName("Salmon");
        mapEntityTypeWithData(Entity1_13Types.EntityType.PUFFERFISH, Entity1_13Types.EntityType.SQUID).mobName("Puffer Fish");
        mapEntityTypeWithData(Entity1_13Types.EntityType.TROPICAL_FISH, Entity1_13Types.EntityType.SQUID).mobName("Tropical Fish");

        // Phantom
        mapEntityTypeWithData(Entity1_13Types.EntityType.PHANTOM, Entity1_13Types.EntityType.PARROT).mobName("Phantom").spawnMetadata(storage -> {
            // The phantom is grey/blue so let's do yellow/blue
            storage.add(new Metadata(15, MetaType1_12.VarInt, 3));
        });

        // Dolphin
        mapEntityTypeWithData(Entity1_13Types.EntityType.DOLPHIN, Entity1_13Types.EntityType.SQUID).mobName("Dolphin");

        // Turtle
        mapEntityTypeWithData(Entity1_13Types.EntityType.TURTLE, Entity1_13Types.EntityType.OCELOT).mobName("Turtle");

        // Rewrite Meta types
        filter().handler((event, meta) -> {
            int typeId = meta.metaType().typeId();

            // Rewrite optional chat to string
            if (typeId == 5) {
                // Json -> Legacy is done below
                meta.setTypeAndValue(MetaType1_12.String, meta.getValue() != null ? meta.getValue().toString() : "");
            }

            // Rewrite items
            else if (typeId == 6) {
                Item item = (Item) meta.getValue();
                meta.setTypeAndValue(MetaType1_12.Slot, protocol.getItemRewriter().handleItemToClient(item));
            }

            // Discontinue particles
            else if (typeId == 15) {
                event.cancel();
            }

            // Rewrite to 1.12 ids
            else if (typeId > 5) {
                meta.setMetaType(MetaType1_12.byId(
                        typeId - 1
                ));
            }
        });

        // Rewrite Custom Name from Chat to String
        filter().filterFamily(Entity1_13Types.EntityType.ENTITY).index(2).handler((event, meta) -> {
            String value = meta.getValue().toString();
            if (!value.isEmpty()) {
                meta.setValue(ChatRewriter.jsonToLegacyText(value));
            }
        });

        // Handle zombie metadata
        filter().filterFamily(Entity1_13Types.EntityType.ZOMBIE).removeIndex(15);

        // Handle turtle metadata (Remove them all for now)
        filter().type(Entity1_13Types.EntityType.TURTLE).cancel(13); // Home pos
        filter().type(Entity1_13Types.EntityType.TURTLE).cancel(14); // Has egg
        filter().type(Entity1_13Types.EntityType.TURTLE).cancel(15); // Laying egg
        filter().type(Entity1_13Types.EntityType.TURTLE).cancel(16); // Travel pos
        filter().type(Entity1_13Types.EntityType.TURTLE).cancel(17); // Going home
        filter().type(Entity1_13Types.EntityType.TURTLE).cancel(18); // Traveling

        // Remove additional fish meta
        filter().filterFamily(Entity1_13Types.EntityType.ABSTRACT_FISHES).cancel(12);
        filter().filterFamily(Entity1_13Types.EntityType.ABSTRACT_FISHES).cancel(13);

        // Remove phantom size
        filter().type(Entity1_13Types.EntityType.PHANTOM).cancel(12);

        // Remove boat splash timer
        filter().type(Entity1_13Types.EntityType.BOAT).cancel(12);

        // Remove Trident special loyalty level
        filter().type(Entity1_13Types.EntityType.TRIDENT).cancel(7);

        // Handle new wolf colors
        filter().type(Entity1_13Types.EntityType.WOLF).index(17).handler((event, meta) -> {
            meta.setValue(15 - (int) meta.getValue());
        });

        // Rewrite AreaEffectCloud
        filter().type(Entity1_13Types.EntityType.AREA_EFFECT_CLOUD).index(9).handler((event, meta) -> {
            Particle particle = (Particle) meta.getValue();

            ParticleMapping.ParticleData data = ParticleMapping.getMapping(particle.getId());

            int firstArg = 0;
            int secondArg = 0;
            int[] particleArgs = data.rewriteMeta(protocol, particle.getArguments());
            if (particleArgs != null && particleArgs.length != 0) {
                if (data.getHandler().isBlockHandler() && particleArgs[0] == 0) {
                    // Air doesn't have a break particle for sub 1.13 clients -> glass pane
                    particleArgs[0] = 102;
                }

                firstArg = particleArgs[0];
                secondArg = particleArgs.length == 2 ? particleArgs[1] : 0;
            }

            event.createExtraMeta(new Metadata(9, MetaType1_12.VarInt, data.getHistoryId()));
            event.createExtraMeta(new Metadata(10, MetaType1_12.VarInt, firstArg));
            event.createExtraMeta(new Metadata(11, MetaType1_12.VarInt, secondArg));

            event.cancel();
        });
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return Entity1_13Types.getTypeFromId(typeId, false);
    }

    @Override
    protected EntityType getObjectTypeFromId(final int typeId) {
        return Entity1_13Types.getTypeFromId(typeId, true);
    }

    @Override
    public int newEntityId(final int newId) {
        return EntityTypeMapping.getOldId(newId);
    }
}
