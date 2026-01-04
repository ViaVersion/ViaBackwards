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

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.utils.ChatUtil;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.util.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Rewriter to handle the addition of new enchantments.
 */
public class EnchantmentRewriter {

    public static final String ENCHANTMENT_LEVEL_TRANSLATION = "enchantment.level.%s";

    protected final Map<String, String> enchantmentMappings = new HashMap<>();
    protected final BackwardsItemRewriter<?, ?, ?> itemRewriter;
    private final boolean jsonFormat;

    public EnchantmentRewriter(BackwardsItemRewriter<?, ?, ?> itemRewriter, boolean jsonFormat) {
        this.itemRewriter = itemRewriter;
        this.jsonFormat = jsonFormat;
    }

    public EnchantmentRewriter(BackwardsItemRewriter<?, ?, ?> itemRewriter) {
        this(itemRewriter, true);
    }

    public void registerEnchantment(String key, String replacementLore) {
        enchantmentMappings.put(Key.stripMinecraftNamespace(key), replacementLore);
    }

    public void handleToClient(Item item) {
        CompoundTag tag = item.tag();
        if (tag == null) return;

        if (tag.getListTag("Enchantments") != null) {
            rewriteEnchantmentsToClient(tag, false);
        }
        if (tag.getListTag("StoredEnchantments") != null) {
            rewriteEnchantmentsToClient(tag, true);
        }
    }

    public void handleToServer(Item item) {
        CompoundTag tag = item.tag();
        if (tag == null) return;

        if (tag.contains(itemRewriter.nbtTagName("Enchantments"))) {
            rewriteEnchantmentsToServer(tag, false);
        }
        if (tag.contains(itemRewriter.nbtTagName("StoredEnchantments"))) {
            rewriteEnchantmentsToServer(tag, true);
        }
    }

    public void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "Enchantments";
        ListTag<CompoundTag> enchantments = tag.getListTag(key, CompoundTag.class);
        List<StringTag> loreToAdd = new ArrayList<>();
        boolean changed = false;

        Iterator<CompoundTag> iterator = enchantments.iterator();
        while (iterator.hasNext()) {
            CompoundTag enchantmentEntry = iterator.next();
            StringTag idTag = enchantmentEntry.getStringTag("id");
            if (idTag == null) {
                continue;
            }

            String enchantmentId = Key.stripMinecraftNamespace(idTag.getValue());
            String remappedName = enchantmentMappings.get(enchantmentId);
            if (remappedName != null) {
                if (!changed) {
                    // Backup original before doing modifications
                    itemRewriter.saveListTag(tag, enchantments, key);
                    changed = true;
                }

                iterator.remove();

                NumberTag levelTag = enchantmentEntry.getNumberTag("lvl");
                int level = levelTag != null ? levelTag.asInt() : 1;
                String loreValue;
                if (jsonFormat) {
                    loreValue = ChatUtil.legacyToJsonString(remappedName, ENCHANTMENT_LEVEL_TRANSLATION.formatted(level), true);
                } else {
                    loreValue = remappedName + " " + getRomanNumber(level);
                }

                loreToAdd.add(new StringTag(loreValue));
            }
        }

        if (!loreToAdd.isEmpty()) {
            // Add dummy enchant for the glow effect if there are no actual enchantments left
            if (!storedEnchant && enchantments.isEmpty()) {
                CompoundTag dummyEnchantment = new CompoundTag();
                dummyEnchantment.putString("id", "");
                dummyEnchantment.putShort("lvl", (short) 0);
                enchantments.add(dummyEnchantment);
            }

            CompoundTag display = tag.getCompoundTag("display");
            if (display == null) {
                tag.put("display", display = new CompoundTag());
            }

            ListTag<StringTag> loreTag = display.getListTag("Lore", StringTag.class);
            if (loreTag == null) {
                display.put("Lore", loreTag = new ListTag<>(StringTag.class));
            } else {
                // Save original lore
                itemRewriter.saveListTag(display, loreTag, "Lore");
            }

            loreToAdd.addAll(loreTag.getValue());
            loreTag.setValue(loreToAdd);
        }
    }

    public void rewriteEnchantmentsToServer(CompoundTag tag, boolean storedEnchant) {
        // Just restore the original tag ig present (lore is always restored in the item rewriter)
        String key = storedEnchant ? "StoredEnchantments" : "Enchantments";
        itemRewriter.restoreListTag(tag, key);
    }

    public static String getRomanNumber(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> ENCHANTMENT_LEVEL_TRANSLATION.formatted(number); // Fallback to translation to match vanilla style
        };
    }
}
