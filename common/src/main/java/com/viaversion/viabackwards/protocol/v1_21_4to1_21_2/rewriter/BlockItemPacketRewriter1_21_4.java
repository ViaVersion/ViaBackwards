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
package com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.rewriter;

import com.viaversion.nbt.tag.ByteArrayTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v1_21_4to1_21_2.Protocol1_21_4To1_21_2;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.CustomModelData1_21_4;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_21_2;
import com.viaversion.viaversion.api.type.types.version.Types1_21_4;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ServerboundPackets1_21_2;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;

import static com.viaversion.viaversion.protocols.v1_21_2to1_21_4.rewriter.BlockItemPacketRewriter1_21_4.downgradeItemData;
import static com.viaversion.viaversion.protocols.v1_21_2to1_21_4.rewriter.BlockItemPacketRewriter1_21_4.updateItemData;

public final class BlockItemPacketRewriter1_21_4 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21_2, ServerboundPacket1_21_2, Protocol1_21_4To1_21_2> {

    public BlockItemPacketRewriter1_21_4(final Protocol1_21_4To1_21_2 protocol) {
        super(protocol,
            Types1_21_4.ITEM, Types1_21_4.ITEM_ARRAY, Types1_21_2.ITEM, Types1_21_2.ITEM_ARRAY,
            Types1_21_4.ITEM_COST, Types1_21_4.OPTIONAL_ITEM_COST, Types1_21_2.ITEM_COST, Types1_21_2.OPTIONAL_ITEM_COST
        );
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

        protocol.registerClientbound(ClientboundPackets1_21_2.SET_HELD_SLOT, wrapper -> {
            final int slot = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.BYTE, (byte) slot);
        });

        protocol.cancelServerbound(ServerboundPackets1_21_2.PICK_ITEM);

        protocol.registerClientbound(ClientboundPackets1_21_2.SET_CURSOR_ITEM, this::passthroughClientboundItem);
        registerSetPlayerInventory(ClientboundPackets1_21_2.SET_PLAYER_INVENTORY);
        registerCooldown1_21_2(ClientboundPackets1_21_2.COOLDOWN);
        registerSetContent1_21_2(ClientboundPackets1_21_2.CONTAINER_SET_CONTENT);
        registerSetSlot1_21_2(ClientboundPackets1_21_2.CONTAINER_SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_21_2.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets1_21_2.SET_EQUIPMENT);
        registerMerchantOffers1_20_5(ClientboundPackets1_21_2.MERCHANT_OFFERS);
        registerContainerClick1_21_2(ServerboundPackets1_21_2.CONTAINER_CLICK);
        registerSetCreativeModeSlot(ServerboundPackets1_21_2.SET_CREATIVE_MODE_SLOT);

        final RecipeDisplayRewriter<ClientboundPacket1_21_2> recipeRewriter = new RecipeDisplayRewriter<>(protocol);
        recipeRewriter.registerUpdateRecipes(ClientboundPackets1_21_2.UPDATE_RECIPES);
        recipeRewriter.registerRecipeBookAdd(ClientboundPackets1_21_2.RECIPE_BOOK_ADD);
        recipeRewriter.registerPlaceGhostRecipe(ClientboundPackets1_21_2.PLACE_GHOST_RECIPE);
    }


    @Override
    public Item handleItemToClient(final UserConnection connection, final Item item) {
        super.handleItemToClient(connection, item);

        final StructuredDataContainer dataContainer = item.dataContainer();
        final CustomModelData1_21_4 modelData = dataContainer.get(StructuredDataKey.CUSTOM_MODEL_DATA1_21_4);
        if (modelData != null) {
            saveTag(createCustomTag(item), customModelDataToTag(modelData), "custom_model_data");
            if (ViaBackwards.getConfig().mapCustomModelData() && modelData.floats().length > 0) {
                // Put first float as old custom model data as this is the most common replacement
                final int data = Float.floatToIntBits(modelData.floats()[0]);
                dataContainer.set(StructuredDataKey.CUSTOM_MODEL_DATA1_20_5, data);
            }
        }

        downgradeItemData(item);
        return item;
    }

    @Override
    public Item handleItemToServer(final UserConnection connection, final Item item) {
        super.handleItemToServer(connection, item);

        final StructuredDataContainer dataContainer = item.dataContainer();
        final CompoundTag customData = dataContainer.get(StructuredDataKey.CUSTOM_DATA);
        if (customData != null) {
            if (customData.remove(nbtTagName("custom_model_data")) instanceof final CompoundTag customModelData) {
                dataContainer.set(StructuredDataKey.CUSTOM_MODEL_DATA1_21_4, customModelDataFromTag(customModelData));
                removeCustomTag(dataContainer, customData);
            }
        }

        updateItemData(item);
        return item;
    }

    private CustomModelData1_21_4 customModelDataFromTag(final CompoundTag tag) {
        final IntArrayTag floatsTag = tag.getIntArrayTag("floats");
        final float[] floats = new float[floatsTag.getValue().length];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = Float.intBitsToFloat(floatsTag.get(i));
        }

        final ByteArrayTag booleansTag = tag.getByteArrayTag("booleans");
        final boolean[] booleans = new boolean[booleansTag.getValue().length];
        for (int i = 0; i < booleans.length; i++) {
            booleans[i] = booleansTag.get(i) != 0;
        }

        final ListTag<StringTag> stringsTag = tag.getListTag("strings", StringTag.class);
        final String[] strings = new String[stringsTag.size()];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = stringsTag.get(i).getValue();
        }

        final IntArrayTag colorsTag = tag.getIntArrayTag("colors");
        return new CustomModelData1_21_4(floats, booleans, strings, colorsTag.getValue());
    }

    private CompoundTag customModelDataToTag(final CustomModelData1_21_4 customModelData) {
        final CompoundTag tag = new CompoundTag();
        final int[] floats = new int[customModelData.floats().length];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = Float.floatToIntBits(customModelData.floats()[i]);
        }
        tag.put("floats", new IntArrayTag(floats));

        final byte[] booleans = new byte[customModelData.booleans().length];
        for (int i = 0; i < booleans.length; i++) {
            booleans[i] = (byte) (customModelData.booleans()[i] ? 1 : 0);
        }
        tag.put("booleans", new ByteArrayTag(booleans));

        final ListTag<StringTag> strings = new ListTag<>(StringTag.class);
        for (final String string : customModelData.strings()) {
            strings.add(new StringTag(string));
        }
        tag.put("strings", strings);

        tag.put("colors", new IntArrayTag(customModelData.colors()));
        return tag;
    }
}
