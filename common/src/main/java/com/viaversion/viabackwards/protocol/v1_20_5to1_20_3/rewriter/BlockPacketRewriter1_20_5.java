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
package com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.protocol.v1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.data.BannerPatterns1_20_5;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ClientboundPacket1_20_5;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.util.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockPacketRewriter1_20_5 extends BlockRewriter<ClientboundPacket1_20_5> {

    public BlockPacketRewriter1_20_5(final Protocol1_20_5To1_20_3 protocol) {
        super(protocol, Types.BLOCK_POSITION1_14, Types.COMPOUND_TAG);
    }

    @Override
    public void handleBlockEntity(final UserConnection connection, final BlockEntity blockEntity) {
        updateBlockEntityTag(blockEntity.tag());
    }

    public void updateBlockEntityTag(@Nullable final CompoundTag tag) {
        if (tag == null) {
            return;
        }

        final Tag profileTag = tag.remove("profile");
        if (profileTag instanceof StringTag) {
            tag.put("SkullOwner", profileTag);
        } else if (profileTag instanceof CompoundTag) {
            updateProfileTag(tag, (CompoundTag) profileTag);
        }

        final ListTag<CompoundTag> patternsTag = tag.getListTag("patterns", CompoundTag.class);
        if (patternsTag != null) {
            for (final CompoundTag patternTag : patternsTag) {
                final String pattern = patternTag.getString("pattern", "");
                final String color = patternTag.getString("color");
                final String compactIdentifier = BannerPatterns1_20_5.fullIdToCompact(Key.stripMinecraftNamespace(pattern));
                if (compactIdentifier == null || color == null) {
                    continue;
                }

                patternTag.remove("pattern");
                patternTag.remove("color");
                patternTag.putString("Pattern", compactIdentifier);
                patternTag.putInt("Color", colorId(color));
            }

            tag.remove("patterns");
            tag.put("Patterns", patternsTag);
        }
    }

    private void updateProfileTag(final CompoundTag tag, final CompoundTag profileTag) {
        final CompoundTag skullOwnerTag = new CompoundTag();
        tag.put("SkullOwner", skullOwnerTag);

        final String name = profileTag.getString("name");
        if (name != null) {
            skullOwnerTag.putString("Name", name);
        }

        final IntArrayTag idTag = profileTag.getIntArrayTag("id");
        if (idTag != null) {
            skullOwnerTag.put("Id", idTag);
        }

        final ListTag<CompoundTag> propertiesListTag = profileTag.getListTag("properties", CompoundTag.class);
        if (propertiesListTag == null) {
            return;
        }

        final CompoundTag propertiesTag = new CompoundTag();
        for (final CompoundTag propertyTag : propertiesListTag) {
            final String property = propertyTag.getString("name", "");
            final String value = propertyTag.getString("value", "");
            final String signature = propertyTag.getString("signature");

            final ListTag<CompoundTag> list = new ListTag<>(CompoundTag.class);
            final CompoundTag updatedPropertyTag = new CompoundTag();
            updatedPropertyTag.putString("Value", value);
            if (signature != null) {
                updatedPropertyTag.putString("Signature", signature);
            }
            list.add(updatedPropertyTag);
            propertiesTag.put(property, list);
        }
        skullOwnerTag.put("Properties", propertiesTag);
    }

    private static int colorId(final String color) {
        return switch (color) {
            case "orange" -> 1;
            case "magenta" -> 2;
            case "light_blue" -> 3;
            case "yellow" -> 4;
            case "lime" -> 5;
            case "pink" -> 6;
            case "gray" -> 7;
            case "light_gray" -> 8;
            case "cyan" -> 9;
            case "purple" -> 10;
            case "blue" -> 11;
            case "brown" -> 12;
            case "green" -> 13;
            case "red" -> 14;
            case "black" -> 15;
            default -> 0;
        };
    }
}
