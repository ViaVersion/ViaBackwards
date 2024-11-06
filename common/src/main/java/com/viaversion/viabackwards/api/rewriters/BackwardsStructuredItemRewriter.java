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
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.data.MappedItem;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.CustomModelData1_21_4;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.rewriter.StructuredItemRewriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsStructuredItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends StructuredItemRewriter<C, S, T> {

    public BackwardsStructuredItemRewriter(
        T protocol,
        Type<Item> itemType, Type<Item[]> itemArrayType, Type<Item> mappedItemType, Type<Item[]> mappedItemArrayType,
        Type<Item> itemCostType, Type<Item> optionalItemCostType, Type<Item> mappedItemCostType, Type<Item> mappedOptionalItemCostType
    ) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType, itemCostType, optionalItemCostType, mappedItemCostType, mappedOptionalItemCostType);
    }

    public BackwardsStructuredItemRewriter(final T protocol, final Type<Item> itemType, final Type<Item[]> itemArrayType, final Type<Item> mappedItemType, final Type<Item[]> mappedItemArrayType) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType);
    }

    public BackwardsStructuredItemRewriter(final T protocol, final Type<Item> itemType, final Type<Item[]> itemArrayType) {
        super(protocol, itemType, itemArrayType);
    }

    @Override
    public Item handleItemToClient(final UserConnection connection, final Item item) {
        if (item.isEmpty()) {
            return item;
        }

        final StructuredDataContainer dataContainer = item.dataContainer();
        updateItemDataComponentTypeIds(dataContainer, true);

        final BackwardsMappingData mappingData = protocol.getMappingData();
        final MappedItem mappedItem = mappingData != null ? mappingData.getMappedItem(item.identifier()) : null;
        if (mappedItem == null) {
            // Just rewrite the id
            if (mappingData != null && mappingData.getItemMappings() != null) {
                item.setIdentifier(mappingData.getNewItemId(item.identifier()));
            }

            updateItemDataComponents(connection, item, true);
            return item;
        }

        // Save original id, set remapped id
        final CompoundTag tag = createCustomTag(item);
        tag.putInt(nbtTagName("id"), item.identifier());
        item.setIdentifier(mappedItem.id());

        // Add custom model data
        if (mappedItem.customModelData() != null) {
            if (connection.getProtocolInfo().protocolVersion().newerThanOrEqualTo(ProtocolVersion.v1_21_4)) {
                if (!dataContainer.has(StructuredDataKey.CUSTOM_MODEL_DATA1_21_4)) {
                    dataContainer.set(StructuredDataKey.CUSTOM_MODEL_DATA1_21_4, new CustomModelData1_21_4(
                        new float[]{mappedItem.customModelData().floatValue()},
                        new boolean[0],
                        new String[0],
                        new int[0]
                    ));
                }
            } else if (!dataContainer.has(StructuredDataKey.CUSTOM_MODEL_DATA1_20_5)) {
                dataContainer.set(StructuredDataKey.CUSTOM_MODEL_DATA1_20_5, mappedItem.customModelData());
            }
        }

        // Set custom name - only done if there is no original one
        if (!dataContainer.has(StructuredDataKey.CUSTOM_NAME)) {
            dataContainer.set(StructuredDataKey.CUSTOM_NAME, mappedItem.tagName());
            tag.putBoolean(nbtTagName("added_custom_name"), true);
        }

        updateItemDataComponents(connection, item, true);
        return item;
    }

    @Override
    public Item handleItemToServer(final UserConnection connection, final Item item) {
        if (item.isEmpty()) {
            return item;
        }

        final StructuredDataContainer dataContainer = item.dataContainer();
        updateItemDataComponentTypeIds(dataContainer, false);

        final BackwardsMappingData mappingData = protocol.getMappingData();
        if (mappingData != null && mappingData.getItemMappings() != null) {
            item.setIdentifier(mappingData.getOldItemId(item.identifier()));
        }

        final CompoundTag customData = dataContainer.get(StructuredDataKey.CUSTOM_DATA);
        if (customData != null) {
            if (customData.remove(nbtTagName("id")) instanceof final IntTag originalTag) {
                item.setIdentifier(originalTag.asInt());
                removeCustomTag(dataContainer, customData);
            }
        }

        restoreTextComponents(item);
        updateItemDataComponents(connection, item, false);
        return item;
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

    protected Tag holderSetToTag(final HolderSet set) {
        if (set.hasIds()) {
            return new IntArrayTag(set.ids());
        } else {
            return new StringTag(set.tagKey());
        }
    }

    protected HolderSet restoreHolderSet(final CompoundTag tag, final String key) {
        final Tag savedTag = tag.get(key);
        if (savedTag == null) {
            return HolderSet.of(new int[0]);
        }

        if (savedTag instanceof StringTag tagKey) {
            return HolderSet.of(tagKey.getValue());
        } else if (savedTag instanceof IntArrayTag idsTag) {
            return HolderSet.of(idsTag.getValue());
        } else {
            return HolderSet.of(new int[0]);
        }
    }

    protected <V> Tag holderToTag(final Holder<V> holder, final BiConsumer<V, CompoundTag> valueSaveFunction) {
        if (holder.hasId()) {
            return new IntTag(holder.id());
        } else {
            final CompoundTag savedTag = new CompoundTag();
            valueSaveFunction.accept(holder.value(), savedTag);
            return savedTag;
        }
    }

    protected <V> Holder<V> restoreHolder(final CompoundTag tag, final String key, final Function<CompoundTag, V> valueRestoreFunction) {
        final Tag savedTag = tag.get(key);
        if (savedTag == null) {
            return Holder.of(0);
        }

        if (savedTag instanceof IntTag idTag) {
            return Holder.of(idTag.asInt());
        } else if (savedTag instanceof CompoundTag compoundTag) {
            return Holder.of(valueRestoreFunction.apply(compoundTag));
        } else {
            return Holder.of(0);
        }
    }

    @Override
    public String nbtTagName() {
        return "VB|" + protocol.getClass().getSimpleName();
    }
}
