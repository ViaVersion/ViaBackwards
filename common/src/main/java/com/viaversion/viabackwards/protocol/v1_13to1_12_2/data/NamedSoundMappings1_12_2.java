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
package com.viaversion.viabackwards.protocol.v1_13to1_12_2.data;

import com.viaversion.viaversion.protocols.v1_12_2to1_13.data.NamedSoundMappings1_13;
import com.viaversion.viaversion.util.Key;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class NamedSoundMappings1_12_2 {
    private static final Map<String, String> SOUNDS = new HashMap<>();

    static {
        try {
            Field field = NamedSoundMappings1_13.class.getDeclaredField("oldToNew");
            field.setAccessible(true);
            Map<String, String> sounds = (Map<String, String>) field.get(null);
            sounds.forEach((sound1_12, sound1_13) -> NamedSoundMappings1_12_2.SOUNDS.put(sound1_13, sound1_12));
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    public static String getOldId(String sound1_13) {
        return SOUNDS.get(Key.stripMinecraftNamespace(sound1_13));
    }
}
