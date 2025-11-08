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
package com.viaversion.viabackwards.protocol.v1_21to1_20_5.rewriter;

import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter;
import com.viaversion.viabackwards.api.rewriters.StructuredEnchantmentRewriter;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.Protocol1_21To1_20_5;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.storage.EnchantmentsPaintingsStorage;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.storage.OpenScreenStorage;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.storage.PlayerRotationStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.EitherHolder;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.AttributeModifiers1_21;
import com.viaversion.viaversion.api.minecraft.item.data.AttributeModifiers1_21.AttributeModifier;
import com.viaversion.viaversion.api.minecraft.item.data.AttributeModifiers1_21.ModifierData;
import com.viaversion.viaversion.api.minecraft.item.data.Enchantments;
import com.viaversion.viaversion.api.minecraft.item.data.JukeboxPlayable;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.mcstructs.text.TextFormatting;
import com.viaversion.viaversion.libs.mcstructs.text.components.StringComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.TranslationComponent;
import com.viaversion.viaversion.protocols.v1_20_2to1_20_3.rewriter.RecipeRewriter1_20_3;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.data.Enchantments1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPacket1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPackets1_21;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.IdRewriteFunction;
import com.viaversion.viaversion.util.SerializerVersion;
import java.util.ArrayList;
import java.util.List;

import static com.viaversion.viaversion.protocols.v1_20_5to1_21.rewriter.BlockItemPacketRewriter1_21.downgradeItemData;
import static com.viaversion.viaversion.protocols.v1_20_5to1_21.rewriter.BlockItemPacketRewriter1_21.updateItemData;

public final class BlockItemPacketRewriter1_21 extends BackwardsStructuredItemRewriter<ClientboundPacket1_21, ServerboundPacket1_20_5, Protocol1_21To1_20_5> {

    private final StructuredEnchantmentRewriter enchantmentRewriter = new StructuredEnchantmentRewriter(this);

    public BlockItemPacketRewriter1_21(final Protocol1_21To1_20_5 protocol) {
        super(protocol);
    }

    @Override
    public void registerPackets() {
        final BlockRewriter<ClientboundPacket1_21> blockRewriter = BlockRewriter.for1_20_2(protocol);
        blockRewriter.registerBlockEvent(ClientboundPackets1_21.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_21.BLOCK_UPDATE);
        blockRewriter.registerSectionBlocksUpdate1_20(ClientboundPackets1_21.SECTION_BLOCKS_UPDATE);
        blockRewriter.registerLevelChunk1_19(ClientboundPackets1_21.LEVEL_CHUNK_WITH_LIGHT, ChunkType1_20_2::new);
        blockRewriter.registerBlockEntityData(ClientboundPackets1_21.BLOCK_ENTITY_DATA);

        registerCooldown(ClientboundPackets1_21.COOLDOWN);
        registerSetContent1_17_1(ClientboundPackets1_21.CONTAINER_SET_CONTENT);
        registerSetSlot1_17_1(ClientboundPackets1_21.CONTAINER_SET_SLOT);
        registerAdvancements1_20_3(ClientboundPackets1_21.UPDATE_ADVANCEMENTS);
        registerSetEquipment(ClientboundPackets1_21.SET_EQUIPMENT);
        registerContainerClick1_17_1(ServerboundPackets1_20_5.CONTAINER_CLICK);
        registerMerchantOffers1_20_5(ClientboundPackets1_21.MERCHANT_OFFERS);
        registerSetCreativeModeSlot(ServerboundPackets1_20_5.SET_CREATIVE_MODE_SLOT);

        protocol.registerClientbound(ClientboundPackets1_21.OPEN_SCREEN, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Id

            // Tracking the type actually matters now with crafters also using container data above index 3
            final int menuType = wrapper.passthrough(Types.VAR_INT);
            wrapper.user().get(OpenScreenStorage.class).setMenuType(menuType);

            protocol.getComponentRewriter().passthroughAndProcess(wrapper);
        });

