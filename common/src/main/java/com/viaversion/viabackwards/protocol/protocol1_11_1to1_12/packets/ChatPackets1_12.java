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

package com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.packets;

import com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.Protocol1_11_1To1_12;
import com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.data.AdvancementTranslations;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.rewriter.RewriterBase;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.protocol1_12to1_11_1.ClientboundPackets1_12;
import com.viaversion.viaversion.rewriter.ComponentRewriter;

public class ChatPackets1_12 extends RewriterBase<Protocol1_11_1To1_12> {

    public static final ComponentRewriter<ClientboundPackets1_12> COMPONENT_REWRITER = new ComponentRewriter<>(null, ComponentRewriter.ReadType.JSON) {
        @Override
        public void processText(UserConnection connection, JsonElement element) {
            super.processText(connection, element);
            if (element == null || !element.isJsonObject()) {
                return;
            }

            JsonObject object = element.getAsJsonObject();
            JsonElement keybind = object.remove("keybind");
            if (keybind == null) {
                return;
            }

            //TODO Add nicer text for the key, also use this component rewriter in more packets
            object.addProperty("text", keybind.getAsString());
        }

        @Override
        protected void handleTranslate(JsonObject object, String translate) {
            String text = AdvancementTranslations.get(translate);
            if (text != null) {
                object.addProperty("translate", text);
            }
        }
    };

    public ChatPackets1_12(Protocol1_11_1To1_12 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_12.CHAT_MESSAGE, wrapper -> {
            JsonElement element = wrapper.passthrough(Type.COMPONENT);
            COMPONENT_REWRITER.processText(wrapper.user(), element);
        });
    }
}
