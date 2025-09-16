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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.provider;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.DialogStyleConfig;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.Protocol1_21_6To1_21_5;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.Button;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.Dialog;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input.BooleanInput;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input.NumberRangeInput;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input.SingleOptionInput;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input.TextInput;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.widget.ItemWidget;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.widget.TextWidget;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.widget.Widget;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage.ChestDialogStorage;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage.ClickEvents;
import com.viaversion.viabackwards.utils.ChatUtil;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.StructuredItem;
import com.viaversion.viaversion.api.minecraft.item.data.TooltipDisplay;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.libs.fastutil.ints.IntSortedSets;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.MathUtil;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.viaversion.viabackwards.utils.ChatUtil.fixStyle;
import static com.viaversion.viabackwards.utils.ChatUtil.translate;

/**
 * Default dialog view emulator. The layout has been stretched into a lot sub-functions for plugins to override.
 */
public class ChestDialogViewProvider implements DialogViewProvider {

    private static final int INVENTORY_SIZE = 27;
    private static final int INVENTORY_LAST_ROW = INVENTORY_SIZE - 9;

    private final Protocol1_21_6To1_21_5 protocol;

    public ChestDialogViewProvider(final Protocol1_21_6To1_21_5 protocol) {
        this.protocol = protocol;
    }

    @Override
    public void openDialog(final UserConnection connection, final Dialog dialog) {
        final State state = connection.getProtocolInfo().getClientState();
        if (state == State.CONFIGURATION) {
            // TODO Implement by ending and re-starting the configuration phase
            return;
        }

        // Batch text widgets following one another into a single MultiTextWidget to be display properly.
        final List<Tag> texts = new ArrayList<>();
        for (final Widget widget : new ArrayList<>(dialog.widgets())) {
            if (widget instanceof final TextWidget textWidget) {
                texts.add(textWidget.label());
                dialog.widgets().remove(textWidget);
            } else if (!texts.isEmpty()) {
                dialog.widgets().add(dialog.widgets().indexOf(widget), new MultiTextWidget(texts.toArray(Tag[]::new)));
                texts.clear();
            }
        }

        final ChestDialogStorage previousStorage = connection.get(ChestDialogStorage.class);
        final ChestDialogStorage storage = new ChestDialogStorage(this, dialog);
        if (previousStorage != null) {
            storage.setPreviousDialog(previousStorage.dialog());
        }
        connection.put(storage);

        openChestView(connection, storage, ChestDialogStorage.Phase.DIALOG_VIEW);
    }

    public void openChestView(final UserConnection connection, final ChestDialogStorage storage, ChestDialogStorage.Phase phase) {
        storage.setPhase(connection, phase);

        final PacketWrapper openScreen = PacketWrapper.create(ClientboundPackets1_21_5.OPEN_SCREEN, connection);
        openScreen.write(Types.VAR_INT, storage.containerId());
        openScreen.write(Types.VAR_INT, 2); // Container type id
        openScreen.write(Types.TAG, handleTag(connection, storage.dialog().title()));
        openScreen.send(Protocol1_21_6To1_21_5.class);
        updateDialog(connection, storage.dialog());
    }

    @Override
    public void closeDialog(final UserConnection connection) {
        final State state = connection.getProtocolInfo().getClientState();
        if (state == State.CONFIGURATION) {
            // TODO Implement by ending and re-starting the configuration phase
            return;
        }

        final ChestDialogStorage storage = connection.get(ChestDialogStorage.class);
        if (storage == null) {
            return;
        }

        final PacketWrapper containerClose = PacketWrapper.create(ClientboundPackets1_21_5.CONTAINER_CLOSE, connection);
        containerClose.write(Types.VAR_INT, storage.containerId());
        containerClose.send(Protocol1_21_6To1_21_5.class);
        if (storage.previousDialog() != null) {
            openDialog(connection, storage.previousDialog());
        } else {
            connection.remove(ChestDialogStorage.class);
        }
    }

