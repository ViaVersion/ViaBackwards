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
package com.viaversion.viabackwards.protocol.v26_3to26_2.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.text.NBTComponentRewriter;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.protocols.v26_2to26_3.packet.ClientboundPacket26_3;

public final class ComponentRewriter26_3 extends NBTComponentRewriter<ClientboundPacket26_3> {

    public ComponentRewriter26_3(final BackwardsProtocol<ClientboundPacket26_3, ?, ?, ?> protocol) {
        super(protocol);
    }

    @Override
    protected void handleShowItem(final UserConnection connection, final CompoundTag itemTag, final CompoundTag componentsTag) {
        super.handleShowItem(connection, itemTag, componentsTag);
        if (componentsTag == null) {
            return;
        }

        removeDataComponents(componentsTag, StructuredDataKey.PROVIDES_TRIM_MATERIAL26_3, StructuredDataKey.PROVIDES_POTTERY_PATTERN, StructuredDataKey.BLOCK_TRANSFORMER,
            StructuredDataKey.COMPOSTABLE, StructuredDataKey.INTERACT_ANIMATION, StructuredDataKey.ATTACK_ANIMATION, StructuredDataKey.TRIM26_3,
            StructuredDataKey.COOKING_FUEL, StructuredDataKey.BREWING_FUEL, StructuredDataKey.VILLAGER_FOOD, StructuredDataKey.MOB_VISIBILITY,
            StructuredDataKey.SIGN_TEXT_BACK, StructuredDataKey.SIGN_TEXT_FRONT, StructuredDataKey.WAXED, StructuredDataKey.CUSHION_COLOR);
        removeDataComponents(componentsTag, "pot_decorations");
    }
}
