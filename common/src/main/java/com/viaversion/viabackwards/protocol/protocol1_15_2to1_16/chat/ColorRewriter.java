package com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.chat;

import java.util.Map.Entry;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.util.ChatColorUtil;

public class ColorRewriter {

	public static String parseJsonObjectText(JsonObject json) {
		String color = "", decoration = "", text = "";
		for (Entry<String, JsonElement> entries : json.entrySet()) {
			String key = entries.getKey();
			JsonElement element = entries.getValue();
			if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() && element.getAsBoolean()) {
				switch (key.toLowerCase()) {
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
			} else if (key.equals("text"))
				text += element.getAsString();
			else if (key.equals("color"))
				color = getColorCodeFromName(element.getAsString());
		}
		return ChatColorUtil.translateAlternateColorCodes(color + decoration) + text;
	}

	/**
	 * Get color code from name<br>
	 * Replace the method from bukkit: `ChatColor.valueOf()`
	 * 
	 * @param name the color name like "red"
	 * @return color code like "&c"
	 */
	public static String getColorCodeFromName(String name) { // can't find class as bukkit to do `ChatColor.valueOf()`
		if (name == null || name.isEmpty())
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
}
