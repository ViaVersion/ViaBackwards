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
import com.viaversion.nbt.tag.FloatTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.LongArrayTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.Protocol1_21_5To1_21_4;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.storage.HashedItemConverterStorage;
import com.viaversion.viabackwards.protocol.v1_21_5to1_21_4.storage.HorseDataStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.data.entity.TrackedEntity;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.PaintingVariant;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk1_18;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.Heightmap;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_21_5;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.ArmorTrimMaterial;
import com.viaversion.viaversion.api.minecraft.item.data.ArmorTrimPattern;
import com.viaversion.viaversion.api.minecraft.item.data.BlocksAttacks;
import com.viaversion.viaversion.api.minecraft.item.data.BlocksAttacks.DamageReduction;
import com.viaversion.viaversion.api.minecraft.item.data.BlocksAttacks.ItemDamageFunction;
import com.viaversion.viaversion.api.minecraft.item.data.Equippable;
import com.viaversion.viaversion.api.minecraft.item.data.ProvidesTrimMaterial;
import com.viaversion.viaversion.api.minecraft.item.data.ToolProperties;
import com.viaversion.viaversion.api.minecraft.item.data.TooltipDisplay;
import com.viaversion.viaversion.api.minecraft.item.data.TropicalFishPattern;
import com.viaversion.viaversion.api.minecraft.item.data.Weapon;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.rewriter.ComponentRewriter;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkBiomesType1_19_4;
import com.viaversion.viaversion.api.type.types.chunk.ChunkBiomesType1_21_5;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_21_5;
import com.viaversion.viaversion.api.type.types.version.VersionedTypes;
import com.viaversion.viaversion.data.item.ItemHasherBase;
import com.viaversion.viaversion.libs.fastutil.ints.IntArrayList;
import com.viaversion.viaversion.libs.fastutil.ints.IntLinkedOpenHashSet;
import com.viaversion.viaversion.libs.fastutil.ints.IntList;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPacket1_21_4;
import com.viaversion.viaversion.protocols.v1_21_2to1_21_4.packet.ServerboundPackets1_21_4;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPacket1_21_5;
import com.viaversion.viaversion.protocols.v1_21_4to1_21_5.packet.ClientboundPackets1_21_5;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeDisplayRewriter;
import com.viaversion.viaversion.util.Either;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.Limit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.BlockItemPacketRewriter1_21_5.downgradeItemData;
import static com.viaversion.viaversion.protocols.v1_21_4to1_21_5.rewriter.BlockItemPacketRewriter1_21_5.updateItemData;
import static com.viaversion.viaversion.util.MathUtil.ceilLog2;

public final class BlockItemPacketRewriter1_21_5 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21_5, ServerboundPacket1_21_4, Protocol1_21_5To1_21_4> {
    private static final int SIGN_BOCK_ENTITY_ID = 7;
    private static final int HANGING_SIGN_BOCK_ENTITY_ID = 8;
    private static final int SADDLE_EQUIPMENT_SLOT = 7;
    static final byte SADDLED_FLAG = 4;

    public BlockItemPacketRewriter1_21_5(final Protocol1_21_5To1_21_4 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_21_5> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_21_5.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_21_5.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_21_5.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelEvent1_21(ClientboundPackets1_21_5.LEVEL_EVENT, 2001);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_21_5.BLOCK_ENTITY_DATA, this::handleBlockEntity);

        protocol.registerClientbound(ClientboundPackets1_21_5.LEVEL_CHUNK_WITH_LIGHT, wrapper -> {
            final EntityTracker tracker = protocol.getEntityRewriter().tracker(wrapper.user());
            final Mappings blockStateMappings = protocol.getMappingData().getBlockStateMappings();
            final Type<Chunk> chunkType = new ChunkType1_21_5(tracker.currentWorldSectionHeight(), ceilLog2(blockStateMappings.size()), ceilLog2(tracker.biomesSent()));
            final Chunk chunk = wrapper.read(chunkType);
            blockRewriter.handleChunk(chunk);
            blockRewriter.handleBlockEntities(null, chunk, wrapper.user());

            final Type<Chunk> newChunkType = new ChunkType1_20_2(tracker.currentWorldSectionHeight(), ceilLog2(blockStateMappings.mappedSize()), ceilLog2(tracker.biomesSent()));
            final CompoundTag heightmapTag = new CompoundTag();
            for (final Heightmap heightmap : chunk.heightmaps()) {
                final String typeKey = heightmapType(heightmap.type());
                if (typeKey == null) {
                    protocol.getLogger().warning("Unknown heightmap type id: " + heightmap.type());
                    continue;
                }

                heightmapTag.put(typeKey, new LongArrayTag(heightmap.data()));
            }

            final Chunk mappedChunk = new Chunk1_18(chunk.getX(), chunk.getZ(), chunk.getSections(), heightmapTag, chunk.blockEntities());
            blockRewriter.handleBlockEntities(this::handleBlockEntity, chunk, wrapper.user());
            wrapper.write(newChunkType, mappedChunk);
        });

