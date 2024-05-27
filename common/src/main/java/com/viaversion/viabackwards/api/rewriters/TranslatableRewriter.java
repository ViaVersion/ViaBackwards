/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingDataLoader;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TranslatableRewriter<C extends ClientboundPacketType> extends ComponentRewriter<C> {

    private static final Map<String, Map<String, String>> TRANSLATABLES = new HashMap<>();
    private final Map<String, String> translatables;

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

    public TranslatableRewriter(final BackwardsProtocol<C, ?, ?, ?> protocol, final ReadType type) {
        this(protocol, type, protocol.getClass().getSimpleName().replace("Protocol", "").split("To")[0].replace("_", "."));
    }

    public TranslatableRewriter(final BackwardsProtocol<C, ?, ?, ?> protocol, final ReadType type, final String sectionIdentifier) {
        super(protocol, type);
        final Map<String, String> translatableMappings = getTranslatableMappings(sectionIdentifier);
        if (translatableMappings == null) {
            protocol.getLogger().warning("Missing " + sectionIdentifier + " translatables!");
            this.translatables = new HashMap<>();
        } else {
            this.translatables = translatableMappings;
        }
    }

    @Override
    protected void handleTranslate(final JsonObject root, final String translate) {
        final String newTranslate = mappedTranslationKey(translate);
        if (newTranslate != null) {
            root.addProperty("translate", newTranslate);
        }
    }

    @Override
    protected void handleTranslate(final UserConnection connection, final CompoundTag parentTag, final StringTag translateTag) {
        final String newTranslate = mappedTranslationKey(translateTag.getValue());
        if (newTranslate != null) {
            parentTag.put("translate", new StringTag(newTranslate));
        }
    }

    public @Nullable String mappedTranslationKey(final String translationKey) {
        return translatables.get(translationKey);
    }

    public Map<String, String> getTranslatableMappings(final String sectionIdentifier) {
        return TRANSLATABLES.get(sectionIdentifier);
    }
}
