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
package com.viaversion.viabackwards.protocol.v1_14to1_13_2.rewriter;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.entities.storage.EntityData;
import com.viaversion.viabackwards.api.entities.storage.EntityPositionHandler;
import com.viaversion.viabackwards.api.rewriters.LegacyEntityRewriter;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.Protocol1_14To1_13_2;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.ChunkLightStorage;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.DifficultyStorage;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.EntityPositionStorage1_14;
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
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_13_2;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;
import com.viaversion.viaversion.rewriter.meta.MetaHandler;

public class EntityPacketRewriter1_14 extends LegacyEntityRewriter<ClientboundPackets1_14, Protocol1_14To1_13_2> {

    private EntityPositionHandler positionHandler;

    public EntityPacketRewriter1_14(Protocol1_14To1_13_2 protocol) {
        super(protocol, Types1_13_2.META_TYPES.optionalComponentType, Types1_13_2.META_TYPES.booleanType);
    }

    //TODO work the method into this class alone
    @Override
    protected void addTrackedEntity(PacketWrapper wrapper, int entityId, EntityType type) {
        super.addTrackedEntity(wrapper, entityId, type);

        // Cache the position for every newly tracked entity
        if (type == EntityTypes1_14.PAINTING) {
            final Position position = wrapper.get(Types.BLOCK_POSITION1_8, 0);
            positionHandler.cacheEntityPosition(wrapper, position.x(), position.y(), position.z(), true, false);
        } else if (wrapper.getId() != ClientboundPackets1_14.LOGIN.getId()) { // ignore join game
            positionHandler.cacheEntityPosition(wrapper, true, false);
        }
    }

