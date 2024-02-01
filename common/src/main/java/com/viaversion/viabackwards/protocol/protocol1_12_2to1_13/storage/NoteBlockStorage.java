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
package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.util.Pair;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NoteBlockStorage implements StorableObject {

    private static final int MAX_NOTE_ID = 24;

    private final Map<Position, Integer> noteBlockUpdates = new ConcurrentHashMap<>();

    public void storeNoteBlockUpdate(final Position position, final int blockStateId) {
        noteBlockUpdates.put(position, blockStateId);
    }

    public Pair<Integer, Integer> getNoteBlockUpdate(final Position position) {
        if (!noteBlockUpdates.containsKey(position)) {
            return null;
        }
        int relativeBlockState = noteBlockUpdates.get(position) - 249;
        relativeBlockState = relativeBlockState / 2; // Get rid of powered state

        return new Pair<>(relativeBlockState / MAX_NOTE_ID + 1, relativeBlockState % MAX_NOTE_ID + 1);
    }

}
