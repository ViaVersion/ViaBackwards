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
package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.RegistryDataStorage;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.SecureChatStorage;
import com.viaversion.viaversion.api.data.entity.DimensionData;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.data.entity.DimensionDataImpl;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.AttributeMappings;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.util.Key;
import java.util.HashMap;
import java.util.Map;

public final class EntityPacketRewriter1_20_5 extends EntityRewriter<ClientboundPackets1_20_5, Protocol1_20_3To1_20_5> {

    public EntityPacketRewriter1_20_5(final Protocol1_20_3To1_20_5 protocol) {
        super(protocol, Types1_20_3.META_TYPES.optionalComponentType, Types1_20_3.META_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_20_5.SPAWN_ENTITY, EntityTypes1_20_5.FALLING_BLOCK);
        registerMetadataRewriter(ClientboundPackets1_20_5.ENTITY_METADATA, Types1_20_5.METADATA_LIST, Types1_20_3.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_20_5.REMOVE_ENTITIES);

        protocol.registerClientbound(State.CONFIGURATION, ClientboundConfigurationPackets1_20_5.REGISTRY_DATA, new PacketHandlers() {
            @Override
            protected void register() {
                handler(wrapper -> {
                    final String registryKey = Key.stripMinecraftNamespace(wrapper.read(Type.STRING));
                    final RegistryEntry[] entries = wrapper.read(Type.REGISTRY_ENTRY_ARRAY);

                    // Track data
                    final RegistryDataStorage registryDataStorage = wrapper.user().get(RegistryDataStorage.class);
                    if (registryKey.equals("worldgen/biome")) {
                        tracker(wrapper.user()).setBiomesSent(entries.length);
                    } else if (registryKey.equals("dimension_type")) {
                        final Map<String, DimensionData> dimensionDataMap = new HashMap<>(entries.length);
                        final String[] keys = new String[entries.length];
                        for (int i = 0; i < entries.length; i++) {
                            final RegistryEntry entry = entries[i];
                            final String dimensionKey = Key.stripMinecraftNamespace(entry.key());
                            dimensionDataMap.put(dimensionKey, new DimensionDataImpl(i, (CompoundTag) entry.tag()));
                            keys[i] = dimensionKey;
                        }
                        registryDataStorage.setDimensionKeys(keys);
                        tracker(wrapper.user()).setDimensions(dimensionDataMap);
                    }

                    // Write to old format
                    final CompoundTag registryTag = new CompoundTag();
                    final ListTag entriesTag = new ListTag();
                    registryTag.putString("type", registryKey);
                    registryTag.put("value", entriesTag);
                    for (int i = 0; i < entries.length; i++) {
                        final RegistryEntry entry = entries[i];
                        final CompoundTag entryCompoundTag = new CompoundTag();
                        entryCompoundTag.putString("name", entry.key());
                        entryCompoundTag.putInt("id", i);
                        entryCompoundTag.put("element", entry.tag());
                        entriesTag.add(entryCompoundTag);
                    }

                    // Store and send together with the rest later
                    registryDataStorage.registryData().put(registryKey, registryTag);
                    wrapper.cancel();
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Entity id
                map(Type.BOOLEAN); // Hardcore
                map(Type.STRING_ARRAY); // World List
                map(Type.VAR_INT); // Max players
                map(Type.VAR_INT); // View distance
                map(Type.VAR_INT); // Simulation distance
                map(Type.BOOLEAN); // Reduced debug info
                map(Type.BOOLEAN); // Show death screen
                map(Type.BOOLEAN); // Limited crafting
                handler(wrapper -> {
                    final int dimensionId = wrapper.read(Type.VAR_INT);
                    final RegistryDataStorage storage = wrapper.user().get(RegistryDataStorage.class);
                    wrapper.write(Type.STRING, storage.dimensionKeys()[dimensionId]);
                });
                map(Type.STRING); // World
                map(Type.LONG); // Seed
                map(Type.BYTE); // Gamemode
                map(Type.BYTE); // Previous gamemode
                map(Type.BOOLEAN); // Debug
                map(Type.BOOLEAN); // Flat
                map(Type.OPTIONAL_GLOBAL_POSITION); // Last death location
                handler(wrapper -> {
                    // Moved to server data
                    final boolean enforcesSecureChat = wrapper.read(Type.BOOLEAN);
                    wrapper.user().get(SecureChatStorage.class).setEnforcesSecureChat(enforcesSecureChat);
                });
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.RESPAWN, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    final int dimensionId = wrapper.read(Type.VAR_INT);
                    final RegistryDataStorage storage = wrapper.user().get(RegistryDataStorage.class);
                    wrapper.write(Type.STRING, storage.dimensionKeys()[dimensionId]);
                });
                map(Type.STRING); // World
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.ENTITY_EFFECT, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Entity ID
            wrapper.passthrough(Type.VAR_INT); // Effect ID
            wrapper.passthrough(Type.BYTE); // Amplifier
            wrapper.passthrough(Type.VAR_INT); // Duration
            wrapper.passthrough(Type.BYTE); // Flags
            wrapper.write(Type.OPTIONAL_COMPOUND_TAG, null); // Add empty factor data
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.ENTITY_PROPERTIES, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Entity ID

            final int size = wrapper.passthrough(Type.VAR_INT);
            int newSize = size;
            for (int i = 0; i < size; i++) {
                // From a registry int ID to a string
                final int id = protocol.getMappingData().getAttributeMappings().getNewId(wrapper.read(Type.VAR_INT));
                if (id == -1) {
                    // Remove new attributes from the list
                    newSize--;

                    wrapper.read(Type.DOUBLE); // Base
                    final int modifierSize = wrapper.read(Type.VAR_INT);
                    for (int j = 0; j < modifierSize; j++) {
                        wrapper.read(Type.UUID); // ID
                        wrapper.read(Type.DOUBLE); // Amount
                        wrapper.read(Type.BYTE); // Operation
                    }
                    continue;
                }

                final String attribute = AttributeMappings.attribute(id);
                wrapper.write(Type.STRING, attribute);

                wrapper.passthrough(Type.DOUBLE); // Base
                final int modifierSize = wrapper.passthrough(Type.VAR_INT);
                for (int j = 0; j < modifierSize; j++) {
                    wrapper.passthrough(Type.UUID); // ID
                    wrapper.passthrough(Type.DOUBLE); // Amount
                    wrapper.passthrough(Type.BYTE); // Operation
                }
            }

            wrapper.set(Type.VAR_INT, 1, newSize);
        });
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, meta) -> {
            int id = meta.metaType().typeId();
            if (id >= Types1_20_5.META_TYPES.armadilloState.typeId()) {
                id--;
            }

            meta.setMetaType(Types1_20_3.META_TYPES.byId(id));
        });

        registerMetaTypeHandler1_20_3(
                Types1_20_3.META_TYPES.itemType,
                Types1_20_3.META_TYPES.blockStateType,
                Types1_20_3.META_TYPES.optionalBlockStateType,
                Types1_20_3.META_TYPES.particleType,
                Types1_20_3.META_TYPES.componentType,
                Types1_20_3.META_TYPES.optionalComponentType
        );

        filter().type(EntityTypes1_20_5.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            final int blockState = meta.value();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(blockState));
        });

        filter().type(EntityTypes1_20_5.ARMADILLO).removeIndex(17); // State
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();

        mapEntityTypeWithData(EntityTypes1_20_5.ARMADILLO, EntityTypes1_20_5.COW).tagName();
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_20_5.getTypeFromId(type);
    }
}