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
package com.viaversion.viabackwards.protocol.template;

import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;

// To replace if needed:
//   ChunkType1_20_2
//   RecipeRewriter1_20_3
//   Types1_21
final class BlockItemPacketRewriter1_99 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21_2, ServerboundPacket1_21_2, Protocol1_98To1_99> {

    public BlockItemPacketRewriter1_99(final Protocol1_98To1_99 protocol) {
        super(protocol, Types1_21_2.ITEM, Types1_21_2.ITEM_ARRAY);
        /*super(protocol,
            Types1_21.ITEM, Types1_21.ITEM_ARRAY, Types1_OLD.ITEM, Types1_OLD.ITEM_ARRAY,
            Types1_21.ITEM_COST, Types1_21.OPTIONAL_ITEM_COST, Types1_OLD.ITEM_COST, Types1_OLD.OPTIONAL_ITEM_COST,
            Types1_21.PARTICLE, Types1_OLD.PARTICLE
        );*/
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_21_2> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_21_2.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_21_2.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_21_2.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent1_21(ClientboundPackets1_21_2.LEVEL_EVENT, 2001);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_21_2.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_20_2::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_21_2.BLOCK_ENTITY_DATA);

        // registerOpenScreen(ClientboundPackets1_21_2.OPEN_SCREEN);
        protocol.registerClientbound(ClientboundPackets1_21_2.SET_CURSOR_ITEM, this::passthroughClientboundItem);
        registerCooldown1_21_2(ClientboundPackets1_21_2.COOLDOWN);
        registerSetContent1_21_2(ClientboundPackets1_21_2.CONTAINER_SET_CONTENT);
        registerSetSlot1_21_2(ClientboundPackets1_21_2.CONTAINER_SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_21_2.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets1_21_2.SET_EQUIPMENT);
        registerMerchantOffers1_20_5(ClientboundPackets1_21_2.MERCHANT_OFFERS);
        registerContainerSetData(ClientboundPackets1_21_2.CONTAINER_SET_DATA);
        registerContainerClick1_21_2(ServerboundPackets1_21_2.CONTAINER_CLICK);
        registerSetCreativeModeSlot(ServerboundPackets1_21_2.SET_CREATIVE_MODE_SLOT);

        final RecipeDisplayRewriter<ClientboundPacket1_21_2> recipeRewriter = new RecipeDisplayRewriter<>(protocol);
        recipeRewriter.registerUpdateRecipes(ClientboundPackets1_21_2.UPDATE_RECIPES);
        recipeRewriter.registerRecipeBookAdd(ClientboundPackets1_21_2.RECIPE_BOOK_ADD);
        recipeRewriter.registerPlaceGhostRecipe(ClientboundPackets1_21_2.PLACE_GHOST_RECIPE);
    }
}
