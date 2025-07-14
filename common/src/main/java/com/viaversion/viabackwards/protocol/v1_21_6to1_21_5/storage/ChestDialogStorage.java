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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage;

import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.Protocol1_21_6To1_21_5;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.Dialog;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.input.TextInput;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.provider.ChestDialogViewProvider;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPackets1_21_5;
import java.util.concurrent.atomic.AtomicInteger;
import org.checkerframework.checker.nullness.qual.Nullable;

import static com.viaversion.viabackwards.utils.ChatUtil.text;
import static com.viaversion.viabackwards.utils.ChatUtil.translate;

/**
 * Per-user storage for {@link ChestDialogViewProvider}
 */
public final class ChestDialogStorage implements StorableObject {

    private static final byte MIN_FAKE_ID = Byte.MAX_VALUE / 2 + 1;
    private static final byte MAX_FAKE_ID = Byte.MAX_VALUE - 1;
    private static final AtomicInteger FAKE_ID_COUNTER = new AtomicInteger(MIN_FAKE_ID);

    private static final Tag[] RESPONSE_BUTTON_LABELS = new Tag[]{
        text(""),
        translate("gui.waitingForResponse.button.inactive", 4),
        translate("gui.waitingForResponse.button.inactive", 3),
        translate("gui.waitingForResponse.button.inactive", 2),
        translate("gui.waitingForResponse.button.inactive", 1),
        translate("gui.back")
    };

    private final ChestDialogViewProvider provider;
    private final Dialog dialog;
    private int containerId;
    private Dialog previousDialog;

    public int page;

    private Item[] items;
    private int confirmationYesIndex = -1;
    private int confirmationNoIndex = -1;

    private int actionIndex = -1;

    private @Nullable Phase phase;

    private int ticksWaitingForResponse = 0;

    private boolean closeButtonEnabled;
    private Tag closeButtonLabel;

    private TextInput currentTextInput;

    private boolean allowClosing;

    public ChestDialogStorage(final ChestDialogViewProvider provider, final Dialog dialog) {
        this.provider = provider;
        this.dialog = dialog;
    }

    public void tick(final UserConnection connection) {
        if (phase != Phase.WAITING_FOR_RESPONSE) {
            return;
        }

        final int index = ticksWaitingForResponse++ / 20;
        if (index > RESPONSE_BUTTON_LABELS.length - 1) {
            return;
        }

        closeButtonEnabled = index >= 1;
        closeButtonLabel = RESPONSE_BUTTON_LABELS[index];
        provider.updateDialog(connection, dialog);
    }

    public Dialog dialog() {
        return dialog;
    }

    public @Nullable Dialog previousDialog() {
        return previousDialog;
    }

    public void setPreviousDialog(final Dialog previousDialog) {
        this.previousDialog = previousDialog;
    }

    public int containerId() {
        return containerId;
    }

    public Item[] items() {
        return items;
    }

    public void setItems(final Item[] items, final int confirmationYesIndex, final int confirmationNoIndex, final int actionIndex) {
        this.items = items;
        this.confirmationYesIndex = confirmationYesIndex;
        this.confirmationNoIndex = confirmationNoIndex;
        this.actionIndex = actionIndex;
    }

    public int confirmationYesIndex() {
        return confirmationYesIndex;
    }

    public int confirmationNoIndex() {
        return confirmationNoIndex;
    }

    public int actionIndex() {
        return actionIndex;
    }

    public @Nullable Phase phase() {
        return phase;
    }

    public boolean closeButtonEnabled() {
        return closeButtonEnabled;
    }

    public Tag closeButtonLabel() {
        return closeButtonLabel;
    }

    public TextInput currentTextInput() {
        return currentTextInput;
    }

    public boolean allowClosing() {
        return allowClosing;
    }

    public void setPhase(final UserConnection connection, final @Nullable Phase phase) {
        if (phase == Phase.DIALOG_VIEW || phase == Phase.ANVIL_VIEW) {
            if (this.phase != null) {
                final PacketWrapper containerClose = PacketWrapper.create(ClientboundPackets1_21_5.CONTAINER_CLOSE, connection);
                containerClose.write(Types.VAR_INT, containerId);
                containerClose.send(Protocol1_21_6To1_21_5.class);
            }
            currentTextInput = null;

            final int id = FAKE_ID_COUNTER.getAndIncrement();
            if (id > MAX_FAKE_ID) {
                FAKE_ID_COUNTER.set(MIN_FAKE_ID);
                containerId = MIN_FAKE_ID;
            } else {
                containerId = (byte) id;
            }
        }

        this.phase = phase;
    }

    public void setCurrentTextInput(final TextInput currentTextInput) {
        this.currentTextInput = currentTextInput;
    }

    public void setAllowClosing(final boolean allowClosing) {
        this.allowClosing = allowClosing;
    }

    public enum Phase {

        DIALOG_VIEW,
        ANVIL_VIEW,
        WAITING_FOR_RESPONSE
    }
}
