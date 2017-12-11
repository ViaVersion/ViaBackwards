package nl.matsv.viabackwards.utils;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class ChatUtil {

	public static String jsonToLegacy(String json) {
		if (json==null) return null;
		String legacy = json.startsWith("{") ? TextComponent.toLegacyText(ComponentSerializer.parse(json)) : json;
		if (legacy.startsWith("§f§f")) legacy = legacy.substring(4, legacy.length());
		legacy = legacy.replaceAll("((§.)*)\"(.*)\"((§.)*)", "$1$3$4");
		return legacy;
	}
}
