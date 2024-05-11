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

import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.rewriter.IdRewriteFunction;

public final class MapColorRewriter {

    /**
     * Returns a packethandler to rewrite map data color ids. Reading starts from the icon count.
     *
     * @param rewriter id rewriter returning mapped colors, or -1 if unmapped
     * @return packethandler to rewrite map data color ids
     */
    public static PacketHandler getRewriteHandler(IdRewriteFunction rewriter) {
        return wrapper -> {
            int iconCount = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < iconCount; i++) {
                wrapper.passthrough(Types.VAR_INT); // Type
                wrapper.passthrough(Types.BYTE); // X
                wrapper.passthrough(Types.BYTE); // Z
                wrapper.passthrough(Types.BYTE); // Direction
                wrapper.passthrough(Types.OPTIONAL_COMPONENT); // Display Name
            }

            short columns = wrapper.passthrough(Types.UNSIGNED_BYTE);
            if (columns < 1) return;

            wrapper.passthrough(Types.UNSIGNED_BYTE); // Rows
            wrapper.passthrough(Types.UNSIGNED_BYTE); // X
            wrapper.passthrough(Types.UNSIGNED_BYTE); // Z
            byte[] data = wrapper.passthrough(Types.BYTE_ARRAY_PRIMITIVE);
            for (int i = 0; i < data.length; i++) {
                int color = data[i] & 0xFF;
                int mappedColor = rewriter.rewrite(color);
                if (mappedColor != -1) {
                    data[i] = (byte) mappedColor;
                }
            }
        };
    }
}
