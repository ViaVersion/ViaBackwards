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
package com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.packets;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.entities.storage.EntityData;
import com.viaversion.viabackwards.api.entities.storage.EntityPositionHandler;
import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.storage.ChunkLightStorage;
import com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.storage.DifficultyStorage;
import com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.storage.EntityPositionStorage1_14;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.VillagerData;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_14;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_13_2;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;
import com.viaversion.viaversion.rewriter.meta.MetaHandler;

public class EntityPackets1_14 extends LegacyEntityRewriter<ClientboundPackets1_14, Protocol1_13_2To1_14> {

    private EntityPositionHandler positionHandler;

    public EntityPackets1_14(Protocol1_13_2To1_14 protocol) {
        super(protocol, Types1_13_2.META_TYPES.optionalComponentType, Types1_13_2.META_TYPES.booleanType);
    }

    //TODO work the method into this class alone
    @Override
    protected void addTrackedEntity(PacketWrapper wrapper, int entityId, EntityType type) throws Exception {
        super.addTrackedEntity(wrapper, entityId, type);

        // Cache the position for every newly tracked entity
        if (type == EntityTypes1_14.PAINTING) {
            final Position position = wrapper.get(Type.POSITION1_8, 0);
            positionHandler.cacheEntityPosition(wrapper, position.x(), position.y(), position.z(), true, false);
        } else if (wrapper.getId() != ClientboundPackets1_14.JOIN_GAME.getId()) { // ignore join game
            positionHandler.cacheEntityPosition(wrapper, true, false);
        }
    }

