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
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.BlockItemPacketRewriter1_21_5;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.SerializerVersion;
import com.viaversion.viaversion.util.TagUtil;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.viaversion.viaversion.util.TagUtil.getNamespacedCompoundTag;
import static com.viaversion.viaversion.util.TagUtil.getNamespacedCompoundTagList;
import static com.viaversion.viaversion.util.TagUtil.getNamespacedNumberTag;
import static com.viaversion.viaversion.util.TagUtil.removeNamespaced;

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
            case "show_text" -> updateShowTextHover(hoverEventTag);
            case "show_entity" -> updateShowEntityHover(hoverEventTag);
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
            case "run_command" -> {
                final StringTag command = clickEventTag.getStringTag("command");
                if (command != null && !command.getValue().startsWith("/")) {
                    command.setValue("/" + command.getValue());
                }
                clickEventTag.put("value", command);
            }
            case "suggest_command" -> clickEventTag.put("value", clickEventTag.getStringTag("command"));
        }
    }

    private void updateShowTextHover(final CompoundTag hoverEventTag) {
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
        if (componentsTag != null) {
            hoverEventTag.remove("components");
            contents.put("components", componentsTag);
        }
    }

    @Override
    protected void handleShowItem(final UserConnection connection, final CompoundTag itemTag, @Nullable final CompoundTag componentsTag) {
        super.handleShowItem(connection, itemTag, componentsTag);
        if (componentsTag == null) {
            return;
        }

        insertUglyJson(componentsTag, connection);
        updateDataComponents(componentsTag);

        removeDataComponents(componentsTag, BlockItemPacketRewriter1_21_5.NEW_DATA_TO_REMOVE);
    }

    private void updateDataComponents(final CompoundTag componentsTag) {
        final CompoundTag tooltipDisplay = getNamespacedCompoundTag(componentsTag, "tooltip_display");
        Set<String> hiddenComponents = Set.of();
        if (tooltipDisplay != null) {
            final ListTag<StringTag> hiddenComponentsTag = tooltipDisplay.getListTag("hidden_components", StringTag.class);
            if (hiddenComponentsTag != null) {
                hiddenComponents = new HashSet<>(hiddenComponentsTag.size());
                for (final StringTag stringTag : hiddenComponentsTag) {
                    hiddenComponents.add(Key.stripMinecraftNamespace(stringTag.getValue()));
                }
            }
        }
        if (hiddenComponents.containsAll(BlockItemPacketRewriter1_21_5.HIDE_ADDITIONAL_KEYS.stream().map(StructuredDataKey::identifier).toList())) {
            componentsTag.put("hide_additional_tooltip", new CompoundTag());
        }

        final ListTag<CompoundTag> attributeModifiers = getNamespacedCompoundTagList(componentsTag, "attribute_modifiers");
        if (attributeModifiers != null) {
            removeNamespaced(componentsTag, "attribute_modifiers");
            final CompoundTag attributesParent = new CompoundTag();
            attributesParent.put("modifiers", attributeModifiers);
            attributesParent.putBoolean("show_in_tooltip", hiddenComponents.contains("attribute_modifiers"));
            componentsTag.put("attribute_modifiers", attributesParent);
        }

        final NumberTag dyedColor = getNamespacedNumberTag(componentsTag, "dyed_color");
        if (dyedColor != null) {
            removeNamespaced(componentsTag, "dyed_color");
            final CompoundTag dyedColorParent = new CompoundTag();
            dyedColorParent.put("rgb", dyedColor);
            dyedColorParent.putBoolean("show_in_tooltip", hiddenComponents.contains("dyed_color"));
            componentsTag.put("dyed_color", dyedColorParent);
        }

        updateShowInTooltip(componentsTag, "unbreakable", hiddenComponents);
        updateShowInTooltip(componentsTag, "dyed_color", hiddenComponents);
        updateShowInTooltip(componentsTag, "trim", hiddenComponents);
        updateShowInTooltip(componentsTag, "jukebox_playable", hiddenComponents);
        handleAdventureModePredicate(componentsTag, "can_place_on", hiddenComponents);
        handleAdventureModePredicate(componentsTag, "can_break", hiddenComponents);
        handleEnchantments(componentsTag, "enchantments", hiddenComponents);
        handleEnchantments(componentsTag, "stored_enchantments", hiddenComponents);
    }

    private void updateShowInTooltip(final CompoundTag tag, final String key, final Set<String> hiddenComponents) {
        final CompoundTag data = getNamespacedCompoundTag(tag, key);
        if (data != null) {
            data.putBoolean("show_in_tooltip", !hiddenComponents.contains(key));
        }
    }

    private void handleAdventureModePredicate(final CompoundTag componentsTag, final String key, final Set<String> hiddenComponents) {
        final ListTag<CompoundTag> blockPredicates = getNamespacedCompoundTagList(componentsTag, key);
        if (blockPredicates == null) {
            return;
        }

        removeDataComponents(componentsTag, key);
        final CompoundTag predicate = new CompoundTag();
        predicate.put("predicates", blockPredicates);
        predicate.putBoolean("show_in_tooltip", !hiddenComponents.contains(key));
        componentsTag.put(key, predicate);
    }

    private void handleEnchantments(final CompoundTag componentsTag, final String key, final Set<String> hiddenComponents) {
        final CompoundTag levels = getNamespacedCompoundTag(componentsTag, key);
        if (levels != null) {
            removeNamespaced(componentsTag, key);
            final CompoundTag enchantments = new CompoundTag();
            enchantments.put("levels", levels);
            enchantments.putBoolean("show_in_tooltip", !hiddenComponents.contains(key));
            componentsTag.put(key, enchantments);
        }
    }

    private void insertUglyJson(final CompoundTag componentsTag, final UserConnection connection) {
        insertUglyJson(componentsTag, "item_name", connection);
        insertUglyJson(componentsTag, "custom_name", connection);

        final String loreKey = TagUtil.getNamespacedTagKey(componentsTag, "lore");
        final ListTag<?> lore = componentsTag.getListTag(loreKey);
        if (lore != null) {
            componentsTag.put(loreKey, updateComponentList(connection, lore));
        }
    }

    public ListTag<StringTag> updateComponentList(final UserConnection connection, final ListTag<?> messages) {
        final ListTag<StringTag> updatedMessages = new ListTag<>(StringTag.class);
        for (final Tag message : messages) {
            updatedMessages.add(new StringTag(toUglyJson(connection, message)));
        }
        return updatedMessages;
    }

    private void insertUglyJson(final CompoundTag componentsTag, final String key, final UserConnection connection) {
        final String actualKey = TagUtil.getNamespacedTagKey(componentsTag, key);
        final Tag tag = componentsTag.get(actualKey);
        if (tag == null) {
            return;
        }

        componentsTag.putString(actualKey, toUglyJson(connection, tag));
    }

    private void updateShowEntityHover(final CompoundTag hoverEventTag) {
        final CompoundTag contents = new CompoundTag();
        hoverEventTag.put("contents", contents);

        final Tag nameTag = hoverEventTag.remove("name");
        if (nameTag != null) {
            contents.put("name", nameTag);
        }

        if (hoverEventTag.remove("id") instanceof StringTag idTag) {
            idTag.setValue(protocol.getEntityRewriter().mappedEntityIdentifier(idTag.getValue()));
            contents.put("type", idTag);
        }

        final Tag uuidTag = hoverEventTag.remove("uuid");
        if (uuidTag != null) {
            contents.put("id", uuidTag);
        }
    }

    String toUglyJson(final UserConnection connection, final Tag value) {
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
