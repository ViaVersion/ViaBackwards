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

import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ShortTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LegacyEnchantmentRewriter {

    private final Map<Short, String> enchantmentMappings = new HashMap<>();
    private final String nbtTagName;
    private Set<Short> hideLevelForEnchants;

    public LegacyEnchantmentRewriter(String nbtTagName) {
        this.nbtTagName = nbtTagName;
    }

    public void registerEnchantment(int id, String replacementLore) {
        enchantmentMappings.put((short) id, replacementLore);
    }

    public void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "ench";
        ListTag enchantments = tag.get(key);
        ListTag remappedEnchantments = new ListTag(CompoundTag.class);
        List<Tag> lore = new ArrayList<>();
        for (Tag enchantmentEntry : enchantments.copy()) {
            Tag idTag = ((CompoundTag) enchantmentEntry).get("id");
            if (idTag == null) continue;

            short newId = ((NumberTag) idTag).asShort();
            String enchantmentName = enchantmentMappings.get(newId);
            if (enchantmentName != null) {
                enchantments.remove(enchantmentEntry);
                short level = ((NumberTag) ((CompoundTag) enchantmentEntry).get("lvl")).asShort();
                if (hideLevelForEnchants != null && hideLevelForEnchants.contains(newId)) {
                    lore.add(new StringTag(enchantmentName));
                } else {
                    lore.add(new StringTag(enchantmentName + " " + EnchantmentRewriter.getRomanNumber(level)));
                }
                remappedEnchantments.add(enchantmentEntry);
            }
        }
        if (!lore.isEmpty()) {
            if (!storedEnchant && enchantments.size() == 0) {
                CompoundTag dummyEnchantment = new CompoundTag();
                dummyEnchantment.put("id", new ShortTag((short) 0));
                dummyEnchantment.put("lvl", new ShortTag((short) 0));
                enchantments.add(dummyEnchantment);

                tag.put(nbtTagName + "|dummyEnchant", new ByteTag());

                IntTag hideFlags = tag.get("HideFlags");
                if (hideFlags == null) {
                    hideFlags = new IntTag();
                } else {
                    tag.put(nbtTagName + "|oldHideFlags", new IntTag(hideFlags.asByte()));
                }

                int flags = hideFlags.asByte() | 1;
                hideFlags.setValue(flags);
                tag.put("HideFlags", hideFlags);
            }

            tag.put(nbtTagName + "|" + key, remappedEnchantments);

            CompoundTag display = tag.get("display");
            if (display == null) {
                tag.put("display", display = new CompoundTag());
            }
            ListTag loreTag = display.get("Lore");
            if (loreTag == null) {
                display.put("Lore", loreTag = new ListTag(StringTag.class));
            }

            lore.addAll(loreTag.getValue());
            loreTag.setValue(lore);
        }
    }

    public void rewriteEnchantmentsToServer(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "ench";
        ListTag remappedEnchantments = tag.remove(nbtTagName + "|" + key);
        ListTag enchantments = tag.get(key);
        if (enchantments == null) {
            enchantments = new ListTag(CompoundTag.class);
        }

        if (!storedEnchant && tag.remove(nbtTagName + "|dummyEnchant") != null) {
            for (Tag enchantment : enchantments.copy()) {
                short id = ((NumberTag) ((CompoundTag) enchantment).get("id")).asShort();
                short level = ((NumberTag) ((CompoundTag) enchantment).get("lvl")).asShort();
                if (id == 0 && level == 0) {
                    enchantments.remove(enchantment);
                }
            }

            IntTag hideFlags = tag.remove(nbtTagName + "|oldHideFlags");
            if (hideFlags != null) {
                tag.put("HideFlags", new IntTag(hideFlags.asByte()));
            } else {
                tag.remove("HideFlags");
            }
        }

        CompoundTag display = tag.get("display");
        // A few null checks just to be safe, though they shouldn't actually be
        ListTag lore = display != null ? display.get("Lore") : null;
        for (Tag enchantment : remappedEnchantments.copy()) {
            enchantments.add(enchantment);
            if (lore != null && lore.size() != 0) {
                lore.remove(lore.get(0));
            }
        }
        if (lore != null && lore.size() == 0) {
            display.remove("Lore");
            if (display.isEmpty()) {
                tag.remove("display");
            }
        }

        tag.put(key, enchantments);
    }

    public void setHideLevelForEnchants(int... enchants) {
        this.hideLevelForEnchants = new HashSet<>();
        for (int enchant : enchants) {
            hideLevelForEnchants.add((short) enchant);
        }
    }
}