        protocol.registerClientbound(ClientboundPackets1_21_5.CHUNKS_BIOMES, wrapper -> {
            final EntityTracker tracker = protocol.getEntityRewriter().tracker(wrapper.user());
            final int globalPaletteBiomeBits = ceilLog2(tracker.biomesSent());
            final Type<DataPalette[]> biomesType = new ChunkBiomesType1_21_5(tracker.currentWorldSectionHeight(), globalPaletteBiomeBits);
            final Type<DataPalette[]> newBiomesType = new ChunkBiomesType1_19_4(tracker.currentWorldSectionHeight(), globalPaletteBiomeBits);

            final int size = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Types.CHUNK_POSITION);
                wrapper.passthroughAndMap(biomesType, newBiomesType);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_21_5.SET_CURSOR_ITEM, this::passthroughClientboundItem);
        registerSetPlayerInventory(ClientboundPackets1_21_5.SET_PLAYER_INVENTORY);
        registerCooldown1_21_2(ClientboundPackets1_21_5.COOLDOWN);
        registerSetContent1_21_2(ClientboundPackets1_21_5.CONTAINER_SET_CONTENT);
        registerSetSlot1_21_2(ClientboundPackets1_21_5.CONTAINER_SET_SLOT);
        registerMerchantOffers1_20_5(ClientboundPackets1_21_5.MERCHANT_OFFERS);

        protocol.registerServerbound(ServerboundPackets1_21_4.SET_CREATIVE_MODE_SLOT, wrapper -> {
            wrapper.passthrough(Types.SHORT); // Slot

            final Item item = handleItemToServer(wrapper.user(), wrapper.read(mappedItemType()));
            wrapper.write(VersionedTypes.V1_21_5.lengthPrefixedItem(), item);
        });

