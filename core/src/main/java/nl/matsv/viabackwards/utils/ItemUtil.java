/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.utils;

import us.myles.ViaVersion.api.minecraft.item.Item;

public class ItemUtil {

    /**
     * Sets all the data of the item into the original item.
     *
     * @param original item to be modified
     * @param item     item to replicate the data from
     * @return modified original item
     */
    public static void copyItem(Item original, Item item) {
        original.setIdentifier(item.getIdentifier());
        original.setAmount(item.getAmount());
        original.setData(item.getData());
        original.setTag(item.getTag() != null ? item.getTag().clone() : null);
    }

    /**
     * Creates a new item object with the same data as the given one.
     *
     * @param item item to be copied
     * @return a new copy of the item
     */
    public static Item copyItem(Item item) {
        if (item == null) return null;
        return new Item(item.getIdentifier(), item.getAmount(), item.getData(), item.getTag() != null ? item.getTag().clone() : null);
    }
}
