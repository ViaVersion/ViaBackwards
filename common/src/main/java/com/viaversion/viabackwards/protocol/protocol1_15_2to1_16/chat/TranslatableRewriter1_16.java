/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.chat;

import java.util.Map.Entry;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonParser;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.libs.kyori.adventure.text.Component;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ChatRewriter;
import com.viaversion.viaversion.util.ChatColorUtil;

@SuppressWarnings("rawtypes")
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
    public void processText(JsonElement value) {
        super.processText(value);

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
        if (hoverEvent != null) {
            // show_text as chat component
            // show_entity and show_item serialized as nbt
            // Let adventure handle all of that
            try {
                Component component = ChatRewriter.HOVER_GSON_SERIALIZER.deserializeFromTree(object);
                JsonObject processedHoverEvent = ((JsonObject) ChatRewriter.HOVER_GSON_SERIALIZER.serializeToTree(component)).getAsJsonObject("hoverEvent");
                processedHoverEvent.remove("contents");
                if(processedHoverEvent.get("action").getAsString().equalsIgnoreCase("show_item")) { // if it's an item
	                JsonElement valueOfHoverEvent = processedHoverEvent.get("value");
	            	if(valueOfHoverEvent.isJsonObject() && valueOfHoverEvent.getAsJsonObject().has("text")) { // should have text, but checking to prevent error
	            		JsonObject hoverValueAsObject = valueOfHoverEvent.getAsJsonObject();
            			String textValueString = hoverValueAsObject.get("text").getAsString();
            			JsonObject jsonItem = JsonParser.parseString(textValueString).getAsJsonObject();
            			if(jsonItem.has("tag") && jsonItem.getAsJsonObject("tag").has("display")) { // check if has display name
            				JsonObject displayOfItem = jsonItem.getAsJsonObject("tag").getAsJsonObject("display");
            				if(displayOfItem.has("Name")) {
	            				String oldName = displayOfItem.get("Name").getAsString();
	            				JsonElement oldNameElement = JsonParser.parseString(oldName);
	            				if(oldNameElement.isJsonObject()) {
		            				String newNameOfItem = getTextFromJson(oldNameElement.getAsJsonObject());
		            				hoverValueAsObject.addProperty("text", textValueString.replace(oldName.replace("\"", "\\\""), newNameOfItem)); // update json where we take this one
		            				// we use replace because of " which are missing on basic item, but with this it will add and so show "Invalid Item"
	            				}
            				}
	            		}
	            	}
                }
                // Remove new format
                object.add("hoverEvent", processedHoverEvent);
            } catch (Exception e) {
                ViaBackwards.getPlatform().getLogger().severe("Error converting hover event component: " + object);
                e.printStackTrace();
            }
        }
    }
    
    private String getTextFromJson(JsonObject json) {
    	String fullText = "";
    	if(json.has("extra") && json.get("extra").isJsonArray()) {
        	for(JsonElement extraContent : json.getAsJsonArray("extra"))
        		if(extraContent.isJsonObject())
        			fullText += parseJsonObjectText(extraContent.getAsJsonObject());
    	} else if(json.has("text")) { // at least has text content
    		fullText += parseJsonObjectText(json);
    	}
    	return fullText;
    }
    
    private String parseJsonObjectText(JsonObject json) {
    	String color = "", decoration = "", text = "";
    	for(Entry<String, JsonElement> entries : json.entrySet()) {
    		String key = entries.getKey();
    		JsonElement element = entries.getValue();
    		if(element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() && element.getAsBoolean()) { // if it's boolean and on true
    			switch(key.toLowerCase()) {
    			case "bold":
    				decoration = "&l";
    				break;
    			case "italic":
    				decoration = "&o";
    				break;
    			case "underlined":
    				decoration = "&n";
    				break;
    			case "strikethrough":
    				decoration = "&m";
    				break;
    			case "obfuscated":
    				decoration = "&k";
    				break;
    			case "reset":
    				decoration = "&r";
    				break;
    			default:
    				ViaBackwards.getPlatform().getLogger().warning("Unknow key '" + key + "' for text.");
    			}
    		} else if(key.equals("text"))
    			text += element.getAsString();
			else if(key.equals("color"))
				color = getColorCodeFromName(element.getAsString());
    	}
    	return ChatColorUtil.translateAlternateColorCodes(color + decoration) + text;
    }
    
    private String getColorCodeFromName(String name) { // can't find class as bukkit to do `ChatColor.valueOf()`
    	if(name == null || name.isEmpty())
    		return "";
    	switch (name.toLowerCase()) {
		case "dark_red":
			return "&4";
		case "red":
			return "&c";
		case "gold":
			return "&6";
		case "yellow":
			return "&e";
		case "dark_green":
			return "&2";
		case "green":
			return "&a";
		case "aqua":
			return "&b";
		case "dark_aqua":
			return "&3";
		case "dark_blue":
			return "&1";
		case "blue":
			return "&9";
		case "light_purple":
			return "&d";
		case "dark_purple":
			return "&5";
		case "white":
			return "&f";
		case "gray":
			return "&7";
		case "dark_gray":
			return "&8";
		case "black":
			return "&0";
		default:
	    	return name;
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
