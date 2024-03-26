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

package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.MappedLegacyBlockItem;
import com.viaversion.viabackwards.api.data.BackwardsMappingDataLoader;
import com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.data.BlockColors;
import com.viaversion.viabackwards.utils.Block;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.util.ComponentUtil;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class LegacyBlockItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends ItemRewriterBase<C, S, T> {

    protected Int2ObjectMap<MappedLegacyBlockItem> replacementData = new Int2ObjectOpenHashMap<>(8); // Raw id -> mapped data

    private static void addMapping(String key, JsonObject object, Int2ObjectMap<MappedLegacyBlockItem> mappings) {
        int id = object.getAsJsonPrimitive("id").getAsInt();
        JsonPrimitive jsonData = object.getAsJsonPrimitive("data");
        short data = jsonData != null ? jsonData.getAsShort() : 0;
        String name = object.getAsJsonPrimitive("name").getAsString();
        JsonPrimitive blockField = object.getAsJsonPrimitive("block");
        boolean block = blockField != null && blockField.getAsBoolean();

        if (key.indexOf('-') == -1) {
            int unmappedId;
            int dataSeparatorIndex = key.indexOf(':');
            if (dataSeparatorIndex != -1) {
                // Include data
                short unmappedData = Short.parseShort(key.substring(dataSeparatorIndex + 1));
                unmappedId = Integer.parseInt(key.substring(0, dataSeparatorIndex));
                unmappedId = (unmappedId << 4) | (unmappedData & 15);
            } else {
                unmappedId = Integer.parseInt(key) << 4;
            }

            mappings.put(unmappedId, new MappedLegacyBlockItem(id, data, name, block));
            return;
        }

        // Range of ids
        String[] split = key.split("-", 2);
        int from = Integer.parseInt(split[0]);
        int to = Integer.parseInt(split[1]);

        // Special block color handling
        if (name.contains("%color%")) {
            for (int i = from; i <= to; i++) {
                mappings.put(i << 4, new MappedLegacyBlockItem(id, data, name.replace("%color%", BlockColors.get(i - from)), block));
            }
        } else {
            MappedLegacyBlockItem mappedBlockItem = new MappedLegacyBlockItem(id, data, name, block);
            for (int i = from; i <= to; i++) {
                mappings.put(i << 4, mappedBlockItem);
            }
        }
    }

    protected LegacyBlockItemRewriter(T protocol, String name) {
        super(protocol, Type.ITEM1_8, Type.ITEM1_8_SHORT_ARRAY, false);
        final JsonObject jsonObject = BackwardsMappingDataLoader.INSTANCE.loadFromDataDir("item-mappings-" + name + ".json");
        for (Map.Entry<String, JsonElement> dataEntry : jsonObject.entrySet()) {
            addMapping(dataEntry.getKey(), dataEntry.getValue().getAsJsonObject(), replacementData);
        }
    }

    public void registerBlockChange(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_8); // 0 - Block Position
                map(Type.VAR_INT); // 1 - Block

                handler(wrapper -> {
                    int idx = wrapper.get(Type.VAR_INT, 0);
                    wrapper.set(Type.VAR_INT, 0, handleBlockID(idx));
                });
            }
        });
    }

    public void registerMultiBlockChange(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // 0 - Chunk X
                map(Type.INT); // 1 - Chunk Z
                map(Type.BLOCK_CHANGE_RECORD_ARRAY);

                handler(wrapper -> {
                    for (BlockChangeRecord record : wrapper.get(Type.BLOCK_CHANGE_RECORD_ARRAY, 0)) {
                        record.setBlockId(handleBlockID(record.getBlockId()));
                    }
                });
            }
        });
    }

    @Override
    public @Nullable Item handleItemToClient(@Nullable Item item) {
        if (item == null) return null;

        MappedLegacyBlockItem data = getMappedBlockItem(item.identifier(), item.data());
        if (data == null) {
            // Just rewrite the id
            return super.handleItemToClient(item);
        }

        short originalData = item.data();
        item.setIdentifier(data.getId());
        // Keep original data if mapped data is set to -1
        if (data.getData() != -1) {
            item.setData(data.getData());
        }

        // Set display name
        if (data.getName() != null) {
            if (item.tag() == null) {
                item.setTag(new CompoundTag());
            }

            CompoundTag display = item.tag().getCompoundTag("display");
            if (display == null) {
                item.tag().put("display", display = new CompoundTag());
            }

            StringTag nameTag = display.getStringTag("Name");
            if (nameTag == null) {
                nameTag = new StringTag(data.getName());
                display.put("Name", nameTag);
                display.put(getNbtTagName() + "|customName", new ByteTag());
            }

            // Handle colors
            String value = nameTag.getValue();
            if (value.contains("%vb_color%")) {
                display.putString("Name", value.replace("%vb_color%", BlockColors.get(originalData)));
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

    public @Nullable Block handleBlock(int blockId, int data) {
        MappedLegacyBlockItem settings = getMappedBlockItem(blockId, data);
        if (settings == null || !settings.isBlock()) return null;

        Block block = settings.getBlock();
        // For some blocks, the data can still be useful (:
        if (block.getData() == -1) {
            return block.withData(data);
        }
        return block;
    }

    private @Nullable MappedLegacyBlockItem getMappedBlockItem(int id, int data) {
        MappedLegacyBlockItem mapping = replacementData.get((id << 4) | (data & 15));
        return mapping != null || data == 0 ? mapping : replacementData.get(id << 4);
    }

    private @Nullable MappedLegacyBlockItem getMappedBlockItem(int rawId) {
        MappedLegacyBlockItem mapping = replacementData.get(rawId);
        return mapping != null ? mapping : replacementData.get(rawId & ~15);
    }

    protected void handleChunk(Chunk chunk) {
        // Map Block Entities
        Map<Pos, CompoundTag> tags = new HashMap<>();
        for (CompoundTag tag : chunk.getBlockEntities()) {
            NumberTag xTag;
            NumberTag yTag;
            NumberTag zTag;
            if ((xTag = tag.getNumberTag("x")) == null
                || (yTag = tag.getNumberTag("y")) == null
                || (zTag = tag.getNumberTag("z")) == null) {
                continue;
            }

            Pos pos = new Pos(xTag.asInt() & 0xF, yTag.asInt(), zTag.asInt() & 0xF);
            tags.put(pos, tag);

            // Handle given Block Entities
            if (pos.getY() < 0 || pos.getY() > 255) continue; // 1.17

            ChunkSection section = chunk.getSections()[pos.getY() >> 4];
            if (section == null) continue;

            int block = section.palette(PaletteType.BLOCKS).idAt(pos.getX(), pos.getY() & 0xF, pos.getZ());

            MappedLegacyBlockItem settings = getMappedBlockItem(block);
            if (settings != null && settings.hasBlockEntityHandler()) {
                settings.getBlockEntityHandler().handleOrNewCompoundTag(block, tag);
            }
        }

        for (int i = 0; i < chunk.getSections().length; i++) {
            ChunkSection section = chunk.getSections()[i];
            if (section == null) {
                continue;
            }

            boolean hasBlockEntityHandler = false;

            // Map blocks
            DataPalette palette = section.palette(PaletteType.BLOCKS);
            for (int j = 0; j < palette.size(); j++) {
                int block = palette.idByIndex(j);
                int btype = block >> 4;
                int meta = block & 0xF;

                Block b = handleBlock(btype, meta);
                if (b != null) {
                    palette.setIdByIndex(j, (b.getId() << 4) | (b.getData() & 0xF));
                }

                // We already know that is has a handler
                if (hasBlockEntityHandler) continue;

                MappedLegacyBlockItem settings = getMappedBlockItem(block);
                if (settings != null && settings.hasBlockEntityHandler()) {
                    hasBlockEntityHandler = true;
                }
            }

            if (!hasBlockEntityHandler) continue;

            // We need to handle a Block Entity :(
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        int block = palette.idAt(x, y, z);

                        MappedLegacyBlockItem settings = getMappedBlockItem(block);
                        if (settings == null || !settings.hasBlockEntityHandler()) continue;

                        Pos pos = new Pos(x, (y + (i << 4)), z);

                        // Already handled above
                        if (tags.containsKey(pos)) continue;

                        CompoundTag tag = new CompoundTag();
                        tag.putInt("x", x + (chunk.getX() << 4));
                        tag.putInt("y", y + (i << 4));
                        tag.putInt("z", z + (chunk.getZ() << 4));

                        settings.getBlockEntityHandler().handleOrNewCompoundTag(block, tag);
                        chunk.getBlockEntities().add(tag);
                    }
                }
            }
        }
    }

    protected CompoundTag getNamedTag(String text) {
        CompoundTag tag = new CompoundTag();
        CompoundTag displayTag = new CompoundTag();
        tag.put("display", displayTag);
        text = "§r" + text;
        displayTag.putString("Name", jsonNameFormat ? ComponentUtil.legacyToJsonString(text) : text);
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
