/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.packets;

import com.google.common.collect.Lists;
import nl.matsv.viabackwards.api.rewriters.BlockItemRewriter;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.Protocol1_11_1To1_12;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data.BlockColors;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data.MapColorMapping;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.storage.InventoryTracker;
import nl.matsv.viabackwards.utils.Block;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.BlockChangeRecord;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_1_2to1_9_3_4.types.Chunk1_9_3_4Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.DoubleTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;

import java.util.Collections;
import java.util.Iterator;

public class BlockItemPackets1_12 extends BlockItemRewriter<Protocol1_11_1To1_12> {
    @Override
    protected void registerPackets(Protocol1_11_1To1_12 protocol) {
        /* Item packets */

        // Confirm Transaction
        protocol.registerOutgoing(State.PLAY, 0x11, 0x11, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE); // Window id
                map(Type.SHORT); // Action number
                map(Type.BOOLEAN); // Accepted
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper packetWrapper) throws Exception {
                        byte window = packetWrapper.get(Type.BYTE, 0);
                        boolean accepted = packetWrapper.get(Type.BOOLEAN, 0);
                        short actionNumber = packetWrapper.get(Type.SHORT, 0);
                        InventoryTracker tracker = packetWrapper.user().get(InventoryTracker.class);

                        boolean clear = false;
                        Iterator<InventoryTracker.ClickWindow> it = Lists.reverse(tracker.getClicks()).iterator();
                        while (it.hasNext()) {
                            InventoryTracker.ClickWindow entry = it.next();
                            if (entry.actionNumber == actionNumber || clear) {
                                it.remove();
                                clear = true;
                            }
                        }
                        if (!accepted) {
                            if (tracker.getLastTransactionWindow() == window &&
                                    tracker.getLastShiftTransaction() != -1) {
                                PacketWrapper confirm = packetWrapper.create(0x6);
                                confirm.write(Type.BYTE, window);
                                confirm.write(Type.SHORT, actionNumber);
                                confirm.write(Type.BOOLEAN, false);
                                confirm.sendToServer(Protocol1_11_1To1_12.class, true, true);
                            }
                            if (tracker.getClicks().size() != 0) {
                                InventoryTracker.ClickWindow entry = tracker.getClicks().get(0);
                                PacketWrapper click = packetWrapper.create(0x8);
                                click.write(Type.UNSIGNED_BYTE, entry.windowId);
                                click.write(Type.SHORT, entry.slot);
                                click.write(Type.BYTE, entry.button);
                                click.write(Type.SHORT, entry.actionNumber);
                                click.write(Type.VAR_INT, entry.mode);
                                CompoundTag tag = new CompoundTag("");
                                tag.put(new DoubleTag("force reject", Double.NaN));
                                click.write(Type.ITEM, new Item((short) 1, (byte) 1, (short) 1, tag));
                                click.sendToServer(Protocol1_11_1To1_12.class, true, true);
                            }
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(State.PLAY, 0x24, 0x24, new PacketRemapper() {
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
                        wrapper.passthrough(Type.BYTE);  //X
                        wrapper.passthrough(Type.BYTE);  //Z
                        Byte[] data = wrapper.read(Type.BYTE_ARRAY);
                        for (int i = 0; i < data.length; i++) {
                            short color = (short) (data[i] & 0xFF);
                            if (color > 143) {
                                color = (short) MapColorMapping.getNearestOldColor(color);
                                data[i] = (byte) color;
                            }
                        }
                        wrapper.write(Type.BYTE_ARRAY, data);
                    }
                });
            }
        });

        // Set slot packet
        protocol.registerOutgoing(State.PLAY, 0x16, 0x16, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE); // 0 - Window ID
                map(Type.SHORT); // 1 - Slot ID
                map(Type.ITEM); // 2 - Slot Value

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item stack = wrapper.get(Type.ITEM, 0);
                        wrapper.set(Type.ITEM, 0, handleItemToClient(stack));
                    }
                });
            }
        });

        // Window items packet
        protocol.registerOutgoing(State.PLAY, 0x14, 0x14, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.ITEM_ARRAY); // 1 - Window Values

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item[] stacks = wrapper.get(Type.ITEM_ARRAY, 0);
                        for (int i = 0; i < stacks.length; i++)
                            stacks[i] = handleItemToClient(stacks[i]);
                    }
                });
            }
        });

        // Entity Equipment Packet
        protocol.registerOutgoing(State.PLAY, 0x3E, 0x3C, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.VAR_INT); // 1 - Slot ID
                map(Type.ITEM); // 2 - Item

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item stack = wrapper.get(Type.ITEM, 0);
                        wrapper.set(Type.ITEM, 0, handleItemToClient(stack));
                    }
                });
            }
        });

        // Plugin message Packet -> Trading
        protocol.registerOutgoing(State.PLAY, 0x18, 0x18, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // 0 - Channel

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        if (wrapper.get(Type.STRING, 0).equalsIgnoreCase("MC|TrList")) {
                            wrapper.passthrough(Type.INT); // Passthrough Window ID

                            int size = wrapper.passthrough(Type.BYTE);
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

        // Confirm transaction
        protocol.registerIncoming(State.PLAY, 0x6, 0x5, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE); // Window id
                map(Type.SHORT); // Action number
                map(Type.BOOLEAN); // Accepted
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper packetWrapper) throws Exception {
                        byte window = packetWrapper.get(Type.BYTE, 0);
                        boolean accepted = packetWrapper.get(Type.BOOLEAN, 0);
                        short actionNumber = packetWrapper.get(Type.SHORT, 0);
                        InventoryTracker tracker = packetWrapper.user().get(InventoryTracker.class);
                        if (tracker.getLastTransactionWindow() == window &&
                                tracker.getLastShiftTransaction() == actionNumber) {
                            tracker.setLastTransactionWindow(-1);
                            tracker.setLastShiftTransaction(-1);
                            tracker.getClicks().clear();
                        }
                    }
                });
            }
        });

        // Click window packet
        protocol.registerIncoming(State.PLAY, 0x08, 0x07, new

                PacketRemapper() {
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
                                InventoryTracker tracker = wrapper.user().get(InventoryTracker.class);
                                short window = wrapper.get(Type.UNSIGNED_BYTE, 0);
                                short slot = wrapper.get(Type.SHORT, 0);
                                byte button = wrapper.get(Type.BYTE, 0);
                                short actionNumber = wrapper.get(Type.SHORT, 1);
                                int mode = wrapper.get(Type.VAR_INT, 0);
                                boolean changedWindow = tracker.getLastTransactionWindow() != window;
                                if (changedWindow) {
                                    tracker.setLastTransactionWindow(window);
                                    tracker.setLastShiftTransaction(-1);
                                    tracker.getClicks().clear();
                                }
                                if (mode == 1) { // Shift click mode
                                    tracker.setLastShiftTransaction(actionNumber);
                                }
                                if (tracker.getLastShiftTransaction() != -1) {
                                    if (tracker.getClicks().size() < 16) {
                                        tracker.getClicks().add(new InventoryTracker.ClickWindow(
                                                window, slot, button, actionNumber, mode));
                                    }
                                }
                                Item item = wrapper.get(Type.ITEM, 0);
                                handleItemToServer(item);
                            }
                        });
                    }
                }
        );

        // Creative Inventory Action
        protocol.registerIncoming(State.PLAY, 0x1B, 0x18, new

                PacketRemapper() {
                    @Override
                    public void registerMap() {
                        map(Type.SHORT); // 0 - Slot
                        map(Type.ITEM); // 1 - Clicked Item

                        handler(new PacketHandler() {
                            @Override
                            public void handle(PacketWrapper wrapper) throws Exception {
                                Item item = wrapper.get(Type.ITEM, 0);
                                handleItemToServer(item);
                            }
                        });
                    }
                }
        );

        /* Block packets */

        // Chunk packet
        protocol.registerOutgoing(State.PLAY, 0x20, 0x20, new

                PacketRemapper() {
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
                }
        );

        // Block Change Packet
        protocol.registerOutgoing(State.PLAY, 0x0B, 0x0B, new

                PacketRemapper() {
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
                }
        );

        // Multi Block Change Packet
        protocol.registerOutgoing(State.PLAY, 0x10, 0x10, new

                PacketRemapper() {
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
                }
        );

        // Update Block Entity
        protocol.registerOutgoing(State.PLAY, 0x09, 0x09, new

                PacketRemapper() {
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

        protocol.getEntityPackets().

                registerMetaHandler().

                handle(e ->

                {
                    Metadata data = e.getData();

                    if (data.getMetaType().getType().equals(Type.ITEM)) // Is Item
                        data.setValue(handleItemToClient((Item) data.getValue()));

                    return data;
                });

        // Client Status
        protocol.registerIncoming(State.PLAY, 0x04, 0x03, new

                PacketRemapper() {
                    @Override
                    public void registerMap() {
                        map(Type.VAR_INT); // Action ID

                        handler(new PacketHandler() {
                            @Override
                            public void handle(PacketWrapper wrapper) throws Exception {
                                // Open Inventory
                                if (wrapper.get(Type.VAR_INT, 0) == 2)
                                    wrapper.cancel(); // TODO is this replaced by something else?
                            }
                        });
                    }
                });
    }

    @Override
    protected void registerRewrites() {
        // Concrete -> Stained clay? (Also got a new name Terracota?)
        rewrite(251)
                .repItem(new Item((short) 159, (byte) 1, (short) -1, getNamedTag("1.12 %viabackwards_color% Concrete")))
                .repBlock(new Block(159, -1));

        // Concrete Powder -> Wool
        rewrite(252)
                .repItem(new Item((short) 35, (byte) 1, (short) -1, getNamedTag("1.12 %viabackwards_color% Concrete Powder")))
                .repBlock(new Block(35, -1));

        // Knowledge book -> book
        rewrite(453)
                .repItem(new Item((short) 340, (byte) 1, (short) 0, getNamedTag("1.12 Knowledge Book")))
                .itemHandler(i -> {
                    CompoundTag tag = i.getTag();

                    if (!tag.contains("ench"))
                        tag.put(new ListTag("ench", Collections.emptyList()));

                    return i;
                });

        // Glazed Terracotta -> Stained Clay
        for (int i = 235; i < 251; i++) {
            rewrite(i).repItem(new Item((short) 159, (byte) 1, (short) (i - 235), getNamedTag("1.12 " + BlockColors.get(i - 235) + " Glazed Terracotta")))
                    .repBlock(new Block(159, (i - 235)));
        }

        // Handle beds
        rewrite(355).repItem(new Item((short) 355, (byte) 1, (short) 0, getNamedTag("1.12 %viabackwards_color% Bed")));

    }
}
