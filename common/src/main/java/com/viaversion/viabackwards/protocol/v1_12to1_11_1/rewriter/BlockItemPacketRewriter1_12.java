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

package com.viaversion.viabackwards.protocol.v1_12to1_11_1.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.LongArrayTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.LegacyBlockItemRewriter;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.Protocol1_12To1_11_1;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.data.MapColorMappings1_11_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ServerboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.SerializerVersion;
import java.util.Iterator;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BlockItemPacketRewriter1_12 extends LegacyBlockItemRewriter<ClientboundPackets1_12, ServerboundPackets1_9_3, Protocol1_12To1_11_1> {

    public BlockItemPacketRewriter1_12(Protocol1_12To1_11_1 protocol) {
        super(protocol, "1.12");
    }

    @Override
    protected void registerPackets() {
        registerBlockChange(ClientboundPackets1_12.BLOCK_UPDATE);
        registerMultiBlockChange(ClientboundPackets1_12.CHUNK_BLOCKS_UPDATE);

        protocol.registerClientbound(ClientboundPackets1_12.MAP_ITEM_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.BYTE);
                map(Types.BOOLEAN);
                handler(wrapper -> {
                    int count = wrapper.passthrough(Types.VAR_INT);
                    for (int i = 0; i < count * 3; i++) {
                        wrapper.passthrough(Types.BYTE);
                    }
                });
                handler(wrapper -> {
                    short columns = wrapper.passthrough(Types.UNSIGNED_BYTE);
                    if (columns <= 0) return;

                    wrapper.passthrough(Types.UNSIGNED_BYTE); // Rows
                    wrapper.passthrough(Types.UNSIGNED_BYTE); // X
                    wrapper.passthrough(Types.UNSIGNED_BYTE); // Z
                    byte[] data = wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
                    for (int i = 0; i < data.length; i++) {
                        short color = (short) (data[i] & 0xFF);
                        if (color > 143) {
                            color = (short) MapColorMappings1_11_1.getNearestOldColor(color);
                            data[i] = (byte) color;
                        }
                    }
                    wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, data);
                });
            }
        });

        registerSetSlot(ClientboundPackets1_12.CONTAINER_SET_SLOT);
        registerSetContent(ClientboundPackets1_12.CONTAINER_SET_CONTENT);
        registerSetEquippedItem(ClientboundPackets1_12.SET_EQUIPPED_ITEM);
        registerCustomPayloadTradeList(ClientboundPackets1_12.CUSTOM_PAYLOAD);

        protocol.registerServerbound(ServerboundPackets1_9_3.CONTAINER_CLICK, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BYTE); // 0 - Window ID
                map(Types.SHORT); // 1 - Slot
                map(Types.BYTE); // 2 - Button
                map(Types.SHORT); // 3 - Action number
                map(Types.VAR_INT); // 4 - Mode
                map(Types.ITEM1_8); // 5 - Clicked Item

                handler(wrapper -> {
                    if (wrapper.get(Types.VAR_INT, 0) == 1) { // Shift click
                        // https://github.com/ViaVersion/ViaVersion/pull/754
                        // Previously clients grab the item from the clicked slot *before* it has
                        // been moved however now they grab the slot item *after* it has been moved
                        // and send that in the packet.
                        wrapper.set(Types.ITEM1_8, 0, null); // Set null item (probably will work)

                        // Apologize (may happen in some cases, maybe if inventory is full?)
                        PacketWrapper confirm = wrapper.create(ServerboundPackets1_12.CONTAINER_ACK);
                        confirm.write(Types.BYTE, wrapper.get(Types.BYTE, 0));
                        confirm.write(Types.SHORT, wrapper.get(Types.SHORT, 1));
                        confirm.write(Types.BOOLEAN, true); // Success - not used

                        wrapper.sendToServer(Protocol1_12To1_11_1.class);
                        wrapper.cancel();
                        confirm.sendToServer(Protocol1_12To1_11_1.class);
                        return;

                    }
                    Item item = wrapper.get(Types.ITEM1_8, 0);
                    handleItemToServer(wrapper.user(), item);
                });
            }
        });

        registerSetCreativeModeSlot(ServerboundPackets1_9_3.SET_CREATIVE_MODE_SLOT);

        protocol.registerClientbound(ClientboundPackets1_12.LEVEL_CHUNK, wrapper -> {
            ClientWorld clientWorld = wrapper.user().getClientWorld(Protocol1_12To1_11_1.class);

            ChunkType1_9_3 type = ChunkType1_9_3.forEnvironment(clientWorld.getEnvironment()); // Use the 1.9.4 Chunk type since nothing changed.
            Chunk chunk = wrapper.passthrough(type);

            handleChunk(chunk);
            for (final CompoundTag tag : chunk.getBlockEntities()) {
                final String id = tag.getString("id");
                if (id == null) {
                    continue;
                }
                if (Key.stripMinecraftNamespace(id).equals("sign")) {
                    handleSignText(tag);
                }
            }
        });

        protocol.registerClientbound(ClientboundPackets1_12.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_8); // 0 - Position
                map(Types.UNSIGNED_BYTE); // 1 - Action
                map(Types.NAMED_COMPOUND_TAG); // 2 - NBT

                handler(wrapper -> {
                    final short type = wrapper.get(Types.UNSIGNED_BYTE, 0);
                    if (type == 9) {
                        final CompoundTag tag = wrapper.get(Types.NAMED_COMPOUND_TAG, 0);
                        handleSignText(tag);
                    } else if (type == 11) {
                        // Remove bed color
                        wrapper.cancel();
                    }
                });
            }
        });

        protocol.getEntityRewriter().filter().handler((event, data) -> {
            if (data.dataType().type().equals(Types.ITEM1_8)) // Is Item
                data.setValue(handleItemToClient(event.user(), (Item) data.getValue()));
        });

        protocol.registerServerbound(ServerboundPackets1_9_3.CLIENT_COMMAND, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Action ID

                handler(wrapper -> {
                    // Open Inventory
                    if (wrapper.get(Types.VAR_INT, 0) == 2) {
                        wrapper.cancel();
                    }
                });
            }
        });
    }

    private void handleSignText(final CompoundTag tag) {
        // Push signs through component conversion, fixes https://github.com/ViaVersion/ViaBackwards/issues/835
        for (int i = 0; i < 4; i++) {
            final StringTag lineTag = tag.getStringTag("Text" + (i + 1));
            if (lineTag == null) {
                continue;
            }

            lineTag.setValue(ComponentUtil.convertJsonOrEmpty(lineTag.getValue(), SerializerVersion.V1_12, SerializerVersion.V1_9).toString());
        }
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, Item item) {
        if (item == null) return null;
        super.handleItemToClient(connection, item);

        if (item.tag() != null) {
            CompoundTag backupTag = new CompoundTag();
            if (handleNbtToClient(item.tag(), backupTag)) {
                item.tag().put("Via|LongArrayTags", backupTag);
            }
        }

        return item;
    }

    private boolean handleNbtToClient(CompoundTag compoundTag, CompoundTag backupTag) {
        // Long array tags were introduced in 1.12 - just remove them
        // Only save the removed tags instead of blindly copying the entire nbt again
        Iterator<Map.Entry<String, Tag>> iterator = compoundTag.iterator();
        boolean hasLongArrayTag = false;
        while (iterator.hasNext()) {
            Map.Entry<String, Tag> entry = iterator.next();
            if (entry.getValue() instanceof CompoundTag tag) {
                CompoundTag nestedBackupTag = new CompoundTag();
                backupTag.put(entry.getKey(), nestedBackupTag);
                hasLongArrayTag |= handleNbtToClient(tag, nestedBackupTag);
            } else if (entry.getValue() instanceof LongArrayTag tag) {
                backupTag.put(entry.getKey(), fromLongArrayTag(tag));
                iterator.remove();
                hasLongArrayTag = true;
            }
        }
        return hasLongArrayTag;
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, Item item) {
        if (item == null) return null;
        item = super.handleItemToServer(connection, item);

        if (item.tag() != null) {
            if (item.tag().remove("Via|LongArrayTags") instanceof CompoundTag tag) {
                handleNbtToServer(item.tag(), tag);
            }
        }

        return item;
    }

    private void handleNbtToServer(CompoundTag compoundTag, CompoundTag backupTag) {
        // Restore the removed long array tags
        for (Map.Entry<String, Tag> entry : backupTag) {
            if (entry.getValue() instanceof CompoundTag) {
                CompoundTag nestedTag = compoundTag.getCompoundTag(entry.getKey());
                handleNbtToServer(nestedTag, (CompoundTag) entry.getValue());
            } else {
                compoundTag.put(entry.getKey(), fromIntArrayTag((IntArrayTag) entry.getValue()));
            }
        }
    }

    private IntArrayTag fromLongArrayTag(LongArrayTag tag) {
        int[] intArray = new int[tag.length() * 2];
        long[] longArray = tag.getValue();
        int i = 0;
        for (long l : longArray) {
            intArray[i++] = (int) (l >> 32);
            intArray[i++] = (int) l;
        }
        return new IntArrayTag(intArray);
    }

    private LongArrayTag fromIntArrayTag(IntArrayTag tag) {
        long[] longArray = new long[tag.length() / 2];
        int[] intArray = tag.getValue();
        for (int i = 0, j = 0; i < intArray.length; i += 2, j++) {
            longArray[j] = (long) intArray[i] << 32 | ((long) intArray[i + 1] & 0xFFFFFFFFL);
        }
        return new LongArrayTag(longArray);
    }
}
