/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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

import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ShortTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ChatRewriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Rewriter to handle the addition of new enchantments.
 */
public class EnchantmentRewriter {

    private final Map<String, String> enchantmentMappings = new HashMap<>();
    private final ItemRewriter<?, ?, ?> itemRewriter;
    private final boolean jsonFormat;

    public EnchantmentRewriter(ItemRewriter<?, ?, ?> itemRewriter, boolean jsonFormat) {
        this.itemRewriter = itemRewriter;
        this.jsonFormat = jsonFormat;
    }

    public EnchantmentRewriter(ItemRewriter<?, ?, ?> itemRewriter) {
        this(itemRewriter, true);
    }

    public void registerEnchantment(String key, String replacementLore) {
        enchantmentMappings.put(key, replacementLore);
    }

    public void handleToClient(Item item) {
        CompoundTag tag = item.tag();
        if (tag == null) return;

        if (tag.get("Enchantments") instanceof ListTag) {
            rewriteEnchantmentsToClient(tag, false);
        }
        if (tag.get("StoredEnchantments") instanceof ListTag) {
            rewriteEnchantmentsToClient(tag, true);
        }
    }

    public void handleToServer(Item item) {
        CompoundTag tag = item.tag();
        if (tag == null) return;

        if (tag.contains(itemRewriter.getNbtTagName() + "|Enchantments")) {
            rewriteEnchantmentsToServer(tag, false);
        }
        if (tag.contains(itemRewriter.getNbtTagName() + "|StoredEnchantments")) {
            rewriteEnchantmentsToServer(tag, true);
        }
    }

    public void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "Enchantments";
        ListTag enchantments = tag.get(key);
        List<Tag> loreToAdd = new ArrayList<>();
        boolean changed = false;

        Iterator<Tag> iterator = enchantments.iterator();
        while (iterator.hasNext()) {
            CompoundTag enchantmentEntry = (CompoundTag) iterator.next();
            Tag idTag = enchantmentEntry.get("id");
            if (!(idTag instanceof StringTag)) continue;

            String enchantmentId = ((StringTag) idTag).getValue();
            String remappedName = enchantmentMappings.get(enchantmentId);
            if (remappedName != null) {
                if (!changed) {
                    // Backup original before doing modifications
                    itemRewriter.saveListTag(tag, enchantments, key);
                    changed = true;
                }

                iterator.remove();

                int level = ((NumberTag) enchantmentEntry.get("lvl")).asInt();
                String loreValue = remappedName + " " + getRomanNumber(level);
                if (jsonFormat) {
                    loreValue = ChatRewriter.legacyTextToJsonString(loreValue);
                }

                loreToAdd.add(new StringTag(loreValue));
            }
        }

        if (!loreToAdd.isEmpty()) {
            // Add dummy enchant for the glow effect if there are no actual enchantments left
            if (!storedEnchant && enchantments.size() == 0) {
                CompoundTag dummyEnchantment = new CompoundTag();
                dummyEnchantment.put("id", new StringTag());
                dummyEnchantment.put("lvl", new ShortTag((short) 0));
                enchantments.add(dummyEnchantment);
            }

            CompoundTag display = tag.get("display");
            if (display == null) {
                tag.put("display", display = new CompoundTag());
            }

            ListTag loreTag = display.get("Lore");
            if (loreTag == null) {
                display.put("Lore", loreTag = new ListTag(StringTag.class));
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
        switch (number) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            case 6:
                return "VI";
            case 7:
                return "VII";
            case 8:
                return "VIII";
            case 9:
                return "IX";
            case 10:
                return "X";
            default:
                return Integer.toString(number);
        }
    }
}
