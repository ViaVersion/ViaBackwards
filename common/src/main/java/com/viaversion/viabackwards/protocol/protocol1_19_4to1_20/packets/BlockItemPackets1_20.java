/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
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
package com.viaversion.viabackwards.protocol.protocol1_19_4to1_20.packets;

import com.viaversion.viabackwards.protocol.protocol1_19_4to1_20.Protocol1_19_4To1_20;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.types.Chunk1_18Type;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ClientboundPackets1_19_4;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ServerboundPackets1_19_4;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.rewriter.RecipeRewriter1_19_4;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.ItemRewriter;

public final class BlockItemPackets1_20 extends ItemRewriter<ClientboundPackets1_19_4, ServerboundPackets1_19_4, Protocol1_19_4To1_20> {

    public BlockItemPackets1_20(final Protocol1_19_4To1_20 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPackets1_19_4> blockRewriter = new BlockRewriter<>(protocol, Type.POSITION1_14);
        blockRewriter.registerBlockAction(ClientboundPackets1_19_4.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_19_4.BLOCK_CHANGE);
        blockRewriter.registerVarLongMultiBlockChange(ClientboundPackets1_19_4.MULTI_BLOCK_CHANGE);
        blockRewriter.registerEffect(ClientboundPackets1_19_4.EFFECT, 1010, 2001);
        blockRewriter.registerChunkData1_19(ClientboundPackets1_19_4.CHUNK_DATA, Chunk1_18Type::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_19_4.BLOCK_ENTITY_DATA);

        registerOpenWindow(ClientboundPackets1_19_4.OPEN_WINDOW);
        registerSetCooldown(ClientboundPackets1_19_4.COOLDOWN);
        registerWindowItems1_17_1(ClientboundPackets1_19_4.WINDOW_ITEMS);
        registerSetSlot1_17_1(ClientboundPackets1_19_4.SET_SLOT);
        registerAdvancements(ClientboundPackets1_19_4.ADVANCEMENTS, Type.FLAT_VAR_INT_ITEM);
        registerEntityEquipmentArray(ClientboundPackets1_19_4.ENTITY_EQUIPMENT);
        registerClickWindow1_17_1(ServerboundPackets1_19_4.CLICK_WINDOW);
        registerTradeList1_19(ClientboundPackets1_19_4.TRADE_LIST);
        registerCreativeInvAction(ServerboundPackets1_19_4.CREATIVE_INVENTORY_ACTION, Type.FLAT_VAR_INT_ITEM);
        registerWindowPropertyEnchantmentHandler(ClientboundPackets1_19_4.WINDOW_PROPERTY);
        registerSpawnParticle1_19(ClientboundPackets1_19_4.SPAWN_PARTICLE);

        protocol.registerClientbound(ClientboundPackets1_19_4.OPEN_SIGN_EDITOR, wrapper -> {
            wrapper.passthrough(Type.POSITION1_14);
            final boolean frontSide = wrapper.read(Type.BOOLEAN);
            if (!frontSide) {
                // TODO track front side to check in serverbound
                wrapper.cancel();
            }
        });
        protocol.registerServerbound(ServerboundPackets1_19_4.UPDATE_SIGN, wrapper -> {
            final Position position = wrapper.passthrough(Type.POSITION1_14);
            wrapper.write(Type.BOOLEAN, true); // Front side

            //TODO Track lines to send back with edited
            /*final CompoundTag tag = new CompoundTag();
            final PacketWrapper signUpdate = wrapper.create(ClientboundPackets1_19_4.BLOCK_ENTITY_DATA);
            signUpdate.send(Protocol1_19_4To1_20.class);
            signUpdate.write(Type.POSITION1_14, position);
            signUpdate.write(Type.VAR_INT, 5); // Sign
            signUpdate.write(Type.NBT, tag);*/
        });

        new RecipeRewriter1_19_4<>(protocol).register(ClientboundPackets1_19_4.DECLARE_RECIPES);
    }
}