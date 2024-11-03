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

import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.InventoryStateIdStorage;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.storage.RecipeStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.MappingData;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.Consumable1_21_2;
import com.viaversion.viaversion.api.minecraft.item.data.DeathProtection;
import com.viaversion.viaversion.api.minecraft.item.data.Equippable;
import com.viaversion.viaversion.api.minecraft.item.data.Instrument1_21_2;
import com.viaversion.viaversion.api.minecraft.item.data.PotionEffect;
import com.viaversion.viaversion.api.minecraft.item.data.PotionEffectData;
import com.viaversion.viaversion.api.minecraft.item.data.UseCooldown;
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
import com.viaversion.viaversion.util.Unit;

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
            wrapper.write(Types.UNSIGNED_BYTE, (short) -1); // Player inventory
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
        backupInconvertibleData(item);
        downgradeItemData(item);
        return item;
    }

    @Override
    public Item handleItemToServer(final UserConnection connection, final Item item) {
        super.handleItemToServer(connection, item);
        updateItemData(item);
        restoreInconvertibleData(item);
        return item;
    }

    // Backup inconvertible data and later restore to prevent data loss for creative mode clients
    private void backupInconvertibleData(final Item item) {
        final StructuredDataContainer data = item.dataContainer();
        final Holder<Instrument1_21_2> instrument = data.get(StructuredDataKey.INSTRUMENT1_21_2);
        if (instrument != null && instrument.isDirect()) {
            saveTag(createCustomTag(item), instrument.value().description(), "instrument_description");
        }

        final HolderSet repairable = data.get(StructuredDataKey.REPAIRABLE);
        if (repairable != null) {
            final CompoundTag tag = new CompoundTag();
            convertHolderSet(tag, repairable);
            saveTag(createCustomTag(item), tag, "repairable");
        }

        final Integer enchantable = data.get(StructuredDataKey.ENCHANTABLE);
        if (enchantable != null) {
            saveTag(createCustomTag(item), new IntTag(enchantable), "enchantable");
        }

        final UseCooldown useCooldown = data.get(StructuredDataKey.USE_COOLDOWN);
        if (useCooldown != null) {
            final CompoundTag tag = new CompoundTag();
            tag.putFloat("seconds", useCooldown.seconds());
            if (useCooldown.cooldownGroup() != null) {
                tag.putString("cooldown_group", useCooldown.cooldownGroup());
            }
        }

        final String itemModel = data.get(StructuredDataKey.ITEM_MODEL);
        if (itemModel != null) {
            saveTag(createCustomTag(item), new StringTag(itemModel), "item_model");
        }

        final Equippable equippable = data.get(StructuredDataKey.EQUIPPABLE);
        if (equippable != null) {
            final CompoundTag tag = new CompoundTag();

            tag.putInt("equipment_slot", equippable.equipmentSlot());
            convertSoundEventHolder(tag, equippable.soundEvent());
            final String model = equippable.model();
            if (model != null) {
                tag.putString("model", model);
            }
            final String cameraOverlay = equippable.cameraOverlay();
            if (cameraOverlay != null) {
                tag.putString("camera_overlay", cameraOverlay);
            }
            if (equippable.allowedEntities() != null) {
                final CompoundTag allowedEntities = new CompoundTag();
                convertHolderSet(allowedEntities, equippable.allowedEntities());

                tag.put("allowed_entities", allowedEntities);
            }
            tag.putBoolean("dispensable", equippable.dispensable());
            tag.putBoolean("swappable", equippable.swappable());
            tag.putBoolean("damage_on_hurt", equippable.damageOnHurt());

            saveTag(createCustomTag(item), tag, "equippable");
        }

        final Unit glider = data.get(StructuredDataKey.GLIDER);
        if (glider != null) {
            saveTag(createCustomTag(item), new ByteTag(true), "glider");
        }

        final String tooltipStyle = data.get(StructuredDataKey.TOOLTIP_STYLE);
        if (tooltipStyle != null) {
            saveTag(createCustomTag(item), new StringTag(tooltipStyle), "tooltip_style");
        }

        final DeathProtection deathProtection = data.get(StructuredDataKey.DEATH_PROTECTION);
        if (deathProtection == null) {
            return;
        }
        final ListTag<CompoundTag> tag = new ListTag<>(CompoundTag.class);
        for (final Consumable1_21_2.ConsumeEffect<?> effect : deathProtection.deathEffects()) {
            final CompoundTag effectTag = new CompoundTag();
            convertConsumableEffect(effectTag, effect);
            tag.add(effectTag);
        }
        saveTag(createCustomTag(item), tag, "death_protection");
    }

    private void convertConsumableEffect(final CompoundTag tag, Consumable1_21_2.ConsumeEffect<?> effect) {
        tag.putInt("id", effect.id());
        if (effect.type() == Consumable1_21_2.ApplyStatusEffects.TYPE && effect.value() instanceof Consumable1_21_2.ApplyStatusEffects value) {
            tag.putString("type", "apply_effects");

            final ListTag<CompoundTag> effects = new ListTag<>(CompoundTag.class);
            for (final PotionEffect potionEffect : value.effects()) {
                final CompoundTag effectTag = new CompoundTag();
                effectTag.putInt("effect", potionEffect.effect());
                convertPotionEffectData(effectTag, potionEffect.effectData());
                effects.add(effectTag);
            }
            tag.put("effects", effects);
            tag.putFloat("probability", value.probability());
        } else if (effect.type() == Types.HOLDER_SET && effect.value() instanceof HolderSet set) {
            tag.putString("type", "remove_effects");

            if (set.hasIds()) {
                tag.put("ids", new IntArrayTag(set.ids()));
            } else {
                tag.putString("tag", set.tagKey());
            }
        } else if (effect.type() == Types.EMPTY) {
            tag.putString("type", "clear_all_effects");
        } else if (effect.type() == Types.FLOAT) {
            tag.putString("type", "teleport_randomly");

            tag.putFloat("probability", (Float) effect.value());
        } else if (effect.type() == Types.SOUND_EVENT && effect.value() instanceof Holder sound) {
            tag.putString("type", "play_sound");

            convertSoundEventHolder(tag, sound);
        }
    }

    private void convertPotionEffectData(final CompoundTag tag, final PotionEffectData data) {
        tag.putInt("amplifier", data.amplifier());
        tag.putInt("duration", data.duration());
        tag.putBoolean("ambient", data.ambient());
        tag.putBoolean("show_particles", data.showParticles());
        tag.putBoolean("show_icon", data.showIcon());
        if (data.hiddenEffect() != null) {
            final CompoundTag hiddenEffect = new CompoundTag();
            convertPotionEffectData(hiddenEffect, data.hiddenEffect());
            tag.put("hidden_effect", hiddenEffect);
        }
    }

    private void convertSoundEventHolder(final CompoundTag tag, final Holder<SoundEvent> holder) {
        if (holder.hasId()) {
            tag.putInt("sound", holder.id());
        } else {
            final SoundEvent event = holder.value();
            tag.putString("identifier", event.identifier());
            if (event.fixedRange() != null) {
                tag.putFloat("fixed_range", event.fixedRange());
            }
        }
    }

    private void convertHolderSet(final CompoundTag tag, final HolderSet set) {
        if (set.hasIds()) {
            tag.put("ids", new IntArrayTag(set.ids()));
        } else {
            tag.putString("tag", set.tagKey());
        }
    }

    private Consumable1_21_2.ConsumeEffect<?> convertConsumableEffect(final CompoundTag tag) {
        final int id = tag.getInt("id");
        final String type = tag.getString("type");
        if ("apply_effects".equals(type)) {
            final ListTag<CompoundTag> effects = tag.getListTag("effects", CompoundTag.class);
            final PotionEffect[] potionEffects = new PotionEffect[effects.size()];
            for (int i = 0; i < effects.size(); i++) {
                final CompoundTag effectTag = effects.get(i);
                final int effect = effectTag.getInt("effect");
                final PotionEffectData data = convertPotionEffectData(effectTag.getCompoundTag("data"));
                potionEffects[i] = new PotionEffect(effect, data);
            }
            final float probability = tag.getFloat("probability");
            return new Consumable1_21_2.ConsumeEffect<>(id, Consumable1_21_2.ApplyStatusEffects.TYPE, new Consumable1_21_2.ApplyStatusEffects(potionEffects, probability));
        } else if ("remove_effects".equals(type)) {
            final HolderSet set = convertHolderSet(tag);
            return new Consumable1_21_2.ConsumeEffect<>(id, Types.HOLDER_SET, set);
        } else if ("clear_all_effects".equals(type)) {
            return new Consumable1_21_2.ConsumeEffect<>(id, Types.EMPTY, Unit.INSTANCE);
        } else if ("teleport_randomly".equals(type)) {
            final float probability = tag.getFloat("probability");
            return new Consumable1_21_2.ConsumeEffect<>(id, Types.FLOAT, probability);
        } else if ("play_sound".equals(type)) {
            final Holder<SoundEvent> sound = convertSoundEventHolder(tag);
            return new Consumable1_21_2.ConsumeEffect<>(id, Types.SOUND_EVENT, sound);
        }
        return null;
    }

    private PotionEffectData convertPotionEffectData(final CompoundTag tag) {
        final int amplifier = tag.getInt("amplifier");
        final int duration = tag.getInt("duration");
        final boolean ambient = tag.getBoolean("ambient");
        final boolean showParticles = tag.getBoolean("show_particles");
        final boolean showIcon = tag.getBoolean("show_icon");
        final CompoundTag hiddenEffect = tag.getCompoundTag("hidden_effect");
        return new PotionEffectData(amplifier, duration, ambient, showParticles, showIcon, hiddenEffect != null ? convertPotionEffectData(hiddenEffect) : null);
    }

    private Holder<SoundEvent> convertSoundEventHolder(final CompoundTag tag) {
        final int soundId = tag.getInt("sound");
        if (soundId != 0) {
            return Holder.of(soundId);
        }

        final String identifier = tag.getString("identifier");
        final Float fixedRange = tag.getFloat("fixed_range");
        return Holder.of(new SoundEvent(identifier, fixedRange));
    }

    private HolderSet convertHolderSet(final CompoundTag tag) {
        if (tag == null) {
            return null;
        }
        final IntArrayTag ids = tag.getIntArrayTag("ids");
        if (ids != null) {
            return HolderSet.of(ids.getValue());
        }
        return HolderSet.of(tag.getString("tag"));
    }

    private void restoreInconvertibleData(final Item item) {
        final StructuredDataContainer data = item.dataContainer();
        final CompoundTag customData = data.get(StructuredDataKey.CUSTOM_DATA);

        final Holder<Instrument1_21_2> instrument = data.get(StructuredDataKey.INSTRUMENT1_21_2);
        if (instrument != null && customData != null) {
            final Tag description = customData.remove(nbtTagName("instrument_description"));
            if (description != null) {
                final Instrument1_21_2 delegate = instrument.value();
                data.set(StructuredDataKey.INSTRUMENT1_21_2, Holder.of(new Instrument1_21_2(delegate.soundEvent(), delegate.useDuration(), delegate.range(), description)));
            }
            removeCustomTag(data, customData);
        }

        final IntArrayTag repairableIds = customData.getIntArrayTag("repairable_ids");
        final String repairableTag = customData.getString("repairable_tag");
        if (repairableIds != null || repairableTag != null) {
            data.set(StructuredDataKey.REPAIRABLE, repairableIds != null ? HolderSet.of(repairableIds.getValue()) : HolderSet.of(repairableTag));
            removeCustomTag(data, customData);
        }

        final IntTag enchantable = customData.getIntTag("enchantable");
        if (enchantable != null) {
            data.set(StructuredDataKey.ENCHANTABLE, enchantable.asInt());
            removeCustomTag(data, customData);
        }

        final CompoundTag useCooldown = customData.getCompoundTag("use_cooldown");
        if (useCooldown != null) {
            final float seconds = useCooldown.getFloat("seconds");
            final String cooldownGroup = useCooldown.getString("cooldown_group");
            data.set(StructuredDataKey.USE_COOLDOWN, new UseCooldown(seconds, cooldownGroup));
            removeCustomTag(data, customData);
        }

        final String itemModel = customData.getString("item_model");
        if (itemModel != null) {
            data.set(StructuredDataKey.ITEM_MODEL, itemModel);
            removeCustomTag(data, customData);
        }

        final CompoundTag equippable = customData.getCompoundTag("equippable");
        if (equippable != null) {
            final int equipmentSlot = equippable.getInt("equipment_slot");
            final Holder<SoundEvent> soundEvent = convertSoundEventHolder(equippable);
            final String model = equippable.getString("model");
            final String cameraOverlay = equippable.getString("camera_overlay");
            final HolderSet allowedEntities = convertHolderSet(equippable.getCompoundTag("allowed_entities"));
            final boolean dispensable = equippable.getBoolean("dispensable");
            final boolean swappable = equippable.getBoolean("swappable");
            final boolean damageOnHurt = equippable.getBoolean("damage_on_hurt");

            data.set(StructuredDataKey.EQUIPPABLE, new Equippable(equipmentSlot, soundEvent, model, cameraOverlay, allowedEntities, dispensable, swappable, damageOnHurt));
            removeCustomTag(data, customData);
        }

        final ByteTag glider = customData.getByteTag("glider");
        if (glider != null) {
            data.set(StructuredDataKey.GLIDER, Unit.INSTANCE);
            removeCustomTag(data, customData);
        }

        final String tooltipStyle = customData.getString("tooltip_style");
        if (tooltipStyle != null) {
            data.set(StructuredDataKey.TOOLTIP_STYLE, tooltipStyle);
            removeCustomTag(data, customData);
        }

        final ListTag<CompoundTag> deathProtection = customData.getListTag("death_protection", CompoundTag.class);
        if (deathProtection != null) {
            final Consumable1_21_2.ConsumeEffect<?>[] effects = new Consumable1_21_2.ConsumeEffect[deathProtection.size()];
            for (int i = 0; i < deathProtection.size(); i++) {
                effects[i] = convertConsumableEffect(deathProtection.get(i));
            }
            data.set(StructuredDataKey.DEATH_PROTECTION, new DeathProtection(effects));
            removeCustomTag(data, customData);
        }
    }
}
