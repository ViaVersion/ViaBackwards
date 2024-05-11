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
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends BackwardsItemRewriterBase<C, S, T> {

    public BackwardsItemRewriter(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType) {
        super(protocol, itemType, itemArrayType, true);
    }

    public BackwardsItemRewriter(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType, Type<Item> mappedItemType, Type<Item[]> mappedItemArrayType) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType, true);
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        CompoundTag display = item.tag() != null ? item.tag().getCompoundTag("display") : null;
        if (protocol.getTranslatableRewriter() != null && display != null) {
            // Handle name and lore components
            StringTag name = display.getStringTag("Name");
            if (name != null) {
                String newValue = protocol.getTranslatableRewriter().processText(connection, name.getValue()).toString();
                if (!newValue.equals(name.getValue())) {
                    saveStringTag(display, name, "Name");
                }

                name.setValue(newValue);
            }

            ListTag<StringTag> lore = display.getListTag("Lore", StringTag.class);
            if (lore != null) {
                boolean changed = false;
                for (StringTag loreEntry : lore) {
                    String newValue = protocol.getTranslatableRewriter().processText(connection, loreEntry.getValue()).toString();
                    if (!changed && !newValue.equals(loreEntry.getValue())) {
                        // Backup original lore before doing any modifications
                        changed = true;
                        saveListTag(display, lore, "Lore");
                    }

                    loreEntry.setValue(newValue);
                }
            }
        }

        MappedItem data = protocol.getMappingData() != null ? protocol.getMappingData().getMappedItem(item.identifier()) : null;
        if (data == null) {
            // Just rewrite the id
            return super.handleItemToClient(connection, item);
        }

        if (item.tag() == null) {
            item.setTag(new CompoundTag());
        }

        // Save original id, set remapped id
        item.tag().putInt(nbtTagName("id"), item.identifier());
        item.setIdentifier(data.id());

        // Add custom model data
        if (data.customModelData() != null && !item.tag().contains("CustomModelData")) {
            item.tag().putInt("CustomModelData", data.customModelData());
        }

        // Set custom name - only done if there is no original one
        if (display == null) {
            item.tag().put("display", display = new CompoundTag());
        }
        if (!display.contains("Name")) {
            display.put("Name", new StringTag(data.jsonName()));
            display.put(nbtTagName("customName"), new ByteTag(false));
        }
        return item;
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable Item item) {
        if (item == null) return null;

        super.handleItemToServer(connection, item);
        if (item.tag() != null) {
            Tag originalId = item.tag().remove(nbtTagName("id"));
            if (originalId instanceof IntTag) {
                item.setIdentifier(((NumberTag) originalId).asInt());
            }
        }
        return item;
    }

    @Override
    public void registerAdvancements(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    wrapper.passthrough(Types.BOOLEAN); // Reset/clear
                    final int size = wrapper.passthrough(Types.VAR_INT); // Mapping size
                    for (int i = 0; i < size; i++) {
                        wrapper.passthrough(Types.STRING); // Identifier
                        wrapper.passthrough(Types.OPTIONAL_STRING); // Parent

                        // Display data
                        if (wrapper.passthrough(Types.BOOLEAN)) {
                            final JsonElement title = wrapper.passthrough(Types.COMPONENT);
                            final JsonElement description = wrapper.passthrough(Types.COMPONENT);
                            final TranslatableRewriter<C> translatableRewriter = protocol.getTranslatableRewriter();
                            if (translatableRewriter != null) {
                                translatableRewriter.processText(wrapper.user(), title);
                                translatableRewriter.processText(wrapper.user(), description);
                            }

                            final Item icon = handleItemToClient(wrapper.user(), wrapper.read(itemType()));
                            wrapper.write(mappedItemType(), icon);

                            wrapper.passthrough(Types.VAR_INT); // Frame type
                            int flags = wrapper.passthrough(Types.INT); // Flags
                            if ((flags & 1) != 0) {
                                wrapper.passthrough(Types.STRING); // Background texture
                            }
                            wrapper.passthrough(Types.FLOAT); // X
                            wrapper.passthrough(Types.FLOAT); // Y
                        }

                        wrapper.passthrough(Types.STRING_ARRAY); // Criteria

                        final int arrayLength = wrapper.passthrough(Types.VAR_INT);
                        for (int array = 0; array < arrayLength; array++) {
                            wrapper.passthrough(Types.STRING_ARRAY); // String array
                        }
                    }
                });
            }
        });
    }

    @Override
    public void registerAdvancements1_20_3(final C packetType) {
        // Insert translatable rewriter
        protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Types.BOOLEAN); // Reset/clear
            final int size = wrapper.passthrough(Types.VAR_INT); // Mapping size
            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Types.STRING); // Identifier
                wrapper.passthrough(Types.OPTIONAL_STRING); // Parent

                // Display data
                if (wrapper.passthrough(Types.BOOLEAN)) {
                    final Tag title = wrapper.passthrough(Types.TAG);
                    final Tag description = wrapper.passthrough(Types.TAG);
                    final TranslatableRewriter<C> translatableRewriter = protocol.getTranslatableRewriter();
                    if (translatableRewriter != null) {
                        translatableRewriter.processTag(wrapper.user(), title);
                        translatableRewriter.processTag(wrapper.user(), description);
                    }

                    final Item icon = handleItemToClient(wrapper.user(), wrapper.read(itemType()));
                    wrapper.write(mappedItemType(), icon);

                    wrapper.passthrough(Types.VAR_INT); // Frame type
                    final int flags = wrapper.passthrough(Types.INT);
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Types.STRING); // Background texture
                    }
                    wrapper.passthrough(Types.FLOAT); // X
                    wrapper.passthrough(Types.FLOAT); // Y
                }

                final int requirements = wrapper.passthrough(Types.VAR_INT);
                for (int array = 0; array < requirements; array++) {
                    wrapper.passthrough(Types.STRING_ARRAY);
                }

                wrapper.passthrough(Types.BOOLEAN); // Send telemetry
            }
        });
    }
}
