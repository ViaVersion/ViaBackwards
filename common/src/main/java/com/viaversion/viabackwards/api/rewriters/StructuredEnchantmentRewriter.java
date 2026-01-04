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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.utils.ChatUtil;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.Enchantments;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.objects.ObjectIterator;
import com.viaversion.viaversion.rewriter.IdRewriteFunction;
import com.viaversion.viaversion.util.ComponentUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter.ENCHANTMENT_LEVEL_TRANSLATION;

public class StructuredEnchantmentRewriter {

    protected final BackwardsStructuredItemRewriter<?, ?, ?> itemRewriter;
    private boolean rewriteIds = true;

    public StructuredEnchantmentRewriter(final BackwardsStructuredItemRewriter<?, ?, ?> itemRewriter) {
        this.itemRewriter = itemRewriter;
    }

    public void handleToClient(final Item item) {
        final StructuredDataContainer data = item.dataContainer();
        final BackwardsMappingData mappingData = itemRewriter.protocol().getMappingData();
        final IdRewriteFunction idRewriteFunction = id -> {
            final Mappings mappings = mappingData.getEnchantmentMappings();
            return mappings.getNewId(id);
        };
        final DescriptionSupplier descriptionSupplier = (id, level) -> {
            final String remappedName = mappingData.mappedEnchantmentName(id);
            return ComponentUtil.jsonStringToTag(ChatUtil.legacyToJsonString("§7" + remappedName, ENCHANTMENT_LEVEL_TRANSLATION.formatted(level), true));
        };
        rewriteEnchantmentsToClient(data, StructuredDataKey.ENCHANTMENTS1_20_5, idRewriteFunction, descriptionSupplier, false);
        rewriteEnchantmentsToClient(data, StructuredDataKey.STORED_ENCHANTMENTS1_20_5, idRewriteFunction, descriptionSupplier, true);
    }

    public void handleToServer(final Item item) {
        final StructuredDataContainer data = item.dataContainer();
        final CompoundTag customData = data.get(StructuredDataKey.CUSTOM_DATA);
        if (customData != null) {
            rewriteEnchantmentsToServer(data, customData, StructuredDataKey.ENCHANTMENTS1_20_5);
            rewriteEnchantmentsToServer(data, customData, StructuredDataKey.STORED_ENCHANTMENTS1_20_5);
        }
    }

    public void rewriteEnchantmentsToClient(final StructuredDataContainer data, final StructuredDataKey<Enchantments> key, final IdRewriteFunction rewriteFunction, final DescriptionSupplier descriptionSupplier, final boolean storedEnchant) {
        final Enchantments enchantments = data.get(key);
        if (enchantments == null || enchantments.size() == 0) {
            return;
        }

        final List<Tag> loreToAdd = new ArrayList<>();
        boolean removedEnchantments = false;
        boolean updatedLore = false;

        final ObjectIterator<Int2IntMap.Entry> iterator = enchantments.enchantments().int2IntEntrySet().iterator();
        final List<PendingIdChange> updatedIds = new ArrayList<>();
        while (iterator.hasNext()) {
            final Int2IntMap.Entry entry = iterator.next();
            final int id = entry.getIntKey();
            final int mappedId = rewriteFunction.rewrite(id);
            final int level = entry.getIntValue();
            if (mappedId != -1) {
                if (rewriteIds) {
                    // Update the map after to iteration to preserve the current ids before possibly saving the original, avoid CME
                    updatedIds.add(new PendingIdChange(id, mappedId, level));
                }
                continue;
            }

            if (!removedEnchantments) {
                // Backup original before doing modifications
                final CompoundTag customData = customData(data);
                itemRewriter.saveListTag(customData, asTag(enchantments), key.identifier());
                removedEnchantments = true;
            }

            final Tag description = descriptionSupplier.get(id, level);
            if (description != null && enchantments.showInTooltip()) {
                loreToAdd.add(description);
                updatedLore = true;
            }

            iterator.remove();
        }

        // Remove all first, then add the new ones
        for (final PendingIdChange change : updatedIds) {
            enchantments.remove(change.id());
        }
        for (final PendingIdChange change : updatedIds) {
            enchantments.add(change.mappedId(), change.level());
        }

        if (removedEnchantments) {
            final CompoundTag tag = customData(data);
            if (!storedEnchant && enchantments.size() == 0) {
                // Add glint override if there are no enchantments left
                final Boolean glintOverride = data.get(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE);
                if (glintOverride != null) {
                    tag.putBoolean(itemRewriter.nbtTagName("glint"), glintOverride);
                } else {
                    tag.putBoolean(itemRewriter.nbtTagName("noglint"), true);
                }
                data.set(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE, true);
            }

            if (enchantments.showInTooltip()) {
                tag.putBoolean(itemRewriter.nbtTagName("show_" + key.identifier()), true);
            }
        }

        if (updatedLore) {
            // Save original lore
            final CompoundTag tag = customData(data);
            final Tag[] lore = data.get(StructuredDataKey.LORE);
            if (lore != null) {
                final List<Tag> loreList = Arrays.asList(lore);
                itemRewriter.saveGenericTagList(tag, loreList, "lore");
                loreToAdd.addAll(loreList);
            } else {
                tag.putBoolean(itemRewriter.nbtTagName("nolore"), true);
            }
            data.set(StructuredDataKey.LORE, loreToAdd.toArray(new Tag[0]));
        }
    }

    private CompoundTag customData(final StructuredDataContainer data) {
        CompoundTag tag = data.get(StructuredDataKey.CUSTOM_DATA);
        if (tag == null) {
            tag = new CompoundTag();
            data.set(StructuredDataKey.CUSTOM_DATA, tag);
        }
        return tag;
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

    public void rewriteEnchantmentsToServer(final StructuredDataContainer data, final CompoundTag tag, final StructuredDataKey<Enchantments> key) {
        final ListTag<CompoundTag> enchantmentsTag = itemRewriter.removeListTag(tag, key.identifier(), CompoundTag.class);
        if (enchantmentsTag == null) {
            return;
        }

        final Tag glintTag = tag.remove(itemRewriter.nbtTagName("glint"));
        if (glintTag instanceof ByteTag) {
            data.set(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE, ((NumberTag) glintTag).asBoolean());
        } else if (tag.remove(itemRewriter.nbtTagName("noglint")) != null) {
            data.remove(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE);
        }

        final List<Tag> lore = itemRewriter.removeGenericTagList(tag, "lore");
        if (lore != null) {
            data.set(StructuredDataKey.LORE, lore.toArray(new Tag[0]));
        } else if (tag.remove(itemRewriter.nbtTagName("nolore")) != null) {
            data.remove(StructuredDataKey.LORE);
        }

        final Enchantments enchantments = new Enchantments(tag.remove(itemRewriter.nbtTagName("show_" + key.identifier())) != null);
        for (final CompoundTag enchantment : enchantmentsTag) {
            enchantments.add(enchantment.getInt("id"), enchantment.getInt("lvl"));
        }
        data.set(key, enchantments);
    }

    public void setRewriteIds(final boolean rewriteIds) {
        this.rewriteIds = rewriteIds;
    }

    @FunctionalInterface
    public interface DescriptionSupplier {

        Tag get(int id, int level);
    }

    private record PendingIdChange(int id, int mappedId, int level) {
    }
}