    public boolean clickDialog(final UserConnection connection, final int container, int slot, final byte mouse, final int mode) {
        final ChestDialogStorage storage = connection.get(ChestDialogStorage.class);
        if (storage == null || storage.containerId() != container) {
            return false;
        }

        if (mode != 0 || slot < 0 || slot >= INVENTORY_SIZE) {
            updateDialog(connection, storage.dialog()); // Resync inventory view
            return true;
        }

        if (storage.phase() == ChestDialogStorage.Phase.ANVIL_VIEW) {
            openChestView(connection, storage, ChestDialogStorage.Phase.DIALOG_VIEW);
            return true;
        }

        if (storage.phase() == ChestDialogStorage.Phase.WAITING_FOR_RESPONSE) {
            if (slot == storage.actionIndex() && storage.closeButtonEnabled()) {
                closeDialog(connection);
            } else {
                updateDialog(connection, storage.dialog()); // Resync inventory view
            }
            return true;
        }

        // Page navigation
        if (slot == INVENTORY_SIZE - 1) {
            final int pages = MathUtil.ceil(storage.items().length / (float) INVENTORY_LAST_ROW);
            if (mouse == 0) {
                storage.page++;
            } else if (mouse == 1) {
                storage.page--;
            }
            storage.page = MathUtil.clamp(storage.page, 0, pages - 1);
        }
        slot += storage.page * INVENTORY_LAST_ROW;

        // Input widgets
        final List<Widget> widgets = storage.dialog().widgets();
        if (slot < widgets.size()) {
            final Widget widget = widgets.get(slot);
            if (widget instanceof final BooleanInput booleanInput) {
                clickBooleanInput(booleanInput);
            } else if (widget instanceof final NumberRangeInput numberRangeInput) {
                clickNumberRangeInput(numberRangeInput, mouse);
            } else if (widget instanceof final TextInput textInput) {
                clickTextInput(connection, textInput);
            } else if (widget instanceof final SingleOptionInput singleOptionInput) {
                clickSingleOptionInput(singleOptionInput);
            } else if (widget instanceof final Button button) {
                clickButton(connection, storage.dialog().afterAction(), button);
            } else if (widget instanceof final Dialog dialog) {
                clickDialogButton(connection, dialog);
            }
        }

        // And some special cases.
        if (slot == storage.confirmationYesIndex()) {
            final Button yesButton = storage.dialog().yesButton();
            if (yesButton != null) {
                clickButton(connection, storage.dialog().afterAction(), yesButton);
            }
        }
        if (slot == storage.confirmationNoIndex()) {
            final Button noButton = storage.dialog().noButton();
            if (noButton != null) {
                clickButton(connection, storage.dialog().afterAction(), noButton);
            }
        }
        if (slot == storage.actionIndex()) {
            final Button actionButton = storage.dialog().actionButton();
            if (actionButton != null) {
                clickButton(connection, storage.dialog().afterAction(), actionButton);
            }
        }

        // Resync inventory view if the actions above didn't close the dialog.
        if (connection.has(ChestDialogStorage.class) && storage.phase() == ChestDialogStorage.Phase.DIALOG_VIEW) {
            updateDialog(connection, storage.dialog());
        }
        return true;
    }

    public void updateDialog(final UserConnection connection, final Dialog dialog) {
        final ChestDialogStorage storage = connection.get(ChestDialogStorage.class);

        final PacketWrapper containerSetContent = PacketWrapper.create(ClientboundPackets1_21_5.CONTAINER_SET_CONTENT, connection);
        containerSetContent.write(Types.VAR_INT, storage.containerId());
        containerSetContent.write(Types.VAR_INT, 0); // Revision
        containerSetContent.write(VersionedTypes.V1_21_5.itemArray, getItems(connection, storage, dialog));
        containerSetContent.write(VersionedTypes.V1_21_5.item, StructuredItem.empty());
        containerSetContent.send(Protocol1_21_6To1_21_5.class);
    }

