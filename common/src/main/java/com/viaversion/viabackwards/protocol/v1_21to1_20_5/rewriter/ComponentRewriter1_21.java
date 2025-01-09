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
package com.viaversion.viabackwards.protocol.v1_21to1_20_5.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.v1_21to1_20_5.Protocol1_21To1_20_5;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.data.Attributes1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.Protocol1_20_5To1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.data.AttributeModifierMappings1_21;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundPacket1_21;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.SerializerVersion;
import com.viaversion.viaversion.util.TagUtil;
import com.viaversion.viaversion.util.UUIDUtil;
import java.util.UUID;

public final class ComponentRewriter1_21 extends TranslatableRewriter<ClientboundPacket1_21> {

    public ComponentRewriter1_21(final Protocol1_21To1_20_5 protocol) {
        super(protocol, ReadType.NBT);
    }

    private void convertAttributeModifiersComponent(final CompoundTag tag) {
        final CompoundTag attributeModifiers = TagUtil.getNamespacedCompoundTag(tag, "attribute_modifiers");
        if (attributeModifiers == null) {
            return;
        }
        final ListTag<CompoundTag> modifiers = attributeModifiers.getListTag("modifiers", CompoundTag.class);
        int size = modifiers.size();
        for (int i = 0; i < size; i++) {
            final CompoundTag modifier = modifiers.get(i);
            final String type = Key.stripMinecraftNamespace(modifier.getString("type"));
            if (Attributes1_20_5.keyToId(type) == -1) {
                // Ignore new attributes
                modifiers.remove(i--);
                size--;
                continue;
            }

            final String id = modifier.getString("id");
            final UUID uuid = Protocol1_20_5To1_21.mapAttributeId(id);
            final String name = AttributeModifierMappings1_21.idToName(id);
            modifier.put("uuid", new IntArrayTag(UUIDUtil.toIntArray(uuid)));
            modifier.putString("name", name != null ? name : id);
        }
    }

    @Override
    protected void handleShowItem(final UserConnection connection, final CompoundTag itemTag, final CompoundTag componentsTag) {
        super.handleShowItem(connection, itemTag, componentsTag);
        if (componentsTag != null) {
            TagUtil.removeNamespaced(componentsTag, "jukebox_playable");
            convertAttributeModifiersComponent(componentsTag);
        }
    }

    @Override
    protected SerializerVersion inputSerializerVersion() {
        return SerializerVersion.V1_20_5;
    }

    @Override
    protected SerializerVersion outputSerializerVersion() {
        return SerializerVersion.V1_20_5;
    }
}
