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
package com.viaversion.viabackwards.protocol.v1_16to1_15_2.rewriter;

import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.Protocol1_16To1_15_2;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ClientboundPackets1_16;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.SerializerVersion;

public class TranslatableRewriter1_16 extends TranslatableRewriter<ClientboundPackets1_16> {

    private static final ChatColor[] COLORS = {
        new ChatColor("black", 0x000000),
        new ChatColor("dark_blue", 0x0000aa),
        new ChatColor("dark_green", 0x00aa00),
        new ChatColor("dark_aqua", 0x00aaaa),
        new ChatColor("dark_red", 0xaa0000),
        new ChatColor("dark_purple", 0xaa00aa),
        new ChatColor("gold", 0xffaa00),
        new ChatColor("gray", 0xaaaaaa),
        new ChatColor("dark_gray", 0x555555),
        new ChatColor("blue", 0x5555ff),
        new ChatColor("green", 0x55ff55),
        new ChatColor("aqua", 0x55ffff),
        new ChatColor("red", 0xff5555),
        new ChatColor("light_purple", 0xff55ff),
        new ChatColor("yellow", 0xffff55),
        new ChatColor("white", 0xffffff)
    };

    public TranslatableRewriter1_16(Protocol1_16To1_15_2 protocol) {
        super(protocol, ReadType.JSON);
    }

    @Override
    public void processText(UserConnection connection, JsonElement value) {
        super.processText(connection, value);

        if (value == null || !value.isJsonObject()) return;

        // c o l o r s
        JsonObject object = value.getAsJsonObject();
        JsonPrimitive color = object.getAsJsonPrimitive("color");
        if (color != null) {
            String colorName = color.getAsString();
            if (!colorName.isEmpty() && colorName.charAt(0) == '#') {
                int rgb = Integer.parseInt(colorName.substring(1), 16);
                String closestChatColor = getClosestChatColor(rgb);
                object.addProperty("color", closestChatColor);
            }
        }

        JsonObject hoverEvent = object.getAsJsonObject("hoverEvent");
        if (hoverEvent == null || !hoverEvent.has("contents")) {
            return;
        }

        // show_text as chat component json, show_entity and show_item serialized as snbt
        JsonObject convertedObject = (JsonObject) ComponentUtil.convertJson(object, SerializerVersion.V1_16, SerializerVersion.V1_15);
        object.add("hoverEvent", convertedObject.getAsJsonObject("hoverEvent"));
    }

    private String getClosestChatColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        ChatColor closest = null;
        int smallestDiff = 0;

        for (ChatColor color : COLORS) {
            if (color.rgb == rgb) {
                return color.colorName;
            }

            // Check by the greatest diff of the 3 values
            int rAverage = (color.r + r) / 2;
            int rDiff = color.r - r;
            int gDiff = color.g - g;
            int bDiff = color.b - b;
            int diff = ((2 + (rAverage >> 8)) * rDiff * rDiff)
                + (4 * gDiff * gDiff)
                + ((2 + ((255 - rAverage) >> 8)) * bDiff * bDiff);
            if (closest == null || diff < smallestDiff) {
                closest = color;
                smallestDiff = diff;
            }
        }
        return closest.colorName;
    }

    private static final class ChatColor {

        private final String colorName;
        private final int rgb;
        private final int r;
        private final int g;
        private final int b;

        ChatColor(String colorName, int rgb) {
            this.colorName = colorName;
            this.rgb = rgb;
            r = (rgb >> 16) & 0xFF;
            g = (rgb >> 8) & 0xFF;
            b = rgb & 0xFF;
        }
    }
}
