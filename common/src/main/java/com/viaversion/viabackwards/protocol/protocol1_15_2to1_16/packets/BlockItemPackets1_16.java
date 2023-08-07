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
package com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.packets;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter;
import com.viaversion.viabackwards.api.rewriters.MapColorRewriter;
import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.data.MapColorRewrites;
import com.viaversion.viabackwards.protocol.protocol1_16_1to1_16_2.storage.BiomeStorage;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.UUIDIntArrayType;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonParser;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.ServerboundPackets1_14;
import com.viaversion.viaversion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;
import com.viaversion.viaversion.protocols.protocol1_15to1_14_4.types.Chunk1_15Type;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.ClientboundPackets1_16;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.packets.InventoryPackets;
import com.viaversion.viaversion.protocols.protocol1_16to1_15_2.types.Chunk1_16Type;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.CompactArrayUtil;
import com.viaversion.viaversion.util.Key;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlockItemPackets1_16 extends com.viaversion.viabackwards.api.rewriters.ItemRewriter<ClientboundPackets1_16, ServerboundPackets1_14, Protocol1_15_2To1_16> {

    private EnchantmentRewriter enchantmentRewriter;

    public BlockItemPackets1_16(Protocol1_15_2To1_16 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        BlockRewriter<ClientboundPackets1_16> blockRewriter = new BlockRewriter<>(protocol, Type.POSITION1_14);

        RecipeRewriter<ClientboundPackets1_16> recipeRewriter = new RecipeRewriter<>(protocol);
        // Remove new smithing type, only in this handler
        protocol.registerClientbound(ClientboundPackets1_16.DECLARE_RECIPES, wrapper -> {
            int size = wrapper.passthrough(Type.VAR_INT);
            int newSize = size;
            for (int i = 0; i < size; i++) {
                String originalType = wrapper.read(Type.STRING);
                String type = Key.stripMinecraftNamespace(originalType);
                if (type.equals("smithing")) {
                    newSize--;

                    wrapper.read(Type.STRING);
                    wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT);
                    wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT);
                    wrapper.read(Type.FLAT_VAR_INT_ITEM);
                    continue;
                }

                wrapper.write(Type.STRING, originalType);
                wrapper.passthrough(Type.STRING); // Recipe Identifier
                recipeRewriter.handleRecipeType(wrapper, type);
            }

            wrapper.set(Type.VAR_INT, 0, newSize);
        });

        registerSetCooldown(ClientboundPackets1_16.COOLDOWN);
        registerWindowItems(ClientboundPackets1_16.WINDOW_ITEMS, Type.FLAT_VAR_INT_ITEM_ARRAY);
        registerSetSlot(ClientboundPackets1_16.SET_SLOT, Type.FLAT_VAR_INT_ITEM);
        registerTradeList(ClientboundPackets1_16.TRADE_LIST);
        registerAdvancements(ClientboundPackets1_16.ADVANCEMENTS, Type.FLAT_VAR_INT_ITEM);

        blockRewriter.registerAcknowledgePlayerDigging(ClientboundPackets1_16.ACKNOWLEDGE_PLAYER_DIGGING);
        blockRewriter.registerBlockAction(ClientboundPackets1_16.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_16.BLOCK_CHANGE);
        blockRewriter.registerMultiBlockChange(ClientboundPackets1_16.MULTI_BLOCK_CHANGE);

        protocol.registerClientbound(ClientboundPackets1_16.ENTITY_EQUIPMENT, wrapper -> {
            int entityId = wrapper.passthrough(Type.VAR_INT);

            List<EquipmentData> equipmentData = new ArrayList<>();
            byte slot;
            do {
                slot = wrapper.read(Type.BYTE);
                Item item = handleItemToClient(wrapper.read(Type.FLAT_VAR_INT_ITEM));
                int rawSlot = slot & 0x7F;
                equipmentData.add(new EquipmentData(rawSlot, item));
            } while ((slot & 0xFFFFFF80) != 0);

            // Send first data in the current packet
            EquipmentData firstData = equipmentData.get(0);
            wrapper.write(Type.VAR_INT, firstData.slot);
            wrapper.write(Type.FLAT_VAR_INT_ITEM, firstData.item);

            // If there are more items, send new packets for them
            for (int i = 1; i < equipmentData.size(); i++) {
                PacketWrapper equipmentPacket = wrapper.create(ClientboundPackets1_15.ENTITY_EQUIPMENT);
                EquipmentData data = equipmentData.get(i);
                equipmentPacket.write(Type.VAR_INT, entityId);
                equipmentPacket.write(Type.VAR_INT, data.slot);
                equipmentPacket.write(Type.FLAT_VAR_INT_ITEM, data.item);
                equipmentPacket.send(Protocol1_15_2To1_16.class);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16.UPDATE_LIGHT, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // x
                map(Type.VAR_INT); // y
                map(Type.BOOLEAN, Type.NOTHING);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16.CHUNK_DATA, wrapper -> {
            Chunk chunk = wrapper.read(new Chunk1_16Type());
            wrapper.write(new Chunk1_15Type(), chunk);

            for (int i = 0; i < chunk.getSections().length; i++) {
                ChunkSection section = chunk.getSections()[i];
                if (section == null) {
                    continue;
                }

                DataPalette palette = section.palette(PaletteType.BLOCKS);
                for (int j = 0; j < palette.size(); j++) {
                    int mappedBlockStateId = protocol.getMappingData().getNewBlockStateId(palette.idByIndex(j));
                    palette.setIdByIndex(j, mappedBlockStateId);
                }
            }

            CompoundTag heightMaps = chunk.getHeightMap();
            for (Tag heightMapTag : heightMaps.values()) {
                LongArrayTag heightMap = (LongArrayTag) heightMapTag;
                int[] heightMapData = new int[256];
                CompactArrayUtil.iterateCompactArrayWithPadding(9, heightMapData.length, heightMap.getValue(), (i, v) -> heightMapData[i] = v);
                heightMap.setValue(CompactArrayUtil.createCompactArray(9, heightMapData.length, i -> heightMapData[i]));
            }

            if (chunk.isBiomeData()) {
                if (wrapper.user().getProtocolInfo().getServerProtocolVersion() >= ProtocolVersion.v1_16_2.getVersion()) {
                    BiomeStorage biomeStorage = wrapper.user().get(BiomeStorage.class);
                    for (int i = 0; i < 1024; i++) {
                        int biome = chunk.getBiomeData()[i];
                        int legacyBiome = biomeStorage.legacyBiome(biome);
                        if (legacyBiome == -1) {
                            ViaBackwards.getPlatform().getLogger().warning("Biome sent that does not exist in the biome registry: " + biome);
                            legacyBiome = 1;
                        }
                        chunk.getBiomeData()[i] = legacyBiome;
                    }
                } else {
                    for (int i = 0; i < 1024; i++) {
                        int biome = chunk.getBiomeData()[i];
                        switch (biome) {
                            case 170: // new nether biomes
                            case 171:
                            case 172:
                            case 173:
                                chunk.getBiomeData()[i] = 8;
                                break;
                        }
                    }
                }
            }

            if (chunk.getBlockEntities() == null) return;
            for (CompoundTag blockEntity : chunk.getBlockEntities()) {
                handleBlockEntity(blockEntity);
            }
        });

        blockRewriter.registerEffect(ClientboundPackets1_16.EFFECT, 1010, 2001);

        registerSpawnParticle(ClientboundPackets1_16.SPAWN_PARTICLE, Type.FLAT_VAR_INT_ITEM, Type.DOUBLE);

        protocol.registerClientbound(ClientboundPackets1_16.WINDOW_PROPERTY, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE); // Window id
                map(Type.SHORT); // Property
                map(Type.SHORT); // Value
                handler(wrapper -> {
                    short property = wrapper.get(Type.SHORT, 0);
                    if (property >= 4 && property <= 6) { // Enchantment id
                        short enchantmentId = wrapper.get(Type.SHORT, 1);
                        if (enchantmentId > 11) { // soul_speed
                            wrapper.set(Type.SHORT, 1, --enchantmentId);
                        } else if (enchantmentId == 11) {
                            wrapper.set(Type.SHORT, 1, (short) 9);
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16.MAP_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // Map ID
                map(Type.BYTE); // Scale
                map(Type.BOOLEAN); // Tracking Position
                map(Type.BOOLEAN); // Locked
                handler(MapColorRewriter.getRewriteHandler(MapColorRewrites::getMappedColor));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_16.BLOCK_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Type.POSITION1_14); // Position
            wrapper.passthrough(Type.UNSIGNED_BYTE); // Action
            CompoundTag tag = wrapper.passthrough(Type.NBT);
            handleBlockEntity(tag);
        });

        registerClickWindow(ServerboundPackets1_14.CLICK_WINDOW, Type.FLAT_VAR_INT_ITEM);
        registerCreativeInvAction(ServerboundPackets1_14.CREATIVE_INVENTORY_ACTION, Type.FLAT_VAR_INT_ITEM);

        protocol.registerServerbound(ServerboundPackets1_14.EDIT_BOOK, wrapper -> handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)));
    }

    private void handleBlockEntity(CompoundTag tag) {
        StringTag idTag = tag.get("id");
        if (idTag == null) return;

        String id = idTag.getValue();
        if (id.equals("minecraft:conduit")) {
            Tag targetUuidTag = tag.remove("Target");
            if (!(targetUuidTag instanceof IntArrayTag)) return;

            // Target -> target_uuid
            UUID targetUuid = UUIDIntArrayType.uuidFromIntArray((int[]) targetUuidTag.getValue());
            tag.put("target_uuid", new StringTag(targetUuid.toString()));
        } else if (id.equals("minecraft:skull")) {
            Tag skullOwnerTag = tag.remove("SkullOwner");
            if (!(skullOwnerTag instanceof CompoundTag)) return;

            CompoundTag skullOwnerCompoundTag = (CompoundTag) skullOwnerTag;
            Tag ownerUuidTag = skullOwnerCompoundTag.remove("Id");
            if (ownerUuidTag instanceof IntArrayTag) {
                UUID ownerUuid = UUIDIntArrayType.uuidFromIntArray((int[]) ownerUuidTag.getValue());
                skullOwnerCompoundTag.put("Id", new StringTag(ownerUuid.toString()));
            }

            // SkullOwner -> Owner
            CompoundTag ownerTag = new CompoundTag();
            for (Map.Entry<String, Tag> entry : skullOwnerCompoundTag) {
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
    public Item handleItemToClient(Item item) {
        if (item == null) return null;

        super.handleItemToClient(item);

        CompoundTag tag = item.tag();
        if (item.identifier() == 771 && tag != null) {
            Tag ownerTag = tag.get("SkullOwner");
            if (ownerTag instanceof CompoundTag) {
                CompoundTag ownerCompundTag = (CompoundTag) ownerTag;
                Tag idTag = ownerCompundTag.get("Id");
                if (idTag instanceof IntArrayTag) {
                    UUID ownerUuid = UUIDIntArrayType.uuidFromIntArray((int[]) idTag.getValue());
                    ownerCompundTag.put("Id", new StringTag(ownerUuid.toString()));
                }
            }
        }

        newToOldHoverEvents(item);
        InventoryPackets.newToOldAttributes(item);
        enchantmentRewriter.handleToClient(item);
        return item;
    }

    @Override
    public Item handleItemToServer(Item item) {
        if (item == null) return null;

        int identifier = item.identifier();
        super.handleItemToServer(item);

        CompoundTag tag = item.tag();
        if (identifier == 771 && tag != null) {
            Tag ownerTag = tag.get("SkullOwner");
            if (ownerTag instanceof CompoundTag) {
                CompoundTag ownerCompundTag = (CompoundTag) ownerTag;
                Tag idTag = ownerCompundTag.get("Id");
                if (idTag instanceof StringTag) {
                    UUID ownerUuid = UUID.fromString((String) idTag.getValue());
                    ownerCompundTag.put("Id", new IntArrayTag(UUIDIntArrayType.uuidToIntArray(ownerUuid)));
                }
            }
        }

        InventoryPackets.oldToNewAttributes(item);
        enchantmentRewriter.handleToServer(item);
        return item;
    }

    private static final class EquipmentData {
        private final int slot;
        private final Item item;

        private EquipmentData(final int slot, final Item item) {
            this.slot = slot;
            this.item = item;
        }
    }

    private static void searchForKeys(JsonObject jsonObject, String key, List<JsonObject> matchingObjects) {
        if (jsonObject.has(key) && jsonObject.get(key).isJsonObject()) {
            matchingObjects.add(jsonObject.getAsJsonObject(key));
        }

        for (String nestedKey : jsonObject.keySet()) {
            JsonElement nestedElement = jsonObject.get(nestedKey);
            if (nestedElement.isJsonObject()) {
                searchForKeys(nestedElement.getAsJsonObject(), key, matchingObjects);
            } else if (nestedElement.isJsonArray()) {
                for (JsonElement arrayElement : nestedElement.getAsJsonArray()) {
                    searchForKeys(arrayElement.getAsJsonObject(), key, matchingObjects);
                }
            }
        }
    }

    public static void newToOldHoverEvents(Item item){
        CompoundTag tag = item.tag();
        if(tag==null) return;

        // Check if item is a book
        ListTag pagesTag = tag.get("pages");
        if(pagesTag==null) return;

        // fix hover events on pages by changing "contents" to "value"
        for(Tag page : pagesTag){
            StringTag stringPage = (StringTag) page;
            try{
                JsonObject jsonObject = JsonParser.parseString(stringPage.getValue()).getAsJsonObject();
                List<JsonObject> hoverEvents = new ArrayList<>();
                searchForKeys(jsonObject, "hoverEvent", hoverEvents);
                for(JsonObject object : hoverEvents){
                    if(object.has("contents")){
                        object.add("value", object.remove("contents"));
                    }
                }
                if(!hoverEvents.isEmpty()){
                    stringPage.setValue(jsonObject.toString());
                }
            } catch (final Exception e){
                // Invalid json object
            }
        }
    }
}
