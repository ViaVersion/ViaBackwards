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
package com.viaversion.viabackwards.protocol.v1_14to1_13_2.rewriter;

import com.google.common.collect.ImmutableSet;
import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.Protocol1_14To1_13_2;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.ChunkLightStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.Environment;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSectionLight;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSectionLightImpl;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_14;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.entitydata.EntityData;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_13;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_14;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.api.type.types.version.Types1_13_2;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.libs.gson.JsonParseException;
import com.viaversion.viaversion.libs.mcstructs.text.utils.TextUtils;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.Protocol1_13_2To1_14;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.RecipeRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.SerializerVersion;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class BlockItemPacketRewriter1_14 extends BackwardsItemRewriter<ClientboundPackets1_14, ServerboundPackets1_13, Protocol1_14To1_13_2> {

    private EnchantmentRewriter enchantmentRewriter;

    public BlockItemPacketRewriter1_14(Protocol1_14To1_13_2 protocol) {
        super(protocol, Types.ITEM1_13_2, Types.ITEM1_13_2_SHORT_ARRAY);
    }

    @Override
    protected void registerPackets() {
        protocol.registerServerbound(ServerboundPackets1_13.EDIT_BOOK, wrapper -> handleItemToServer(wrapper.user(), wrapper.passthrough(Types.ITEM1_13_2)));

        protocol.registerClientbound(ClientboundPackets1_14.OPEN_SCREEN, wrapper -> {
            int windowId = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.UNSIGNED_BYTE, (short) windowId);

            int type = wrapper.read(Types.VAR_INT);
            String stringType = null;
            String containerTitle = null;
            int slotSize = 0;
            if (type < 6) {
                if (type == 2) containerTitle = "Barrel";
                stringType = "minecraft:container";
                slotSize = (type + 1) * 9;
            } else {
                switch (type) {
                    case 11 -> stringType = "minecraft:crafting_table";
                    case 9, 20, 13, 14 -> {
                        if (type == 9) containerTitle = "Blast Furnace";
                        else if (type == 20) containerTitle = "Smoker";
                        else if (type == 14) containerTitle = "Grindstone";
                        stringType = "minecraft:furnace";
                        slotSize = 3;
                    }
                    case 6 -> {
                        stringType = "minecraft:dropper";
                        slotSize = 9;
                    }
                    case 12 -> stringType = "minecraft:enchanting_table";
                    case 10 -> {
                        stringType = "minecraft:brewing_stand";
                        slotSize = 5;
                    }
                    case 18 -> stringType = "minecraft:villager";
                    case 8 -> {
                        stringType = "minecraft:beacon";
                        slotSize = 1;
                    }
                    case 21, 7 -> {
                        if (type == 21) containerTitle = "Cartography Table";
                        stringType = "minecraft:anvil";
                    }
                    case 15 -> {
                        stringType = "minecraft:hopper";
                        slotSize = 5;
                    }
                    case 19 -> {
                        stringType = "minecraft:shulker_box";
                        slotSize = 27;
                    }
                }
            }

            if (stringType == null) {
                protocol.getLogger().warning("Can't open inventory for player! Type: " + type);
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.STRING, stringType);

            JsonElement title = wrapper.read(Types.COMPONENT);
            if (containerTitle != null) {
                // Don't rewrite renamed, only translatable titles
                JsonObject object;
                if (title.isJsonObject() && (object = title.getAsJsonObject()).has("translate")) {
                    // Don't rewrite other 9x3 translatable containers
                    if (type != 2 || object.getAsJsonPrimitive("translate").getAsString().equals("container.barrel")) {
                        title = ComponentUtil.legacyToJson(containerTitle);
                    }
                }
            }

            wrapper.write(Types.COMPONENT, title);
            wrapper.write(Types.UNSIGNED_BYTE, (short) slotSize);
        });

        // Horse window -> Open Window
        protocol.registerClientbound(ClientboundPackets1_14.HORSE_SCREEN_OPEN, ClientboundPackets1_13.OPEN_SCREEN, wrapper -> {
            wrapper.passthrough(Types.UNSIGNED_BYTE); // Window id
            wrapper.write(Types.STRING, "EntityHorse"); // Type
            JsonObject object = new JsonObject();
            object.addProperty("translate", "minecraft.horse");
            wrapper.write(Types.COMPONENT, object); // Title
            wrapper.write(Types.UNSIGNED_BYTE, wrapper.read(Types.VAR_INT).shortValue()); // Number of slots
            wrapper.passthrough(Types.INT); // Entity id
        });

        BlockRewriter<ClientboundPackets1_14> blockRewriter = BlockRewriter.legacy(protocol);

        registerCooldown(ClientboundPackets1_14.COOLDOWN);
        registerSetContent(ClientboundPackets1_14.CONTAINER_SET_CONTENT);
        registerSetSlot(ClientboundPackets1_14.CONTAINER_SET_SLOT);
        registerAdvancements(ClientboundPackets1_14.UPDATE_ADVANCEMENTS);

        // Trade List -> Plugin Message
        protocol.registerClientbound(ClientboundPackets1_14.MERCHANT_OFFERS, ClientboundPackets1_13.CUSTOM_PAYLOAD, wrapper -> {
            wrapper.write(Types.STRING, "minecraft:trader_list");

            int windowId = wrapper.read(Types.VAR_INT);
            wrapper.write(Types.INT, windowId);

            int size = wrapper.passthrough(Types.UNSIGNED_BYTE);
            for (int i = 0; i < size; i++) {
                // Input Item
                Item input = wrapper.read(Types.ITEM1_13_2);
                input = handleItemToClient(wrapper.user(), input);
                wrapper.write(Types.ITEM1_13_2, input);


                // Output Item
                Item output = wrapper.read(Types.ITEM1_13_2);
                output = handleItemToClient(wrapper.user(), output);
                wrapper.write(Types.ITEM1_13_2, output);

                boolean secondItem = wrapper.passthrough(Types.BOOLEAN); // Has second item
                if (secondItem) {
                    // Second Item
                    Item second = wrapper.read(Types.ITEM1_13_2);
                    second = handleItemToClient(wrapper.user(), second);
                    wrapper.write(Types.ITEM1_13_2, second);
                }

                wrapper.passthrough(Types.BOOLEAN); // Trade disabled
                wrapper.passthrough(Types.INT); // Number of tools uses
                wrapper.passthrough(Types.INT); // Maximum number of trade uses

                wrapper.read(Types.INT);
                wrapper.read(Types.INT);
                wrapper.read(Types.FLOAT);
            }
            wrapper.read(Types.VAR_INT);
            wrapper.read(Types.VAR_INT);
            wrapper.read(Types.BOOLEAN);
        });

        // Open Book -> Plugin Message
        protocol.registerClientbound(ClientboundPackets1_14.OPEN_BOOK, ClientboundPackets1_13.CUSTOM_PAYLOAD, wrapper -> {
            wrapper.write(Types.STRING, "minecraft:book_open");
            wrapper.passthrough(Types.VAR_INT);
        });

        protocol.registerClientbound(ClientboundPackets1_14.SET_EQUIPPED_ITEM, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Entity ID
                map(Types.VAR_INT); // 1 - Slot ID
                map(Types.ITEM1_13_2); // 2 - Item

                handler(wrapper -> handleItemToClient(wrapper.user(), wrapper.get(Types.ITEM1_13_2, 0)));

                handler(wrapper -> {
                    int entityId = wrapper.get(Types.VAR_INT, 0);
                    EntityType entityType = wrapper.user().getEntityTracker(Protocol1_14To1_13_2.class).entityType(entityId);
                    if (entityType == null) return;

                    if (entityType.isOrHasParent(EntityTypes1_14.ABSTRACT_HORSE)) {
                        wrapper.setPacketType(ClientboundPackets1_13.SET_ENTITY_DATA);
                        wrapper.resetReader();
                        wrapper.passthrough(Types.VAR_INT);
                        wrapper.read(Types.VAR_INT);
                        Item item = wrapper.read(Types.ITEM1_13_2);
                        int armorType = item == null || item.identifier() == 0 ? 0 : item.identifier() - 726;
                        if (armorType < 0 || armorType > 3) {
                            wrapper.cancel();
                            return;
                        }
                        List<EntityData> entityDataList = new ArrayList<>();
                        entityDataList.add(new EntityData(16, Types1_13_2.ENTITY_DATA_TYPES.varIntType, armorType));
                        wrapper.write(Types1_13.ENTITY_DATA_LIST, entityDataList);
                    }
                });
            }
        });

        RecipeRewriter<ClientboundPackets1_14> recipeHandler = new RecipeRewriter<>(protocol);
        final Set<String> removedTypes = ImmutableSet.of("crafting_special_suspiciousstew", "blasting", "smoking", "campfire_cooking", "stonecutting");
        protocol.registerClientbound(ClientboundPackets1_14.UPDATE_RECIPES, wrapper -> {
            int size = wrapper.passthrough(Types.VAR_INT);
            int deleted = 0;
            for (int i = 0; i < size; i++) {
                String type = wrapper.read(Types.STRING);
                String id = wrapper.read(Types.STRING); // Recipe Identifier
                type = Key.stripMinecraftNamespace(type);
                if (removedTypes.contains(type)) {
                    switch (type) {
                        case "blasting", "smoking", "campfire_cooking" -> {
                            wrapper.read(Types.STRING); // Group
                            wrapper.read(Types.ITEM1_13_2_ARRAY); // Ingredients
                            wrapper.read(Types.ITEM1_13_2);
                            wrapper.read(Types.FLOAT); // EXP
                            wrapper.read(Types.VAR_INT); // Cooking time
                        }
                        case "stonecutting" -> {
                            wrapper.read(Types.STRING); // Group?
                            wrapper.read(Types.ITEM1_13_2_ARRAY); // Ingredients
                            wrapper.read(Types.ITEM1_13_2); // Result
                        }
                    }
                    deleted++;
                    continue;
                }
                wrapper.write(Types.STRING, id);
                wrapper.write(Types.STRING, type);

                // Handle the rest of the types
                recipeHandler.handleRecipeType(wrapper, type);
            }
            wrapper.set(Types.VAR_INT, 0, size - deleted);
        });


        registerContainerClick(ServerboundPackets1_13.CONTAINER_CLICK);
        registerSetCreativeModeSlot(ServerboundPackets1_13.SET_CREATIVE_MODE_SLOT);

        protocol.registerClientbound(ClientboundPackets1_14.BLOCK_DESTRUCTION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.BLOCK_POSITION1_14, Types.BLOCK_POSITION1_8);
                map(Types.BYTE);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14, Types.BLOCK_POSITION1_8);
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.BLOCK_EVENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14, Types.BLOCK_POSITION1_8); // Location
                map(Types.UNSIGNED_BYTE); // Action id
                map(Types.UNSIGNED_BYTE); // Action param
                map(Types.VAR_INT); // Block id - /!\ NOT BLOCK STATE
                handler(wrapper -> {
                    int mappedId = protocol.getMappingData().getNewBlockId(wrapper.get(Types.VAR_INT, 0));
                    if (mappedId == -1) {
                        wrapper.cancel();
                        return;
                    }
                    wrapper.set(Types.VAR_INT, 0, mappedId);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.BLOCK_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14, Types.BLOCK_POSITION1_8);
                map(Types.VAR_INT);
                handler(wrapper -> {
                    int id = wrapper.get(Types.VAR_INT, 0);

                    wrapper.set(Types.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(id));
                });
            }
        });

        blockRewriter.registerChunkBlocksUpdate(ClientboundPackets1_14.CHUNK_BLOCKS_UPDATE);

        protocol.registerClientbound(ClientboundPackets1_14.EXPLODE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.FLOAT); // X
                map(Types.FLOAT); // Y
                map(Types.FLOAT); // Z
                map(Types.FLOAT); // Radius
                handler(wrapper -> {
                    for (int i = 0; i < 3; i++) {
                        float coord = wrapper.get(Types.FLOAT, i);

                        if (coord < 0f) {
                            coord = (float) Math.floor(coord);
                            wrapper.set(Types.FLOAT, i, coord);
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.LEVEL_CHUNK, wrapper -> {
            ClientWorld clientWorld = wrapper.user().getClientWorld(Protocol1_14To1_13_2.class);
            Chunk chunk = wrapper.read(ChunkType1_14.TYPE);
            wrapper.write(ChunkType1_13.forEnvironment(clientWorld.getEnvironment()), chunk);

            ChunkLightStorage.ChunkLight chunkLight = wrapper.user().get(ChunkLightStorage.class).getStoredLight(chunk.getX(), chunk.getZ());
            for (int i = 0; i < chunk.getSections().length; i++) {
                ChunkSection section = chunk.getSections()[i];
                if (section == null) continue;

                ChunkSectionLight sectionLight = ChunkSectionLightImpl.createWithBlockLight();
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
                                if (Protocol1_13_2To1_14.MAPPINGS.getNonFullBlocks().contains(id)) {
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

        protocol.registerClientbound(ClientboundPackets1_14.FORGET_LEVEL_CHUNK, wrapper -> {
            int x = wrapper.passthrough(Types.INT);
            int z = wrapper.passthrough(Types.INT);
            wrapper.user().get(ChunkLightStorage.class).unloadChunk(x, z);
        });

        protocol.registerClientbound(ClientboundPackets1_14.LEVEL_EVENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Effect Id
                map(Types.BLOCK_POSITION1_14, Types.BLOCK_POSITION1_8); // Location
                map(Types.INT); // Data
                handler(wrapper -> {
                    int id = wrapper.get(Types.INT, 0);
                    int data = wrapper.get(Types.INT, 1);
                    if (id == 1010) { // Play record
                        wrapper.set(Types.INT, 1, protocol.getMappingData().getNewItemId(data));
                    } else if (id == 2001) { // Block break + block break sound
                        wrapper.set(Types.INT, 1, protocol.getMappingData().getNewBlockStateId(data));
                    }
                });
            }
        });

        registerLevelParticles(ClientboundPackets1_14.LEVEL_PARTICLES, Types.FLOAT);

        protocol.registerClientbound(ClientboundPackets1_14.MAP_ITEM_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.BYTE);
                map(Types.BOOLEAN);
                read(Types.BOOLEAN); // Locked
            }
        });

        protocol.registerClientbound(ClientboundPackets1_14.SET_DEFAULT_SPAWN_POSITION, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_14, Types.BLOCK_POSITION1_8);
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
    public Item handleItemToClient(UserConnection connection, Item item) {
        if (item == null) return null;
        super.handleItemToClient(connection, item);

        // Lore now uses JSON
        CompoundTag tag = item.tag();
        CompoundTag display;
        if (tag != null && (display = tag.getCompoundTag("display")) != null) {
            ListTag<StringTag> lore = display.getListTag("Lore", StringTag.class);
            if (lore != null) {
                saveListTag(display, lore, "Lore");

                try {
                    final Iterator<StringTag> each = lore.iterator();
                    while (each.hasNext()) {
                        final StringTag loreEntry = each.next();
                        final var component = SerializerVersion.V1_12.toComponent(loreEntry.getValue());
                        if (component == null) {
                            each.remove();
                            continue;
                        }
                        TextUtils.setTranslator(component, s -> Protocol1_12_2To1_13.MAPPINGS.getMojangTranslation().
                            getOrDefault(s, TranslatableRewriter.getTranslatableMappings("1.14").get(s)));
                        loreEntry.setValue(component.asLegacyFormatString());
                    }
                } catch (final JsonParseException e) {
                    display.remove("Lore");
                }
            }
        }

        enchantmentRewriter.handleToClient(item);
        return item;
    }

    @Override
    public Item handleItemToServer(UserConnection connection, Item item) {
        if (item == null) return null;

        // Lore now uses JSON
        CompoundTag tag = item.tag();
        CompoundTag display;
        if (tag != null && (display = tag.getCompoundTag("display")) != null) {
            // Transform to json if no backup tag is found (else process that in the super method)
            ListTag<StringTag> lore = display.getListTag("Lore", StringTag.class);
            if (lore != null && !hasBackupTag(display, "Lore")) {
                for (StringTag loreEntry : lore) {
                    loreEntry.setValue(ComponentUtil.legacyToJsonString(loreEntry.getValue()));
                }
            }
        }

        enchantmentRewriter.handleToServer(item);

        // Call this last to check for the backup lore above
        super.handleItemToServer(connection, item);
        return item;
    }
}
