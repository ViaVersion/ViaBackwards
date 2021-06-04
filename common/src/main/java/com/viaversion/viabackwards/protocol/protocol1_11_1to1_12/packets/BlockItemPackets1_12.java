/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.packets;

import com.viaversion.viabackwards.api.rewriters.LegacyBlockItemRewriter;
import com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.Protocol1_11_1To1_12;
import com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.data.MapColorMapping;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.LongArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.ServerboundPackets1_12;
import com.viaversion.viaversion.protocols.protocol1_9_1_2to1_9_3_4.types.Chunk1_9_3_4Type;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Iterator;
import java.util.Map;

public class BlockItemPackets1_12 extends LegacyBlockItemRewriter<Protocol1_11_1To1_12> {

    public BlockItemPackets1_12(Protocol1_11_1To1_12 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_12.MAP_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.BYTE);
                map(Type.BOOLEAN);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int count = wrapper.passthrough(Type.VAR_INT);
                        for (int i = 0; i < count * 3; i++) {
                            wrapper.passthrough(Type.BYTE);
                        }
                    }
                });
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        short columns = wrapper.passthrough(Type.UNSIGNED_BYTE);
                        if (columns <= 0) return;

                        short rows = wrapper.passthrough(Type.UNSIGNED_BYTE);
                        wrapper.passthrough(Type.UNSIGNED_BYTE); // X
                        wrapper.passthrough(Type.UNSIGNED_BYTE); // Z
                        byte[] data = wrapper.read(Type.BYTE_ARRAY_PRIMITIVE);
                        for (int i = 0; i < data.length; i++) {
                            short color = (short) (data[i] & 0xFF);
                            if (color > 143) {
                                color = (short) MapColorMapping.getNearestOldColor(color);
                                data[i] = (byte) color;
                            }
                        }
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, data);
                    }
                });
            }
        });

        registerSetSlot(ClientboundPackets1_12.SET_SLOT, Type.ITEM);
        registerWindowItems(ClientboundPackets1_12.WINDOW_ITEMS, Type.ITEM_ARRAY);
        registerEntityEquipment(ClientboundPackets1_12.ENTITY_EQUIPMENT, Type.ITEM);

        // Plugin message Packet -> Trading
        protocol.registerClientbound(ClientboundPackets1_12.PLUGIN_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // 0 - Channel

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        if (wrapper.get(Type.STRING, 0).equalsIgnoreCase("MC|TrList")) {
                            wrapper.passthrough(Type.INT); // Passthrough Window ID

                            int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                            for (int i = 0; i < size; i++) {
                                wrapper.write(Type.ITEM, handleItemToClient(wrapper.read(Type.ITEM))); // Input Item
                                wrapper.write(Type.ITEM, handleItemToClient(wrapper.read(Type.ITEM))); // Output Item

                                boolean secondItem = wrapper.passthrough(Type.BOOLEAN); // Has second item
                                if (secondItem)
                                    wrapper.write(Type.ITEM, handleItemToClient(wrapper.read(Type.ITEM))); // Second Item

                                wrapper.passthrough(Type.BOOLEAN); // Trade disabled
                                wrapper.passthrough(Type.INT); // Number of tools uses
                                wrapper.passthrough(Type.INT); // Maximum number of trade uses
                            }
                        }
                    }
                });
            }
        });

        protocol.registerServerbound(ServerboundPackets1_9_3.CLICK_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.SHORT); // 1 - Slot
                map(Type.BYTE); // 2 - Button
                map(Type.SHORT); // 3 - Action number
                map(Type.VAR_INT); // 4 - Mode
                map(Type.ITEM); // 5 - Clicked Item

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        if (wrapper.get(Type.VAR_INT, 0) == 1) { // Shift click
                            // https://github.com/ViaVersion/ViaVersion/pull/754
                            // Previously clients grab the item from the clicked slot *before* it has
                            // been moved however now they grab the slot item *after* it has been moved
                            // and send that in the packet.
                            wrapper.set(Type.ITEM, 0, null); // Set null item (probably will work)

                            // Apologize (may happen in some cases, maybe if inventory is full?)
                            PacketWrapper confirm = wrapper.create(ServerboundPackets1_12.WINDOW_CONFIRMATION);
                            confirm.write(Type.BYTE, wrapper.get(Type.UNSIGNED_BYTE, 0).byteValue());
                            confirm.write(Type.SHORT, wrapper.get(Type.SHORT, 1));
                            confirm.write(Type.BOOLEAN, false); // Success - not used

                            wrapper.sendToServer(Protocol1_11_1To1_12.class);
                            wrapper.cancel();
                            confirm.sendToServer(Protocol1_11_1To1_12.class);
                            return;

                        }
                        Item item = wrapper.get(Type.ITEM, 0);
                        handleItemToServer(item);
                    }
                });
            }
        });

        registerCreativeInvAction(ServerboundPackets1_9_3.CREATIVE_INVENTORY_ACTION, Type.ITEM);

        protocol.registerClientbound(ClientboundPackets1_12.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

                        Chunk1_9_3_4Type type = new Chunk1_9_3_4Type(clientWorld); // Use the 1.9.4 Chunk type since nothing changed.
                        Chunk chunk = wrapper.passthrough(type);

                        handleChunk(chunk);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_12.BLOCK_CHANGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // 0 - Block Position
                map(Type.VAR_INT); // 1 - Block

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int idx = wrapper.get(Type.VAR_INT, 0);
                        wrapper.set(Type.VAR_INT, 0, handleBlockID(idx));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_12.MULTI_BLOCK_CHANGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Chunk X
                map(Type.INT); // 1 - Chunk Z
                map(Type.BLOCK_CHANGE_RECORD_ARRAY);

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        for (BlockChangeRecord record : wrapper.get(Type.BLOCK_CHANGE_RECORD_ARRAY, 0)) {
                            record.setBlockId(handleBlockID(record.getBlockId()));
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_12.BLOCK_ENTITY_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // 0 - Position
                map(Type.UNSIGNED_BYTE); // 1 - Action
                map(Type.NBT); // 2 - NBT

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        // Remove bed color
                        if (wrapper.get(Type.UNSIGNED_BYTE, 0) == 11)
                            wrapper.cancel();
                    }
                });
            }
        });

        protocol.getEntityRewriter().filter().handler((event, meta) -> {
            if (meta.metaType().type().equals(Type.ITEM)) // Is Item
                meta.setValue(handleItemToClient((Item) meta.getValue()));
        });

        protocol.registerServerbound(ServerboundPackets1_9_3.CLIENT_STATUS, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Action ID

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        // Open Inventory
                        if (wrapper.get(Type.VAR_INT, 0) == 2) {
                            wrapper.cancel();
                        }
                    }
                });
            }
        });
    }

    @Override
    public @Nullable Item handleItemToClient(Item item) {
        if (item == null) return null;
        super.handleItemToClient(item);

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
            if (entry.getValue() instanceof CompoundTag) {
                CompoundTag nestedBackupTag = new CompoundTag();
                backupTag.put(entry.getKey(), nestedBackupTag);
                hasLongArrayTag |= handleNbtToClient((CompoundTag) entry.getValue(), nestedBackupTag);
            } else if (entry.getValue() instanceof LongArrayTag) {
                backupTag.put(entry.getKey(), fromLongArrayTag((LongArrayTag) entry.getValue()));
                iterator.remove();
                hasLongArrayTag = true;
            }
        }
        return hasLongArrayTag;
    }

    @Override
    public @Nullable Item handleItemToServer(Item item) {
        if (item == null) return null;
        super.handleItemToServer(item);

        if (item.tag() != null) {
            Tag tag = item.tag().remove("Via|LongArrayTags");
            if (tag instanceof CompoundTag) {
                handleNbtToServer(item.tag(), (CompoundTag) tag);
            }
        }

        return item;
    }

    private void handleNbtToServer(CompoundTag compoundTag, CompoundTag backupTag) {
        // Restore the removed long array tags
        for (Map.Entry<String, Tag> entry : backupTag) {
            if (entry.getValue() instanceof CompoundTag) {
                CompoundTag nestedTag = compoundTag.get(entry.getKey());
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
