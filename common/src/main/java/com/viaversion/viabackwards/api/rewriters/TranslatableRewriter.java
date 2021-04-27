/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.rewriter.ComponentRewriter;

import java.util.HashMap;
import java.util.Map;

public class TranslatableRewriter extends ComponentRewriter {

    private static final Map<String, Map<String, String>> TRANSLATABLES = new HashMap<>();
    protected final Map<String, String> newTranslatables;

    public static void loadTranslatables() {
        JsonObject jsonObject = VBMappingDataLoader.loadData("translation-mappings.json");
        for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
            Map<String, String> versionMappings = new HashMap<>();
            TRANSLATABLES.put(entry.getKey(), versionMappings);
            for (Map.Entry<String, JsonElement> translationEntry : entry.getValue().getAsJsonObject().entrySet()) {
                versionMappings.put(translationEntry.getKey(), translationEntry.getValue().getAsString());
            }
        }
    }

    public TranslatableRewriter(BackwardsProtocol protocol) {
        this(protocol, protocol.getClass().getSimpleName().split("To")[1].replace("_", "."));
    }

    public TranslatableRewriter(BackwardsProtocol protocol, String sectionIdentifier) {
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
        protocol.registerOutgoing(State.LOGIN, 0x00, 0x00, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerDisconnect(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerChatMessage(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerLegacyOpenWindow(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // Id
                map(Type.STRING); // Window Type
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerOpenWindow(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Id
                map(Type.VAR_INT); // Window Type
                handler(wrapper -> processText(wrapper.passthrough(Type.COMPONENT)));
            }
        });
    }

    public void registerTabList(ClientboundPacketType packetType) {
        protocol.registerOutgoing(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    processText(wrapper.passthrough(Type.COMPONENT));
                    processText(wrapper.passthrough(Type.COMPONENT));
                });
            }
        });
    }

    @Override
    protected void handleTranslate(JsonObject root, String translate) {
        String newTranslate = newTranslatables.get(translate);
        if (newTranslate != null) {
            root.addProperty("translate", newTranslate);
        }
    }
}
