/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
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
package com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.protocol.v26_3to26_2.Protocol26_3To26_2;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType26_1;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPacket26_3;
import com.viaversion.viaversion.rewriter.block.BlockRewriter1_21_5;

import static com.viaversion.viaversion.protocols.v26_2to26_3.rewriter.BlockPacketRewriter26_3.POT_DECORATION_SIDES;

public final class BlockPacketRewriter26_3 extends BlockRewriter1_21_5<ClientboundPacket26_3> {

    public BlockPacketRewriter26_3(final Protocol26_3To26_2 protocol) {
        super(protocol, ChunkType26_1::new);
    }

    @Override
    public void handleBlockEntity(final UserConnection connection, final BlockEntity blockEntity) {
        super.handleBlockEntity(connection, blockEntity);

        final CompoundTag tag = blockEntity.tag();
        if (tag == null) {
            return;
        }

        final CompoundTag decorations = tag.getCompoundTag("sherds");
        if (decorations == null) {
            return;
        }

        final ListTag<StringTag> sherds = new ListTag<>(StringTag.class);
        for (final String side : POT_DECORATION_SIDES) {
            final CompoundTag sideTag = decorations.getCompoundTag(side);
            final String item = sideTag != null ? sideTag.getString("id") : null;
            sherds.add(new StringTag(item != null ? item : "minecraft:brick"));
        }
        tag.put("sherds", sherds);
    }
}
