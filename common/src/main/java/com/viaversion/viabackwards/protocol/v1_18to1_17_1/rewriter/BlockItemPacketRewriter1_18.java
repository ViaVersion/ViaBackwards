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
package com.viaversion.viabackwards.protocol.v1_18to1_17_1.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.v1_18to1_17_1.Protocol1_18To1_17_1;
import com.viaversion.viabackwards.protocol.v1_18to1_17_1.data.BlockEntityMappings1_17_1;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.chunks.BaseChunk;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_17;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.packet.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.v1_17to1_17_1.packet.ClientboundPackets1_17_1;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.MathUtil;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class BlockItemPacketRewriter1_18 extends BackwardsItemRewriter<ClientboundPackets1_18, ServerboundPackets1_17, Protocol1_18To1_17_1> {

    public BlockItemPacketRewriter1_18(final Protocol1_18To1_17_1 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_ARRAY);
    }

    @Override
    protected void registerPackets() {
        new RecipeRewriter<>(protocol).register(ClientboundPackets1_18.UPDATE_RECIPES);

        registerCooldown(ClientboundPackets1_18.COOLDOWN);
        registerSetContent1_17_1(ClientboundPackets1_18.CONTAINER_SET_CONTENT);
        registerSetSlot1_17_1(ClientboundPackets1_18.CONTAINER_SET_SLOT);
        registerSetEquipment(ClientboundPackets1_18.SET_EQUIPMENT);
        registerMerchantOffers(ClientboundPackets1_18.MERCHANT_OFFERS);
        registerAdvancements(ClientboundPackets1_18.UPDATE_ADVANCEMENTS);
        registerContainerClick1_17_1(ServerboundPackets1_17.CONTAINER_CLICK);

        protocol.registerClientbound(ClientboundPackets1_18.LEVEL_EVENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Effect id
                map(Types.BLOCK_POSITION1_14); // Location
                map(Types.INT); // Data
                handler(wrapper -> {
                    int id = wrapper.get(Types.INT, 0);
                    int data = wrapper.get(Types.INT, 1);
                    if (id == 1010) { // Play record
                        wrapper.set(Types.INT, 1, protocol.getMappingData().getNewItemId(data));
                    }
                });
            }
        });

        registerSetCreativeModeSlot(ServerboundPackets1_17.SET_CREATIVE_MODE_SLOT);

        protocol.registerClientbound(ClientboundPackets1_18.LEVEL_PARTICLES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Particle id
                map(Types.BOOLEAN); // Override limiter
                map(Types.DOUBLE); // X
                map(Types.DOUBLE); // Y
                map(Types.DOUBLE); // Z
                map(Types.FLOAT); // Offset X
                map(Types.FLOAT); // Offset Y
                map(Types.FLOAT); // Offset Z
                map(Types.FLOAT); // Max speed
                map(Types.INT); // Particle Count
                handler(wrapper -> {
                    int id = wrapper.get(Types.INT, 0);
                    if (id == 3) { // Block marker
                        int blockState = wrapper.read(Types.VAR_INT);
                        if (blockState == 7786) { // Light block
                            wrapper.set(Types.INT, 0, 3);
                        } else {
                            // Else assume barrier block
                            wrapper.set(Types.INT, 0, 2);
                        }
                        return;
                    }

                    ParticleMappings mappings = protocol.getMappingData().getParticleMappings();
                    if (mappings.isBlockParticle(id)) {
                        int data = wrapper.passthrough(Types.VAR_INT);
                        wrapper.set(Types.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(data));
                    } else if (mappings.isItemParticle(id)) {
                        passthroughClientboundItem(wrapper);
                    }

                    int newId = protocol.getMappingData().getNewParticleId(id);
                    if (newId != id) {
                        wrapper.set(Types.INT, 0, newId);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_18.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14);
                handler(wrapper -> {
                    final int id = wrapper.read(Types.VAR_INT);
                    final CompoundTag tag = wrapper.read(Types.NAMED_COMPOUND_TAG);

                    final int mappedId = BlockEntityMappings1_17_1.mappedId(id);
                    if (mappedId == -1) {
                        wrapper.cancel();
                        return;
                    }

                    final String identifier = protocol.getMappingData().blockEntities().get(id);
                    if (identifier == null) {
                        wrapper.cancel();
                        return;
                    }

                    // The 1.18 server doesn't include the id and positions (x, y, z) in the NBT anymore
                    // If those were the only fields on the block entity (e.g.: skull, bed), we'll receive a null NBT
                    // We initialize one and add the missing fields, so it can be handled correctly down the line
                    final CompoundTag newTag = tag == null ? new CompoundTag() : tag;
                    final BlockPosition pos = wrapper.get(Types.BLOCK_POSITION1_14, 0);

                    // The protocol converters downstream rely on this field, let's add it back
                    newTag.putString("id", Key.namespaced(identifier));

                    // Weird glitches happen with the 1.17 client and below if these fields are missing
                    // Some examples are block entity models becoming invisible (e.g.: signs, banners)
                    newTag.putInt("x", pos.x());
                    newTag.putInt("y", pos.y());
                    newTag.putInt("z", pos.z());

                    handleSpawner(id, newTag);
                    wrapper.write(Types.UNSIGNED_BYTE, (short) mappedId);
                    wrapper.write(Types.NAMED_COMPOUND_TAG, newTag);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_18.LEVEL_CHUNK_WITH_LIGHT, ClientboundPackets1_17_1.LEVEL_CHUNK, wrapper -> {
            final EntityTracker tracker = protocol.getEntityRewriter().tracker(wrapper.user());
            final ChunkType1_18 chunkType = new ChunkType1_18(tracker.currentWorldSectionHeight(),
                MathUtil.ceilLog2(protocol.getMappingData().getBlockStateMappings().mappedSize()),
                MathUtil.ceilLog2(tracker.biomesSent()));
            final Chunk oldChunk = wrapper.read(chunkType);
            final ChunkSection[] sections = oldChunk.getSections();
            final BitSet mask = new BitSet(oldChunk.getSections().length);
            final int[] biomeData = new int[sections.length * ChunkSection.BIOME_SIZE];
            int biomeIndex = 0;
            for (int j = 0; j < sections.length; j++) {
                final ChunkSection section = sections[j];
                // Write biome palette into biome array
                final DataPalette biomePalette = section.palette(PaletteType.BIOMES);
                for (int i = 0; i < ChunkSection.BIOME_SIZE; i++) {
                    biomeData[biomeIndex++] = biomePalette.idAt(i);
                }

                // Rewrite to empty section
                if (section.getNonAirBlocksCount() == 0) {
                    sections[j] = null;
                } else {
                    mask.set(j);
                }
            }

            final List<CompoundTag> blockEntityTags = new ArrayList<>(oldChunk.blockEntities().size());
            for (final BlockEntity blockEntity : oldChunk.blockEntities()) {
                final String id = protocol.getMappingData().blockEntities().get(blockEntity.typeId());
                if (id == null) {
                    // Shrug
                    continue;
                }

                final CompoundTag tag;
                if (blockEntity.tag() != null) {
                    tag = blockEntity.tag();
                    handleSpawner(blockEntity.typeId(), tag);
                } else {
                    tag = new CompoundTag();
                }

                blockEntityTags.add(tag);
                tag.putInt("x", (oldChunk.getX() << 4) + blockEntity.sectionX());
                tag.putInt("y", blockEntity.y());
                tag.putInt("z", (oldChunk.getZ() << 4) + blockEntity.sectionZ());
                tag.putString("id", Key.namespaced(id));
            }

            final Chunk chunk = new BaseChunk(oldChunk.getX(), oldChunk.getZ(), true, false, mask,
                oldChunk.getSections(), biomeData, oldChunk.getHeightMap(), blockEntityTags);
            wrapper.write(new ChunkType1_17(tracker.currentWorldSectionHeight()), chunk);

            // Create and send light packet first
            final PacketWrapper lightPacket = wrapper.create(ClientboundPackets1_17_1.LIGHT_UPDATE);
            lightPacket.write(Types.VAR_INT, chunk.getX());
            lightPacket.write(Types.VAR_INT, chunk.getZ());
            lightPacket.write(Types.BOOLEAN, wrapper.read(Types.BOOLEAN)); // Trust edges
            lightPacket.write(Types.LONG_ARRAY_PRIMITIVE, wrapper.read(Types.LONG_ARRAY_PRIMITIVE)); // Sky light mask
            lightPacket.write(Types.LONG_ARRAY_PRIMITIVE, wrapper.read(Types.LONG_ARRAY_PRIMITIVE)); // Block light mask
            lightPacket.write(Types.LONG_ARRAY_PRIMITIVE, wrapper.read(Types.LONG_ARRAY_PRIMITIVE)); // Empty sky light mask
            lightPacket.write(Types.LONG_ARRAY_PRIMITIVE, wrapper.read(Types.LONG_ARRAY_PRIMITIVE)); // Empty block light mask

            final int skyLightLength = wrapper.read(Types.VAR_INT);
            lightPacket.write(Types.VAR_INT, skyLightLength);
            for (int i = 0; i < skyLightLength; i++) {
                lightPacket.write(Types.BYTE_ARRAY_PRIMITIVE, wrapper.read(Types.BYTE_ARRAY_PRIMITIVE));
            }

            final int blockLightLength = wrapper.read(Types.VAR_INT);
            lightPacket.write(Types.VAR_INT, blockLightLength);
            for (int i = 0; i < blockLightLength; i++) {
                lightPacket.write(Types.BYTE_ARRAY_PRIMITIVE, wrapper.read(Types.BYTE_ARRAY_PRIMITIVE));
            }

            lightPacket.send(Protocol1_18To1_17_1.class);
        });

        protocol.cancelClientbound(ClientboundPackets1_18.SET_SIMULATION_DISTANCE);
    }

    private void handleSpawner(final int typeId, final CompoundTag tag) {
        if (typeId == 8) {
            final CompoundTag spawnData = tag.getCompoundTag("SpawnData");
            final CompoundTag entity;
            if (spawnData != null && (entity = spawnData.getCompoundTag("entity")) != null) {
                tag.put("SpawnData", entity);
            }
        }
    }
}