    protected Item createPageNavigationItem() {
        final DialogStyleConfig config = ViaBackwards.getConfig().dialogStyleConfig();

        return createItem(
            "minecraft:arrow",
            translate(config.pageNavigationTitle()),

            config.pageNavigationNext(),
            config.pageNavigationPrevious()
        );
    }

    protected Item createActionButtonItem(final UserConnection connection, final Button button) {
        final Tag label = handleTag(connection, button.label());
        if (button.tooltip() == null) {
            return createItem("minecraft:oak_button", label);
        } else {
            return createItem("minecraft:oak_button", label, handleTag(connection, button.tooltip()));
        }
    }

    protected Item createCloseButtonItem(final Tag label) {
        return createItem("minecraft:oak_button", label);
    }

    protected Item getItemWidget(final UserConnection connection, final ItemWidget itemWidget) {
        final String identifier = itemWidget.item().getString("id");
        final int count = itemWidget.item().getInt("count", 1);

        final Tag label = translate(Key.stripMinecraftNamespace(identifier));
        final Item item = createItem(identifier, label);
        item.setAmount(count);
        if (itemWidget.description() != null) {
            item.dataContainer().set(StructuredDataKey.LORE, new Tag[]{
                handleTag(connection, fixStyle(itemWidget.description().label()))
            });
        }
        if (!itemWidget.showTooltip()) {
            item.dataContainer().set(StructuredDataKey.TOOLTIP_DISPLAY, new TooltipDisplay(true, IntSortedSets.EMPTY_SET));
        }
        // If we were to parse item components from NBT for chat items they would be parsed here and stored into the data container.
        // In VV, chat items are rewritten manually at the time being and therefore no conversion code exists.
        return item;
    }

    protected Item getMultiTextWidget(final UserConnection connection, final MultiTextWidget multiTextWidget) {
        final Tag name = handleTag(connection, multiTextWidget.labels()[0]);
        final int length = multiTextWidget.labels().length;
        if (length == 1) {
            return createItem("minecraft:paper", name);
        }

        final Tag[] lore = new Tag[length - 1];
        for (int i = 1; i < length; i++) {
            lore[i - 1] = handleTag(connection, fixStyle(multiTextWidget.labels()[i]));
        }
        return createItem("minecraft:paper", name, lore);
    }

    protected Item getBooleanInput(final UserConnection connection, final BooleanInput booleanInput) {
        final DialogStyleConfig config = ViaBackwards.getConfig().dialogStyleConfig();

        final String item = booleanInput.value() ? "minecraft:lime_dye" : "minecraft:gray_dye";
        final Tag[] label = ChatUtil.split(booleanInput.label(), "\n");

        // The only one that supports newlines in the label
        if (label.length == 1) {
            return createItem(
                item,
                handleTag(connection, booleanInput.label()),
                translate(config.toggleValue())
            );
        } else {
            final Tag[] lore = new Tag[label.length];
            for (int i = 1; i < label.length; i++) {
                lore[i - 1] = handleTag(connection, fixStyle(label[i]));
            }
            lore[lore.length - 1] = translate(config.toggleValue());
            return createItem(
                item,
                handleTag(connection, label[0]),
                lore
            );
        }
    }

    protected void clickBooleanInput(final BooleanInput booleanInput) {
        booleanInput.setValue(!booleanInput.value());
    }

    protected Item getNumberRangeInput(final UserConnection connection, final NumberRangeInput numberRangeInput) {
        final DialogStyleConfig config = ViaBackwards.getConfig().dialogStyleConfig();

        final Tag label = handleTag(connection, numberRangeInput.displayName());
        return createItem(
            "minecraft:clock",
            label,

            String.format(config.increaseValue(), numberRangeInput.step()),
            String.format(config.decreaseValue(), numberRangeInput.step()),
            String.format(config.valueRange(), numberRangeInput.start(), numberRangeInput.end())
        );
    }

