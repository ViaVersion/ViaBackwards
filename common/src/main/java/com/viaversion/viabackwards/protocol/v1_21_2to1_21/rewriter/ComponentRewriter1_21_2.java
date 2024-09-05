/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
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
package com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.util.SerializerVersion;
import com.viaversion.viaversion.util.TagUtil;

public final class ComponentRewriter1_21_2 extends TranslatableRewriter<ClientboundPacket1_21_2> {

    public ComponentRewriter1_21_2(final Protocol1_21_2To1_21 protocol) {
        super(protocol, ReadType.NBT);
    }

    @Override
    protected void handleShowItem(final UserConnection connection, final CompoundTag itemTag, final CompoundTag componentsTag) {
        super.handleShowItem(connection, itemTag, componentsTag);
        if (componentsTag == null) {
            return;
        }

        // TODO attribute types

        final CompoundTag instrument = TagUtil.getNamespacedCompoundTag(componentsTag, "instrument");
        if (instrument != null) {
            instrument.remove("description");
        }

        final CompoundTag useRemainder = TagUtil.getNamespacedCompoundTag(componentsTag, "use_remainder");
        final CompoundTag food = TagUtil.getNamespacedCompoundTag(componentsTag, "food");
        if (food != null) {
            if (useRemainder != null) {
                food.put("using_converts_to", useRemainder);
            }
            food.putFloat("eat_seconds", 1.6F);
        }

        TagUtil.removeNamespaced(componentsTag, "repairable");
        TagUtil.removeNamespaced(componentsTag, "enchantable");
        TagUtil.removeNamespaced(componentsTag, "consumable");
        TagUtil.removeNamespaced(componentsTag, "use_remainder");
        TagUtil.removeNamespaced(componentsTag, "use_cooldown");
        TagUtil.removeNamespaced(componentsTag, "item_model");
        TagUtil.removeNamespaced(componentsTag, "equippable");
        TagUtil.removeNamespaced(componentsTag, "glider");
        TagUtil.removeNamespaced(componentsTag, "tooltip_style");
        TagUtil.removeNamespaced(componentsTag, "death_protection");
        TagUtil.removeNamespaced(componentsTag, "damage_resistant");
    }

    @Override
    protected SerializerVersion inputSerializerVersion() {
        return SerializerVersion.V1_20_5;
    }
}
