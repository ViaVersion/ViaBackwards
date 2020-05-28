package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.chat;

import com.google.common.base.Preconditions;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;

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
