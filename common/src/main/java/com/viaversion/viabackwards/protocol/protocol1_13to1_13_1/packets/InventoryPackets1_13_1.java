/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_13to1_13_1.packets;

import com.viaversion.viabackwards.protocol.protocol1_13to1_13_1.Protocol1_13To1_13_1;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;
import com.viaversion.viaversion.rewriter.ItemRewriter;

public class InventoryPackets1_13_1 extends ItemRewriter<ClientboundPackets1_13, ServerboundPackets1_13, Protocol1_13To1_13_1> {

    public InventoryPackets1_13_1(Protocol1_13To1_13_1 protocol) {
        super(protocol, Type.ITEM1_13, Type.ITEM1_13_ARRAY);
    }

    @Override
    public void registerPackets() {
        registerSetCooldown(ClientboundPackets1_13.COOLDOWN);
        registerWindowItems(ClientboundPackets1_13.WINDOW_ITEMS, Type.ITEM1_13_ARRAY);
        registerSetSlot(ClientboundPackets1_13.SET_SLOT, Type.ITEM1_13);

        protocol.registerClientbound(ClientboundPackets1_13.PLUGIN_MESSAGE, wrapper -> {
            String channel = wrapper.passthrough(Type.STRING);
            if (channel.equals("minecraft:trader_list")) {
                wrapper.passthrough(Type.INT); //Passthrough Window ID

                int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                for (int i = 0; i < size; i++) {
                    //Input Item
                    Item input = wrapper.passthrough(Type.ITEM1_13);
                    handleItemToClient(input);
                    //Output Item
                    Item output = wrapper.passthrough(Type.ITEM1_13);
                    handleItemToClient(output);

                    boolean secondItem = wrapper.passthrough(Type.BOOLEAN); //Has second item
                    if (secondItem) {
                        //Second Item
                        Item second = wrapper.passthrough(Type.ITEM1_13);
                        handleItemToClient(second);
                    }

                    wrapper.passthrough(Type.BOOLEAN); //Trade disabled
                    wrapper.passthrough(Type.INT); //Number of tools uses
                    wrapper.passthrough(Type.INT); //Maximum number of trade uses
                }
            }
        });

        registerEntityEquipment(ClientboundPackets1_13.ENTITY_EQUIPMENT, Type.ITEM1_13);
        registerClickWindow(ServerboundPackets1_13.CLICK_WINDOW, Type.ITEM1_13);
        registerCreativeInvAction(ServerboundPackets1_13.CREATIVE_INVENTORY_ACTION, Type.ITEM1_13);

        registerSpawnParticle(ClientboundPackets1_13.SPAWN_PARTICLE, Type.ITEM1_13, Type.FLOAT);
    }
}
