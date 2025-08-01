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

import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.minecraft.item.Item;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LegacyEnchantmentRewriter {

    private final Map<Short, String> enchantmentMappings = new HashMap<>();
    private final String nbtTagName;
    private final boolean dummyEnchantment;

    private Set<Short> hideLevelForEnchants;

    public LegacyEnchantmentRewriter(String nbtTagName) {
        this(nbtTagName, true);
    }

    public LegacyEnchantmentRewriter(String nbtTagName, boolean dummyEnchantment) {
        this.nbtTagName = nbtTagName;
        this.dummyEnchantment = dummyEnchantment;
    }

    public void registerEnchantment(int id, String replacementLore) {
        enchantmentMappings.put((short) id, replacementLore);
    }

    public void handleToClient(Item item) {
        CompoundTag tag = item.tag();
        if (tag == null) return;

        if (tag.getListTag("ench") != null) {
            rewriteEnchantmentsToClient(tag, false);
        }
        if (tag.getListTag("StoredEnchantments") != null) {
            rewriteEnchantmentsToClient(tag, true);
        }
    }

    public void handleToServer(Item item) {
        CompoundTag tag = item.tag();
        if (tag == null) return;

        if (tag.getListTag(nbtTagName + "|ench", CompoundTag.class) != null) {
            rewriteEnchantmentsToServer(tag, false);
        }
        if (tag.getListTag(nbtTagName + "|StoredEnchantments", CompoundTag.class) != null) {
            rewriteEnchantmentsToServer(tag, true);
        }
    }

    public void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "ench";
        ListTag<CompoundTag> enchantments = tag.getListTag(key, CompoundTag.class);
        ListTag<CompoundTag> remappedEnchantments = new ListTag<>(CompoundTag.class);
        List<StringTag> lore = new ArrayList<>();
        for (CompoundTag enchantmentEntry : enchantments.copy()) {
            NumberTag idTag = enchantmentEntry.getNumberTag("id");
            if (idTag == null) continue;

            short newId = idTag.asShort();
            String enchantmentName = enchantmentMappings.get(newId);
            if (enchantmentName != null) {
                enchantments.remove(enchantmentEntry);
                NumberTag levelTag = enchantmentEntry.getNumberTag("lvl");
                short level = levelTag != null ? levelTag.asShort() : 1;
                if (hideLevelForEnchants != null && hideLevelForEnchants.contains(newId)) {
                    lore.add(new StringTag(enchantmentName));
                } else {
                    lore.add(new StringTag(enchantmentName + " " + EnchantmentRewriter.getRomanNumber(level)));
                }
                remappedEnchantments.add(enchantmentEntry);
            }
        }
        if (!lore.isEmpty()) {
            if (this.dummyEnchantment && !storedEnchant && enchantments.isEmpty()) {
                CompoundTag dummyEnchantment = new CompoundTag();
                dummyEnchantment.putShort("id", (short) 0);
                dummyEnchantment.putShort("lvl", (short) 0);

                enchantments.add(dummyEnchantment);
                tag.put(nbtTagName + "|dummyEnchant", new ByteTag(false));

                NumberTag hideFlags = tag.getNumberTag("HideFlags");
                if (hideFlags == null) {
                    hideFlags = new IntTag();
                } else {
                    tag.putInt(nbtTagName + "|oldHideFlags", hideFlags.asByte());
                }

                int flags = hideFlags.asByte() | 1;
                tag.putInt("HideFlags", flags);
            }

            tag.put(nbtTagName + "|" + key, remappedEnchantments);

            CompoundTag display = tag.getCompoundTag("display");
            if (display == null) {
                tag.put("display", display = new CompoundTag());
            }
            ListTag<StringTag> loreTag = display.getListTag("Lore", StringTag.class);
            if (loreTag == null) {
                display.put("Lore", loreTag = new ListTag<>(StringTag.class));
            }

            lore.addAll(loreTag.getValue());
            loreTag.setValue(lore);
        }
    }

    public void rewriteEnchantmentsToServer(CompoundTag tag, boolean storedEnchant) {
        String key = storedEnchant ? "StoredEnchantments" : "ench";
        ListTag<CompoundTag> enchantments = tag.getListTag(key, CompoundTag.class);
        if (enchantments == null) {
            enchantments = new ListTag<>(CompoundTag.class);
        }

        if (!storedEnchant && tag.remove(nbtTagName + "|dummyEnchant") != null) {
            for (CompoundTag enchantment : enchantments.copy()) {
                NumberTag idTag = enchantment.getNumberTag("id");
                NumberTag levelTag = enchantment.getNumberTag("lvl");
                short id = idTag != null ? idTag.asShort() : 0;
                short level = levelTag != null ? levelTag.asShort() : 0;
                if (id == 0 && level == 0) {
                    enchantments.remove(enchantment);
                }
            }

            Tag hideFlags = tag.remove(nbtTagName + "|oldHideFlags");
            if (hideFlags instanceof IntTag intTag) {
                tag.putInt("HideFlags", intTag.asByte());
            } else {
                tag.remove("HideFlags");
            }
        }

        CompoundTag display = tag.getCompoundTag("display");
        // A few null checks just to be safe, though they shouldn't actually be
        ListTag<StringTag> lore = display != null ? display.getListTag("Lore", StringTag.class) : null;
        ListTag<CompoundTag> remappedEnchantments = (ListTag<CompoundTag>) tag.remove(nbtTagName + "|" + key);
        for (CompoundTag enchantment : remappedEnchantments.copy()) {
            enchantments.add(enchantment);
            if (lore != null && !lore.isEmpty()) {
                lore.remove(lore.get(0));
            }
        }
        if (lore != null && lore.isEmpty()) {
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
