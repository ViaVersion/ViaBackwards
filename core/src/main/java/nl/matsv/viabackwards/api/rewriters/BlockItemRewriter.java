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

import lombok.*;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.utils.Block;
import nl.matsv.viabackwards.utils.ItemUtil;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.viaversion.libs.opennbt.conversion.builtin.CompoundTagConverter;
import us.myles.viaversion.libs.opennbt.tag.builtin.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BlockItemRewriter<T extends BackwardsProtocol> extends Rewriter<T> {
    private static final CompoundTagConverter converter = new CompoundTagConverter();
    private final Map<Integer, BlockItemData> replacementData = new ConcurrentHashMap<>();

    protected void rewriteItem(int oldItem, Item newItem) {
        replacementData.put(oldItem, new BlockItemData(oldItem, newItem, null, null));
    }

    protected void rewriteBlockItem(int oldId, Item newItem, Block newBlock) {
        rewriteBlockItem(oldId, newItem, newBlock, null);
    }

    protected void rewriteBlockItem(int oldId, Item newItem, Block newBlock, BlockEntityHandler handler) {
        replacementData.put(oldId, new BlockItemData(oldId, newItem, newBlock, handler));
    }

    protected Item handleItemToClient(Item item) {
        if (item == null)
            return null;
        if (!replacementData.containsKey((int) item.getId()))
            return item;
        BlockItemData data = replacementData.get((int) item.getId());
        if (!data.hasRepItem())
            return item;

        Item i = ItemUtil.copyItem(data.getRepItem());

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
            // Remove data blockEntityHandler
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

        Block b = replacementData.get(block).getRepBlock();
        // For some blocks, the data can still be useful (:
        if (b.getData() != -1)
            b.setData(data);
        return b;
    }

    protected void handleChunk(Chunk chunk) {
        // Map Block Entities
        Map<Pos, CompoundTag> tags = new HashMap<>();
        for (CompoundTag tag : chunk.getBlockEntities()) {
            if (!(tag.contains("x") && tag.contains("y") && tag.contains("z")))
                continue;
            Pos pos = new Pos(
                    (int) tag.get("x").getValue() % 16,
                    (int) tag.get("y").getValue(),
                    (int) tag.get("z").getValue() % 16);
            tags.put(pos, tag);
        }

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
                        // Entity Tags
                        if (hasBlockEntityHandler(btype)) {
                            Pos pos = new Pos(x, (y + (i << 4)), z);
                            CompoundTag tag = null;
                            if (tags.containsKey(pos)) {
                                tag = tags.get(pos);
                            } else {
                                tag = new CompoundTag("");
                                tag.put(new IntTag("x", x + (chunk.getX() << 4)));
                                tag.put(new IntTag("y", y + (i << 4)));
                                tag.put(new IntTag("z", z + (chunk.getZ() << 4)));
                                chunk.getBlockEntities().add(tag);
                            }
                            replacementData.get(btype).getBlockEntityHandler().handleOrNewCompoundTag(block, tag);
                        }
                    }
                }
            }
        }
    }

    protected boolean containsBlock(int block) {
        return replacementData.containsKey(block) && replacementData.get(block).hasRepBlock();
    }

    protected boolean hasBlockEntityHandler(int block) {
        return replacementData.containsKey(block) && replacementData.get(block).hasHandler();
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

    public interface BlockEntityHandler {
        CompoundTag handleOrNewCompoundTag(int block, CompoundTag tag);
    }

    @Data
    @RequiredArgsConstructor
    public class BlockItemData {
        private final int id;
        private final Item repItem;
        private final Block repBlock;
        private final BlockEntityHandler blockEntityHandler;

        public boolean hasRepItem() {
            return repItem != null;
        }

        public boolean hasRepBlock() {
            return repBlock != null;
        }

        public boolean hasHandler() {
            return blockEntityHandler != null;
        }

    }

    @Data
    @AllArgsConstructor
    @ToString
    @EqualsAndHashCode
    private class Pos {
        private int x, y, z;
    }
}
