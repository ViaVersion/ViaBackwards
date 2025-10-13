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
package com.viaversion.viabackwards.protocol.v1_21_9to1_21_7.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ClientboundPacket1_21_9;

public final class ComponentRewriter1_21_9 extends NBTComponentRewriter<ClientboundPacket1_21_9> {

    public ComponentRewriter1_21_9(final BackwardsProtocol<ClientboundPacket1_21_9, ?, ?, ?> protocol) {
        super(protocol);
    }

    @Override
    protected void processCompoundTag(final UserConnection connection, final CompoundTag tag) {
        super.processCompoundTag(connection, tag);

        // Throw out the new object type and its properties
        final String type = tag.getString("type");
        if ("object".equals(type)) {
            tag.putString("text", "");
            tag.remove("type");
        }
        if (tag.remove("sprite") != null) {
            tag.putString("text", "");
        }
        if (tag.remove("player") != null) {
            tag.putString("text", "");
        }
        tag.remove("atlas");
    }

    @Override
    protected void handleShowItem(final UserConnection connection, final CompoundTag itemTag, final CompoundTag componentsTag) {
        super.handleShowItem(connection, itemTag, componentsTag);
        if (componentsTag == null) {
            return;
        }

        removeDataComponents(componentsTag, StructuredDataKey.ENTITY_DATA1_21_9, StructuredDataKey.BLOCK_ENTITY_DATA1_21_9);
    }
}
