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
package com.viaversion.viabackwards.protocol.v1_12to1_11_1.data;

import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;

public class MapColorMapping {
    private static final Int2IntMap MAPPING = new Int2IntOpenHashMap(64, 0.99F);

    static {
        MAPPING.defaultReturnValue(-1);
        MAPPING.put(144, 59); // (148, 124, 114) -> (148, 124, 114)
        MAPPING.put(145, 56); // (180, 153, 139) -> (180, 153, 139)
        MAPPING.put(146, 56); // (209, 177, 161) -> (209, 177, 161)
        MAPPING.put(147, 45); // (111, 94, 85) -> (111, 94, 85)
        MAPPING.put(148, 63); // (112, 58, 25) -> (112, 58, 25)
        MAPPING.put(149, 60); // (137, 71, 31) -> (137, 71, 31)
        MAPPING.put(150, 60); // (159, 82, 36) -> (159, 82, 36)
        MAPPING.put(151, 136); // (84, 43, 19) -> (84, 43, 19)
        MAPPING.put(152, 83); // (105, 61, 76) -> (105, 61, 76)
        MAPPING.put(153, 83); // (129, 75, 93) -> (129, 75, 93)
        MAPPING.put(154, 80); // (149, 87, 108) -> (149, 87, 108)
        MAPPING.put(155, 115); // (79, 46, 57) -> (79, 46, 57)
        MAPPING.put(156, 39); // (79, 76, 97) -> (79, 76, 97)
        MAPPING.put(157, 39); // (97, 93, 119) -> (97, 93, 119)
        MAPPING.put(158, 36); // (112, 108, 138) -> (112, 108, 138)
        MAPPING.put(159, 47); // (59, 57, 73) -> (59, 57, 73)
        MAPPING.put(160, 60); // (131, 94, 25) -> (131, 94, 25)
        MAPPING.put(161, 61); // (160, 115, 31) -> (160, 115, 31)
        MAPPING.put(162, 62); // (186, 133, 36) -> (186, 133, 36)
        MAPPING.put(163, 137); // (98, 70, 19) -> (98, 70, 19)
        MAPPING.put(164, 108); // (73, 83, 37) -> (73, 83, 37)
        MAPPING.put(165, 108); // (89, 101, 46) -> (89, 101, 46)
        MAPPING.put(166, 109); // (103, 117, 53) -> (103, 117, 53)
        MAPPING.put(167, 111); // (55, 62, 28) -> (55, 62, 28)
        MAPPING.put(168, 112); // (113, 54, 55) -> (113, 54, 55)
        MAPPING.put(169, 113); // (138, 66, 67) -> (138, 66, 67)
        MAPPING.put(170, 114); // (160, 77, 78) -> (160, 77, 78)
        MAPPING.put(171, 115); // (85, 41, 41) -> (85, 41, 41)
        MAPPING.put(172, 118); // (40, 29, 25) -> (40, 29, 25)
        MAPPING.put(173, 107); // (49, 35, 30) -> (49, 35, 30)
        MAPPING.put(174, 107); // (57, 41, 35) -> (57, 41, 35)
        MAPPING.put(175, 118); // (30, 22, 19) -> (30, 22, 19)
        MAPPING.put(176, 91); // (95, 76, 69) -> (95, 76, 69)
        MAPPING.put(177, 45); // (116, 92, 85) -> (116, 92, 85)
        MAPPING.put(178, 46); // (135, 107, 98) -> (135, 107, 98)
        MAPPING.put(179, 47); // (71, 57, 52) -> (71, 57, 52)
        MAPPING.put(180, 85); // (61, 65, 65) -> (61, 65, 65)
        MAPPING.put(181, 44); // (75, 79, 79) -> (75, 79, 79)
        MAPPING.put(182, 27); // (87, 92, 92) -> (87, 92, 92)
        MAPPING.put(183, 84); // (46, 49, 49) -> (46, 49, 49)
        MAPPING.put(184, 83); // (86, 52, 62) -> (86, 52, 62)
        MAPPING.put(185, 83); // (105, 63, 76) -> (105, 63, 76)
        MAPPING.put(186, 83); // (122, 73, 88) -> (122, 73, 88)
        MAPPING.put(187, 84); // (65, 39, 47) -> (65, 39, 47)
        MAPPING.put(188, 84); // (54, 44, 65) -> (54, 44, 65)
        MAPPING.put(189, 71); // (66, 53, 79) -> (66, 53, 79)
        MAPPING.put(190, 71); // (76, 62, 92) -> (76, 62, 92)
        MAPPING.put(191, 87); // (40, 33, 49) -> (40, 33, 49)
        MAPPING.put(192, 107); // (54, 35, 25) -> (54, 35, 25)
        MAPPING.put(193, 139); // (66, 43, 30) -> (66, 43, 30)
        MAPPING.put(194, 43); // (76, 50, 35) -> (76, 50, 35)
        MAPPING.put(195, 107); // (40, 26, 19) -> (40, 26, 19)
        MAPPING.put(196, 111); // (54, 58, 30) -> (54, 58, 30)
        MAPPING.put(197, 111); // (66, 71, 36) -> (66, 71, 36)
        MAPPING.put(198, 111); // (76, 82, 42) -> (76, 82, 42)
        MAPPING.put(199, 107); // (40, 43, 22) -> (40, 43, 22)
        MAPPING.put(200, 112); // (100, 42, 32) -> (100, 42, 32)
        MAPPING.put(201, 113); // (123, 52, 40) -> (123, 52, 40)
        MAPPING.put(202, 113); // (142, 60, 46) -> (142, 60, 46)
        MAPPING.put(203, 115); // (75, 32, 24) -> (75, 32, 24)
        MAPPING.put(204, 116); // (26, 16, 11) -> (26, 16, 11)
        MAPPING.put(205, 117); // (32, 19, 14) -> (32, 19, 14)
        MAPPING.put(206, 107); // (37, 22, 16) -> (37, 22, 16)
        MAPPING.put(207, 119); // (20, 12, 8) -> (20, 12, 8)
    }

    public static int getNearestOldColor(int color) {
        return MAPPING.getOrDefault(color, color);
    }
}