        protocol.registerServerbound(ServerboundPackets1_21_4.CONTAINER_CLICK, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Container id
            wrapper.passthrough(Types.VAR_INT); // State id
            wrapper.passthrough(Types.SHORT); // Slot
            wrapper.passthrough(Types.BYTE); // Button
            wrapper.passthrough(Types.VAR_INT); // Mode

            // Try our best to get a hashed item out of it - will be wrong for some data component types that don't have their conversion implemented
            final HashedItemConverterStorage hashedItemConverter = wrapper.user().get(HashedItemConverterStorage.class);
            final int affectedItems = Limit.max(wrapper.passthrough(Types.VAR_INT), 128);
            for (int i = 0; i < affectedItems; i++) {
                wrapper.passthrough(Types.SHORT); // Slot
                final Item item = handleItemToServer(wrapper.user(), wrapper.read(mappedItemType()));
                wrapper.write(Types.HASHED_ITEM, ItemHasherBase.toHashedItem(hashedItemConverter.hasher(), item));
            }

            final Item carriedItem = handleItemToServer(wrapper.user(), wrapper.read(mappedItemType()));
            wrapper.write(Types.HASHED_ITEM, ItemHasherBase.toHashedItem(hashedItemConverter.hasher(), carriedItem));
        });

        protocol.registerClientbound(ClientboundPackets1_21_5.SET_EQUIPMENT, wrapper -> {
            final int entityId = wrapper.passthrough(Types.VAR_INT);
            final TrackedEntity trackedEntity = protocol.getEntityRewriter().tracker(wrapper.user()).entity(entityId);

            // Remove saddle equipment, keep the rest
            final IntList keptSlots = new IntArrayList();
            final List<Item> keptItems = new ArrayList<>();
            byte value;
            do {
                value = wrapper.read(Types.BYTE);
                final int equipmentSlot = value & 0x7F;
                final Item item = wrapper.read(itemType());
                if (equipmentSlot == SADDLE_EQUIPMENT_SLOT) {
                    // Send saddle entity data for horses
                    if (trackedEntity != null && trackedEntity.entityType().isOrHasParent(EntityTypes1_21_5.ABSTRACT_HORSE)) {
                        sendSaddledEntityData(wrapper.user(), trackedEntity, entityId, item.identifier() == 800);
                    }
                } else {
                    keptSlots.add(equipmentSlot);
                    keptItems.add(handleItemToClient(wrapper.user(), item));
                }
            } while (value < 0);

            if (keptSlots.isEmpty()) {
                wrapper.cancel();
                return;
            }

            for (int i = 0; i < keptSlots.size(); i++) {
                final int slot = keptSlots.getInt(i);
                final boolean more = i < keptSlots.size() - 1;
                wrapper.write(Types.BYTE, (byte) (more ? (slot | -128) : slot));
                wrapper.write(mappedItemType(), keptItems.get(i));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_21_5.UPDATE_ADVANCEMENTS, wrapper -> {
            wrapper.passthrough(Types.BOOLEAN); // Reset/clear
            int size = wrapper.passthrough(Types.VAR_INT); // Mapping size
            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Types.STRING); // Identifier
                wrapper.passthrough(Types.OPTIONAL_STRING); // Parent

                // Display data
                if (wrapper.passthrough(Types.BOOLEAN)) {
                    final Tag title = wrapper.passthrough(Types.TAG);
                    final Tag description = wrapper.passthrough(Types.TAG);
                    final ComponentRewriter componentRewriter = protocol.getComponentRewriter();
                    if (componentRewriter != null) {
                        componentRewriter.processTag(wrapper.user(), title);
                        componentRewriter.processTag(wrapper.user(), description);
                    }

                    passthroughClientboundItem(wrapper); // Icon
                    wrapper.passthrough(Types.VAR_INT); // Frame type
                    int flags = wrapper.passthrough(Types.INT); // Flags
                    if ((flags & 1) != 0) {
                        convertClientAsset(wrapper);
                    }
                    wrapper.passthrough(Types.FLOAT); // X
                    wrapper.passthrough(Types.FLOAT); // Y
                }

                int requirements = wrapper.passthrough(Types.VAR_INT);
                for (int array = 0; array < requirements; array++) {
                    wrapper.passthrough(Types.STRING_ARRAY);
                }

                wrapper.passthrough(Types.BOOLEAN); // Send telemetry
            }

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

        final RecipeDisplayRewriter<ClientboundPacket1_21_5> recipeRewriter = new RecipeDisplayRewriter<>(protocol) {
            @Override
            protected void handleSmithingTrimSlotDisplay(final PacketWrapper wrapper) {
                handleSlotDisplay(wrapper); // Base
                handleSlotDisplay(wrapper); // Material
                wrapper.read(ArmorTrimPattern.TYPE1_21_5); // Patterm
                // Add empty slot display...
                wrapper.write(Types.VAR_INT, 0);
            }
        };
        recipeRewriter.registerUpdateRecipes(ClientboundPackets1_21_5.UPDATE_RECIPES);
        recipeRewriter.registerRecipeBookAdd(ClientboundPackets1_21_5.RECIPE_BOOK_ADD);
        recipeRewriter.registerPlaceGhostRecipe(ClientboundPackets1_21_5.PLACE_GHOST_RECIPE);
    }

    private void convertClientAsset(final PacketWrapper wrapper) {
        final String background = wrapper.read(Types.STRING);
        final String namespace = Key.namespace(background);
        final String path = Key.stripNamespace(background);
        wrapper.write(Types.STRING, namespace + ":textures/" + path + ".png");
    }

    private void sendSaddledEntityData(final UserConnection connection, final TrackedEntity trackedEntity, final int entityId, final boolean saddled) {
        byte data = 0;
        if (trackedEntity.hasData()) {
            final HorseDataStorage horseDataStorage = trackedEntity.data().get(HorseDataStorage.class);
            if (horseDataStorage != null) {
                if (horseDataStorage.saddled() == saddled) {
                    return;
                }

                data = horseDataStorage.data();
            }
        }

        trackedEntity.data().put(new HorseDataStorage(data, saddled));

        if (saddled) {
            data = (byte) (data | SADDLED_FLAG);
        }

        final PacketWrapper entityDataPacket = PacketWrapper.create(ClientboundPackets1_21_2.SET_ENTITY_DATA, connection);
        final List<EntityData> entityDataList = new ArrayList<>();
        entityDataList.add(new EntityData(17, VersionedTypes.V1_21_4.entityDataTypes.byteType, data));
        entityDataPacket.write(Types.VAR_INT, entityId);
        entityDataPacket.write(VersionedTypes.V1_21_4.entityDataList, entityDataList);
        entityDataPacket.send(Protocol1_21_5To1_21_4.class);
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

        final Tag customName = tag.get("CustomName");
        if (customName != null) {
            tag.putString("CustomName", protocol.getComponentRewriter().toUglyJson(connection, customName));
        }
    }

    private void updateSignMessages(final UserConnection connection, final CompoundTag tag) {
        if (tag == null) {
            return;
        }

        final ListTag<?> messages = tag.getListTag("messages");
        tag.put("messages", protocol.getComponentRewriter().updateComponentList(connection, messages));

        final ListTag<?> filteredMessages = tag.getListTag("filtered_messages");
        if (filteredMessages != null) {
            tag.put("filtered_messages", protocol.getComponentRewriter().updateComponentList(connection, filteredMessages));
        }
    }

    private String heightmapType(final int id) {
        return switch (id) {
            case 0 -> "WORLD_SURFACE_WG";
            case 1 -> "WORLD_SURFACE";
            case 2 -> "OCEAN_FLOOR_WG";
            case 3 -> "OCEAN_FLOOR";
            case 4 -> "MOTION_BLOCKING";
            case 5 -> "MOTION_BLOCKING_NO_LEAVES";
            default -> null;
        };
    }

    @Override
    protected void backupInconvertibleData(final UserConnection connection, final Item item, final StructuredDataContainer dataContainer, final CompoundTag backupTag) {
        super.backupInconvertibleData(connection, item, dataContainer, backupTag);
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
            weaponTag.putFloat("disable_blocking_for_seconds", weapon.disableBlockingForSeconds());
        }

        final ProvidesTrimMaterial providesTrimMaterial = dataContainer.get(StructuredDataKey.PROVIDES_TRIM_MATERIAL);
        if (providesTrimMaterial != null) {
            final Tag materialTag = eitherHolderToTag(providesTrimMaterial.material(), (material, tag) -> {
                tag.putString("asset_name", material.assetName());
                tag.putInt("item_id", material.itemId());
                tag.putFloat("item_model_index", material.itemModelIndex());
                final CompoundTag overrideArmorMaterials = new CompoundTag();
                material.overrideArmorMaterials().forEach(overrideArmorMaterials::putString);
                tag.put("override_armor_materials", overrideArmorMaterials);
                tag.put("description", material.description());
            });
            backupTag.put("provides_trim_material", materialTag);
        }

        final BlocksAttacks blocksAttacks = dataContainer.get(StructuredDataKey.BLOCKS_ATTACKS);
        if (blocksAttacks != null) {
            final CompoundTag blocksAttackTag = new CompoundTag();
            backupTag.put("blocks_attack", blocksAttackTag);
            blocksAttackTag.putFloat("block_delay_seconds", blocksAttacks.blockDelaySeconds());
            blocksAttackTag.putFloat("disable_cooldown_scale", blocksAttacks.disableCooldownScale());

            final ListTag<CompoundTag> damageReductions = new ListTag<>(CompoundTag.class);
            blocksAttackTag.put("damage_reductions", damageReductions);
            for (final DamageReduction damageReduction : blocksAttacks.damageReductions()) {
                final CompoundTag damageReductionTag = new CompoundTag();
                damageReductionTag.putFloat("horizontal_blocking_angle", damageReduction.horizontalBlockingAngle());
                if (damageReduction.type() != null) {
                    damageReductionTag.put("type", holderSetToTag(damageReduction.type()));
                }
                damageReductionTag.putFloat("base", damageReduction.base());
                damageReductionTag.putFloat("factor", damageReduction.factor());
                damageReductions.add(damageReductionTag);
            }

            final CompoundTag itemDamageTag = new CompoundTag();
            blocksAttackTag.put("item_damage", itemDamageTag);
            itemDamageTag.putFloat("threshold", blocksAttacks.itemDamage().threshold());
            itemDamageTag.putFloat("base", blocksAttacks.itemDamage().base());
            itemDamageTag.putFloat("factor", blocksAttacks.itemDamage().factor());
            if (blocksAttacks.bypassedByTag() != null) {
                itemDamageTag.putString("bypassed_by", blocksAttacks.bypassedByTag());
            }
            if (blocksAttacks.blockSound() != null) {
                blocksAttackTag.put("block_sound", holderToTag(blocksAttacks.blockSound(), this::saveSoundEvent));
            }
            if (blocksAttacks.disableSound() != null) {
                blocksAttackTag.put("disable_sound", holderToTag(blocksAttacks.disableSound(), this::saveSoundEvent));
            }
        }

        final TooltipDisplay tooltipDisplay = dataContainer.get(StructuredDataKey.TOOLTIP_DISPLAY);
        if (tooltipDisplay != null) {
            backupTag.put("hidden_components", new IntArrayTag(tooltipDisplay.hiddenComponents().toIntArray()));
        }

        final TropicalFishPattern tropicalFishPattern = dataContainer.get(StructuredDataKey.TROPICAL_FISH_PATTERN);
        if (tropicalFishPattern != null) {
            backupTag.putInt("tropical_fish_pattern", tropicalFishPattern.packedId());
        }

        saveKeyData(StructuredDataKey.PROVIDES_BANNER_PATTERNS, dataContainer, backupTag);
        saveFloatData(StructuredDataKey.POTION_DURATION_SCALE, dataContainer, backupTag);
        saveIntData(StructuredDataKey.VILLAGER_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.FOX_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.SALMON_SIZE, dataContainer, backupTag);
        saveIntData(StructuredDataKey.PARROT_VARIANT, dataContainer, backupTag);
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
        saveIntData(StructuredDataKey.WOLF_SOUND_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.COW_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.PIG_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.WOLF_VARIANT, dataContainer, backupTag);

        final Either<Integer, String> chickenVariant = dataContainer.get(StructuredDataKey.CHICKEN_VARIANT);
        if (chickenVariant != null) {
            if (chickenVariant.isLeft()) {
                backupTag.putInt("chicken_variant", chickenVariant.left());
            } else {
                backupTag.putString("chicken_variant", chickenVariant.right());
            }
        }

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

        saveHolderData(StructuredDataKey.BREAK_SOUND, dataContainer, backupTag, this::saveSoundEvent);
    }

    @Override
    protected void handleItemDataComponentsToClient(final UserConnection connection, final Item item, final StructuredDataContainer dataContainer) {
        super.handleItemDataComponentsToClient(connection, item, dataContainer);
        downgradeItemData(item);
    }

    @Override
    protected void handleItemDataComponentsToServer(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToServer(connection, item, container);
        updateItemData(item);
    }

    @Override
    protected void restoreBackupData(final Item item, final StructuredDataContainer data, final CompoundTag customData) {
        super.restoreBackupData(item, data, customData);
        if (!(customData.remove(nbtTagName("backup")) instanceof final CompoundTag backupTag)) {
            return;
        }

        final IntArrayTag hiddenComponentsTag = backupTag.getIntArrayTag("hidden_components");
        if (hiddenComponentsTag != null) {
            data.set(StructuredDataKey.TOOLTIP_DISPLAY, new TooltipDisplay(data.has(StructuredDataKey.HIDE_TOOLTIP), new IntLinkedOpenHashSet(hiddenComponentsTag.getValue())));
        }

        if (backupTag.getBoolean("tool")) {
            data.replace(StructuredDataKey.TOOL1_20_5, StructuredDataKey.TOOL1_21_5, t -> new ToolProperties(t.rules(), t.defaultMiningSpeed(), t.damagePerBlock(), true));
        }

        if (backupTag.getBoolean("equippable")) {
            data.replace(StructuredDataKey.EQUIPPABLE1_21_2, StructuredDataKey.EQUIPPABLE1_21_5, e -> new Equippable(e.equipmentSlot(), e.soundEvent(), e.model(), e.cameraOverlay(), e.allowedEntities(), e.dispensable(), e.swappable(), e.damageOnHurt(), true));
        }

        final CompoundTag weaponTag = backupTag.getCompoundTag("weapon");
        if (weaponTag != null) {
            data.set(StructuredDataKey.WEAPON, new Weapon(weaponTag.getInt("item_damage_per_attack"), weaponTag.getFloat("disable_blocking_for_seconds")));
        }

        final Tag materialTag = backupTag.get("provides_trim_material");
        if (materialTag != null) {
            data.set(StructuredDataKey.PROVIDES_TRIM_MATERIAL, new ProvidesTrimMaterial(restoreEitherHolder(backupTag, "provides_trim_material", tag -> {
                final String assetName = tag.getString("asset_name");
                final int itemId = tag.getInt("item_id");
                final float itemModelIndex = tag.getFloat("item_model_index");
                final CompoundTag overrideArmorMaterialsTag = tag.getCompoundTag("override_armor_materials");
                final Map<String, String> overrideArmorMaterials = new HashMap<>();
                for (final String key : overrideArmorMaterialsTag.keySet()) {
                    overrideArmorMaterials.put(key, overrideArmorMaterialsTag.getString(key));
                }
                final Tag description = tag.get("description");
                return new ArmorTrimMaterial(assetName, itemId, itemModelIndex, overrideArmorMaterials, description);
            })));
        }

        final CompoundTag blocksAttackTag = backupTag.getCompoundTag("blocks_attack");
        if (blocksAttackTag != null) {
            final float blockDelaySeconds = blocksAttackTag.getFloat("block_delay_seconds");
            final float disableCooldownScale = blocksAttackTag.getFloat("disable_cooldown_scale");
            final CompoundTag itemDamageTag = blocksAttackTag.getCompoundTag("item_damage");
            final ItemDamageFunction itemDamage = new ItemDamageFunction(itemDamageTag.getFloat("threshold"), itemDamageTag.getFloat("base"), itemDamageTag.getFloat("factor"));
            final String bypassedBy = blocksAttackTag.getString("bypassed_by");
            final Holder<SoundEvent> blockSound = blocksAttackTag.contains("block_sound") ? restoreHolder(blocksAttackTag, "block_sound", this::tagToSound) : null;
            final Holder<SoundEvent> disableSound = blocksAttackTag.contains("disable_sound") ? restoreHolder(blocksAttackTag, "disable_sound", this::tagToSound) : null;

            final List<DamageReduction> damageReductions = new ArrayList<>();
            for (final CompoundTag damageReductionTag : blocksAttackTag.getListTag("damage_reductions", CompoundTag.class)) {
                final float horizontalBlockingAngle = damageReductionTag.getFloat("horizontal_blocking_angle");
                final HolderSet type = damageReductionTag.contains("type") ? restoreHolderSet(damageReductionTag, "type") : null;
                final float base = damageReductionTag.getFloat("base");
                final float factor = damageReductionTag.getFloat("factor");
                damageReductions.add(new DamageReduction(horizontalBlockingAngle, type, base, factor));
            }

            data.set(StructuredDataKey.BLOCKS_ATTACKS, new BlocksAttacks(blockDelaySeconds, disableCooldownScale, damageReductions.toArray(new DamageReduction[0]), itemDamage, bypassedBy, blockSound, disableSound));
        }

        final IntTag chickenVariant = backupTag.getIntTag("chicken_variant");
        if (chickenVariant != null) {
            data.set(StructuredDataKey.CHICKEN_VARIANT, Either.left(chickenVariant.asInt()));
        } else {
            final String chickenVariantKey = backupTag.getString("chicken_variant");
            if (chickenVariantKey != null) {
                data.set(StructuredDataKey.CHICKEN_VARIANT, Either.right(chickenVariantKey));
            }
        }

        final IntTag tropicalFishPattern = backupTag.getIntTag("tropical_fish_pattern");
        if (tropicalFishPattern != null) {
            data.set(StructuredDataKey.TROPICAL_FISH_PATTERN, new TropicalFishPattern(tropicalFishPattern.asInt()));
        }

        restoreKeyData(StructuredDataKey.PROVIDES_BANNER_PATTERNS, data, backupTag);
        restoreFloatData(StructuredDataKey.POTION_DURATION_SCALE, data, backupTag);
        restoreIntData(StructuredDataKey.VILLAGER_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.FOX_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.SALMON_SIZE, data, backupTag);
        restoreIntData(StructuredDataKey.PARROT_VARIANT, data, backupTag);
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
        restoreIntData(StructuredDataKey.WOLF_SOUND_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.COW_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.PIG_VARIANT, data, backupTag);
        restoreIntData(StructuredDataKey.WOLF_VARIANT, data, backupTag);

        restoreHolderData(StructuredDataKey.BREAK_SOUND, data, backupTag, this::tagToSound);
        restoreHolderData(StructuredDataKey.PAINTING_VARIANT, data, backupTag, tag -> {
            final int width = tag.getInt("width");
            final int height = tag.getInt("height");
            final String assetId = tag.getString("asset_id");
            final Tag title = tag.get("title");
            final Tag author = tag.get("author");
            return new PaintingVariant(width, height, assetId, title, author);
        });

        removeCustomTag(data, customData);
    }

    private SoundEvent tagToSound(final CompoundTag tag) {
        final String identifier = tag.getString("identifier");
        final FloatTag fixedRangeTag = tag.getFloatTag("fixed_range");
        return new SoundEvent(identifier, fixedRangeTag != null ? fixedRangeTag.asFloat() : null);
    }
}
