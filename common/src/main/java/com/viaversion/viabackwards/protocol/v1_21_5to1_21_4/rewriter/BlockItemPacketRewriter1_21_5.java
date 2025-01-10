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
package com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.Protocol1_21_5To1_21_4;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_21_4;
import com.viaversion.viaversion.api.type.types.version.Types1_21_5;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPacket1_21_4;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPackets1_21_4;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPackets1_21_5;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;

public final class BlockItemPacketRewriter1_21_5 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21_5, ServerboundPacket1_21_4, Protocol1_21_5To1_21_4> {

    public BlockItemPacketRewriter1_21_5(final Protocol1_21_5To1_21_4 protocol) {
        super(protocol,
            Types1_21_5.ITEM, Types1_21_5.ITEM_ARRAY, Types1_21_4.ITEM, Types1_21_4.ITEM_ARRAY,
            Types1_21_5.ITEM_COST, Types1_21_5.OPTIONAL_ITEM_COST, Types1_21_4.ITEM_COST, Types1_21_4.OPTIONAL_ITEM_COST
        );
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_21_5> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_21_5.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_21_5.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_21_5.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent1_21(ClientboundPackets1_21_5.LEVEL_EVENT, 2001);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_21_5.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_20_2::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_21_5.BLOCK_ENTITY_DATA);

        protocol.registerClientbound(ClientboundPackets1_21_5.SET_CURSOR_ITEM, this::passthroughClientboundItem);
        registerSetPlayerInventory(ClientboundPackets1_21_5.SET_PLAYER_INVENTORY);
        registerCooldown1_21_2(ClientboundPackets1_21_5.COOLDOWN);
        registerSetContent1_21_2(ClientboundPackets1_21_5.CONTAINER_SET_CONTENT);
        registerSetSlot1_21_2(ClientboundPackets1_21_5.CONTAINER_SET_SLOT);
        registerSetEquipment(ClientboundPackets1_21_5.SET_EQUIPMENT);
        registerMerchantOffers1_20_5(ClientboundPackets1_21_5.MERCHANT_OFFERS);
        registerContainerClick1_21_2(ServerboundPackets1_21_4.CONTAINER_CLICK);
        registerSetCreativeModeSlot(ServerboundPackets1_21_4.SET_CREATIVE_MODE_SLOT);

        registerAdvancements1_20_3(ClientboundPackets1_21_5.UPDATE_ADVANCEMENTS);
        protocol.appendClientbound(ClientboundPackets1_21_5.UPDATE_ADVANCEMENTS, wrapper -> {
            wrapper.passthrough(Types.STRING_ARRAY); // Removed
            final int progressSize = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < progressSize; i++) {
                wrapper.passthrough(Types.STRING); // Key

                final int criterionSize = wrapper.passthrough(Types.VAR_INT);
                for (int j = 0; j < criterionSize; j++) {
                    wrapper.passthrough(Types.STRING); // Key
                    wrapper.passthrough(Types.OPTIONAL_LONG); // Obtained instant
                }
            }

            wrapper.read(Types.BOOLEAN); // Show advancements
        });

        final RecipeDisplayRewriter<ClientboundPacket1_21_5> recipeRewriter = new RecipeDisplayRewriter<>(protocol);
        recipeRewriter.registerUpdateRecipes(ClientboundPackets1_21_5.UPDATE_RECIPES);
        recipeRewriter.registerRecipeBookAdd(ClientboundPackets1_21_5.RECIPE_BOOK_ADD);
        recipeRewriter.registerPlaceGhostRecipe(ClientboundPackets1_21_5.PLACE_GHOST_RECIPE);
    }
}
