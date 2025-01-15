/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPacket1_21_5;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.SerializerVersion;
import com.viaversion.viaversion.util.TagUtil;

public final class ComponentRewriter1_21_5 extends NBTComponentRewriter<ClientboundPacket1_21_5> {

    public ComponentRewriter1_21_5(final BackwardsProtocol<ClientboundPacket1_21_5, ?, ?, ?> protocol) {
        super(protocol);
    }

    @Override
    protected void processCompoundTag(final UserConnection connection, final CompoundTag tag) {
        super.processCompoundTag(connection, tag);
        if (tag.remove("hover_event") instanceof final CompoundTag hoverEvent) {
            tag.put("hoverEvent", hoverEvent);
        }

        if (tag.remove("click_event") instanceof final CompoundTag clickEvent) {
            tag.put("clickEvent", clickEvent);
            updateClickEvent(clickEvent);
        }
    }

    @Override
    protected void handleHoverEvent(final UserConnection connection, final CompoundTag hoverEventTag) {
        final String action = hoverEventTag.getString("action");
        if (action == null) {
            return;
        }

        switch (action) {
            case "show_text" -> updateShowTextHover(connection, hoverEventTag);
            case "show_entity" -> updateShowEntityHover(connection, hoverEventTag);
            case "show_item" -> updateShowItemHover(connection, hoverEventTag);
        }
    }

    private void updateClickEvent(final CompoundTag clickEventTag) {
        final String action = clickEventTag.getString("action");
        if (action == null) {
            return;
        }

        switch (action) {
            case "open_url" -> clickEventTag.put("value", clickEventTag.getStringTag("url"));
            case "change_page" -> clickEventTag.putString("value", Integer.toString(clickEventTag.getInt("page")));
            case "run_command", "suggest_command" -> clickEventTag.put("value", clickEventTag.getStringTag("command"));
        }
    }

    private void updateShowTextHover(final UserConnection connection, final CompoundTag hoverEventTag) {
        final Tag text = hoverEventTag.remove("value");
        hoverEventTag.put("contents", text);
    }

    private void updateShowItemHover(final UserConnection connection, final CompoundTag hoverEventTag) {
        final CompoundTag contents = new CompoundTag();
        hoverEventTag.put("contents", contents);

        if (hoverEventTag.get("count") instanceof NumberTag countTag) {
            contents.put("count", countTag);
        }
        if (hoverEventTag.get("id") instanceof StringTag idTag) {
            contents.put("id", idTag);
        }

        final CompoundTag componentsTag = hoverEventTag.getCompoundTag("components");
        handleShowItem(connection, contents, componentsTag);
        if (componentsTag == null) {
            return;
        }

        hoverEventTag.remove("components");
        contents.put("components", componentsTag);

        final CompoundTag useRemainder = TagUtil.getNamespacedCompoundTag(componentsTag, "use_remainder");
        if (useRemainder != null) {
            handleShowItem(connection, useRemainder);
        }
        handleContainerContents(connection, componentsTag);
        handleItemArrayContents(connection, componentsTag, "bundle_contents");
        handleItemArrayContents(connection, componentsTag, "charged_projectiles");
        handleWrittenBookContents(connection, componentsTag);

        insertUglyJson(componentsTag, connection);
    }

    private void insertUglyJson(final CompoundTag componentsTag, final UserConnection connection) {
        insertUglyJson(componentsTag, "item_name", connection);
        insertUglyJson(componentsTag, "custom_name", connection);

        final String loreKey = componentsTag.contains("lore") ? "lore" : "minecraft:lore";
        final ListTag<?> lore = componentsTag.getListTag(loreKey);
        if (lore == null) {
            return;
        }

        final ListTag<StringTag> updatedLore = new ListTag<>(StringTag.class);
        componentsTag.put(loreKey, updatedLore);
        for (final Tag line : lore) {
            updatedLore.add(new StringTag(toUglyJson(connection, line)));
        }
    }

    private void insertUglyJson(final CompoundTag componentsTag, final String key, final UserConnection connection) {
        String actualKey = Key.namespaced(key);
        Tag tag = componentsTag.get(actualKey);
        if (tag == null) {
            actualKey = Key.stripMinecraftNamespace(key);
            tag = componentsTag.get(actualKey);
            if (tag == null) {
                return;
            }
        }

        componentsTag.putString(actualKey, toUglyJson(connection, tag));
    }

    private void updateShowEntityHover(final UserConnection connection, final CompoundTag hoverEventTag) {
        final CompoundTag contents = new CompoundTag();
        hoverEventTag.put("contents", contents);

        final Tag nameTag = hoverEventTag.remove("name");
        if (nameTag != null) {
            contents.put("name", nameTag);
        }

        if (hoverEventTag.remove("id") instanceof StringTag typeTag) {
            contents.put("type", typeTag);
        }

        final Tag uuidTag = hoverEventTag.remove("uuid");
        if (uuidTag != null) {
            contents.put("id", uuidTag);
        }
    }

    private String toUglyJson(final UserConnection connection, final Tag value) {
        processTag(connection, value);
        return SerializerVersion.V1_21_4.toString(SerializerVersion.V1_21_4.toComponent(value));
    }

    @Override
    protected void handleWrittenBookContents(final UserConnection connection, final CompoundTag tag) {
        final CompoundTag book = TagUtil.getNamespacedCompoundTag(tag, "written_book_content");
        if (book == null) {
            return;
        }

        final ListTag<CompoundTag> pagesTag = book.getListTag("pages", CompoundTag.class);
        if (pagesTag == null) {
            return;
        }

        for (final CompoundTag compoundTag : pagesTag) {
            final Tag raw = compoundTag.get("raw");
            compoundTag.putString("raw", toUglyJson(connection, raw));

            final Tag filtered = compoundTag.get("filtered");
            if (filtered != null) {
                compoundTag.putString("filtered", toUglyJson(connection, raw));
            }
        }
    }
}
