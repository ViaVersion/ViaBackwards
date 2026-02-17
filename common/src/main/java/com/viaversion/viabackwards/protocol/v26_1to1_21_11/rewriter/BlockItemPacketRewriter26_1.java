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
package com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.Protocol26_1To1_21_11;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_21_5;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.RecipeDisplayRewriter1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundPacket1_21_9;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;
import com.viaversion.viaversion.rewriter.block.BlockRewriter1_21_5;

import static com.viaversion.viaversion.protocols.v1_21_11to26_1.rewriter.BlockItemPacketRewriter26_1.downgradeData;
import static com.viaversion.viaversion.protocols.v1_21_11to26_1.rewriter.BlockItemPacketRewriter26_1.upgradeData;

public final class BlockItemPacketRewriter26_1 extends BackwardsStructuredItemRewriter<ClientboundPacket26_1, ServerboundPacket1_21_9, Protocol26_1To1_21_11> {

    public BlockItemPacketRewriter26_1(final Protocol26_1To1_21_11 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket26_1> blockRewriter = new BlockRewriter1_21_5<>(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets26_1.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets26_1.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets26_1.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent1_21(ClientboundPackets26_1.LEVEL_EVENT, 2001);
        blockRewriter.registerBlockEntityData(ClientboundPackets26_1.BLOCK_ENTITY_DATA);
        protocol.registerClientbound(ClientboundPackets26_1.LEVEL_CHUNK_WITH_LIGHT, wrapper -> {
            final Chunk chunk = blockRewriter.handleChunk1_19(wrapper, ChunkType26_1::new, ChunkType1_21_5::new);
            blockRewriter.handleBlockEntities(chunk, wrapper.user());
        });

        registerSetCursorItem(ClientboundPackets26_1.SET_CURSOR_ITEM);
        registerSetPlayerInventory(ClientboundPackets26_1.SET_PLAYER_INVENTORY);
        registerCooldown1_21_2(ClientboundPackets26_1.COOLDOWN);
        registerSetContent1_21_2(ClientboundPackets26_1.CONTAINER_SET_CONTENT);
        registerSetSlot1_21_2(ClientboundPackets26_1.CONTAINER_SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets26_1.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets26_1.SET_EQUIPMENT);
        registerMerchantOffers1_20_5(ClientboundPackets26_1.MERCHANT_OFFERS);
        registerContainerClick1_21_5(ServerboundPackets1_21_6.CONTAINER_CLICK);
        registerSetCreativeModeSlot1_21_5(ServerboundPackets1_21_6.SET_CREATIVE_MODE_SLOT);

        final RecipeDisplayRewriter<ClientboundPacket26_1> recipeRewriter = new RecipeDisplayRewriter1_21_5<>(protocol) {
            @Override
            protected void handleDyeSlotDisplay(final PacketWrapper wrapper) {
                wrapper.consumeReadsOnly(() -> super.handleDyeSlotDisplay(wrapper));
            }

            @Override
            protected void handleOnlyWithComponentSlotDisplay(final PacketWrapper wrapper) {
                wrapper.consumeReadsOnly(() -> super.handleOnlyWithComponentSlotDisplay(wrapper));
            }

            @Override
            protected void handleWithRemainderSlotDisplay(final PacketWrapper wrapper) {
                wrapper.consumeReadsOnly(() -> super.handleWithRemainderSlotDisplay(wrapper));
            }
        };
        recipeRewriter.registerUpdateRecipes(ClientboundPackets26_1.UPDATE_RECIPES);
        recipeRewriter.registerRecipeBookAdd(ClientboundPackets26_1.RECIPE_BOOK_ADD);
        recipeRewriter.registerPlaceGhostRecipe(ClientboundPackets26_1.PLACE_GHOST_RECIPE);
    }

    @Override
    protected void handleItemDataComponentsToClient(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToClient(connection, item, container);
        downgradeData(item, container);
    }

    @Override
    protected void handleItemDataComponentsToServer(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToServer(connection, item, container);
        upgradeData(item, container);
    }

    @Override
    protected void restoreBackupData(final Item item, final StructuredDataContainer container, final CompoundTag customData) {
        super.restoreBackupData(item, container, customData);

    }

    @Override
    protected void backupInconvertibleData(final UserConnection connection, final Item item, final StructuredDataContainer dataContainer, final CompoundTag backupTag) {
        super.backupInconvertibleData(connection, item, dataContainer, backupTag);

    }
}
