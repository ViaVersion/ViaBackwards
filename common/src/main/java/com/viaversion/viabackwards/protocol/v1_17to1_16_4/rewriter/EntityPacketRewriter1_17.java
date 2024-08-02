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
package com.viaversion.viabackwards.protocol.v1_17to1_16_4.rewriter;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_17to1_16_4.Protocol1_17To1_16_4;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_16_2;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_17;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_16;
import com.viaversion.viaversion.api.type.types.version.Types1_17;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.nbt.tag.*;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ClientboundPackets1_17;
import com.viaversion.viaversion.util.TagUtil;

public final class EntityPacketRewriter1_17 extends EntityRewriter<ClientboundPackets1_17, Protocol1_17To1_16_4> {

    private boolean warned = ViaBackwards.getConfig().bedrockAtY0() || ViaBackwards.getConfig().suppressEmulationWarnings();

    public EntityPacketRewriter1_17(Protocol1_17To1_16_4 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerTrackerWithData(ClientboundPackets1_17.ADD_ENTITY, EntityTypes1_17.FALLING_BLOCK);
        registerSpawnTracker(ClientboundPackets1_17.ADD_MOB);
        registerTracker(ClientboundPackets1_17.ADD_EXPERIENCE_ORB, EntityTypes1_17.EXPERIENCE_ORB);
        registerTracker(ClientboundPackets1_17.ADD_PAINTING, EntityTypes1_17.PAINTING);
        registerTracker(ClientboundPackets1_17.ADD_PLAYER, EntityTypes1_17.PLAYER);
        registerSetEntityData(ClientboundPackets1_17.SET_ENTITY_DATA, Types1_17.ENTITY_DATA_LIST, Types1_16.ENTITY_DATA_LIST);

        protocol.appendClientbound(ClientboundPackets1_17.ADD_ENTITY, wrapper -> {
            final int entityType = wrapper.get(Types.VAR_INT, 1);
            if (entityType != EntityTypes1_16_2.ITEM_FRAME.getId()) {
                return;
            }

            // Older clients will ignore the data field and the server sets the item frame rotation by the yaw/pitch field inside the packet,
            // newer clients do the opposite and ignore yaw/pitch and use the data field from the packet.
            final int data = wrapper.get(Types.INT, 0);

            float pitch = 0F;
            float yaw = 0F;
            switch (Math.abs(data % 6)) {
                case 0 /* down */ -> pitch = 90F;
                case 1 /* up */ -> pitch = -90F;
                case 2 /* north */ -> yaw = 180F;
                case 4 /* west */ -> yaw = 90F;
                case 5 /* east */ -> yaw = 270;
            }
            wrapper.set(Types.BYTE, 0, (byte) (pitch * 256F / 360F));
            wrapper.set(Types.BYTE, 1, (byte) (yaw * 256F / 360F));
        });

        protocol.registerClientbound(ClientboundPackets1_17.REMOVE_ENTITY, ClientboundPackets1_16_2.REMOVE_ENTITIES, wrapper -> {
            int entityId = wrapper.read(Types.VAR_INT);
            tracker(wrapper.user()).removeEntity(entityId);

            // Write into single value array
            int[] array = {entityId};
            wrapper.write(Types.VAR_INT_ARRAY_PRIMITIVE, array);
        });

        protocol.registerClientbound(ClientboundPackets1_17.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity ID
                map(Types.BOOLEAN); // Hardcore
                map(Types.BYTE); // Gamemode
                map(Types.BYTE); // Previous Gamemode
                map(Types.STRING_ARRAY); // Worlds
                map(Types.NAMED_COMPOUND_TAG); // Dimension registry
                map(Types.NAMED_COMPOUND_TAG); // Current dimension data
                map(Types.STRING); // World
                handler(wrapper -> {
                    byte previousGamemode = wrapper.get(Types.BYTE, 1);
                    if (previousGamemode == -1) { // "Unset" gamemode removed
                        wrapper.set(Types.BYTE, 1, (byte) 0);
                    }
                });
                handler(getTrackerHandler(EntityTypes1_17.PLAYER, Types.INT));
                handler(worldDataTrackerHandler(1));
                handler(wrapper -> {
                    CompoundTag registry = wrapper.get(Types.NAMED_COMPOUND_TAG, 0);
                    ListTag<CompoundTag> biomes = TagUtil.getRegistryEntries(registry, "worldgen/biome");
                    for (CompoundTag biome : biomes) {
                        CompoundTag biomeCompound = biome.getCompoundTag("element");
                        StringTag category = biomeCompound.getStringTag("category");
                        if (category.getValue().equalsIgnoreCase("underground")) {
                            category.setValue("none");
                        }
                    }

                    ListTag<CompoundTag> dimensions = TagUtil.getRegistryEntries(registry, "dimension_type");
                    for (CompoundTag dimension : dimensions) {
                        CompoundTag dimensionCompound = dimension.getCompoundTag("element");
                        reduceExtendedHeight(dimensionCompound, false);
                    }

                    reduceExtendedHeight(wrapper.get(Types.NAMED_COMPOUND_TAG, 1), true);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.NAMED_COMPOUND_TAG); // Dimension data
                map(Types.STRING); // World
                handler(worldDataTrackerHandler(0));
                handler(wrapper -> reduceExtendedHeight(wrapper.get(Types.NAMED_COMPOUND_TAG, 0), true));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.PLAYER_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.DOUBLE);
                map(Types.FLOAT);
                map(Types.FLOAT);
                map(Types.BYTE);
                map(Types.VAR_INT);
                read(Types.BOOLEAN); // Dismount vehicle ¯\_(ツ)_/¯
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17.UPDATE_ATTRIBUTES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Entity id
                handler(wrapper -> wrapper.write(Types.INT, wrapper.read(Types.VAR_INT))); // Collection length
            }
        });

        protocol.mergePacket(ClientboundPackets1_17.PLAYER_COMBAT_ENTER, ClientboundPackets1_16_2.PLAYER_COMBAT, 0);
        protocol.mergePacket(ClientboundPackets1_17.PLAYER_COMBAT_END, ClientboundPackets1_16_2.PLAYER_COMBAT, 1);
        protocol.registerClientbound(ClientboundPackets1_17.PLAYER_COMBAT_KILL, ClientboundPackets1_16_2.PLAYER_COMBAT, wrapper -> {
            wrapper.write(Types.VAR_INT, 2);

            wrapper.passthrough(Types.VAR_INT); // Duration/Player id
            wrapper.passthrough(Types.INT); // Killer id
            protocol.getComponentRewriter().processText(wrapper.user(), wrapper.passthrough(Types.COMPONENT));
        });
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, data) -> {
            data.setDataType(Types1_16.ENTITY_DATA_TYPES.byId(data.dataType().typeId()));

            EntityDataType type = data.dataType();
            if (type == Types1_16.ENTITY_DATA_TYPES.particleType) {
                Particle particle = (Particle) data.getValue();
                if (particle.id() == 16) { // Dust / Dust Transition
                    // Remove transition target color values 4-6
                    particle.getArguments().subList(4, 7).clear();
                } else if (particle.id() == 37) { // Vibration Signal
                    // No nice mapping possible without tracking entity positions and doing particle tasks
                    particle.setId(0);
                    particle.getArguments().clear();
                    return;
                }

                rewriteParticle(event.user(), particle);
            } else if (type == Types1_16.ENTITY_DATA_TYPES.poseType) {
                // Goat LONG_JUMP added at 6
                int pose = data.value();
                if (pose == 6) {
                    data.setValue(1); // FALL_FLYING
                } else if (pose > 6) {
                    data.setValue(pose - 1);
                }
            }
        });

        // Particles have already been handled
        registerEntityDataTypeHandler(Types1_16.ENTITY_DATA_TYPES.itemType, null, Types1_16.ENTITY_DATA_TYPES.optionalBlockStateType, null,
            Types1_16.ENTITY_DATA_TYPES.componentType, Types1_16.ENTITY_DATA_TYPES.optionalComponentType);

        filter().type(EntityTypes1_17.AXOLOTL).cancel(17);
        filter().type(EntityTypes1_17.AXOLOTL).cancel(18);
        filter().type(EntityTypes1_17.AXOLOTL).cancel(19);

        filter().type(EntityTypes1_17.GLOW_SQUID).cancel(16);

        filter().type(EntityTypes1_17.GOAT).cancel(17);

        filter().type(EntityTypes1_17.SHULKER).addIndex(17); // TODO Handle attachment pos?

        filter().removeIndex(7); // Ticks frozen
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();

        mapEntityTypeWithData(EntityTypes1_17.AXOLOTL, EntityTypes1_17.TROPICAL_FISH).jsonName();
        mapEntityTypeWithData(EntityTypes1_17.GOAT, EntityTypes1_17.SHEEP).jsonName();
        mapEntityTypeWithData(EntityTypes1_17.GLOW_SQUID, EntityTypes1_17.SQUID).jsonName();
        mapEntityTypeWithData(EntityTypes1_17.GLOW_ITEM_FRAME, EntityTypes1_17.ITEM_FRAME);
    }

    @Override
    public EntityType typeFromId(int typeId) {
        return EntityTypes1_17.getTypeFromId(typeId);
    }

    private void reduceExtendedHeight(CompoundTag tag, boolean warn) {
        NumberTag minY = tag.getNumberTag("min_y");
        NumberTag height = tag.getNumberTag("height");
        NumberTag logicalHeight = tag.getNumberTag("logical_height");
        if (minY.asInt() != 0 || height.asInt() > 256 || logicalHeight.asInt() > 256) {
            if (warn && !warned) {
                protocol.getLogger().warning("Increased world height is NOT SUPPORTED for 1.16 players and below. They will see a void below y 0 and above 256. You can enable the `bedrock-at-y-0` config option to replace the air with a bedrock layer.");
                warned = true;
            }

            tag.putInt("height", Math.min(256, height.asInt()));
            tag.putInt("logical_height", Math.min(256, logicalHeight.asInt()));
        }
    }
}
