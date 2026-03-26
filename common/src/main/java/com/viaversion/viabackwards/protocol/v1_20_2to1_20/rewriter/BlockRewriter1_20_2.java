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
package com.viaversion.viabackwards.protocol.v1_20_2to1_20.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.data.PotionEffects1_20_2;
import com.viaversion.viaversion.protocols.v1_20to1_20_2.packet.ClientboundPackets1_20_2;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.util.Key;

public final class BlockRewriter1_20_2 extends BlockRewriter<ClientboundPackets1_20_2> {

    public BlockRewriter1_20_2(final Protocol<ClientboundPackets1_20_2, ?, ?, ?> protocol) {
        super(protocol, Types.BLOCK_POSITION1_14, Types.NAMED_COMPOUND_TAG, ChunkType1_20_2::new, ChunkType1_18::new);
    }

    @Override
    public void handleBlockEntity(final UserConnection connection, final BlockEntity blockEntity) {
        final CompoundTag tag = blockEntity.tag();
        final Tag primaryEffect = tag.remove("primary_effect");
        if (primaryEffect instanceof StringTag) {
            final String effectKey = Key.stripMinecraftNamespace(((StringTag) primaryEffect).getValue());
            tag.putInt("Primary", PotionEffects1_20_2.keyToId(effectKey) + 1); // Empty effect at 0
        }

        final Tag secondaryEffect = tag.remove("secondary_effect");
        if (secondaryEffect instanceof StringTag) {
            final String effectKey = Key.stripMinecraftNamespace(((StringTag) secondaryEffect).getValue());
            tag.putInt("Secondary", PotionEffects1_20_2.keyToId(effectKey) + 1); // Empty effect at 0
        }

        final CompoundTag skullOwnerTag = tag.getCompoundTag("SkullOwner");
        if (skullOwnerTag != null && !skullOwnerTag.contains("Id") && skullOwnerTag.contains("Properties")) {
            skullOwnerTag.put("Id", new IntArrayTag(new int[]{0, 0, 0, 0}));
        }
    }
}
