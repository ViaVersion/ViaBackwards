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
package com.viaversion.viabackwards.protocol.v1_22to1_21_5.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v1_22to1_21_5.Protocol1_22To1_21_5;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.AttributeModifiers1_21;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_21_5;
import com.viaversion.viaversion.api.type.types.version.Types1_21_5;
import com.viaversion.viaversion.api.type.types.version.Types1_22;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.RecipeDisplayRewriter1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.packet.ClientboundPacket1_22;
import com.viaversion.viaversion.protocols.v1_21_5to1_22.packet.ClientboundPackets1_22;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;

import static com.viaversion.viaversion.protocols.v1_21_5to1_22.rewriter.BlockItemPacketRewriter1_22.downgradeItemData;
import static com.viaversion.viaversion.protocols.v1_21_5to1_22.rewriter.BlockItemPacketRewriter1_22.updateItemData;

public final class BlockItemPacketRewriter1_22 extends BackwardsStructuredItemRewriter<ClientboundPacket1_22, ServerboundPacket1_21_5, Protocol1_22To1_21_5> {

    public BlockItemPacketRewriter1_22(final Protocol1_22To1_21_5 protocol) {
        super(protocol,
            Types1_22.ITEM, Types1_22.ITEM_ARRAY, Types1_21_5.ITEM, Types1_21_5.ITEM_ARRAY,
            Types1_22.ITEM_COST, Types1_22.OPTIONAL_ITEM_COST, Types1_21_5.ITEM_COST, Types1_21_5.OPTIONAL_ITEM_COST
        );
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_22> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_22.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_22.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_22.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent1_21(ClientboundPackets1_22.LEVEL_EVENT, 2001);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_22.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_21_5::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_22.BLOCK_ENTITY_DATA);

        protocol.registerClientbound(ClientboundPackets1_22.SET_CURSOR_ITEM, this::passthroughClientboundItem);
        registerSetPlayerInventory(ClientboundPackets1_22.SET_PLAYER_INVENTORY);
        registerCooldown1_21_2(ClientboundPackets1_22.COOLDOWN);
        registerSetContent1_21_2(ClientboundPackets1_22.CONTAINER_SET_CONTENT);
        registerSetSlot1_21_2(ClientboundPackets1_22.CONTAINER_SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_22.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets1_22.SET_EQUIPMENT);
        registerMerchantOffers1_20_5(ClientboundPackets1_22.MERCHANT_OFFERS);
        registerContainerClick1_21_5(ServerboundPackets1_21_5.CONTAINER_CLICK);
        registerSetCreativeModeSlot1_21_5(ServerboundPackets1_21_5.SET_CREATIVE_MODE_SLOT, Types1_22.LENGTH_PREFIXED_ITEM, Types1_21_5.LENGTH_PREFIXED_ITEM);

        final RecipeDisplayRewriter<ClientboundPacket1_22> recipeRewriter = new RecipeDisplayRewriter1_21_5<>(protocol);
        recipeRewriter.registerUpdateRecipes(ClientboundPackets1_22.UPDATE_RECIPES);
        recipeRewriter.registerRecipeBookAdd(ClientboundPackets1_22.RECIPE_BOOK_ADD);
        recipeRewriter.registerPlaceGhostRecipe(ClientboundPackets1_22.PLACE_GHOST_RECIPE);
    }

    @Override
    public Item handleItemToClient(final UserConnection connection, final Item item) {
        super.handleItemToClient(connection, item);

        final StructuredDataContainer dataContainer = item.dataContainer();
        final CompoundTag backupTag = new CompoundTag();

        final AttributeModifiers1_21 attributeModifiers = dataContainer.get(StructuredDataKey.ATTRIBUTE_MODIFIERS1_22);
        if (attributeModifiers != null && attributeModifiers.display().id() != 0) {
            backupTag.putInt("attribute_modifiers_display", attributeModifiers.display().id());
            if (attributeModifiers.display() instanceof AttributeModifiers1_21.OverrideText overrideText) {
                backupTag.put("attribute_modifiers_display_text", overrideText.component());
            }
        }

        if (!backupTag.isEmpty()) {
            saveTag(createCustomTag(item), backupTag, "backup");
        }

        downgradeItemData(item);
        return item;
    }

    @Override
    public Item handleItemToServer(final UserConnection connection, final Item item) {
        super.handleItemToServer(connection, item);
        restoreData(item.dataContainer());
        updateItemData(item);
        return item;
    }

    private void restoreData(final StructuredDataContainer data) {
        final CompoundTag customData = data.get(StructuredDataKey.CUSTOM_DATA);
        if (customData == null || !(customData.remove(nbtTagName("backup")) instanceof final CompoundTag backupTag)) {
            return;
        }

        final int attributeModifiersDisplay = backupTag.getInt("attribute_modifiers_display");
        if (attributeModifiersDisplay != 0) {
            data.replace(StructuredDataKey.ATTRIBUTE_MODIFIERS1_21_5, StructuredDataKey.ATTRIBUTE_MODIFIERS1_22, modifiers -> {
                if (attributeModifiersDisplay == 2) {
                    return new AttributeModifiers1_21(modifiers.modifiers(), new AttributeModifiers1_21.OverrideText(backupTag.get("attribute_modifiers_display_text")));
                }
                return new AttributeModifiers1_21(modifiers.modifiers(), new AttributeModifiers1_21.Display(attributeModifiersDisplay));
            });
        }

        removeCustomTag(data, customData);
    }
}
