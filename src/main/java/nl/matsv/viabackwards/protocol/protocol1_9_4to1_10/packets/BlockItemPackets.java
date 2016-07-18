/*
 *
 *     Copyright (C) 2016 Matsv
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.packets;

import nl.matsv.viabackwards.api.rewriters.BlockItemRewriter;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.Protocol1_9To1_10;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.chunks.Chunk1_10;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.chunks.Chunk1_10Type;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.chunks.ChunkSection1_10;
import nl.matsv.viabackwards.utils.Block;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

// TODO REWRITE PLUGINS MESSAGE ITEMS
public class BlockItemPackets extends BlockItemRewriter<Protocol1_9To1_10> {

    protected void registerPackets(Protocol1_9To1_10 protocol) {
        /* Item packets */

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
        protocol.registerOutgoing(State.PLAY, 0x3C, 0x3C, new PacketRemapper() {
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

        // Click window packet
        protocol.registerIncoming(State.PLAY, 0x07, 0x07, new

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
                                Item item = wrapper.get(Type.ITEM, 0);
                                handleItemToServer(item);
                            }
                        });
                    }
                }

        );

        // Creative Inventory Action
        protocol.registerIncoming(State.PLAY, 0x18, 0x18, new

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

                                Chunk1_10Type type = new Chunk1_10Type(clientWorld);
                                Chunk1_10 chunk = (Chunk1_10) wrapper.passthrough(type);

                                for (int i = 0; i < chunk.getSections().length; i++) {
                                    ChunkSection1_10 section = chunk.getSections()[i];
                                    if (section == null)
                                        continue;

                                    for (int x = 0; x < 16; x++) {
                                        for (int y = 0; y < 16; y++) {
                                            for (int z = 0; z < 16; z++) {
                                                int block = section.getBlockId(x, y, z);
                                                if (containsBlock(block)) {
                                                    Block b = handleBlock(block);
                                                    section.setBlock(x, y, z, b.getId(), b.getData());
                                                }
                                            }
                                        }
                                    }
                                }
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

                        handler(new PacketHandler() {
                            @Override
                            public void handle(PacketWrapper wrapper) throws Exception {
                                int count = wrapper.passthrough(Type.VAR_INT); // Array length

                                for (int i = 0; i < count; i++) {
                                    wrapper.passthrough(Type.UNSIGNED_BYTE); // Horizontal position
                                    wrapper.passthrough(Type.UNSIGNED_BYTE); // Y coords

                                    int id = wrapper.read(Type.VAR_INT); // Block ID
                                    wrapper.write(Type.VAR_INT, handleBlockID(id));
                                }
                            }
                        });
                    }
                }

        );

        /* Register Metadata */
        protocol.getEntityPackets().registerMetaRewriter((isObject, entityType, data) -> {
            if (data.getTypeID() == 5) // Is Item
                data.setValue(handleItemToClient((Item) data.getValue()));

            return data;
        });
    }

    protected int handleBlockID(int idx) {
        int type = idx >> 4;

        if (!containsBlock(type))
            return idx;

        Block b = handleBlock(type);
        return (b.getId() << 4 | (b.getData() & 15));
    }

    @Override
    protected void registerRewrites() {
        rewriteItem(255, new Item((short) 166, (byte) 1, (short) 0, getNamedTag("1.10 Structure Block"))); // Structure block only item since the structure block is in 1.9
        rewriteBlockItem(217, new Item((short) 287, (byte) 1, (short) 0, getNamedTag("1.10 Structure Void")), new Block(287, 0)); // Structure void to string
        rewriteBlockItem(213, new Item((short) 159, (byte) 1, (short) 1, getNamedTag("1.10 Magma Block")), new Block(159, 1)); // Magma block to orange clay
        rewriteBlockItem(214, new Item((short) 159, (byte) 1, (short) 14, getNamedTag("1.10 Nether Ward Block")), new Block(159, 14)); // Nether wart block to red clay
        rewriteBlockItem(215, new Item((short) 112, (byte) 1, (short) 0, getNamedTag("1.10 Red Nether Bricks")), new Block(112, 0)); // Red nether brick to nether brick
        rewriteBlockItem(216, new Item((short) 155, (byte) 1, (short) 0, getNamedTag("1.10 Bone Block")), new Block(155, 0)); // Bone block to quartz
    }
}
