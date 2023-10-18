/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.template.protocol.rewriter;

import com.viaversion.viabackwards.api.rewriters.ItemRewriter;
import com.viaversion.viabackwards.template.protocol.Protocol1_98To_99;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.types.Chunk1_18Type;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ClientboundPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.packet.ServerboundPackets1_20_2;
import com.viaversion.viaversion.protocols.protocol1_20_2to1_20.rewriter.RecipeRewriter1_20_2;
import com.viaversion.viaversion.rewriter.BlockRewriter;

// To replace if needed:
//   Chunk1_18Type
//   RecipeRewriter1_20_2
public final class BlockItemPacketRewriter1_99 extends ItemRewriter<ClientboundPackets1_20_2, ServerboundPackets1_20_2, Protocol1_98To_99> {

    public BlockItemPacketRewriter1_99(final Protocol1_98To_99 protocol) {
        super(protocol, Type.ITEM1_20_2, Type.ITEM1_20_2_VAR_INT_ARRAY);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPackets1_20_2> blockRewriter = new BlockRewriter<>(protocol, Type.POSITION1_14);
        blockRewriter.registerBlockAction(ClientboundPackets1_20_2.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_20_2.BLOCK_CHANGE);
        blockRewriter.registerVarLongMultiBlockChange1_20(ClientboundPackets1_20_2.MULTI_BLOCK_CHANGE);
        blockRewriter.registerEffect(ClientboundPackets1_20_2.EFFECT, 1010, 2001);
        blockRewriter.registerChunkData1_19(ClientboundPackets1_20_2.CHUNK_DATA, Chunk1_18Type::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_20_2.BLOCK_ENTITY_DATA);

        registerSetCooldown(ClientboundPackets1_20_2.COOLDOWN);
        registerWindowItems1_17_1(ClientboundPackets1_20_2.WINDOW_ITEMS);
        registerSetSlot1_17_1(ClientboundPackets1_20_2.SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_20_2.ADVANCEMENTS);
        registerEntityEquipmentArray(ClientboundPackets1_20_2.ENTITY_EQUIPMENT);
        registerClickWindow1_17_1(ServerboundPackets1_20_2.CLICK_WINDOW);
        registerTradeList1_19(ClientboundPackets1_20_2.TRADE_LIST);
        registerCreativeInvAction(ServerboundPackets1_20_2.CREATIVE_INVENTORY_ACTION, Type.ITEM1_20_2);
        registerWindowPropertyEnchantmentHandler(ClientboundPackets1_20_2.WINDOW_PROPERTY);
        registerSpawnParticle1_19(ClientboundPackets1_20_2.SPAWN_PARTICLE);

        new RecipeRewriter1_20_2<>(protocol).register(ClientboundPackets1_20_2.DECLARE_RECIPES);
    }
}