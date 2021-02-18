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
