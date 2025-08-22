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
package com.viaversion.viabackwards.protocol.v1_16to1_15_2.rewriter;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntArrayTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.LongArrayTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter;
import com.viaversion.viabackwards.api.rewriters.MapColorRewriter;
import com.viaversion.viabackwards.protocol.v1_16_2to1_16_1.storage.BiomeStorage;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.Protocol1_16To1_15_2;
import com.viaversion.viabackwards.protocol.v1_16to1_15_2.data.MapColorMappings1_15_2;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_15;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_16;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ServerboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_14_4to1_15.packet.ClientboundPackets1_15;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.packet.ClientboundPackets1_16;
import com.viaversion.viaversion.protocols.v1_15_2to1_16.rewriter.ItemPacketRewriter1_16;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.CompactArrayUtil;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.UUIDUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlockItemPacketRewriter1_16 extends BackwardsItemRewriter<ClientboundPackets1_16, ServerboundPackets1_14, Protocol1_16To1_15_2> {

    private EnchantmentRewriter enchantmentRewriter;

    public BlockItemPacketRewriter1_16(Protocol1_16To1_15_2 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_SHORT_ARRAY);
    }

    @Override
    protected void registerPackets() {
        BlockRewriter<ClientboundPackets1_16> blockRewriter = BlockRewriter.for1_14(protocol);

        RecipeRewriter<ClientboundPackets1_16> recipeRewriter = new RecipeRewriter<>(protocol);
        // Remove new smithing type, only in this handler
        protocol.registerClientbound(ClientboundPackets1_16.UPDATE_RECIPES, wrapper -> {
            int size = wrapper.passthrough(Types.VAR_INT);
            int newSize = size;
            for (int i = 0; i < size; i++) {
                String originalType = wrapper.read(Types.STRING);
                String type = Key.stripMinecraftNamespace(originalType);
                if (type.equals("smithing")) {
                    newSize--;

                    wrapper.read(Types.STRING);
                    wrapper.read(Types.ITEM1_13_2_ARRAY);
                    wrapper.read(Types.ITEM1_13_2_ARRAY);
                    wrapper.read(Types.ITEM1_13_2);
                    continue;
                }

                wrapper.write(Types.STRING, originalType);
                wrapper.passthrough(Types.STRING); // Recipe Identifier
                recipeRewriter.handleRecipeType(wrapper, type);
            }

            wrapper.set(Types.VAR_INT, 0, newSize);
        });

        registerCooldown(ClientboundPackets1_16.COOLDOWN);
        registerSetContent(ClientboundPackets1_16.CONTAINER_SET_CONTENT);
        registerSetSlot(ClientboundPackets1_16.CONTAINER_SET_SLOT);
        registerMerchantOffers(ClientboundPackets1_16.MERCHANT_OFFERS);
        registerAdvancements(ClientboundPackets1_16.UPDATE_ADVANCEMENTS);

        blockRewriter.registerBlockBreakAck(ClientboundPackets1_16.BLOCK_BREAK_ACK);
        blockRewriter.registerBlockEvent(ClientboundPackets1_16.BLOCK_EVENT);
        blockRewriter.registerBlockUpdate(ClientboundPackets1_16.BLOCK_UPDATE);
        blockRewriter.registerChunkBlocksUpdate(ClientboundPackets1_16.CHUNK_BLOCKS_UPDATE);
        blockRewriter.registerLevelChunk(ClientboundPackets1_16.LEVEL_CHUNK, ChunkType1_16.TYPE, ChunkType1_15.TYPE, (connection, chunk) -> {
            CompoundTag heightMaps = chunk.getHeightMap();
            for (Tag heightMapTag : heightMaps.values()) {
                if (!(heightMapTag instanceof LongArrayTag heightMap)) {
                    continue;
                }

                int[] heightMapData = new int[256];
                CompactArrayUtil.iterateCompactArrayWithPadding(9, heightMapData.length, heightMap.getValue(), (i, v) -> heightMapData[i] = v);
                heightMap.setValue(CompactArrayUtil.createCompactArray(9, heightMapData.length, i -> heightMapData[i]));
            }

            if (chunk.isBiomeData()) {
                if (connection.getProtocolInfo().serverProtocolVersion().newerThanOrEqualTo(ProtocolVersion.v1_16_2)) {
                    BiomeStorage biomeStorage = connection.get(BiomeStorage.class);
                    for (int i = 0; i < 1024; i++) {
                        int biome = chunk.getBiomeData()[i];
                        int legacyBiome = biomeStorage.legacyBiome(biome);
                        if (legacyBiome == -1) {
                            protocol.getLogger().warning("Biome sent that does not exist in the biome registry: " + biome);
                            legacyBiome = 1;
                        }
                        chunk.getBiomeData()[i] = legacyBiome;
                    }
                } else {
                    for (int i = 0; i < 1024; i++) {
                        int biome = chunk.getBiomeData()[i];
                        switch (biome) {
                            case 170, 171, 172, 173 -> chunk.getBiomeData()[i] = 8;
                        }
                    }
                }
            }

            if (chunk.getBlockEntities() == null) return;
            for (CompoundTag blockEntity : chunk.getBlockEntities()) {
                handleBlockEntity(blockEntity);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16.SET_EQUIPMENT, ClientboundPackets1_15.SET_EQUIPPED_ITEM, wrapper -> {
            int entityId = wrapper.passthrough(Types.VAR_INT);

            List<EquipmentData> equipmentData = new ArrayList<>();
            byte slot;
            do {
                slot = wrapper.read(Types.BYTE);
                Item item = handleItemToClient(wrapper.user(), wrapper.read(Types.ITEM1_13_2));
                int rawSlot = slot & 0x7F;
                equipmentData.add(new EquipmentData(rawSlot, item));
            } while ((slot & 0xFFFFFF80) != 0);

            // Send first data in the current packet
            EquipmentData firstData = equipmentData.get(0);
            wrapper.write(Types.VAR_INT, firstData.slot);
            wrapper.write(Types.ITEM1_13_2, firstData.item);

            // If there are more items, send new packets for them
            for (int i = 1; i < equipmentData.size(); i++) {
                PacketWrapper equipmentPacket = wrapper.create(ClientboundPackets1_15.SET_EQUIPPED_ITEM);
                EquipmentData data = equipmentData.get(i);
                equipmentPacket.write(Types.VAR_INT, entityId);
                equipmentPacket.write(Types.VAR_INT, data.slot);
                equipmentPacket.write(Types.ITEM1_13_2, data.item);
                equipmentPacket.send(Protocol1_16To1_15_2.class);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16.LIGHT_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // x
                map(Types.VAR_INT); // y
                read(Types.BOOLEAN);
            }
        });

        blockRewriter.registerLevelEvent(ClientboundPackets1_16.LEVEL_EVENT, 1010, 2001);

        protocol.registerClientbound(ClientboundPackets1_16.CONTAINER_SET_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE); // Window id
                map(Types.SHORT); // Property
                map(Types.SHORT); // Value
                handler(wrapper -> {
                    short property = wrapper.get(Types.SHORT, 0);
                    if (property >= 4 && property <= 6) { // Enchantment id
                        short enchantmentId = wrapper.get(Types.SHORT, 1);
                        if (enchantmentId > 11) { // soul_speed
                            wrapper.set(Types.SHORT, 1, --enchantmentId);
                        } else if (enchantmentId == 11) {
                            wrapper.set(Types.SHORT, 1, (short) 9);
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16.MAP_ITEM_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Map ID
                map(Types.BYTE); // Scale
                map(Types.BOOLEAN); // Tracking Position
                map(Types.BOOLEAN); // Locked
                handler(MapColorRewriter.getRewriteHandler(MapColorMappings1_15_2::getMappedColor));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16.BLOCK_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Types.BLOCK_POSITION1_14); // Position
            wrapper.passthrough(Types.UNSIGNED_BYTE); // Action
            CompoundTag tag = wrapper.passthrough(Types.NAMED_COMPOUND_TAG);
            handleBlockEntity(tag);
        });

        registerContainerClick(ServerboundPackets1_14.CONTAINER_CLICK);
        registerSetCreativeModeSlot(ServerboundPackets1_14.SET_CREATIVE_MODE_SLOT);

        protocol.registerServerbound(ServerboundPackets1_14.EDIT_BOOK, wrapper -> handleItemToServer(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)));
    }

    private void handleBlockEntity(CompoundTag tag) {
        String id = tag.getString("id");
        if (id == null) return;

        id = Key.namespaced(id);
        if (id.equals("minecraft:conduit")) {
            Tag targetUuidTag = tag.remove("Target");
            if (!(targetUuidTag instanceof IntArrayTag)) return;

            // Target -> target_uuid
            UUID targetUuid = UUIDUtil.fromIntArray((int[]) targetUuidTag.getValue());
            tag.putString("target_uuid", targetUuid.toString());
        } else if (id.equals("minecraft:skull")) {
            if (!(tag.remove("SkullOwner") instanceof CompoundTag skullOwnerTag)) return;

            if (skullOwnerTag.remove("Id") instanceof IntArrayTag ownerUuidTag) {
                UUID ownerUuid = UUIDUtil.fromIntArray(ownerUuidTag.getValue());
                skullOwnerTag.putString("Id", ownerUuid.toString());
            }

            // SkullOwner -> Owner
            CompoundTag ownerTag = new CompoundTag();
            for (Map.Entry<String, Tag> entry : skullOwnerTag) {
                ownerTag.put(entry.getKey(), entry.getValue());
            }
            tag.put("Owner", ownerTag);
        }
    }

    @Override
    protected void registerRewrites() {
        enchantmentRewriter = new EnchantmentRewriter(this);
        enchantmentRewriter.registerEnchantment("minecraft:soul_speed", "§7Soul Speed");
    }

    @Override
    public Item handleItemToClient(UserConnection connection, Item item) {
        if (item == null) return null;

        item = super.handleItemToClient(connection, item);

        CompoundTag tag = item.tag();
        if (item.identifier() == 771 && tag != null) {
            CompoundTag ownerTag = tag.getCompoundTag("SkullOwner");
            if (ownerTag != null) {
                IntArrayTag idTag = ownerTag.getIntArrayTag("Id");
                if (idTag != null) {
                    UUID ownerUuid = UUIDUtil.fromIntArray(idTag.getValue());
                    ownerTag.putString("Id", ownerUuid.toString());
                }
            }
        }

        // Handle hover event changes in written book pages
        if (item.identifier() == 759 && tag != null) {
            ListTag<StringTag> pagesTag = tag.getListTag("pages", StringTag.class);
            if (pagesTag != null) {
                for (StringTag page : pagesTag) {
                    JsonElement jsonElement = protocol.getComponentRewriter().processText(connection, page.getValue());
                    page.setValue(jsonElement.toString());
                }
            }
        }

        ItemPacketRewriter1_16.newToOldAttributes(item);
        enchantmentRewriter.handleToClient(item);
        return item;
    }

    @Override
    public Item handleItemToServer(UserConnection connection, Item item) {
        if (item == null) return null;

        int identifier = item.identifier();
        item = super.handleItemToServer(connection, item);

        CompoundTag tag = item.tag();
        if (identifier == 771 && tag != null) {
            CompoundTag ownerTag = tag.getCompoundTag("SkullOwner");
            if (ownerTag != null) {
                StringTag idTag = ownerTag.getStringTag("Id");
                if (idTag != null) {
                    UUID ownerUuid = UUID.fromString(idTag.getValue());
                    ownerTag.put("Id", new IntArrayTag(UUIDUtil.toIntArray(ownerUuid)));
                }
            }
        }

        ItemPacketRewriter1_16.oldToNewAttributes(item);
        enchantmentRewriter.handleToServer(item);
        return item;
    }

    private record EquipmentData(int slot, Item item) {
    }
}
