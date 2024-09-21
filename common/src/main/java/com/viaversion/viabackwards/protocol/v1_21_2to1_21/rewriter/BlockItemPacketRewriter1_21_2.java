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
package com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.InventoryStateIdStorage;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.ItemTagStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.StructuredItem;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_21;
import com.viaversion.viaversion.api.type.types.version.Types1_21_2;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.rewriter.RecipeRewriter1_21_2;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.SoundRewriter;

import static com.viaversion.viaversion.protocols.v1_21to1_21_2.rewriter.BlockItemPacketRewriter1_21_2.downgradeItemData;
import static com.viaversion.viaversion.protocols.v1_21to1_21_2.rewriter.BlockItemPacketRewriter1_21_2.updateItemData;

public final class BlockItemPacketRewriter1_21_2 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21_2, ServerboundPacket1_20_5, Protocol1_21_2To1_21> {

    public BlockItemPacketRewriter1_21_2(final Protocol1_21_2To1_21 protocol) {
        super(protocol,
            Types1_21_2.ITEM, Types1_21_2.ITEM_ARRAY, Types1_21.ITEM, Types1_21.ITEM_ARRAY,
            Types1_21_2.ITEM_COST, Types1_21_2.OPTIONAL_ITEM_COST, Types1_21.ITEM_COST, Types1_21.OPTIONAL_ITEM_COST,
            Types1_21_2.PARTICLE, Types1_21.PARTICLE
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

        registerAdvancements1_20_3(ClientboundPackets1_21_2.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets1_21_2.SET_EQUIPMENT);
        registerMerchantOffers1_20_5(ClientboundPackets1_21_2.MERCHANT_OFFERS);
        registerSetCreativeModeSlot(ServerboundPackets1_20_5.SET_CREATIVE_MODE_SLOT);
        registerLevelParticles1_20_5(ClientboundPackets1_21_2.LEVEL_PARTICLES);

        protocol.registerClientbound(ClientboundPackets1_21_2.COOLDOWN, wrapper -> {
            final MappingData mappingData = protocol.getMappingData();
            final String itemIdentifier = wrapper.read(Types.STRING);
            final int id = mappingData.getFullItemMappings().id(itemIdentifier);
            if (id != -1) {
                final int mappedId = mappingData.getFullItemMappings().getNewId(id);
                wrapper.write(Types.VAR_INT, mappedId);
            } else {
                wrapper.cancel();
            }
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.SET_CURSOR_ITEM, ClientboundPackets1_21.CONTAINER_SET_SLOT, wrapper -> {
            wrapper.write(Types.BYTE, (byte) -1); // Player inventory
            wrapper.write(Types.VAR_INT, wrapper.user().get(InventoryStateIdStorage.class).stateId()); // State id; re-use the last known one
            wrapper.write(Types.SHORT, (short) -1); // Cursor
            final Item item = wrapper.passthrough(Types1_21_2.ITEM);
            handleItemToClient(wrapper.user(), item);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.CONTAINER_SET_CONTENT, wrapper -> {
            updateContainerId(wrapper);

            final int stateId = wrapper.passthrough(Types.VAR_INT);
            wrapper.user().get(InventoryStateIdStorage.class).setStateId(stateId);

            final Item[] items = wrapper.read(itemArrayType());
            wrapper.write(mappedItemArrayType(), items);
            for (int i = 0; i < items.length; i++) {
                items[i] = handleItemToClient(wrapper.user(), items[i]);
            }
            passthroughClientboundItem(wrapper);
        });
        protocol.registerClientbound(ClientboundPackets1_21_2.CONTAINER_SET_SLOT, wrapper -> {
            updateContainerId(wrapper);

            final int stateId = wrapper.passthrough(Types.VAR_INT);
            wrapper.user().get(InventoryStateIdStorage.class).setStateId(stateId);

            wrapper.passthrough(Types.SHORT); // Slot id
            passthroughClientboundItem(wrapper);
        });
        protocol.registerClientbound(ClientboundPackets1_21_2.SET_HELD_SLOT, ClientboundPackets1_21.SET_CARRIED_ITEM);
        protocol.registerClientbound(ClientboundPackets1_21_2.CONTAINER_CLOSE, this::updateContainerId);
        protocol.registerClientbound(ClientboundPackets1_21_2.CONTAINER_SET_DATA, this::updateContainerId);
        protocol.registerClientbound(ClientboundPackets1_21_2.HORSE_SCREEN_OPEN, this::updateContainerId);
        protocol.registerClientbound(ClientboundPackets1_21_2.PLACE_GHOST_RECIPE, this::updateContainerId);
        protocol.registerServerbound(ServerboundPackets1_20_5.CONTAINER_CLOSE, this::updateContainerIdServerbound);
        protocol.registerServerbound(ServerboundPackets1_20_5.PLACE_RECIPE, this::updateContainerIdServerbound);
        protocol.registerServerbound(ServerboundPackets1_20_5.CONTAINER_CLICK, wrapper -> {
            updateContainerIdServerbound(wrapper);
            wrapper.passthrough(Types.VAR_INT); // State id
            wrapper.passthrough(Types.SHORT); // Slot
            wrapper.passthrough(Types.BYTE); // Button
            wrapper.passthrough(Types.VAR_INT); // Mode
            final int length = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < length; i++) {
                wrapper.passthrough(Types.SHORT); // Slot
                passthroughServerboundItem(wrapper);
            }
            passthroughServerboundItem(wrapper);
        });

        protocol.registerServerbound(ServerboundPackets1_20_5.USE_ITEM_ON, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Hand
            wrapper.passthrough(Types.BLOCK_POSITION1_14); // Block position
            wrapper.passthrough(Types.VAR_INT); // Direction
            wrapper.passthrough(Types.FLOAT); // X
            wrapper.passthrough(Types.FLOAT); // Y
            wrapper.passthrough(Types.FLOAT); // Z
            wrapper.passthrough(Types.BOOLEAN); // Inside
            wrapper.write(Types.BOOLEAN, false); // World border hit
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.SET_PLAYER_INVENTORY, ClientboundPackets1_21.CONTAINER_SET_SLOT, wrapper -> {
            wrapper.write(Types.BYTE, (byte) -2); // Player inventory
            wrapper.write(Types.VAR_INT, 0); // 0 state id
            final int slot = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.SHORT, (short) slot);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.EXPLODE, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // Center X
            wrapper.passthrough(Types.DOUBLE); // Center Y
            wrapper.passthrough(Types.DOUBLE); // Center Z

            // The server will already send block changes separately
            wrapper.write(Types.FLOAT, 0F); // Power
            wrapper.write(Types.VAR_INT, 0); // No blocks affected

            double knockbackX = 0;
            double knockbackY = 0;
            double knockbackZ = 0;
            if (wrapper.read(Types.BOOLEAN)) {
                knockbackX = wrapper.read(Types.DOUBLE);
                knockbackY = wrapper.read(Types.DOUBLE);
                knockbackZ = wrapper.read(Types.DOUBLE);
            }
            wrapper.write(Types.FLOAT, (float) knockbackX);
            wrapper.write(Types.FLOAT, (float) knockbackY);
            wrapper.write(Types.FLOAT, (float) knockbackZ);

            wrapper.write(Types.VAR_INT, 0); // Block interaction type

            final Particle explosionParticle = wrapper.read(Types1_21.PARTICLE);
            rewriteParticle(wrapper.user(), explosionParticle);
            // As small and large explosion particle
            wrapper.write(Types1_21_2.PARTICLE, explosionParticle);
            wrapper.write(Types1_21_2.PARTICLE, explosionParticle);

            new SoundRewriter<>(protocol).soundHolderHandler().handle(wrapper);
        });

        new RecipeRewriter1_21_2<>(protocol) {
            @Override
            protected void handleIngredient(final PacketWrapper wrapper) {
                wrapper.write(mappedItemArrayType(), ingredient(wrapper));
            }

            @Override
            public void handleCraftingShaped(final PacketWrapper wrapper) {
                wrapper.passthrough(Types.STRING); // Group
                wrapper.passthrough(Types.VAR_INT); // Crafting book category
                wrapper.passthrough(Types.VAR_INT); // Width
                wrapper.passthrough(Types.VAR_INT); // Height

                final int ingredients = wrapper.read(Types.VAR_INT);
                for (int i = 0; i < ingredients; i++) {
                    wrapper.write(mappedItemArrayType(), ingredient(wrapper));
                }

                wrapper.write(mappedItemType(), rewrite(wrapper.user(), wrapper.read(itemType()))); // Result
                wrapper.passthrough(Types.BOOLEAN); // Show notification
            }

            @Override
            public void handleCraftingShapeless(final PacketWrapper wrapper) {
                wrapper.passthrough(Types.STRING); // Group
                wrapper.passthrough(Types.VAR_INT); // Crafting book category

                // Move below
                final Item result = rewrite(wrapper.user(), wrapper.read(itemType()));

                final int ingredients = wrapper.passthrough(Types.VAR_INT);
                for (int i = 0; i < ingredients; i++) {
                    wrapper.write(mappedItemArrayType(), ingredient(wrapper));
                }

                wrapper.write(mappedItemType(), result);
            }

            private Item[] ingredient(final PacketWrapper wrapper) {
                final HolderSet ingredient = wrapper.read(Types.HOLDER_SET).rewrite(id -> protocol.getMappingData().getNewItemId(id));
                if (ingredient.hasTagKey()) {
                    final ItemTagStorage tagStorage = wrapper.user().get(ItemTagStorage.class);
                    final int[] tagEntries = tagStorage.itemTag(ingredient.tagKey());
                    if (tagEntries == null || tagEntries.length == 0) {
                        // Most cannot be empty; add a dummy ingredient, though this would only come from bad data
                        return new Item[]{new StructuredItem(1, 1)};
                    }

                    final Item[] items = new Item[tagEntries.length];
                    for (int i = 0; i < tagEntries.length; i++) {
                        items[i] = new StructuredItem(tagEntries[i], 1);
                    }
                    return items;
                }

                final int[] ids = ingredient.ids();
                final Item[] items = new Item[ids.length];
                for (int i = 0; i < ids.length; i++) {
                    items[i] = new StructuredItem(ids[i], 1);
                }
                return items;
            }

            @Override
            public void handleRecipeType(final PacketWrapper wrapper, final String type) {
                if (type.equals("crafting_transmute")) {
                    wrapper.read(Types.STRING); // Group
                    wrapper.read(Types.VAR_INT); // Crafting book category
                    wrapper.read(Types.HOLDER_SET); // Input
                    wrapper.read(Types.HOLDER_SET); // Material
                    wrapper.read(Types.VAR_INT); // Result item ID
                } else {
                    super.handleRecipeType(wrapper, type);
                }
            }
        }.register1_20_5(ClientboundPackets1_21_2.UPDATE_RECIPES);
    }

    private void updateContainerId(final PacketWrapper wrapper) {
        final int containerId = wrapper.read(Types.VAR_INT);
        wrapper.write(Types.UNSIGNED_BYTE, (short) containerId);
    }

    private void updateContainerIdServerbound(final PacketWrapper wrapper) {
        final short containerId = wrapper.read(Types.UNSIGNED_BYTE);
        final int intId = (byte) containerId;
        wrapper.write(Types.VAR_INT, intId);
    }

    @Override
    public Item handleItemToClient(final UserConnection connection, final Item item) {
        super.handleItemToClient(connection, item);
        downgradeItemData(item);
        return item;
    }

    @Override
    public Item handleItemToServer(final UserConnection connection, final Item item) {
        super.handleItemToServer(connection, item);
        updateItemData(item);
        return item;
    }
}
