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

package com.viaversion.viabackwards.protocol.v1_12to1_11_1.rewriter;

import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.Protocol1_12To1_11_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;

public class ComponentRewriter1_12 extends JsonNBTComponentRewriter<ClientboundPackets1_12> {

    public ComponentRewriter1_12(Protocol1_12To1_11_1 protocol) {
        super(protocol, ComponentRewriterBase.ReadType.JSON);
    }

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
}
