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
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input.BooleanInput;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input.NumberRangeInput;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input.SingleOptionInput;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input.TextInput;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.widget.ItemWidget;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.widget.TextWidget;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.widget.Widget;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.provider.ChestDialogViewProvider;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage.RegistryAndTags;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage.ServerLinks;
import com.viaversion.viaversion.util.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The dialog structure. See the used subclasses for further details on the
 * specific structure. Note that the data hold here is directly from the server
 * and needs to be remapped manually when used. See {@link ChestDialogViewProvider} for an example
 */
public final class Dialog implements Widget {

    private final List<Widget> widgets = new ArrayList<>();
    private final Tag title;
    private final @Nullable Tag externalTitle;
    private final boolean canCloseWithEscape;
    private final AfterAction afterAction;

    private @Nullable Button actionButton;
    private @Nullable Button yesButton;
    private @Nullable Button noButton;

    private int columns;
    private int buttonWidth;

    public Dialog(final RegistryAndTags registryAndTags, final ServerLinks serverLinks, final CompoundTag tag) {
        final String type = tag.getString("type");
        if (type == null) {
            throw new IllegalArgumentException("Dialog type is missing in tag: " + tag);
        }

        this.title = tag.get("title");
        this.externalTitle = tag.get("external_title");
        this.canCloseWithEscape = tag.getBoolean("can_close_with_escape", true);
        this.afterAction = AfterAction.valueOf(tag.getString("after_action", "close").toUpperCase(Locale.ROOT));

        ListTag<CompoundTag> bodyListTag = tag.getListTag("body", CompoundTag.class);
        if (bodyListTag == null) {
            CompoundTag bodyTag = tag.getCompoundTag("body");
            if (bodyTag != null) {
                bodyListTag = new ListTag<>(CompoundTag.class);
                bodyListTag.add(bodyTag);
            }
        }
        if (bodyListTag != null) {
            for (final CompoundTag bodyTag : bodyListTag) {
                fillBodyWidget(bodyTag);
            }
        }

        final ListTag<CompoundTag> inputListTag = tag.getListTag("inputs", CompoundTag.class);
        if (inputListTag != null) {
            for (final CompoundTag inputTag : inputListTag) {
                fillInputWidget(inputTag);
            }
        }

        switch (Key.stripMinecraftNamespace(type)) {
            case "notice" -> fillNoticeDialog(tag);
            case "server_links" -> fillServerLinksDialog(serverLinks, tag);
            case "dialog_list" -> fillDialogList(registryAndTags, serverLinks, tag);
            case "multi_action" -> fillMultiActionDialog(tag);
            case "confirmation" -> fillConfirmationDialog(tag);
            default -> throw new IllegalArgumentException("Unknown dialog type: " + type + " in tag: " + tag);
        }
    }

    private void fillBodyWidget(final CompoundTag tag) {
        final String type = tag.getString("type");
        if (type == null) {
            throw new IllegalArgumentException("Dialog type is missing in tag: " + tag);
        }

        if (Key.stripMinecraftNamespace(type).equals("plain_message")) {
            widgets.add(new TextWidget(tag));
        } else if (Key.stripMinecraftNamespace(type).equals("item")) {
            widgets.add(new ItemWidget(tag));
        } else {
            throw new IllegalArgumentException("Unknown dialog body type: " + type + " in tag: " + tag);
        }
    }

    private void fillInputWidget(final CompoundTag tag) {
        final String type = tag.getString("type");
        if (type == null) {
            throw new IllegalArgumentException("Dialog type is missing in tag: " + tag);
        }

        widgets.add(switch (Key.stripMinecraftNamespace(type)) {
            case "boolean" -> new BooleanInput(tag);
            case "number_range" -> new NumberRangeInput(tag);
            case "single_option" -> new SingleOptionInput(tag);
            case "text" -> new TextInput(tag);
            default -> throw new IllegalArgumentException("Unknown dialog input type: " + type + " in tag: " + tag);
        });
    }

