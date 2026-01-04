/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.v1_11_1to1_11.rewriter;

import com.viaversion.viabackwards.api.rewriters.LegacyBlockItemRewriter;
import com.viaversion.viabackwards.api.rewriters.LegacyEnchantmentRewriter;
import com.viaversion.viabackwards.protocol.v1_11_1to1_11.Protocol1_11_1To1_11;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;

public class ItemPacketRewriter1_11_1 extends LegacyBlockItemRewriter<ClientboundPackets1_9_3, ServerboundPackets1_9_3, Protocol1_11_1To1_11> {

    private LegacyEnchantmentRewriter enchantmentRewriter;

    public ItemPacketRewriter1_11_1(Protocol1_11_1To1_11 protocol) {
        super(protocol, "1.11.1");
    }

    @Override
    protected void registerPackets() {
        registerSetSlot(ClientboundPackets1_9_3.CONTAINER_SET_SLOT);
        registerSetContent(ClientboundPackets1_9_3.CONTAINER_SET_CONTENT);
        registerSetEquippedItem(ClientboundPackets1_9_3.SET_EQUIPPED_ITEM);
        registerCustomPayloadTradeList(ClientboundPackets1_9_3.CUSTOM_PAYLOAD);

        registerContainerClick(ServerboundPackets1_9_3.CONTAINER_CLICK);
        registerSetCreativeModeSlot(ServerboundPackets1_9_3.SET_CREATIVE_MODE_SLOT);

        // Handle item entity data
        protocol.getEntityRewriter().filter().handler((event, data) -> {
            if (data.dataType().type().equals(Types.ITEM1_8)) { // Is Item
                data.setValue(handleItemToClient(event.user(), (Item) data.getValue()));
            }
        });
    }

    @Override
    protected void registerRewrites() {
        enchantmentRewriter = new LegacyEnchantmentRewriter(nbtTagName());
        enchantmentRewriter.registerEnchantment(22, "§7Sweeping Edge");
    }

    @Override
    public Item handleItemToClient(UserConnection connection, Item item) {
        if (item == null) return null;
        super.handleItemToClient(connection, item);

        enchantmentRewriter.handleToClient(item);
        return item;
    }

    @Override
    public Item handleItemToServer(UserConnection connection, Item item) {
        if (item == null) return null;
        item = super.handleItemToServer(connection, item);

        enchantmentRewriter.handleToServer(item);
        return item;
    }
}
