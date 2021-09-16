/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.Protocol1_17_1To1_18;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.data.BlockEntityIds;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.minecraft.blockentity.BlockEntity;
import com.viaversion.viaversion.api.minecraft.chunks.BaseChunk;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_17_1to1_17.ClientboundPackets1_17_1;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.types.Chunk1_17Type;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.types.Chunk1_18Type;
import com.viaversion.viaversion.util.MathUtil;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public final class BlockItemPackets1_18 extends ItemRewriter<Protocol1_17_1To1_18> {

    private static final int WIDTH_BITS = 2;
    private static final int HORIZONTAL_MASK = 3;
    private static final int BIOMES_PER_CHUNK = 4 * 4 * 4;

    public BlockItemPackets1_18(final Protocol1_17_1To1_18 protocol, final TranslatableRewriter translatableRewriter) {
        super(protocol, translatableRewriter);
    }

    @Override
    protected void registerPackets() {
        /*final BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION1_14);

        new RecipeRewriter1_16(protocol).registerDefaultHandler(ClientboundPackets1_17_1.DECLARE_RECIPES);

        registerSetCooldown(ClientboundPackets1_17_1.COOLDOWN);
        registerWindowItems(ClientboundPackets1_17_1.WINDOW_ITEMS, Type.FLAT_VAR_INT_ITEM_ARRAY);
        registerSetSlot(ClientboundPackets1_17_1.SET_SLOT, Type.FLAT_VAR_INT_ITEM);
        registerEntityEquipmentArray(ClientboundPackets1_17_1.ENTITY_EQUIPMENT, Type.FLAT_VAR_INT_ITEM);
        registerTradeList(ClientboundPackets1_17_1.TRADE_LIST, Type.FLAT_VAR_INT_ITEM);
        registerAdvancements(ClientboundPackets1_17_1.ADVANCEMENTS, Type.FLAT_VAR_INT_ITEM);
        registerClickWindow1_17(ServerboundPackets1_17.CLICK_WINDOW, Type.FLAT_VAR_INT_ITEM);

        blockRewriter.registerAcknowledgePlayerDigging(ClientboundPackets1_17_1.ACKNOWLEDGE_PLAYER_DIGGING);
        blockRewriter.registerBlockAction(ClientboundPackets1_17_1.BLOCK_ACTION);
        blockRewriter.registerEffect(ClientboundPackets1_17_1.EFFECT, 1010, 2001);
        blockRewriter.registerBlockChange(ClientboundPackets1_17_1.BLOCK_CHANGE);
        blockRewriter.registerVarLongMultiBlockChange(ClientboundPackets1_17_1.MULTI_BLOCK_CHANGE);

        registerCreativeInvAction(ServerboundPackets1_17.CREATIVE_INVENTORY_ACTION, Type.FLAT_VAR_INT_ITEM);
        protocol.registerServerbound(ServerboundPackets1_17.EDIT_BOOK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)));
            }
        });

        registerSpawnParticle(ClientboundPackets1_17_1.SPAWN_PARTICLE, Type.FLAT_VAR_INT_ITEM, Type.DOUBLE);*/

        protocol.registerClientbound(ClientboundPackets1_17_1.BLOCK_ENTITY_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14);
                handler(wrapper -> {
                    final int id = wrapper.read(Type.VAR_INT);
                    final int mappedId = BlockEntityIds.mappedId(id);
                    if (mappedId == -1) {
                        wrapper.cancel();
                        return;
                    }

                    wrapper.write(Type.UNSIGNED_BYTE, (short) mappedId);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_17_1.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    final EntityTracker tracker = protocol.getEntityRewriter().tracker(wrapper.user());
                    final Chunk1_18Type chunkType = new Chunk1_18Type(tracker.currentWorldSectionHeight(),
                            MathUtil.ceilLog2(protocol.getMappingData().getBlockStateMappings().size()),
                            MathUtil.ceilLog2(tracker.biomesSent()));
                    final Chunk oldChunk = wrapper.read(chunkType);
                    final int[] biomeData = new int[oldChunk.getSections().length * 64];
                    final ChunkSection[] sections = oldChunk.getSections();
                    for (int i = 0; i < sections.length; i++) {
                        final ChunkSection section = sections[i];
                        // Write biome palette into biome array
                        final DataPalette biomePalette = section.palette(PaletteType.BIOMES);
                        for (int biomeIndex = i * BIOMES_PER_CHUNK; biomeIndex < (i * BIOMES_PER_CHUNK) + BIOMES_PER_CHUNK; biomeIndex++) {
                            final int minX = (biomeIndex & HORIZONTAL_MASK) << 2;
                            final int minY = ((biomeIndex >> WIDTH_BITS + WIDTH_BITS) << 2) & 15;
                            final int minZ = (biomeIndex >> WIDTH_BITS & HORIZONTAL_MASK) << 2;
                            biomeData[biomeIndex] = biomePalette.idAt(minX, minY, minZ);
                        }

                        // Rewrite ids
                        /*for (int i = 0; i < section.getPaletteSize(); i++) {
                            int old = section.getPaletteEntry(i);
                            section.setPaletteEntry(i, protocol.getMappingData().getNewBlockStateId(old));
                        }*/
                    }

                    final List<CompoundTag> blockEntityTags = new ArrayList<>(oldChunk.blockEntities().size());
                    for (final BlockEntity blockEntity : oldChunk.blockEntities()) {
                        final CompoundTag tag = blockEntity.tag() != null ? blockEntity.tag() : new CompoundTag();
                        final String id = protocol.getMappingData().blockEntities().get(blockEntity.typeId());
                        if (id == null) {
                            // Shrug
                            continue;
                        }

                        blockEntityTags.add(tag);
                        tag.put("x", new IntTag((oldChunk.getX() << 4) + blockEntity.sectionX()));
                        tag.put("y", new IntTag(blockEntity.y()));
                        tag.put("z", new IntTag((oldChunk.getZ() << 4) + blockEntity.sectionZ()));
                        tag.put("id", new StringTag(id));
                    }

                    final BitSet mask = new BitSet(oldChunk.getSections().length);
                    mask.set(0, oldChunk.getSections().length);
                    final Chunk chunk = new BaseChunk(oldChunk.getX(), oldChunk.getZ(), true, false, mask,
                            oldChunk.getSections(), biomeData, oldChunk.getHeightMap(), blockEntityTags);
                    wrapper.write(new Chunk1_17Type(tracker.currentWorldSectionHeight()), chunk);

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
            }
        });
    }
}