    @Override
    protected void registerPackets() {
        positionHandler = new EntityPositionHandler(this, EntityPositionStorage1_14.class, EntityPositionStorage1_14::new);

        protocol.registerClientbound(ClientboundPackets1_14.ENTITY_EVENT, wrapper -> {
            int entityId = wrapper.passthrough(Types.INT);
            byte status = wrapper.passthrough(Types.BYTE);
            // Check for death status
            if (status != 3) return;

            EntityTracker tracker = tracker(wrapper.user());
            EntityType entityType = tracker.entityType(entityId);
            if (entityType != EntityTypes1_14.PLAYER) return;

            // Remove equipment, else the client will see ghost items
            for (int i = 0; i <= 5; i++) {
                PacketWrapper equipmentPacket = wrapper.create(ClientboundPackets1_13.SET_EQUIPPED_ITEM);
                equipmentPacket.write(Types.VAR_INT, entityId);
                equipmentPacket.write(Types.VAR_INT, i);
                equipmentPacket.write(Types.ITEM1_13_2, null);
                equipmentPacket.send(Protocol1_14To1_13_2.class);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.TELEPORT_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                handler(wrapper -> positionHandler.cacheEntityPosition(wrapper, false, false));
            }
        });

        PacketHandlers relativeMoveHandler = new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.SHORT);
                map(Types.SHORT);
                map(Types.SHORT);
                handler(wrapper -> {
                    double x = wrapper.get(Types.SHORT, 0) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
                    double y = wrapper.get(Types.SHORT, 1) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
                    double z = wrapper.get(Types.SHORT, 2) / EntityPositionHandler.RELATIVE_MOVE_FACTOR;
                    positionHandler.cacheEntityPosition(wrapper, x, y, z, false, true);
                });
            }
        };
        protocol.registerClientbound(ClientboundPackets1_14.MOVE_ENTITY_POS, relativeMoveHandler);
        protocol.registerClientbound(ClientboundPackets1_14.MOVE_ENTITY_POS_ROT, relativeMoveHandler);

        protocol.registerClientbound(ClientboundPackets1_14.ADD_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.UUID); // 1 - UUID
                map(Types.VAR_INT, Types.BYTE); // 2 - Type
                map(Types.DOUBLE); // 3 - X
                map(Types.DOUBLE); // 4 - Y
                map(Types.DOUBLE); // 5 - Z
                map(Types.BYTE); // 6 - Pitch
                map(Types.BYTE); // 7 - Yaw
                map(Types.INT); // 8 - Data
                map(Types.SHORT); // 9 - Velocity X
                map(Types.SHORT); // 10 - Velocity Y
                map(Types.SHORT); // 11 - Velocity Z

                handler(getObjectTrackerHandler());

                handler(wrapper -> {
                    int id = wrapper.get(Types.BYTE, 0);
                    int mappedId = newEntityId(id);
                    EntityTypes1_13.EntityType entityType = EntityTypes1_13.getTypeFromId(mappedId, false);
                    EntityTypes1_13.ObjectType objectType;
                    if (entityType.isOrHasParent(EntityTypes1_13.EntityType.MINECART_ABSTRACT)) {
                        objectType = EntityTypes1_13.ObjectType.MINECART;
                        int data = switch (entityType) {
                            case CHEST_MINECART -> 1;
                            case FURNACE_MINECART -> 2;
                            case TNT_MINECART -> 3;
                            case SPAWNER_MINECART -> 4;
                            case HOPPER_MINECART -> 5;
                            case COMMAND_BLOCK_MINECART -> 6;
                            default -> 0;
                        };
                        if (data != 0)
                            wrapper.set(Types.INT, 0, data);
                    } else {
                        objectType = EntityTypes1_13.ObjectType.fromEntityType(entityType).orElse(null);
                    }

                    if (objectType == null) return;

                    wrapper.set(Types.BYTE, 0, (byte) objectType.getId());

                    int data = wrapper.get(Types.INT, 0);
                    if (objectType == EntityTypes1_13.ObjectType.FALLING_BLOCK) {
                        int blockState = wrapper.get(Types.INT, 0);
                        int combined = protocol.getMappingData().getNewBlockStateId(blockState);
                        wrapper.set(Types.INT, 0, combined);
                    } else if (entityType.isOrHasParent(EntityTypes1_13.EntityType.ABSTRACT_ARROW)) {
                        wrapper.set(Types.INT, 0, data + 1);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.ADD_MOB, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.UUID); // 1 - Entity UUID
                map(Types.VAR_INT); // 2 - Entity Type
                map(Types.DOUBLE); // 3 - X
                map(Types.DOUBLE); // 4 - Y
                map(Types.DOUBLE); // 5 - Z
                map(Types.BYTE); // 6 - Yaw
                map(Types.BYTE); // 7 - Pitch
                map(Types.BYTE); // 8 - Head Pitch
                map(Types.SHORT); // 9 - Velocity X
                map(Types.SHORT); // 10 - Velocity Y
                map(Types.SHORT); // 11 - Velocity Z
                map(Types1_14.METADATA_LIST, Types1_13_2.METADATA_LIST); // 12 - Metadata

                handler(wrapper -> {
                    int type = wrapper.get(Types.VAR_INT, 1);
                    EntityType entityType = EntityTypes1_14.getTypeFromId(type);
                    addTrackedEntity(wrapper, wrapper.get(Types.VAR_INT, 0), entityType);

                    int oldId = newEntityId(type);
                    if (oldId == -1) {
                        EntityData entityData = entityDataForType(entityType);
                        if (entityData == null) {
                            protocol.getLogger().warning("Could not find entity type mapping " + type + "/" + entityType);
                            wrapper.cancel();
                        } else {
                            wrapper.set(Types.VAR_INT, 1, entityData.replacementId());
                        }
                    } else {
                        wrapper.set(Types.VAR_INT, 1, oldId);
                    }
                });

                // Handle entity type & metadata
                handler(getMobSpawnRewriter1_11(Types1_13_2.METADATA_LIST));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.ADD_EXPERIENCE_ORB, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.DOUBLE); // Needs to be mapped for the position cache
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                handler(wrapper -> addTrackedEntity(wrapper, wrapper.get(Types.VAR_INT, 0), EntityTypes1_14.EXPERIENCE_ORB));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.ADD_GLOBAL_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.BYTE);
                map(Types.DOUBLE); // Needs to be mapped for the position cache
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                handler(wrapper -> addTrackedEntity(wrapper, wrapper.get(Types.VAR_INT, 0), EntityTypes1_14.LIGHTNING_BOLT));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.ADD_PAINTING, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.UUID);
                map(Types.VAR_INT);
                map(Types.BLOCK_POSITION1_14, Types.BLOCK_POSITION1_8);
                map(Types.BYTE);

                // Track entity
                handler(wrapper -> addTrackedEntity(wrapper, wrapper.get(Types.VAR_INT, 0), EntityTypes1_14.PAINTING));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.ADD_PLAYER, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.UUID); // 1 - Player UUID
                map(Types.DOUBLE); // 2 - X
                map(Types.DOUBLE); // 3 - Y
                map(Types.DOUBLE); // 4 - Z
                map(Types.BYTE); // 5 - Yaw
                map(Types.BYTE); // 6 - Pitch
                map(Types1_14.METADATA_LIST, Types1_13_2.METADATA_LIST); // 7 - Metadata

                handler(getTrackerAndMetaHandler(Types1_13_2.METADATA_LIST, EntityTypes1_14.PLAYER));
                handler(wrapper -> positionHandler.cacheEntityPosition(wrapper, true, false));
            }
        });

        registerRemoveEntities(ClientboundPackets1_14.REMOVE_ENTITIES);
        registerMetadataRewriter(ClientboundPackets1_14.SET_ENTITY_DATA, Types1_14.METADATA_LIST, Types1_13_2.METADATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_14.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Entity ID
                map(Types.UNSIGNED_BYTE); // 1 - Gamemode
                map(Types.INT); // 2 - Dimension

                handler(getTrackerHandler(EntityTypes1_14.PLAYER, Types.INT));
                handler(getDimensionHandler(1));
                handler(wrapper -> {
                    short difficulty = wrapper.user().get(DifficultyStorage.class).getDifficulty();
                    wrapper.write(Types.UNSIGNED_BYTE, difficulty);

                    wrapper.passthrough(Types.UNSIGNED_BYTE); // Max Players
                    wrapper.passthrough(Types.STRING); // Level Type
                    wrapper.read(Types.VAR_INT); // Read View Distance

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
                map(Types.INT); // 0 - Dimension ID

                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                    int dimensionId = wrapper.get(Types.INT, 0);
                    clientWorld.setEnvironment(dimensionId);

                    short difficulty = wrapper.user().get(DifficultyStorage.class).getDifficulty();
                    wrapper.write(Types.UNSIGNED_BYTE, difficulty);

                    wrapper.user().get(ChunkLightStorage.class).clear();
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, meta) -> {
            int typeId = meta.metaType().typeId();
            if (typeId <= 15) {
                meta.setMetaType(Types1_13_2.META_TYPES.byId(typeId));
            }
        });

        registerMetaTypeHandler(Types1_13_2.META_TYPES.itemType, null, Types1_13_2.META_TYPES.optionalBlockStateType, null,
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
            if (type.isOrHasParent(EntityTypes1_14.ABSTRACT_ILLAGER) || type == EntityTypes1_14.RAVAGER || type == EntityTypes1_14.WITCH) {
                int index = event.index();
                if (index == 14) {
                    event.cancel();
                } else if (index > 14) {
                    event.setIndex(index - 1);
                }
            }
        });

        filter().type(EntityTypes1_14.AREA_EFFECT_CLOUD).index(10).handler((event, meta) -> {
            rewriteParticle(event.user(), (Particle) meta.getValue());
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
        filter().type(EntityTypes1_14.LIVING_ENTITY).handler((event, meta) -> {
            int index = event.index();
            if (index == 12) {
                Position position = (Position) meta.getValue();
                if (position != null) {
                    // Use bed
                    PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_13.PLAYER_SLEEP, null, event.user());
                    wrapper.write(Types.VAR_INT, event.entityId());
                    wrapper.write(Types.BLOCK_POSITION1_8, position);

                    try {
                        wrapper.scheduleSend(Protocol1_14To1_13_2.class);
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
    public void onMappingDataLoaded() {
        mapTypes();

        mapEntityTypeWithData(EntityTypes1_14.CAT, EntityTypes1_14.OCELOT).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.TRADER_LLAMA, EntityTypes1_14.LLAMA).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.FOX, EntityTypes1_14.WOLF).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.PANDA, EntityTypes1_14.POLAR_BEAR).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.PILLAGER, EntityTypes1_14.VILLAGER).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.WANDERING_TRADER, EntityTypes1_14.VILLAGER).jsonName();
        mapEntityTypeWithData(EntityTypes1_14.RAVAGER, EntityTypes1_14.COW).jsonName();
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_14.getTypeFromId(typeId);
    }
}
