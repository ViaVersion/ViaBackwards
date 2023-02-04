/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_16_1to1_16_2.packets;

import com.viaversion.viabackwards.protocol.protocol1_16_1to1_16_2.Protocol1_16_1To1_16_2;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord1_8;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import com.viaversion.viaversion.protocols.protocol1_16_2to1_16_1.types.Chunk1_16_2Type;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.ServerboundPackets1_16;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.data.RecipeRewriter1_16;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.types.Chunk1_16Type;
import com.viaversion.viaversion.rewriter.BlockRewriter;

public class BlockItemPackets1_16_2 extends com.viaversion.viabackwards.api.rewriters.ItemRewriter<ClientboundPackets1_16_2, ServerboundPackets1_16, Protocol1_16_1To1_16_2> {

    public BlockItemPackets1_16_2(Protocol1_16_1To1_16_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        BlockRewriter<ClientboundPackets1_16_2> blockRewriter = new BlockRewriter<>(protocol, Type.POSITION1_14);

        new RecipeRewriter1_16<>(protocol).registerDefaultHandler(ClientboundPackets1_16_2.DECLARE_RECIPES);

        registerSetCooldown(ClientboundPackets1_16_2.COOLDOWN);
        registerWindowItems(ClientboundPackets1_16_2.WINDOW_ITEMS, Type.FLAT_VAR_INT_ITEM_ARRAY);
        registerSetSlot(ClientboundPackets1_16_2.SET_SLOT, Type.FLAT_VAR_INT_ITEM);
        registerEntityEquipmentArray(ClientboundPackets1_16_2.ENTITY_EQUIPMENT);
        registerTradeList(ClientboundPackets1_16_2.TRADE_LIST);
        registerAdvancements(ClientboundPackets1_16_2.ADVANCEMENTS, Type.FLAT_VAR_INT_ITEM);

        protocol.registerClientbound(ClientboundPackets1_16_2.UNLOCK_RECIPES, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.passthrough(Type.VAR_INT);
                    wrapper.passthrough(Type.BOOLEAN); // Open
                    wrapper.passthrough(Type.BOOLEAN); // Filter
                    wrapper.passthrough(Type.BOOLEAN); // Furnace Open
                    wrapper.passthrough(Type.BOOLEAN); // Filter furnace
                    // Blast furnace / smoker
                    wrapper.read(Type.BOOLEAN);
                    wrapper.read(Type.BOOLEAN);
                    wrapper.read(Type.BOOLEAN);
                    wrapper.read(Type.BOOLEAN);
                });
            }
        });

        blockRewriter.registerAcknowledgePlayerDigging(ClientboundPackets1_16_2.ACKNOWLEDGE_PLAYER_DIGGING);
        blockRewriter.registerBlockAction(ClientboundPackets1_16_2.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_16_2.BLOCK_CHANGE);

        protocol.registerClientbound(ClientboundPackets1_16_2.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    Chunk chunk = wrapper.read(new Chunk1_16_2Type());
                    wrapper.write(new Chunk1_16Type(), chunk);

                    chunk.setIgnoreOldLightData(true);
                    for (int i = 0; i < chunk.getSections().length; i++) {
                        ChunkSection section = chunk.getSections()[i];
                        if (section == null) continue;
                        for (int j = 0; j < section.getPaletteSize(); j++) {
                            int old = section.getPaletteEntry(j);
                            section.setPaletteEntry(j, protocol.getMappingData().getNewBlockStateId(old));
                        }
                    }

                    for (CompoundTag blockEntity : chunk.getBlockEntities()) {
                        if (blockEntity != null) {
                            handleBlockEntity(blockEntity);
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16_2.BLOCK_ENTITY_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14);
                map(Type.UNSIGNED_BYTE);
                handler(wrapper -> {
                    handleBlockEntity(wrapper.passthrough(Type.NBT));
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16_2.MULTI_BLOCK_CHANGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    long chunkPosition = wrapper.read(Type.LONG);
                    wrapper.read(Type.BOOLEAN); // Ignore old light data

                    int chunkX = (int) (chunkPosition >> 42);
                    int chunkY = (int) (chunkPosition << 44 >> 44);
                    int chunkZ = (int) (chunkPosition << 22 >> 42);
                    wrapper.write(Type.INT, chunkX);
                    wrapper.write(Type.INT, chunkZ);

                    BlockChangeRecord[] blockChangeRecord = wrapper.read(Type.VAR_LONG_BLOCK_CHANGE_RECORD_ARRAY);
                    wrapper.write(Type.BLOCK_CHANGE_RECORD_ARRAY, blockChangeRecord);
                    for (int i = 0; i < blockChangeRecord.length; i++) {
                        BlockChangeRecord record = blockChangeRecord[i];
                        int blockId = protocol.getMappingData().getNewBlockStateId(record.getBlockId());
                        // Relative y -> absolute y
                        blockChangeRecord[i] = new BlockChangeRecord1_8(record.getSectionX(), record.getY(chunkY), record.getSectionZ(), blockId);
                    }
                });
            }
        });

        blockRewriter.registerEffect(ClientboundPackets1_16_2.EFFECT, 1010, 2001);

        registerSpawnParticle(ClientboundPackets1_16_2.SPAWN_PARTICLE, Type.FLAT_VAR_INT_ITEM, Type.DOUBLE);

        registerClickWindow(ServerboundPackets1_16.CLICK_WINDOW, Type.FLAT_VAR_INT_ITEM);
        registerCreativeInvAction(ServerboundPackets1_16.CREATIVE_INVENTORY_ACTION, Type.FLAT_VAR_INT_ITEM);
        protocol.registerServerbound(ServerboundPackets1_16.EDIT_BOOK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)));
            }
        });
    }

    private void handleBlockEntity(CompoundTag tag) {
        StringTag idTag = tag.get("id");
        if (idTag == null) return;
        if (idTag.getValue().equals("minecraft:skull")) {
            // Workaround an old client bug: MC-68487
            Tag skullOwnerTag = tag.get("SkullOwner");
            if (!(skullOwnerTag instanceof CompoundTag)) return;

            CompoundTag skullOwnerCompoundTag = (CompoundTag) skullOwnerTag;
            if (!skullOwnerCompoundTag.contains("Id")) return;

            CompoundTag properties = skullOwnerCompoundTag.get("Properties");
            if (properties == null) return;

            ListTag textures = properties.get("textures");
            if (textures == null) return;

            CompoundTag first = textures.size() > 0 ? textures.get(0) : null;
            if (first == null) return;

            // Make the client cache the skinprofile over this uuid
            int hashCode = first.get("Value").getValue().hashCode();
            int[] uuidIntArray = {hashCode, 0, 0, 0}; //TODO split texture in 4 for a lower collision chance
            skullOwnerCompoundTag.put("Id", new IntArrayTag(uuidIntArray));
        }
    }
}
