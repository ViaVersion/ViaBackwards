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

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.data.MappedItem;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsStructuredItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends BackwardsItemRewriter<C, S, T> {

    protected final StructuredEnchantmentRewriter enchantmentRewriter = new StructuredEnchantmentRewriter(this);

    public BackwardsStructuredItemRewriter(final T protocol, final Type<Item> itemType, final Type<Item[]> itemArrayType) {
        super(protocol, itemType, itemArrayType);
    }

    public BackwardsStructuredItemRewriter(final T protocol, final Type<Item> itemType, final Type<Item[]> itemArrayType, final Type<Item> mappedItemType, final Type<Item[]> mappedItemArrayType) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType);
    }

    @Override
    public @Nullable Item handleItemToClient(final UserConnection connection, @Nullable final Item item) {
        if (item == null) {
            return null;
        }

        final StructuredDataContainer data = item.structuredData();
        data.setIdLookup(protocol, true);

        if (protocol.getMappingData().getEnchantmentMappings() != null) {
            enchantmentRewriter.handleToClient(item);
        }

        if (protocol.getTranslatableRewriter() != null) {
            // Handle name and lore components
            final StructuredData<Tag> customNameData = data.getNonEmpty(StructuredDataKey.CUSTOM_NAME);
            if (customNameData != null) {
                final Tag originalName = customNameData.value().copy();
                protocol.getTranslatableRewriter().processTag(connection, customNameData.value());
                if (!customNameData.value().equals(originalName)) {
                    saveTag(createCustomTag(item), originalName, "Name");
                }
            }

            final StructuredData<Tag[]> loreData = data.getNonEmpty(StructuredDataKey.LORE);
            if (loreData != null) {
                for (final Tag tag : loreData.value()) {
                    protocol.getTranslatableRewriter().processTag(connection, tag);
                }
            }
        }

        final BackwardsMappings mappingData = protocol.getMappingData();
        final MappedItem mappedItem = mappingData != null ? mappingData.getMappedItem(item.identifier()) : null;
        if (mappedItem == null) {
            // Just rewrite the id
            if (mappingData != null && mappingData.getItemMappings() != null) {
                item.setIdentifier(mappingData.getNewItemId(item.identifier()));
            }
            return item;
        }

        // Save original id, set remapped id
        final CompoundTag tag = createCustomTag(item);
        tag.putInt(nbtTagName("id"), item.identifier());
        item.setIdentifier(mappedItem.id());

        // Add custom model data
        if (mappedItem.customModelData() != null && !data.contains(StructuredDataKey.CUSTOM_MODEL_DATA)) {
            data.set(StructuredDataKey.CUSTOM_MODEL_DATA, mappedItem.customModelData());
        }

        // Set custom name - only done if there is no original one
        if (!data.contains(StructuredDataKey.CUSTOM_NAME)) {
            data.set(StructuredDataKey.CUSTOM_NAME, mappedItem.tagName());
            tag.putBoolean(nbtTagName("customName"), true);
        }
        return item;
    }

    @Override
    public @Nullable Item handleItemToServer(final UserConnection connection, @Nullable final Item item) {
        if (item == null) {
            return null;
        }

        final BackwardsMappings mappingData = protocol.getMappingData();
        if (mappingData != null && mappingData.getItemMappings() != null) {
            item.setIdentifier(mappingData.getOldItemId(item.identifier()));
        }

        final StructuredDataContainer data = item.structuredData();
        data.setIdLookup(protocol, false);

        if (protocol.getMappingData().getEnchantmentMappings() != null) {
            enchantmentRewriter.handleToServer(item);
        }

        final CompoundTag tag = customTag(item);
        if (tag != null) {
            final Tag originalId = tag.remove(nbtTagName("id"));
            if (originalId instanceof IntTag) {
                item.setIdentifier(((NumberTag) originalId).asInt());
            }
        }

        restoreDisplayTag(item);
        return item;
    }

    protected @Nullable CompoundTag customTag(final Item item) {
        final StructuredData<CompoundTag> customData = item.structuredData().getNonEmpty(StructuredDataKey.CUSTOM_DATA);
        return customData != null ? customData.value() : null;
    }

    protected CompoundTag createCustomTag(final Item item) {
        final StructuredDataContainer data = item.structuredData();
        final StructuredData<CompoundTag> customData = data.getNonEmpty(StructuredDataKey.CUSTOM_DATA);
        if (customData != null) {
            return customData.value();
        }

        final CompoundTag tag = new CompoundTag();
        data.set(StructuredDataKey.CUSTOM_DATA, tag);
        return tag;
    }

    @Override
    protected void restoreDisplayTag(final Item item) {
        final StructuredDataContainer data = item.structuredData();
        final StructuredData<CompoundTag> customData = data.getNonEmpty(StructuredDataKey.CUSTOM_DATA);
        if (customData == null) {
            return;
        }

        // Remove custom name
        if (customData.value().remove(nbtTagName("customName")) != null) {
            data.remove(StructuredDataKey.CUSTOM_NAME);
        } else {
            final Tag name = removeBackupTag(customData.value(), "Name");
            if (name != null) {
                data.set(StructuredDataKey.CUSTOM_NAME, name);
            }
        }
    }

    protected void saveTag(CompoundTag customData, Tag tag, String name) {
        String backupName = nbtTagName(name);
        if (!customData.contains(backupName)) {
            customData.put(backupName, tag);
        }
    }

    protected @Nullable Tag removeBackupTag(CompoundTag customData, String tagName) {
        return customData.remove(nbtTagName(tagName));
    }
}
