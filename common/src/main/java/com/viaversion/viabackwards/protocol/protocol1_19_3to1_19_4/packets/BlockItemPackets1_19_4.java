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
package com.viaversion.viabackwards.protocol.protocol1_19_3to1_19_4.packets;

import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.protocol1_19_3to1_19_4.Protocol1_19_3To1_19_4;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ServerboundPackets1_19_3;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.rewriter.RecipeRewriter1_19_3;
import com.viaversion.viaversion.protocols.protocol1_19_4to1_19_3.ClientboundPackets1_19_4;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.util.Key;

public final class BlockItemPackets1_19_4 extends BackwardsItemRewriter<ClientboundPackets1_19_4, ServerboundPackets1_19_3, Protocol1_19_3To1_19_4> {

    public BlockItemPackets1_19_4(final Protocol1_19_3To1_19_4 protocol) {
        super(protocol, Type.ITEM1_13_2, Type.ITEM1_13_2_ARRAY);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPackets1_19_4> blockRewriter = BlockRewriter.for1_14(protocol);
        blockRewriter.registerBlockAction(ClientboundPackets1_19_4.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_19_4.BLOCK_CHANGE);
        blockRewriter.registerVarLongMultiBlockChange(ClientboundPackets1_19_4.MULTI_BLOCK_CHANGE);
        blockRewriter.registerEffect(ClientboundPackets1_19_4.EFFECT, 1010, 2001);
        blockRewriter.registerChunkData1_19(ClientboundPackets1_19_4.CHUNK_DATA, ChunkType1_18::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_19_4.BLOCK_ENTITY_DATA);

        protocol.registerClientbound(ClientboundPackets1_19_4.OPEN_WINDOW, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Container id
                map(Type.VAR_INT); // Container type
                map(Type.COMPONENT); // Title
                handler(wrapper -> {
                    final int windowType = wrapper.get(Type.VAR_INT, 1);
                    if (windowType == 21) { // New smithing menu
                        wrapper.cancel();
                    } else if (windowType > 21) {
                        wrapper.set(Type.VAR_INT, 1, windowType - 1);
                    }

                    protocol.getTranslatableRewriter().processText(wrapper.user(), wrapper.get(Type.COMPONENT, 0));
                });
            }
        });

        registerSetCooldown(ClientboundPackets1_19_4.COOLDOWN);
        registerWindowItems1_17_1(ClientboundPackets1_19_4.WINDOW_ITEMS);
        registerSetSlot1_17_1(ClientboundPackets1_19_4.SET_SLOT);
        registerAdvancements(ClientboundPackets1_19_4.ADVANCEMENTS);
        registerEntityEquipmentArray(ClientboundPackets1_19_4.ENTITY_EQUIPMENT);
        registerClickWindow1_17_1(ServerboundPackets1_19_3.CLICK_WINDOW);
        registerTradeList1_19(ClientboundPackets1_19_4.TRADE_LIST);
        registerCreativeInvAction(ServerboundPackets1_19_3.CREATIVE_INVENTORY_ACTION);
        registerWindowPropertyEnchantmentHandler(ClientboundPackets1_19_4.WINDOW_PROPERTY);
        registerSpawnParticle1_19(ClientboundPackets1_19_4.SPAWN_PARTICLE);

        final RecipeRewriter1_19_3<ClientboundPackets1_19_4> recipeRewriter = new RecipeRewriter1_19_3<ClientboundPackets1_19_4>(protocol) {
            @Override
            public void handleCraftingShaped(final PacketWrapper wrapper) throws Exception {
                final int ingredients = wrapper.passthrough(Type.VAR_INT) * wrapper.passthrough(Type.VAR_INT);
                wrapper.passthrough(Type.STRING); // Group
                wrapper.passthrough(Type.VAR_INT); // Crafting book category
                for (int i = 0; i < ingredients; i++) {
                    handleIngredient(wrapper);
                }
                rewrite(wrapper.user(), wrapper.passthrough(Type.ITEM1_13_2)); // Result

                // Remove notification boolean
                wrapper.read(Type.BOOLEAN);
            }
        };
        protocol.registerClientbound(ClientboundPackets1_19_4.DECLARE_RECIPES, wrapper -> {
            final int size = wrapper.passthrough(Type.VAR_INT);
            int newSize = size;
            for (int i = 0; i < size; i++) {
                final String type = wrapper.read(Type.STRING);
                final String cutType = Key.stripMinecraftNamespace(type);
                if (cutType.equals("smithing_transform") || cutType.equals("smithing_trim")) {
                    newSize--;
                    wrapper.read(Type.STRING); // Recipe identifier
                    wrapper.read(Type.ITEM1_13_2_ARRAY); // Template
                    wrapper.read(Type.ITEM1_13_2_ARRAY); // Base
                    wrapper.read(Type.ITEM1_13_2_ARRAY); // Additions
                    if (cutType.equals("smithing_transform")) {
                        wrapper.read(Type.ITEM1_13_2); // Result
                    }
                    continue;
                } else if (cutType.equals("crafting_decorated_pot")) {
                    newSize--;
                    wrapper.read(Type.STRING); // Recipe identifier
                    wrapper.read(Type.VAR_INT); // Crafting book category
                    continue;
                }

                wrapper.write(Type.STRING, type);
                wrapper.passthrough(Type.STRING); // Recipe Identifier
                recipeRewriter.handleRecipeType(wrapper, cutType);
            }

            wrapper.set(Type.VAR_INT, 0, newSize);
        });
    }
}