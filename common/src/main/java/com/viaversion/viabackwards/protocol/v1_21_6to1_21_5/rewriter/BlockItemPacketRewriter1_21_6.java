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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.Protocol1_21_6To1_21_5;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.AttributeModifiers1_21;
import com.viaversion.viaversion.api.minecraft.item.data.Equippable;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ServerboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.RecipeDisplayRewriter1_21_5;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPacket1_21_6;
import com.viaversion.viaversion.protocols.v1_21_5to1_21_6.packet.ClientboundPackets1_21_6;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;

import static com.viaversion.viaversion.protocols.v1_21_5to1_21_6.rewriter.BlockItemPacketRewriter1_21_6.downgradeItemData;
import static com.viaversion.viaversion.protocols.v1_21_5to1_21_6.rewriter.BlockItemPacketRewriter1_21_6.upgradeItemData;

public final class BlockItemPacketRewriter1_21_6 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21_6, ServerboundPacket1_21_5, Protocol1_21_6To1_21_5> {
    private static final int SIGN_BOCK_ENTITY_ID = 7;
    private static final int HANGING_SIGN_BOCK_ENTITY_ID = 8;

    public BlockItemPacketRewriter1_21_6(final Protocol1_21_6To1_21_5 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_21_6> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_21_6.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_21_6.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_21_6.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent1_21(ClientboundPackets1_21_6.LEVEL_EVENT, 2001);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_21_6.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_21_5::new, this::handleBlockEntity);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_21_6.BLOCK_ENTITY_DATA, this::handleBlockEntity);

        protocol.registerClientbound(ClientboundPackets1_21_6.SET_CURSOR_ITEM, this::passthroughClientboundItem);
        registerSetPlayerInventory(ClientboundPackets1_21_6.SET_PLAYER_INVENTORY);
        registerCooldown1_21_2(ClientboundPackets1_21_6.COOLDOWN);
        registerSetContent1_21_2(ClientboundPackets1_21_6.CONTAINER_SET_CONTENT);
        registerSetSlot1_21_2(ClientboundPackets1_21_6.CONTAINER_SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_21_6.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets1_21_6.SET_EQUIPMENT);
        registerMerchantOffers1_20_5(ClientboundPackets1_21_6.MERCHANT_OFFERS);
        registerContainerClick1_21_5(ServerboundPackets1_21_5.CONTAINER_CLICK);
        registerSetCreativeModeSlot1_21_5(ServerboundPackets1_21_5.SET_CREATIVE_MODE_SLOT);

        final RecipeDisplayRewriter<ClientboundPacket1_21_6> recipeRewriter = new RecipeDisplayRewriter1_21_5<>(protocol);
        recipeRewriter.registerUpdateRecipes(ClientboundPackets1_21_6.UPDATE_RECIPES);
        recipeRewriter.registerRecipeBookAdd(ClientboundPackets1_21_6.RECIPE_BOOK_ADD);
        recipeRewriter.registerPlaceGhostRecipe(ClientboundPackets1_21_6.PLACE_GHOST_RECIPE);
    }

    @Override
    protected void handleItemDataComponentsToClient(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        downgradeItemData(item);
        super.handleItemDataComponentsToClient(connection, item, container);
    }

    @Override
    protected void handleItemDataComponentsToServer(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        upgradeItemData(item);
        super.handleItemDataComponentsToServer(connection, item, container);
    }

    @Override
    protected void backupInconvertibleData(final UserConnection connection, final Item item, final StructuredDataContainer dataContainer, final CompoundTag backupTag) {
        super.backupInconvertibleData(connection, item, dataContainer, backupTag);
        final AttributeModifiers1_21 attributeModifiers = dataContainer.get(StructuredDataKey.ATTRIBUTE_MODIFIERS1_21_6);
        if (attributeModifiers != null) {
            final ListTag<CompoundTag> modifiersBackup = new ListTag<>(CompoundTag.class);
            boolean needsBackup = false;
            for (final AttributeModifiers1_21.AttributeModifier modifier : attributeModifiers.modifiers()) {
                if (modifier.display().id() != 0) {
                    needsBackup = true;
                }

                final CompoundTag modifierBackup = new CompoundTag();
                modifiersBackup.add(modifierBackup);
                modifierBackup.putInt("id", modifier.display().id());
                if (modifier.display() instanceof AttributeModifiers1_21.OverrideText overrideText) {
                    modifierBackup.put("text", overrideText.component());
                }
            }
            if (needsBackup) {
                backupTag.put("attribute_modifiers_displays", modifiersBackup);
            }
        }

        final Equippable equippable = dataContainer.get(StructuredDataKey.EQUIPPABLE1_21_6);
        if (equippable != null && equippable.canBeSheared()) {
            final CompoundTag equippableTag = new CompoundTag();
            equippableTag.putBoolean("can_be_sheared", true);
            saveSoundEventHolder(equippableTag, equippable.shearingSound());
            backupTag.put("equippable", equippableTag);
        }
    }

    @Override
    protected void restoreBackupData(final Item item, final StructuredDataContainer container, final CompoundTag customData) {
        super.restoreBackupData(item, container, customData);
        if (!(customData.remove(nbtTagName("backup")) instanceof final CompoundTag backupTag)) {
            return;
        }

        final ListTag<CompoundTag> attributeModifiersDisplays = backupTag.getListTag("attribute_modifiers_displays", CompoundTag.class);
        if (attributeModifiersDisplays != null) {
            container.replace(StructuredDataKey.ATTRIBUTE_MODIFIERS1_21_5, StructuredDataKey.ATTRIBUTE_MODIFIERS1_21_6, modifiers -> {
                final AttributeModifiers1_21.AttributeModifier[] updatedModifiers = new AttributeModifiers1_21.AttributeModifier[modifiers.modifiers().length];
                for (int i = 0; i < modifiers.modifiers().length; i++) {
                    final CompoundTag modifierBackup = attributeModifiersDisplays.get(i);
                    final int id = modifierBackup.getInt("id");
                    final AttributeModifiers1_21.Display display = id == 2 ? new AttributeModifiers1_21.OverrideText(modifierBackup.get("text")) : new AttributeModifiers1_21.Display(id);
                    final AttributeModifiers1_21.AttributeModifier modifier = modifiers.modifiers()[i];
                    updatedModifiers[i] = new AttributeModifiers1_21.AttributeModifier(modifier.attribute(), modifier.modifier(), modifier.slotType(), display);
                }
                return new AttributeModifiers1_21(updatedModifiers);
            });
        }

        final CompoundTag equippableTag = backupTag.getCompoundTag("equippable");
        if (equippableTag != null) {
            container.replace(StructuredDataKey.EQUIPPABLE1_21_5, StructuredDataKey.EQUIPPABLE1_21_6, equippable -> new Equippable(
                equippable.equipmentSlot(), equippable.soundEvent(), equippable.model(), equippable.cameraOverlay(), equippable.allowedEntities(),
                equippable.dispensable(), equippable.swappable(), equippable.damageOnHurt(), equippable.equipOnInteract(),
                equippableTag.getBoolean("can_be_sheared"),
                restoreSoundEventHolder(equippableTag)
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
}
