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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.connection.ProtocolInfo;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;

// 1.20.5-1.21.4
public class BackwardsFullStructuredItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType,
    T extends BackwardsProtocol<C, ?, ?, S>> extends BackwardsStructuredItemRewriter<C, S, T> {

    public BackwardsFullStructuredItemRewriter(final T protocol) {
        super(protocol);
    }

    @Override
    public Item handleItemToServer(final UserConnection connection, final Item item) {
        if (Item.isEmpty(item)) {
            return item;
        }

        final ProtocolInfo protocolInfo = connection.getProtocolInfo();
        if (protocolInfo.serverProtocolVersion().newerThanOrEqualTo(ProtocolVersion.v1_21_5)
            && !protocol.getEntityRewriter().tracker(connection).canInstaBuild()) {
            // We don't actually need anything but the amount, id, and hashes.
            // Just pass it on without deeper handling, only leaving custom data.
            final CompoundTag customData = item.dataContainer().get(StructuredDataKey.CUSTOM_DATA);
            if (customData == null) {
                // Not valid
                item.dataContainer().data().clear();
            } else {
                customData.keySet().removeIf(key -> !key.equals(ORIGINAL_HASHES_KEY));
                item.dataContainer().data().keySet().removeIf(key -> key != StructuredDataKey.CUSTOM_DATA);
            }
        }

        return super.handleItemToServer(connection, item);
    }

    @Override
    protected void restoreBackupData(final Item item, final StructuredDataContainer container, final CompoundTag customData) {
        // Keep original hashes intact until 1.21.5
        final Tag hashes = customData.get(ORIGINAL_HASHES_KEY);

        super.restoreBackupData(item, container, customData);

        if (hashes != null) {
            customData.put(ORIGINAL_HASHES_KEY, hashes);

            // Add back
            if (!container.has(StructuredDataKey.CUSTOM_DATA)) {
                container.set(StructuredDataKey.CUSTOM_DATA, customData);
                customData.putBoolean(MARKER_KEY, true);
            }
        }
    }
}