        protocol.registerClientbound(ClientboundPackets1_21.CONTAINER_SET_DATA, wrapper -> {
            wrapper.passthrough(Types.UNSIGNED_BYTE); // Container id
            final short property = wrapper.passthrough(Types.SHORT);
            if (property >= 4 && property <= 6) { // Enchantment hints
                final OpenScreenStorage openScreenStorage = wrapper.user().get(OpenScreenStorage.class);
                if (openScreenStorage.menuType() != 13) { // Enchantment table
                    return;
                }

                final short enchantmentId = wrapper.read(Types.SHORT);
                final EnchantmentsPaintingsStorage storage = wrapper.user().get(EnchantmentsPaintingsStorage.class);
                final String key = storage.enchantments().idToKey(enchantmentId);
                final int mappedId = key != null ? Enchantments1_20_5.keyToId(key) : -1;
                wrapper.write(Types.SHORT, (short) mappedId);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_21.HORSE_SCREEN_OPEN, wrapper -> {
            wrapper.passthrough(Types.UNSIGNED_BYTE); // Container id

            // From columns to size
            final int columns = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.VAR_INT, columns * 3 + 1);
        });

        protocol.registerClientbound(ClientboundPackets1_21.LEVEL_EVENT, wrapper -> {
            final int event = wrapper.passthrough(Types.INT);
            wrapper.passthrough(Types.BLOCK_POSITION1_14);

            final int data = wrapper.read(Types.INT);
            if (event == 1010) {
                final int itemId = wrapper.user().get(EnchantmentsPaintingsStorage.class).jubeboxSongToItem(data);
                if (itemId == -1) {
                    wrapper.cancel();
                    return;
                }

                wrapper.write(Types.INT, itemId);
            } else if (event == 2001) {
                wrapper.write(Types.INT, protocol.getMappingData().getNewBlockStateId(data));
            } else {
                wrapper.write(Types.INT, data);
            }
        });

        protocol.registerServerbound(ServerboundPackets1_20_5.USE_ITEM, wrapper -> {
            wrapper.passthrough(Types.VAR_INT); // Hand
            wrapper.passthrough(Types.VAR_INT); // Sequence

            final PlayerRotationStorage rotation = wrapper.user().get(PlayerRotationStorage.class);
            wrapper.write(Types.FLOAT, rotation.yaw());
            wrapper.write(Types.FLOAT, rotation.pitch());
        });

        new RecipeRewriter1_20_3<>(protocol).register1_20_5(ClientboundPackets1_21.UPDATE_RECIPES);
    }

    @Override
    public Item handleItemToClient(final UserConnection connection, Item item) {
        if (item.isEmpty()) {
            return item;
        }

        final StructuredDataContainer data = item.dataContainer();
        data.setIdLookup(protocol, true);

        // Enchantments
        final EnchantmentsPaintingsStorage storage = connection.get(EnchantmentsPaintingsStorage.class);
        final IdRewriteFunction idRewriteFunction = id -> {
            final String key = storage.enchantments().idToKey(id);
            return key != null ? Enchantments1_20_5.keyToId(key) : -1;
        };
        final StructuredEnchantmentRewriter.DescriptionSupplier descriptionSupplier = (id, level) -> {
            final Tag description = storage.enchantmentDescription(id);
            if (description == null) {
                return new StringTag("Unknown enchantment");
            }

            final var component = SerializerVersion.V1_20_5.toComponent(description);
            component.getStyle().setItalic(false);
            component.getStyle().setFormatting(TextFormatting.GRAY);
            if (level != 1 || storage.enchantmentMaxLevel(id) != 1) {
                component.getSiblings().add(new StringComponent(" "));
                component.getSiblings().add(new TranslationComponent(EnchantmentRewriter.ENCHANTMENT_LEVEL_TRANSLATION.formatted(level)));
            }

            return SerializerVersion.V1_20_5.toTag(component);
        };
        enchantmentRewriter.rewriteEnchantmentsToClient(data, StructuredDataKey.ENCHANTMENTS1_20_5, idRewriteFunction, descriptionSupplier, false);
        enchantmentRewriter.rewriteEnchantmentsToClient(data, StructuredDataKey.STORED_ENCHANTMENTS1_20_5, idRewriteFunction, descriptionSupplier, true);

        final int identifier = item.identifier();

        // Order is important
        backupInconvertibleData(item);
        item = super.handleItemToClient(connection, item);
        downgradeItemData(item);

        if (data.has(StructuredDataKey.RARITY)) {
            return item;
        }
        // Change rarity of trident and piglin banner pattern
        final boolean trident = identifier == 1188;
        if (trident || identifier == 1200) {
            data.set(StructuredDataKey.RARITY, trident ? 3 : 1); // Epic or Uncommon
            saveTag(createCustomTag(item), new ByteTag(true), "rarity");
        }
        return item;
    }

    @Override
    public Item handleItemToServer(final UserConnection connection, Item item) {
        if (item.isEmpty()) {
            return item;
        }

        final StructuredDataContainer data = item.dataContainer();
        data.setIdLookup(protocol, false);

        // Rewrite enchantments
        final EnchantmentsPaintingsStorage storage = connection.get(EnchantmentsPaintingsStorage.class);
        rewriteEnchantmentToServer(storage, item, StructuredDataKey.ENCHANTMENTS1_20_5);
        rewriteEnchantmentToServer(storage, item, StructuredDataKey.STORED_ENCHANTMENTS1_20_5);

        // Restore originals if present
        enchantmentRewriter.handleToServer(item);

        // Order is important
        item = super.handleItemToServer(connection, item);
        updateItemData(item);
        restoreInconvertibleData(item);

        final CompoundTag customData = data.get(StructuredDataKey.CUSTOM_DATA);
        if (customData == null) {
            return item;
        }
        if (customData.remove(nbtTagName("rarity")) != null) {
            data.remove(StructuredDataKey.RARITY);
            removeCustomTag(data, customData);
        }
        return item;
    }

