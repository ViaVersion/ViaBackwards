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
package com.viaversion.viabackwards.provider;

import com.viaversion.viabackwards.protocol.protocol1_20to1_20_2.provider.AdvancementCriteriaProvider;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;

public final class BukkitAdvancementCriteriaProvider extends AdvancementCriteriaProvider {

    @Override
    public String[] getCriteria(final String key) {
        final Advancement advancement = Bukkit.getAdvancement(NamespacedKey.fromString(key));
        if (advancement == null) {
            return null;
        }
        return advancement.getCriteria().toArray(new String[0]);
    }
}
