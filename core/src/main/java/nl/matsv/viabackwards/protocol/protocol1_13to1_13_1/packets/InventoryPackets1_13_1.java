package nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets;

import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class InventoryPackets1_13_1 {

    public static void register(Protocol protocol) {

        /*
            Outgoing packets
         */

        // Window items packet
        protocol.registerOutgoing(State.PLAY, 0x15, 0x15, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.FLAT_ITEM_ARRAY); // 1 - Window Values

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item[] stacks = wrapper.get(Type.FLAT_ITEM_ARRAY, 0);
                        for (Item stack : stacks)
                            toClient(stack);
                    }
                });
            }
        });

        // Set slot packet
        protocol.registerOutgoing(State.PLAY, 0x17, 0x17, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE); // 0 - Window ID
                map(Type.SHORT); // 1 - Slot ID
                map(Type.FLAT_ITEM); // 2 - Slot Value

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item stack = wrapper.get(Type.FLAT_ITEM, 0);
                        toClient(stack);
                    }
                });
            }
        });

        //Plugin Message
        protocol.registerOutgoing(State.PLAY, 0x19, 0x19, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        String channel = wrapper.passthrough(Type.STRING);
                        if (channel.equals("minecraft:trader_list")) {
                            wrapper.passthrough(Type.INT); //Passthrough Window ID

                            int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                            for (int i = 0; i < size; i++) {
                                //Input Item
                                Item input = wrapper.passthrough(Type.FLAT_ITEM);
                                toClient(input);
                                //Output Item
                                Item output = wrapper.passthrough(Type.FLAT_ITEM);
                                toClient(input);

                                boolean secondItem = wrapper.passthrough(Type.BOOLEAN); //Has second item
                                if (secondItem) {
                                    //Second Item
                                    Item second = wrapper.passthrough(Type.FLAT_ITEM);
                                    toClient(input);
                                }

                                wrapper.passthrough(Type.BOOLEAN); //Trade disabled
                                wrapper.passthrough(Type.INT); //Number of tools uses
                                wrapper.passthrough(Type.INT); //Maximum number of trade uses
                            }
                        }
                    }
                });
            }
        });

        // Entity Equipment Packet
        protocol.registerOutgoing(State.PLAY, 0x42, 0x42, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.VAR_INT); // 1 - Slot ID
                map(Type.FLAT_ITEM); // 2 - Item

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item stack = wrapper.get(Type.FLAT_ITEM, 0);
                        toClient(stack);
                    }
                });
            }
        });


        /*
            Incoming packets
         */

        // Click window packet
        protocol.registerIncoming(State.PLAY, 0x08, 0x08, new PacketRemapper() {
                    @Override
                    public void registerMap() {
                        map(Type.UNSIGNED_BYTE); // 0 - Window ID
                        map(Type.SHORT); // 1 - Slot
                        map(Type.BYTE); // 2 - Button
                        map(Type.SHORT); // 3 - Action number
                        map(Type.VAR_INT); // 4 - Mode
                        map(Type.FLAT_ITEM); // 5 - Clicked Item

                        handler(new PacketHandler() {
                            @Override
                            public void handle(PacketWrapper wrapper) throws Exception {
                                Item item = wrapper.get(Type.FLAT_ITEM, 0);
                                toServer(item);
                            }
                        });
                    }
                }
        );

        // Creative Inventory Action
        protocol.registerIncoming(State.PLAY, 0x24, 0x24, new PacketRemapper() {
                    @Override
                    public void registerMap() {
                        map(Type.SHORT); // 0 - Slot
                        map(Type.FLAT_ITEM); // 1 - Clicked Item

                        handler(new PacketHandler() {
                            @Override
                            public void handle(PacketWrapper wrapper) throws Exception {
                                Item item = wrapper.get(Type.FLAT_ITEM, 0);
                                toServer(item);
                            }
                        });
                    }
                }
        );
    }

    public static void toClient(Item item) {
        if (item == null) return;
        item.setId((short) getOldItemId(item.getId()));
    }

    //1.13.1 Item Id
    public static int getNewItemId(int itemId) {
        if (itemId >= 443) {
            return itemId + 5;
        }
        return itemId;
    }

    public static void toServer(Item item) {
        if (item == null) return;
        item.setId((short) getNewItemId(item.getId()));
    }

    //1.13 Item Id
    public static int getOldItemId(int newId) {
        if (newId >= 448) {
            return newId - 5;
        }
        return newId;
    }
}
