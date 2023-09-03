/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.utils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class ChatUtil {
    private static final Pattern UNUSED_COLOR_PATTERN = Pattern.compile("(?>(?>§[0-fk-or])*(§r|\\Z))|(?>(?>§[0-f])*(§[0-f]))");
    private static final Pattern UNUSED_COLOR_PATTERN_PREFIX = Pattern.compile("(?>(?>§[0-fk-or])*(§r))|(?>(?>§[0-f])*(§[0-f]))");

    public static String removeUnusedColor(String legacy, char defaultColor) {
        return removeUnusedColor(legacy, defaultColor, false);
    }

    private static class ChatFormattingState {
        private final Set<Character> formatting;
        private final char defaultColor;
        private char color;

        private ChatFormattingState(char defaultColor) {
            this(new HashSet<>(), defaultColor, defaultColor);
        }

        public ChatFormattingState(Set<Character> formatting, char defaultColor, char color) {
            this.formatting = formatting;
            this.defaultColor = defaultColor;
            this.color = color;
        }

        private ChatFormattingState setColor(char newColor) {
            formatting.clear();
            color = newColor;
            return this;
        }

        public ChatFormattingState copy() {
            return new ChatFormattingState(new HashSet<>(formatting), defaultColor, color);
        }

        public void appendTo(StringBuilder builder) {
            builder.append('§').append(color);
            for (Character formatCharacter : formatting) {
                builder.append('§').append(formatCharacter);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ChatFormattingState that = (ChatFormattingState) o;
            return defaultColor == that.defaultColor && color == that.color && Objects.equals(
                formatting, that.formatting);
        }

        @Override
        public int hashCode() {
            return Objects.hash(formatting, defaultColor, color);
        }

        public ChatFormattingState processNextAndControlChar(char controlChar) {
            ChatFormattingState copy = copy();
            if (controlChar == 'r') {
                return copy.setColor(defaultColor);
            }
            if (controlChar == 'l' || controlChar == 'm' || controlChar == 'n' || controlChar == 'o') {
                copy.formatting.add(controlChar);
                return copy;
            }
            return copy.setColor(controlChar);
        }
    }

    public static String fromLegacy(String legacy, char defaultColor, int limit) {
        return fromLegacy(legacy, defaultColor, limit, false);
    }

    public static String fromLegacyPrefix(String legacy, char defaultColor, int limit) {
        return fromLegacy(legacy, defaultColor, limit, true);
    }

    public static String fromLegacy(String legacy, char defaultColor, int limit, boolean isPrefix) {
        legacy = removeUnusedColor(legacy, defaultColor, isPrefix);
        if (legacy.length() > limit) legacy = legacy.substring(0, limit);
        if (legacy.endsWith("§")) legacy = legacy.substring(0, legacy.length() - 1);
        return legacy;
    }

    public static String removeUnusedColor(String legacy, char defaultColor, boolean isPrefix) {
        if (legacy == null) return null;
        Pattern pattern = isPrefix ? UNUSED_COLOR_PATTERN_PREFIX : UNUSED_COLOR_PATTERN;
        legacy = pattern.matcher(legacy).replaceAll("$1$2");
        StringBuilder builder = new StringBuilder();
        ChatFormattingState builderState = new ChatFormattingState(defaultColor);
        ChatFormattingState lastState = new ChatFormattingState(defaultColor);
        for (int i = 0; i < legacy.length(); i++) {
            char current = legacy.charAt(i);
            if (current != '§' || i == legacy.length() - 1) {
                if (!lastState.equals(builderState)) {
                    lastState.appendTo(builder);
                    builderState = lastState;
                }
                builder.append(current);
                continue;
            }
            current = legacy.charAt(++i);
            lastState = lastState.processNextAndControlChar(current);
        }
        return builder.toString();
    }
}
