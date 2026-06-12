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
package com.viaversion.viabackwards.utils;

public class VelocityUtil {

    public static short toLegacyVelocity(double value) {
        // Round instead of truncating toward zero. The 1.21.9+ LP-vector encoding is lossy: 0.4 b/t
        // decodes as 0.39998779, and (long)(value * 8000) floors that to 3199 instead of 3200. Rounding
        // restores the intended legacy short and matches a native 1.8 server, which sends 0.4 directly
        // as (int)(0.4 * 8000) = 3200.
        return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, Math.round(value * 8000)));
    }

}
