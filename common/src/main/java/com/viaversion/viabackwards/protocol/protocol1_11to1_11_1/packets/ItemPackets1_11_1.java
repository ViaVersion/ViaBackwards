/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.protocol1_11to1_11_1.packets;

import com.viaversion.viabackwards.api.rewriters.LegacyBlockItemRewriter;
import com.viaversion.viabackwards.api.rewriters.LegacyEnchantmentRewriter;
import com.viaversion.viabackwards.protocol.protocol1_11to1_11_1.Protocol1_11To1_11_1;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;

public class ItemPackets1_11_1 extends LegacyBlockItemRewriter<ClientboundPackets1_9_3, ServerboundPackets1_9_3, Protocol1_11To1_11_1> {

    private LegacyEnchantmentRewriter enchantmentRewriter;

    public ItemPackets1_11_1(Protocol1_11To1_11_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        registerSetSlot(ClientboundPackets1_9_3.SET_SLOT, Type.ITEM);
        registerWindowItems(ClientboundPackets1_9_3.WINDOW_ITEMS, Type.ITEM1_8_ARRAY);
        registerEntityEquipment(ClientboundPackets1_9_3.ENTITY_EQUIPMENT, Type.ITEM);

        // Plugin message Packet -> Trading
        protocol.registerClientbound(ClientboundPackets1_9_3.PLUGIN_MESSAGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // 0 - Channel

                handler(wrapper -> {
                    if (wrapper.get(Type.STRING, 0).equalsIgnoreCase("MC|TrList")) {
                        wrapper.passthrough(Type.INT); // Passthrough Window ID

                        int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                        for (int i = 0; i < size; i++) {
                            wrapper.write(Type.ITEM, handleItemToClient(wrapper.read(Type.ITEM))); // Input Item
                            wrapper.write(Type.ITEM, handleItemToClient(wrapper.read(Type.ITEM))); // Output Item

                            boolean secondItem = wrapper.passthrough(Type.BOOLEAN); // Has second item
                            if (secondItem) {
                                wrapper.write(Type.ITEM, handleItemToClient(wrapper.read(Type.ITEM))); // Second Item
                            }

                            wrapper.passthrough(Type.BOOLEAN); // Trade disabled
                            wrapper.passthrough(Type.INT); // Number of tools uses
                            wrapper.passthrough(Type.INT); // Maximum number of trade uses
                        }
                    }
                });
            }
        });

        registerClickWindow(ServerboundPackets1_9_3.CLICK_WINDOW, Type.ITEM);
        registerCreativeInvAction(ServerboundPackets1_9_3.CREATIVE_INVENTORY_ACTION, Type.ITEM);

        // Handle item metadata
        protocol.getEntityRewriter().filter().handler((event, meta) -> {
            if (meta.metaType().type().equals(Type.ITEM)) { // Is Item
                meta.setValue(handleItemToClient((Item) meta.getValue()));
            }
        });
    }

    @Override
    protected void registerRewrites() {
        enchantmentRewriter = new LegacyEnchantmentRewriter(nbtTagName);
        enchantmentRewriter.registerEnchantment(22, "§7Sweeping Edge");
    }

    @Override
    public Item handleItemToClient(Item item) {
        if (item == null) return null;
        super.handleItemToClient(item);

        CompoundTag tag = item.tag();
        if (tag == null) return item;

        if (tag.get("ench") instanceof ListTag) {
            enchantmentRewriter.rewriteEnchantmentsToClient(tag, false);
        }
        if (tag.get("StoredEnchantments") instanceof ListTag) {
            enchantmentRewriter.rewriteEnchantmentsToClient(tag, true);
        }
        return item;
    }

    @Override
    public Item handleItemToServer(Item item) {
        if (item == null) return null;
        super.handleItemToServer(item);

        CompoundTag tag = item.tag();
        if (tag == null) return item;

        if (tag.contains(nbtTagName + "|ench")) {
            enchantmentRewriter.rewriteEnchantmentsToServer(tag, false);
        }
        if (tag.contains(nbtTagName + "|StoredEnchantments")) {
            enchantmentRewriter.rewriteEnchantmentsToServer(tag, true);
        }
        return item;
    }
}
