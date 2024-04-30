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
package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.rewriter;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.RegistryDataStorage;
import com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage.SecureChatStorage;
import com.viaversion.viaversion.api.data.entity.DimensionData;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.data.entity.DimensionDataImpl;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.FloatTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Attributes1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.storage.BannerPatternStorage;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.MathUtil;
import java.util.HashMap;
import java.util.Map;

public final class EntityPacketRewriter1_20_5 extends EntityRewriter<ClientboundPacket1_20_5, Protocol1_20_3To1_20_5> {

    public EntityPacketRewriter1_20_5(final Protocol1_20_3To1_20_5 protocol) {
        super(protocol, Types1_20_3.META_TYPES.optionalComponentType, Types1_20_3.META_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_20_5.SPAWN_ENTITY, EntityTypes1_20_5.FALLING_BLOCK);
        registerMetadataRewriter(ClientboundPackets1_20_5.ENTITY_METADATA, Types1_20_5.METADATA_LIST, Types1_20_3.METADATA_LIST);
        registerRemoveEntities(ClientboundPackets1_20_5.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundPackets1_20_5.ENTITY_EQUIPMENT, wrapper -> {
            wrapper.passthrough(Type.VAR_INT); // Entity id
            byte slot;
            do {
                slot = wrapper.read(Type.BYTE);
                if (slot == 6) {
                    //TODO
                    // Body to... something else? the actual inventory slot is still broken for llamas
                    // Incoming click also needs to be fixed
                    slot = 2;
                }

                wrapper.write(Type.BYTE, slot);

                final Item item = protocol.getItemRewriter().handleItemToClient(wrapper.user(), wrapper.read(Types1_20_5.ITEM));
                wrapper.write(Type.ITEM1_20_2, item);
            } while (slot < 0);
        });

        protocol.registerClientbound(ClientboundConfigurationPackets1_20_5.REGISTRY_DATA, wrapper -> {
            wrapper.cancel();

            final String registryKey = Key.stripMinecraftNamespace(wrapper.read(Type.STRING));
            if (registryKey.equals("wolf_variant")) {
                // There's only one wolf variant now
                return;
            }

            final RegistryDataStorage registryDataStorage = wrapper.user().get(RegistryDataStorage.class);
            final RegistryEntry[] entries = wrapper.read(Type.REGISTRY_ENTRY_ARRAY);
            // Track banner pattern and material ids for conversion in items
            if (registryKey.equals("banner_pattern")) {
                final BannerPatternStorage bannerStorage = new BannerPatternStorage();
                wrapper.user().put(bannerStorage);
                for (int i = 0; i < entries.length; i++) {
                    bannerStorage.bannerPatterns().put(i, entries[i].key());
                }
                return;
            }

            // Track biome and dimension data
            if (registryKey.equals("worldgen/biome")) {
                tracker(wrapper.user()).setBiomesSent(entries.length);

                // Update format of particles
                for (final RegistryEntry entry : entries) {
                    if (entry.tag() == null) {
                        continue;
                    }

                    final CompoundTag effects = ((CompoundTag) entry.tag()).getCompoundTag("effects");
                    final CompoundTag particle = effects.getCompoundTag("particle");
                    if (particle != null) {
                        final CompoundTag particleOptions = particle.getCompoundTag("options");
                        final String particleType = particleOptions.getString("type");
                        updateParticleFormat(particleOptions, Key.stripMinecraftNamespace(particleType));
                    }
                }
            } else if (registryKey.equals("dimension_type")) {
                final Map<String, DimensionData> dimensionDataMap = new HashMap<>(entries.length);
                final String[] keys = new String[entries.length];
                for (int i = 0; i < entries.length; i++) {
                    final RegistryEntry entry = entries[i];
                    Preconditions.checkNotNull(entry.tag(), "Server unexpectedly sent null dimension data for " + entry.key());

                    final String dimensionKey = Key.stripMinecraftNamespace(entry.key());
                    final CompoundTag tag = (CompoundTag) entry.tag();
                    updateDimensionTypeData(tag);
                    dimensionDataMap.put(dimensionKey, new DimensionDataImpl(i, tag));
                    keys[i] = dimensionKey;
                }
                registryDataStorage.setDimensionKeys(keys);
                tracker(wrapper.user()).setDimensions(dimensionDataMap);
            }

            // Write to old format
            final boolean isTrimPattern = registryKey.equals("trim_pattern");
            final CompoundTag registryTag = new CompoundTag();
            final ListTag<CompoundTag> entriesTag = new ListTag<>(CompoundTag.class);
            registryTag.putString("type", registryKey);
            registryTag.put("value", entriesTag);
            for (int i = 0; i < entries.length; i++) {
                final RegistryEntry entry = entries[i];
                Preconditions.checkNotNull(entry.tag(), "Server unexpectedly sent null registry data entry for " + entry.key());

                if (isTrimPattern) {
                    final CompoundTag patternTag = (CompoundTag) entry.tag();
                    final StringTag templateItem = patternTag.getStringTag("template_item");
                    if (Protocol1_20_5To1_20_3.MAPPINGS.getFullItemMappings().id(templateItem.getValue()) == -1) {
                        // Skip new items
                        continue;
                    }
                }

                final CompoundTag entryCompoundTag = new CompoundTag();
                entryCompoundTag.putString("name", entry.key());
                entryCompoundTag.putInt("id", i);
                entryCompoundTag.put("element", entry.tag());
                entriesTag.add(entryCompoundTag);
            }

            // Store and send together with the rest later
            registryDataStorage.registryData().put(registryKey, registryTag);
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
                map(Type.VAR_INT); // Portal cooldown
                handler(wrapper -> {
                    // Moved to server data
                    final boolean enforcesSecureChat = wrapper.read(Type.BOOLEAN);
                    wrapper.user().get(SecureChatStorage.class).setEnforcesSecureChat(enforcesSecureChat);
                });
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
                handler(playerTrackerHandler());
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

            final int amplifier = wrapper.read(Type.VAR_INT);
            wrapper.write(Type.BYTE, (byte) MathUtil.clamp(amplifier, Byte.MIN_VALUE, Byte.MAX_VALUE));

            wrapper.passthrough(Type.VAR_INT); // Duration
            wrapper.passthrough(Type.BYTE); // Flags
            wrapper.write(Type.OPTIONAL_COMPOUND_TAG, null); // Add empty factor data
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.ENTITY_PROPERTIES, wrapper -> {
            final int entityId = wrapper.passthrough(Type.VAR_INT);
            final int size = wrapper.passthrough(Type.VAR_INT);
            int newSize = size;
            for (int i = 0; i < size; i++) {
                // From a registry int ID to a string
                final int attributeId = wrapper.read(Type.VAR_INT);
                final String attribute = Attributes1_20_5.idToKey(attributeId);
                int mappedId = protocol.getMappingData().getAttributeMappings().getNewId(attributeId);
                if ("generic.jump_strength".equals(attribute)) {
                    final EntityType type = tracker(wrapper.user()).entityType(entityId);
                    if (type == null || !type.isOrHasParent(EntityTypes1_20_5.HORSE)) {
                        // Jump strength only applies to horses in old versions
                        mappedId = -1;
                    }
                }

                if (mappedId == -1) {
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

    private void updateParticleFormat(final CompoundTag options, final String particleType) {
        if ("block".equals(particleType) || "block_marker".equals(particleType) || "falling_dust".equals(particleType) || "dust_pillar".equals(particleType)) {
            // TODO Can be a string
            moveTag(options, "block_state", "value");
        } else if ("item".equals(particleType)) {
            Tag item = options.remove("item");
            if (item instanceof StringTag) {
                final CompoundTag compoundTag = new CompoundTag();
                compoundTag.put("id", item);
                item = compoundTag;
            }
            options.put("value", item);
        } else if ("dust_color_transition".equals(particleType)) {
            moveTag(options, "from_color", "fromColor");
            moveTag(options, "to_color", "toColor");
        } else if ("entity_effect".equals(particleType)) {
            Tag color = options.remove("color");
            if (color instanceof ListTag) {
                //noinspection unchecked
                ListTag<? extends NumberTag> colorParts = (ListTag<? extends NumberTag>) color;
                color = new FloatTag(encodeARGB(
                    colorParts.get(0).getValue().floatValue(),
                    colorParts.get(1).getValue().floatValue(),
                    colorParts.get(2).getValue().floatValue(),
                    colorParts.get(3).getValue().floatValue()
                ));
            }
            options.put("value", color);
        }
    }

    private int encodeARGB(final float a, final float r, final float g, final float b) {
        final int encodedAlpha = encodeColorPart(a);
        final int encodedRed = encodeColorPart(r);
        final int encodedGreen = encodeColorPart(g);
        final int encodedBlue = encodeColorPart(b);
        return encodedAlpha << 24 | encodedRed << 16 | encodedGreen << 8 | encodedBlue;
    }

    private int encodeColorPart(final float part) {
        return (int) Math.floor(part * 255);
    }

    private void moveTag(final CompoundTag compoundTag, final String from, final String to) {
        final Tag tag = compoundTag.remove(from);
        if (tag != null) {
            compoundTag.put(to, tag);
        }
    }

    private void updateDimensionTypeData(final CompoundTag elementTag) {
        final CompoundTag monsterSpawnLightLevel = elementTag.getCompoundTag("monster_spawn_light_level");
        if (monsterSpawnLightLevel != null) {
            final CompoundTag value = new CompoundTag();
            monsterSpawnLightLevel.put("value", value);
            value.putInt("min_inclusive", monsterSpawnLightLevel.getInt("min_inclusive"));
            value.putInt("max_inclusive", monsterSpawnLightLevel.getInt("max_inclusive"));
        }
    }

    @Override
    protected void registerRewrites() {
        filter().mapMetaType(typeId -> {
            if (typeId == Types1_20_5.META_TYPES.particlesType.typeId()) {
                // Handled with living entity
                return Types1_20_5.META_TYPES.particlesType;
            }

            int id = typeId;
            if (typeId >= Types1_20_5.META_TYPES.wolfVariantType.typeId()) {
                id--;
            }
            if (typeId >= Types1_20_5.META_TYPES.armadilloState.typeId()) {
                id--;
            }
            if (typeId >= Types1_20_5.META_TYPES.particlesType.typeId()) {
                id--;
            }
            return Types1_20_3.META_TYPES.byId(id);
        });

        registerMetaTypeHandler1_20_3(
            Types1_20_3.META_TYPES.itemType,
            Types1_20_3.META_TYPES.blockStateType,
            Types1_20_3.META_TYPES.optionalBlockStateType,
            Types1_20_3.META_TYPES.particleType,
            null,
            Types1_20_3.META_TYPES.componentType,
            Types1_20_3.META_TYPES.optionalComponentType
        );

        filter().type(EntityTypes1_20_5.LIVINGENTITY).index(10).handler((event, meta) -> {
            final Particle[] particles = meta.value();
            int color = 0;
            for (final Particle particle : particles) {
                if (particle.id() == protocol.getMappingData().getParticleMappings().id("entity_effect")) {
                    // Remove color argument, use one of them for the ambient particle color
                    color = particle.<Integer>removeArgument(0).getValue();
                }
            }
            meta.setTypeAndValue(Types1_20_3.META_TYPES.varIntType, color);
        });

        filter().type(EntityTypes1_20_5.AREA_EFFECT_CLOUD).addIndex(9); // Color
        filter().type(EntityTypes1_20_5.AREA_EFFECT_CLOUD).index(11).handler((event, meta) -> {
            final Particle particle = meta.value();
            if (particle.id() == protocol.getMappingData().getParticleMappings().mappedId("entity_effect")) {
                // Move color to its own metadata
                final int color = particle.<Integer>removeArgument(0).getValue();
                event.createExtraMeta(new Metadata(9, Types1_20_3.META_TYPES.varIntType, color));
            }
        });

        filter().type(EntityTypes1_20_5.MINECART_ABSTRACT).index(11).handler((event, meta) -> {
            final int blockState = meta.value();
            meta.setValue(protocol.getMappingData().getNewBlockStateId(blockState));
        });

        filter().type(EntityTypes1_20_5.LLAMA).addIndex(20); // Carpet color
        filter().type(EntityTypes1_20_5.ARMADILLO).removeIndex(17); // State
        filter().type(EntityTypes1_20_5.WOLF).removeIndex(22); // Wolf variant
        filter().type(EntityTypes1_20_5.OMINOUS_ITEM_SPAWNER).removeIndex(8); // Item
    }

    @Override
    public void onMappingDataLoaded() {
        mapTypes();

        mapEntityTypeWithData(EntityTypes1_20_5.ARMADILLO, EntityTypes1_20_5.COW).tagName();
        mapEntityTypeWithData(EntityTypes1_20_5.BOGGED, EntityTypes1_20_5.STRAY).tagName();
        mapEntityTypeWithData(EntityTypes1_20_5.BREEZE_WIND_CHARGE, EntityTypes1_20_5.WIND_CHARGE);
        mapEntityTypeWithData(EntityTypes1_20_5.OMINOUS_ITEM_SPAWNER, EntityTypes1_20_5.TEXT_DISPLAY);
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_20_5.getTypeFromId(type);
    }
}