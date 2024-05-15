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
package com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.rewriter;

import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.Protocol1_16_2To1_16_1;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord1_8;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_16;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_16_2;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ClientboundPackets1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ServerboundPackets1_16;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.protocols.v1_16_1to1_16_2.packet.ClientboundPackets1_16_2;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;

public class BlockItemPacketRewriter1_16_2 extends BackwardsItemRewriter<ClientboundPackets1_16_2, ServerboundPackets1_16, Protocol1_16_2To1_16_1> {

    public BlockItemPacketRewriter1_16_2(Protocol1_16_2To1_16_1 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_SHORT_ARRAY);
    }

    @Override
    protected void registerPackets() {
        BlockRewriter<ClientboundPackets1_16_2> blockRewriter = BlockRewriter.for1_14(protocol);

        new RecipeRewriter<>(protocol).register(ClientboundPackets1_16_2.UPDATE_RECIPES);

        registerCooldown(ClientboundPackets1_16_2.COOLDOWN);
        registerSetContent(ClientboundPackets1_16_2.CONTAINER_SET_CONTENT);
        registerSetSlot(ClientboundPackets1_16_2.CONTAINER_SET_SLOT);
        registerSetEquipment(ClientboundPackets1_16_2.SET_EQUIPMENT);
        registerMerchantOffers(ClientboundPackets1_16_2.MERCHANT_OFFERS);
        registerAdvancements(ClientboundPackets1_16_2.UPDATE_ADVANCEMENTS);

        protocol.registerClientbound(ClientboundPackets1_16_2.RECIPE, wrapper -> {
            wrapper.passthrough(Types.VAR_INT);
            wrapper.passthrough(Types.BOOLEAN); // Open
            wrapper.passthrough(Types.BOOLEAN); // Filter
            wrapper.passthrough(Types.BOOLEAN); // Furnace Open
            wrapper.passthrough(Types.BOOLEAN); // Filter furnace
            // Blast furnace / smoker
            wrapper.read(Types.BOOLEAN);
            wrapper.read(Types.BOOLEAN);
            wrapper.read(Types.BOOLEAN);
            wrapper.read(Types.BOOLEAN);
        });

        blockRewriter.registerBlockBreakAck(ClientboundPackets1_16_2.BLOCK_BREAK_ACK);
        blockRewriter.registerBlockEvent(ClientboundPackets1_16_2.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_16_2.BLOCK_UPDATE);
        blockRewriter.registerLevelChunk(ClientboundPackets1_16_2.LEVEL_CHUNK, ChunkType1_16_2.TYPE, ChunkType1_16.TYPE, (connection, chunk) -> {
            chunk.setIgnoreOldLightData(true);

            for (CompoundTag blockEntity : chunk.getBlockEntities()) {
                if (blockEntity != null) {
                    handleBlockEntity(blockEntity);
                }
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16_2.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14);
                map(Types.UNSIGNED_BYTE);
                handler(wrapper -> handleBlockEntity(wrapper.passthrough(Types.NAMED_COMPOUND_TAG)));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16_2.SECTION_BLOCKS_UPDATE, ClientboundPackets1_16.CHUNK_BLOCKS_UPDATE, wrapper -> {
            long chunkPosition = wrapper.read(Types.LONG);
            wrapper.read(Types.BOOLEAN); // Ignore old light data

            int chunkX = (int) (chunkPosition >> 42);
            int chunkY = (int) (chunkPosition << 44 >> 44);
            int chunkZ = (int) (chunkPosition << 22 >> 42);
            wrapper.write(Types.INT, chunkX);
            wrapper.write(Types.INT, chunkZ);

            BlockChangeRecord[] blockChangeRecord = wrapper.read(Types.VAR_LONG_BLOCK_CHANGE_ARRAY);
            wrapper.write(Types.BLOCK_CHANGE_ARRAY, blockChangeRecord);
            for (int i = 0; i < blockChangeRecord.length; i++) {
                BlockChangeRecord record = blockChangeRecord[i];
                int blockId = protocol.getMappingData().getNewBlockStateId(record.getBlockId());
                // Relative y -> absolute y
                blockChangeRecord[i] = new BlockChangeRecord1_8(record.getSectionX(), record.getY(chunkY), record.getSectionZ(), blockId);
            }
        });

        blockRewriter.registerLevelEvent(ClientboundPackets1_16_2.LEVEL_EVENT, 1010, 2001);

        registerLevelParticles(ClientboundPackets1_16_2.LEVEL_PARTICLES, Types.DOUBLE);

        registerContainerClick(ServerboundPackets1_16.CONTAINER_CLICK);
        registerSetCreativeModeSlot(ServerboundPackets1_16.SET_CREATIVE_MODE_SLOT);
        protocol.registerServerbound(ServerboundPackets1_16.EDIT_BOOK, wrapper -> handleItemToServer(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)));
    }

    private void handleBlockEntity(CompoundTag tag) {
        StringTag idTag = tag.getStringTag("id");
        if (idTag == null) return;
        if (idTag.getValue().equals("minecraft:skull")) {
            // Workaround an old client bug: MC-68487
            CompoundTag skullOwnerTag = tag.getCompoundTag("SkullOwner");
            if (skullOwnerTag == null) return;

            if (!skullOwnerTag.contains("Id")) return;

            CompoundTag properties = skullOwnerTag.getCompoundTag("Properties");
            if (properties == null) return;

            ListTag<CompoundTag> textures = properties.getListTag("textures", CompoundTag.class);
            if (textures == null) return;

            CompoundTag first = !textures.isEmpty() ? textures.get(0) : null;
            if (first == null) return;

            // Make the client cache the skinprofile over this uuid
            int hashCode = first.get("Value").getValue().hashCode();
            int[] uuidIntArray = {hashCode, 0, 0, 0}; //TODO split texture in 4 for a lower collision chance
            skullOwnerTag.put("Id", new IntArrayTag(uuidIntArray));
        }
    }
}
