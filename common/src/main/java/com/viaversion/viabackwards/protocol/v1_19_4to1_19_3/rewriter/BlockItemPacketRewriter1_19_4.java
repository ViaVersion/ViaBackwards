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
package com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.Protocol1_19_4To1_19_3;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ServerboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.rewriter.RecipeRewriter1_19_3;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.util.Key;

public final class BlockItemPacketRewriter1_19_4 extends BackwardsItemRewriter<ClientboundPackets1_19_4, ServerboundPackets1_19_3, Protocol1_19_4To1_19_3> {

    public BlockItemPacketRewriter1_19_4(final Protocol1_19_4To1_19_3 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_ARRAY);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPackets1_19_4> blockRewriter = BlockRewriter.for1_14(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_19_4.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_19_4.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate(ClientboundPackets1_19_4.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent(ClientboundPackets1_19_4.LEVEL_EVENT, 1010, 2001);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_19_4.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_18::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_19_4.BLOCK_ENTITY_DATA);

        protocol.registerClientbound(ClientboundPackets1_19_4.OPEN_SCREEN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Container id
                map(Types.VAR_INT); // Container type
                map(Types.COMPONENT); // Title
                handler(wrapper -> {
                    final int windowType = wrapper.get(Types.VAR_INT, 1);
                    if (windowType == 21) { // New smithing menu
                        wrapper.cancel();
                    } else if (windowType > 21) {
                        wrapper.set(Types.VAR_INT, 1, windowType - 1);
                    }

                    protocol.getComponentRewriter().processText(wrapper.user(), wrapper.get(Types.COMPONENT, 0));
                });
            }
        });

        registerCooldown(ClientboundPackets1_19_4.COOLDOWN);
        registerSetContent1_17_1(ClientboundPackets1_19_4.CONTAINER_SET_CONTENT);
        registerSetSlot1_17_1(ClientboundPackets1_19_4.CONTAINER_SET_SLOT);
        registerAdvancements(ClientboundPackets1_19_4.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets1_19_4.SET_EQUIPMENT);
        registerContainerClick1_17_1(ServerboundPackets1_19_3.CONTAINER_CLICK);
        registerMerchantOffers1_19(ClientboundPackets1_19_4.MERCHANT_OFFERS);
        registerSetCreativeModeSlot(ServerboundPackets1_19_3.SET_CREATIVE_MODE_SLOT);
        registerContainerSetData(ClientboundPackets1_19_4.CONTAINER_SET_DATA);

        final RecipeRewriter1_19_3<ClientboundPackets1_19_4> recipeRewriter = new RecipeRewriter1_19_3<>(protocol) {
            @Override
            public void handleCraftingShaped(final PacketWrapper wrapper) {
                final int ingredients = wrapper.passthrough(Types.VAR_INT) * wrapper.passthrough(Types.VAR_INT);
                wrapper.passthrough(Types.STRING); // Group
                wrapper.passthrough(Types.VAR_INT); // Crafting book category
                for (int i = 0; i < ingredients; i++) {
                    handleIngredient(wrapper);
                }
                rewrite(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)); // Result

                // Remove notification boolean
                wrapper.read(Types.BOOLEAN);
            }
        };
        protocol.registerClientbound(ClientboundPackets1_19_4.UPDATE_RECIPES, wrapper -> {
            final int size = wrapper.passthrough(Types.VAR_INT);
            int newSize = size;
            for (int i = 0; i < size; i++) {
                final String type = wrapper.read(Types.STRING);
                final String cutType = Key.stripMinecraftNamespace(type);
                if (cutType.equals("smithing_transform") || cutType.equals("smithing_trim")) {
                    newSize--;
                    wrapper.read(Types.STRING); // Recipe identifier
                    wrapper.read(Types.ITEM1_13_2_ARRAY); // Template
                    wrapper.read(Types.ITEM1_13_2_ARRAY); // Base
                    wrapper.read(Types.ITEM1_13_2_ARRAY); // Additions
                    if (cutType.equals("smithing_transform")) {
                        wrapper.read(Types.ITEM1_13_2); // Result
                    }
                    continue;
                } else if (cutType.equals("crafting_decorated_pot")) {
                    newSize--;
                    wrapper.read(Types.STRING); // Recipe identifier
                    wrapper.read(Types.VAR_INT); // Crafting book category
                    continue;
                }

                wrapper.write(Types.STRING, type);
                wrapper.passthrough(Types.STRING); // Recipe Identifier
                recipeRewriter.handleRecipeType(wrapper, cutType);
            }

            wrapper.set(Types.VAR_INT, 0, newSize);
        });
    }
}
