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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input.Input;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.widget.Widget;
import com.viaversion.viaversion.util.Key;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Button implements Widget {

    public static final Button DEFAULT = defaulted();
    private static final String[] CLICK_EVENTS = {
        "open_url",
        "run_command",
        "suggest_command",
        "show_dialog",
        "change_page",
        "copy_to_clipboard",
        "custom"
    };

    private final Dialog dialog;
    private final Tag label;
    private final int width;

    private final @Nullable Tag tooltip;
    private @Nullable CompoundTag clickEvent;

    private @Nullable Template template;

    private @Nullable String id;
    private @Nullable CompoundTag additions;

    public Button(final Dialog dialog, final CompoundTag tag) {
        this.dialog = dialog;
        this.label = tag.get("label");
        this.tooltip = tag.get("tooltip");

        final int width = tag.getInt("width", 150);
        if (width < 1 || width > 1024) {
            throw new IllegalArgumentException("Width must be between 1 and 1024, got: " + width);
        }
        this.width = width;

        final CompoundTag actionTag = tag.getCompoundTag("action");
        if (actionTag == null) {
            return;
        }

        final String type = actionTag.getString("type");
        if (type == null) {
            throw new IllegalArgumentException("Action type is missing in tag: " + tag);
        }

        for (final String event : CLICK_EVENTS) {
            if (Key.stripMinecraftNamespace(type).equals(event)) {
                clickEvent = actionTag.copy();
                clickEvent.put("action", clickEvent.remove("type"));
                return;
            }
        }

        if (Key.stripMinecraftNamespace(type).equals("dynamic/run_command")) {
            template = Template.fromString(actionTag.getString("template"));
        } else if (Key.stripMinecraftNamespace(type).equals("dynamic/custom")) {
            id = actionTag.getString("id");
            additions = actionTag.getCompoundTag("additions");
        }
    }

    private static Button defaulted() {
        final CompoundTag tag = new CompoundTag();
        final CompoundTag label = new CompoundTag();
        tag.put("label", label);
        label.putString("translate", "gui.ok");

        return new Button(null, tag);
    }

    public static Button openUrl(final Tag label, final String url) {
        final CompoundTag tag = new CompoundTag();
        final CompoundTag actionTag = new CompoundTag();
        final CompoundTag openUrlTag = new CompoundTag();

        tag.put("label", label);
        tag.put("action", actionTag);
        actionTag.putString("type", "open_url");
        actionTag.put("action", openUrlTag);
        openUrlTag.putString("url", url);
        return new Button(null, tag);
    }

    /**
     * Creates a click event for this button based on the provided inputs.
     *
     * @return a CompoundTag representing the click event, which can be used in a button action.
     */
    public @Nullable CompoundTag clickEvent() {
        if (clickEvent != null) {
            return clickEvent;
        }

        if (dialog == null) {
            return null;
        }

        final Input[] inputs = dialog.widgets()
            .stream()
            .filter(widget -> widget instanceof Input)
            .toArray(Input[]::new);

        if (template != null) {
            final Map<String, String> substitutions = new HashMap<>();
            for (final Input input : inputs) {
                substitutions.put(input.key(), input.asCommandSubstitution());
            }

            final CompoundTag tag = new CompoundTag();
            tag.putString("action", "run_command");
            tag.putString("command", template.instantiate(substitutions));
            return tag;
        }

        if (id != null) {
            final CompoundTag additions = this.additions != null ? this.additions.copy() : new CompoundTag();
            for (final Input input : inputs) {
                additions.put(input.key(), input.asTag());
            }

            final CompoundTag tag = new CompoundTag();
            tag.putString("action", "custom");
            tag.putString("id", id);
            tag.put("payload", additions);
            return tag;
        }

        return null;
    }

    public Tag label() {
        return label;
    }

    public @Nullable Tag tooltip() {
        return tooltip;
    }

    public int width() {
        return width;
    }
}
