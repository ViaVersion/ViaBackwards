/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_19_4to1_20.packets;

import com.google.common.collect.Sets;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_19_4to1_20.Protocol1_19_4To1_20;
import com.viaversion.viaversion.api.data.entity.TrackedEntity;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19_4Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_19_4;
import com.viaversion.viaversion.api.type.types.version.Types1_20;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ClientboundPackets1_19_4;
import com.viaversion.viaversion.util.Key;
import java.util.Set;

public final class EntityPackets1_20 extends EntityRewriter<ClientboundPackets1_19_4, Protocol1_19_4To1_20> {

    private final Set<String> newTrimPatterns = Sets.newHashSet("host_armor_trim_smithing_template", "raiser_armor_trim_smithing_template",
            "silence_armor_trim_smithing_template", "shaper_armor_trim_smithing_template", "wayfinder_armor_trim_smithing_template");

    public EntityPackets1_20(final Protocol1_19_4To1_20 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        registerMetadataRewriter(ClientboundPackets1_19_4.ENTITY_METADATA, Types1_20.METADATA_LIST, Types1_19_4.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_19_4.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundPackets1_19_4.SPAWN_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Entity id
                map(Type.UUID); // Entity UUID
                map(Type.VAR_INT); // Entity type
                map(Type.DOUBLE); // X
                map(Type.DOUBLE); // Y
                map(Type.DOUBLE); // Z
                map(Type.BYTE); // Pitch
                map(Type.BYTE); // Yaw
                map(Type.BYTE); // Head yaw
                map(Type.VAR_INT); // Data
                handler(getSpawnTrackerWithDataHandler1_19(Entity1_19_4Types.FALLING_BLOCK));
                handler(wrapper -> {
                    final int entityId = wrapper.get(Type.VAR_INT, 0);
                    final EntityType entityType = tracker(wrapper.user()).entityType(entityId);
                    if (entityType == Entity1_19_4Types.ITEM_DISPLAY) {
                        // Turn it upside down
                        wrapper.set(Type.BYTE, 0, (byte) -wrapper.get(Type.BYTE, 0));
                        wrapper.set(Type.BYTE, 1, (byte) (wrapper.get(Type.BYTE, 1) - 128));
                    }
                });
            }
        });

        final PacketHandler displayYawPitchHandler = wrapper -> {
            final TrackedEntity trackedEntity = tracker(wrapper.user()).entity(wrapper.get(Type.VAR_INT, 0));
            if (trackedEntity == null || trackedEntity.entityType() != Entity1_19_4Types.ITEM_DISPLAY) {
                return;
            }

            // Turn it upside down
            wrapper.set(Type.BYTE, 0, (byte) (wrapper.get(Type.BYTE, 0) - 128));
            wrapper.set(Type.BYTE, 1, (byte) -wrapper.get(Type.BYTE, 1));
        };
        protocol.registerClientbound(ClientboundPackets1_19_4.ENTITY_POSITION_AND_ROTATION, new PacketHandlers() {
            @Override
            protected void register() {
                map(Type.VAR_INT); // Entity id
                map(Type.SHORT);   // Delta X
                map(Type.SHORT);   // Delta Y
                map(Type.SHORT);   // Delta Z
                map(Type.BYTE);    // Yaw
                map(Type.BYTE);    // Pitch
                handler(displayYawPitchHandler);
            }
        });
        protocol.registerClientbound(ClientboundPackets1_19_4.ENTITY_ROTATION, new PacketHandlers() {
            @Override
            protected void register() {
                map(Type.VAR_INT); // Entity id
                map(Type.BYTE);    // Yaw
                map(Type.BYTE);    // Pitch
                handler(displayYawPitchHandler);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.ENTITY_HEAD_LOOK, wrapper -> {
            final TrackedEntity trackedEntity = tracker(wrapper.user()).entity(wrapper.passthrough(Type.VAR_INT));
            if (trackedEntity == null || trackedEntity.entityType() != Entity1_19_4Types.ITEM_DISPLAY) {
                return;
            }

            wrapper.write(Type.BYTE, (byte) (wrapper.read(Type.BYTE) - 128));
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.ENTITY_TELEPORT, new PacketHandlers() {
            @Override
            protected void register() {
                map(Type.VAR_INT); // Entity id
                map(Type.DOUBLE);  // X
                map(Type.DOUBLE);  // Y
                map(Type.DOUBLE);  // Z
                map(Type.BYTE);    // Yaw
                map(Type.BYTE);    // Pitch
                handler(wrapper -> {
                    final TrackedEntity trackedEntity = tracker(wrapper.user()).entity(wrapper.get(Type.VAR_INT, 0));
                    if (trackedEntity == null || trackedEntity.entityType() != Entity1_19_4Types.ITEM_DISPLAY) {
                        return;
                    }

                    wrapper.set(Type.BYTE, 0, (byte) (wrapper.get(Type.BYTE, 0) - 128));
                    wrapper.set(Type.BYTE, 1, (byte) -wrapper.get(Type.BYTE, 1));
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Entity id
                map(Type.BOOLEAN); // Hardcore
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // World List
                map(Type.NBT); // Dimension registry
                map(Type.STRING); // Dimension key
                map(Type.STRING); // World
                map(Type.LONG); // Seed
                map(Type.VAR_INT); // Max players
                map(Type.VAR_INT); // Chunk radius
                map(Type.VAR_INT); // Simulation distance
                map(Type.BOOLEAN); // Reduced debug info
                map(Type.BOOLEAN); // Show death screen
                map(Type.BOOLEAN); // Debug
                map(Type.BOOLEAN); // Flat
                map(Type.OPTIONAL_GLOBAL_POSITION); // Last death location
                read(Type.VAR_INT); // Portal cooldown

                handler(dimensionDataHandler()); // Caches dimensions to access data like height later
                handler(biomeSizeTracker()); // Tracks the amount of biomes sent for chunk data
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
                handler(wrapper -> {
                    final CompoundTag registry = wrapper.get(Type.NBT, 0);
                    final ListTag values = ((CompoundTag) registry.get("minecraft:trim_pattern")).get("value");
                    for (final Tag entry : values) {
                        final CompoundTag element = ((CompoundTag) entry).get("element");
                        final StringTag templateItem = element.get("template_item");
                        if (newTrimPatterns.contains(Key.stripMinecraftNamespace(templateItem.getValue()))) {
                            templateItem.setValue("minecraft:spire_armor_trim_smithing_template");
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_19_4.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Dimension
                map(Type.STRING); // World
                map(Type.LONG); // Seed
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous gamemode
                map(Type.BOOLEAN); // Debug
                map(Type.BOOLEAN); // Flat
                map(Type.BYTE); // Data to keep
                map(Type.OPTIONAL_GLOBAL_POSITION); // Last death location
                read(Type.VAR_INT); // Portal cooldown
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
            }
        });
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, meta) -> meta.setMetaType(Types1_19_4.META_TYPES.byId(meta.metaType().typeId())));
        registerMetaTypeHandler(Types1_19_4.META_TYPES.itemType, Types1_19_4.META_TYPES.blockStateType, Types1_19_4.META_TYPES.optionalBlockStateType,
                Types1_19_4.META_TYPES.particleType, Types1_19_4.META_TYPES.componentType, Types1_19_4.META_TYPES.optionalComponentType);

        filter().filterFamily(Entity1_19_4Types.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            final int blockState = meta.value();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(blockState));
        });
    }

    @Override
    public EntityType typeFromId(final int type) {
        return Entity1_19_4Types.getTypeFromId(type);
    }
}