package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.chat;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import us.myles.viaversion.libs.gson.JsonArray;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;
import us.myles.viaversion.libs.gson.JsonPrimitive;

public class TranslatableRewriter1_16 extends TranslatableRewriter {

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

    public TranslatableRewriter1_16(BackwardsProtocol protocol) {
        super(protocol);
    }

    @Override
    public String processTranslate(String value) {
        JsonElement root = JSON_PARSER.parse(value);
        if (!root.isJsonObject()) {
            return super.processTranslate(value);
        }

        processTranslate(root);
        return super.processTranslate(root.toString());
    }

    private void processTranslate(JsonElement value) {
        if (!value.isJsonObject()) return;

        // Iterate all sub components
        JsonObject object = value.getAsJsonObject();
        JsonArray with = object.getAsJsonArray("with");
        if (with != null) {
            for (JsonElement element : with) {
                processTranslate(element);
            }
        }
        JsonArray extra = object.getAsJsonArray("extra");
        if (extra != null) {
            for (JsonElement element : extra) {
                processTranslate(element);
            }
        }

        // Hoverevent structure changed
        JsonObject hoverEvent = object.getAsJsonObject("hoverEvent");
        if (hoverEvent != null) {
            JsonElement contentsElement = hoverEvent.remove("contents");
            String action = hoverEvent.getAsJsonPrimitive("action").getAsString();
            if (contentsElement != null) {
                if (action.equals("show_text")) {
                    // show_text as chat component
                    processTranslate(contentsElement);
                    hoverEvent.add("value", contentsElement);
                } else if (action.equals("show_item")) {
                    JsonObject item = contentsElement.getAsJsonObject();
                    JsonElement count = item.remove("count");
                    if (count != null) {
                        item.addProperty("Count", count.getAsByte());
                    }

                    hoverEvent.addProperty("value", contentsElement.toString());
                } else {
                    //TODO escape/fix?
                    // the server sends the json as a string
                    hoverEvent.addProperty("value", contentsElement.toString());
                }
            }
        }

        // c o l o r s
        JsonPrimitive color = object.getAsJsonPrimitive("color");
        if (color != null) {
            String colorName = color.getAsString();
            if (!colorName.isEmpty() && colorName.charAt(0) == '#') {
                int rgb = Integer.parseInt(colorName.substring(1), 16);
                String closestChatColor = getClosestChatColor(rgb);
                object.addProperty("color", closestChatColor);
            }
        }
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
            int rDiff = Math.abs(color.r - r);
            int gDiff = Math.abs(color.g - g);
            int bDiff = Math.abs(color.b - b);
            int maxDiff = Math.max(Math.max(rDiff, gDiff), bDiff);
            if (closest == null || maxDiff < smallestDiff) {
                closest = color;
                smallestDiff = maxDiff;
            }
        }
        return closest.colorName;
    }

    private static final class ChatColor {

        private final String colorName;
        private final int rgb;
        private final int r, g, b;

        ChatColor(String colorName, int rgb) {
            this.colorName = colorName;
            this.rgb = rgb;
            r = (rgb >> 16) & 0xFF;
            g = (rgb >> 8) & 0xFF;
            b = rgb & 0xFF;
        }
    }
}
