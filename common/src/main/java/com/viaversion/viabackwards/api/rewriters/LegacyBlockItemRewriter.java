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
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_12;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ShortTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.util.ComponentUtil;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.viaversion.viaversion.util.IdAndData;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class LegacyBlockItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends BackwardsItemRewriterBase<C, S, T> {

    protected final Int2ObjectMap<MappedLegacyBlockItem> replacementData = new Int2ObjectOpenHashMap<>(8); // Raw id -> mapped data

    protected LegacyBlockItemRewriter(T protocol, String name, Type<Item> itemType, Type<Item[]> itemArrayType, Type<Item> mappedItemType, Type<Item[]> mappedItemArrayType) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType, false);
        final JsonObject jsonObject = readMappingsFile("item-mappings-" + name + ".json");
        for (final MappedLegacyBlockItem.Type value : MappedLegacyBlockItem.Type.values()) {
            addMappings(value, jsonObject, replacementData);
        }
    }

    protected LegacyBlockItemRewriter(T protocol, String name, Type<Item> itemType, Type<Item[]> itemArrayType) {
        this(protocol, name, itemType, itemArrayType, itemType, itemArrayType);
    }

    protected LegacyBlockItemRewriter(T protocol, String name) {
        this(protocol, name, Type.ITEM1_8, Type.ITEM1_8_SHORT_ARRAY);
    }

    private void addMappings(MappedLegacyBlockItem.Type type, JsonObject object, Int2ObjectMap<MappedLegacyBlockItem> mappings) {
        if (object.has(type.getName())) {
            final JsonObject mappingsObject = object.getAsJsonObject(type.getName());
            for (Map.Entry<String, JsonElement> dataEntry : mappingsObject.entrySet()) {
                addMapping(dataEntry.getKey(), dataEntry.getValue().getAsJsonObject(), type, mappings);
            }
        }
    }

    private void addMapping(String key, JsonObject object, MappedLegacyBlockItem.Type type, Int2ObjectMap<MappedLegacyBlockItem> mappings) {
        int id = object.getAsJsonPrimitive("id").getAsInt();
        JsonPrimitive jsonData = object.getAsJsonPrimitive("data");
        short data = jsonData != null ? jsonData.getAsShort() : 0;
        String name = type != MappedLegacyBlockItem.Type.BLOCK ? object.getAsJsonPrimitive("name").getAsString() : null;

        if (key.indexOf('-') == -1) {
            int unmappedId;
            int dataSeparatorIndex = key.indexOf(':');
            if (dataSeparatorIndex != -1) {
                // Include data
                short unmappedData = Short.parseShort(key.substring(dataSeparatorIndex + 1));
                unmappedId = Integer.parseInt(key.substring(0, dataSeparatorIndex));
                unmappedId = IdAndData.toRawData(unmappedId, unmappedData);
            } else {
                unmappedId = IdAndData.toRawData(Integer.parseInt(key));
            }

            mappings.put(unmappedId, new MappedLegacyBlockItem(id, data, name, type));
            return;
        }

        // Range of ids
        String[] split = key.split("-", 2);
        int from = Integer.parseInt(split[0]);
        int to = Integer.parseInt(split[1]);

        // Special block color handling
        if (name != null && name.contains("%color%")) {
            for (int i = from; i <= to; i++) {
                mappings.put(IdAndData.toRawData(i), new MappedLegacyBlockItem(id, data, name.replace("%color%", BlockColors.get(i - from)), type));
            }
        } else {
            MappedLegacyBlockItem mappedBlockItem = new MappedLegacyBlockItem(id, data, name, type);
            for (int i = from; i <= to; i++) {
                mappings.put(IdAndData.toRawData(i), mappedBlockItem);
            }
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
                    wrapper.set(Type.VAR_INT, 0, handleBlockId(idx));
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
                        record.setBlockId(handleBlockId(record.getBlockId()));
                    }
                });
            }
        });
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable Item item) {
        if (item == null) return null;

        MappedLegacyBlockItem data = getMappedBlockItem(item.identifier(), item.data());
        if (data == null || data.getType() == MappedLegacyBlockItem.Type.BLOCK) {
            // Just rewrite the id
            return super.handleItemToClient(connection, item);
        }
        if (item.tag() == null) {
            item.setTag(new CompoundTag());
        }

        short originalData = item.data();
        item.tag().putInt(nbtTagName("id"), item.identifier());
        item.setIdentifier(data.getId());
        // Keep original data if mapped data is set to -1
        if (data.getData() != -1) {
            item.setData(data.getData());
            item.tag().putShort(nbtTagName("data"), originalData);
        }

        // Set display name
        if (data.getName() != null) {
            CompoundTag display = item.tag().getCompoundTag("display");
            if (display == null) {
                item.tag().put("display", display = new CompoundTag());
            }

            StringTag nameTag = display.getStringTag("Name");
            if (nameTag == null) {
                nameTag = new StringTag(data.getName());
                display.put("Name", nameTag);
                display.put(nbtTagName("customName"), new ByteTag());
            }

            // Handle colors
            String value = nameTag.getValue();
            if (value.contains("%vb_color%")) {
                display.putString("Name", value.replace("%vb_color%", BlockColors.get(originalData)));
            }
        }
        return item;
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable final Item item) {
        if (item == null) return null;
        super.handleItemToServer(connection, item);
        if (item.tag() != null) {
            Tag originalId = item.tag().remove(nbtTagName("id"));
            if (originalId instanceof IntTag) {
                item.setIdentifier(((NumberTag) originalId).asInt());
            }
            Tag originalData = item.tag().remove(nbtTagName("data"));
            if (originalData instanceof ShortTag) {
                item.setData(((NumberTag) originalData).asShort());
            }
        }
        return item;
    }

    public PacketHandler getFallingBlockHandler() {
        return wrapper -> {
            final Optional<EntityTypes1_12.ObjectType> type = EntityTypes1_12.ObjectType.findById(wrapper.get(Type.BYTE, 0));
            if (type.isPresent() && type.get() == EntityTypes1_12.ObjectType.FALLING_BLOCK) {
                final int objectData = wrapper.get(Type.INT, 0);

                final IdAndData block = handleBlock(objectData & 4095, objectData >> 12 & 15);
                if (block == null) return;

                wrapper.set(Type.INT, 0, block.getId() | block.getData() << 12);
            }
        };
    }

    public @Nullable IdAndData handleBlock(int blockId, int data) {
        MappedLegacyBlockItem settings = getMappedBlockItem(blockId, data);
        if (settings == null || settings.getType() == MappedLegacyBlockItem.Type.ITEM) {
            return null;
        }

        IdAndData block = settings.getBlock();
        // For some blocks, the data can still be useful (:
        if (block.getData() == -1) {
            return block.withData(data);
        }
        return block;
    }

    public int handleBlockId(final int rawId) {
        final int id = IdAndData.getId(rawId);
        final int data = IdAndData.getData(rawId);

        final IdAndData mappedBlock = handleBlock(id, data);
        if (mappedBlock == null) return rawId;

        return IdAndData.toRawData(mappedBlock.getId(), mappedBlock.getData());
    }

    public void handleChunk(Chunk chunk) {
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

                IdAndData b = handleBlock(btype, meta);
                if (b != null) {
                    palette.setIdByIndex(j, IdAndData.toRawData(b.getId(), b.getData()));
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

    private @Nullable MappedLegacyBlockItem getMappedBlockItem(int id, int data) {
        MappedLegacyBlockItem mapping = replacementData.get(IdAndData.toRawData(id, data));
        return mapping != null || data == 0 ? mapping : replacementData.get(IdAndData.toRawData(id));
    }

    private @Nullable MappedLegacyBlockItem getMappedBlockItem(int rawId) {
        MappedLegacyBlockItem mapping = replacementData.get(rawId);
        return mapping != null ? mapping : replacementData.get(IdAndData.removeData(rawId));
    }

    protected JsonObject readMappingsFile(final String name) {
        return BackwardsMappingDataLoader.INSTANCE.loadFromDataDir(name);
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
