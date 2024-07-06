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
package com.viaversion.viabackwards.protocol.v1_16to1_15_2.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.Protocol1_16To1_15_2;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.storage.WorldNameTracker;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.storage.WolfDataMaskStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_16;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.remapper.ValueTransformer;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_14;
import com.viaversion.viaversion.api.type.types.version.Types1_16;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.packet.ClientboundPackets1_15;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ClientboundPackets1_16;
import com.viaversion.viaversion.util.Key;

public class EntityPacketRewriter1_16 extends EntityRewriter<ClientboundPackets1_16, Protocol1_16To1_15_2> {

    private final ValueTransformer<String, Integer> dimensionTransformer = new ValueTransformer<>(Types.STRING, Types.INT) {
        @Override
        public Integer transform(PacketWrapper wrapper, String input) {
            input = Key.namespaced(input);
            return switch (input) {
                case "minecraft:the_nether" -> -1;
                case "minecraft:the_end" -> 1;
                default -> 0; // Including overworld
            };
        }
    };

    public EntityPacketRewriter1_16(Protocol1_16To1_15_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_16.ADD_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity id
                map(Types.UUID); // 1 - Entity UUID
                map(Types.VAR_INT); // 2 - Entity Type
                map(Types.DOUBLE); // 3 - X
                map(Types.DOUBLE); // 4 - Y
                map(Types.DOUBLE); // 5 - Z
                map(Types.BYTE); // 6 - Pitch
                map(Types.BYTE); // 7 - Yaw
                map(Types.INT); // 8 - Data
                handler(wrapper -> {
                    EntityType entityType = typeFromId(wrapper.get(Types.VAR_INT, 1));
                    if (entityType == EntityTypes1_16.LIGHTNING_BOLT) {
                        // Map to old weather entity packet
                        wrapper.cancel();

                        PacketWrapper spawnLightningPacket = wrapper.create(ClientboundPackets1_15.ADD_GLOBAL_ENTITY);
                        spawnLightningPacket.write(Types.VAR_INT, wrapper.get(Types.VAR_INT, 0)); // Entity id
                        spawnLightningPacket.write(Types.BYTE, (byte) 1); // Lightning type
                        spawnLightningPacket.write(Types.DOUBLE, wrapper.get(Types.DOUBLE, 0)); // X
                        spawnLightningPacket.write(Types.DOUBLE, wrapper.get(Types.DOUBLE, 1)); // Y
                        spawnLightningPacket.write(Types.DOUBLE, wrapper.get(Types.DOUBLE, 2)); // Z
                        spawnLightningPacket.send(Protocol1_16To1_15_2.class);
                    }
                });
                handler(getSpawnTrackerWithDataHandler(EntityTypes1_16.FALLING_BLOCK));
            }
        });

        registerSpawnTracker(ClientboundPackets1_16.ADD_MOB);

        protocol.registerClientbound(ClientboundPackets1_16.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(dimensionTransformer); // Dimension Type
                handler(wrapper -> {
                    // Grab the tracker for world names
                    WorldNameTracker worldNameTracker = wrapper.user().get(WorldNameTracker.class);
                    String nextWorldName = wrapper.read(Types.STRING); // World Name

                    wrapper.passthrough(Types.LONG); // Seed
                    wrapper.passthrough(Types.UNSIGNED_BYTE); // Gamemode
                    wrapper.read(Types.BYTE); // Previous gamemode

                    // Grab client world
                    ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                    int dimension = wrapper.get(Types.INT, 0);

                    // Send a dummy respawn with a different dimension if the world name was different and the same dimension was used
                    if (clientWorld.getEnvironment() != null && dimension == clientWorld.getEnvironment().id()
                        && (wrapper.user().isClientSide() || Via.getPlatform().isProxy()
                        || wrapper.user().getProtocolInfo().protocolVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2) // Hotfix for https://github.com/ViaVersion/ViaBackwards/issues/381
                        || !nextWorldName.equals(worldNameTracker.getWorldName()))) {
                        PacketWrapper packet = wrapper.create(ClientboundPackets1_15.RESPAWN);
                        packet.write(Types.INT, dimension == 0 ? -1 : 0);
                        packet.write(Types.LONG, 0L);
                        packet.write(Types.UNSIGNED_BYTE, (short) 0);
                        packet.write(Types.STRING, "default");
                        packet.send(Protocol1_16To1_15_2.class);
                    }

                    clientWorld.setEnvironment(dimension);

                    wrapper.write(Types.STRING, "default"); // Level type
                    wrapper.read(Types.BOOLEAN); // Debug
                    if (wrapper.read(Types.BOOLEAN)) {
                        wrapper.set(Types.STRING, 0, "flat");
                    }
                    wrapper.read(Types.BOOLEAN); // Keep all playerdata

                    // Finally update the world name
                    worldNameTracker.setWorldName(nextWorldName);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); //  Entity ID
                map(Types.UNSIGNED_BYTE); // Gamemode
                read(Types.BYTE); // Previous gamemode
                read(Types.STRING_ARRAY); // World list
                read(Types.NAMED_COMPOUND_TAG); // whatever this is
                map(dimensionTransformer); // Dimension Type
                handler(wrapper -> {
                    WorldNameTracker worldNameTracker = wrapper.user().get(WorldNameTracker.class);
                    worldNameTracker.setWorldName(wrapper.read(Types.STRING)); // Save the world name
                });
                map(Types.LONG); // Seed
                map(Types.UNSIGNED_BYTE); // Max players
                handler(wrapper -> {
                    ClientWorld clientChunks = wrapper.user().get(ClientWorld.class);
                    clientChunks.setEnvironment(wrapper.get(Types.INT, 1));
                    tracker(wrapper.user()).addEntity(wrapper.get(Types.INT, 0), EntityTypes1_16.PLAYER);

                    wrapper.write(Types.STRING, "default"); // Level type

                    wrapper.passthrough(Types.VAR_INT); // View distance
                    wrapper.passthrough(Types.BOOLEAN); // Reduced debug info
                    wrapper.passthrough(Types.BOOLEAN); // Show death screen

                    wrapper.read(Types.BOOLEAN); // Debug
                    if (wrapper.read(Types.BOOLEAN)) {
                        wrapper.set(Types.STRING, 0, "flat");
                    }
                });
            }
        });

        registerTracker(ClientboundPackets1_16.ADD_EXPERIENCE_ORB, EntityTypes1_16.EXPERIENCE_ORB);
        // F Spawn Global Object, it is no longer with us :(
        registerTracker(ClientboundPackets1_16.ADD_PAINTING, EntityTypes1_16.PAINTING);
        registerTracker(ClientboundPackets1_16.ADD_PLAYER, EntityTypes1_16.PLAYER);
        registerRemoveEntities(ClientboundPackets1_16.REMOVE_ENTITIES);
        registerSetEntityData(ClientboundPackets1_16.SET_ENTITY_DATA, Types1_16.ENTITY_DATA_LIST, Types1_14.ENTITY_DATA_LIST);

        protocol.registerClientbound(ClientboundPackets1_16.UPDATE_ATTRIBUTES, wrapper -> {
            wrapper.passthrough(Types.VAR_INT);
            int size = wrapper.passthrough(Types.INT);
            for (int i = 0; i < size; i++) {
                String attributeIdentifier = wrapper.read(Types.STRING);
                String oldKey = protocol.getMappingData().attributeIdentifierMappings().get(attributeIdentifier);
                wrapper.write(Types.STRING, oldKey != null ? oldKey : Key.stripMinecraftNamespace(attributeIdentifier));

                wrapper.passthrough(Types.DOUBLE);
                int modifierSize = wrapper.passthrough(Types.VAR_INT);
                for (int j = 0; j < modifierSize; j++) {
                    wrapper.passthrough(Types.UUID);
                    wrapper.passthrough(Types.DOUBLE);
                    wrapper.passthrough(Types.BYTE);
                }
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16.PLAYER_INFO, wrapper -> {
            int action = wrapper.passthrough(Types.VAR_INT);
            int playerCount = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < playerCount; i++) {
                wrapper.passthrough(Types.UUID);
                if (action == 0) { // Add
                    wrapper.passthrough(Types.STRING);
                    int properties = wrapper.passthrough(Types.VAR_INT);
                    for (int j = 0; j < properties; j++) {
                        wrapper.passthrough(Types.STRING);
                        wrapper.passthrough(Types.STRING);
                        wrapper.passthrough(Types.OPTIONAL_STRING);
                    }
                    wrapper.passthrough(Types.VAR_INT);
                    wrapper.passthrough(Types.VAR_INT);
                    // Display Name
                    protocol.getComponentRewriter().processText(wrapper.user(), wrapper.passthrough(Types.OPTIONAL_COMPONENT));
                } else if (action == 1) { // Update Game Mode
                    wrapper.passthrough(Types.VAR_INT);
                } else if (action == 2) { // Update Ping
                    wrapper.passthrough(Types.VAR_INT);
                } else if (action == 3) { // Update Display Name
                    // Display name
                    protocol.getComponentRewriter().processText(wrapper.user(), wrapper.passthrough(Types.OPTIONAL_COMPONENT));
                } // 4 = Remove Player
            }
        });
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, data) -> {
            data.setDataType(Types1_14.ENTITY_DATA_TYPES.byId(data.dataType().typeId()));

            EntityDataType type = data.dataType();
            if (type == Types1_14.ENTITY_DATA_TYPES.itemType) {
                data.setValue(protocol.getItemRewriter().handleItemToClient(event.user(), (Item) data.getValue()));
            } else if (type == Types1_14.ENTITY_DATA_TYPES.optionalBlockStateType) {
                data.setValue(protocol.getMappingData().getNewBlockStateId((int) data.getValue()));
            } else if (type == Types1_14.ENTITY_DATA_TYPES.particleType) {
                rewriteParticle(event.user(), (Particle) data.getValue());
            } else if (type == Types1_14.ENTITY_DATA_TYPES.optionalComponentType) {
                JsonElement text = data.value();
                if (text != null) {
                    protocol.getComponentRewriter().processText(event.user(), text);
                }
            }
        });

        filter().type(EntityTypes1_16.ZOGLIN).cancel(16);
        filter().type(EntityTypes1_16.HOGLIN).cancel(15);

        filter().type(EntityTypes1_16.PIGLIN).cancel(16);
        filter().type(EntityTypes1_16.PIGLIN).cancel(17);
        filter().type(EntityTypes1_16.PIGLIN).cancel(18);

        filter().type(EntityTypes1_16.STRIDER).index(15).handler((event, data) -> {
            boolean baby = data.value();
            data.setTypeAndValue(Types1_14.ENTITY_DATA_TYPES.varIntType, baby ? 1 : 3);
        });
        filter().type(EntityTypes1_16.STRIDER).cancel(16);
        filter().type(EntityTypes1_16.STRIDER).cancel(17);
        filter().type(EntityTypes1_16.STRIDER).cancel(18);

        filter().type(EntityTypes1_16.FISHING_BOBBER).cancel(8);

        filter().type(EntityTypes1_16.ABSTRACT_ARROW).cancel(8);
        filter().type(EntityTypes1_16.ABSTRACT_ARROW).handler((event, data) -> {
            if (event.index() >= 8) {
                event.setIndex(event.index() + 1);
            }
        });

        filter().type(EntityTypes1_16.WOLF).index(16).handler((event, data) -> {
            byte mask = data.value();
            StoredEntityData entityData = tracker(event.user()).entityData(event.entityId());
            entityData.put(new WolfDataMaskStorage(mask));
        });

        filter().type(EntityTypes1_16.WOLF).index(20).handler((event, data) -> {
            StoredEntityData entityData = tracker(event.user()).entityDataIfPresent(event.entityId());
            byte previousMask = 0;
            if (entityData != null) {
                WolfDataMaskStorage wolfData = entityData.get(WolfDataMaskStorage.class);
                if (wolfData != null) {
                    previousMask = wolfData.tameableMask();
                }
            }

            int angerTime = data.value();
            byte tameableMask = (byte) (angerTime > 0 ? previousMask | 2 : previousMask & -3);
            event.createExtraData(new EntityData(16, Types1_14.ENTITY_DATA_TYPES.byteType, tameableMask));
            event.cancel();
        });
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();

        mapEntityTypeWithData(EntityTypes1_16.HOGLIN, EntityTypes1_16.COW).jsonName();
        mapEntityTypeWithData(EntityTypes1_16.ZOGLIN, EntityTypes1_16.COW).jsonName();
        mapEntityTypeWithData(EntityTypes1_16.PIGLIN, EntityTypes1_16.ZOMBIFIED_PIGLIN).jsonName();
        mapEntityTypeWithData(EntityTypes1_16.STRIDER, EntityTypes1_16.MAGMA_CUBE).jsonName();
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_16.getTypeFromId(typeId);
    }
}
