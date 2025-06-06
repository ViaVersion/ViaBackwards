/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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

import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.ShortTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingDataLoader;
import com.viaversion.viabackwards.api.data.MappedLegacyBlockItem;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.data.BlockColors1_11_1;
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
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.IdAndData;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class LegacyBlockItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends BackwardsItemRewriterBase<C, S, T> {

    protected final Int2ObjectMap<MappedLegacyBlockItem> itemReplacements = new Int2ObjectOpenHashMap<>(8); // Raw id -> mapped data
    protected final Int2ObjectMap<MappedLegacyBlockItem> blockReplacements = new Int2ObjectOpenHashMap<>(8); // Raw id -> mapped data

    protected LegacyBlockItemRewriter(T protocol, String name, Type<Item> itemType, Type<Item[]> itemArrayType, Type<Item> mappedItemType, Type<Item[]> mappedItemArrayType) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType, false);

        Int2ObjectMap<MappedLegacyBlockItem> blockItemReplacements = new Int2ObjectOpenHashMap<>(8);
        final JsonObject jsonObject = readMappingsFile("item-mappings-" + name + ".json");
        addMappings(MappedLegacyBlockItem.Type.ITEM, jsonObject, itemReplacements);
        addMappings(MappedLegacyBlockItem.Type.BLOCK_ITEM, jsonObject, blockItemReplacements);
        addMappings(MappedLegacyBlockItem.Type.BLOCK, jsonObject, blockReplacements);

        blockReplacements.putAll(blockItemReplacements);
        itemReplacements.putAll(blockItemReplacements);
    }

    protected LegacyBlockItemRewriter(T protocol, String name, Type<Item> itemType, Type<Item[]> itemArrayType) {
        this(protocol, name, itemType, itemArrayType, itemType, itemArrayType);
    }

    protected LegacyBlockItemRewriter(T protocol, String name) {
        this(protocol, name, Types.ITEM1_8, Types.ITEM1_8_SHORT_ARRAY);
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
                unmappedId = compress(unmappedId, unmappedData);
            } else {
                unmappedId = compress(Integer.parseInt(key), -1);
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
                mappings.put(compress(i, -1), new MappedLegacyBlockItem(id, data, name.replace("%color%", BlockColors1_11_1.get(i - from)), type));
            }
        } else {
            MappedLegacyBlockItem mappedBlockItem = new MappedLegacyBlockItem(id, data, name, type);
            for (int i = from; i <= to; i++) {
                mappings.put(compress(i, -1), mappedBlockItem);
            }
        }
    }

    public void registerBlockChange(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_8); // 0 - Block Position
                map(Types.VAR_INT); // 1 - Block

                handler(wrapper -> {
                    int idx = wrapper.get(Types.VAR_INT, 0);
                    wrapper.set(Types.VAR_INT, 0, handleBlockId(idx));
                });
            }
        });
    }

    public void registerMultiBlockChange(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Chunk X
                map(Types.INT); // 1 - Chunk Z
                map(Types.BLOCK_CHANGE_ARRAY);

                handler(wrapper -> {
                    for (BlockChangeRecord record : wrapper.get(Types.BLOCK_CHANGE_ARRAY, 0)) {
                        record.setBlockId(handleBlockId(record.getBlockId()));
                    }
                });
            }
        });
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable Item item) {
        if (item == null) return null;

        MappedLegacyBlockItem data = getMappedItem(item.identifier(), item.data());
        if (data == null) {
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
                display.put(nbtTagName("customName"), new ByteTag(false));
            }

            // Handle colors
            String value = nameTag.getValue();
            if (value.contains("%vb_color%")) {
                display.putString("Name", value.replace("%vb_color%", BlockColors1_11_1.get(originalData)));
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
            final int objectData = wrapper.get(Types.INT, 0);
            final EntityTypes1_12.ObjectType type = EntityTypes1_12.ObjectType.findById(wrapper.get(Types.BYTE, 0), objectData);
            if (type == EntityTypes1_12.ObjectType.FALLING_BLOCK) {
                final IdAndData block = handleBlock(objectData & 4095, objectData >> 12 & 15);
                if (block == null) return;

                wrapper.set(Types.INT, 0, block.getId() | block.getData() << 12);
            }
        };
    }

    public @Nullable IdAndData handleBlock(int blockId, int data) {
        MappedLegacyBlockItem settings = getMappedBlock(blockId, data);
        if (settings == null) {
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
            if (pos.y() < 0 || pos.y() > 255) continue; // 1.17

            ChunkSection section = chunk.getSections()[pos.y() >> 4];
            if (section == null) continue;

            int block = section.palette(PaletteType.BLOCKS).idAt(pos.x(), pos.y() & 0xF, pos.z());

            MappedLegacyBlockItem settings = getMappedBlock(block);
            if (settings != null && settings.hasBlockEntityHandler()) {
                settings.getBlockEntityHandler().handleCompoundTag(block, tag);
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

                MappedLegacyBlockItem settings = getMappedBlock(block);
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

                        MappedLegacyBlockItem settings = getMappedBlock(block);
                        if (settings == null || !settings.hasBlockEntityHandler()) continue;

                        Pos pos = new Pos(x, (y + (i << 4)), z);

                        // Already handled above
                        if (tags.containsKey(pos)) continue;

                        CompoundTag tag = new CompoundTag();
                        tag.putInt("x", x + (chunk.getX() << 4));
                        tag.putInt("y", y + (i << 4));
                        tag.putInt("z", z + (chunk.getZ() << 4));

                        settings.getBlockEntityHandler().handleCompoundTag(block, tag);
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

    private @Nullable MappedLegacyBlockItem getMappedBlock(int id, int data) {
        MappedLegacyBlockItem mapping = blockReplacements.get(compress(id, data));
        return mapping != null ? mapping : blockReplacements.get(compress(id, -1));
    }

    private @Nullable MappedLegacyBlockItem getMappedItem(int id, int data) {
        MappedLegacyBlockItem mapping = itemReplacements.get(compress(id, data));
        return mapping != null ? mapping : itemReplacements.get(compress(id, -1));
    }

    private @Nullable MappedLegacyBlockItem getMappedBlock(int rawId) {
        int id = IdAndData.getId(rawId);
        int data = IdAndData.getData(rawId);
        return getMappedBlock(id, data);
    }

    protected JsonObject readMappingsFile(final String name) {
        return BackwardsMappingDataLoader.INSTANCE.loadFromDataDir(name);
    }

    protected int compress(final int id, final int data) {
        // Using IdAndData for the internal storage can cause id overlaps in edge cases and would lead to wrong data
        return (id << 16) | (data & 0xFFFF);
    }

    private record Pos(int x, short y, int z) {

        public Pos(int x, int y, int z) {
            this(x, (short) y, z);
        }
    }
}
