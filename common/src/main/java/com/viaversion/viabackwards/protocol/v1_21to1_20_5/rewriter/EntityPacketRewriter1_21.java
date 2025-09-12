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
package com.viaversion.viabackwards.protocol.v1_21to1_20_5.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.Protocol1_21To1_20_5;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.storage.EnchantmentsPaintingsStorage;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.storage.PlayerRotationStorage;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.PaintingVariant;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.WolfVariant;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityDataType;
import com.viaversion.viaversion.api.minecraft.entitydata.types.EntityDataTypes1_20_5;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.data.Paintings1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPacket1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.KeyMappings;
import java.util.HashMap;
import java.util.Map;

public final class EntityPacketRewriter1_21 extends EntityRewriter<ClientboundPacket1_21, Protocol1_21To1_20_5> {

    private final Map<String, PaintingData> oldPaintings = new HashMap<>();

    public EntityPacketRewriter1_21(final Protocol1_21To1_20_5 protocol) {
        super(protocol, protocol.mappedTypes().entityDataTypes().optionalComponentType, protocol.mappedTypes().entityDataTypes().booleanType);

        for (int i = 0; i < Paintings1_20_5.PAINTINGS.length; i++) {
            final PaintingVariant painting = Paintings1_20_5.PAINTINGS[i];
            oldPaintings.put(painting.assetId(), new PaintingData(painting, i));
        }
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_21.ADD_ENTITY, EntityTypes1_20_5.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets1_21.SET_ENTITY_DATA);
        registerRemoveEntities(ClientboundPackets1_21.REMOVE_ENTITIES);

