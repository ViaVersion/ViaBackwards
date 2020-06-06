package nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets;

import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;

public class InventoryPackets1_13_1 {

    public static void register(Protocol protocol) {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, InventoryPackets1_13_1::toClient, InventoryPackets1_13_1::toServer);

        itemRewriter.registerSetCooldown(ClientboundPackets1_13.COOLDOWN, InventoryPackets1_13_1::getOldItemId);
        itemRewriter.registerWindowItems(ClientboundPackets1_13.WINDOW_ITEMS, Type.FLAT_ITEM_ARRAY);
        itemRewriter.registerSetSlot(ClientboundPackets1_13.SET_SLOT, Type.FLAT_ITEM);

        protocol.registerOutgoing(ClientboundPackets1_13.PLUGIN_MESSAGE, new PacketRemapper() {
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

        itemRewriter.registerEntityEquipment(ClientboundPackets1_13.ENTITY_EQUIPMENT, Type.FLAT_ITEM);
        itemRewriter.registerClickWindow(ServerboundPackets1_13.CLICK_WINDOW, Type.FLAT_ITEM);
        itemRewriter.registerCreativeInvAction(ServerboundPackets1_13.CREATIVE_INVENTORY_ACTION, Type.FLAT_ITEM);
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
