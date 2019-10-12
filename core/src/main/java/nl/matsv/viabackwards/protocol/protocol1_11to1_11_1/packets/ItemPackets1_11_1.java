/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_11to1_11_1.packets;

import nl.matsv.viabackwards.api.rewriters.BlockItemRewriter;
import nl.matsv.viabackwards.protocol.protocol1_11to1_11_1.Protocol1_11To1_11_1;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class ItemPackets1_11_1 extends BlockItemRewriter<Protocol1_11To1_11_1> {

    @Override
    protected void registerPackets(Protocol1_11To1_11_1 protocol) {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);

        // Set slot packet
        itemRewriter.registerSetSlot(Type.ITEM, 0x16, 0x16);

        // Window items packet
        itemRewriter.registerWindowItems(Type.ITEM_ARRAY, 0x14, 0x14);

        // Entity Equipment Packet
        itemRewriter.registerEntityEquipment(Type.ITEM, 0x3C, 0x3C);

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

        // Click window packet
        itemRewriter.registerClickWindow(Type.ITEM, 0x07, 0x07);

        // Creative Inventory Action
        itemRewriter.registerCreativeInvAction(Type.ITEM, 0x18, 0x18);

        // Handle item metadata
        protocol.getEntityPackets().registerMetaHandler().handle(e -> {
            Metadata data = e.getData();

            if (data.getMetaType().getType().equals(Type.ITEM)) // Is Item
                data.setValue(handleItemToClient((Item) data.getValue()));

            return data;
        });
    }

    @Override
    protected void registerRewrites() {
        rewrite(452).repItem(new Item((short) 265, (byte) 1, (short) 0, getNamedTag("1.11.2 Iron Nugget")));
    }
}
