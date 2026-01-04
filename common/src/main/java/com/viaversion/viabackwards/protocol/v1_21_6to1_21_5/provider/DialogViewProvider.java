/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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

import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data.Dialog;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.platform.providers.Provider;

/**
 * Interface for providing dialog view functionality to this protocol. Requires a storage
 * to save per-user data while being able to have open and close logic static.
 * <p>
 * See {@link Dialog} for the structure of a dialog.
 * <p>
 * See {@link ChestDialogViewProvider} for the protocol level emulation using a chest.
 */
public interface DialogViewProvider extends Provider {

    void openDialog(final UserConnection connection, final Dialog dialog);

    void closeDialog(final UserConnection connection);

}
