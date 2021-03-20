/*
 * ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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
package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data;

import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.EntityTypeRewriter;
import us.myles.viaversion.libs.fastutil.ints.Int2IntMap;
import us.myles.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;

import java.lang.reflect.Field;

public class EntityTypeMapping {
    private static final Int2IntMap TYPES = new Int2IntOpenHashMap();

    static {
        TYPES.defaultReturnValue(-1);
        try {
            Field field = EntityTypeRewriter.class.getDeclaredField("ENTITY_TYPES");
            field.setAccessible(true);
            Int2IntMap entityTypes = (Int2IntMap) field.get(null);
            for (Int2IntMap.Entry entry : entityTypes.int2IntEntrySet()) {
                EntityTypeMapping.TYPES.put(entry.getIntValue(), entry.getIntKey());
            }
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    public static int getOldId(int type1_13) {
        return TYPES.get(type1_13);
    }
}
