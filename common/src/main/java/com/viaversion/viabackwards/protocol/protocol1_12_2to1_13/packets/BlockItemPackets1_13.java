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

package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.packets;

import com.google.common.primitives.Ints;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.FlowerPotHandler;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage.NoteBlockStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_13;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ServerboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data.BlockIdData;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data.SpawnEggRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class BlockItemPackets1_13 extends com.viaversion.viabackwards.api.rewriters.ItemRewriter<ClientboundPackets1_13, ServerboundPackets1_12_1, Protocol1_12_2To1_13> {

    private final Map<String, String> enchantmentMappings = new HashMap<>();
    private final String extraNbtTag;

    public BlockItemPackets1_13(Protocol1_12_2To1_13 protocol) {
        super(protocol, null, null);
        extraNbtTag = "VB|" + protocol.getClass().getSimpleName() + "|2";
    }

    public static boolean isDamageable(int id) {
        return id >= 256 && id <= 259 // iron shovel, pickaxe, axe, flint and steel
            || id == 261 // bow
            || id >= 267 && id <= 279 // iron sword, wooden+stone+diamond swords, shovels, pickaxes, axes
            || id >= 283 && id <= 286 // gold sword, shovel, pickaxe, axe
            || id >= 290 && id <= 294 // hoes
            || id >= 298 && id <= 317 // armors
            || id == 346 // fishing rod
            || id == 359 // shears
            || id == 398 // carrot on a stick
            || id == 442 // shield
            || id == 443; // elytra
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_13.COOLDOWN, wrapper -> {
            int itemId = wrapper.read(Type.VAR_INT);
            int oldId = protocol.getMappingData().getItemMappings().getNewId(itemId);
            if (oldId == -1) {
                wrapper.cancel();
                return;
            }

            if (SpawnEggRewriter.getEntityId(oldId).isPresent()) {
                wrapper.write(Type.VAR_INT, 383 << 4);
                return;
            }

            wrapper.write(Type.VAR_INT, oldId >> 4);
        });

        protocol.registerClientbound(ClientboundPackets1_13.BLOCK_ACTION, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_8); // Location
                map(Type.UNSIGNED_BYTE); // Action Id
                map(Type.UNSIGNED_BYTE); // Action param
                map(Type.VAR_INT); // Block Id - /!\ NOT BLOCK STATE ID
                handler(wrapper -> {
                    int blockId = wrapper.get(Type.VAR_INT, 0);

                    if (blockId == 73)
                        blockId = 25;
                    else if (blockId == 99)
                        blockId = 33;
                    else if (blockId == 92)
                        blockId = 29;
                    else if (blockId == 142)
                        blockId = 54;
                    else if (blockId == 305)
                        blockId = 146;
                    else if (blockId == 249)
                        blockId = 130;
                    else if (blockId == 257)
                        blockId = 138;
                    else if (blockId == 140)
                        blockId = 52;
                    else if (blockId == 472)
                        blockId = 209;
                    else if (blockId >= 483 && blockId <= 498)
                        blockId = blockId - 483 + 219;

                    if (blockId == 25) { // Note block
                        final NoteBlockStorage noteBlockStorage = wrapper.user().get(NoteBlockStorage.class);

                        final Position position = wrapper.get(Type.POSITION1_8, 0);
                        final Pair<Integer, Integer> update = noteBlockStorage.getNoteBlockUpdate(position);
                        if (update != null) { // Use values from block state update
                            wrapper.set(Type.UNSIGNED_BYTE, 0, update.key().shortValue());
                            wrapper.set(Type.UNSIGNED_BYTE, 1, update.value().shortValue());
                        }
                    }

                    wrapper.set(Type.VAR_INT, 0, blockId);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_8); // 0 - Position
                map(Type.UNSIGNED_BYTE); // 1 - Action
                map(Type.NAMED_COMPOUND_TAG); // 2 - NBT Data

                handler(wrapper -> {
                    BackwardsBlockEntityProvider provider = Via.getManager().getProviders().get(BackwardsBlockEntityProvider.class);

                    // TODO conduit handling
                    if (wrapper.get(Type.UNSIGNED_BYTE, 0) == 5) {
                        wrapper.cancel();
                    }

                    wrapper.set(Type.NAMED_COMPOUND_TAG, 0,
                        provider.transform(
                            wrapper.user(),
                            wrapper.get(Type.POSITION1_8, 0),
                            wrapper.get(Type.NAMED_COMPOUND_TAG, 0)
                        ));
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.UNLOAD_CHUNK, wrapper -> {
            int chunkMinX = wrapper.passthrough(Type.INT) << 4;
            int chunkMinZ = wrapper.passthrough(Type.INT) << 4;
            int chunkMaxX = chunkMinX + 15;
            int chunkMaxZ = chunkMinZ + 15;
            BackwardsBlockStorage blockStorage = wrapper.user().get(BackwardsBlockStorage.class);
            blockStorage.getBlocks().entrySet().removeIf(entry -> {
                Position position = entry.getKey();
                return position.x() >= chunkMinX && position.z() >= chunkMinZ
                    && position.x() <= chunkMaxX && position.z() <= chunkMaxZ;
            });
        });

        // Block Change
        protocol.registerClientbound(ClientboundPackets1_13.BLOCK_CHANGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_8); // 0 - Position

                handler(wrapper -> {
                    int blockState = wrapper.read(Type.VAR_INT);
                    Position position = wrapper.get(Type.POSITION1_8, 0);

                    // Note block special treatment
                    if (blockState >= 249 && blockState <= 748) { // Note block states id range
                        wrapper.user().get(NoteBlockStorage.class).storeNoteBlockUpdate(position, blockState);
                    }

                    // Store blocks
                    BackwardsBlockStorage storage = wrapper.user().get(BackwardsBlockStorage.class);
                    storage.checkAndStore(position, blockState);

                    wrapper.write(Type.VAR_INT, protocol.getMappingData().getNewBlockStateId(blockState));

                    // Flower pot special treatment
                    flowerPotSpecialTreatment(wrapper.user(), blockState, position);
                });
            }
        });

        // Multi Block Change
        protocol.registerClientbound(ClientboundPackets1_13.MULTI_BLOCK_CHANGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // 0 - Chunk X
                map(Type.INT); // 1 - Chunk Z
                map(Type.BLOCK_CHANGE_RECORD_ARRAY);
                handler(wrapper -> {
                    BackwardsBlockStorage storage = wrapper.user().get(BackwardsBlockStorage.class);

                    for (BlockChangeRecord record : wrapper.get(Type.BLOCK_CHANGE_RECORD_ARRAY, 0)) {
                        int chunkX = wrapper.get(Type.INT, 0);
                        int chunkZ = wrapper.get(Type.INT, 1);
                        int block = record.getBlockId();
                        Position position = new Position(
                            record.getSectionX() + (chunkX * 16),
                            record.getY(),
                            record.getSectionZ() + (chunkZ * 16));

                        // Store if needed
                        storage.checkAndStore(position, block);

                        // Flower pot special treatment
                        flowerPotSpecialTreatment(wrapper.user(), block, position);

                        // Change to old id
                        record.setBlockId(protocol.getMappingData().getNewBlockStateId(block));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.WINDOW_ITEMS, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE);
                map(Type.ITEM1_13_SHORT_ARRAY, Type.ITEM1_8_SHORT_ARRAY);

                handler(itemArrayToClientHandler(Type.ITEM1_8_SHORT_ARRAY));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.SET_SLOT, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE);
                map(Type.SHORT);
                map(Type.ITEM1_13, Type.ITEM1_8);

                handler(itemToClientHandler(Type.ITEM1_8));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.CHUNK_DATA, wrapper -> {
            ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

            ChunkType1_9_3 type_old = ChunkType1_9_3.forEnvironment(clientWorld.getEnvironment());
            ChunkType1_13 type = ChunkType1_13.forEnvironment(clientWorld.getEnvironment());
            Chunk chunk = wrapper.read(type);

            // Handle Block Entities before block rewrite
            BackwardsBlockEntityProvider provider = Via.getManager().getProviders().get(BackwardsBlockEntityProvider.class);
            BackwardsBlockStorage storage = wrapper.user().get(BackwardsBlockStorage.class);
            for (CompoundTag tag : chunk.getBlockEntities()) {
                StringTag idTag = tag.getStringTag("id");
                if (idTag == null) continue;

                String id = idTag.getValue();

                // Ignore if we don't handle it
                if (!provider.isHandled(id)) continue;

                int sectionIndex = tag.getNumberTag("y").asInt() >> 4;
                if (sectionIndex < 0 || sectionIndex > 15) {
                    // 1.17 chunks
                    continue;
                }

                ChunkSection section = chunk.getSections()[sectionIndex];

                int x = tag.getNumberTag("x").asInt();
                int y = tag.getNumberTag("y").asInt();
                int z = tag.getNumberTag("z").asInt();
                Position position = new Position(x, (short) y, z);

                int block = section.palette(PaletteType.BLOCKS).idAt(x & 0xF, y & 0xF, z & 0xF);
                storage.checkAndStore(position, block);

                provider.transform(wrapper.user(), position, tag);
            }

            // Rewrite new blocks to old blocks
            for (int i = 0; i < chunk.getSections().length; i++) {
                ChunkSection section = chunk.getSections()[i];
                if (section == null) {
                    continue;
                }

                DataPalette palette = section.palette(PaletteType.BLOCKS);
                // Flower pots require a special treatment, they are no longer block entities :(
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            int block = palette.idAt(x, y, z);

                            // Check if the block is a flower
                            if (FlowerPotHandler.isFlowah(block)) {
                                Position pos = new Position(
                                    (x + (chunk.getX() << 4)),
                                    (short) (y + (i << 4)),
                                    (z + (chunk.getZ() << 4))
                                );
                                // Store block
                                storage.checkAndStore(pos, block);

                                CompoundTag nbt = provider.transform(wrapper.user(), pos, "minecraft:flower_pot");

                                chunk.getBlockEntities().add(nbt);
                            }
                        }
                    }
                }

                for (int j = 0; j < palette.size(); j++) {
                    int mappedBlockStateId = protocol.getMappingData().getNewBlockStateId(palette.idByIndex(j));
                    palette.setIdByIndex(j, mappedBlockStateId);
                }
            }


            if (chunk.isBiomeData()) {
                for (int i = 0; i < 256; i++) {
                    int biome = chunk.getBiomeData()[i];
                    int newId = -1;
                    switch (biome) {
                        case 40: // end biomes
                        case 41:
                        case 42:
                        case 43:
                            newId = 9;
                            break;
                        case 47: // deep ocean biomes
                        case 48:
                        case 49:
                            newId = 24;
                            break;
                        case 50: // deep frozen... let's just pick the frozen variant
                            newId = 10;
                            break;
                        case 44: // the other new ocean biomes
                        case 45:
                        case 46:
                            newId = 0;
                            break;
                    }

                    if (newId != -1) {
                        chunk.getBiomeData()[i] = newId;
                    }
                }
            }

            wrapper.write(type_old, chunk);
        });

        protocol.registerClientbound(ClientboundPackets1_13.EFFECT, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Effect Id
                map(Type.POSITION1_8); // Location
                map(Type.INT); // Data
                handler(wrapper -> {
                    int id = wrapper.get(Type.INT, 0);
                    int data = wrapper.get(Type.INT, 1);
                    if (id == 1010) { // Play record
                        wrapper.set(Type.INT, 1, protocol.getMappingData().getItemMappings().getNewId(data) >> 4);
                    } else if (id == 2001) { // Block break + block break sound
                        data = protocol.getMappingData().getNewBlockStateId(data);
                        int blockId = data >> 4;
                        int blockData = data & 0xF;
                        wrapper.set(Type.INT, 1, (blockId & 0xFFF) | (blockData << 12));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.MAP_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.BYTE);
                map(Type.BOOLEAN);
                handler(wrapper -> {
                    int iconCount = wrapper.passthrough(Type.VAR_INT);
                    for (int i = 0; i < iconCount; i++) {
                        int type = wrapper.read(Type.VAR_INT);
                        byte x = wrapper.read(Type.BYTE);
                        byte z = wrapper.read(Type.BYTE);
                        byte direction = wrapper.read(Type.BYTE);
                        wrapper.read(Type.OPTIONAL_COMPONENT);
                        if (type > 9) {
                            wrapper.set(Type.VAR_INT, 1, wrapper.get(Type.VAR_INT, 1) - 1);
                            continue;
                        }
                        wrapper.write(Type.BYTE, (byte) ((type << 4) | (direction & 0x0F)));
                        wrapper.write(Type.BYTE, x);
                        wrapper.write(Type.BYTE, z);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.ENTITY_EQUIPMENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.VAR_INT);
                map(Type.ITEM1_13, Type.ITEM1_8);

                handler(itemToClientHandler(Type.ITEM1_8));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.WINDOW_PROPERTY, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE); // Window Id
                map(Type.SHORT); // Property
                map(Type.SHORT); // Value
                handler(wrapper -> {
                    short property = wrapper.get(Type.SHORT, 0);
                    // Enchantment table
                    if (property >= 4 && property <= 6) {
                        short oldId = wrapper.get(Type.SHORT, 1);
                        wrapper.set(Type.SHORT, 1, (short) protocol.getMappingData().getEnchantmentMappings().getNewId(oldId));
                    }
                });
            }
        });


        protocol.registerServerbound(ServerboundPackets1_12_1.CREATIVE_INVENTORY_ACTION, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.SHORT);
                map(Type.ITEM1_8, Type.ITEM1_13);

                handler(itemToServerHandler(Type.ITEM1_13));
            }
        });

        protocol.registerServerbound(ServerboundPackets1_12_1.CLICK_WINDOW, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE);
                map(Type.SHORT);
                map(Type.BYTE);
                map(Type.SHORT);
                map(Type.VAR_INT);
                map(Type.ITEM1_8, Type.ITEM1_13);

                handler(itemToServerHandler(Type.ITEM1_13));
            }
        });
    }

    @Override
    protected void registerRewrites() {
        enchantmentMappings.put("minecraft:loyalty", "§7Loyalty");
        enchantmentMappings.put("minecraft:impaling", "§7Impaling");
        enchantmentMappings.put("minecraft:riptide", "§7Riptide");
        enchantmentMappings.put("minecraft:channeling", "§7Channeling");
    }

    @Override
    public Item handleItemToClient(Item item) {
        if (item == null) return null;

        // Custom mappings/super call moved down
        int originalId = item.identifier();

        Integer rawId = null;
        boolean gotRawIdFromTag = false;

        CompoundTag tag = item.tag();

        // Use tag to get original ID and data
        Tag originalIdTag;
        if (tag != null && (originalIdTag = tag.remove(extraNbtTag)) instanceof NumberTag) {
            rawId = ((NumberTag) originalIdTag).asInt();
            gotRawIdFromTag = true;
        }

        if (rawId == null) {
            // Look for custom mappings
            super.handleItemToClient(item);

            // Handle one-way special case
            if (item.identifier() == -1) {
                if (originalId == 362) { // base/colorless shulker box
                    rawId = 0xe50000; // purple shulker box
                } else {
                    if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                        ViaBackwards.getPlatform().getLogger().warning("Failed to get 1.12 item for " + originalId);
                    }

                    rawId = 0x10000;
                }
            } else {  // Use the found custom mapping
                // Take the newly added tag
                if (tag == null) {
                    tag = item.tag();
                }

                rawId = itemIdToRaw(item.identifier(), item, tag);
            }
        }

        item.setIdentifier(rawId >> 16);
        item.setData((short) (rawId & 0xFFFF));

        // NBT changes
        if (tag != null) {
            if (isDamageable(item.identifier())) {
                Tag damageTag = tag.remove("Damage");
                if (!gotRawIdFromTag && damageTag instanceof NumberTag) {
                    item.setData(((NumberTag) damageTag).asShort());
                }
            }

            if (item.identifier() == 358) { // map
                Tag mapTag = tag.remove("map");
                if (!gotRawIdFromTag && mapTag instanceof NumberTag) {
                    item.setData(((NumberTag) mapTag).asShort());
                }
            }

            // Shield and banner
            invertShieldAndBannerId(item, tag);

            // Display Name now uses JSON
            CompoundTag display = tag.getCompoundTag("display");
            if (display != null) {
                StringTag name = display.getStringTag("Name");
                if (name != null) {
                    display.putString(extraNbtTag + "|Name", name.getValue());
                    name.setValue(protocol.jsonToLegacy(name.getValue()));
                }
            }

            // ench is now Enchantments and now uses identifiers
            rewriteEnchantmentsToClient(tag, false);
            rewriteEnchantmentsToClient(tag, true);

            rewriteCanPlaceToClient(tag, "CanPlaceOn");
            rewriteCanPlaceToClient(tag, "CanDestroy");
        }
        return item;
    }

    private int itemIdToRaw(int oldId, Item item, CompoundTag tag) {
        Optional<String> eggEntityId = SpawnEggRewriter.getEntityId(oldId);
        if (eggEntityId.isPresent()) {
            if (tag == null) {
                item.setTag(tag = new CompoundTag());
            }
            if (!tag.contains("EntityTag")) {
                CompoundTag entityTag = new CompoundTag();
                entityTag.putString("id", eggEntityId.get());
                tag.put("EntityTag", entityTag);
            }
            return 0x17f0000; // 383 << 16;
        }

        return (oldId >> 4) << 16 | oldId & 0xF;
    }

    private void rewriteCanPlaceToClient(CompoundTag tag, String tagName) {
        // The tag was manually created incorrectly so ignore rewriting it
        ListTag<?> blockTag = tag.getListTag(tagName);
        if (blockTag == null) return;

        ListTag<StringTag> newCanPlaceOn = new ListTag<>(StringTag.class);
        tag.put(extraNbtTag + "|" + tagName, blockTag.copy());
        for (Tag oldTag : blockTag) {
            Object value = oldTag.getValue();
            String[] newValues = value instanceof String ?
                BlockIdData.fallbackReverseMapping.get(Key.stripMinecraftNamespace((String) value)) : null;
            if (newValues != null) {
                for (String newValue : newValues) {
                    newCanPlaceOn.add(new StringTag(newValue));
                }
            } else {
                newCanPlaceOn.add(new StringTag(oldTag.getValue().toString()));
            }
        }
        tag.put(tagName, newCanPlaceOn);
    }

    //TODO un-ugly all of this
    private void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnch) {
        String key = storedEnch ? "StoredEnchantments" : "Enchantments";
        ListTag<CompoundTag> enchantments = tag.getListTag(key, CompoundTag.class);
        if (enchantments == null) return;

        ListTag<CompoundTag> noMapped = new ListTag<>(CompoundTag.class);
        ListTag<CompoundTag> newEnchantments = new ListTag<>(CompoundTag.class);
        List<StringTag> lore = new ArrayList<>();
        boolean hasValidEnchants = false;
        for (CompoundTag enchantmentEntry : enchantments.copy()) {
            StringTag idTag = enchantmentEntry.getStringTag("id");
            if (idTag == null) {
                continue;
            }

            String newId = idTag.getValue();
            NumberTag levelTag = enchantmentEntry.getNumberTag("lvl");
            if (levelTag == null) {
                continue;
            }

            int levelValue = levelTag.asInt();
            short level = levelValue < Short.MAX_VALUE ? (short) levelValue : Short.MAX_VALUE;

            String mappedEnchantmentId = enchantmentMappings.get(newId);
            if (mappedEnchantmentId != null) {
                lore.add(new StringTag(mappedEnchantmentId + " " + EnchantmentRewriter.getRomanNumber(level)));
                noMapped.add(enchantmentEntry);
            } else if (!newId.isEmpty()) {
                Short oldId = Protocol1_13To1_12_2.MAPPINGS.getOldEnchantmentsIds().inverse().get(Key.stripMinecraftNamespace(newId));
                if (oldId == null) {
                    if (!newId.startsWith("viaversion:legacy/")) {
                        // Custom enchant (?)
                        noMapped.add(enchantmentEntry);

                        // Some custom-enchant plugins write it into the lore manually, which would double its entry
                        if (ViaBackwards.getConfig().addCustomEnchantsToLore()) {
                            String name = newId;
                            int index = name.indexOf(':') + 1;
                            if (index != 0 && index != name.length()) {
                                name = name.substring(index);
                            }
                            name = "§7" + Character.toUpperCase(name.charAt(0)) + name.substring(1).toLowerCase(Locale.ENGLISH);

                            lore.add(new StringTag(name + " " + EnchantmentRewriter.getRomanNumber(level)));
                        }

                        if (Via.getManager().isDebug()) {
                            ViaBackwards.getPlatform().getLogger().warning("Found unknown enchant: " + newId);
                        }
                        continue;
                    } else {
                        oldId = Short.valueOf(newId.substring(18));
                    }
                }

                if (level != 0) {
                    hasValidEnchants = true;
                }

                CompoundTag newEntry = new CompoundTag();
                newEntry.putShort("id", oldId);
                newEntry.putShort("lvl", level);
                newEnchantments.add(newEntry);
            }
        }

        // Put here to hide empty enchantment from 1.14 rewrites
        if (!storedEnch && !hasValidEnchants) {
            NumberTag hideFlags = tag.getNumberTag("HideFlags");
            if (hideFlags == null) {
                hideFlags = new IntTag();
                tag.put(extraNbtTag + "|DummyEnchant", new ByteTag());
            } else {
                tag.putInt(extraNbtTag + "|OldHideFlags", hideFlags.asByte());
            }

            if (newEnchantments.isEmpty()) {
                CompoundTag enchEntry = new CompoundTag();
                enchEntry.putShort("id", (short) 0);
                enchEntry.putShort("lvl", (short) 0);
                newEnchantments.add(enchEntry);
            }

            int value = hideFlags.asByte() | 1;
            tag.putInt("HideFlags", value);
        }

        if (!noMapped.isEmpty()) {
            tag.put(extraNbtTag + "|" + key, noMapped);

            if (!lore.isEmpty()) {
                CompoundTag display = tag.getCompoundTag("display");
                if (display == null) {
                    tag.put("display", display = new CompoundTag());
                }

                ListTag<StringTag> loreTag = display.getListTag("Lore", StringTag.class);
                if (loreTag == null) {
                    display.put("Lore", loreTag = new ListTag<>(StringTag.class));
                    tag.put(extraNbtTag + "|DummyLore", new ByteTag());
                } else if (!loreTag.isEmpty()) {
                    ListTag<StringTag> oldLore = new ListTag<>(StringTag.class);
                    for (StringTag value : loreTag) {
                        oldLore.add(value.copy());
                    }
                    tag.put(extraNbtTag + "|OldLore", oldLore);

                    lore.addAll(loreTag.getValue());
                }

                loreTag.setValue(lore);
            }
        }

        tag.remove("Enchantments");
        tag.put(storedEnch ? key : "ench", newEnchantments);
    }

    @Override
    public Item handleItemToServer(Item item) {
        if (item == null) return null;
        CompoundTag tag = item.tag();

        // Save original id
        int originalId = (item.identifier() << 16 | item.data() & 0xFFFF);

        int rawId = (item.identifier() << 4 | item.data() & 0xF);

        // NBT Additions
        if (isDamageable(item.identifier())) {
            if (tag == null) item.setTag(tag = new CompoundTag());
            tag.putInt("Damage", item.data());
        }
        if (item.identifier() == 358) { // map
            if (tag == null) item.setTag(tag = new CompoundTag());
            tag.putInt("map", item.data());
        }

        // NBT Changes
        if (tag != null) {
            // Shield and banner
            invertShieldAndBannerId(item, tag);

            // Display Name now uses JSON
            CompoundTag display = tag.getCompoundTag("display");
            if (display != null) {
                StringTag name = display.getStringTag("Name");
                if (name != null) {
                    Tag via = display.remove(extraNbtTag + "|Name");
                    name.setValue(via instanceof StringTag ? ((StringTag) via).getValue() : ComponentUtil.legacyToJsonString(name.getValue()));
                }
            }

            // ench is now Enchantments and now uses identifiers
            rewriteEnchantmentsToServer(tag, false);
            rewriteEnchantmentsToServer(tag, true);

            rewriteCanPlaceToServer(tag, "CanPlaceOn");
            rewriteCanPlaceToServer(tag, "CanDestroy");

            // Handle SpawnEggs
            if (item.identifier() == 383) {
                CompoundTag entityTag = tag.getCompoundTag("EntityTag");
                StringTag identifier;
                if (entityTag != null && (identifier = entityTag.getStringTag("id")) != null) {
                    rawId = SpawnEggRewriter.getSpawnEggId(identifier.getValue());
                    if (rawId == -1) {
                        rawId = 25100288; // Bat fallback
                    } else {
                        entityTag.remove("id");
                        if (entityTag.isEmpty()) {
                            tag.remove("EntityTag");
                        }
                    }
                } else {
                    // Fallback to bat
                    rawId = 25100288;
                }
            }
            if (tag.isEmpty()) {
                item.setTag(tag = null);
            }
        }

        // Handle custom mappings
        int identifier = item.identifier();
        item.setIdentifier(rawId);
        super.handleItemToServer(item);

        // Mapped with original data, we can return here
        if (item.identifier() != rawId && item.identifier() != -1) return item;

        // Set to legacy id again
        item.setIdentifier(identifier);

        int newId = -1;
        if (protocol.getMappingData().getItemMappings().inverse().getNewId(rawId) == -1) {
            if (!isDamageable(item.identifier()) && item.identifier() != 358) { // Map
                if (tag == null) {
                    item.setTag(tag = new CompoundTag());
                }
                tag.putInt(extraNbtTag, originalId); // Data will be lost, saving original id
            }

            if (item.identifier() == 229) { // purple shulker box
                newId = 362; // directly set the new id -> base/colorless shulker box
            } else if (item.identifier() == 31 && item.data() == 0) { // Shrub was removed
                rawId = 32 << 4; // Dead Bush
            } else if (protocol.getMappingData().getItemMappings().inverse().getNewId(rawId & ~0xF) != -1) {
                rawId &= ~0xF; // Remove data
            } else {
                if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                    ViaBackwards.getPlatform().getLogger().warning("Failed to get 1.13 item for " + item.identifier());
                }
                rawId = 16; // Stone
            }
        }

        if (newId == -1) {
            newId = protocol.getMappingData().getItemMappings().inverse().getNewId(rawId);
        }

        item.setIdentifier(newId);
        item.setData((short) 0);
        return item;
    }

    private void rewriteCanPlaceToServer(CompoundTag tag, String tagName) {
        if (tag.getListTag(tagName) == null) return;

        ListTag<?> blockTag = tag.remove(extraNbtTag + "|" + tagName);
        if (blockTag != null) {
            tag.put(tagName, blockTag.copy());
        } else if ((blockTag = tag.getListTag(tagName)) != null) {
            ListTag<StringTag> newCanPlaceOn = new ListTag<>(StringTag.class);
            for (Tag oldTag : blockTag) {
                Object value = oldTag.getValue();
                String oldId = Key.stripMinecraftNamespace(value.toString());
                int key = Ints.tryParse(oldId);
                String numberConverted = BlockIdData.numberIdToString.get(key);
                if (numberConverted != null) {
                    oldId = numberConverted;
                }

                String lowerCaseId = oldId.toLowerCase(Locale.ROOT);
                String[] newValues = BlockIdData.blockIdMapping.get(lowerCaseId);
                if (newValues != null) {
                    for (String newValue : newValues) {
                        newCanPlaceOn.add(new StringTag(newValue));
                    }
                } else {
                    newCanPlaceOn.add(new StringTag(lowerCaseId));
                }
            }
            tag.put(tagName, newCanPlaceOn);
        }
    }

    private void rewriteEnchantmentsToServer(CompoundTag tag, boolean storedEnch) {
        String key = storedEnch ? "StoredEnchantments" : "Enchantments";
        ListTag<CompoundTag> enchantments = tag.getListTag(storedEnch ? key : "ench", CompoundTag.class);
        if (enchantments == null) return;

        ListTag<CompoundTag> newEnchantments = new ListTag<>(CompoundTag.class);
        boolean dummyEnchant = false;
        if (!storedEnch) {
            Tag hideFlags = tag.remove(extraNbtTag + "|OldHideFlags");
            if (hideFlags instanceof IntTag) {
                tag.putInt("HideFlags", ((NumberTag) hideFlags).asByte());
                dummyEnchant = true;
            } else if (tag.remove(extraNbtTag + "|DummyEnchant") != null) {
                tag.remove("HideFlags");
                dummyEnchant = true;
            }
        }

        for (CompoundTag entryTag : enchantments) {
            NumberTag idTag = entryTag.getNumberTag("id");
            NumberTag levelTag = entryTag.getNumberTag("lvl");
            CompoundTag enchantmentEntry = new CompoundTag();
            short oldId = idTag != null ? idTag.asShort() : 0;
            short level = levelTag != null ? levelTag.asShort() : 0;
            if (dummyEnchant && oldId == 0 && level == 0) {
                continue; // Skip dummy enchatment
            }

            String newId = Protocol1_13To1_12_2.MAPPINGS.getOldEnchantmentsIds().get(oldId);
            if (newId == null) {
                newId = "viaversion:legacy/" + oldId;
            }
            enchantmentEntry.putString("id", newId);

            enchantmentEntry.putShort("lvl", level);
            newEnchantments.add(enchantmentEntry);
        }

        ListTag<CompoundTag> noMapped = tag.getListTag(extraNbtTag + "|Enchantments", CompoundTag.class);
        if (noMapped != null) {
            for (CompoundTag value : noMapped) {
                newEnchantments.add(value);
            }
            tag.remove(extraNbtTag + "|Enchantments");
        }

        CompoundTag display = tag.getCompoundTag("display");
        if (display == null) {
            tag.put("display", display = new CompoundTag());
        }

        ListTag<StringTag> oldLore = tag.getListTag(extraNbtTag + "|OldLore", StringTag.class);
        if (oldLore != null) {
            ListTag<StringTag> lore = display.getListTag("Lore", StringTag.class);
            if (lore == null) {
                tag.put("Lore", lore = new ListTag<>(StringTag.class));
            }

            lore.setValue(oldLore.getValue());
            tag.remove(extraNbtTag + "|OldLore");
        } else if (tag.remove(extraNbtTag + "|DummyLore") != null) {
            display.remove("Lore");
            if (display.isEmpty()) {
                tag.remove("display");
            }
        }

        if (!storedEnch) {
            tag.remove("ench");
        }
        tag.put(key, newEnchantments);
    }

    private void invertShieldAndBannerId(Item item, CompoundTag tag) {
        if (item.identifier() != 442 && item.identifier() != 425) return;

        CompoundTag blockEntityTag = tag.getCompoundTag("BlockEntityTag");
        if (blockEntityTag == null) return;

        NumberTag base = blockEntityTag.getNumberTag("Base");
        if (base != null) {
            blockEntityTag.putInt("Base", 15 - base.asInt()); // Invert color id
        }

        ListTag<CompoundTag> patterns = blockEntityTag.getListTag("Patterns", CompoundTag.class);
        if (patterns != null) {
            for (CompoundTag pattern : patterns) {
                NumberTag colorTag = pattern.getNumberTag("Color");
                pattern.putInt("Color", 15 - colorTag.asInt()); // Invert color id
            }
        }
    }

    // TODO find a less hacky way to do this (https://bugs.mojang.com/browse/MC-74231)
    private static void flowerPotSpecialTreatment(UserConnection user, int blockState, Position position) throws Exception {
        if (FlowerPotHandler.isFlowah(blockState)) {
            BackwardsBlockEntityProvider beProvider = Via.getManager().getProviders().get(BackwardsBlockEntityProvider.class);

            CompoundTag nbt = beProvider.transform(user, position, "minecraft:flower_pot");

            // Remove the flowerpot
            PacketWrapper blockUpdateRemove = PacketWrapper.create(ClientboundPackets1_12_1.BLOCK_CHANGE, user);
            blockUpdateRemove.write(Type.POSITION1_8, position);
            blockUpdateRemove.write(Type.VAR_INT, 0);
            blockUpdateRemove.scheduleSend(Protocol1_12_2To1_13.class);

            // Create the flowerpot
            PacketWrapper blockCreate = PacketWrapper.create(ClientboundPackets1_12_1.BLOCK_CHANGE, user);
            blockCreate.write(Type.POSITION1_8, position);
            blockCreate.write(Type.VAR_INT, Protocol1_12_2To1_13.MAPPINGS.getNewBlockStateId(blockState));
            blockCreate.scheduleSend(Protocol1_12_2To1_13.class);

            // Send a block entity update
            PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_12_1.BLOCK_ENTITY_DATA, user);
            wrapper.write(Type.POSITION1_8, position);
            wrapper.write(Type.UNSIGNED_BYTE, (short) 5);
            wrapper.write(Type.NAMED_COMPOUND_TAG, nbt);
            wrapper.scheduleSend(Protocol1_12_2To1_13.class);

        }
    }
}
