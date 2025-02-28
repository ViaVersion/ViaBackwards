/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.Protocol1_21_5To1_21_4;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.storage.HorseDataStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.TrackedEntity;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.WolfVariant;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_5;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_21_4;
import com.viaversion.viaversion.api.type.types.version.Types1_21_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.util.Key;
import java.util.UUID;

public final class EntityPacketRewriter1_21_5 extends EntityRewriter<ClientboundPacket1_21_5, Protocol1_21_5To1_21_4> {

    public EntityPacketRewriter1_21_5(final Protocol1_21_5To1_21_4 protocol) {
        super(protocol, Types1_21_4.ENTITY_DATA_TYPES.optionalComponentType, Types1_21_4.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerSetEntityData(ClientboundPackets1_21_5.SET_ENTITY_DATA, Types1_21_5.ENTITY_DATA_LIST, Types1_21_4.ENTITY_DATA_LIST);
        registerRemoveEntities(ClientboundPackets1_21_5.REMOVE_ENTITIES);

        protocol.appendClientbound(ClientboundPackets1_21_5.ADD_ENTITY, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);

            final UUID uuid = wrapper.read(Types.UUID);
            final int entityType = wrapper.read(Types.VAR_INT);
            if (entityType != EntityTypes1_21_5.EXPERIENCE_ORB.getId()) {
                wrapper.write(Types.UUID, uuid);
                wrapper.write(Types.VAR_INT, entityType);
                wrapper.passthrough(Types.DOUBLE); // X
                wrapper.passthrough(Types.DOUBLE); // Y
                wrapper.passthrough(Types.DOUBLE); // Z
                wrapper.passthrough(Types.BYTE); // Pitch
                wrapper.passthrough(Types.BYTE); // Yaw
                wrapper.passthrough(Types.BYTE); // Head yaw
                wrapper.passthrough(Types.VAR_INT); // Data
                getSpawnTrackerWithDataHandler1_19(EntityTypes1_21_5.FALLING_BLOCK).handle(wrapper);
                return;
            }

            tracker(wrapper.user()).addEntity(entityId, EntityTypes1_21_5.EXPERIENCE_ORB);

            // Back to its own special packet
            wrapper.setPacketType(ClientboundPackets1_21_2.ADD_EXPERIENCE_ORB);
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.read(Types.BYTE); // Pitch
            wrapper.read(Types.BYTE); // Yaw
            wrapper.read(Types.BYTE); // Head yaw

            final int data = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.SHORT, (short) data);

            final short velocityX = wrapper.read(Types.SHORT);
            final short velocityY = wrapper.read(Types.SHORT);
            final short velocityZ = wrapper.read(Types.SHORT);
            if (velocityX != 0 || velocityY != 0 || velocityZ != 0) {
                // Send movement separately
                final PacketWrapper motionPacket = wrapper.create(ClientboundPackets1_21_2.SET_ENTITY_MOTION);
                motionPacket.write(Types.VAR_INT, entityId);
                motionPacket.write(Types.SHORT, velocityX);
                motionPacket.write(Types.SHORT, velocityY);
                motionPacket.write(Types.SHORT, velocityZ);
                wrapper.send(Protocol1_21_5To1_21_4.class);
                motionPacket.send(Protocol1_21_5To1_21_4.class);
                wrapper.cancel();
            }
        });

        final RegistryDataRewriter registryDataRewriter = new RegistryDataRewriter(protocol) {
            @Override
            public RegistryEntry[] handle(final UserConnection connection, final String key, final RegistryEntry[] entries) {
                final boolean trimPatternRegistry = key.equals("trim_pattern");
                if (trimPatternRegistry || key.equals("trim_material")) {
                    updateTrim(entries, trimPatternRegistry ? "template_item" : "ingredient");
                    return super.handle(connection, key, entries);
                }
                if (!key.equals("wolf_variant")) {
                    return super.handle(connection, key, entries);
                }

                for (final RegistryEntry entry : entries) {
                    if (entry.tag() == null) {
                        continue;
                    }

                    final CompoundTag variant = (CompoundTag) entry.tag();
                    final CompoundTag assets = (CompoundTag) variant.remove("assets");
                    variant.put("wild_texture", assets.get("wild"));
                    variant.put("tame_texture", assets.get("tame"));
                    variant.put("angry_texture", assets.get("angry"));
                    variant.put("biomes", new ListTag<>(StringTag.class));
                }
                return entries;
            }

            private void updateTrim(final RegistryEntry[] entries, final String itemKey) {
                for (final RegistryEntry entry : entries) {
                    if (entry.tag() == null) {
                        continue;
                    }

                    final CompoundTag tag = (CompoundTag) entry.tag();
                    tag.putString(itemKey, "stone"); // dummy ingredient
                }
            }
        };
        protocol.registerClientbound(ClientboundConfigurationPackets1_21.REGISTRY_DATA, wrapper -> {
            final String registryKey = Key.stripMinecraftNamespace(wrapper.passthrough(Types.STRING));
            if (registryKey.equals("pig_variant") || registryKey.equals("cow_variant")
                || registryKey.equals("frog_variant") || registryKey.equals("cat_variant")
                || registryKey.equals("chicken_variant")) {
                wrapper.cancel();
                return;
            }

            final RegistryEntry[] entries = wrapper.read(Types.REGISTRY_ENTRY_ARRAY);
            wrapper.write(Types.REGISTRY_ENTRY_ARRAY, registryDataRewriter.handle(wrapper.user(), registryKey, entries));
        });

        protocol.registerClientbound(ClientboundPackets1_21_5.LOGIN, wrapper -> {
            final int entityId = wrapper.passthrough(Types.INT); // Entity id
            wrapper.passthrough(Types.BOOLEAN); // Hardcore
            wrapper.passthrough(Types.STRING_ARRAY); // World List
            wrapper.passthrough(Types.VAR_INT); // Max players
            wrapper.passthrough(Types.VAR_INT); // View distance
            wrapper.passthrough(Types.VAR_INT); // Simulation distance
            wrapper.passthrough(Types.BOOLEAN); // Reduced debug info
            wrapper.passthrough(Types.BOOLEAN); // Show death screen
            wrapper.passthrough(Types.BOOLEAN); // Limited crafting
            final int dimensionId = wrapper.passthrough(Types.VAR_INT);
            final String world = wrapper.passthrough(Types.STRING);
            trackWorldDataByKey1_20_5(wrapper.user(), dimensionId, world);
            trackPlayer(wrapper.user(), entityId);
        });

        protocol.registerClientbound(ClientboundPackets1_21_5.RESPAWN, wrapper -> {
            final int dimensionId = wrapper.passthrough(Types.VAR_INT);
            final String world = wrapper.passthrough(Types.STRING);
            trackWorldDataByKey1_20_5(wrapper.user(), dimensionId, world);
        });

        protocol.registerClientbound(ClientboundPackets1_21_5.SET_PLAYER_TEAM, wrapper -> {
            wrapper.passthrough(Types.STRING); // Team Name
            final byte action = wrapper.passthrough(Types.BYTE); // Mode
            if (action == 0 || action == 2) {
                wrapper.passthrough(Types.TAG); // Display Name
                wrapper.passthrough(Types.BYTE); // Flags

                final int nametagVisibility = wrapper.read(Types.VAR_INT);
                final int collisionRule = wrapper.read(Types.VAR_INT);
                wrapper.write(Types.STRING, visibility(nametagVisibility));
                wrapper.write(Types.STRING, collision(collisionRule));

                wrapper.passthrough(Types.VAR_INT); // Color
                wrapper.passthrough(Types.TAG); // Prefix
                wrapper.passthrough(Types.TAG); // Suffix
            }
        });
    }

    private String visibility(final int id) {
        return switch (id) {
            case 0 -> "always";
            case 1 -> "never";
            case 2 -> "hideForOtherTeams";
            case 3 -> "hideForOwnTeam";
            default -> "always";
        };
    }

    private String collision(final int id) {
        return switch (id) {
            case 0 -> "always";
            case 1 -> "never";
            case 2 -> "pushOtherTeams";
            case 3 -> "pushOwnTeam";
            default -> "always";
        };
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, data) -> {
            final int id = data.dataType().typeId();
            if (id == Types1_21_5.ENTITY_DATA_TYPES.wolfVariantType.typeId()) {
                final int type = data.value();
                final Holder<WolfVariant> variant = Holder.of(type);
                data.setTypeAndValue(Types1_21_4.ENTITY_DATA_TYPES.wolfVariantType, variant);
                return;
            }

            int mappedId = id;
            if (id == Types1_21_5.ENTITY_DATA_TYPES.cowVariantType.typeId()
                || id == Types1_21_5.ENTITY_DATA_TYPES.pigVariantType.typeId()
                || id == Types1_21_5.ENTITY_DATA_TYPES.chickenVariantType.typeId()
                || id == Types1_21_5.ENTITY_DATA_TYPES.wolfSoundVariantType.typeId()) {
                event.cancel();
                return;
            } else if (id > Types1_21_5.ENTITY_DATA_TYPES.chickenVariantType.typeId()) {
                mappedId -= 4;
            } else if (id > Types1_21_5.ENTITY_DATA_TYPES.pigVariantType.typeId()) {
                mappedId -= 3;
            } else if (id > Types1_21_5.ENTITY_DATA_TYPES.wolfSoundVariantType.typeId()) {
                mappedId -= 2;
            } else if (id > Types1_21_5.ENTITY_DATA_TYPES.cowVariantType.typeId()) {
                mappedId -= 1;
            }
            data.setDataType(Types1_21_4.ENTITY_DATA_TYPES.byId(mappedId));
        });

        registerEntityDataTypeHandler1_20_3(
            Types1_21_4.ENTITY_DATA_TYPES.itemType,
            Types1_21_4.ENTITY_DATA_TYPES.blockStateType,
            Types1_21_4.ENTITY_DATA_TYPES.optionalBlockStateType,
            Types1_21_4.ENTITY_DATA_TYPES.particleType,
            Types1_21_4.ENTITY_DATA_TYPES.particlesType,
            Types1_21_4.ENTITY_DATA_TYPES.componentType,
            Types1_21_4.ENTITY_DATA_TYPES.optionalComponentType
        );

        filter().type(EntityTypes1_21_5.ABSTRACT_MINECART).addIndex(13); // Custom display
        filter().type(EntityTypes1_21_5.ABSTRACT_MINECART).index(11).handler((event, data) -> {
            final int state = (int) data.getValue();
            final int mappedBlockState = protocol.getMappingData().getNewBlockStateId(state);
            data.setTypeAndValue(Types1_21_5.ENTITY_DATA_TYPES.varIntType, mappedBlockState);
            event.createExtraData(new EntityData(13, Types1_21_4.ENTITY_DATA_TYPES.booleanType, true));
        });

        filter().type(EntityTypes1_21_5.MOOSHROOM).index(17).handler(((event, data) -> {
            final int typeId = data.value();
            final String typeName = typeId == 0 ? "red" : "brown";
            data.setTypeAndValue(Types1_21_5.ENTITY_DATA_TYPES.stringType, typeName);
        }));

        filter().type(EntityTypes1_21_5.ABSTRACT_HORSE).index(17).handler((event, data) -> {
            // Store data for later
            final TrackedEntity entity = event.trackedEntity();
            if (entity.hasData()) {
                final HorseDataStorage horseDataStorage = entity.data().get(HorseDataStorage.class);
                if (horseDataStorage != null && horseDataStorage.saddled()) {
                    entity.data().put(new HorseDataStorage(data.value(), true));
                    return;
                }
            }
            entity.data().put(new HorseDataStorage(data.value(), false));
        });

        filter().type(EntityTypes1_21_5.CHICKEN).cancel(17); // Chicken variant
        filter().type(EntityTypes1_21_5.COW).cancel(17); // Cow variant
        filter().type(EntityTypes1_21_5.PIG).cancel(19); // Pig variant
        filter().type(EntityTypes1_21_5.WOLF).cancel(23); // Sound variant
        filter().type(EntityTypes1_21_5.EXPERIENCE_ORB).cancel(8); // Value

        filter().type(EntityTypes1_21_5.DOLPHIN).addIndex(17); // Treasure pos
        filter().type(EntityTypes1_21_5.TURTLE).addIndex(17); // Home pos

        // Saddled
        filter().type(EntityTypes1_21_5.PIG).addIndex(17);
        filter().type(EntityTypes1_21_5.STRIDER).addIndex(19);

    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_21_5.getTypeFromId(type);
    }
}
