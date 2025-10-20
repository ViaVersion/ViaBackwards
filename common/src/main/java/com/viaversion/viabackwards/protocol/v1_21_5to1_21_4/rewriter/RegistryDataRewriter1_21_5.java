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
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import java.util.List;

public final class RegistryDataRewriter1_21_5 extends BackwardsRegistryRewriter {

    public RegistryDataRewriter1_21_5(final BackwardsProtocol<?, ?, ?, ?> protocol) {
        super(protocol);

        final List<String> newRegistries = List.of("pig_variant", "cow_variant", "frog_variant", "cat_variant",
            "chicken_variant", "test_environment", "test_instance", "wolf_sound_variant");
        for (final String registry : newRegistries) {
            remove(registry);
        }
    }

    @Override
    public RegistryEntry[] handle(final UserConnection connection, final String key, final RegistryEntry[] entries) {
        final boolean trimPatternRegistry = key.equals("trim_pattern");
        if (trimPatternRegistry || key.equals("trim_material")) {
            updateTrim(entries, trimPatternRegistry ? "template_item" : "ingredient");
            return super.handle(connection, key, entries);
        }

        if (key.equals("enchantment")) {
            updateEnchantment(entries);
            return super.handle(connection, key, entries);
        }

        if (!key.equals("wolf_variant")) {
            return super.handle(connection, key, entries);
        }

        for (final RegistryEntry entry : entries) {
            if (entry.tag() == null) {
                continue;
            }

            final CompoundTag variant = (CompoundTag) entry.tag();
            final CompoundTag assets = (CompoundTag) variant.remove("assets");
            variant.put("wild_texture", assets.get("wild"));
            variant.put("tame_texture", assets.get("tame"));
            variant.put("angry_texture", assets.get("angry"));
            variant.put("biomes", new ListTag<>(StringTag.class));
        }
        return entries;
    }

    private void updateTrim(final RegistryEntry[] entries, final String itemKey) {
        for (final RegistryEntry entry : entries) {
            if (entry.tag() == null) {
                continue;
            }

            final CompoundTag tag = (CompoundTag) entry.tag();
            tag.putString(itemKey, "stone"); // dummy ingredient
        }
    }

    private void updateEnchantment(final RegistryEntry[] entries) {
        for (final RegistryEntry entry : entries) {
            if (entry.tag() == null) {
                continue;
            }

            final CompoundTag enchantment = (CompoundTag) entry.tag();
            final ListTag<StringTag> slots = enchantment.getListTag("slots", StringTag.class);
            if (slots != null) {
                slots.getValue().removeIf(tag -> tag.getValue().equals("saddle")); // Remove saddle slot
            }
        }
    }
}
