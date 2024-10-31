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
package com.viaversion.viabackwards.protocol.v1_13to1_12_2.rewriter;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.entities.storage.EntityPositionHandler;
import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.data.EntityIdMappings1_12_2;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.data.PaintingNames1_13;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.data.ParticleIdMappings1_12_2;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.storage.BackwardsBlockStorage;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.storage.NoteBlockStorage;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.storage.PlayerPositionStorage1_13;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_12;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_12;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ServerboundPackets1_12_1;

public class EntityPacketRewriter1_13 extends LegacyEntityRewriter<ClientboundPackets1_13, Protocol1_13To1_12_2> {

    public EntityPacketRewriter1_13(Protocol1_13To1_12_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_13.PLAYER_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.FLOAT);
                map(Types.FLOAT);
                map(Types.BYTE);
                handler(wrapper -> {
                    if (!ViaBackwards.getConfig().isFix1_13FacePlayer()) return;

                    PlayerPositionStorage1_13 playerStorage = wrapper.user().get(PlayerPositionStorage1_13.class);
                    byte bitField = wrapper.get(Types.BYTE, 0);
                    playerStorage.setX(toSet(bitField, 0, playerStorage.x(), wrapper.get(Types.DOUBLE, 0)));
                    playerStorage.setY(toSet(bitField, 1, playerStorage.y(), wrapper.get(Types.DOUBLE, 1)));
                    playerStorage.setZ(toSet(bitField, 2, playerStorage.z(), wrapper.get(Types.DOUBLE, 2)));
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.ADD_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.UUID);
                map(Types.BYTE);
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.BYTE);
                map(Types.BYTE);
                map(Types.INT);

                handler(getObjectTrackerHandler());

