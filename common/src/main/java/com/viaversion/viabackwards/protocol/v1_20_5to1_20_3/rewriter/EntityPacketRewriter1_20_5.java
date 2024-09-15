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
package com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.rewriter;

import com.google.common.base.Preconditions;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.FloatTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.storage.RegistryDataStorage;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.storage.SecureChatStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.DimensionData;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.data.entity.DimensionDataImpl;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.Protocol1_20_3To1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.data.Attributes1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundConfigurationPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.storage.ArmorTrimStorage;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.storage.BannerPatternStorage;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.KeyMappings;
import com.viaversion.viaversion.util.MathUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EntityPacketRewriter1_20_5 extends EntityRewriter<ClientboundPacket1_20_5, Protocol1_20_5To1_20_3> {

    public EntityPacketRewriter1_20_5(final Protocol1_20_5To1_20_3 protocol) {
        super(protocol, Types1_20_3.ENTITY_DATA_TYPES.optionalComponentType, Types1_20_3.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_20_5.ADD_ENTITY, EntityTypes1_20_5.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets1_20_5.SET_ENTITY_DATA, Types1_20_5.ENTITY_DATA_LIST, Types1_20_3.ENTITY_DATA_LIST);
        registerRemoveEntities(ClientboundPackets1_20_5.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundPackets1_20_5.SET_EQUIPMENT, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT); // Entity id
            final EntityType type = tracker(wrapper.user()).entityType(entityId);
            byte slot;
            do {
                slot = wrapper.read(Types.BYTE);
                final Item item = protocol.getItemRewriter().handleItemToClient(wrapper.user(), wrapper.read(Types1_20_5.ITEM));
                final int rawSlot = slot & 0x7F;

                if (rawSlot == 6) {
                    final boolean lastSlot = (slot & 0xFFFFFF80) == 0;
                    slot = (byte) (lastSlot ? 4 : 4 | 0xFFFFFF80); // Map body slot index to chest slot index for horses, also wolves

                    if (type != null && type.isOrHasParent(EntityTypes1_20_5.LLAMA)) {
                        // Cancel equipment and set correct entity data instead
                        wrapper.cancel();
                        sendCarpetColorUpdate(wrapper.user(), entityId, item);
                    }
                }

                wrapper.write(Types.BYTE, slot);
                wrapper.write(Types.ITEM1_20_2, item);
            } while ((slot & 0xFFFFFF80) != 0);
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.HORSE_SCREEN_OPEN, wrapper -> {
            wrapper.passthrough(Types.UNSIGNED_BYTE); // Container id

            // The body armor slot was moved to equipment
            final int size = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.VAR_INT, size + 1);
        });

        protocol.registerClientbound(ClientboundConfigurationPackets1_20_5.REGISTRY_DATA, wrapper -> {
            wrapper.cancel();

            final String registryKey = Key.stripMinecraftNamespace(wrapper.read(Types.STRING));
            if (registryKey.equals("wolf_variant")) {
                // There's only one wolf variant now
                return;
            }

            final RegistryDataStorage registryDataStorage = wrapper.user().get(RegistryDataStorage.class);
            final RegistryEntry[] entries = wrapper.read(Types.REGISTRY_ENTRY_ARRAY);

            // Track trim patterns and armor trims for conversion in items
            if (registryKey.equals("banner_pattern")) {
                // Don't send it
                wrapper.user().get(BannerPatternStorage.class).setBannerPatterns(toMappings(entries));
                return;
            }

            final boolean isTrimPattern = registryKey.equals("trim_pattern");
            if (isTrimPattern) {
                wrapper.user().get(ArmorTrimStorage.class).setTrimPatterns(toMappings(entries));
            } else if (registryKey.equals("trim_material")) {
                wrapper.user().get(ArmorTrimStorage.class).setTrimMaterials(toMappings(entries));
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
                    if (Protocol1_20_3To1_20_5.MAPPINGS.getFullItemMappings().id(templateItem.getValue()) == -1) {
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

        protocol.registerClientbound(ClientboundPackets1_20_5.LOGIN, new PacketHandlers() {
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
                handler(wrapper -> {
                    final int dimensionId = wrapper.read(Types.VAR_INT);
                    final RegistryDataStorage storage = wrapper.user().get(RegistryDataStorage.class);
                    wrapper.write(Types.STRING, storage.dimensionKeys()[dimensionId]);
                });
                map(Types.STRING); // World
                map(Types.LONG); // Seed
                map(Types.BYTE); // Gamemode
                map(Types.BYTE); // Previous gamemode
                map(Types.BOOLEAN); // Debug
                map(Types.BOOLEAN); // Flat
                map(Types.OPTIONAL_GLOBAL_POSITION); // Last death location
                map(Types.VAR_INT); // Portal cooldown
                handler(wrapper -> {
                    // Moved to server data
                    final boolean enforcesSecureChat = wrapper.read(Types.BOOLEAN);
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
                    final int dimensionId = wrapper.read(Types.VAR_INT);
                    final RegistryDataStorage storage = wrapper.user().get(RegistryDataStorage.class);
                    wrapper.write(Types.STRING, storage.dimensionKeys()[dimensionId]);
                });
                map(Types.STRING); // World
                handler(worldDataTrackerHandlerByKey()); // Tracks world height and name for chunk data and entity (un)tracking
            }
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.UPDATE_MOB_EFFECT, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Entity ID
            wrapper.passthrough(Types.VAR_INT); // Effect ID

            final int amplifier = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.BYTE, (byte) MathUtil.clamp(amplifier, Byte.MIN_VALUE, Byte.MAX_VALUE));

            wrapper.passthrough(Types.VAR_INT); // Duration
            wrapper.passthrough(Types.BYTE); // Flags
            wrapper.write(Types.OPTIONAL_COMPOUND_TAG, null); // Add empty factor data
        });

        protocol.registerClientbound(ClientboundPackets1_20_5.UPDATE_ATTRIBUTES, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            final int size = wrapper.passthrough(Types.VAR_INT);
            int newSize = size;
            for (int i = 0; i < size; i++) {
                // From a registry int ID to a string
                final int attributeId = wrapper.read(Types.VAR_INT);
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

                    wrapper.read(Types.DOUBLE); // Base
                    final int modifierSize = wrapper.read(Types.VAR_INT);
                    for (int j = 0; j < modifierSize; j++) {
                        wrapper.read(Types.UUID); // ID
                        wrapper.read(Types.DOUBLE); // Amount
                        wrapper.read(Types.BYTE); // Operation
                    }
                    continue;
                }

                wrapper.write(Types.STRING, attribute);

                wrapper.passthrough(Types.DOUBLE); // Base
                final int modifierSize = wrapper.passthrough(Types.VAR_INT);
                for (int j = 0; j < modifierSize; j++) {
                    wrapper.passthrough(Types.UUID); // ID
                    wrapper.passthrough(Types.DOUBLE); // Amount
                    wrapper.passthrough(Types.BYTE); // Operation
                }
            }

            wrapper.set(Types.VAR_INT, 1, newSize);
        });
    }

    private KeyMappings toMappings(final RegistryEntry[] entries) {
        final String[] keys = new String[entries.length];
        for (int i = 0; i < entries.length; i++) {
            keys[i] = Key.stripMinecraftNamespace(entries[i].key());
        }
        return new KeyMappings(keys);
    }

    private void updateParticleFormat(final CompoundTag options, final String particleType) {
        if ("block".equals(particleType) || "block_marker".equals(particleType) || "falling_dust".equals(particleType) || "dust_pillar".equals(particleType)) {
            Tag blockState = options.remove("block_state");
            if (blockState instanceof StringTag) {
                final CompoundTag compoundTag = new CompoundTag();
                compoundTag.put("Name", blockState);
                blockState = compoundTag;
            }
            options.put("value", blockState);
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

    private int removeAlpha(final int argb) {
        return argb & 0x00FFFFFF;
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

    private void sendCarpetColorUpdate(final UserConnection connection, final int entityId, final Item item) {
        final PacketWrapper setEntityData = PacketWrapper.create(ClientboundPackets1_20_3.SET_ENTITY_DATA, connection);
        setEntityData.write(Types.VAR_INT, entityId); // Entity id
        int color = -1;
        if (item != null) {
            // Convert carpet item id to dyed color id
            if (item.identifier() >= 445 && item.identifier() <= 460) {
                color = item.identifier() - 445;
            }
        }
        final List<EntityData> entityDataList = new ArrayList<>();
        entityDataList.add(new EntityData(20, Types1_20_3.ENTITY_DATA_TYPES.varIntType, color));
        setEntityData.write(Types1_20_3.ENTITY_DATA_LIST, entityDataList);
        setEntityData.send(Protocol1_20_5To1_20_3.class);
    }

    @Override
    protected void registerRewrites() {
        filter().handler((event, data) -> {
            final int typeId = data.dataType().typeId();
            if (typeId == Types1_20_5.ENTITY_DATA_TYPES.particlesType.typeId()) {
                final Particle[] particles = data.value();
                int color = 0;
                for (final Particle particle : particles) {
                    if (particle.id() == protocol.getMappingData().getParticleMappings().id("entity_effect")) {
                        // Remove color argument, use one of them for the ambient particle color
                        color = particle.<Integer>removeArgument(0).getValue();
                    }
                }
                data.setTypeAndValue(Types1_20_3.ENTITY_DATA_TYPES.varIntType, removeAlpha(color));
                return;
            }

            int id = typeId;
            if (typeId >= Types1_20_5.ENTITY_DATA_TYPES.armadilloState.typeId()) {
                id--;
            }
            if (typeId >= Types1_20_5.ENTITY_DATA_TYPES.wolfVariantType.typeId()) {
                id--;
            }
            if (typeId >= Types1_20_5.ENTITY_DATA_TYPES.particlesType.typeId()) {
                id--;
            }
            data.setDataType(Types1_20_3.ENTITY_DATA_TYPES.byId(id));
        });

        registerEntityDataTypeHandler1_20_3(
            Types1_20_3.ENTITY_DATA_TYPES.itemType,
            Types1_20_3.ENTITY_DATA_TYPES.blockStateType,
            Types1_20_3.ENTITY_DATA_TYPES.optionalBlockStateType,
            Types1_20_3.ENTITY_DATA_TYPES.particleType,
            null,
            Types1_20_3.ENTITY_DATA_TYPES.componentType,
            Types1_20_3.ENTITY_DATA_TYPES.optionalComponentType
        );
        registerBlockStateHandler(EntityTypes1_20_5.ABSTRACT_MINECART, 11);

        filter().type(EntityTypes1_20_5.AREA_EFFECT_CLOUD).addIndex(9); // Color
        filter().type(EntityTypes1_20_5.AREA_EFFECT_CLOUD).index(11).handler((event, data) -> {
            final Particle particle = data.value();
            if (particle.id() == protocol.getMappingData().getParticleMappings().mappedId("entity_effect")) {
                // Move color to its own entity data
                final int color = particle.<Integer>removeArgument(0).getValue();
                event.createExtraData(new EntityData(9, Types1_20_3.ENTITY_DATA_TYPES.varIntType, removeAlpha(color)));
            }
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
