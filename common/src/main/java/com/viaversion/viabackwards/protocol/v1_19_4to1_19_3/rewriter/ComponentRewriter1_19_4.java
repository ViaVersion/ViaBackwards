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
package com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.rewriter;

import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.Protocol1_19_4To1_19_3;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.protocols.v1_19_3to1_19_4.packet.ClientboundPackets1_19_4;

public final class ComponentRewriter1_19_4 extends JsonNBTComponentRewriter<ClientboundPackets1_19_4> {

    public ComponentRewriter1_19_4(final Protocol1_19_4To1_19_3 protocol) {
        super(protocol, ReadType.JSON);
    }

    @Override
    protected void handleTranslate(final JsonObject root, final String translate) {
        super.handleTranslate(root, translate);

        if ((translate.startsWith("vb.item.") || translate.startsWith("vb.entity.")) && root.has("fallback")) {
            // Add fallback as actual translate value
            final JsonElement fallback = root.remove("fallback");
            root.add("translate", fallback);
        }
    }
}
