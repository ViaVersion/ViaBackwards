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
package com.viaversion.viabackwards.protocol.v26_1to1_21_11.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v26_1to1_21_11.Protocol26_1To1_21_11;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_7to1_21_9.packet.ServerboundPacket1_21_9;

import static com.viaversion.viaversion.protocols.v1_21_11to26_1.rewriter.BlockItemPacketRewriter26_1.downgradeData;
import static com.viaversion.viaversion.protocols.v1_21_11to26_1.rewriter.BlockItemPacketRewriter26_1.upgradeData;

public final class BlockItemPacketRewriter26_1 extends BackwardsStructuredItemRewriter<ClientboundPacket26_1, ServerboundPacket1_21_9, Protocol26_1To1_21_11> {

    public BlockItemPacketRewriter26_1(final Protocol26_1To1_21_11 protocol) {
        super(protocol);
    }

    @Override
    protected void handleItemDataComponentsToClient(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToClient(connection, item, container);
        downgradeData(protocol.mappedTypes().structuredDataKeys(), container);
    }

    @Override
    protected void handleItemDataComponentsToServer(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToServer(connection, item, container);
        upgradeData(protocol, protocol.types().structuredDataKeys(), container);
    }

    @Override
    protected void restoreBackupData(final Item item, final StructuredDataContainer container, final CompoundTag customData) {
        super.restoreBackupData(item, container, customData);
        if (!(customData.remove(nbtTagName("backup")) instanceof final CompoundTag backupTag)) {
            return;
        }

        restoreIntData(StructuredDataKey.ADDITIONAL_TRADE_COST, container, backupTag);
        restoreIntData(StructuredDataKey.DYE, container, backupTag);
        restoreIntData(StructuredDataKey.CAT_SOUND_VARIANT, container, backupTag);
        restoreIntData(StructuredDataKey.CHICKEN_SOUND_VARIANT, container, backupTag);
        restoreIntData(StructuredDataKey.COW_SOUND_VARIANT, container, backupTag);
        restoreIntData(StructuredDataKey.PIG_SOUND_VARIANT, container, backupTag);
    }

    @Override
    protected void backupInconvertibleData(final UserConnection connection, final Item item, final StructuredDataContainer dataContainer, final CompoundTag backupTag) {
        super.backupInconvertibleData(connection, item, dataContainer, backupTag);
        saveIntData(StructuredDataKey.ADDITIONAL_TRADE_COST, dataContainer, backupTag);
        saveIntData(StructuredDataKey.DYE, dataContainer, backupTag);
        saveIntData(StructuredDataKey.CAT_SOUND_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.CHICKEN_SOUND_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.COW_SOUND_VARIANT, dataContainer, backupTag);
        saveIntData(StructuredDataKey.PIG_SOUND_VARIANT, dataContainer, backupTag);
    }
}
