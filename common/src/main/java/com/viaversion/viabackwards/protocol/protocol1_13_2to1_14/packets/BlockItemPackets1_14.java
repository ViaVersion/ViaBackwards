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
package com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.packets;

import com.google.common.collect.ImmutableSet;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.entities.storage.EntityTracker;
import com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.storage.ChunkLightStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.Environment;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_14Types;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.minecraft.metadata.types.MetaType1_13_2;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ChatRewriter;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data.RecipeRewriter1_13_2;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.types.Chunk1_13Type;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.Protocol1_14To1_13_2;
import com.viaversion.viaversion.protocols.protocol1_14to1_13_2.types.Chunk1_14Type;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.ItemRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BlockItemPackets1_14 extends com.viaversion.viabackwards.api.rewriters.ItemRewriter<Protocol1_13_2To1_14> {

    private EnchantmentRewriter enchantmentRewriter;

    public BlockItemPackets1_14(Protocol1_13_2To1_14 protocol, TranslatableRewriter translatableRewriter) {
        super(protocol, translatableRewriter);
    }

    @Override
    protected void registerPackets() {
        protocol.registerIncoming(ServerboundPackets1_13.EDIT_BOOK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)));
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.OPEN_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
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
                    }
                });
            }
        });

        // Horse window -> Open Window
        protocol.registerOutgoing(ClientboundPackets1_14.OPEN_HORSE_WINDOW, ClientboundPackets1_13.OPEN_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.passthrough(Type.UNSIGNED_BYTE); // Window id
                        wrapper.write(Type.STRING, "EntityHorse"); // Type
                        JsonObject object = new JsonObject();
                        object.addProperty("translate", "minecraft.horse");
                        wrapper.write(Type.COMPONENT, object); // Title
                        wrapper.write(Type.UNSIGNED_BYTE, wrapper.read(Type.VAR_INT).shortValue()); // Number of slots
                        wrapper.passthrough(Type.INT); // Entity id
                    }
                });
            }
        });

        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);
        BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION);

        itemRewriter.registerSetCooldown(ClientboundPackets1_14.COOLDOWN);
        itemRewriter.registerWindowItems(ClientboundPackets1_14.WINDOW_ITEMS, Type.FLAT_VAR_INT_ITEM_ARRAY);
        itemRewriter.registerSetSlot(ClientboundPackets1_14.SET_SLOT, Type.FLAT_VAR_INT_ITEM);
        itemRewriter.registerAdvancements(ClientboundPackets1_14.ADVANCEMENTS, Type.FLAT_VAR_INT_ITEM);

        // Trade List -> Plugin Message
        protocol.registerOutgoing(ClientboundPackets1_14.TRADE_LIST, ClientboundPackets1_13.PLUGIN_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.write(Type.STRING, "minecraft:trader_list");

                        int windowId = wrapper.read(Type.VAR_INT);
                        wrapper.write(Type.INT, windowId);

                        int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                        for (int i = 0; i < size; i++) {
                            // Input Item
                            Item input = wrapper.read(Type.FLAT_VAR_INT_ITEM);
                            input = handleItemToClient(input);
                            wrapper.write(Type.FLAT_VAR_INT_ITEM, input);


                            // Output Item
                            Item output = wrapper.read(Type.FLAT_VAR_INT_ITEM);
                            output = handleItemToClient(output);
                            wrapper.write(Type.FLAT_VAR_INT_ITEM, output);

                            boolean secondItem = wrapper.passthrough(Type.BOOLEAN); // Has second item
                            if (secondItem) {
                                // Second Item
                                Item second = wrapper.read(Type.FLAT_VAR_INT_ITEM);
                                second = handleItemToClient(second);
                                wrapper.write(Type.FLAT_VAR_INT_ITEM, second);
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
                    }
                });
            }
        });

        // Open Book -> Plugin Message
        protocol.registerOutgoing(ClientboundPackets1_14.OPEN_BOOK, ClientboundPackets1_13.PLUGIN_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.write(Type.STRING, "minecraft:book_open");
                        wrapper.passthrough(Type.VAR_INT);
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.ENTITY_EQUIPMENT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Entity ID
                map(Type.VAR_INT); // 1 - Slot ID
                map(Type.FLAT_VAR_INT_ITEM); // 2 - Item

                handler(itemRewriter.itemToClientHandler(Type.FLAT_VAR_INT_ITEM));

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = wrapper.get(Type.VAR_INT, 0);
                        EntityType entityType = wrapper.user().get(EntityTracker.class).get(getProtocol()).getEntityType(entityId);
                        if (entityType == null) return;

                        if (entityType.isOrHasParent(Entity1_14Types.ABSTRACT_HORSE)) {
                            wrapper.setId(0x3F);
                            wrapper.resetReader();
                            wrapper.passthrough(Type.VAR_INT);
                            wrapper.read(Type.VAR_INT);
                            Item item = wrapper.read(Type.FLAT_VAR_INT_ITEM);
                            int armorType = item == null || item.getIdentifier() == 0 ? 0 : item.getIdentifier() - 726;
                            if (armorType < 0 || armorType > 3) {
                                ViaBackwards.getPlatform().getLogger().warning("Received invalid horse armor: " + item);
                                wrapper.cancel();
                                return;
                            }
                            List<Metadata> metadataList = new ArrayList<>();
                            metadataList.add(new Metadata(16, MetaType1_13_2.VarInt, armorType));
                            wrapper.write(Types1_13.METADATA_LIST, metadataList);
                        }
                    }
                });
            }
        });

        RecipeRewriter recipeHandler = new RecipeRewriter1_13_2(protocol, this::handleItemToClient);
        protocol.registerOutgoing(ClientboundPackets1_14.DECLARE_RECIPES, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    private final Set<String> removedTypes = ImmutableSet.of("crafting_special_suspiciousstew", "blasting", "smoking", "campfire_cooking", "stonecutting");

                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int size = wrapper.passthrough(Type.VAR_INT);
                        int deleted = 0;
                        for (int i = 0; i < size; i++) {
                            String type = wrapper.read(Type.STRING);
                            String id = wrapper.read(Type.STRING); // Recipe Identifier
                            type = type.replace("minecraft:", "");
                            if (removedTypes.contains(type)) {
                                switch (type) {
                                    case "blasting":
                                    case "smoking":
                                    case "campfire_cooking":
                                        wrapper.read(Type.STRING); // Group
                                        wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                        wrapper.read(Type.FLAT_VAR_INT_ITEM);
                                        wrapper.read(Type.FLOAT); // EXP
                                        wrapper.read(Type.VAR_INT); // Cooking time
                                        break;
                                    case "stonecutting":
                                        wrapper.read(Type.STRING); // Group?
                                        wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                        wrapper.read(Type.FLAT_VAR_INT_ITEM); // Result
                                        break;
                                }
                                deleted++;
                                continue;
                            }
                            wrapper.write(Type.STRING, id);
                            wrapper.write(Type.STRING, type);

                            // Handle the rest of the types
                            recipeHandler.handle(wrapper, type);
                        }
                        wrapper.set(Type.VAR_INT, 0, size - deleted);
                    }
                });
            }
        });


        itemRewriter.registerClickWindow(ServerboundPackets1_13.CLICK_WINDOW, Type.FLAT_VAR_INT_ITEM);
        itemRewriter.registerCreativeInvAction(ServerboundPackets1_13.CREATIVE_INVENTORY_ACTION, Type.FLAT_VAR_INT_ITEM);

        protocol.registerOutgoing(ClientboundPackets1_14.BLOCK_BREAK_ANIMATION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.POSITION1_14, Type.POSITION);
                map(Type.BYTE);
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.BLOCK_ENTITY_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14, Type.POSITION);
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.BLOCK_ACTION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14, Type.POSITION); // Location
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

        protocol.registerOutgoing(ClientboundPackets1_14.BLOCK_CHANGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14, Type.POSITION);
                map(Type.VAR_INT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.VAR_INT, 0);

                        wrapper.set(Type.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(id));
                    }
                });
            }
        });

        blockRewriter.registerMultiBlockChange(ClientboundPackets1_14.MULTI_BLOCK_CHANGE);

        protocol.registerOutgoing(ClientboundPackets1_14.EXPLOSION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.FLOAT); // X
                map(Type.FLOAT); // Y
                map(Type.FLOAT); // Z
                map(Type.FLOAT); // Radius
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        for (int i = 0; i < 3; i++) {
                            float coord = wrapper.get(Type.FLOAT, i);

                            if (coord < 0f) {
                                coord = (float) Math.floor(coord);
                                wrapper.set(Type.FLOAT, i, coord);
                            }
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                        Chunk chunk = wrapper.read(new Chunk1_14Type());
                        wrapper.write(new Chunk1_13Type(clientWorld), chunk);

                        ChunkLightStorage.ChunkLight chunkLight = wrapper.user().get(ChunkLightStorage.class).getStoredLight(chunk.getX(), chunk.getZ());
                        for (int i = 0; i < chunk.getSections().length; i++) {
                            ChunkSection section = chunk.getSections()[i];
                            if (section == null) continue;

                            if (chunkLight == null) {
                                section.setBlockLight(ChunkLightStorage.FULL_LIGHT);
                                if (clientWorld.getEnvironment() == Environment.NORMAL) {
                                    section.setSkyLight(ChunkLightStorage.FULL_LIGHT);
                                }
                            } else {
                                final byte[] blockLight = chunkLight.getBlockLight()[i];
                                section.setBlockLight(blockLight != null ? blockLight : ChunkLightStorage.FULL_LIGHT);
                                if (clientWorld.getEnvironment() == Environment.NORMAL) {
                                    final byte[] skyLight = chunkLight.getSkyLight()[i];
                                    section.setSkyLight(skyLight != null ? skyLight : ChunkLightStorage.FULL_LIGHT);
                                }
                            }

                            if (Via.getConfig().isNonFullBlockLightFix() && section.getNonAirBlocksCount() != 0 && section.hasBlockLight()) {
                                for (int x = 0; x < 16; x++) {
                                    for (int y = 0; y < 16; y++) {
                                        for (int z = 0; z < 16; z++) {
                                            int id = section.getFlatBlock(x, y, z);
                                            if (Protocol1_14To1_13_2.MAPPINGS.getNonFullBlocks().contains(id)) {
                                                section.getBlockLightNibbleArray().set(x, y, z, 0);
                                            }
                                        }
                                    }
                                }
                            }

                            for (int j = 0; j < section.getPaletteSize(); j++) {
                                int old = section.getPaletteEntry(j);
                                int newId = protocol.getMappingData().getNewBlockStateId(old);
                                section.setPaletteEntry(j, newId);
                            }
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.UNLOAD_CHUNK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int x = wrapper.passthrough(Type.INT);
                        int z = wrapper.passthrough(Type.INT);
                        wrapper.user().get(ChunkLightStorage.class).unloadChunk(x, z);
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.EFFECT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Effect Id
                map(Type.POSITION1_14, Type.POSITION); // Location
                map(Type.INT); // Data
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.INT, 0);
                        int data = wrapper.get(Type.INT, 1);
                        if (id == 1010) { // Play record
                            wrapper.set(Type.INT, 1, protocol.getMappingData().getNewItemId(data));
                        } else if (id == 2001) { // Block break + block break sound
                            wrapper.set(Type.INT, 1, protocol.getMappingData().getNewBlockStateId(data));
                        }
                    }
                });
            }
        });

        itemRewriter.registerSpawnParticle(ClientboundPackets1_14.SPAWN_PARTICLE, Type.FLAT_VAR_INT_ITEM, Type.FLOAT);

        protocol.registerOutgoing(ClientboundPackets1_14.MAP_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.BYTE);
                map(Type.BOOLEAN);
                map(Type.BOOLEAN, Type.NOTHING); // Locked
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_14.SPAWN_POSITION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14, Type.POSITION);
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
        CompoundTag tag = item.getTag();
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
        CompoundTag tag = item.getTag();
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
