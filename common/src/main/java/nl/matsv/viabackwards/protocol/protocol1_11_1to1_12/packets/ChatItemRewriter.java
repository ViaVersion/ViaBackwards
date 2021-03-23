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
package nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.packets;

import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.viaversion.libs.gson.JsonArray;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;
import us.myles.viaversion.libs.gson.JsonPrimitive;

public class ChatItemRewriter {

    public static void toClient(JsonElement element, UserConnection user) {
        if (element instanceof JsonObject) {
            JsonObject obj = (JsonObject) element;
            if (obj.has("hoverEvent")) {
                if (obj.get("hoverEvent") instanceof JsonObject) {
                    JsonObject hoverEvent = (JsonObject) obj.get("hoverEvent");
                    if (hoverEvent.has("action") && hoverEvent.has("value")) {
                        String type = hoverEvent.get("action").getAsString();
                        if (type.equals("show_item") || type.equals("show_entity")) {
                            JsonElement value = hoverEvent.get("value");

                            if (value.isJsonArray()) {
                                JsonArray newArray = new JsonArray();

                                int index = 0;
                                for (JsonElement valueElement : value.getAsJsonArray()) {
                                    if (valueElement.isJsonPrimitive() && valueElement.getAsJsonPrimitive().isString()) {
                                        String newValue = index + ":" + valueElement.getAsString();
                                        newArray.add(new JsonPrimitive(newValue));
                                    }
                                }

                                hoverEvent.add("value", newArray);
                            }
                        }
                    }
                }
            } else if (obj.has("extra")) {
                toClient(obj.get("extra"), user);
            }
        } else if (element instanceof JsonArray) {
            JsonArray array = (JsonArray) element;
            for (JsonElement value : array) {
                toClient(value, user);
            }
        }
    }
}
