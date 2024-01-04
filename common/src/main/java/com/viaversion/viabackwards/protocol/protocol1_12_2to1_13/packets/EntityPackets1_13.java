/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_12;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_12;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ServerboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import java.util.Optional;

public class EntityPackets1_13 extends LegacyEntityRewriter<ClientboundPackets1_13, Protocol1_12_2To1_13> {

    public EntityPackets1_13(Protocol1_12_2To1_13 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_13.PLAYER_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.FLOAT);
                map(Type.FLOAT);
                map(Type.BYTE);
                handler(wrapper -> {
                    if (!ViaBackwards.getConfig().isFix1_13FacePlayer()) return;

                    PlayerPositionStorage1_13 playerStorage = wrapper.user().get(PlayerPositionStorage1_13.class);
                    byte bitField = wrapper.get(Type.BYTE, 0);
                    playerStorage.setX(toSet(bitField, 0, playerStorage.getX(), wrapper.get(Type.DOUBLE, 0)));
                    playerStorage.setY(toSet(bitField, 1, playerStorage.getY(), wrapper.get(Type.DOUBLE, 1)));
                    playerStorage.setZ(toSet(bitField, 2, playerStorage.getZ(), wrapper.get(Type.DOUBLE, 2)));
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
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

                handler(wrapper -> {
                    Optional<EntityTypes1_13.ObjectType> optionalType = EntityTypes1_13.ObjectType.findById(wrapper.get(Type.BYTE, 0));
                    if (!optionalType.isPresent()) return;

                    EntityTypes1_13.ObjectType type = optionalType.get();
                    if (type == EntityTypes1_13.ObjectType.FALLING_BLOCK) {
                        int blockState = wrapper.get(Type.INT, 0);
                        int combined = Protocol1_12_2To1_13.MAPPINGS.getNewBlockStateId(blockState);
                        combined = ((combined >> 4) & 0xFFF) | ((combined & 0xF) << 12);
                        wrapper.set(Type.INT, 0, combined);
                    } else if (type == EntityTypes1_13.ObjectType.ITEM_FRAME) {
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
                    } else if (type == EntityTypes1_13.ObjectType.TRIDENT) {
                        wrapper.set(Type.BYTE, 0, (byte) EntityTypes1_13.ObjectType.TIPPED_ARROW.getId());
                    }
                });
            }
        });

        registerTracker(ClientboundPackets1_13.SPAWN_EXPERIENCE_ORB, EntityTypes1_13.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_13.SPAWN_GLOBAL_ENTITY, EntityTypes1_13.EntityType.LIGHTNING_BOLT);

        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_MOB, new PacketHandlers() {
            @Override
            public void register() {
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

                handler(wrapper -> {
                    int type = wrapper.get(Type.VAR_INT, 1);
                    EntityType entityType = EntityTypes1_13.getTypeFromId(type, false);
                    tracker(wrapper.user()).addEntity(wrapper.get(Type.VAR_INT, 0), entityType);

                    int oldId = EntityTypeMapping.getOldId(type);
                    if (oldId == -1) {
                        if (!hasData(entityType)) {
                            ViaBackwards.getPlatform().getLogger().warning("Could not find 1.12 entity type for 1.13 entity type " + type + "/" + entityType);
                        }
                    } else {
                        wrapper.set(Type.VAR_INT, 1, oldId);
                    }
                });

                // Rewrite entity type / metadata
                handler(getMobSpawnRewriter(Types1_12.METADATA_LIST));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.UUID);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.BYTE);
                map(Type.BYTE);
                map(Types1_13.METADATA_LIST, Types1_12.METADATA_LIST);

                handler(getTrackerAndMetaHandler(Types1_12.METADATA_LIST, EntityTypes1_13.EntityType.PLAYER));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_PAINTING, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.UUID);

                handler(getTrackerHandler(EntityTypes1_13.EntityType.PAINTING, Type.VAR_INT));
                handler(wrapper -> {
                    int motive = wrapper.read(Type.VAR_INT);
                    String title = PaintingMapping.getStringId(motive);
                    wrapper.write(Type.STRING, title);
                });
            }
        });

        registerJoinGame(ClientboundPackets1_13.JOIN_GAME, EntityTypes1_13.EntityType.PLAYER);

        protocol.registerClientbound(ClientboundPackets1_13.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // 0 - Dimension ID

                handler(getDimensionHandler(0));
                handler(wrapper -> wrapper.user().get(BackwardsBlockStorage.class).clear());
            }
        });

        registerRemoveEntities(ClientboundPackets1_13.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_13.ENTITY_METADATA, Types1_13.METADATA_LIST, Types1_12.METADATA_LIST);

        // Face Player (new packet)
        protocol.registerClientbound(ClientboundPackets1_13.FACE_PLAYER, null, wrapper -> {
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
        });

        if (ViaBackwards.getConfig().isFix1_13FacePlayer()) {
            PacketHandlers movementRemapper = new PacketHandlers() {
                @Override
                public void register() {
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
        mapEntityTypeWithData(EntityTypes1_13.EntityType.DROWNED, EntityTypes1_13.EntityType.ZOMBIE_VILLAGER).plainName();

        // Fishy
        mapEntityTypeWithData(EntityTypes1_13.EntityType.COD, EntityTypes1_13.EntityType.SQUID).plainName();
        mapEntityTypeWithData(EntityTypes1_13.EntityType.SALMON, EntityTypes1_13.EntityType.SQUID).plainName();
        mapEntityTypeWithData(EntityTypes1_13.EntityType.PUFFERFISH, EntityTypes1_13.EntityType.SQUID).plainName();
        mapEntityTypeWithData(EntityTypes1_13.EntityType.TROPICAL_FISH, EntityTypes1_13.EntityType.SQUID).plainName();

        // Phantom
        mapEntityTypeWithData(EntityTypes1_13.EntityType.PHANTOM, EntityTypes1_13.EntityType.PARROT).plainName().spawnMetadata(storage -> {
            // The phantom is grey/blue so let's do yellow/blue
            storage.add(new Metadata(15, MetaType1_12.VarInt, 3));
        });

        // Dolphin
        mapEntityTypeWithData(EntityTypes1_13.EntityType.DOLPHIN, EntityTypes1_13.EntityType.SQUID).plainName();

        // Turtle
        mapEntityTypeWithData(EntityTypes1_13.EntityType.TURTLE, EntityTypes1_13.EntityType.OCELOT).plainName();

        // Rewrite Meta types
        filter().handler((event, meta) -> {
            int typeId = meta.metaType().typeId();
            if (typeId == 4) {
                JsonElement element = meta.value();
                protocol.translatableRewriter().processText(element);
                meta.setMetaType(MetaType1_12.Chat);
            } else if (typeId == 5) {
                // Rewrite optional chat to string
                JsonElement element = meta.value();
                meta.setTypeAndValue(MetaType1_12.String, protocol.jsonToLegacy(element));
            } else if (typeId == 6) {
                Item item = (Item) meta.getValue();
                meta.setTypeAndValue(MetaType1_12.Slot, protocol.getItemRewriter().handleItemToClient(item));
            } else if (typeId == 15) {
                // Discontinue particles
                event.cancel();
            } else {
                meta.setMetaType(MetaType1_12.byId(typeId > 5 ? typeId - 1 : typeId));
            }
        });

        // Handle zombie metadata
        filter().filterFamily(EntityTypes1_13.EntityType.ZOMBIE).removeIndex(15);

        // Handle turtle metadata (Remove them all for now)
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(13); // Home pos
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(14); // Has egg
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(15); // Laying egg
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(16); // Travel pos
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(17); // Going home
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(18); // Traveling

        // Remove additional fish meta
        filter().filterFamily(EntityTypes1_13.EntityType.ABSTRACT_FISHES).cancel(12);
        filter().filterFamily(EntityTypes1_13.EntityType.ABSTRACT_FISHES).cancel(13);

        // Remove phantom size
        filter().type(EntityTypes1_13.EntityType.PHANTOM).cancel(12);

        // Remove boat splash timer
        filter().type(EntityTypes1_13.EntityType.BOAT).cancel(12);

        // Remove Trident special loyalty level
        filter().type(EntityTypes1_13.EntityType.TRIDENT).cancel(7);

        // Handle new wolf colors
        filter().type(EntityTypes1_13.EntityType.WOLF).index(17).handler((event, meta) -> {
            meta.setValue(15 - (int) meta.getValue());
        });

        // Rewrite AreaEffectCloud
        filter().type(EntityTypes1_13.EntityType.AREA_EFFECT_CLOUD).index(9).handler((event, meta) -> {
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
        return EntityTypes1_13.getTypeFromId(typeId, false);
    }

    @Override
    protected EntityType getObjectTypeFromId(final int typeId) {
        return EntityTypes1_13.getTypeFromId(typeId, true);
    }

    @Override
    public int newEntityId(final int newId) {
        return EntityTypeMapping.getOldId(newId);
    }

    private static double toSet(int field, int bitIndex, double origin, double packetValue) {
        // If bit is set, coordinate is relative
        return (field & (1 << bitIndex)) != 0 ? origin + packetValue : packetValue;
    }
}