                handler(wrapper -> {
                    EntityTypes1_13.ObjectType type = EntityTypes1_13.ObjectType.findById(wrapper.get(Types.BYTE, 0));
                    if (type == EntityTypes1_13.ObjectType.FALLING_BLOCK) {
                        int blockState = wrapper.get(Types.INT, 0);
                        int combined = Protocol1_13To1_12_2.MAPPINGS.getNewBlockStateId(blockState);
                        combined = ((combined >> 4) & 0xFFF) | ((combined & 0xF) << 12);
                        wrapper.set(Types.INT, 0, combined);
                    } else if (type == EntityTypes1_13.ObjectType.ITEM_FRAME) {
                        int data = wrapper.get(Types.INT, 0);
                        data = switch (data) {
                            case 3 -> 0;
                            case 4 -> 1;
                            case 5 -> 3;
                            default -> data;
                        };
                        wrapper.set(Types.INT, 0, data);
                    } else if (type == EntityTypes1_13.ObjectType.TRIDENT) {
                        wrapper.set(Types.BYTE, 0, (byte) EntityTypes1_13.ObjectType.TIPPED_ARROW.getId());
                    }
                });
            }
        });

        registerTracker(ClientboundPackets1_13.ADD_EXPERIENCE_ORB, EntityTypes1_13.EntityType.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_13.ADD_GLOBAL_ENTITY, EntityTypes1_13.EntityType.LIGHTNING_BOLT);

        protocol.registerClientbound(ClientboundPackets1_13.ADD_MOB, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.UUID);
                map(Types.VAR_INT);
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.BYTE);
                map(Types.BYTE);
                map(Types.BYTE);
                map(Types.SHORT);
                map(Types.SHORT);
                map(Types.SHORT);
                map(Types1_13.ENTITY_DATA_LIST, Types1_12.ENTITY_DATA_LIST);

                handler(wrapper -> {
                    int type = wrapper.get(Types.VAR_INT, 1);
                    EntityType entityType = EntityTypes1_13.getTypeFromId(type, false);
                    tracker(wrapper.user()).addEntity(wrapper.get(Types.VAR_INT, 0), entityType);

                    int oldId = EntityIdMappings1_12_2.getOldId(type);
                    if (oldId == -1) {
                        if (!hasData(entityType)) {
                            protocol.getLogger().warning("Could not find entity type mapping " + type + "/" + entityType);
                        }
                    } else {
                        wrapper.set(Types.VAR_INT, 1, oldId);
                    }
                });

                // Rewrite entity type / ddata
                handler(getMobSpawnRewriter1_11(Types1_12.ENTITY_DATA_LIST));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.ADD_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.UUID);
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.BYTE);
                map(Types.BYTE);
                map(Types1_13.ENTITY_DATA_LIST, Types1_12.ENTITY_DATA_LIST);

                handler(getTrackerAndDataHandler(Types1_12.ENTITY_DATA_LIST, EntityTypes1_13.EntityType.PLAYER));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.ADD_PAINTING, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.UUID);

                handler(getTrackerHandler(EntityTypes1_13.EntityType.PAINTING));
                handler(wrapper -> {
                    int motive = wrapper.read(Types.VAR_INT);
                    String title = PaintingNames1_13.getStringId(motive);
                    wrapper.write(Types.STRING, title);
                });
            }
        });

        registerJoinGame(ClientboundPackets1_13.LOGIN, EntityTypes1_13.EntityType.PLAYER);

        protocol.registerClientbound(ClientboundPackets1_13.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Dimension ID

                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().getClientWorld(Protocol1_13To1_12_2.class);
                    int dimensionId = wrapper.get(Types.INT, 0);

                    if (clientWorld.setEnvironment(dimensionId)) {
                        tracker(wrapper.user()).clearEntities();
                        wrapper.user().get(BackwardsBlockStorage.class).clear();
                        wrapper.user().get(NoteBlockStorage.class).clear();
                    }
                });
            }
        });

        registerRemoveEntities(ClientboundPackets1_13.REMOVE_ENTITIES);
        registerSetEntityData(ClientboundPackets1_13.SET_ENTITY_DATA, Types1_13.ENTITY_DATA_LIST, Types1_12.ENTITY_DATA_LIST);

        // Face Player (new packet)
        protocol.registerClientbound(ClientboundPackets1_13.PLAYER_LOOK_AT, null, wrapper -> {
            wrapper.cancel();

            if (!ViaBackwards.getConfig().isFix1_13FacePlayer()) return;

            // We will just accept a possible, very minor mismatch between server and client position,
            // and will take the server's one in both cases, else we would have to cache all entities' positions.
            final int anchor = wrapper.read(Types.VAR_INT); // feet/eyes enum
            final double x = wrapper.read(Types.DOUBLE);
            final double y = wrapper.read(Types.DOUBLE);
            final double z = wrapper.read(Types.DOUBLE);

            PlayerPositionStorage1_13 positionStorage = wrapper.user().get(PlayerPositionStorage1_13.class);

            // Send teleport packet to client
            PacketWrapper positionAndLook = wrapper.create(ClientboundPackets1_12_1.PLAYER_POSITION);
            positionAndLook.write(Types.DOUBLE, 0D);
            positionAndLook.write(Types.DOUBLE, 0D);
            positionAndLook.write(Types.DOUBLE, 0D);

            //TODO properly cache and calculate head position?
            EntityPositionHandler.writeFacingDegrees(positionAndLook, positionStorage.x(),
                anchor == 1 ? positionStorage.y() + 1.62 : positionStorage.y(),
                positionStorage.z(), x, y, z);

            positionAndLook.write(Types.BYTE, (byte) 7); // bitfield, 0=absolute, 1=relative - x,y,z relative, yaw,pitch absolute
            positionAndLook.write(Types.VAR_INT, -1);
            positionAndLook.send(Protocol1_13To1_12_2.class);
        });

        if (ViaBackwards.getConfig().isFix1_13FacePlayer()) {
            PacketHandler movementRemapper = wrapper -> {
                final double x = wrapper.passthrough(Types.DOUBLE);
                final double y = wrapper.passthrough(Types.DOUBLE);
                final double z = wrapper.passthrough(Types.DOUBLE);
                wrapper.user().get(PlayerPositionStorage1_13.class).setPosition(x, y, z);
            };
            protocol.registerServerbound(ServerboundPackets1_12_1.MOVE_PLAYER_POS, movementRemapper); // Player Position
            protocol.registerServerbound(ServerboundPackets1_12_1.MOVE_PLAYER_POS_ROT, movementRemapper); // Player Position And Look (serverbound)
            protocol.registerServerbound(ServerboundPackets1_12_1.MOVE_VEHICLE, movementRemapper); // Vehicle Move (serverbound)
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
        mapEntityTypeWithData(EntityTypes1_13.EntityType.PHANTOM, EntityTypes1_13.EntityType.PARROT).plainName().spawnEntityData(storage -> {
            // The phantom is grey/blue so let's do yellow/blue
            storage.add(new EntityData(15, EntityDataTypes1_12.VAR_INT, 3));
        });

        // Dolphin
        mapEntityTypeWithData(EntityTypes1_13.EntityType.DOLPHIN, EntityTypes1_13.EntityType.SQUID).plainName();

        // Turtle
        mapEntityTypeWithData(EntityTypes1_13.EntityType.TURTLE, EntityTypes1_13.EntityType.OCELOT).plainName();

        // Rewrite Data types
        filter().handler((event, data) -> {
            int typeId = data.dataType().typeId();
            if (typeId == 4) {
                JsonElement element = data.value();
                protocol.translatableRewriter().processText(event.user(), element);
                data.setDataType(EntityDataTypes1_12.COMPONENT);
            } else if (typeId == 5) {
                // Rewrite optional chat to string
                JsonElement element = data.value();
                data.setTypeAndValue(EntityDataTypes1_12.STRING, protocol.jsonToLegacy(event.user(), element));
            } else if (typeId == 6) {
                Item item = (Item) data.getValue();
                data.setTypeAndValue(EntityDataTypes1_12.ITEM, protocol.getItemRewriter().handleItemToClient(event.user(), item));
            } else if (typeId == 15) {
                // Discontinue particles
                event.cancel();
            } else {
                data.setDataType(EntityDataTypes1_12.byId(typeId > 5 ? typeId - 1 : typeId));
            }
        });

        // Handle zombie entity data
        filter().type(EntityTypes1_13.EntityType.ZOMBIE).removeIndex(15);

        // Handle turtle entity data (Remove them all for now)
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(13); // Home pos
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(14); // Has egg
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(15); // Laying egg
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(16); // Travel pos
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(17); // Going home
        filter().type(EntityTypes1_13.EntityType.TURTLE).cancel(18); // Traveling

        // Remove additional fish data
        filter().type(EntityTypes1_13.EntityType.ABSTRACT_FISH).cancel(12);
        filter().type(EntityTypes1_13.EntityType.ABSTRACT_FISH).cancel(13);

        // Remove phantom size
        filter().type(EntityTypes1_13.EntityType.PHANTOM).cancel(12);

        // Remove boat splash timer
        filter().type(EntityTypes1_13.EntityType.BOAT).cancel(12);

        // Remove Trident special loyalty level
        filter().type(EntityTypes1_13.EntityType.TRIDENT).cancel(7);

        // Handle new wolf colors
        filter().type(EntityTypes1_13.EntityType.WOLF).index(17).handler((event, data) -> {
            data.setValue(15 - (int) data.getValue());
        });

        // Rewrite AreaEffectCloud
        filter().type(EntityTypes1_13.EntityType.AREA_EFFECT_CLOUD).index(9).handler((event, data) -> {
            Particle particle = (Particle) data.getValue();

            ParticleIdMappings1_12_2.ParticleData particleData = ParticleIdMappings1_12_2.getMapping(particle.id());

            int firstArg = 0;
            int secondArg = 0;
            int[] particleArgs = particleData.rewriteMeta(protocol, particle.getArguments());
            if (particleArgs != null && particleArgs.length != 0) {
                if (particleData.getHandler().isBlockHandler() && particleArgs[0] == 0) {
                    // Air doesn't have a break particle for sub 1.13 clients -> glass pane
                    particleArgs[0] = 102;
                }

                firstArg = particleArgs[0];
                secondArg = particleArgs.length == 2 ? particleArgs[1] : 0;
            }

            event.createExtraData(new EntityData(9, EntityDataTypes1_12.VAR_INT, particleData.getHistoryId()));
            event.createExtraData(new EntityData(10, EntityDataTypes1_12.VAR_INT, firstArg));
            event.createExtraData(new EntityData(11, EntityDataTypes1_12.VAR_INT, secondArg));

            event.cancel();
        });
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_13.getTypeFromId(typeId, false);
    }

    @Override
    public EntityType objectTypeFromId(int typeId) {
        return EntityTypes1_13.getTypeFromId(typeId, true);
    }

    @Override
    public int newEntityId(final int newId) {
        return EntityIdMappings1_12_2.getOldId(newId);
    }

    private static double toSet(int field, int bitIndex, double origin, double packetValue) {
        // If bit is set, coordinate is relative
        return (field & (1 << bitIndex)) != 0 ? origin + packetValue : packetValue;
    }
}