    private void fillNoticeDialog(final CompoundTag tag) {
        final CompoundTag actionTag = tag.getCompoundTag("action");
        actionButton = actionTag == null ? Button.DEFAULT : new Button(this, actionTag);
    }

    private void fillServerLinksDialog(final ServerLinks serverLinks, final CompoundTag tag) {
        fillDialogBase(tag);
        buttonWidth = tag.getInt("button_width", 150);

        if (serverLinks == null) {
            return;
        }
        for (final Map.Entry<Tag, String> entry : serverLinks.links().entrySet()) {
            final Tag label = entry.getKey();
            final String url = entry.getValue();
            widgets.add(Button.openUrl(label, url));
        }
    }

    private void fillDialogList(final RegistryAndTags registryAndTags, final ServerLinks serverLinks, final CompoundTag tag) {
        // Hold them as either a list of inlined, a singleton inlined, a registry entry or a tag.
        ListTag<CompoundTag> dialogsTag = tag.getListTag("dialogs", CompoundTag.class);
        if (dialogsTag == null) {
            CompoundTag dialogTag = tag.getCompoundTag("dialogs");
            if (dialogTag != null) {
                dialogsTag = new ListTag<>(CompoundTag.class);
                dialogsTag.add(dialogTag);
            }

            StringTag registryDialogTag = tag.getStringTag("dialogs");
            if (registryDialogTag != null) {
                dialogsTag = new ListTag<>(CompoundTag.class);
                final String key = registryDialogTag.getValue();
                if (key.startsWith("#")) {
                    for (final CompoundTag entry : registryAndTags.fromRegistryKey(key.substring(1))) {
                        dialogsTag.add(entry);
                    }
                } else {
                    dialogsTag.add(registryAndTags.fromRegistry(key));
                }
            }
        }
        widgets.addAll(dialogsTag.stream().map(dialog -> new Dialog(registryAndTags, serverLinks, dialog)).toList());

        fillDialogBase(tag);
        buttonWidth = tag.getInt("button_width", 150);
    }

    private void fillDialogBase(final CompoundTag tag) {
        final CompoundTag exitActionTag = tag.getCompoundTag("exit_action");
        actionButton = exitActionTag == null ? null : new Button(this, exitActionTag);

        final int columns = tag.getInt("columns", 2);
        if (columns < 1) {
            throw new IllegalArgumentException("Columns must be non-negative, got: " + columns);
        }
        this.columns = columns;
    }

    private void fillMultiActionDialog(final CompoundTag tag) {
        final ListTag<CompoundTag> actionsTag = tag.getListTag("actions", CompoundTag.class);
        if (actionsTag == null || actionsTag.isEmpty()) {
            throw new IllegalArgumentException("Actions must not be empty in tag: " + tag);
        }
        widgets.addAll(actionsTag.stream().map(actionTag -> new Button(this, actionTag)).toList());

        fillDialogBase(tag);
    }

    private void fillConfirmationDialog(final CompoundTag tag) {
        yesButton = new Button(this, tag.getCompoundTag("yes"));
        noButton = new Button(this, tag.getCompoundTag("no"));
    }

    public Tag title() {
        return title;
    }

    public @Nullable Tag externalTitle() {
        return externalTitle;
    }

    public boolean canCloseWithEscape() {
        return canCloseWithEscape;
    }

    public AfterAction afterAction() {
        return afterAction;
    }

    public enum AfterAction {
        CLOSE,
        NONE,
        WAIT_FOR_RESPONSE
    }

    public @Nullable Button actionButton() {
        return actionButton;
    }

    public @Nullable Button yesButton() {
        return yesButton;
    }

    public @Nullable Button noButton() {
        return noButton;
    }

    public int columns() {
        return columns;
    }

    public int buttonWidth() {
        return buttonWidth;
    }

    public List<Widget> widgets() {
        return widgets;
    }

}
