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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.VBMappingDataLoader;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TranslatableRewriter<C extends ClientboundPacketType> extends ComponentRewriter<C> {

    private static final Map<String, Map<String, String>> TRANSLATABLES = new HashMap<>();
    private final Map<String, String> newTranslatables;

    public static void loadTranslatables() {
        JsonObject jsonObject = VBMappingDataLoader.loadFromDataDir("translation-mappings.json");
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            Map<String, String> versionMappings = new HashMap<>();
            TRANSLATABLES.put(entry.getKey(), versionMappings);
            for (Map.Entry<String, JsonElement> translationEntry : entry.getValue().getAsJsonObject().entrySet()) {
                versionMappings.put(translationEntry.getKey(), translationEntry.getValue().getAsString());
            }
        }
    }

    public TranslatableRewriter(BackwardsProtocol<C, ?, ?, ?> protocol) {
        this(protocol, protocol.getClass().getSimpleName().split("To")[1].replace("_", "."));
    }

    public TranslatableRewriter(BackwardsProtocol<C, ?, ?, ?> protocol, String sectionIdentifier) {
        super(protocol);
        final Map<String, String> newTranslatables = TRANSLATABLES.get(sectionIdentifier);
        if (newTranslatables == null) {
            ViaBackwards.getPlatform().getLogger().warning("Error loading " + sectionIdentifier + " translatables!");
            this.newTranslatables = new HashMap<>();
        } else {
            this.newTranslatables = newTranslatables;
        }
    }

    public void registerPing() {
        protocol.registerClientbound(State.LOGIN, 0x00, 0x00, wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
    }

    public void registerDisconnect(C packetType) {
        protocol.registerClientbound(packetType, wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
    }

    public void registerLegacyOpenWindow(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE); // Id
                map(Type.STRING); // Window Type
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerOpenWindow(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Id
                map(Type.VAR_INT); // Window Type
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerTabList(C packetType) {
        protocol.registerClientbound(packetType, wrapper -> {
            processText(wrapper.passthrough(Type.COMPONENT));
            processText(wrapper.passthrough(Type.COMPONENT));
        });
    }

    public void registerCombatKill(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.INT);
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    @Override
    protected void handleTranslate(JsonObject root, String translate) {
        String newTranslate = mappedTranslationKey(translate);
        if (newTranslate != null) {
            root.addProperty("translate", newTranslate);
        }
    }

    public @Nullable String mappedTranslationKey(final String translationKey) {
        return newTranslatables.get(translationKey);
    }
}
