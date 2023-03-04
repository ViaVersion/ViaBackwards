/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
        T extends BackwardsProtocol<C, ?, ?, S>> extends ItemRewriterBase<C, S, T> {

    public ItemRewriter(T protocol) {
        super(protocol, true);
    }

    @Override
    public @Nullable Item handleItemToClient(@Nullable Item item) {
        if (item == null) {
            return null;
        }

        CompoundTag display = item.tag() != null ? item.tag().get("display") : null;
        if (protocol.getTranslatableRewriter() != null && display != null) {
            // Handle name and lore components
            StringTag name = display.get("Name");
            if (name != null) {
                String newValue = protocol.getTranslatableRewriter().processText(name.getValue()).toString();
                if (!newValue.equals(name.getValue())) {
                    saveStringTag(display, name, "Name");
                }

                name.setValue(newValue);
            }

            ListTag lore = display.get("Lore");
            if (lore != null) {
                boolean changed = false;
                for (Tag loreEntryTag : lore) {
                    if (!(loreEntryTag instanceof StringTag)) continue;

                    StringTag loreEntry = (StringTag) loreEntryTag;
                    String newValue = protocol.getTranslatableRewriter().processText(loreEntry.getValue()).toString();
                    if (!changed && !newValue.equals(loreEntry.getValue())) {
                        // Backup original lore before doing any modifications
                        changed = true;
                        saveListTag(display, lore, "Lore");
                    }

                    loreEntry.setValue(newValue);
                }
            }
        }

        MappedItem data = protocol.getMappingData().getMappedItem(item.identifier());
        if (data == null) {
            // Just rewrite the id
            return super.handleItemToClient(item);
        }

        if (item.tag() == null) {
            item.setTag(new CompoundTag());
        }

        // Save original id, set remapped id
        item.tag().put(nbtTagName + "|id", new IntTag(item.identifier()));
        item.setIdentifier(data.getId());

        // Add custom model data
        if (data.customModelData() != null && !item.tag().contains("CustomModelData")) {
            item.tag().put("CustomModelData", new IntTag(data.customModelData()));
        }

        // Set custom name - only done if there is no original one
        if (display == null) {
            item.tag().put("display", display = new CompoundTag());
        }
        if (!display.contains("Name")) {
            display.put("Name", new StringTag(data.getJsonName()));
            display.put(nbtTagName + "|customName", new ByteTag());
        }
        return item;
    }

    @Override
    public @Nullable Item handleItemToServer(@Nullable Item item) {
        if (item == null) return null;

        super.handleItemToServer(item);
        if (item.tag() != null) {
            IntTag originalId = item.tag().remove(nbtTagName + "|id");
            if (originalId != null) {
                item.setIdentifier(originalId.asInt());
            }
        }
        return item;
    }

    @Override
    public void registerAdvancements(C packetType, Type<Item> type) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    wrapper.passthrough(Type.BOOLEAN); // Reset/clear
                    final int size = wrapper.passthrough(Type.VAR_INT); // Mapping size
                    for (int i = 0; i < size; i++) {
                        wrapper.passthrough(Type.STRING); // Identifier

                        // Parent
                        if (wrapper.passthrough(Type.BOOLEAN)) {
                            wrapper.passthrough(Type.STRING);
                        }

                        // Display data
                        if (wrapper.passthrough(Type.BOOLEAN)) {
                            final JsonElement title = wrapper.passthrough(Type.COMPONENT);
                            final JsonElement description = wrapper.passthrough(Type.COMPONENT);
                            final TranslatableRewriter<C> translatableRewriter = protocol.getTranslatableRewriter();
                            if (translatableRewriter != null) {
                                translatableRewriter.processText(title);
                                translatableRewriter.processText(description);
                            }

                            handleItemToClient(wrapper.passthrough(type)); // Icon
                            wrapper.passthrough(Type.VAR_INT); // Frame type
                            int flags = wrapper.passthrough(Type.INT); // Flags
                            if ((flags & 1) != 0) {
                                wrapper.passthrough(Type.STRING); // Background texture
                            }
                            wrapper.passthrough(Type.FLOAT); // X
                            wrapper.passthrough(Type.FLOAT); // Y
                        }

                        wrapper.passthrough(Type.STRING_ARRAY); // Criteria

                        final int arrayLength = wrapper.passthrough(Type.VAR_INT);
                        for (int array = 0; array < arrayLength; array++) {
                            wrapper.passthrough(Type.STRING_ARRAY); // String array
                        }
                    }
                });
            }
        });
    }
}
