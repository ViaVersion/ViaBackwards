/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_21_6to1_21_5.data;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import java.util.Map;

public record Template(List<String> segments, List<String> variables) {

    private static final int MAX_LENGTH = 2000000;

    public static Template fromString(final String string) {
        final Builder<String> segmentsBuilder = ImmutableList.builder();
        final Builder<String> variablesBuilder = ImmutableList.builder();
        int currentIndex = 0;
        int dollarIndex = string.indexOf('$');

        while (dollarIndex != -1) {
            if (dollarIndex != string.length() - 1 && string.charAt(dollarIndex + 1) == '(') {
                segmentsBuilder.add(string.substring(currentIndex, dollarIndex));

                final int closingParenIndex = string.indexOf(41, dollarIndex + 1);
                if (closingParenIndex == -1) {
                    throw new IllegalArgumentException("Unterminated macro variable");
                }

                final String variableName = string.substring(dollarIndex + 2, closingParenIndex);
                if (!isValidVariableName(variableName)) {
                    throw new IllegalArgumentException("Invalid macro variable name '" + variableName + "'");
                }

                variablesBuilder.add(variableName);
                currentIndex = closingParenIndex + 1;
                dollarIndex = string.indexOf(36, currentIndex);
            } else {
                dollarIndex = string.indexOf(36, dollarIndex + 1);
            }
        }

        if (currentIndex == 0) {
            throw new IllegalArgumentException("No variables in macro");
        }
        if (currentIndex != string.length()) {
            segmentsBuilder.add(string.substring(currentIndex));
        }
        return new Template(segmentsBuilder.build(), variablesBuilder.build());
    }

    private static boolean isValidVariableName(final String string) {
        for (int i = 0; i < string.length(); i++) {
            final char c = string.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }

        return true;
    }

    public String instantiate(final Map<String, String> map) {
        return substitute(variables().stream().map(string -> map.getOrDefault(string, "")).toList());
    }

    private String substitute(final List<String> list) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < this.variables.size(); i++) {
            out.append(this.segments.get(i)).append(list.get(i));
            if (out.length() > MAX_LENGTH) {
                throw new IllegalArgumentException("Output too long (> " + MAX_LENGTH + ")");
            }
        }
        if (this.segments.size() > this.variables.size()) {
            out.append(this.segments.get(this.segments.size() - 1));
            if (out.length() > MAX_LENGTH) {
                throw new IllegalArgumentException("Output too long (> " + MAX_LENGTH + ")");
            }
        }
        return out.toString();
    }
}
