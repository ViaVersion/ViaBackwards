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
package com.viaversion.viabackwards.protocol.v1_13_1to1_13.rewriter;

import com.viaversion.viabackwards.protocol.v1_13_1to1_13.Protocol1_13_1To1_13;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ServerboundPackets1_13;
import com.viaversion.viaversion.rewriter.ItemRewriter;

public class ItemPacketRewriter1_13_1 extends ItemRewriter<ClientboundPackets1_13, ServerboundPackets1_13, Protocol1_13_1To1_13> {

    public ItemPacketRewriter1_13_1(Protocol1_13_1To1_13 protocol) {
        super(protocol, Types.ITEM1_13, Types.ITEM1_13_SHORT_ARRAY);
    }

    @Override
    public void registerPackets() {
        registerCooldown(ClientboundPackets1_13.COOLDOWN);
        registerSetContent(ClientboundPackets1_13.CONTAINER_SET_CONTENT);
        registerSetSlot(ClientboundPackets1_13.CONTAINER_SET_SLOT);

        protocol.registerClientbound(ClientboundPackets1_13.CUSTOM_PAYLOAD, wrapper -> {
            String channel = wrapper.passthrough(Types.STRING);
            if (channel.equals("minecraft:trader_list")) {
                wrapper.passthrough(Types.INT); //Passthrough Window ID

                int size = wrapper.passthrough(Types.UNSIGNED_BYTE);
                for (int i = 0; i < size; i++) {
                    //Input Item
                    Item input = wrapper.passthrough(Types.ITEM1_13);
                    handleItemToClient(wrapper.user(), input);
                    //Output Item
                    Item output = wrapper.passthrough(Types.ITEM1_13);
                    handleItemToClient(wrapper.user(), output);

                    boolean secondItem = wrapper.passthrough(Types.BOOLEAN); //Has second item
                    if (secondItem) {
                        //Second Item
                        Item second = wrapper.passthrough(Types.ITEM1_13);
                        handleItemToClient(wrapper.user(), second);
                    }

                    wrapper.passthrough(Types.BOOLEAN); //Trade disabled
                    wrapper.passthrough(Types.INT); //Number of tools uses
                    wrapper.passthrough(Types.INT); //Maximum number of trade uses
                }
            }
        });

        registerSetEquippedItem(ClientboundPackets1_13.SET_EQUIPPED_ITEM);
        registerContainerClick(ServerboundPackets1_13.CONTAINER_CLICK);
        registerSetCreativeModeSlot(ServerboundPackets1_13.SET_CREATIVE_MODE_SLOT);
    }
}
