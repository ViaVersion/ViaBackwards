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
package com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.StructuredItem;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.util.SerializerVersion;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ComponentRewriter1_20_5 extends JsonNBTComponentRewriter<ClientboundPacket1_20_5> {

    private final com.viaversion.viaversion.protocols.v1_20_3to1_20_5.rewriter.ComponentRewriter1_20_5<ClientboundPacket1_20_5> vvRewriter;

    public ComponentRewriter1_20_5(final BackwardsProtocol<ClientboundPacket1_20_5, ?, ?, ?> protocol) {
        super(protocol, ReadType.NBT);
        vvRewriter = new com.viaversion.viaversion.protocols.v1_20_3to1_20_5.rewriter.ComponentRewriter1_20_5<>(protocol, Types1_20_5.STRUCTURED_DATA);
    }

    @Override
    protected void handleShowItem(final UserConnection connection, final CompoundTag itemTag, @Nullable final CompoundTag componentsTag) {
        super.handleShowItem(connection, itemTag, componentsTag);
        if (componentsTag == null) {
            return;
        }

        final StringTag idTag = itemTag.getStringTag("id");
        if (idTag == null) {
            return;
        }

        final List<StructuredData<?>> data = vvRewriter.toData(connection, componentsTag);
        if (data.isEmpty()) {
            return;
        }

        final int identifier = this.protocol.getMappingData().getFullItemMappings().id(idTag.getValue());

        final StructuredItem structuredItem = new StructuredItem(identifier, 1, new StructuredDataContainer(data.toArray(StructuredData[]::new)));
        final Item dataItem = protocol.getItemRewriter().handleItemToClient(connection, structuredItem);
        if (dataItem.tag() == null) {
            return;
        }

        itemTag.remove("components");
        itemTag.put("tag", new StringTag(outputSerializerVersion().toSNBT(dataItem.tag())));
    }

    @Override
    protected SerializerVersion inputSerializerVersion() {
        return SerializerVersion.V1_20_5;
    }

    @Override
    protected SerializerVersion outputSerializerVersion() {
        return SerializerVersion.V1_20_3;
    }
}
