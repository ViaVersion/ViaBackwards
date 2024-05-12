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

package com.viaversion.viabackwards.protocol.v1_13to1_12_2.rewriter;

import com.google.common.primitives.Ints;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.rewriters.BackwardsItemRewriter;
import com.viaversion.viabackwards.api.rewriters.EnchantmentRewriter;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.block_entity_handlers.FlowerPotHandler;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.provider.BackwardsBlockEntityProvider;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.storage.BackwardsBlockStorage;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.storage.NoteBlockStorage;
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
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_13;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.nbt.tag.ByteTag;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.IntTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.nbt.tag.Tag;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ServerboundPackets1_12_1;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.data.BlockIdData;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.data.SpawnEggRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.IdAndData;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.Pair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class BlockItemPacketRewriter1_13 extends BackwardsItemRewriter<ClientboundPackets1_13, ServerboundPackets1_12_1, Protocol1_13To1_12_2> {

    private final Map<String, String> enchantmentMappings = new HashMap<>();
    private final String extraNbtTag;

    public BlockItemPacketRewriter1_13(Protocol1_13To1_12_2 protocol) {
        super(protocol, null, null);
        extraNbtTag = nbtTagName("2");
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
            int itemId = wrapper.read(Types.VAR_INT);
            int oldId = protocol.getMappingData().getItemMappings().getNewId(itemId);
            if (oldId == -1) {
                wrapper.cancel();
                return;
            }

            if (SpawnEggRewriter.getEntityId(oldId).isPresent()) {
                wrapper.write(Types.VAR_INT, IdAndData.toRawData(383));
                return;
            }

            wrapper.write(Types.VAR_INT, IdAndData.getId(oldId));
        });

        protocol.registerClientbound(ClientboundPackets1_13.BLOCK_EVENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_8); // Location
                map(Types.UNSIGNED_BYTE); // Action Id
                map(Types.UNSIGNED_BYTE); // Action param
                map(Types.VAR_INT); // Block Id - /!\ NOT BLOCK STATE ID
                handler(wrapper -> {
                    int blockId = wrapper.get(Types.VAR_INT, 0);

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

                        final Position position = wrapper.get(Types.BLOCK_POSITION1_8, 0);
                        final Pair<Integer, Integer> update = noteBlockStorage.getNoteBlockUpdate(position);
                        if (update != null) { // Use values from block state update
                            wrapper.set(Types.UNSIGNED_BYTE, 0, update.key().shortValue());
                            wrapper.set(Types.UNSIGNED_BYTE, 1, update.value().shortValue());
                        }
                    }

                    wrapper.set(Types.VAR_INT, 0, blockId);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_8); // 0 - Position
                map(Types.UNSIGNED_BYTE); // 1 - Action
                map(Types.NAMED_COMPOUND_TAG); // 2 - NBT Data

                handler(wrapper -> {
                    BackwardsBlockEntityProvider provider = Via.getManager().getProviders().get(BackwardsBlockEntityProvider.class);

                    // TODO conduit handling
                    if (wrapper.get(Types.UNSIGNED_BYTE, 0) == 5) {
                        wrapper.cancel();
                    }

                    wrapper.set(Types.NAMED_COMPOUND_TAG, 0,
                        provider.transform(
                            wrapper.user(),
                            wrapper.get(Types.BLOCK_POSITION1_8, 0),
                            wrapper.get(Types.NAMED_COMPOUND_TAG, 0)
                        ));
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.FORGET_LEVEL_CHUNK, wrapper -> {
            int chunkMinX = wrapper.passthrough(Types.INT) << 4;
            int chunkMinZ = wrapper.passthrough(Types.INT) << 4;
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
        protocol.registerClientbound(ClientboundPackets1_13.BLOCK_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_8); // 0 - Position

                handler(wrapper -> {
                    int blockState = wrapper.read(Types.VAR_INT);
                    Position position = wrapper.get(Types.BLOCK_POSITION1_8, 0);

                    // Note block special treatment
                    if (blockState >= 249 && blockState <= 748) { // Note block states id range
                        wrapper.user().get(NoteBlockStorage.class).storeNoteBlockUpdate(position, blockState);
                    }

                    // Store blocks
                    BackwardsBlockStorage storage = wrapper.user().get(BackwardsBlockStorage.class);
                    storage.checkAndStore(position, blockState);

                    wrapper.write(Types.VAR_INT, protocol.getMappingData().getNewBlockStateId(blockState));

                    // Flower pot special treatment
                    flowerPotSpecialTreatment(wrapper.user(), blockState, position);
                });
            }
        });

        // Multi Block Change
        protocol.registerClientbound(ClientboundPackets1_13.CHUNK_BLOCKS_UPDATE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // 0 - Chunk X
                map(Types.INT); // 1 - Chunk Z
                map(Types.BLOCK_CHANGE_ARRAY);
                handler(wrapper -> {
                    BackwardsBlockStorage storage = wrapper.user().get(BackwardsBlockStorage.class);

                    for (BlockChangeRecord record : wrapper.get(Types.BLOCK_CHANGE_ARRAY, 0)) {
                        int chunkX = wrapper.get(Types.INT, 0);
                        int chunkZ = wrapper.get(Types.INT, 1);
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

        protocol.registerClientbound(ClientboundPackets1_13.CONTAINER_SET_CONTENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE);
                map(Types.ITEM1_13_SHORT_ARRAY, Types.ITEM1_8_SHORT_ARRAY);

                handler(wrapper -> {
                    final Item[] items = wrapper.get(Types.ITEM1_8_SHORT_ARRAY, 0);
                    for (Item item : items) {
                        handleItemToClient(wrapper.user(), item);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.CONTAINER_SET_SLOT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE);
                map(Types.SHORT);
                map(Types.ITEM1_13, Types.ITEM1_8);

                handler(wrapper -> handleItemToClient(wrapper.user(), wrapper.get(Types.ITEM1_8, 0)));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.LEVEL_CHUNK, wrapper -> {
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
                    int newId = switch (biome) {
                        case 40, 41, 42, 43 -> 9; // end biomes
                        case 47, 48, 49 -> 24; // deep ocean biomes
                        case 50 -> 10; // deep frozen... let's just pick the frozen variant
                        case 44, 45, 46 -> 0; // the other new ocean biomes
                        default -> -1;
                    };

                    if (newId != -1) {
                        chunk.getBiomeData()[i] = newId;
                    }
                }
            }

            wrapper.write(type_old, chunk);
        });

        protocol.registerClientbound(ClientboundPackets1_13.LEVEL_EVENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Effect Id
                map(Types.BLOCK_POSITION1_8); // Location
                map(Types.INT); // Data
                handler(wrapper -> {
                    int id = wrapper.get(Types.INT, 0);
                    int data = wrapper.get(Types.INT, 1);
                    if (id == 1010) { // Play record
                        wrapper.set(Types.INT, 1, protocol.getMappingData().getItemMappings().getNewId(data) >> 4);
                    } else if (id == 2001) { // Block break + block break sound
                        data = protocol.getMappingData().getNewBlockStateId(data);
                        int blockId = data >> 4;
                        int blockData = data & 0xF;
                        wrapper.set(Types.INT, 1, (blockId & 0xFFF) | (blockData << 12));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.MAP_ITEM_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.BYTE);
                map(Types.BOOLEAN);
                handler(wrapper -> {
                    int iconCount = wrapper.passthrough(Types.VAR_INT);
                    for (int i = 0; i < iconCount; i++) {
                        int type = wrapper.read(Types.VAR_INT);
                        byte x = wrapper.read(Types.BYTE);
                        byte z = wrapper.read(Types.BYTE);
                        byte direction = wrapper.read(Types.BYTE);
                        wrapper.read(Types.OPTIONAL_COMPONENT);
                        if (type > 9) {
                            wrapper.set(Types.VAR_INT, 1, wrapper.get(Types.VAR_INT, 1) - 1);
                            continue;
                        }
                        wrapper.write(Types.BYTE, (byte) ((type << 4) | (direction & 0x0F)));
                        wrapper.write(Types.BYTE, x);
                        wrapper.write(Types.BYTE, z);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.SET_EQUIPPED_ITEM, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.VAR_INT);
                map(Types.ITEM1_13, Types.ITEM1_8);

                handler(wrapper -> handleItemToClient(wrapper.user(), wrapper.get(Types.ITEM1_8, 0)));
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.CONTAINER_SET_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE); // Window Id
                map(Types.SHORT); // Property
                map(Types.SHORT); // Value
                handler(wrapper -> {
                    short property = wrapper.get(Types.SHORT, 0);
                    // Enchantment table
                    if (property >= 4 && property <= 6) {
                        short oldId = wrapper.get(Types.SHORT, 1);
                        wrapper.set(Types.SHORT, 1, (short) protocol.getMappingData().getEnchantmentMappings().getNewId(oldId));
                    }
                });
            }
        });


        protocol.registerServerbound(ServerboundPackets1_12_1.SET_CREATIVE_MODE_SLOT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.SHORT);
                map(Types.ITEM1_8, Types.ITEM1_13);

                handler(wrapper -> handleItemToServer(wrapper.user(), wrapper.get(Types.ITEM1_13, 0)));
            }
        });

        protocol.registerServerbound(ServerboundPackets1_12_1.CONTAINER_CLICK, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE);
                map(Types.SHORT);
                map(Types.BYTE);
                map(Types.SHORT);
                map(Types.VAR_INT);
                map(Types.ITEM1_8, Types.ITEM1_13);

                handler(wrapper -> handleItemToServer(wrapper.user(), wrapper.get(Types.ITEM1_13, 0)));
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
    public Item handleItemToClient(UserConnection connection, Item item) {
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
            super.handleItemToClient(connection, item);

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
                    name.setValue(protocol.jsonToLegacy(connection, name.getValue()));
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
                Short oldId = Protocol1_12_2To1_13.MAPPINGS.getOldEnchantmentsIds().inverse().get(Key.stripMinecraftNamespace(newId));
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
                tag.put(extraNbtTag + "|DummyEnchant", new ByteTag(false));
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
                    tag.put(extraNbtTag + "|DummyLore", new ByteTag(false));
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
    public Item handleItemToServer(UserConnection connection, Item item) {
        if (item == null) return null;
        CompoundTag tag = item.tag();

        // Save original id
        int originalId = (item.identifier() << 16 | item.data() & 0xFFFF);

        int rawId = IdAndData.toRawData(item.identifier(), item.data());

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
        super.handleItemToServer(connection, item);

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
                rawId = IdAndData.toRawData(32); // Dead Bush
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

        ListTag<?> blockTag = tag.getListTag(extraNbtTag + "|" + tagName);
        if (blockTag != null) {
            tag.remove(extraNbtTag + "|" + tagName);
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

            String newId = Protocol1_12_2To1_13.MAPPINGS.getOldEnchantmentsIds().get(oldId);
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
    private static void flowerPotSpecialTreatment(UserConnection user, int blockState, Position position) {
        if (FlowerPotHandler.isFlowah(blockState)) {
            BackwardsBlockEntityProvider beProvider = Via.getManager().getProviders().get(BackwardsBlockEntityProvider.class);

            CompoundTag nbt = beProvider.transform(user, position, "minecraft:flower_pot");

            // Remove the flowerpot
            PacketWrapper blockUpdateRemove = PacketWrapper.create(ClientboundPackets1_12_1.BLOCK_UPDATE, user);
            blockUpdateRemove.write(Types.BLOCK_POSITION1_8, position);
            blockUpdateRemove.write(Types.VAR_INT, 0);
            blockUpdateRemove.scheduleSend(Protocol1_13To1_12_2.class);

            // Create the flowerpot
            PacketWrapper blockCreate = PacketWrapper.create(ClientboundPackets1_12_1.BLOCK_UPDATE, user);
            blockCreate.write(Types.BLOCK_POSITION1_8, position);
            blockCreate.write(Types.VAR_INT, Protocol1_13To1_12_2.MAPPINGS.getNewBlockStateId(blockState));
            blockCreate.scheduleSend(Protocol1_13To1_12_2.class);

            // Send a block entity update
            PacketWrapper wrapper = PacketWrapper.create(ClientboundPackets1_12_1.BLOCK_ENTITY_DATA, user);
            wrapper.write(Types.BLOCK_POSITION1_8, position);
            wrapper.write(Types.UNSIGNED_BYTE, (short) 5);
            wrapper.write(Types.NAMED_COMPOUND_TAG, nbt);
            wrapper.scheduleSend(Protocol1_13To1_12_2.class);

        }
    }
}