        final RegistryDataRewriter registryDataRewriter = new BackwardsRegistryRewriter(protocol);
        protocol.registerClientbound(ClientboundConfigurationPackets1_21.REGISTRY_DATA, wrapper -> {
            final String key = Key.stripMinecraftNamespace(wrapper.passthrough(Types.STRING));
            final RegistryEntry[] entries = wrapper.passthrough(Types.REGISTRY_ENTRY_ARRAY);
            final boolean paintingVariant = key.equals("painting_variant");
            final boolean enchantment = key.equals("enchantment");
            if (paintingVariant || enchantment || key.equals("jukebox_song")) {
                // Track custom registries and cancel the packet
                final String[] keys = new String[entries.length];
                for (int i = 0; i < entries.length; i++) {
                    keys[i] = Key.stripMinecraftNamespace(entries[i].key());
                }

                final EnchantmentsPaintingsStorage storage = wrapper.user().get(EnchantmentsPaintingsStorage.class);
                if (paintingVariant) {
                    storage.setPaintings(new KeyMappings(keys), paintingMappingsForEntries(entries));
                } else if (enchantment) {
                    final Tag[] descriptions = new Tag[entries.length];
                    final int[] maxLevels = new int[entries.length];
                    for (int i = 0; i < entries.length; i++) {
                        final RegistryEntry entry = entries[i];
                        if (entry.tag() instanceof final CompoundTag tag) {
                            descriptions[i] = tag.get("description");
                            maxLevels[i] = tag.getInt("max_level");
                        }
                    }
                    storage.setEnchantments(new KeyMappings(keys), descriptions, maxLevels);
                } else {
                    final int[] jukeboxSongMappings = new int[keys.length];
                    for (int i = 0; i < keys.length; i++) {
                        final int itemId = protocol.getMappingData().getFullItemMappings().mappedId("music_disc_" + keys[i]);
                        jukeboxSongMappings[i] = itemId;
                    }
                    storage.setJubeboxSongsToItems(jukeboxSongMappings);
                }

                wrapper.cancel();
            } else {
                registryDataRewriter.trackDimensionAndBiomes(wrapper.user(), key, entries);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_21.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity id
                map(Types.BOOLEAN); // Hardcore
                map(Types.STRING_ARRAY); // World List
                map(Types.VAR_INT); // Max players
                map(Types.VAR_INT); // View distance
                map(Types.VAR_INT); // Simulation distance
                map(Types.BOOLEAN); // Reduced debug info
                map(Types.BOOLEAN); // Show death screen
                map(Types.BOOLEAN); // Limited crafting
                map(Types.VAR_INT); // Dimension key
                map(Types.STRING); // World
                handler(worldDataTrackerHandlerByKey1_20_5(3));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_21.RESPAWN, wrapper -> {
            final int dimensionId = wrapper.passthrough(Types.VAR_INT);
            final String world = wrapper.passthrough(Types.STRING);
            trackWorldDataByKey1_20_5(wrapper.user(), dimensionId, world); // Tracks world height and name for chunk data and entity (un)tracking
        });

        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_POS_ROT, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z

            storePlayerRotation(wrapper);
        });

        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_ROT, this::storePlayerRotation);
    }

    private void storePlayerRotation(final PacketWrapper wrapper) {
        final float yaw = wrapper.passthrough(Types.FLOAT);
        final float pitch = wrapper.passthrough(Types.FLOAT);

        wrapper.user().get(PlayerRotationStorage.class).setRotation(yaw, pitch);
    }

    private int[] paintingMappingsForEntries(final RegistryEntry[] entries) {
        final int[] mappings = new int[entries.length];
        for (int i = 0; i < entries.length; i++) {
            final RegistryEntry entry = entries[i];
            final PaintingData paintingData = oldPaintings.get(Key.stripMinecraftNamespace(entry.key()));
            if (paintingData != null) {
                mappings[i] = paintingData.id;
                continue;
            }

            // Figure out which works by size
            if (entry.tag() == null) {
                continue;
            }

            final CompoundTag tag = (CompoundTag) entry.tag();
            for (int j = 0; j < Paintings1_20_5.PAINTINGS.length; j++) {
                final PaintingVariant painting = Paintings1_20_5.PAINTINGS[j];
                if (painting.width() == tag.getInt("width") && painting.height() == tag.getInt("height")) {
                    mappings[i] = j;
                    break;
                }
            }
        }
        return mappings;
    }

    @Override
    protected void registerRewrites() {
        final EntityDataTypes1_20_5 mappedEntityDataTypes = VersionedTypes.V1_20_5.entityDataTypes;
        filter().handler((event, data) -> {
            final EntityDataType type = data.dataType();
            if (type == VersionedTypes.V1_21.entityDataTypes.wolfVariantType) {
                final Holder<WolfVariant> variant = data.value();
                if (variant.hasId()) {
                    data.setTypeAndValue(mappedEntityDataTypes.wolfVariantType, variant.id());
                } else {
                    event.cancel();
                }
            } else if (type == VersionedTypes.V1_21.entityDataTypes.paintingVariantType) {
                final Holder<PaintingVariant> variant = data.value();
                if (variant.hasId()) {
                    final EnchantmentsPaintingsStorage storage = event.user().get(EnchantmentsPaintingsStorage.class);
                    final int mappedId = storage.mappedPainting(variant.id());
                    data.setTypeAndValue(mappedEntityDataTypes.paintingVariantType, mappedId);
                } else {
                    event.cancel();
                }
            } else {
                data.setDataType(mappedEntityDataTypes.byId(type.typeId()));
            }
        });
        registerEntityDataTypeHandler1_20_3(
            mappedEntityDataTypes.itemType,
            mappedEntityDataTypes.blockStateType,
            mappedEntityDataTypes.optionalBlockStateType,
            mappedEntityDataTypes.particleType,
            mappedEntityDataTypes.particlesType,
            mappedEntityDataTypes.componentType,
            mappedEntityDataTypes.optionalComponentType
        );
        registerBlockStateHandler(EntityTypes1_20_5.ABSTRACT_MINECART, 11);
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_20_5.getTypeFromId(type);
    }

    private record PaintingData(PaintingVariant painting, int id) {
    }
}
