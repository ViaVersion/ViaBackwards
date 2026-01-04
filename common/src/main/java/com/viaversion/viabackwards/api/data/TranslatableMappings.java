/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
package com.viaversion.viabackwards.api.data;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TranslatableMappings {

    private static final Map<String, Map<String, String>> TRANSLATABLES = new HashMap<>();

    public static void loadTranslatables() {
        if (!TRANSLATABLES.isEmpty()) {
            throw new IllegalStateException("Translatables already loaded!");
        }
        fillTranslatables(BackwardsMappingDataLoader.INSTANCE.loadFromDataDir("translation-mappings.json"), TRANSLATABLES);
    }

    public static void fillTranslatables(final JsonObject jsonObject, final Map<String, Map<String, String>> translatables) {
        for (final Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            final Map<String, String> versionMappings = new HashMap<>();
            translatables.put(entry.getKey(), versionMappings);
            for (final Map.Entry<String, JsonElement> translationEntry : entry.getValue().getAsJsonObject().entrySet()) {
                versionMappings.put(translationEntry.getKey(), translationEntry.getValue().getAsString());
            }
        }
    }

    public static Map<String, String> translatablesFor(final Protocol<?, ?, ?, ?> protocol) {
        final String version = protocol.getClass().getSimpleName()
            .replace("Protocol", "")
            .split("To")[0]
            .replace("_", ".");
        return translatablesFor(version);
    }

    public static Map<String, String> translatablesFor(final String version) {
        final Map<String, String> translatableMappings = getTranslatableMappings(version);
        if (translatableMappings == null) {
            ViaBackwards.getPlatform().getLogger().warning("Missing " + version + " translatables!");
            return new HashMap<>();
        }
        return translatableMappings;
    }

    public static @Nullable Map<String, String> getTranslatableMappings(final String sectionIdentifier) {
        return TRANSLATABLES.get(sectionIdentifier);
    }
}