    protected void clickNumberRangeInput(final NumberRangeInput numberRangeInput, final int mouse) {
        float value = numberRangeInput.value();
        if (mouse == 0) { // Left click
            value += numberRangeInput.step();
        } else if (mouse == 1) { // Right click
            value -= numberRangeInput.step();
        }
        numberRangeInput.setClampedValue(value);
    }

    protected Item getTextInput(final UserConnection connection, final TextInput textInput) {
        final DialogStyleConfig config = ViaBackwards.getConfig().dialogStyleConfig();

        final Tag currentValue = translate(String.format(config.currentValue(), textInput.value()));
        if (textInput.label() == null) {
            return createItem("minecraft:writable_book", currentValue);
        } else {
            final Tag label = handleTag(connection, textInput.label());
            return createItem("minecraft:writable_book", label, currentValue, translate(config.editValue()));
        }
    }

    protected void clickTextInput(final UserConnection connection, final TextInput textInput) {
        final ChestDialogStorage storage = connection.get(ChestDialogStorage.class);
        openAnvilView(connection, storage, translate("§7Edit text"), textInput.value(), textInput);
    }

    protected Item getSingleOptionInput(final UserConnection connection, final SingleOptionInput singleOptionInput) {
        final DialogStyleConfig config = ViaBackwards.getConfig().dialogStyleConfig();

        final Tag displayName = singleOptionInput.options()[singleOptionInput.value()].computeDisplay();
        final Tag label;
        if (singleOptionInput.label() != null) {
            label = translate("options.generic_value", singleOptionInput.label(), displayName);
        } else {
            label = displayName;
        }
        return createItem(
            "minecraft:bookshelf",
            handleTag(connection, label),
            config.nextOption(),
            config.previousOption()
        );
    }

    protected void clickSingleOptionInput(final SingleOptionInput singleOptionInput) {
        singleOptionInput.setClampedValue(singleOptionInput.value() + 1);
    }

    protected Item getButton(final UserConnection connection, final Button button) {
        return createItem("minecraft:oak_button", handleTag(connection, button.label()));
    }

    public void clickButton(final UserConnection connection, final Dialog.AfterAction afterAction, @Nullable final Button button) {
        final ChestDialogStorage storage = connection.get(ChestDialogStorage.class);
        switch (afterAction) {
            case CLOSE -> closeDialog(connection);
            case WAIT_FOR_RESPONSE -> storage.setPhase(null, ChestDialogStorage.Phase.WAITING_FOR_RESPONSE);
        }

        if (button == null || button.clickEvent() == null) {
            return;
        }

        final CompoundTag clickEvent = button.clickEvent();
        final String action = Key.stripMinecraftNamespace(clickEvent.getString("action"));
        switch (action) {
            case "open_url" -> {
                // We can't open a URL for the client, so roughly emulate by opening an Anvil containing the URL.
                final String url = clickEvent.getString("url");
                openAnvilView(connection, storage, translate("Open URL"), url, null);
            }
            case "run_command" -> {
                // The vanilla client validates for signed argument types and has more requirements for this click event,
                // but we can't do this here and therefore just always send the packet assuming this is correct...
                final PacketWrapper chatCommand = PacketWrapper.create(ServerboundPackets1_21_6.CHAT_COMMAND, connection);
                String command = clickEvent.getString("command");
                if (command.startsWith("/")) {
                    command = command.substring(1);
                }
                chatCommand.write(Types.STRING, command);
                chatCommand.sendToServer(Protocol1_21_6To1_21_5.class);
            }
            case "copy_to_clipboard" -> {
                // Same as above, we can't access the clipboard
                final String value = clickEvent.getString("value");
                openAnvilView(connection, storage, translate("Copy to clipboard"), value, null);
            }
        }

        ClickEvents.handleClickEvent(connection, clickEvent); // Handle show_dialog and custom
    }

