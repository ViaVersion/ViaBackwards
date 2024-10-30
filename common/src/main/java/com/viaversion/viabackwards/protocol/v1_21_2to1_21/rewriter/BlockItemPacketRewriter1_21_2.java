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
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.RecipeStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.item.Item;
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
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.SoundRewriter;
import com.viaversion.viaversion.util.Key;

import static com.viaversion.viaversion.protocols.v1_21to1_21_2.rewriter.BlockItemPacketRewriter1_21_2.downgradeItemData;
import static com.viaversion.viaversion.protocols.v1_21to1_21_2.rewriter.BlockItemPacketRewriter1_21_2.updateItemData;

public final class BlockItemPacketRewriter1_21_2 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21_2, ServerboundPacket1_20_5, Protocol1_21_2To1_21> {

    public BlockItemPacketRewriter1_21_2(final Protocol1_21_2To1_21 protocol) {
        super(protocol,
            Types1_21_2.ITEM, Types1_21_2.ITEM_ARRAY, Types1_21.ITEM, Types1_21.ITEM_ARRAY,
            Types1_21_2.ITEM_COST, Types1_21_2.OPTIONAL_ITEM_COST, Types1_21.ITEM_COST, Types1_21.OPTIONAL_ITEM_COST
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
        protocol.registerServerbound(ServerboundPackets1_20_5.CONTAINER_CLOSE, this::updateContainerIdServerbound);
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
            wrapper.write(Types.UNSIGNED_BYTE, (short) -2); // Player inventory
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
            protocol.getParticleRewriter().rewriteParticle(wrapper.user(), explosionParticle);
            // As small and large explosion particle
            wrapper.write(Types1_21_2.PARTICLE, explosionParticle);
            wrapper.write(Types1_21_2.PARTICLE, explosionParticle);

            new SoundRewriter<>(protocol).soundHolderHandler().handle(wrapper);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.RECIPE_BOOK_ADD, null, wrapper -> {
            final RecipeStorage recipeStorage = wrapper.user().get(RecipeStorage.class);
            final int size = wrapper.read(Types.VAR_INT);
            for (int i = 0; i < size; i++) {
                recipeStorage.readRecipe(wrapper);
            }

            final boolean replace = wrapper.read(Types.BOOLEAN);
            if (replace) {
                recipeStorage.clearRecipes();
            }

            recipeStorage.sendRecipes(wrapper.user());
            wrapper.cancel();
        });
        protocol.registerClientbound(ClientboundPackets1_21_2.RECIPE_BOOK_REMOVE, ClientboundPackets1_21.RECIPE, wrapper -> {
            final RecipeStorage recipeStorage = wrapper.user().get(RecipeStorage.class);
            final int[] ids = wrapper.read(Types.VAR_INT_ARRAY_PRIMITIVE);
            recipeStorage.lockRecipes(wrapper, ids);
        });
        protocol.registerClientbound(ClientboundPackets1_21_2.RECIPE_BOOK_SETTINGS, null, wrapper -> {
            final RecipeStorage recipeStorage = wrapper.user().get(RecipeStorage.class);
            final boolean[] settings = new boolean[RecipeStorage.RECIPE_BOOK_SETTINGS];
            for (int i = 0; i < RecipeStorage.RECIPE_BOOK_SETTINGS; i++) {
                settings[i] = wrapper.read(Types.BOOLEAN);
            }
            recipeStorage.setRecipeBookSettings(settings);

            wrapper.cancel();
        });
        protocol.registerClientbound(ClientboundPackets1_21_2.UPDATE_RECIPES, wrapper -> {
            // Inputs for furnaces etc. Old clients get these from the full recipes
            final int size = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < size; i++) {
                wrapper.read(Types.STRING); // Recipe group
                wrapper.read(Types.VAR_INT_ARRAY_PRIMITIVE); // Items
            }

            final RecipeStorage recipeStorage = wrapper.user().get(RecipeStorage.class);
            recipeStorage.readStoneCutterRecipes(wrapper);

            // Send later with the recipe book init
            wrapper.cancel();
        });
        protocol.registerClientbound(ClientboundPackets1_21_2.PLACE_GHOST_RECIPE, wrapper -> {
            this.updateContainerId(wrapper);
            wrapper.cancel(); // Full recipe display, this doesn't look mappable
        });
        protocol.registerServerbound(ServerboundPackets1_20_5.PLACE_RECIPE, wrapper -> {
            this.updateContainerIdServerbound(wrapper);

            final String recipe = Key.stripMinecraftNamespace(wrapper.read(Types.STRING));
            wrapper.write(Types.VAR_INT, Integer.parseInt(recipe));
        });
        protocol.registerServerbound(ServerboundPackets1_20_5.RECIPE_BOOK_SEEN_RECIPE, wrapper -> {
            final String recipe = Key.stripMinecraftNamespace(wrapper.read(Types.STRING));
            wrapper.write(Types.VAR_INT, Integer.parseInt(recipe));
        });
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
