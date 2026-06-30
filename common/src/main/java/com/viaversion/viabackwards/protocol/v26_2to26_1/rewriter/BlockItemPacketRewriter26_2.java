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
package com.viaversion.viabackwards.protocol.v26_2to26_1.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.rewriters.BackwardsStructuredItemRewriter;
import com.viaversion.viabackwards.protocol.v26_2to26_1.Protocol26_2To26_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntityImpl;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPacket26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ClientboundPackets26_1;
import com.viaversion.viaversion.protocols.v1_21_11to26_1.packet.ServerboundPacket26_1;

import static com.viaversion.viaversion.protocols.v26_1to26_2.rewriter.BlockItemPacketRewriter26_2.downgradeData;
import static com.viaversion.viaversion.protocols.v26_1to26_2.rewriter.BlockItemPacketRewriter26_2.upgradeData;

public final class BlockItemPacketRewriter26_2 extends BackwardsStructuredItemRewriter<ClientboundPacket26_1, ServerboundPacket26_1, Protocol26_2To26_1> {

    private static final int BED_BLOCK_ENTITY_TYPE_ID = 25; // minecraft:bed

    public BlockItemPacketRewriter26_2(final Protocol26_2To26_1 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.replaceClientbound(ClientboundPackets26_1.LEVEL_CHUNK_WITH_LIGHT, wrapper -> {
            final Chunk chunk = protocol.getBlockRewriter().handleChunk1_18(wrapper);
            protocol.getBlockRewriter().handleBlockEntities(chunk, wrapper.user());

            final EntityTracker tracker = wrapper.user().getEntityTracker(Protocol26_2To26_1.class);

            for (int i = 0; i < chunk.getSections().length; i++) {
                final ChunkSection section = chunk.getSections()[i];

                final DataPalette blockPalette = section.palette(PaletteType.BLOCKS);

                boolean containsBed = false;
                for (int idx = 0; idx < blockPalette.size(); idx++) {
                    if (bedBlockState(blockPalette.idByIndex(idx))) {
                        containsBed = true;
                        break;
                    }
                }

                if (!containsBed) {
                    continue;
                }

                for (int idx = 0; idx < ChunkSection.SIZE; idx++) {
                    if (!bedBlockState(blockPalette.idAt(idx))) {
                        continue;
                    }

                    final byte packedXZ = (byte) (ChunkSection.xFromIndex(idx) << 4 | ChunkSection.zFromIndex(idx));
                    final short y = (short) (ChunkSection.yFromIndex(idx) + tracker.currentMinY() + (i << 4));
                    chunk.blockEntities().add(new BlockEntityImpl(packedXZ, y,
                        BED_BLOCK_ENTITY_TYPE_ID, new CompoundTag()));
                }
            }
        });

        protocol.replaceClientbound(ClientboundPackets26_1.BLOCK_UPDATE, wrapper -> {
            final BlockPosition pos = wrapper.passthrough(Types.BLOCK_POSITION1_14);

            final int blockId = wrapper.read(Types.VAR_INT);
            final int mappedBlockId = protocol.getMappingData().getNewBlockStateId(blockId);
            wrapper.write(Types.VAR_INT, mappedBlockId);

            if (bedBlockState(blockId)) {
                sendBedEntity(wrapper.user(), pos);
            }
        });

        protocol.replaceClientbound(ClientboundPackets26_1.SECTION_BLOCKS_UPDATE, wrapper -> {
            final long position = wrapper.passthrough(Types.LONG);

            final int chunkX = (int) (position >> 42);
            final int chunkY = (int) (position << 44 >> 44);
            final int chunkZ = (int) (position << 22 >> 42);

            for (final BlockChangeRecord record : wrapper.passthrough(Types.VAR_LONG_BLOCK_CHANGE_ARRAY)) {
                final int blockId = record.getBlockId();
                record.setBlockId(protocol.getMappingData().getNewBlockStateId(blockId));

                if (!bedBlockState(blockId)) {
                    continue;
                }

                final int x = record.getSectionX() + (chunkX << 4);
                final int y = record.getSectionY() + (chunkY << 4);
                final int z = record.getSectionZ() + (chunkZ << 4);
                sendBedEntity(wrapper.user(), new BlockPosition(x, y, z));
            }
        });
    }

    private static boolean bedBlockState(final int blockStateId) {
        return blockStateId >= 1931 && blockStateId <= 2186; // all bed states
    }

    private static void sendBedEntity(final UserConnection user, final BlockPosition pos) {
        final PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets26_1.BLOCK_ENTITY_DATA, user);
        wrapper.write(Types.BLOCK_POSITION1_14, pos);
        wrapper.write(Types.VAR_INT, BED_BLOCK_ENTITY_TYPE_ID);
        wrapper.write(Types.TRUSTED_COMPOUND_TAG, new CompoundTag());
        wrapper.send(Protocol26_2To26_1.class);
    }

    @Override
    protected void handleItemDataComponentsToClient(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToClient(connection, item, container);
        downgradeData(protocol.types().structuredDataKeys(), container);
    }

    @Override
    protected void handleItemDataComponentsToServer(final UserConnection connection, final Item item, final StructuredDataContainer container) {
        super.handleItemDataComponentsToServer(connection, item, container);
        upgradeData(container);
    }

    @Override
    protected void restoreBackupData(final Item item, final StructuredDataContainer container, final CompoundTag customData) {
        super.restoreBackupData(item, container, customData);
    }

    @Override
    protected void backupInconvertibleData(final UserConnection connection, final Item item, final StructuredDataContainer dataContainer, final CompoundTag backupTag) {
        super.backupInconvertibleData(connection, item, dataContainer, backupTag);
    }
}