    protected Item createTextInputItem(final String value) {
        final DialogStyleConfig config = ViaBackwards.getConfig().dialogStyleConfig();
        return createItem("minecraft:paper", translate(value), config.setText());
    }

    protected Item createTextCopyItem(final String value) {
        final DialogStyleConfig config = ViaBackwards.getConfig().dialogStyleConfig();
        return createItem("minecraft:paper", translate(value), config.close());
    }

    protected void openAnvilView(
        final UserConnection connection,
        final ChestDialogStorage storage,
        final Tag title,
        final String value,
        final TextInput textInput
    ) {
        storage.setPhase(connection, ChestDialogStorage.Phase.ANVIL_VIEW);

        final PacketWrapper openScreen = PacketWrapper.create(ClientboundPackets1_21_5.OPEN_SCREEN, connection);
        openScreen.write(Types.VAR_INT, storage.containerId());
        openScreen.write(Types.VAR_INT, 8); // Container type id
        openScreen.write(Types.TAG, title);
        openScreen.send(Protocol1_21_6To1_21_5.class);

        final Item[] items = new Item[1];
        items[0] = textInput != null ? createTextInputItem(value) : createTextCopyItem(value);
        storage.setCurrentTextInput(textInput);

        final PacketWrapper containerSetContent = PacketWrapper.create(ClientboundPackets1_21_5.CONTAINER_SET_CONTENT, connection);
        containerSetContent.write(Types.VAR_INT, storage.containerId());
        containerSetContent.write(Types.VAR_INT, 0); // Revision
        containerSetContent.write(VersionedTypes.V1_21_5.itemArray, items);
        containerSetContent.write(VersionedTypes.V1_21_5.item, StructuredItem.empty());
        containerSetContent.send(Protocol1_21_6To1_21_5.class);
    }

    public void updateAnvilText(final UserConnection connection, final String value) {
        if (value.isEmpty()) {
            return;
        }

        final ChestDialogStorage storage = connection.get(ChestDialogStorage.class);
        if (storage.currentTextInput() != null) {
            storage.currentTextInput().setClampedValue(value);
        }
    }

    protected Item getDialog(final UserConnection connection, final Dialog dialog) {
        final Tag title = dialog.externalTitle() != null ? dialog.externalTitle() : dialog.title();
        return createItem("minecraft:command_block", handleTag(connection, title));
    }

    protected void clickDialogButton(final UserConnection connection, final Dialog dialog) {
        closeDialog(connection);
        openDialog(connection, dialog);
    }

    protected Item getItem(final UserConnection connection, final Widget widget) {
        if (widget instanceof final ItemWidget itemWidget) {
            return getItemWidget(connection, itemWidget);
        } else if (widget instanceof final MultiTextWidget multiTextWidget) {
            return getMultiTextWidget(connection, multiTextWidget);
        } else if (widget instanceof final BooleanInput booleanInput) {
            return getBooleanInput(connection, booleanInput);
        } else if (widget instanceof final NumberRangeInput numberRangeInput) {
            return getNumberRangeInput(connection, numberRangeInput);
        } else if (widget instanceof TextInput textInput) {
            return getTextInput(connection, textInput);
        } else if (widget instanceof SingleOptionInput singleOptionInput) {
            return getSingleOptionInput(connection, singleOptionInput);
        } else if (widget instanceof Button button) {
            return getButton(connection, button);
        } else if (widget instanceof Dialog dialog) {
            return getDialog(connection, dialog);
        }

        throw new IllegalArgumentException("Unknown widget type: " + widget.getClass().getName());
    }