    private void rewriteEnchantmentToServer(final EnchantmentsPaintingsStorage storage, final Item item, final StructuredDataKey<Enchantments> key) {
        final Enchantments enchantments = item.dataContainer().get(key);
        if (enchantments == null) {
            return;
        }

        final List<PendingIdChange> updatedIds = new ArrayList<>();
        for (final Int2IntMap.Entry entry : enchantments.enchantments().int2IntEntrySet()) {
            final int id = entry.getIntKey();
            final String enchantmentKey = Enchantments1_20_5.idToKey(id);
            if (enchantmentKey == null) {
                continue;
            }

            final int mappedId = storage.enchantments().keyToId(enchantmentKey);
            if (id != mappedId) {
                final int level = entry.getIntValue();
                updatedIds.add(new PendingIdChange(id, mappedId, level));
            }
        }

        // Remove first, then add updated entries
        for (final PendingIdChange change : updatedIds) {
            enchantments.remove(change.id);
        }
        for (final PendingIdChange change : updatedIds) {
            enchantments.add(change.mappedId, change.level);
        }
    }

    private void backupInconvertibleData(final Item item) {
        final StructuredDataContainer data = item.dataContainer();
        data.setIdLookup(protocol, true);

        final CompoundTag backupTag = new CompoundTag();

        final JukeboxPlayable jukeboxPlayable = data.get(StructuredDataKey.JUKEBOX_PLAYABLE1_21);
        if (jukeboxPlayable != null) {
            final CompoundTag tag = new CompoundTag();
            if (jukeboxPlayable.song().hasHolder()) {
                final Holder<JukeboxPlayable.JukeboxSong> songHolder = jukeboxPlayable.song().holder();
                tag.put("song", holderToTag(songHolder, (song, songTag) -> {
                    saveSoundEventHolder(songTag, song.soundEvent());
                    songTag.put("description", song.description());
                    songTag.putFloat("length_in_seconds", song.lengthInSeconds());
                    songTag.putInt("comparator_output", song.comparatorOutput());
                }));
            } else {
                tag.putString("song_identifier", jukeboxPlayable.song().key());
            }
            tag.putBoolean("show_in_tooltip", jukeboxPlayable.showInTooltip());

            backupTag.put("jukebox_playable", tag);
        }

        final AttributeModifiers1_21 attributeModifiers = data.get(StructuredDataKey.ATTRIBUTE_MODIFIERS1_21);
        if (attributeModifiers != null) {
            final ListTag<StringTag> attributeIds = new ListTag<>(StringTag.class);
            for (final AttributeModifier modifier : attributeModifiers.modifiers()) {
                attributeIds.add(new StringTag(modifier.modifier().id()));
            }
            backupTag.put("attribute_modifiers", attributeIds);
        }

        if (!backupTag.isEmpty()) {
            saveTag(createCustomTag(item), backupTag, "inconvertible_data");
        }
    }

    private void restoreInconvertibleData(final Item item) {
        final StructuredDataContainer data = item.dataContainer();
        final CompoundTag customData = data.get(StructuredDataKey.CUSTOM_DATA);
        if (customData == null || !(customData.remove(nbtTagName("inconvertible_data")) instanceof CompoundTag tag)) {
            return;
        }

        final CompoundTag jukeboxPlayableTag = tag.getCompoundTag("jukebox_playable");
        if (jukeboxPlayableTag != null) {
            final EitherHolder<JukeboxPlayable.JukeboxSong> song;
            final String songIdentifier = tag.getString("song_identifier");
            if (songIdentifier != null) {
                song = EitherHolder.of(songIdentifier);
            } else {
                song = EitherHolder.of(restoreHolder(jukeboxPlayableTag, "song", songTag -> {
                    final Holder<SoundEvent> soundEvent = restoreSoundEventHolder(songTag);
                    final Tag description = songTag.get("description");
                    final float lengthInSeconds = songTag.getFloat("length_in_seconds");
                    final int comparatorOutput = songTag.getInt("comparator_output");
                    return new JukeboxPlayable.JukeboxSong(soundEvent, description, lengthInSeconds, comparatorOutput);
                }));
            }

            final JukeboxPlayable jukeboxPlayable = new JukeboxPlayable(song, tag.getBoolean("show_in_tooltip"));
            data.set(StructuredDataKey.JUKEBOX_PLAYABLE1_21, jukeboxPlayable);
        }

        final ListTag<StringTag> attributeIds = tag.getListTag("attribute_modifiers", StringTag.class);
        final AttributeModifiers1_21 attributeModifiers = data.get(StructuredDataKey.ATTRIBUTE_MODIFIERS1_21);
        if (attributeIds != null && attributeModifiers != null && attributeIds.size() == attributeModifiers.modifiers().length) {
            for (int i = 0; i < attributeIds.size(); i++) {
                final String id = attributeIds.get(i).getValue();
                final AttributeModifier modifier = attributeModifiers.modifiers()[i];
                final ModifierData updatedModifierData = new ModifierData(id, modifier.modifier().amount(), modifier.modifier().operation());
                attributeModifiers.modifiers()[i] = new AttributeModifier(modifier.attribute(), updatedModifierData, modifier.slotType());
            }
        }

        removeCustomTag(data, customData);
    }

    private record PendingIdChange(int id, int mappedId, int level) {
    }
}
