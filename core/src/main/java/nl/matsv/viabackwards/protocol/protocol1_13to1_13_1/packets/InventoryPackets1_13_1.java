package nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets;

import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class InventoryPackets1_13_1 {

    public static void register(Protocol protocol) {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, InventoryPackets1_13_1::toClient, InventoryPackets1_13_1::toServer);

        // Window items packet
        itemRewriter.registerWindowItems(Type.FLAT_ITEM_ARRAY, 0x15, 0x15);

        // Set slot packet
        itemRewriter.registerSetSlot(Type.FLAT_ITEM, 0x17, 0x17);

        // Plugin Message
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
                                toClient(output);

                                boolean secondItem = wrapper.passthrough(Type.BOOLEAN); //Has second item
                                if (secondItem) {
                                    //Second Item
                                    Item second = wrapper.passthrough(Type.FLAT_ITEM);
                                    toClient(second);
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
        itemRewriter.registerEntityEquipment(Type.FLAT_ITEM, 0x42, 0x42);


        // Click window packet
        itemRewriter.registerClickWindow(Type.FLAT_ITEM, 0x08, 0x08);

        // Creative Inventory Action
        itemRewriter.registerCreativeInvAction(Type.FLAT_ITEM, 0x24, 0x24);
    }

    public static void toClient(Item item) {
        if (item == null) return;
        item.setIdentifier(getOldItemId(item.getIdentifier()));
    }

    // 1.13.1 Item Id
    public static int getNewItemId(int itemId) {
        if (itemId >= 443) {
            return itemId + 5;
        }
        return itemId;
    }

    public static void toServer(Item item) {
        if (item == null) return;
        item.setIdentifier(getNewItemId(item.getIdentifier()));
    }

    // 1.13 Item Id
    public static int getOldItemId(int newId) {
        if (newId >= 448) {
            return newId - 5;
        }
        return newId;
    }
}
