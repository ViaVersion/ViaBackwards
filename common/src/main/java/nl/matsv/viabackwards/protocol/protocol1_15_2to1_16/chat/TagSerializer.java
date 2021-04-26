/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
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
package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.chat;

import com.google.common.base.Preconditions;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class to serialize a JsonObject with Minecraft's CompoundTag serialization
 */
public class TagSerializer {

    private static final Pattern PLAIN_TEXT = Pattern.compile("[A-Za-z0-9._+-]+");

    public static String toString(JsonObject object) {
        StringBuilder builder = new StringBuilder("{");
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            Preconditions.checkArgument(entry.getValue().isJsonPrimitive());
            if (builder.length() != 1) {
                builder.append(',');
            }

            String escapedText = escape(entry.getValue().getAsString());
            builder.append(entry.getKey()).append(':').append(escapedText);
        }
        return builder.append('}').toString();
    }

    /**
     * Utility method to convert a CompoundTag to a JsonObject, helpful for debugging.
     */
    public static JsonObject toJson(CompoundTag tag) {
        JsonObject object = new JsonObject();
        for (Map.Entry<String, Tag> entry : tag.entrySet()) {
            object.add(entry.getKey(), toJson(entry.getValue()));
        }
        return object;
    }

    private static JsonElement toJson(Tag tag) {
        if (tag instanceof CompoundTag) {
            return toJson((CompoundTag) tag);
        } else if (tag instanceof ListTag) {
            ListTag list = (ListTag) tag;
            JsonArray array = new JsonArray();
            for (Tag listEntry : list) {
                array.add(toJson(listEntry));
            }
            return array;
        } else {
            return new JsonPrimitive(tag.getValue().toString());
        }
    }

    public static String escape(String s) {
        if (PLAIN_TEXT.matcher(s).matches()) return s;

        StringBuilder builder = new StringBuilder(" ");
        char currentQuote = '\0';
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '\\') {
                builder.append('\\');
            } else if (c == '\"' || c == '\'') {
                if (currentQuote == '\0') {
                    currentQuote = ((c == '\"') ? '\'' : '\"');
                }
                if (currentQuote == c) {
                    builder.append('\\');
                }
            }
            builder.append(c);
        }

        if (currentQuote == '\0') {
            currentQuote = '\"';
        }

        builder.setCharAt(0, currentQuote);
        builder.append(currentQuote);
        return builder.toString();
    }
}
