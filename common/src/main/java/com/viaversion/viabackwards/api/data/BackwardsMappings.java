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
package com.viaversion.viabackwards.api.data;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.BiMappings;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.data.MappingDataBase;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.util.Key;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsMappings extends MappingDataBase {

    private final Class<? extends Protocol<?, ?, ?, ?>> vvProtocolClass;
    protected Int2ObjectMap<MappedItem> backwardsItemMappings;
    private Map<String, String> backwardsSoundMappings;
    private Map<String, String> entityNames;

    public BackwardsMappings(final String unmappedVersion, final String mappedVersion) {
        this(unmappedVersion, mappedVersion, null);
    }

    public BackwardsMappings(final String unmappedVersion, final String mappedVersion, @Nullable final Class<? extends Protocol<?, ?, ?, ?>> vvProtocolClass) {
        super(unmappedVersion, mappedVersion);
        Preconditions.checkArgument(vvProtocolClass == null || !vvProtocolClass.isAssignableFrom(BackwardsProtocol.class));
        this.vvProtocolClass = vvProtocolClass;
    }

    @Override
    protected void loadExtras(final CompoundTag data) {
        final CompoundTag itemNames = data.getCompoundTag("itemnames");
        if (itemNames != null) {
            Preconditions.checkNotNull(itemMappings);
            backwardsItemMappings = new Int2ObjectOpenHashMap<>(itemNames.size());

            final CompoundTag extraItemData = data.getCompoundTag("itemdata");
            for (final Map.Entry<String, Tag> entry : itemNames.entrySet()) {
                final StringTag name = (StringTag) entry.getValue();
                final int id = Integer.parseInt(entry.getKey());
                Integer customModelData = null;
                if (extraItemData != null && extraItemData.contains(entry.getKey())) {
                    final CompoundTag entryTag = extraItemData.getCompoundTag(entry.getKey());
                    final NumberTag customModelDataTag = entryTag.getNumberTag("custom_model_data");
                    customModelData = customModelDataTag != null ? customModelDataTag.asInt() : null;
                }

                backwardsItemMappings.put(id, new MappedItem(getNewItemId(id), name.getValue(), customModelData));
            }
        }

        final CompoundTag entityNames = data.getCompoundTag("entitynames");
        if (entityNames != null) {
            this.entityNames = new HashMap<>(entityNames.size());
            for (final Map.Entry<String, Tag> entry : entityNames.entrySet()) {
                final StringTag mappedTag = (StringTag) entry.getValue();
                this.entityNames.put(entry.getKey(), mappedTag.getValue());
            }
        }

        final CompoundTag soundNames = data.getCompoundTag("soundnames");
        if (soundNames != null) {
            backwardsSoundMappings = new HashMap<>(soundNames.size());
            for (final Map.Entry<String, Tag> entry : soundNames.entrySet()) {
                final StringTag mappedTag = (StringTag) entry.getValue();
                backwardsSoundMappings.put(entry.getKey(), mappedTag.getValue());
            }
        }
    }

    @Override
    protected @Nullable BiMappings loadBiMappings(final CompoundTag data, final String key) {
        if (key.equals("items") && vvProtocolClass != null) {
            final Mappings mappings = super.loadMappings(data, key);
            final MappingData mappingData = Via.getManager().getProtocolManager().getProtocol(vvProtocolClass).getMappingData();
            if (mappingData != null && mappingData.getItemMappings() != null) {
                return ItemMappings.of(mappings, mappingData.getItemMappings());
            }
        }
        return super.loadBiMappings(data, key);
    }

    /**
     * @see #getMappedItem(int) for custom backwards mappings
     */
    @Override
    public int getNewItemId(final int id) {
        // Don't warn on missing here
        return this.itemMappings.getNewId(id);
    }

    @Override
    public int getNewBlockId(final int id) {
        // Don't warn on missing here
        return this.blockMappings.getNewId(id);
    }

    @Override
    public int getOldItemId(final int id) {
        // Warn on missing
        return checkValidity(id, this.itemMappings.inverse().getNewId(id), "item");
    }

    public @Nullable MappedItem getMappedItem(final int id) {
        return backwardsItemMappings != null ? backwardsItemMappings.get(id) : null;
    }

    public @Nullable String getMappedNamedSound(final String id) {
        if (backwardsSoundMappings == null) {
            return null;
        }
        return backwardsSoundMappings.get(Key.stripMinecraftNamespace(id));
    }

    public @Nullable String mappedEntityName(final String entityName) {
        if (entityNames == null) {
            getLogger().severe("No entity mappings found when requesting them for " + entityName);
            new Exception().printStackTrace();
            return null;
        }
        return entityNames.get(entityName);
    }

    public @Nullable Int2ObjectMap<MappedItem> getBackwardsItemMappings() {
        return backwardsItemMappings;
    }

    public @Nullable Map<String, String> getBackwardsSoundMappings() {
        return backwardsSoundMappings;
    }

    public @Nullable Class<? extends Protocol<?, ?, ?, ?>> getViaVersionProtocolClass() {
        return vvProtocolClass;
    }

    @Override
    protected Logger getLogger() {
        return ViaBackwards.getPlatform().getLogger();
    }

    @Override
    protected @Nullable CompoundTag readMappingsFile(final String name) {
        return BackwardsMappingDataLoader.INSTANCE.loadNBTFromDir(name);
    }
}
