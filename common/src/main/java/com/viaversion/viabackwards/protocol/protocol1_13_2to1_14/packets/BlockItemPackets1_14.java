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
package com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.packets;

import com.google.common.collect.ImmutableSet;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter;
import com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.storage.ChunkLightStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.Environment;
import com.viaversion.viaversion.api.minecraft.chunks.*;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_14;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_13;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_14;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.api.type.types.version.Types1_13_2;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ChatRewriter;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.Protocol1_14To1_13_2;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BlockItemPackets1_14 extends com.viaversion.viabackwards.api.rewriters.ItemRewriter<ClientboundPackets1_14, ServerboundPackets1_13, Protocol1_13_2To1_14> {

    private EnchantmentRewriter enchantmentRewriter;

    public BlockItemPackets1_14(Protocol1_13_2To1_14 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerServerbound(ServerboundPackets1_13.EDIT_BOOK, wrapper -> handleItemToServer(wrapper.passthrough(Type.ITEM1_13_2)));

        protocol.registerClientbound(ClientboundPackets1_14.OPEN_WINDOW, wrapper -> {
            int windowId = wrapper.read(Type.VAR_INT);
            wrapper.write(Type.UNSIGNED_BYTE, (short) windowId);

            int type = wrapper.read(Type.VAR_INT);
            String stringType = null;
            String containerTitle = null;
            int slotSize = 0;
            if (type < 6) {
                if (type == 2) containerTitle = "Barrel";
                stringType = "minecraft:container";
                slotSize = (type + 1) * 9;
            } else {
                switch (type) {
                    case 11:
                        stringType = "minecraft:crafting_table";
                        break;
                    case 9: //blast furnace
                    case 20: //smoker
                    case 13: //furnace
                    case 14: //grindstone
                        if (type == 9) containerTitle = "Blast Furnace";
                        else if (type == 20) containerTitle = "Smoker";
                        else if (type == 14) containerTitle = "Grindstone";
                        stringType = "minecraft:furnace";
                        slotSize = 3;
                        break;
                    case 6:
                        stringType = "minecraft:dropper";
                        slotSize = 9;
                        break;
                    case 12:
                        stringType = "minecraft:enchanting_table";
                        break;
                    case 10:
                        stringType = "minecraft:brewing_stand";
                        slotSize = 5;
                        break;
                    case 18:
                        stringType = "minecraft:villager";
                        break;
                    case 8:
                        stringType = "minecraft:beacon";
                        slotSize = 1;
                        break;
                    case 21: //cartography_table
                    case 7:
                        if (type == 21) containerTitle = "Cartography Table";
                        stringType = "minecraft:anvil";
                        break;
                    case 15:
                        stringType = "minecraft:hopper";
                        slotSize = 5;
                        break;
                    case 19:
                        stringType = "minecraft:shulker_box";
                        slotSize = 27;
                        break;
                }
            }

            if (stringType == null) {
                ViaBackwards.getPlatform().getLogger().warning("Can't open inventory for 1.13 player! Type: " + type);
                wrapper.cancel();
                return;
            }

            wrapper.write(Type.STRING, stringType);

            JsonElement title = wrapper.read(Type.COMPONENT);
            if (containerTitle != null) {
                // Don't rewrite renamed, only translatable titles
                JsonObject object;
                if (title.isJsonObject() && (object = title.getAsJsonObject()).has("translate")) {
                    // Don't rewrite other 9x3 translatable containers
                    if (type != 2 || object.getAsJsonPrimitive("translate").getAsString().equals("container.barrel")) {
                        title = ChatRewriter.legacyTextToJson(containerTitle);
                    }
                }
            }

            wrapper.write(Type.COMPONENT, title);
            wrapper.write(Type.UNSIGNED_BYTE, (short) slotSize);
        });

        // Horse window -> Open Window
        protocol.registerClientbound(ClientboundPackets1_14.OPEN_HORSE_WINDOW, ClientboundPackets1_13.OPEN_WINDOW, wrapper -> {
            wrapper.passthrough(Type.UNSIGNED_BYTE); // Window id
            wrapper.write(Type.STRING, "EntityHorse"); // Type
            JsonObject object = new JsonObject();
            object.addProperty("translate", "minecraft.horse");
            wrapper.write(Type.COMPONENT, object); // Title
            wrapper.write(Type.UNSIGNED_BYTE, wrapper.read(Type.VAR_INT).shortValue()); // Number of slots
            wrapper.passthrough(Type.INT); // Entity id
        });

        BlockRewriter<ClientboundPackets1_14> blockRewriter = BlockRewriter.legacy(protocol);

        registerSetCooldown(ClientboundPackets1_14.COOLDOWN);
        registerWindowItems(ClientboundPackets1_14.WINDOW_ITEMS, Type.ITEM1_13_2_SHORT_ARRAY);
        registerSetSlot(ClientboundPackets1_14.SET_SLOT, Type.ITEM1_13_2);
        registerAdvancements(ClientboundPackets1_14.ADVANCEMENTS, Type.ITEM1_13_2);

        // Trade List -> Plugin Message
        protocol.registerClientbound(ClientboundPackets1_14.TRADE_LIST, ClientboundPackets1_13.PLUGIN_MESSAGE, wrapper -> {
            wrapper.write(Type.STRING, "minecraft:trader_list");

            int windowId = wrapper.read(Type.VAR_INT);
            wrapper.write(Type.INT, windowId);

            int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
            for (int i = 0; i < size; i++) {
                // Input Item
                Item input = wrapper.read(Type.ITEM1_13_2);
                input = handleItemToClient(input);
                wrapper.write(Type.ITEM1_13_2, input);


                // Output Item
                Item output = wrapper.read(Type.ITEM1_13_2);
                output = handleItemToClient(output);
                wrapper.write(Type.ITEM1_13_2, output);

                boolean secondItem = wrapper.passthrough(Type.BOOLEAN); // Has second item
                if (secondItem) {
                    // Second Item
                    Item second = wrapper.read(Type.ITEM1_13_2);
                    second = handleItemToClient(second);
                    wrapper.write(Type.ITEM1_13_2, second);
                }

                wrapper.passthrough(Type.BOOLEAN); // Trade disabled
                wrapper.passthrough(Type.INT); // Number of tools uses
                wrapper.passthrough(Type.INT); // Maximum number of trade uses

                wrapper.read(Type.INT);
                wrapper.read(Type.INT);
                wrapper.read(Type.FLOAT);
            }
            wrapper.read(Type.VAR_INT);
            wrapper.read(Type.VAR_INT);
            wrapper.read(Type.BOOLEAN);
        });

        // Open Book -> Plugin Message
        protocol.registerClientbound(ClientboundPackets1_14.OPEN_BOOK, ClientboundPackets1_13.PLUGIN_MESSAGE, wrapper -> {
            wrapper.write(Type.STRING, "minecraft:book_open");
            wrapper.passthrough(Type.VAR_INT);
        });

        protocol.registerClientbound(ClientboundPackets1_14.ENTITY_EQUIPMENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.VAR_INT); // 1 - Slot ID
                map(Type.ITEM1_13_2); // 2 - Item

                handler(itemToClientHandler(Type.ITEM1_13_2));

                handler(wrapper -> {
                    int entityId = wrapper.get(Type.VAR_INT, 0);
                    EntityType entityType = wrapper.user().getEntityTracker(Protocol1_13_2To1_14.class).entityType(entityId);
                    if (entityType == null) return;

                    if (entityType.isOrHasParent(EntityTypes1_14.ABSTRACT_HORSE)) {
                        wrapper.setPacketType(ClientboundPackets1_13.ENTITY_METADATA);
                        wrapper.resetReader();
                        wrapper.passthrough(Type.VAR_INT);
                        wrapper.read(Type.VAR_INT);
                        Item item = wrapper.read(Type.ITEM1_13_2);
                        int armorType = item == null || item.identifier() == 0 ? 0 : item.identifier() - 726;
                        if (armorType < 0 || armorType > 3) {
                            wrapper.cancel();
                            return;
                        }
                        List<Metadata> metadataList = new ArrayList<>();
                        metadataList.add(new Metadata(16, Types1_13_2.META_TYPES.varIntType, armorType));
                        wrapper.write(Types1_13.METADATA_LIST, metadataList);
                    }
                });
            }
        });

        RecipeRewriter<ClientboundPackets1_14> recipeHandler = new RecipeRewriter<>(protocol);
        final Set<String> removedTypes = ImmutableSet.of("crafting_special_suspiciousstew", "blasting", "smoking", "campfire_cooking", "stonecutting");
        protocol.registerClientbound(ClientboundPackets1_14.DECLARE_RECIPES, wrapper -> {
            int size = wrapper.passthrough(Type.VAR_INT);
            int deleted = 0;
            for (int i = 0; i < size; i++) {
                String type = wrapper.read(Type.STRING);
                String id = wrapper.read(Type.STRING); // Recipe Identifier
                type = Key.stripMinecraftNamespace(type);
                if (removedTypes.contains(type)) {
                    switch (type) {
                        case "blasting":
                        case "smoking":
                        case "campfire_cooking":
                            wrapper.read(Type.STRING); // Group
                            wrapper.read(Type.ITEM1_13_2_ARRAY); // Ingredients
                            wrapper.read(Type.ITEM1_13_2);
                            wrapper.read(Type.FLOAT); // EXP
                            wrapper.read(Type.VAR_INT); // Cooking time
                            break;
                        case "stonecutting":
                            wrapper.read(Type.STRING); // Group?
                            wrapper.read(Type.ITEM1_13_2_ARRAY); // Ingredients
                            wrapper.read(Type.ITEM1_13_2); // Result
                            break;
                    }
                    deleted++;
                    continue;
                }
                wrapper.write(Type.STRING, id);
                wrapper.write(Type.STRING, type);

                // Handle the rest of the types
                recipeHandler.handleRecipeType(wrapper, type);
            }
            wrapper.set(Type.VAR_INT, 0, size - deleted);
        });


        registerClickWindow(ServerboundPackets1_13.CLICK_WINDOW, Type.ITEM1_13_2);
        registerCreativeInvAction(ServerboundPackets1_13.CREATIVE_INVENTORY_ACTION, Type.ITEM1_13_2);

        protocol.registerClientbound(ClientboundPackets1_14.BLOCK_BREAK_ANIMATION, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.POSITION1_14, Type.POSITION1_8);
                map(Type.BYTE);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_14, Type.POSITION1_8);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.BLOCK_ACTION, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_14, Type.POSITION1_8); // Location
                map(Type.UNSIGNED_BYTE); // Action id
                map(Type.UNSIGNED_BYTE); // Action param
                map(Type.VAR_INT); // Block id - /!\ NOT BLOCK STATE
                handler(wrapper -> {
                    int mappedId = protocol.getMappingData().getNewBlockId(wrapper.get(Type.VAR_INT, 0));
                    if (mappedId == -1) {
                        wrapper.cancel();
                        return;
                    }
                    wrapper.set(Type.VAR_INT, 0, mappedId);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.BLOCK_CHANGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_14, Type.POSITION1_8);
                map(Type.VAR_INT);
                handler(wrapper -> {
                    int id = wrapper.get(Type.VAR_INT, 0);

                    wrapper.set(Type.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(id));
                });
            }
        });

        blockRewriter.registerMultiBlockChange(ClientboundPackets1_14.MULTI_BLOCK_CHANGE);

        protocol.registerClientbound(ClientboundPackets1_14.EXPLOSION, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.FLOAT); // X
                map(Type.FLOAT); // Y
                map(Type.FLOAT); // Z
                map(Type.FLOAT); // Radius
                handler(wrapper -> {
                    for (int i = 0; i < 3; i++) {
                        float coord = wrapper.get(Type.FLOAT, i);

                        if (coord < 0f) {
                            coord = (float) Math.floor(coord);
                            wrapper.set(Type.FLOAT, i, coord);
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.CHUNK_DATA, wrapper -> {
            ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
            Chunk chunk = wrapper.read(ChunkType1_14.TYPE);
            wrapper.write(ChunkType1_13.forEnvironment(clientWorld.getEnvironment()), chunk);

            ChunkLightStorage.ChunkLight chunkLight = wrapper.user().get(ChunkLightStorage.class).getStoredLight(chunk.getX(), chunk.getZ());
            for (int i = 0; i < chunk.getSections().length; i++) {
                ChunkSection section = chunk.getSections()[i];
                if (section == null) continue;

                ChunkSectionLight sectionLight = new ChunkSectionLightImpl();
                section.setLight(sectionLight);
                if (chunkLight == null) {
                    sectionLight.setBlockLight(ChunkLightStorage.FULL_LIGHT);
                    if (clientWorld.getEnvironment() == Environment.NORMAL) {
                        sectionLight.setSkyLight(ChunkLightStorage.FULL_LIGHT);
                    }
                } else {
                    byte[] blockLight = chunkLight.blockLight()[i];
                    sectionLight.setBlockLight(blockLight != null ? blockLight : ChunkLightStorage.FULL_LIGHT);
                    if (clientWorld.getEnvironment() == Environment.NORMAL) {
                        byte[] skyLight = chunkLight.skyLight()[i];
                        sectionLight.setSkyLight(skyLight != null ? skyLight : ChunkLightStorage.FULL_LIGHT);
                    }
                }

                DataPalette palette = section.palette(PaletteType.BLOCKS);
                if (Via.getConfig().isNonFullBlockLightFix() && section.getNonAirBlocksCount() != 0 && sectionLight.hasBlockLight()) {
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                int id = palette.idAt(x, y, z);
                                if (Protocol1_14To1_13_2.MAPPINGS.getNonFullBlocks().contains(id)) {
                                    sectionLight.getBlockLightNibbleArray().set(x, y, z, 0);
                                }
                            }
                        }
                    }
                }

                for (int j = 0; j < palette.size(); j++) {
                    int mappedBlockStateId = protocol.getMappingData().getNewBlockStateId(palette.idByIndex(j));
                    palette.setIdByIndex(j, mappedBlockStateId);
                }
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.UNLOAD_CHUNK, wrapper -> {
            int x = wrapper.passthrough(Type.INT);
            int z = wrapper.passthrough(Type.INT);
            wrapper.user().get(ChunkLightStorage.class).unloadChunk(x, z);
        });

        protocol.registerClientbound(ClientboundPackets1_14.EFFECT, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Effect Id
                map(Type.POSITION1_14, Type.POSITION1_8); // Location
                map(Type.INT); // Data
                handler(wrapper -> {
                    int id = wrapper.get(Type.INT, 0);
                    int data = wrapper.get(Type.INT, 1);
                    if (id == 1010) { // Play record
                        wrapper.set(Type.INT, 1, protocol.getMappingData().getNewItemId(data));
                    } else if (id == 2001) { // Block break + block break sound
                        wrapper.set(Type.INT, 1, protocol.getMappingData().getNewBlockStateId(data));
                    }
                });
            }
        });

        registerSpawnParticle(ClientboundPackets1_14.SPAWN_PARTICLE, Type.ITEM1_13_2, Type.FLOAT);

        protocol.registerClientbound(ClientboundPackets1_14.MAP_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.BYTE);
                map(Type.BOOLEAN);
                read(Type.BOOLEAN); // Locked
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.SPAWN_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_14, Type.POSITION1_8);
            }
        });
    }

    @Override
    protected void registerRewrites() {
        enchantmentRewriter = new EnchantmentRewriter(this, false);
        enchantmentRewriter.registerEnchantment("minecraft:multishot", "§7Multishot");
        enchantmentRewriter.registerEnchantment("minecraft:quick_charge", "§7Quick Charge");
        enchantmentRewriter.registerEnchantment("minecraft:piercing", "§7Piercing");
    }

    @Override
    public Item handleItemToClient(Item item) {
        if (item == null) return null;
        super.handleItemToClient(item);

        // Lore now uses JSON
        CompoundTag tag = item.tag();
        CompoundTag display;
        if (tag != null && (display = tag.get("display")) != null) {
            ListTag lore = display.get("Lore");
            if (lore != null) {
                saveListTag(display, lore, "Lore");

                for (Tag loreEntry : lore) {
                    if (!(loreEntry instanceof StringTag)) continue;

                    StringTag loreEntryTag = (StringTag) loreEntry;
                    String value = loreEntryTag.getValue();
                    if (value != null && !value.isEmpty()) {
                        loreEntryTag.setValue(ChatRewriter.jsonToLegacyText(value));
                    }
                }
            }
        }

        enchantmentRewriter.handleToClient(item);
        return item;
    }

    @Override
    public Item handleItemToServer(Item item) {
        if (item == null) return null;

        // Lore now uses JSON
        CompoundTag tag = item.tag();
        CompoundTag display;
        if (tag != null && (display = tag.get("display")) != null) {
            // Transform to json if no backup tag is found (else process that in the super method)
            ListTag lore = display.get("Lore");
            if (lore != null && !hasBackupTag(display, "Lore")) {
                for (Tag loreEntry : lore) {
                    if (loreEntry instanceof StringTag) {
                        StringTag loreEntryTag = (StringTag) loreEntry;
                        loreEntryTag.setValue(ChatRewriter.legacyTextToJsonString(loreEntryTag.getValue()));
                    }
                }
            }
        }

        enchantmentRewriter.handleToServer(item);

        // Call this last to check for the backup lore above
        super.handleItemToServer(item);
        return item;
    }
}
