package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.chat;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
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

    public void processText(JsonElement value) {
        super.processText(value);
        if (!value.isJsonObject()) return;

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
    }

    @Override
    protected void handleHoverEvent(JsonObject hoverEvent) {
        // Don't call super, convert and process contents here
        JsonElement contentsElement = hoverEvent.remove("contents");
        if (contentsElement == null) return;

        // show_text as chat component
        // show_entity and show_item serialized as nbt
        String action = hoverEvent.getAsJsonPrimitive("action").getAsString();
        switch (action) {
            case "show_text":
                processText(contentsElement);
                hoverEvent.add("value", contentsElement);
                break;
            case "show_item":
                JsonObject item = contentsElement.getAsJsonObject();
                JsonElement count = item.remove("count");
                item.addProperty("Count", count != null ? count.getAsByte() : 1);

                hoverEvent.addProperty("value", TagSerializer.toString(item));
                break;
            case "show_entity":
                JsonObject entity = contentsElement.getAsJsonObject();
                JsonObject name = entity.getAsJsonObject("name");
                if (name != null) {
                    processText(name);
                    entity.addProperty("name", name.toString());
                }

                JsonObject hoverObject = new JsonObject();
                hoverObject.addProperty("text", TagSerializer.toString(entity));
                hoverEvent.add("value", hoverObject);
                break;
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