    protected Item[] getItems(final UserConnection connection, final ChestDialogStorage storage, final Dialog dialog) {
        final Item[] items = StructuredItem.emptyArray(INVENTORY_SIZE);
        int confirmationYesIndex = -1;
        int confirmationNoIndex = -1;
        int actionIndex = -1;

        if (storage.phase() == ChestDialogStorage.Phase.WAITING_FOR_RESPONSE) {
            actionIndex = 13;

            items[actionIndex] = createCloseButtonItem(storage.closeButtonLabel());
            storage.setItems(items, confirmationYesIndex, confirmationNoIndex, actionIndex);
            return items;
        }

        final List<Widget> widgets = dialog.widgets();
        if (widgets.size() > INVENTORY_LAST_ROW) {
            final int begin = storage.page * INVENTORY_LAST_ROW;
            final int end = Math.min((storage.page + 1) * INVENTORY_LAST_ROW, widgets.size());
            for (int i = 0; i < end - begin; i++) {
                items[i] = getItem(connection, widgets.get(begin + i));
            }

            items[INVENTORY_SIZE - 1] = createPageNavigationItem();
        } else {
            for (int i = 0; i < widgets.size(); i++) {
                items[i] = getItem(connection, widgets.get(i));
            }
        }

        // And some special cases.
        if (dialog.yesButton() != null && dialog.noButton() != null) {
            confirmationYesIndex = widgets.isEmpty() ? 11 : INVENTORY_SIZE - 7;
            confirmationNoIndex = widgets.isEmpty() ? 15 : INVENTORY_SIZE - 3;

            items[confirmationYesIndex] = createActionButtonItem(connection, dialog.yesButton());
            items[confirmationNoIndex] = createActionButtonItem(connection, dialog.noButton());
        }
        if (dialog.actionButton() != null) {
            actionIndex = widgets.isEmpty() ? 13 : INVENTORY_SIZE - 9;

            items[actionIndex] = createActionButtonItem(connection, dialog.actionButton());
        }

        storage.setItems(items, confirmationYesIndex, confirmationNoIndex, actionIndex);
        return items;
    }

    /**
     * Handles the 1.21.6 tag inside the Dialog structure and rewrites it to the 1.21.5 format. This is necessary
     * for replacing translations used by Dialogs which only exist in 1.21.6+. Call this function for every text component
     * you display from the server.
     *
     * @param connection the user connection
     * @param tag        the text component as a tag, or null if no tag is present
     * @return the rewritten tag, or null if the input tag was null
     */
    protected @Nullable Tag handleTag(final UserConnection connection, final @Nullable Tag tag) {
        if (tag == null) {
            return null;
        }

        protocol.getComponentRewriter().processTag(connection, tag);
        return tag;
    }

    /**
     * This doesn't actually exist in Minecraft but is created if multiple {@link TextWidget} follow one another in order
     * to improve readability in chest inventories.
     */
    public record MultiTextWidget(Tag[] labels) implements Widget {

    }

    // -------------------------------------------------------------------------------------

    protected Item createItem(final String identifier, final Tag name) {
        return createItem(identifier, name, new String[0]);
    }

    protected Item createItem(final String identifier, final Tag name, final Tag... description) {
        final int id = protocol.getMappingData().getFullItemMappings().mappedId(identifier);

        final StructuredDataContainer data = new StructuredDataContainer();
        data.setIdLookup(protocol, true);
        data.set(StructuredDataKey.ITEM_NAME, name);
        if (description != null) {
            data.set(StructuredDataKey.LORE, description);
        }
        return new StructuredItem(id, 1, data);
    }

    protected Item createItem(final String identifier, final Tag name, final String... description) {
        final int id = protocol.getMappingData().getFullItemMappings().mappedId(identifier);

        final StructuredDataContainer data = new StructuredDataContainer();
        data.setIdLookup(protocol, true);
        data.set(StructuredDataKey.ITEM_NAME, name);
        if (description.length > 0) {
            final List<Tag> lore = new ArrayList<>();
            for (final String s : description) {
                lore.add(translate(s));
            }
            data.set(StructuredDataKey.LORE, lore.toArray(new Tag[0]));
        }
        return new StructuredItem(id, 1, data);
    }
}
