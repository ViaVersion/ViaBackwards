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
import nl.matsv.viabackwards.api.data.MappedLegacyBlockItem;
import nl.matsv.viabackwards.api.data.VBMappingDataLoader;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data.BlockColors;
import nl.matsv.viabackwards.utils.Block;
import org.jetbrains.annotations.Nullable;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.rewriters.IdRewriteFunction;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;
import us.myles.viaversion.libs.gson.JsonPrimitive;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.IntTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

import java.util.HashMap;
import java.util.Map;

public abstract class LegacyBlockItemRewriter<T extends BackwardsProtocol> extends ItemRewriterBase<T> {

    private static final Map<String, Map<Integer, MappedLegacyBlockItem>> LEGACY_MAPPINGS = new HashMap<>();
    protected final Map<Integer, MappedLegacyBlockItem> replacementData;

    static {
        JsonObject jsonObject = VBMappingDataLoader.loadFromDataDir("legacy-mappings.json");
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            Map<Integer, MappedLegacyBlockItem> mappings = new HashMap<>();
            LEGACY_MAPPINGS.put(entry.getKey(), mappings);
            for (Map.Entry<String, JsonElement> dataEntry : entry.getValue().getAsJsonObject().entrySet()) {
                JsonObject object = dataEntry.getValue().getAsJsonObject();
                int id = object.getAsJsonPrimitive("id").getAsInt();
                JsonPrimitive jsonData = object.getAsJsonPrimitive("data");
                short data = jsonData != null ? jsonData.getAsShort() : 0;
                String name = object.getAsJsonPrimitive("name").getAsString();
                JsonPrimitive blockField = object.getAsJsonPrimitive("block");
                boolean block = blockField != null && blockField.getAsBoolean();

                if (dataEntry.getKey().indexOf('-') != -1) {
                    // Range of ids
                    String[] split = dataEntry.getKey().split("-", 2);
                    int from = Integer.parseInt(split[0]);
                    int to = Integer.parseInt(split[1]);

                    // Special block color handling
                    if (name.contains("%color%")) {
                        for (int i = from; i <= to; i++) {
                            mappings.put(i, new MappedLegacyBlockItem(id, data, name.replace("%color%", BlockColors.get(i - from)), block));
                        }
                    } else {
                        MappedLegacyBlockItem mappedBlockItem = new MappedLegacyBlockItem(id, data, name, block);
                        for (int i = from; i <= to; i++) {
                            mappings.put(i, mappedBlockItem);
                        }
                    }
                } else {
                    mappings.put(Integer.parseInt(dataEntry.getKey()), new MappedLegacyBlockItem(id, data, name, block));
                }
            }
        }
    }

    protected LegacyBlockItemRewriter(T protocol, @Nullable IdRewriteFunction oldRewriter, @Nullable IdRewriteFunction newRewriter) {
        super(protocol, oldRewriter, newRewriter, false);
        replacementData = LEGACY_MAPPINGS.get(protocol.getClass().getSimpleName().split("To")[1].replace("_", "."));
    }

    protected LegacyBlockItemRewriter(T protocol) {
        this(protocol, null, null);
    }

    @Override
    @Nullable
    public Item handleItemToClient(Item item) {
        if (item == null) return null;

        MappedLegacyBlockItem data = replacementData.get(item.getIdentifier());
        if (data == null) {
            // Just rewrite the id
            return super.handleItemToClient(item);
        }

        if (item.getTag() == null) {
            item.setTag(new CompoundTag(""));
        }

        // Backup data for toServer
        short originalData = item.getData();
        item.getTag().put(createViaNBT(item));

        item.setIdentifier(data.getId());
        // Keep original data if mapped data is set to -1
        if (data.getData() != -1) {
            item.setData(data.getData());
        }

        // Set display name
        if (data.getName() != null) {
            CompoundTag tag = item.getTag().get("display");
            if (tag == null) {
                item.getTag().put(tag = new CompoundTag("display"));
            }
            StringTag nameTag = tag.get("Name");
            if (nameTag == null) {
                tag.put(nameTag = new StringTag("Name", data.getName()));
            }

            // Handle colors
            String value = nameTag.getValue();
            if (value.contains("%vb_color%")) {
                tag.put(new StringTag("Name", value.replace("%vb_color%", BlockColors.get(originalData))));
            }
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

    @Nullable
    public Block handleBlock(int blockId, int data) {
        MappedLegacyBlockItem settings = replacementData.get(blockId);
        if (settings == null || !settings.isBlock()) return null;

        Block block = settings.getBlock();
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
            Tag xTag;
            Tag yTag;
            Tag zTag;
            if ((xTag = tag.get("x")) == null || (yTag = tag.get("y")) == null || (zTag = tag.get("z")) == null) {
                continue;
            }

            Pos pos = new Pos(
                    (int) xTag.getValue() & 0xF,
                    (int) yTag.getValue(),
                    (int) zTag.getValue() & 0xF);
            tags.put(pos, tag);

            // Handle given Block Entities
            ChunkSection section = chunk.getSections()[pos.getY() >> 4];
            if (section == null) continue;

            int block = section.getFlatBlock(pos.getX(), pos.getY() & 0xF, pos.getZ());
            int btype = block >> 4;

            MappedLegacyBlockItem settings = replacementData.get(btype);
            if (settings != null && settings.hasBlockEntityHandler()) {
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

                MappedLegacyBlockItem settings = replacementData.get(btype);
                if (settings != null && settings.hasBlockEntityHandler()) {
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

                        MappedLegacyBlockItem settings = replacementData.get(btype);
                        if (settings == null || !settings.hasBlockEntityHandler()) continue;

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

        private final int x;
        private final short y;
        private final int z;

        private Pos(int x, int y, int z) {
            this.x = x;
            this.y = (short) y;
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
