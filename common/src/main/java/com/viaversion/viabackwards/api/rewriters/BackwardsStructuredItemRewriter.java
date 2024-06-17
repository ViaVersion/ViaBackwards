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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.data.MappedItem;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.FullMappings;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntFunction;
import com.viaversion.viaversion.rewriter.StructuredItemRewriter;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsStructuredItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends StructuredItemRewriter<C, S, T> {

    public BackwardsStructuredItemRewriter(final T protocol, final Type<Item> itemType, final Type<Item[]> itemArrayType) {
        super(protocol, itemType, itemArrayType);
    }

    public BackwardsStructuredItemRewriter(final T protocol, final Type<Item> itemType, final Type<Item[]> itemArrayType, final Type<Item> mappedItemType, final Type<Item[]> mappedItemArrayType) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType);
    }

    @Override
    public Item handleItemToClient(final UserConnection connection, final Item item) {
        if (item.isEmpty()) {
            return item;
        }

        final StructuredDataContainer dataContainer = item.dataContainer();
        final BackwardsMappingData mappingData = protocol.getMappingData();
        if (mappingData != null && mappingData.getDataComponentSerializerMappings() != null) {
            final FullMappings mappings = mappingData.getDataComponentSerializerMappings();
            dataContainer.setIdLookup(protocol, true);
            dataContainer.updateIds(protocol, mappings::getNewId);
        }

        if (protocol.getComponentRewriter() != null) {
            // Handle name and lore components
            updateComponent(connection, item, StructuredDataKey.ITEM_NAME, "item_name");
            updateComponent(connection, item, StructuredDataKey.CUSTOM_NAME, "custom_name");

            final StructuredData<Tag[]> loreData = dataContainer.getNonEmpty(StructuredDataKey.LORE);
            if (loreData != null) {
                for (final Tag tag : loreData.value()) {
                    protocol.getComponentRewriter().processTag(connection, tag);
                }
            }
        }

        Int2IntFunction itemIdRewriter = null;
        Int2IntFunction blockIdRewriter = null;
        if (mappingData != null) {
            itemIdRewriter = mappingData.getItemMappings() != null ? mappingData::getNewItemId : null;
            blockIdRewriter = mappingData.getBlockMappings() != null ? mappingData::getNewBlockId : null;
        }

        final MappedItem mappedItem = mappingData != null ? mappingData.getMappedItem(item.identifier()) : null;
        if (mappedItem == null) {
            // Just rewrite the id
            if (mappingData != null && mappingData.getItemMappings() != null) {
                item.setIdentifier(mappingData.getNewItemId(item.identifier()));
            }

            updateItemComponents(connection, dataContainer, this::handleItemToClient, itemIdRewriter, blockIdRewriter);
            return item;
        }

        // Save original id, set remapped id
        final CompoundTag tag = createCustomTag(item);
        tag.putInt(nbtTagName("id"), item.identifier());
        item.setIdentifier(mappedItem.id());

        // Add custom model data
        if (mappedItem.customModelData() != null && !dataContainer.contains(StructuredDataKey.CUSTOM_MODEL_DATA)) {
            dataContainer.set(StructuredDataKey.CUSTOM_MODEL_DATA, mappedItem.customModelData());
        }

        // Set custom name - only done if there is no original one
        if (!dataContainer.contains(StructuredDataKey.CUSTOM_NAME)) {
            dataContainer.set(StructuredDataKey.CUSTOM_NAME, mappedItem.tagName());
            tag.putBoolean(nbtTagName("added_custom_name"), true);
        }

        updateItemComponents(connection, dataContainer, this::handleItemToClient, itemIdRewriter, blockIdRewriter);
        return item;
    }

    @Override
    public Item handleItemToServer(final UserConnection connection, final Item item) {
        if (item.isEmpty()) {
            return item;
        }

        final BackwardsMappingData mappingData = protocol.getMappingData();
        final StructuredDataContainer dataContainer = item.dataContainer();
        if (mappingData != null) {
            if (mappingData.getItemMappings() != null) {
                item.setIdentifier(mappingData.getOldItemId(item.identifier()));
            }

            final FullMappings dataComponentMappings = mappingData.getDataComponentSerializerMappings();
            if (dataComponentMappings != null) {
                dataContainer.setIdLookup(protocol, false);
                dataContainer.updateIds(protocol, id -> dataComponentMappings.inverse().getNewId(id));
            }
        }


        final CompoundTag tag = customTag(item);
        if (tag != null) {
            final Tag originalId = tag.remove(nbtTagName("id"));
            if (originalId instanceof IntTag) {
                item.setIdentifier(((NumberTag) originalId).asInt());
            }
        }

        restoreTextComponents(item);

        Int2IntFunction itemIdRewriter = null;
        Int2IntFunction blockIdRewriter = null;
        if (mappingData != null) {
            itemIdRewriter = mappingData.getItemMappings() != null ? mappingData::getOldItemId : null;
            blockIdRewriter = mappingData.getBlockMappings() != null ? mappingData::getOldBlockId : null;
        }
        updateItemComponents(connection, dataContainer, this::handleItemToServer, itemIdRewriter, blockIdRewriter);
        return item;
    }

    protected @Nullable CompoundTag customTag(final Item item) {
        final StructuredData<CompoundTag> customData = item.dataContainer().getNonEmpty(StructuredDataKey.CUSTOM_DATA);
        return customData != null ? customData.value() : null;
    }

    protected void saveListTag(CompoundTag tag, ListTag<?> original, String name) {
        // Multiple places might try to backup data
        String backupName = nbtTagName(name);
        if (!tag.contains(backupName)) {
            tag.put(backupName, original.copy());
        }
    }

    public <T extends Tag> @Nullable ListTag<T> removeListTag(CompoundTag tag, String tagName, Class<T> tagType) {
        String backupName = nbtTagName(tagName);
        ListTag<T> data = tag.getListTag(backupName, tagType);
        if (data == null) {
            return null;
        }

        tag.remove(backupName);
        return data;
    }

    protected void saveGenericTagList(CompoundTag tag, List<Tag> original, String name) {
        // List tags cannot contain tags of different types, so we have to store them a bit more awkwardly as an indexed compound tag
        String backupName = nbtTagName(name);
        if (!tag.contains(backupName)) {
            CompoundTag output = new CompoundTag();
            for (int i = 0; i < original.size(); i++) {
                output.put(Integer.toString(i), original.get(i));
            }
            tag.put(backupName, output);
        }
    }

    protected List<Tag> removeGenericTagList(CompoundTag tag, String name) {
        String backupName = nbtTagName(name);
        CompoundTag data = tag.getCompoundTag(backupName);
        if (data == null) {
            return null;
        }

        tag.remove(backupName);
        return new ArrayList<>(data.values());
    }

    @Override
    public String nbtTagName() {
        return "VB|" + protocol.getClass().getSimpleName();
    }
}
