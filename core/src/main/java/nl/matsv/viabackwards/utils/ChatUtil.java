package nl.matsv.viabackwards.utils;

import java.util.regex.Pattern;

public class ChatUtil {
    private static final Pattern UNUSED_COLOR_PATTERN = Pattern.compile("(?>(?>§[0-fk-or])*(§r|\\Z))|(?>(?>§[0-f])*(§[0-f]))");
    private static final Pattern UNUSED_COLOR_PATTERN_PREFIX = Pattern.compile("(?>(?>§[0-fk-or])*(§r))|(?>(?>§[0-f])*(§[0-f]))");

    public static String removeUnusedColor(String legacy, char defaultColor) {
        return removeUnusedColor(legacy, defaultColor, false);
    }

    public static String removeUnusedColor(String legacy, char defaultColor, boolean isPrefix) {
        if (legacy == null) return null;
        Pattern pattern = isPrefix ? UNUSED_COLOR_PATTERN_PREFIX : UNUSED_COLOR_PATTERN;
        legacy = pattern.matcher(legacy).replaceAll("$1$2");
        StringBuilder builder = new StringBuilder();
        char last = defaultColor;
        for (int i = 0; i < legacy.length(); i++) {
            char current = legacy.charAt(i);
            if (current != '§' || i == legacy.length() - 1) {
                builder.append(current);
                continue;
            }
            current = legacy.charAt(++i);
            if (current == last) continue;
            builder.append('§').append(current);
            last = current;
        }
        return builder.toString();
    }
}
