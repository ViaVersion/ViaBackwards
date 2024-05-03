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
package com.viaversion.viabackwards.protocol.protocol1_20_5to1_21.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_5to1_21.Protocol1_20_5To1_21;
import com.viaversion.viabackwards.protocol.protocol1_20_5to1_21.storage.EnchantmentsPaintingsStorage;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.api.type.types.version.Types1_21;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_21to1_20_5.data.Paintings1_20_5;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.KeyMappings;
import java.util.HashMap;
import java.util.Map;

public final class EntityPacketRewriter1_21 extends EntityRewriter<ClientboundPacket1_20_5, Protocol1_20_5To1_21> {

    private final Map<String, PaintingData> oldPaintings = new HashMap<>();

    public EntityPacketRewriter1_21(final Protocol1_20_5To1_21 protocol) {
        super(protocol, Types1_20_5.META_TYPES.optionalComponentType, Types1_20_5.META_TYPES.booleanType);

        for (int i = 0; i < Paintings1_20_5.PAINTINGS.length; i++) {
            final Paintings1_20_5.PaintingVariant painting = Paintings1_20_5.PAINTINGS[i];
            oldPaintings.put(painting.key(), new PaintingData(painting, i));
        }
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_20_5.SPAWN_ENTITY, EntityTypes1_20_5.FALLING_BLOCK);
        registerMetadataRewriter(ClientboundPackets1_20_5.ENTITY_METADATA, Types1_21.METADATA_LIST, Types1_20_5.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_20_5.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundConfigurationPackets1_20_5.REGISTRY_DATA, wrapper -> {
            final String key = Key.stripMinecraftNamespace(wrapper.passthrough(Type.STRING));
            final RegistryEntry[] entries = wrapper.passthrough(Type.REGISTRY_ENTRY_ARRAY);
            final boolean paintingVariant = key.equals("painting_variant");
            if (paintingVariant || key.equals("enchantment")) {
                // Track custom registries and cancel the packet
                final String[] keys = new String[entries.length];
                for (int i = 0; i < entries.length; i++) {
                    keys[i] = Key.stripMinecraftNamespace(entries[i].key());
                }

                final EnchantmentsPaintingsStorage storage = wrapper.user().get(EnchantmentsPaintingsStorage.class);
                if (paintingVariant) {
                    storage.setPaintings(new KeyMappings(keys), paintingMappingsForEntries(entries));
                } else {
                    final Tag[] descriptions = new Tag[entries.length];
                    for (int i = 0; i < entries.length; i++) {
                        final RegistryEntry entry = entries[i];
                        if (entry.tag() != null) {
                            descriptions[i] = ((CompoundTag) entry.tag()).get("description");
                        }
                    }
                    storage.setEnchantments(new KeyMappings(keys), descriptions);
                }

                wrapper.cancel();
            } else {
                handleRegistryData1_20_5(wrapper.user(), key, entries);
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
                map(Type.VAR_INT); // Dimension key
                map(Type.STRING); // World
                handler(worldDataTrackerHandlerByKey1_20_5(3));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.RESPAWN, wrapper -> {
            final int dimensionId = wrapper.passthrough(Type.VAR_INT);
            final String world = wrapper.passthrough(Type.STRING);
            trackWorldDataByKey1_20_5(wrapper.user(), dimensionId, world); // Tracks world height and name for chunk data and entity (un)tracking
        });
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
                final Paintings1_20_5.PaintingVariant painting = Paintings1_20_5.PAINTINGS[j];
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
        filter().mapMetaType(Types1_20_5.META_TYPES::byId);

        registerMetaTypeHandler1_20_3(
            Types1_20_5.META_TYPES.itemType,
            Types1_20_5.META_TYPES.blockStateType,
            Types1_20_5.META_TYPES.optionalBlockStateType,
            Types1_20_5.META_TYPES.particleType,
            Types1_20_5.META_TYPES.particlesType,
            Types1_20_5.META_TYPES.componentType,
            Types1_20_5.META_TYPES.optionalComponentType
        );

        filter().metaType(Types1_20_5.META_TYPES.paintingVariantType).handler((event, meta) -> {
            final EnchantmentsPaintingsStorage storage = event.user().get(EnchantmentsPaintingsStorage.class);
            final int id = meta.value();
            meta.setValue(storage.mappedPainting(id));
        });

        filter().type(EntityTypes1_20_5.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            final int blockState = meta.value();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(blockState));
        });
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_20_5.getTypeFromId(type);
    }

    private static final class PaintingData {
        private final Paintings1_20_5.PaintingVariant painting;
        private final int id;

        private PaintingData(final Paintings1_20_5.PaintingVariant painting, final int id) {
            this.painting = painting;
            this.id = id;
        }
    }
}