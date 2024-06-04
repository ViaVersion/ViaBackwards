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
package com.viaversion.viabackwards.protocol.v1_20to1_19_4.rewriter;

import com.google.common.collect.Sets;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_20to1_19_4.Protocol1_20To1_19_4;
import com.viaversion.viaversion.api.minecraft.Quaternion;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_4;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_19_4;
import com.viaversion.viaversion.api.type.types.version.Types1_20;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.util.Key;
import java.util.Set;

public final class EntityPacketRewriter1_20 extends EntityRewriter<ClientboundPackets1_19_4, Protocol1_20To1_19_4> {

    private final Set<String> newTrimPatterns = Sets.newHashSet("host_armor_trim_smithing_template", "raiser_armor_trim_smithing_template",
        "silence_armor_trim_smithing_template", "shaper_armor_trim_smithing_template", "wayfinder_armor_trim_smithing_template");
    private static final Quaternion Y_FLIPPED_ROTATION = new Quaternion(0, 1, 0, 0);

    public EntityPacketRewriter1_20(final Protocol1_20To1_19_4 protocol) {
        super(protocol, Types1_19_4.ENTITY_DATA_TYPES.optionalComponentType, Types1_19_4.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_19_4.ADD_ENTITY, EntityTypes1_19_4.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets1_19_4.SET_ENTITY_DATA, Types1_20.ENTITY_DATA_LIST, Types1_19_4.ENTITY_DATA_LIST);
        registerRemoveEntities(ClientboundPackets1_19_4.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundPackets1_19_4.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity id
                map(Types.BOOLEAN); // Hardcore
                map(Types.BYTE); // Gamemode
                map(Types.BYTE); // Previous Gamemode
                map(Types.STRING_ARRAY); // World List
                map(Types.NAMED_COMPOUND_TAG); // Dimension registry
                map(Types.STRING); // Dimension key
                map(Types.STRING); // World
                map(Types.LONG); // Seed
                map(Types.VAR_INT); // Max players
                map(Types.VAR_INT); // Chunk radius
                map(Types.VAR_INT); // Simulation distance
                map(Types.BOOLEAN); // Reduced debug info
                map(Types.BOOLEAN); // Show death screen
                map(Types.BOOLEAN); // Debug
                map(Types.BOOLEAN); // Flat
                map(Types.OPTIONAL_GLOBAL_POSITION); // Last death location
                read(Types.VAR_INT); // Portal cooldown

                handler(dimensionDataHandler()); // Caches dimensions to access data like height later
                handler(biomeSizeTracker()); // Tracks the amount of biomes sent for chunk data
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
                handler(wrapper -> {
                    final CompoundTag registry = wrapper.get(Types.NAMED_COMPOUND_TAG, 0);

                    final ListTag<CompoundTag> values;
                    // A 1.20 server can't send this element, and the 1.20 client still works, if the element is missing
                    // on a 1.19.4 client there is an exception, so in case the 1.20 server doesn't send the element we put in an original 1.20 element
                    CompoundTag trimPatternTag = registry.getCompoundTag("minecraft:trim_pattern");
                    if (trimPatternTag != null || (trimPatternTag = registry.getCompoundTag("trim_pattern")) != null) {
                        values = trimPatternTag.getListTag("value", CompoundTag.class);
                    } else {
                        final CompoundTag trimPatternRegistry = Protocol1_20To1_19_4.MAPPINGS.getTrimPatternRegistry().copy();
                        registry.put("minecraft:trim_pattern", trimPatternRegistry);
                        values = trimPatternRegistry.getListTag("value", CompoundTag.class);
                    }

                    for (final CompoundTag entry : values) {
                        final CompoundTag element = entry.getCompoundTag("element");
                        final StringTag templateItem = element.getStringTag("template_item");
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
                map(Types.STRING); // Dimension
                map(Types.STRING); // World
                map(Types.LONG); // Seed
                map(Types.UNSIGNED_BYTE); // Gamemode
                map(Types.BYTE); // Previous gamemode
                map(Types.BOOLEAN); // Debug
                map(Types.BOOLEAN); // Flat
                map(Types.BYTE); // Data to keep
                map(Types.OPTIONAL_GLOBAL_POSITION); // Last death location
                read(Types.VAR_INT); // Portal cooldown
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
            }
        });
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, meta) -> meta.setDataType(Types1_19_4.ENTITY_DATA_TYPES.byId(meta.dataType().typeId())));
        registerMetaTypeHandler(Types1_19_4.ENTITY_DATA_TYPES.itemType, Types1_19_4.ENTITY_DATA_TYPES.blockStateType, Types1_19_4.ENTITY_DATA_TYPES.optionalBlockStateType,
            Types1_19_4.ENTITY_DATA_TYPES.particleType, Types1_19_4.ENTITY_DATA_TYPES.componentType, Types1_19_4.ENTITY_DATA_TYPES.optionalComponentType);
        registerMinecartBlockStateHandler(EntityTypes1_19_4.ABSTRACT_MINECART);

        // Rotate item display by 180 degrees around the Y axis
        filter().type(EntityTypes1_19_4.ITEM_DISPLAY).handler((event, meta) -> {
            if (event.trackedEntity().hasSentEntityData() || event.hasExtraData()) {
                return;
            }

            if (event.dataAtIndex(12) == null) {
                event.createExtraData(new EntityData(12, Types1_19_4.ENTITY_DATA_TYPES.quaternionType, Y_FLIPPED_ROTATION));
            }
        });
        filter().type(EntityTypes1_19_4.ITEM_DISPLAY).index(12).handler((event, meta) -> {
            final Quaternion quaternion = meta.value();
            meta.setValue(rotateY180(quaternion));
        });
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_19_4.getTypeFromId(type);
    }

    private Quaternion rotateY180(final Quaternion quaternion) {
        return new Quaternion(-quaternion.z(), quaternion.w(), quaternion.x(), -quaternion.y());
    }
}
