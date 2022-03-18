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
package com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;

public class PlayerLastCursorItem implements StorableObject {
    private Item lastCursorItem;

    public Item getLastCursorItem() {
        return copyItem(lastCursorItem);
    }

    public void setLastCursorItem(Item item) {
        this.lastCursorItem = copyItem(item);
    }

    public void setLastCursorItem(Item item, int amount) {
        this.lastCursorItem = copyItem(item);
        this.lastCursorItem.setAmount(amount);
    }

    public boolean isSet() {
        return lastCursorItem != null;
    }

    private static Item copyItem(Item item) {
        if (item == null) {
            return null;
        }
        Item copy = new DataItem(item);
        copy.setTag(copy.tag() == null ? null : copy.tag().clone());
        return copy;
    }
}
