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
package com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.Protocol1_21_9To1_21_7;
import com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.storage.DimensionScaleStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.ResolvableProfile;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.RecipeDisplayRewriter1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPacket1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ServerboundPackets1_21_6;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPacket1_21_9;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPackets1_21_9;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;

import static com.viaversion.viaversion.protocols.v1_21_7to1_21_9.rewriter.BlockItemPacketRewriter1_21_9.downgradeData;
import static com.viaversion.viaversion.protocols.v1_21_7to1_21_9.rewriter.BlockItemPacketRewriter1_21_9.upgradeData;

public final class BlockItemPacketRewriter1_21_9 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21_9, ServerboundPacket1_21_6, Protocol1_21_9To1_21_7> {
    private static final int SIGN_BOCK_ENTITY_ID = 7;
    private static final int HANGING_SIGN_BOCK_ENTITY_ID = 8;

    public BlockItemPacketRewriter1_21_9(final Protocol1_21_9To1_21_7 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_21_9> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_21_9.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_21_9.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_21_9.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent1_21(ClientboundPackets1_21_9.LEVEL_EVENT, 2001);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_21_9.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_21_5::new, this::handleBlockEntity);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_21_9.BLOCK_ENTITY_DATA, this::handleBlockEntity);

        registerSetCursorItem(ClientboundPackets1_21_9.SET_CURSOR_ITEM);
        registerSetPlayerInventory(ClientboundPackets1_21_9.SET_PLAYER_INVENTORY);
        registerCooldown1_21_2(ClientboundPackets1_21_9.COOLDOWN);
        registerSetContent1_21_2(ClientboundPackets1_21_9.CONTAINER_SET_CONTENT);
        registerSetSlot1_21_2(ClientboundPackets1_21_9.CONTAINER_SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_21_9.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets1_21_9.SET_EQUIPMENT);
        registerMerchantOffers1_20_5(ClientboundPackets1_21_9.MERCHANT_OFFERS);
        registerContainerClick1_21_5(ServerboundPackets1_21_6.CONTAINER_CLICK);
        registerSetCreativeModeSlot1_21_5(ServerboundPackets1_21_6.SET_CREATIVE_MODE_SLOT);

        final RecipeDisplayRewriter<ClientboundPacket1_21_9> recipeRewriter = new RecipeDisplayRewriter1_21_5<>(protocol);
        recipeRewriter.registerUpdateRecipes(ClientboundPackets1_21_9.UPDATE_RECIPES);
        recipeRewriter.registerRecipeBookAdd(ClientboundPackets1_21_9.RECIPE_BOOK_ADD);
        recipeRewriter.registerPlaceGhostRecipe(ClientboundPackets1_21_9.PLACE_GHOST_RECIPE);

        protocol.registerClientbound(ClientboundPackets1_21_9.INITIALIZE_BORDER, this::updateBorderCenter);
        protocol.registerClientbound(ClientboundPackets1_21_9.SET_BORDER_CENTER, this::updateBorderCenter);
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
    protected void backupInconvertibleData(final UserConnection connection, final Item item, final StructuredDataContainer dataContainer, final CompoundTag backupTag) {
        super.backupInconvertibleData(connection, item, dataContainer, backupTag);
        final ResolvableProfile profile = dataContainer.get(StructuredDataKey.PROFILE1_21_9);
        if (profile != null) {
            final CompoundTag profileTag = new CompoundTag();
            if (profile.bodyTexture() != null) {
                profileTag.putString("body_texture", profile.bodyTexture());
            }
            if (profile.capeTexture() != null) {
                profileTag.putString("cape_texture", profile.capeTexture());
            }
            if (profile.elytraTexture() != null) {
                profileTag.putString("elytra_texture", profile.elytraTexture());
            }
            if (profile.modelType() != null) {
                profileTag.putBoolean("model", profile.modelType() == 0);
            }
            if (!profileTag.isEmpty()) {
                backupTag.put("profile", profileTag);
            }
        }
    }

    @Override
    protected void restoreBackupData(final Item item, final StructuredDataContainer container, final CompoundTag customData) {
        super.restoreBackupData(item, container, customData);
        if (!(customData.remove(nbtTagName("backup")) instanceof final CompoundTag backupTag)) {
            return;
        }
        final CompoundTag profileTag = backupTag.getCompoundTag("profile");
        if (profileTag != null) {
            container.replace(StructuredDataKey.PROFILE1_20_5, StructuredDataKey.PROFILE1_21_9, profile -> new ResolvableProfile(
                profile,
                profileTag.getString("body_texture"),
                profileTag.getString("cape_texture"),
                profileTag.getString("elytra_texture"),
                profileTag.contains("model") ? (profileTag.getBoolean("model") ? 0 : 1) : null
            ));
        }
    }

    private void handleBlockEntity(final UserConnection connection, final BlockEntity blockEntity) {
        final CompoundTag tag = blockEntity.tag();
        if (tag == null) {
            return;
        }

        if (blockEntity.typeId() == SIGN_BOCK_ENTITY_ID || blockEntity.typeId() == HANGING_SIGN_BOCK_ENTITY_ID) {
            updateSignMessages(connection, tag.getCompoundTag("front_text"));
            updateSignMessages(connection, tag.getCompoundTag("back_text"));
        }
    }

    private void updateSignMessages(final UserConnection connection, final CompoundTag tag) {
        if (tag == null) {
            return;
        }

        final ListTag<?> messages = tag.getListTag("messages");
        protocol.getComponentRewriter().processTag(connection, messages);
        final ListTag<?> filteredMessages = tag.getListTag("filtered_messages");
        if (filteredMessages != null) {
            protocol.getComponentRewriter().processTag(connection, filteredMessages);
        }
    }

    private void updateBorderCenter(final PacketWrapper wrapper) {
        double centerX = wrapper.read(Types.DOUBLE);
        double centerZ = wrapper.read(Types.DOUBLE);

        final EntityTracker tracker = protocol.getEntityRewriter().tracker(wrapper.user());
        if (tracker.currentDimensionId() != -1) {
            final double scale = wrapper.user().get(DimensionScaleStorage.class).getScale(tracker.currentDimensionId());
            centerX *= scale;
            centerZ *= scale;
        }

        wrapper.write(Types.DOUBLE, centerX);
        wrapper.write(Types.DOUBLE, centerZ);
    }
}
