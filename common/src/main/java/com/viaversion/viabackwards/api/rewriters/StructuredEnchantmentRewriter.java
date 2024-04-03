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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.Enchantments;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.IntIntPair;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectIterator;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.util.ComponentUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StructuredEnchantmentRewriter {

    protected final BackwardsItemRewriter<?, ?, ?> itemRewriter;
    private boolean rewriteIds = true;

    public StructuredEnchantmentRewriter(final BackwardsItemRewriter<?, ?, ?> itemRewriter) {
        this.itemRewriter = itemRewriter;
    }

    public void handleToClient(final Item item) {
        final StructuredDataContainer data = item.structuredData();
        rewriteEnchantmentsToClient(data, StructuredDataKey.ENCHANTMENTS, false);
        rewriteEnchantmentsToClient(data, StructuredDataKey.STORED_ENCHANTMENTS, true);
    }

    public void handleToServer(final Item item) {
        final StructuredDataContainer data = item.structuredData();
        final StructuredData<CompoundTag> customData = data.getNonEmpty(StructuredDataKey.CUSTOM_DATA);
        if (customData == null) {
            return;
        }

        final CompoundTag tag = customData.value();
        if (tag.contains(itemRewriter.getNbtTagName() + "|enchantments")) {
            rewriteEnchantmentsToServer(data, tag, StructuredDataKey.ENCHANTMENTS, false);
        }
        if (tag.contains(itemRewriter.getNbtTagName() + "|stored_enchantments")) {
            rewriteEnchantmentsToServer(data, tag, StructuredDataKey.STORED_ENCHANTMENTS, true);
        }
    }

    public void rewriteEnchantmentsToClient(final StructuredDataContainer data, final StructuredDataKey<Enchantments> key, final boolean storedEnchant) {
        final StructuredData<Enchantments> enchantmentsData = data.getNonEmpty(key);
        if (enchantmentsData == null) {
            return;
        }

        final CompoundTag tag = data.computeIfAbsent(StructuredDataKey.CUSTOM_DATA, $ -> new CompoundTag()).value();
        final Enchantments enchantments = enchantmentsData.value();
        final List<Tag> loreToAdd = new ArrayList<>();
        boolean changed = false;

        final ObjectIterator<Int2IntMap.Entry> iterator = enchantments.enchantments().int2IntEntrySet().iterator();
        final List<IntIntPair> updatedIds = new ArrayList<>();
        while (iterator.hasNext()) {
            final Int2IntMap.Entry entry = iterator.next();
            final BackwardsMappings mappingData = itemRewriter.protocol().getMappingData();
            final Mappings mappings = mappingData.getEnchantmentMappings();
            final int mappedId = mappings.getNewId(entry.getIntKey());
            if (mappedId != -1) {
                if (rewriteIds) {
                    // Update the map after to iteration to preserve the current ids before possibly saving the original, avoid CME
                    updatedIds.add(IntIntPair.of(entry.getIntKey(), mappedId));
                }
                continue;
            }

            final String remappedName = mappingData.mappedEnchantmentName(entry.getIntKey());
            if (remappedName != null) {
                if (!changed) {
                    // Backup original before doing modifications
                    itemRewriter.saveListTag(tag, asTag(enchantments), key.identifier());
                    changed = true;
                }

                final int level = entry.getIntValue();
                loreToAdd.add(ComponentUtil.jsonStringToTag(ComponentUtil.legacyToJsonString("§7" + remappedName + " " + EnchantmentRewriter.getRomanNumber(level), true)));
                iterator.remove();
            }
        }

        for (final IntIntPair pair : updatedIds) {
            enchantments.add(pair.firstInt(), pair.secondInt());
        }

        if (loreToAdd.isEmpty()) {
            // No removed enchantments
            return;
        }

        // Add glint override if there are no enchantments left
        if (!storedEnchant && enchantments.size() == 0) {
            final StructuredData<Boolean> glintOverride = data.getNonEmpty(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE);
            if (glintOverride != null) {
                tag.putBoolean(itemRewriter.getNbtTagName() + "|glint", glintOverride.value());
            } else {
                tag.putBoolean(itemRewriter.getNbtTagName() + "|noglint", true);
            }
            data.set(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE, true);
        }

        // Save original lore
        final StructuredData<Tag[]> loreData = data.getNonEmpty(StructuredDataKey.LORE);
        if (loreData != null) {
            final List<Tag> loreList = Arrays.asList(loreData.value());
            itemRewriter.saveGenericTagList(tag, loreList, "lore");
            loreToAdd.addAll(loreList);
        } else {
            tag.putBoolean(itemRewriter.getNbtTagName() + "|nolore", true);
        }

        if (enchantments.showInTooltip()) {
            tag.putBoolean(itemRewriter.getNbtTagName() + "|show_" + key.identifier(), true);
        }

        data.set(StructuredDataKey.LORE, loreToAdd.toArray(new Tag[0]));
    }

    private ListTag<CompoundTag> asTag(final Enchantments enchantments) {
        final ListTag<CompoundTag> listTag = new ListTag<>(CompoundTag.class);
        for (final Int2IntMap.Entry entry : enchantments.enchantments().int2IntEntrySet()) {
            final CompoundTag enchantment = new CompoundTag();
            enchantment.putInt("id", entry.getIntKey());
            enchantment.putInt("lvl", entry.getIntValue());
            listTag.add(enchantment);
        }
        return listTag;
    }

    public void rewriteEnchantmentsToServer(final StructuredDataContainer data, final CompoundTag tag, final StructuredDataKey<Enchantments> key, final boolean storedEnchant) {
        final ListTag<CompoundTag> enchantmentsTag = itemRewriter.removeListTag(tag, key.identifier(), CompoundTag.class);
        if (enchantmentsTag == null) {
            return;
        }

        final Tag glintTag = tag.remove(itemRewriter.getNbtTagName() + "|glint");
        if (glintTag instanceof ByteTag) {
            data.set(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE, ((NumberTag) glintTag).asBoolean());
        } else if (tag.remove(itemRewriter.getNbtTagName() + "|noglint") != null) {
            data.remove(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE);
        }

        final List<Tag> lore = itemRewriter.removeGenericTagList(tag, "lore");
        if (lore != null) {
            data.set(StructuredDataKey.LORE, lore.toArray(new Tag[0]));
        } else if (tag.remove(itemRewriter.getNbtTagName() + "|nolore") != null) {
            data.remove(StructuredDataKey.LORE);
        }

        final Enchantments enchantments = new Enchantments(tag.remove(itemRewriter.getNbtTagName() + "|show_" + key.identifier()) != null);
        for (final CompoundTag enchantment : enchantmentsTag) {
            enchantments.add(enchantment.getInt("id"), enchantment.getInt("lvl"));
        }
        data.set(key, enchantments);
    }

    public void setRewriteIds(final boolean rewriteIds) {
        this.rewriteIds = rewriteIds;
    }
}
