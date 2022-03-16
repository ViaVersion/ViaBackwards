/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_18_2to1_19.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.minecraft.Position;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.LinkedList;
import java.util.Queue;

public final class BlockAckStorage implements StorableObject {

    private final Queue<BlockAction> actions = new LinkedList<>();

    public void add(final Position position, final int action, final short direction) {
        actions.add(new BlockAction(position, (byte) action, direction));

        // Some actions may be left unacknowledged by modded servers
        if (actions.size() > 100) {
            actions.poll();
        }
    }

    public @Nullable BlockAction poll() {
        return actions.poll();
    }

    public static final class BlockAction {
        private final Position position;
        private final byte action;
        private final short direction;

        public BlockAction(final Position position, final byte action, final short direction) {
            this.position = position;
            this.action = action;
            this.direction = direction;
        }

        public Position position() {
            return position;
        }

        public byte action() {
            return action;
        }

        public short direction() {
            return direction;
        }
    }
}
