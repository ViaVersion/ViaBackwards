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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.Protocol1_21_5To1_21_4;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.PaintingVariant;
import com.viaversion.viaversion.api.minecraft.PigVariant;
import com.viaversion.viaversion.api.minecraft.WolfVariant;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.Equippable;
import com.viaversion.viaversion.api.minecraft.item.data.ToolProperties;
import com.viaversion.viaversion.api.minecraft.item.data.Weapon;
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

import static com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.BlockItemPacketRewriter1_21_5.downgradeItemData;
import static com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.BlockItemPacketRewriter1_21_5.updateItemData;

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

    @Override
    public Item handleItemToClient(final UserConnection connection, final Item item) {
        super.handleItemToClient(connection, item);

        final StructuredDataContainer dataContainer = item.dataContainer();
        final CompoundTag backupTag = new CompoundTag();

        final ToolProperties toolProperties = dataContainer.get(StructuredDataKey.TOOL1_21_5);
        if (toolProperties != null && toolProperties.canDestroyBlocksInCreative()) {
            backupTag.putBoolean("tool", true);
        }

        final Equippable equippable = dataContainer.get(StructuredDataKey.EQUIPPABLE1_21_5);
        if (equippable != null && equippable.equipOnInteract()) {
            backupTag.putBoolean("equippable", true);
        }

        final Weapon weapon = dataContainer.get(StructuredDataKey.WEAPON);
        if (weapon != null) {
            final CompoundTag weaponTag = new CompoundTag();
            backupTag.put("weapon", weaponTag);
            weaponTag.putInt("item_damage_per_attack", weapon.itemDamagePerAttack());
            if (weapon.canDisableBlocking()) {
                weaponTag.putBoolean("can_disable_blocking", true);
            }
        }

        saveFloatData(StructuredDataKey.POTION_DURATION_SCALE, dataContainer, backupTag);
        saveIntData(StructuredDataKey.VILLAGER_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.FOX_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.SALMON_SIZE, dataContainer, backupTag);
        saveIntData(StructuredDataKey.PARROT_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.TROPICAL_FISH_PATTERN, dataContainer, backupTag);
        saveIntData(StructuredDataKey.TROPICAL_FISH_BASE_COLOR, dataContainer, backupTag);
        saveIntData(StructuredDataKey.TROPICAL_FISH_PATTERN_COLOR, dataContainer, backupTag);
        saveIntData(StructuredDataKey.MOOSHROOM_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.RABBIT_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.FROG_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.HORSE_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.LLAMA_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.AXOLOTL_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.CAT_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.CAT_COLLAR, dataContainer, backupTag);
        saveIntData(StructuredDataKey.SHEEP_COLOR, dataContainer, backupTag);
        saveIntData(StructuredDataKey.SHULKER_COLOR, dataContainer, backupTag);

        saveHolderData(StructuredDataKey.PIG_VARIANT, dataContainer, backupTag, (pigVariant, tag) -> {
            tag.putInt("model_type", pigVariant.modelType());
            tag.putString("texture", pigVariant.texture());
            if (pigVariant.biomes() != null) {
                tag.put("biomes", holderSetToTag(pigVariant.biomes()));
            }
        });
        saveHolderData(StructuredDataKey.PAINTING_VARIANT, dataContainer, backupTag, (paintingVariant, tag) -> {
            tag.putInt("width", paintingVariant.width());
            tag.putInt("height", paintingVariant.height());
            tag.putString("asset_id", paintingVariant.assetId());
            if (paintingVariant.title() != null) {
                tag.put("title", paintingVariant.title());
            }
            if (paintingVariant.author() != null) {
                tag.put("author", paintingVariant.author());
            }
        });
        saveHolderData(StructuredDataKey.WOLF_VARIANT, dataContainer, backupTag, (wolfVariant, tag) -> {
            tag.putString("wild_texture", wolfVariant.wildTexture());
            tag.putString("tame_texture", wolfVariant.tameTexture());
            tag.putString("angry_texture", wolfVariant.angryTexture());
            tag.put("biomes", holderSetToTag(wolfVariant.biomes()));
        });

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
        if (customData == null || !(customData.remove(nbtTagName("backup")) instanceof CompoundTag backupTag)) {
            return;
        }

        if (backupTag.getBoolean("tool")) {
            data.replace(StructuredDataKey.TOOL1_20_5, StructuredDataKey.TOOL1_21_5, t -> new ToolProperties(t.rules(), t.defaultMiningSpeed(), t.damagePerBlock(), true));
        }

        if (backupTag.getBoolean("equippable")) {
            data.replace(StructuredDataKey.EQUIPPABLE1_21_2, StructuredDataKey.EQUIPPABLE1_21_5, e -> new Equippable(e.equipmentSlot(), e.soundEvent(), e.model(), e.cameraOverlay(), e.allowedEntities(), e.dispensable(), e.swappable(), e.damageOnHurt(), true));
        }

        final CompoundTag weaponTag = backupTag.getCompoundTag("weapon");
        if (weaponTag != null) {
            data.set(StructuredDataKey.WEAPON, new Weapon(weaponTag.getInt("item_damage_per_attack"), weaponTag.getBoolean("can_disable_blocking")));
        }

        restoreFloatData(StructuredDataKey.POTION_DURATION_SCALE, data, backupTag);
        restoreIntData(StructuredDataKey.VILLAGER_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.FOX_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.SALMON_SIZE, data, backupTag);
        restoreIntData(StructuredDataKey.PARROT_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.TROPICAL_FISH_PATTERN, data, backupTag);
        restoreIntData(StructuredDataKey.TROPICAL_FISH_BASE_COLOR, data, backupTag);
        restoreIntData(StructuredDataKey.TROPICAL_FISH_PATTERN_COLOR, data, backupTag);
        restoreIntData(StructuredDataKey.MOOSHROOM_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.RABBIT_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.FROG_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.HORSE_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.LLAMA_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.AXOLOTL_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.CAT_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.CAT_COLLAR, data, backupTag);
        restoreIntData(StructuredDataKey.SHEEP_COLOR, data, backupTag);
        restoreIntData(StructuredDataKey.SHULKER_COLOR, data, backupTag);

        restoreHolderData(StructuredDataKey.PIG_VARIANT, data, backupTag, tag -> {
            final int modelType = tag.getInt("model_type");
            final String texture = tag.getString("texture");
            HolderSet biomes = null;
            if (tag.contains("biomes")) {
                biomes = restoreHolderSet(tag, "biomes");
            }
            return new PigVariant(modelType, texture, biomes);
        });
        restoreHolderData(StructuredDataKey.PAINTING_VARIANT, data, backupTag, tag -> {
            final int width = tag.getInt("width");
            final int height = tag.getInt("height");
            final String assetId = tag.getString("asset_id");
            final Tag title = tag.get("title");
            final Tag author = tag.get("author");
            return new PaintingVariant(width, height, assetId, title, author);
        });
        restoreHolderData(StructuredDataKey.WOLF_VARIANT, data, backupTag, tag -> {
            final String wildTexture = tag.getString("wild_texture");
            final String tameTexture = tag.getString("tame_texture");
            final String angryTexture = tag.getString("angry_texture");
            final HolderSet biomes = restoreHolderSet(tag, "biomes");
            return new WolfVariant(wildTexture, tameTexture, angryTexture, biomes);
        });

        removeCustomTag(data, customData);
    }
}
