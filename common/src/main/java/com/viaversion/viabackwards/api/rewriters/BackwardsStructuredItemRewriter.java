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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.FloatTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.data.MappedItem;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.EitherHolder;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.CustomModelData1_21_4;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.rewriter.StructuredItemRewriter;
import com.viaversion.viaversion.util.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BackwardsStructuredItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends StructuredItemRewriter<C, S, T> {

    private static final int[] EMPTY_INT_ARRAY = new int[0];

    public BackwardsStructuredItemRewriter(T protocol) {
        super(protocol);
    }

    @Override
    protected void backupInconvertibleData(final UserConnection connection, final Item item, final StructuredDataContainer dataContainer, final CompoundTag backupTag) {
        super.backupInconvertibleData(connection, item, dataContainer, backupTag);

        final BackwardsMappingData mappingData = protocol.getMappingData();
        final MappedItem mappedItem = mappingData != null ? mappingData.getMappedItem(item.identifier()) : null;
        if (mappedItem == null) {
            return;
        }

        final CompoundTag customTag = createCustomTag(item);
        customTag.putInt(nbtTagName("id"), item.identifier()); // Save original id

        // Add custom model data
        if (mappedItem.customModelData() != null) {
            if (connection.getProtocolInfo().protocolVersion().newerThanOrEqualTo(ProtocolVersion.v1_21_4)) {
                if (!dataContainer.has(StructuredDataKey.CUSTOM_MODEL_DATA1_21_4)) {
                    dataContainer.set(StructuredDataKey.CUSTOM_MODEL_DATA1_21_4, new CustomModelData1_21_4(
                        new float[]{mappedItem.customModelData().floatValue()},
                        new boolean[0],
                        new String[0],
                        EMPTY_INT_ARRAY
                    ));
                }
            } else if (!dataContainer.has(StructuredDataKey.CUSTOM_MODEL_DATA1_20_5)) {
                dataContainer.set(StructuredDataKey.CUSTOM_MODEL_DATA1_20_5, mappedItem.customModelData());
            }
        }

        // Set custom name - only done if there is no original one
        if (!dataContainer.has(StructuredDataKey.CUSTOM_NAME)) {
            dataContainer.set(StructuredDataKey.CUSTOM_NAME, mappedItem.tagName());
            customTag.putBoolean(nbtTagName("added_custom_name"), true);
        }
    }

    @Override
    protected void restoreBackupData(final Item item, final StructuredDataContainer container, final CompoundTag customData) {
        super.restoreBackupData(item, container, customData);
        if (removeBackupTag(customData, "id") instanceof final IntTag originalTag) {
            item.setIdentifier(originalTag.asInt());
            removeCustomTag(container, customData);
        }
    }

    protected void saveListTag(CompoundTag tag, ListTag<?> original, String name) {
        // Multiple places might try to backup data
        String backupName = nbtTagName(name);
        if (!tag.contains(backupName)) {
            tag.put(backupName, original.copy());
        }
    }

    public <T extends Tag> @Nullable ListTag<T> removeListTag(CompoundTag tag, String tagName, Class<T> tagType) {
        String backupName = nbtTagName(tagName);
        ListTag<T> data = tag.getListTag(backupName, tagType);
        if (data == null) {
            return null;
        }

        tag.remove(backupName);
        return data;
    }

    protected void saveGenericTagList(CompoundTag tag, List<Tag> original, String name) {
        // List tags cannot contain tags of different types, so we have to store them a bit more awkwardly as an indexed compound tag
        String backupName = nbtTagName(name);
        if (!tag.contains(backupName)) {
            CompoundTag output = new CompoundTag();
            for (int i = 0; i < original.size(); i++) {
                output.put(Integer.toString(i), original.get(i));
            }
            tag.put(backupName, output);
        }
    }

    protected List<Tag> removeGenericTagList(CompoundTag tag, String name) {
        String backupName = nbtTagName(name);
        CompoundTag data = tag.getCompoundTag(backupName);
        if (data == null) {
            return null;
        }

        tag.remove(backupName);
        return new ArrayList<>(data.values());
    }

    protected Tag holderSetToTag(final HolderSet set) {
        if (set.hasIds()) {
            return new IntArrayTag(set.ids());
        } else {
            return new StringTag(set.tagKey());
        }
    }

    protected HolderSet restoreHolderSet(final CompoundTag tag, final String key) {
        final Tag savedTag = tag.get(key);
        if (savedTag == null) {
            return HolderSet.of(EMPTY_INT_ARRAY);
        }

        if (savedTag instanceof StringTag tagKey) {
            return HolderSet.of(tagKey.getValue());
        } else if (savedTag instanceof IntArrayTag idsTag) {
            return HolderSet.of(idsTag.getValue());
        } else {
            return HolderSet.of(EMPTY_INT_ARRAY);
        }
    }

    protected <V> Tag holderToTag(final Holder<V> holder, final BiConsumer<V, CompoundTag> valueSaveFunction) {
        if (holder.hasId()) {
            return new IntTag(holder.id());
        } else {
            final CompoundTag savedTag = new CompoundTag();
            valueSaveFunction.accept(holder.value(), savedTag);
            return savedTag;
        }
    }

    protected <V> Tag eitherHolderToTag(final EitherHolder<V> holder, final BiConsumer<V, CompoundTag> valueSaveFunction) {
        if (holder.hasKey()) {
            return new StringTag(holder.key());
        } else {
            return holderToTag(holder.holder(), valueSaveFunction);
        }
    }

    protected <V> void saveEitherHolderData(final StructuredDataKey<EitherHolder<V>> key, final StructuredDataContainer data, final CompoundTag backupTag, final BiConsumer<V, CompoundTag> valueSaveFunction) {
        final EitherHolder<V> holder = data.get(key);
        if (holder != null) {
            backupTag.put(key.identifier(), eitherHolderToTag(holder, valueSaveFunction));
        }
    }

    protected <V> void saveHolderData(final StructuredDataKey<Holder<V>> key, final StructuredDataContainer data, final CompoundTag backupTag, final BiConsumer<V, CompoundTag> valueSaveFunction) {
        final Holder<V> holder = data.get(key);
        if (holder != null) {
            backupTag.put(key.identifier(), holderToTag(holder, valueSaveFunction));
        }
    }

    protected <V> Holder<V> restoreHolder(final CompoundTag tag, final String key, final Function<CompoundTag, V> valueRestoreFunction) {
        final Tag savedTag = tag.get(key);
        if (savedTag == null) {
            return Holder.of(0);
        }

        if (savedTag instanceof IntTag idTag) {
            return Holder.of(idTag.asInt());
        } else if (savedTag instanceof CompoundTag compoundTag) {
            return Holder.of(valueRestoreFunction.apply(compoundTag));
        } else {
            return Holder.of(0);
        }
    }

    protected <V> EitherHolder<V> restoreEitherHolder(final CompoundTag tag, final String key, final Function<CompoundTag, V> valueRestoreFunction) {
        final Tag savedTag = tag.get(key);
        if (savedTag == null) {
            return EitherHolder.of(Holder.of(0));
        }

        if (savedTag instanceof StringTag keyTag) {
            return EitherHolder.of(keyTag.getValue());
        } else {
            return EitherHolder.of(restoreHolder(tag, key, valueRestoreFunction));
        }
    }

    protected <V> void restoreHolderData(final StructuredDataKey<Holder<V>> key, final StructuredDataContainer data, final CompoundTag backupTag, final Function<CompoundTag, V> valueRestoreFunction) {
        if (backupTag.contains(key.identifier())) {
            data.set(key, restoreHolder(backupTag, key.identifier(), valueRestoreFunction));
        }
    }

    protected void saveStringData(final StructuredDataKey<String> key, final StructuredDataContainer data, final CompoundTag backupTag) {
        final String value = data.get(key);
        if (value != null) {
            backupTag.putString(key.identifier(), value);
        }
    }

    protected void restoreStringData(final StructuredDataKey<String> key, final StructuredDataContainer data, final CompoundTag backupTag) {
        final String value = backupTag.getString(key.identifier());
        if (value != null) {
            data.set(key, value);
        }
    }

    protected void saveKeyData(final StructuredDataKey<Key> key, final StructuredDataContainer data, final CompoundTag backupTag) {
        final Key value = data.get(key);
        if (value != null) {
            backupTag.putString(key.identifier(), value.original());
        }
    }

    protected void restoreKeyData(final StructuredDataKey<Key> key, final StructuredDataContainer data, final CompoundTag backupTag) {
        final String value = backupTag.getString(key.identifier());
        if (value != null) {
            data.set(key, Key.of(value));
        }
    }

    protected void saveIntData(final StructuredDataKey<Integer> key, final StructuredDataContainer data, final CompoundTag backupTag) {
        final Integer variant = data.get(key);
        if (variant != null) {
            backupTag.putInt(key.identifier(), variant);
        }
    }

    protected void restoreIntData(final StructuredDataKey<Integer> key, final StructuredDataContainer data, final CompoundTag backupTag) {
        final IntTag variant = backupTag.getIntTag(key.identifier());
        if (variant != null) {
            data.set(key, variant.asInt());
        }
    }

    protected void saveFloatData(final StructuredDataKey<Float> key, final StructuredDataContainer data, final CompoundTag backupTag) {
        final Float variant = data.get(key);
        if (variant != null) {
            backupTag.putFloat(key.identifier(), variant);
        }
    }

    protected void restoreFloatData(final StructuredDataKey<Float> key, final StructuredDataContainer data, final CompoundTag backupTag) {
        final FloatTag variant = backupTag.getFloatTag(key.identifier());
        if (variant != null) {
            data.set(key, variant.asFloat());
        }
    }

    protected void saveSoundEventHolder(final CompoundTag tag, final Holder<SoundEvent> holder) {
        tag.put("sound_event", holderToTag(holder, this::saveSoundEvent));
    }

    protected void saveSoundEvent(final SoundEvent soundEvent, final CompoundTag tag) {
        tag.putString("identifier", soundEvent.identifier());
        if (soundEvent.fixedRange() != null) {
            tag.putFloat("fixed_range", soundEvent.fixedRange());
        }
    }

    protected Holder<SoundEvent> restoreSoundEventHolder(final CompoundTag tag) {
        return restoreSoundEventHolder(tag, "sound_event");
    }

    protected Holder<SoundEvent> restoreSoundEventHolder(final CompoundTag tag, final String key) {
        return restoreHolder(tag, key, soundEventTag -> {
            final String identifier = soundEventTag.getString("identifier");
            final FloatTag fixedRange = soundEventTag.getFloatTag("fixed_range");
            return new SoundEvent(identifier, fixedRange != null ? fixedRange.asFloat() : null);
        });
    }

    @Override
    public String nbtTagName() {
        return "VB|" + protocol.getClass().getSimpleName();
    }
}
