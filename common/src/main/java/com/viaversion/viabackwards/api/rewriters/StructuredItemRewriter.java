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
import com.viaversion.viabackwards.api.data.MappedItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

public class StructuredItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends ItemRewriter<C, S, T> {

    public StructuredItemRewriter(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType) {
        super(protocol, itemType, itemArrayType);
    }

    public StructuredItemRewriter(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType, Type<Item> mappedItemType, Type<Item[]> mappedItemArrayType) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType);
    }

    @Override
    public @Nullable Item handleItemToClient(@Nullable Item item) {
        if (item == null) {
            return null;
        }

        // TODO Translatable rewriter on name and lore

        MappedItem data = protocol.getMappingData() != null ? protocol.getMappingData().getMappedItem(item.identifier()) : null;
        if (data == null) {
            // Just rewrite the id
            if (protocol.getMappingData() != null && protocol.getMappingData().getItemMappings() != null) {
                item.setIdentifier(protocol.getMappingData().getNewItemId(item.identifier()));
            }
        }

        // TODO Save original id, set remapped id
        if (protocol.getMappingData() != null && protocol.getMappingData().getItemMappings() != null) {
            item.setIdentifier(protocol.getMappingData().getNewItemId(item.identifier()));
        }
        // TODO custom model data and custom name
        return item;
    }

    @Override
    public @Nullable Item handleItemToServer(@Nullable Item item) {
        if (item == null) return null;

        if (protocol.getMappingData() != null && protocol.getMappingData().getItemMappings() != null) {
            item.setIdentifier(protocol.getMappingData().getOldItemId(item.identifier()));
        }

        // TODO restore original id and display
        return item;
    }

    @Override
    protected void restoreDisplayTag(final Item item) {
        // TODO
    }
}
