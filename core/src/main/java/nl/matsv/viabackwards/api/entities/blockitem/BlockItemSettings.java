/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api.entities.blockitem;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import nl.matsv.viabackwards.utils.Block;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;

@RequiredArgsConstructor
@Getter
public class BlockItemSettings {
    private final int id;
    private Item repItem;
    private Block repBlock;
    private BlockEntityHandler blockEntityHandler;
    private ItemHandler itemHandler;

    public BlockItemSettings repItem(Item item) {
        this.repItem = item;
        return this;
    }

    public BlockItemSettings repBlock(Block block) {
        this.repBlock = block;
        return this;
    }

    public BlockItemSettings blockEntityHandler(BlockEntityHandler handler) {
        this.blockEntityHandler = handler;
        return this;
    }

    public BlockItemSettings itemHandler(ItemHandler handler) {
        this.itemHandler = handler;
        return this;
    }

    public boolean hasRepItem() {
        return repItem != null;
    }

    public boolean hasRepBlock() {
        return repBlock != null;
    }

    public boolean hasEntityHandler() {
        return blockEntityHandler != null;
    }

    public boolean hasItemTagHandler() {
        return itemHandler != null;
    }

    public interface BlockEntityHandler {
        CompoundTag handleOrNewCompoundTag(int block, CompoundTag tag);
    }

    public interface ItemHandler {
        Item handle(Item i);
    }

}
