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
package com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.packets;

import com.viaversion.viabackwards.api.rewriters.ItemRewriter;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.Protocol1_17_1To1_18;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.data.BlockEntityIds;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.chunks.BaseChunk;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_17;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_18;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_17_1to1_17.ClientboundPackets1_17_1;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.MathUtil;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class BlockItemPackets1_18 extends ItemRewriter<ClientboundPackets1_18, ServerboundPackets1_17, Protocol1_17_1To1_18> {

    public BlockItemPackets1_18(final Protocol1_17_1To1_18 protocol) {
        super(protocol, Type.ITEM1_13_2, Type.ITEM1_13_2_ARRAY);
    }

    @Override
    protected void registerPackets() {
        new RecipeRewriter<>(protocol).register(ClientboundPackets1_18.DECLARE_RECIPES);

        registerSetCooldown(ClientboundPackets1_18.COOLDOWN);
        registerWindowItems1_17_1(ClientboundPackets1_18.WINDOW_ITEMS);
        registerSetSlot1_17_1(ClientboundPackets1_18.SET_SLOT);
        registerEntityEquipmentArray(ClientboundPackets1_18.ENTITY_EQUIPMENT);
        registerTradeList(ClientboundPackets1_18.TRADE_LIST);
        registerAdvancements(ClientboundPackets1_18.ADVANCEMENTS);
        registerClickWindow1_17_1(ServerboundPackets1_17.CLICK_WINDOW);

        protocol.registerClientbound(ClientboundPackets1_18.EFFECT, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Effect id
                map(Type.POSITION1_14); // Location
                map(Type.INT); // Data
                handler(wrapper -> {
                    int id = wrapper.get(Type.INT, 0);
                    int data = wrapper.get(Type.INT, 1);
                    if (id == 1010) { // Play record
                        wrapper.set(Type.INT, 1, protocol.getMappingData().getNewItemId(data));
                    }
                });
            }
        });

        registerCreativeInvAction(ServerboundPackets1_17.CREATIVE_INVENTORY_ACTION);

        protocol.registerClientbound(ClientboundPackets1_18.SPAWN_PARTICLE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Particle id
                map(Type.BOOLEAN); // Override limiter
                map(Type.DOUBLE); // X
                map(Type.DOUBLE); // Y
                map(Type.DOUBLE); // Z
                map(Type.FLOAT); // Offset X
                map(Type.FLOAT); // Offset Y
                map(Type.FLOAT); // Offset Z
                map(Type.FLOAT); // Max speed
                map(Type.INT); // Particle Count
                handler(wrapper -> {
                    int id = wrapper.get(Type.INT, 0);
                    if (id == 3) { // Block marker
                        int blockState = wrapper.read(Type.VAR_INT);
                        if (blockState == 7786) { // Light block
                            wrapper.set(Type.INT, 0, 3);
                        } else {
                            // Else assume barrier block
                            wrapper.set(Type.INT, 0, 2);
                        }
                        return;
                    }

                    ParticleMappings mappings = protocol.getMappingData().getParticleMappings();
                    if (mappings.isBlockParticle(id)) {
                        int data = wrapper.passthrough(Type.VAR_INT);
                        wrapper.set(Type.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(data));
                    } else if (mappings.isItemParticle(id)) {
                        handleItemToClient(wrapper.passthrough(Type.ITEM1_13_2));
                    }

                    int newId = protocol.getMappingData().getNewParticleId(id);
                    if (newId != id) {
                        wrapper.set(Type.INT, 0, newId);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_18.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_14);
                handler(wrapper -> {
                    final int id = wrapper.read(Type.VAR_INT);
                    final CompoundTag tag = wrapper.read(Type.NAMED_COMPOUND_TAG);

                    final int mappedId = BlockEntityIds.mappedId(id);
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
                    final Position pos = wrapper.get(Type.POSITION1_14, 0);

                    // The protocol converters downstream rely on this field, let's add it back
                    newTag.put("id", new StringTag(Key.namespaced(identifier)));

                    // Weird glitches happen with the 1.17 client and below if these fields are missing
                    // Some examples are block entity models becoming invisible (e.g.: signs, banners)
                    newTag.put("x", new IntTag(pos.x()));
                    newTag.put("y", new IntTag(pos.y()));
                    newTag.put("z", new IntTag(pos.z()));

                    handleSpawner(id, newTag);
                    wrapper.write(Type.UNSIGNED_BYTE, (short) mappedId);
                    wrapper.write(Type.NAMED_COMPOUND_TAG, newTag);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_18.CHUNK_DATA, wrapper -> {
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
                tag.put("x", new IntTag((oldChunk.getX() << 4) + blockEntity.sectionX()));
                tag.put("y", new IntTag(blockEntity.y()));
                tag.put("z", new IntTag((oldChunk.getZ() << 4) + blockEntity.sectionZ()));
                tag.put("id", new StringTag(Key.namespaced(id)));
            }

            final Chunk chunk = new BaseChunk(oldChunk.getX(), oldChunk.getZ(), true, false, mask,
                    oldChunk.getSections(), biomeData, oldChunk.getHeightMap(), blockEntityTags);
            wrapper.write(new ChunkType1_17(tracker.currentWorldSectionHeight()), chunk);

            // Create and send light packet first
            final PacketWrapper lightPacket = wrapper.create(ClientboundPackets1_17_1.UPDATE_LIGHT);
            lightPacket.write(Type.VAR_INT, chunk.getX());
            lightPacket.write(Type.VAR_INT, chunk.getZ());
            lightPacket.write(Type.BOOLEAN, wrapper.read(Type.BOOLEAN)); // Trust edges
            lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE)); // Sky light mask
            lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE)); // Block light mask
            lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE)); // Empty sky light mask
            lightPacket.write(Type.LONG_ARRAY_PRIMITIVE, wrapper.read(Type.LONG_ARRAY_PRIMITIVE)); // Empty block light mask

            final int skyLightLength = wrapper.read(Type.VAR_INT);
            lightPacket.write(Type.VAR_INT, skyLightLength);
            for (int i = 0; i < skyLightLength; i++) {
                lightPacket.write(Type.BYTE_ARRAY_PRIMITIVE, wrapper.read(Type.BYTE_ARRAY_PRIMITIVE));
            }

            final int blockLightLength = wrapper.read(Type.VAR_INT);
            lightPacket.write(Type.VAR_INT, blockLightLength);
            for (int i = 0; i < blockLightLength; i++) {
                lightPacket.write(Type.BYTE_ARRAY_PRIMITIVE, wrapper.read(Type.BYTE_ARRAY_PRIMITIVE));
            }

            lightPacket.send(Protocol1_17_1To1_18.class);
        });

        protocol.cancelClientbound(ClientboundPackets1_18.SET_SIMULATION_DISTANCE);
    }

    private void handleSpawner(final int typeId, final CompoundTag tag) {
        if (typeId == 8) {
            final CompoundTag spawnData = tag.get("SpawnData");
            final CompoundTag entity;
            if (spawnData != null && (entity = spawnData.get("entity")) != null) {
                tag.put("SpawnData", entity);
            }
        }
    }
}
