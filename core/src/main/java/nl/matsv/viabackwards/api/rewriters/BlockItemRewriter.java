/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.utils.Block;
import nl.matsv.viabackwards.utils.ItemUtil;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.viaversion.libs.opennbt.conversion.builtin.CompoundTagConverter;
import us.myles.viaversion.libs.opennbt.tag.builtin.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BlockItemRewriter<T extends BackwardsProtocol> extends Rewriter<T> {
    private static final CompoundTagConverter converter = new CompoundTagConverter();
    private final Map<Short, Item> itemRewriter = new ConcurrentHashMap<>();
    private final Map<Integer, Block> blockRewriter = new ConcurrentHashMap<>();

    protected void rewriteItem(int oldItem, Item newItem) {
        itemRewriter.put((short) oldItem, newItem);
    }

    protected void rewriteBlockItem(int oldId, Item newItem, Block newBlock) {
        itemRewriter.put((short) oldId, newItem);
        blockRewriter.put(oldId, newBlock);
    }

    protected Item handleItemToClient(Item item) {
        if (item == null)
            return null;
        if (!itemRewriter.containsKey(item.getId()))
            return item;
        Item i = ItemUtil.copyItem(itemRewriter.get(item.getId()));

        if (i.getTag() == null)
            i.setTag(new CompoundTag(""));
        i.getTag().put(createViaNBT(item));

        if (item.getTag() != null)
            for (Tag ai : item.getTag())
                i.getTag().put(ai);

        i.setAmount(item.getAmount());
        return i;
    }

    protected Item handleItemToServer(Item item) {
        if (item == null || item.getTag() == null)
            return null;
        CompoundTag tag = item.getTag();
        if (tag.contains("ViaBackwards|" + getProtocolName())) {
            CompoundTag via = tag.get("ViaBackwards|" + getProtocolName());

            short id = (short) via.get("id").getValue();
            short data = (short) via.get("data").getValue();
            byte amount = (byte) via.get("amount").getValue();
            CompoundTag extras = via.get("extras");

            item.setId(id);
            item.setData(data);
            item.setAmount(amount);
            item.setTag(converter.convert("", converter.convert(extras)));
            // Remove data tag
            tag.remove("ViaBackwards|" + getProtocolName());
        }
        return item;
    }

    protected int handleBlockID(int idx) {
        int type = idx >> 4;
        int meta = idx & 15;

        if (!containsBlock(type))
            return idx;

        Block b = handleBlock(type, meta);
        return (b.getId() << 4 | (b.getData() & 15));
    }

    protected Block handleBlock(int block, int data) {
        if (!containsBlock(block))
            return null;

        Block b = blockRewriter.get(block);
        // For some blocks, the data can still be useful (:
        if (b.getData() == -1)
            b.setData(data);
        return b;
    }

    protected void handleChunk(Chunk chunk) {
        for (int i = 0; i < chunk.getSections().length; i++) {
            ChunkSection section = chunk.getSections()[i];
            if (section == null)
                continue;

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        int block = section.getBlock(x, y, z);
                        int btype = block >> 4;
                        if (containsBlock(btype)) {
                            Block b = handleBlock(btype, block & 15); // Type / data
                            section.setBlock(x, y, z, b.getId(), b.getData());
                        }
                    }
                }
            }
        }
    }

    protected boolean containsBlock(int block) {
        return blockRewriter.containsKey(block);
    }

    private CompoundTag createViaNBT(Item i) {
        CompoundTag tag = new CompoundTag("ViaBackwards|" + getProtocolName());
        tag.put(new ShortTag("id", i.getId()));
        tag.put(new ShortTag("data", i.getData()));
        tag.put(new ByteTag("amount", i.getAmount()));
        if (i.getTag() != null) {
            tag.put(converter.convert("extras", converter.convert(i.getTag())));
        } else
            tag.put(new CompoundTag("extras"));
        return tag;
    }

    protected CompoundTag getNamedTag(String text) {
        CompoundTag tag = new CompoundTag("");
        tag.put(new CompoundTag("display"));
        ((CompoundTag) tag.get("display")).put(new StringTag("Name", text));
        return tag;
    }

    private String getProtocolName() {
        return getProtocol().getClass().getSimpleName();
    }

}