    @Override
    protected void registerPackets() {
        positionHandler = new EntityPositionHandler(this, EntityPositionStorage1_14.class, EntityPositionStorage1_14::new);

        protocol.registerClientbound(ClientboundPackets1_14.ENTITY_STATUS, wrapper -> {
            int entityId = wrapper.passthrough(Type.INT);
            byte status = wrapper.passthrough(Type.BYTE);
            // Check for death status
            if (status != 3) return;

            EntityTracker tracker = tracker(wrapper.user());
            EntityType entityType = tracker.entityType(entityId);
            if (entityType != EntityTypes1_14.PLAYER) return;

            // Remove equipment, else the client will see ghost items
            for (int i = 0; i <= 5; i++) {
                PacketWrapper equipmentPacket = wrapper.create(ClientboundPackets1_13.ENTITY_EQUIPMENT);
                equipmentPacket.write(Type.VAR_INT, entityId);
                equipmentPacket.write(Type.VAR_INT, i);
                equipmentPacket.write(Type.ITEM1_13_2, null);
                equipmentPacket.send(Protocol1_13_2To1_14.class);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.ENTITY_TELEPORT, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                handler(wrapper -> positionHandler.cacheEntityPosition(wrapper, false, false));
            }
        });

        PacketHandlers relativeMoveHandler = new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.SHORT);
                map(Type.SHORT);
                map(Type.SHORT);
                handler(wrapper -> {
                    double x = wrapper.get(Type.SHORT, 0) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
                    double y = wrapper.get(Type.SHORT, 1) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
                    double z = wrapper.get(Type.SHORT, 2) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
                    positionHandler.cacheEntityPosition(wrapper, x, y, z, false, true);
                });
            }
        };
        protocol.registerClientbound(ClientboundPackets1_14.ENTITY_POSITION, relativeMoveHandler);
        protocol.registerClientbound(ClientboundPackets1_14.ENTITY_POSITION_AND_ROTATION, relativeMoveHandler);

        protocol.registerClientbound(ClientboundPackets1_14.SPAWN_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.UUID); // 1 - UUID
                map(Type.VAR_INT, Type.BYTE); // 2 - Type
                map(Type.DOUBLE); // 3 - X
                map(Type.DOUBLE); // 4 - Y
                map(Type.DOUBLE); // 5 - Z
                map(Type.BYTE); // 6 - Pitch
                map(Type.BYTE); // 7 - Yaw
                map(Type.INT); // 8 - Data
                map(Type.SHORT); // 9 - Velocity X
                map(Type.SHORT); // 10 - Velocity Y
                map(Type.SHORT); // 11 - Velocity Z

                handler(getObjectTrackerHandler());

                handler(wrapper -> {
                    int id = wrapper.get(Type.BYTE, 0);
                    int mappedId = newEntityId(id);
                    EntityTypes1_13.EntityType entityType = EntityTypes1_13.getTypeFromId(mappedId, false);
                    EntityTypes1_13.ObjectType objectType;
                    if (entityType.isOrHasParent(EntityTypes1_13.EntityType.MINECART_ABSTRACT)) {
                        objectType = EntityTypes1_13.ObjectType.MINECART;
                        int data = 0;
                        switch (entityType) {
                            case CHEST_MINECART:
                                data = 1;
                                break;
                            case FURNACE_MINECART:
                                data = 2;
                                break;
                            case TNT_MINECART:
                                data = 3;
                                break;
                            case SPAWNER_MINECART:
                                data = 4;
                                break;
                            case HOPPER_MINECART:
                                data = 5;
                                break;
                            case COMMAND_BLOCK_MINECART:
                                data = 6;
                                break;
                        }
                        if (data != 0)
                            wrapper.set(Type.INT, 0, data);
                    } else {
                        objectType = EntityTypes1_13.ObjectType.fromEntityType(entityType).orElse(null);
                    }

                    if (objectType == null) return;

                    wrapper.set(Type.BYTE, 0, (byte) objectType.getId());

                    int data = wrapper.get(Type.INT, 0);
                    if (objectType == EntityTypes1_13.ObjectType.FALLING_BLOCK) {
                        int blockState = wrapper.get(Type.INT, 0);
                        int combined = protocol.getMappingData().getNewBlockStateId(blockState);
                        wrapper.set(Type.INT, 0, combined);
                    } else if (entityType.isOrHasParent(EntityTypes1_13.EntityType.ABSTRACT_ARROW)) {
                        wrapper.set(Type.INT, 0, data + 1);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.SPAWN_MOB, new PacketHandlers() {
            @Override
            public void register() {
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
                map(Types1_14.METADATA_LIST, Types1_13_2.METADATA_LIST); // 12 - Metadata

                handler(wrapper -> {
                    int type = wrapper.get(Type.VAR_INT, 1);
                    EntityType entityType = EntityTypes1_14.getTypeFromId(type);
                    addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), entityType);

                    int oldId = newEntityId(type);
                    if (oldId == -1) {
                        EntityData entityData = entityDataForType(entityType);
                        if (entityData == null) {
                            ViaBackwards.getPlatform().getLogger().warning("Could not find 1.13.2 entity type for 1.14 entity type " + type + "/" + entityType);
                            wrapper.cancel();
                        } else {
                            wrapper.set(Type.VAR_INT, 1, entityData.replacementId());
                        }
                    } else {
                        wrapper.set(Type.VAR_INT, 1, oldId);
                    }
                });

                // Handle entity type & metadata
                handler(getMobSpawnRewriter(Types1_13_2.METADATA_LIST));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.SPAWN_EXPERIENCE_ORB, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.DOUBLE); // Needs to be mapped for the position cache
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                handler(wrapper -> addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), EntityTypes1_14.EXPERIENCE_ORB));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.SPAWN_GLOBAL_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity id
                map(Type.BYTE);
                map(Type.DOUBLE); // Needs to be mapped for the position cache
                map(Type.DOUBLE);
                map(Type.DOUBLE);
                handler(wrapper -> addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), EntityTypes1_14.LIGHTNING_BOLT));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.SPAWN_PAINTING, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.UUID);
                map(Type.VAR_INT);
                map(Type.POSITION1_14, Type.POSITION1_8);
                map(Type.BYTE);

                // Track entity
                handler(wrapper -> addTrackedEntity(wrapper, wrapper.get(Type.VAR_INT, 0), EntityTypes1_14.PAINTING));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.SPAWN_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.UUID); // 1 - Player UUID
                map(Type.DOUBLE); // 2 - X
                map(Type.DOUBLE); // 3 - Y
                map(Type.DOUBLE); // 4 - Z
                map(Type.BYTE); // 5 - Yaw
                map(Type.BYTE); // 6 - Pitch
                map(Types1_14.METADATA_LIST, Types1_13_2.METADATA_LIST); // 7 - Metadata

                handler(getTrackerAndMetaHandler(Types1_13_2.METADATA_LIST, EntityTypes1_14.PLAYER));
                handler(wrapper -> positionHandler.cacheEntityPosition(wrapper, true, false));
            }
        });

        registerRemoveEntities(ClientboundPackets1_14.DESTROY_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_14.ENTITY_METADATA, Types1_14.METADATA_LIST, Types1_13_2.METADATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_14.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // 0 - Entity ID
                map(Type.UNSIGNED_BYTE); // 1 - Gamemode
                map(Type.INT); // 2 - Dimension

                handler(getTrackerHandler(EntityTypes1_14.PLAYER, Type.INT));
                handler(getDimensionHandler(1));
                handler(wrapper -> {
                    short difficulty = wrapper.user().get(DifficultyStorage.class).getDifficulty();
                    wrapper.write(Type.UNSIGNED_BYTE, difficulty);

                    wrapper.passthrough(Type.UNSIGNED_BYTE); // Max Players
                    wrapper.passthrough(Type.STRING); // Level Type
                    wrapper.read(Type.VAR_INT); // Read View Distance

                    //TODO Track client position
                    // Manually add position storage
                    /*int entitiyId = wrapper.get(Type.INT, 0);
                    StoredEntityData storedEntity = protocol.getEntityRewriter().tracker(wrapper.user()).entityData(entitiyId);
                    storedEntity.put(new EntityPositionStorage1_14());*/
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // 0 - Dimension ID

                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                    int dimensionId = wrapper.get(Type.INT, 0);
                    clientWorld.setEnvironment(dimensionId);

                    short difficulty = wrapper.user().get(DifficultyStorage.class).getDifficulty();
                    wrapper.write(Type.UNSIGNED_BYTE, difficulty);

                    wrapper.user().get(ChunkLightStorage.class).clear();
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        mapTypes(EntityTypes1_14.values(), EntityTypes1_13.EntityType.class);

        mapEntityTypeWithData(EntityTypes1_14.CAT, EntityTypes1_14.OCELOT).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.TRADER_LLAMA, EntityTypes1_14.LLAMA).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.FOX, EntityTypes1_14.WOLF).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.PANDA, EntityTypes1_14.POLAR_BEAR).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.PILLAGER, EntityTypes1_14.VILLAGER).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.WANDERING_TRADER, EntityTypes1_14.VILLAGER).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.RAVAGER, EntityTypes1_14.COW).jsonName();

        filter().handler((event, meta) -> {
            int typeId = meta.metaType().typeId();
            if (typeId <= 15) {
                meta.setMetaType(Types1_13_2.META_TYPES.byId(typeId));
            }
        });

        registerMetaTypeHandler(Types1_13_2.META_TYPES.itemType, Types1_13_2.META_TYPES.blockStateType, null, null,
            Types1_13_2.META_TYPES.componentType, Types1_13_2.META_TYPES.optionalComponentType);

        filter().type(EntityTypes1_14.PILLAGER).cancel(15);

        filter().type(EntityTypes1_14.FOX).cancel(15);
        filter().type(EntityTypes1_14.FOX).cancel(16);
        filter().type(EntityTypes1_14.FOX).cancel(17);
        filter().type(EntityTypes1_14.FOX).cancel(18);

        filter().type(EntityTypes1_14.PANDA).cancel(15);
        filter().type(EntityTypes1_14.PANDA).cancel(16);
        filter().type(EntityTypes1_14.PANDA).cancel(17);
        filter().type(EntityTypes1_14.PANDA).cancel(18);
        filter().type(EntityTypes1_14.PANDA).cancel(19);
        filter().type(EntityTypes1_14.PANDA).cancel(20);

        filter().type(EntityTypes1_14.CAT).cancel(18);
        filter().type(EntityTypes1_14.CAT).cancel(19);
        filter().type(EntityTypes1_14.CAT).cancel(20);

        filter().handler((event, meta) -> {
            EntityType type = event.entityType();
            if (type == null) return;
            if (type.isOrHasParent(EntityTypes1_14.ABSTRACT_ILLAGER_BASE) || type == EntityTypes1_14.RAVAGER || type == EntityTypes1_14.WITCH) {
                int index = event.index();
                if (index == 14) {
                    event.cancel();
                } else if (index > 14) {
                    event.setIndex(index - 1);
                }
            }
        });

        filter().type(EntityTypes1_14.AREA_EFFECT_CLOUD).index(10).handler((event, meta) -> {
            rewriteParticle((Particle) meta.getValue());
        });

        filter().type(EntityTypes1_14.FIREWORK_ROCKET).index(8).handler((event, meta) -> {
            meta.setMetaType(Types1_13_2.META_TYPES.varIntType);
            Integer value = (Integer) meta.getValue();
            if (value == null) {
                meta.setValue(0);
            }
        });

        filter().type(EntityTypes1_14.ABSTRACT_ARROW).removeIndex(9);

        filter().type(EntityTypes1_14.VILLAGER).cancel(15); // Head shake timer

        MetaHandler villagerDataHandler = (event, meta) -> {
            VillagerData villagerData = (VillagerData) meta.getValue();
            meta.setTypeAndValue(Types1_13_2.META_TYPES.varIntType, villagerDataToProfession(villagerData));
            if (meta.id() == 16) {
                event.setIndex(15); // decreased by 2 again in one of the following handlers
            }
        };

        filter().type(EntityTypes1_14.ZOMBIE_VILLAGER).index(18).handler(villagerDataHandler);
        filter().type(EntityTypes1_14.VILLAGER).index(16).handler(villagerDataHandler);

        // Holding arms up - from bitfield into own boolean
        filter().type(EntityTypes1_14.ABSTRACT_SKELETON).index(13).handler((event, meta) -> {
            byte value = (byte) meta.getValue();
            if ((value & 4) != 0) {
                event.createExtraMeta(new Metadata(14, Types1_13_2.META_TYPES.booleanType, true));
            }
        });
        filter().type(EntityTypes1_14.ZOMBIE).index(13).handler((event, meta) -> {
            byte value = (byte) meta.getValue();
            if ((value & 4) != 0) {
                event.createExtraMeta(new Metadata(16, Types1_13_2.META_TYPES.booleanType, true));
            }
        });

        filter().type(EntityTypes1_14.ZOMBIE).addIndex(16);

        // Remove bed location
        filter().type(EntityTypes1_14.LIVINGENTITY).handler((event, meta) -> {
            int index = event.index();
            if (index == 12) {
                Position position = (Position) meta.getValue();
                if (position != null) {
                    // Use bed
                    PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_13.USE_BED, null, event.user());
                    wrapper.write(Type.VAR_INT, event.entityId());
                    wrapper.write(Type.POSITION1_8, position);

                    try {
                        wrapper.scheduleSend(Protocol1_13_2To1_14.class);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                event.cancel();
            } else if (index > 12) {
                event.setIndex(index - 1);
            }
        });

        // Pose
        filter().removeIndex(6);

        filter().type(EntityTypes1_14.OCELOT).index(13).handler((event, meta) -> {
            event.setIndex(15);
            meta.setTypeAndValue(Types1_13_2.META_TYPES.varIntType, 0);
        });

        filter().type(EntityTypes1_14.CAT).handler((event, meta) -> {
            if (event.index() == 15) {
                meta.setValue(1);
            } else if (event.index() == 13) {
                meta.setValue((byte) ((byte) meta.getValue() & 0x4));
            }
        });

        filter().handler((event, meta) -> {
            if (meta.metaType().typeId() > 15) {
                throw new IllegalArgumentException("Unhandled metadata: " + meta);
            }
        });
    }

    public int villagerDataToProfession(VillagerData data) {
        switch (data.profession()) {
            case 1: // Armorer
            case 10: // Mason
            case 13: // Toolsmith
            case 14: // Weaponsmith
                return 3; // Blacksmith
            case 2: // Butcher
            case 8: // Leatherworker
                return 4; // Butcher
            case 3: // Cartographer
            case 9: // Librarian
                return 1; // Librarian
            case 4: // Cleric
                return 2; // Priest
            case 5: // Farmer
            case 6: // Fisherman
            case 7: // Fletcher
            case 12: // Shepherd
                return 0; // Farmer
            case 0: // None
            case 11: // Nitwit
            default:
                return 5; // Nitwit
        }
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_14.getTypeFromId(typeId);
    }
}
