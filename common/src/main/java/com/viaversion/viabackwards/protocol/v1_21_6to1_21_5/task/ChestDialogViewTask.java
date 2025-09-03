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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.task;

import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.storage.ChestDialogStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.connection.StorableObjectTask;

public final class ChestDialogViewTask extends StorableObjectTask<ChestDialogStorage> {

    public ChestDialogViewTask() {
        super(ChestDialogStorage.class);
    }

    @Override
    public void run(final UserConnection userConnection, final ChestDialogStorage storableObject) {
        storableObject.tick(userConnection);
    }
}
