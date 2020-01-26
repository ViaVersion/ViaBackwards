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

import net.md_5.bungee.api.ChatColor;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.blockitem.BlockItemSettings;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data.BlockColors;
import nl.matsv.viabackwards.utils.Block;
import nl.matsv.viabackwards.utils.ItemUtil;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.rewriters.IdRewriteFunction;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.IntTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

import java.util.HashMap;
import java.util.Map;

public abstract class LegacyBlockItemRewriter<T extends BackwardsProtocol> extends ItemRewriterBase<T> {

    private final Map<Integer, BlockItemSettings> replacementData = new HashMap<>();

    protected LegacyBlockItemRewriter(T protocol, IdRewriteFunction oldRewriter, IdRewriteFunction newRewriter) {
        super(protocol, oldRewriter, newRewriter, false);
    }

    protected LegacyBlockItemRewriter(T protocol) {
        super(protocol, false);
    }

    protected BlockItemSettings rewrite(int itemId) {
        BlockItemSettings settings = new BlockItemSettings(itemId);
        replacementData.put(itemId, settings);
        return settings;
    }

    @Override
    public Item handleItemToClient(Item item) {
        if (item == null) return null;

        BlockItemSettings data = replacementData.get(item.getIdentifier());
        if (data == null) {
            // Just rewrite the id
            return super.handleItemToClient(item);
        }

        Item original = ItemUtil.copyItem(item);
        if (data.hasRepItem()) {
            // Also includes the already mapped id
            ItemUtil.copyItem(item, data.getRepItem());

            if (item.getTag() == null) {
                item.setTag(new CompoundTag(""));
            }

            // Backup data for toServer
            item.getTag().put(createViaNBT(original));

            // Keep original data (aside from the name)
            if (original.getTag() != null) {
                for (Tag ai : original.getTag()) {
                    item.getTag().put(ai);
                }
            }

            // Handle colors
            CompoundTag tag = item.getTag().get("display");
            if (tag != null) {
                StringTag nameTag = tag.get("Name");
                if (nameTag != null) {
                    String value = nameTag.getValue();
                    if (value.contains("%vb_color%")) {
                        tag.put(new StringTag("Name", value.replace("%vb_color%", BlockColors.get(original.getData()))));
                    }
                }
            }

            item.setAmount(original.getAmount());
            // Keep original data when -1
            if (item.getData() == -1) {
                item.setData(original.getData());
            }
        } else {
            // Set the mapped id if no custom item is defined
            super.handleItemToClient(item);
        }

        if (data.hasItemTagHandler()) {
            if (!item.getTag().contains(nbtTagName)) {
                item.getTag().put(createViaNBT(original));
            }
            data.getItemHandler().handle(item);
        }

        return item;
    }

    public int handleBlockID(int idx) {
        int type = idx >> 4;
        int meta = idx & 15;

        Block b = handleBlock(type, meta);
        if (b == null) return idx;

        return (b.getId() << 4 | (b.getData() & 15));
    }

    public Block handleBlock(int blockId, int data) {
        BlockItemSettings settings = replacementData.get(blockId);
        if (settings == null || !settings.hasRepBlock()) return null;

        Block block = settings.getRepBlock();
        // For some blocks, the data can still be useful (:
        if (block.getData() == -1) {
            return block.withData(data);
        }
        return block;
    }

    protected void handleChunk(Chunk chunk) {
        // Map Block Entities
        Map<Pos, CompoundTag> tags = new HashMap<>();
        for (CompoundTag tag : chunk.getBlockEntities()) {
            if (!(tag.contains("x") && tag.contains("y") && tag.contains("z")))
                continue;
            Pos pos = new Pos(
                    (int) tag.get("x").getValue() & 0xF,
                    (int) tag.get("y").getValue(),
                    (int) tag.get("z").getValue() & 0xF);
            tags.put(pos, tag);

            // Handle given Block Entities
            ChunkSection section = chunk.getSections()[pos.getY() >> 4];
            if (section == null) continue;
            int block = section.getFlatBlock(pos.getX(), pos.getY() & 0xF, pos.getZ());
            int btype = block >> 4;

            BlockItemSettings settings = replacementData.get(btype);
            if (settings != null && settings.hasEntityHandler()) {
                settings.getBlockEntityHandler().handleOrNewCompoundTag(block, tag);
            }
        }

        for (int i = 0; i < chunk.getSections().length; i++) {
            ChunkSection section = chunk.getSections()[i];
            if (section == null) continue;

            boolean hasBlockEntityHandler = false;

            // Map blocks
            for (int j = 0; j < section.getPaletteSize(); j++) {
                int block = section.getPaletteEntry(j);
                int btype = block >> 4;
                int meta = block & 0xF;

                Block b = handleBlock(btype, meta);
                if (b != null) {
                    section.setPaletteEntry(j, (b.getId() << 4) | (b.getData() & 0xF));
                }

                // We already know that is has a handler
                if (hasBlockEntityHandler) continue;

                BlockItemSettings settings = replacementData.get(btype);
                if (settings != null && settings.hasEntityHandler()) {
                    hasBlockEntityHandler = true;
                }
            }

            if (!hasBlockEntityHandler) continue;

            // We need to handle a Block Entity :(
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        int block = section.getFlatBlock(x, y, z);
                        int btype = block >> 4;
                        int meta = block & 15;

                        BlockItemSettings settings = replacementData.get(btype);
                        if (settings == null || !settings.hasEntityHandler()) continue;

                        Pos pos = new Pos(x, (y + (i << 4)), z);

                        // Already handled above
                        if (tags.containsKey(pos)) continue;

                        CompoundTag tag = new CompoundTag("");
                        tag.put(new IntTag("x", x + (chunk.getX() << 4)));
                        tag.put(new IntTag("y", y + (i << 4)));
                        tag.put(new IntTag("z", z + (chunk.getZ() << 4)));

                        settings.getBlockEntityHandler().handleOrNewCompoundTag(block, tag);
                        chunk.getBlockEntities().add(tag);
                    }
                }
            }
        }
    }

    protected CompoundTag getNamedTag(String text) {
        CompoundTag tag = new CompoundTag("");
        tag.put(new CompoundTag("display"));
        text = ChatColor.RESET + text;
        ((CompoundTag) tag.get("display")).put(new StringTag("Name", jsonNameFormat ? ChatRewriter.legacyTextToJson(text) : text));
        return tag;
    }

    private static final class Pos {

        private final int x, y, z;

        private Pos(final int x, final int y, final int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pos pos = (Pos) o;
            if (x != pos.x) return false;
            if (y != pos.y) return false;
            return z == pos.z;
        }

        @Override
        public int hashCode() {
            int result = x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }

        @Override
        public String toString() {
            return "Pos{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
        }
    }
}
