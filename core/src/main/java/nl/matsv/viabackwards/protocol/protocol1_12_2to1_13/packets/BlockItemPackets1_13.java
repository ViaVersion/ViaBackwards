/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import com.google.common.primitives.Ints;
import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.EnchantmentRewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.block_entity_handlers.FlowerPotHandler;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.BlockChangeRecord;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_12_1to1_12.ServerboundPackets1_12_1;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.BlockIdData;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.SpawnEggRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.types.Chunk1_13Type;
import us.myles.ViaVersion.protocols.protocol1_9_1_2to1_9_3_4.types.Chunk1_9_3_4Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.viaversion.libs.opennbt.conversion.ConverterRegistry;
import us.myles.viaversion.libs.opennbt.tag.builtin.ByteTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.IntTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ShortTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class BlockItemPackets1_13 extends nl.matsv.viabackwards.api.rewriters.ItemRewriter<Protocol1_12_2To1_13> {

    private final Map<String, String> enchantmentMappings = new HashMap<>();
    private final String extraNbtTag;

    public BlockItemPackets1_13(Protocol1_12_2To1_13 protocol) {
        super(protocol, null, id -> BackwardsMappings.itemMappings.getMappedItem(id));
        extraNbtTag = "VB|" + protocol.getClass().getSimpleName() + "|2";
    }

    public static int toOldId(int oldId) {
        if (oldId < 0) {
            oldId = 0; // Some plugins use negative numbers to clear blocks, remap them to air.
        }
        int newId = BackwardsMappings.blockMappings.getNewId(oldId);
        if (newId != -1)
            return newId;

        ViaBackwards.getPlatform().getLogger().warning("Missing block completely " + oldId);
        // Default stone
        return 1 << 4;
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
        protocol.registerOutgoing(ClientboundPackets1_13.COOLDOWN, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int itemId = wrapper.read(Type.VAR_INT);
                        int oldId = MappingData.oldToNewItems.inverse().get(itemId);
                        if (oldId != -1) {
                            Optional<String> eggEntityId = SpawnEggRewriter.getEntityId(oldId);
                            if (eggEntityId.isPresent()) {
                                itemId = 383 << 16;
                            } else {
                                itemId = (oldId >> 4) << 16 | oldId & 0xF;
                            }
                        }
                        wrapper.write(Type.VAR_INT, itemId);
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.BLOCK_ACTION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // Location
                map(Type.UNSIGNED_BYTE); // Action Id
                map(Type.UNSIGNED_BYTE); // Action param
                map(Type.VAR_INT); // Block Id - /!\ NOT BLOCK STATE ID
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
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

                        wrapper.set(Type.VAR_INT, 0, blockId);
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.BLOCK_ENTITY_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // 0 - Position
                map(Type.UNSIGNED_BYTE); // 1 - Action
                map(Type.NBT); // 2 - NBT Data

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        BackwardsBlockEntityProvider provider = Via.getManager().getProviders().get(BackwardsBlockEntityProvider.class);

                        // TODO conduit handling
                        if (wrapper.get(Type.UNSIGNED_BYTE, 0) == 5) {
                            wrapper.cancel();
                        }

                        wrapper.set(Type.NBT, 0,
                                provider.transform(
                                        wrapper.user(),
                                        wrapper.get(Type.POSITION, 0),
                                        wrapper.get(Type.NBT, 0)
                                ));
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.UNLOAD_CHUNK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int chunkMinX = wrapper.passthrough(Type.INT) << 4;
                        int chunkMinZ = wrapper.passthrough(Type.INT) << 4;
                        int chunkMaxX = chunkMinX + 15;
                        int chunkMaxZ = chunkMinZ + 15;
                        BackwardsBlockStorage blockStorage = wrapper.user().get(BackwardsBlockStorage.class);
                        blockStorage.getBlocks().entrySet().removeIf(entry -> {
                            Position position = entry.getKey();
                            return position.getX() >= chunkMinX && position.getZ() >= chunkMinZ
                                    && position.getX() <= chunkMaxX && position.getZ() <= chunkMaxZ;
                        });
                    }
                });
            }
        });

        // Block Change
        protocol.registerOutgoing(ClientboundPackets1_13.BLOCK_CHANGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // 0 - Position

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int blockState = wrapper.read(Type.VAR_INT);
                        Position position = wrapper.get(Type.POSITION, 0);

                        // Store blocks
                        BackwardsBlockStorage storage = wrapper.user().get(BackwardsBlockStorage.class);
                        storage.checkAndStore(position, blockState);

                        wrapper.write(Type.VAR_INT, toOldId(blockState));

                        // Flower pot special treatment
                        flowerPotSpecialTreatment(wrapper.user(), blockState, position);
                    }
                });
            }
        });

        // Multi Block Change
        protocol.registerOutgoing(ClientboundPackets1_13.MULTI_BLOCK_CHANGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Chunk X
                map(Type.INT); // 1 - Chunk Z
                map(Type.BLOCK_CHANGE_RECORD_ARRAY);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
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
                            record.setBlockId(toOldId(block));
                        }
                    }
                });
            }
        });

        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);

        protocol.registerOutgoing(ClientboundPackets1_13.WINDOW_ITEMS, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE);
                map(Type.FLAT_ITEM_ARRAY, Type.ITEM_ARRAY);

                handler(itemRewriter.itemArrayHandler(Type.ITEM_ARRAY));
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.SET_SLOT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE);
                map(Type.SHORT);
                map(Type.FLAT_ITEM, Type.ITEM);

                handler(itemRewriter.itemToClientHandler(Type.ITEM));
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

                    Chunk1_9_3_4Type type_old = new Chunk1_9_3_4Type(clientWorld);
                    Chunk1_13Type type = new Chunk1_13Type(clientWorld);
                    Chunk chunk = wrapper.read(type);

                    // Handle Block Entities before block rewrite
                    BackwardsBlockEntityProvider provider = Via.getManager().getProviders().get(BackwardsBlockEntityProvider.class);
                    BackwardsBlockStorage storage = wrapper.user().get(BackwardsBlockStorage.class);
                    for (CompoundTag tag : chunk.getBlockEntities()) {
                        Tag idTag = tag.get("id");
                        if (idTag == null) continue;

                        String id = (String) idTag.getValue();

                        // Ignore if we don't handle it
                        if (!provider.isHandled(id)) continue;

                        int sectionIndex = ((int) tag.get("y").getValue()) >> 4;
                        ChunkSection section = chunk.getSections()[sectionIndex];

                        int x = (int) tag.get("x").getValue();
                        int y = (int) tag.get("y").getValue();
                        int z = (int) tag.get("z").getValue();
                        Position position = new Position(x, (short) y, z);

                        int block = section.getFlatBlock(x & 0xF, y & 0xF, z & 0xF);
                        storage.checkAndStore(position, block);

                        provider.transform(wrapper.user(), position, tag);
                    }

                    // Rewrite new blocks to old blocks
                    for (int i = 0; i < chunk.getSections().length; i++) {
                        ChunkSection section = chunk.getSections()[i];
                        if (section == null) {
                            continue;
                        }

                        // Flower pots require a special treatment, they are no longer block entities :(
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                for (int x = 0; x < 16; x++) {
                                    int block = section.getFlatBlock(x, y, z);

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

                        for (int p = 0; p < section.getPaletteSize(); p++) {
                            int old = section.getPaletteEntry(p);
                            if (old != 0) {
                                int oldId = toOldId(old);
                                section.setPaletteEntry(p, oldId);
                            }
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
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.EFFECT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Effect Id
                map(Type.POSITION); // Location
                map(Type.INT); // Data
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int id = wrapper.get(Type.INT, 0);
                        int data = wrapper.get(Type.INT, 1);
                        if (id == 1010) { // Play record
                            wrapper.set(Type.INT, 1, MappingData.oldToNewItems.inverse().get(data) >> 4);
                        } else if (id == 2001) { // Block break + block break sound
                            data = toOldId(data);
                            int blockId = data >> 4;
                            int blockData = data & 0xF;
                            wrapper.set(Type.INT, 1, (blockId & 0xFFF) | (blockData << 12));
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.MAP_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.BYTE);
                map(Type.BOOLEAN);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int iconCount = wrapper.passthrough(Type.VAR_INT);
                        for (int i = 0; i < iconCount; i++) {
                            int type = wrapper.read(Type.VAR_INT);
                            byte x = wrapper.read(Type.BYTE);
                            byte z = wrapper.read(Type.BYTE);
                            byte direction = wrapper.read(Type.BYTE);
                            if (wrapper.read(Type.BOOLEAN)) {
                                wrapper.read(Type.COMPONENT);
                            }
                            if (type > 9) {
                                wrapper.set(Type.VAR_INT, 1, wrapper.get(Type.VAR_INT, 1) - 1);
                                continue;
                            }
                            wrapper.write(Type.BYTE, (byte) ((type << 4) | (direction & 0x0F)));
                            wrapper.write(Type.BYTE, x);
                            wrapper.write(Type.BYTE, z);
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.ENTITY_EQUIPMENT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.VAR_INT);
                map(Type.FLAT_ITEM, Type.ITEM);

                handler(itemRewriter.itemToClientHandler(Type.ITEM));
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.WINDOW_PROPERTY, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // Window Id
                map(Type.SHORT); // Property
                map(Type.SHORT); // Value
                handler(wrapper -> {
                    short property = wrapper.get(Type.SHORT, 0);
                    // Enchantment table
                    if (property >= 4 && property <= 6) {
                        short oldId = wrapper.get(Type.SHORT, 1);
                        wrapper.set(Type.SHORT, 1, (short) BackwardsMappings.enchantmentMappings.getNewId(oldId));
                    }
                });
            }
        });


        protocol.registerIncoming(ServerboundPackets1_12_1.CREATIVE_INVENTORY_ACTION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.SHORT);
                map(Type.ITEM, Type.FLAT_ITEM);

                handler(itemRewriter.itemToServerHandler(Type.FLAT_ITEM));
            }
        });

        protocol.registerIncoming(ServerboundPackets1_12_1.CLICK_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE);
                map(Type.SHORT);
                map(Type.BYTE);
                map(Type.SHORT);
                map(Type.VAR_INT);
                map(Type.ITEM, Type.FLAT_ITEM);

                handler(itemRewriter.itemToServerHandler(Type.FLAT_ITEM));
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
        int originalId = item.getIdentifier();

        Integer rawId = null;
        boolean gotRawIdFromTag = false;

        CompoundTag tag = item.getTag();

        // Use tag to get original ID and data
        Tag originalIdTag;
        if (tag != null && (originalIdTag = tag.remove(extraNbtTag)) != null) {
            rawId = (Integer) originalIdTag.getValue();
            gotRawIdFromTag = true;
        }

        if (rawId == null) {
            // Look for custom mappings
            super.handleItemToClient(item);

            // No custom mapping found, look at VV mappings
            if (item.getIdentifier() == originalId) {
                int oldId = MappingData.oldToNewItems.inverse().get(item.getIdentifier());
                if (oldId != -1) {
                    rawId = itemIdToRaw(oldId, item, tag);
                } else if (item.getIdentifier() == 362) { // base/colorless shulker box
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
                    tag = item.getTag();
                }

                rawId = itemIdToRaw(item.getIdentifier(), item, tag);
            }
        }

        item.setIdentifier(rawId >> 16);
        item.setData((short) (rawId & 0xFFFF));

        // NBT changes
        if (tag != null) {
            if (isDamageable(item.getIdentifier())) {
                Tag damageTag = tag.remove("Damage");
                if (!gotRawIdFromTag && damageTag instanceof IntTag) {
                    item.setData((short) (int) damageTag.getValue());
                }
            }

            if (item.getIdentifier() == 358) { // map
                Tag mapTag = tag.remove("map");
                if (!gotRawIdFromTag && mapTag instanceof IntTag) {
                    item.setData((short) (int) mapTag.getValue());
                }
            }

            // Shield and banner
            invertShieldAndBannerId(item, tag);

            // Display Name now uses JSON
            CompoundTag display = tag.get("display");
            if (display != null) {
                StringTag name = display.get("Name");
                if (name instanceof StringTag) {
                    display.put(new StringTag(extraNbtTag + "|Name", name.getValue()));
                    name.setValue(ChatRewriter.jsonTextToLegacy(name.getValue()));
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
                item.setTag(tag = new CompoundTag("tag"));
            }
            if (!tag.contains("EntityTag")) {
                CompoundTag entityTag = new CompoundTag("EntityTag");
                entityTag.put(new StringTag("id", eggEntityId.get()));
                tag.put(entityTag);
            }
            return 0x17f0000; // 383 << 16;
        }

        return (oldId >> 4) << 16 | oldId & 0xF;
    }

    private void rewriteCanPlaceToClient(CompoundTag tag, String tagName) {
        ListTag blockTag = tag.get(tagName);
        if (blockTag == null) return;

        ListTag newCanPlaceOn = new ListTag(tagName, StringTag.class);
        tag.put(ConverterRegistry.convertToTag(extraNbtTag + "|" + tagName, ConverterRegistry.convertToValue(blockTag)));
        for (Tag oldTag : blockTag) {
            Object value = oldTag.getValue();
            String[] newValues = value instanceof String ?
                    BlockIdData.fallbackReverseMapping.get(((String) value).replace("minecraft:", "")) : null;
            if (newValues != null) {
                for (String newValue : newValues) {
                    newCanPlaceOn.add(new StringTag("", newValue));
                }
            } else {
                newCanPlaceOn.add(oldTag);
            }
        }
        tag.put(newCanPlaceOn);
    }

    private void rewriteEnchantmentsToClient(CompoundTag tag, boolean storedEnch) {
        String key = storedEnch ? "StoredEnchantments" : "Enchantments";
        ListTag enchantments = tag.get(key);
        if (enchantments == null) return;

        ListTag noMapped = new ListTag(extraNbtTag + "|" + key, CompoundTag.class);
        ListTag newEnchantments = new ListTag(storedEnch ? key : "ench", CompoundTag.class);
        List<Tag> lore = new ArrayList<>();
        boolean hasValidEnchants = false;
        for (Tag enchantmentEntryTag : enchantments.clone()) {
            CompoundTag enchantmentEntry = (CompoundTag) enchantmentEntryTag;
            String newId = (String) enchantmentEntry.get("id").getValue();
            Number levelValue = (Number) enchantmentEntry.get("lvl").getValue();
            int intValue = levelValue.intValue();
            short level = intValue < Short.MAX_VALUE ? levelValue.shortValue() : Short.MAX_VALUE;

            String mappedEnchantmentId = enchantmentMappings.get(newId);
            if (mappedEnchantmentId != null) {
                lore.add(new StringTag("", mappedEnchantmentId + " " + EnchantmentRewriter.getRomanNumber(level)));
                noMapped.add(enchantmentEntry);
            } else if (!newId.isEmpty()) {
                Short oldId = MappingData.oldEnchantmentsIds.inverse().get(newId);
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

                            lore.add(new StringTag("", name + " " + EnchantmentRewriter.getRomanNumber(level)));
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

                CompoundTag newEntry = new CompoundTag("");
                newEntry.put(new ShortTag("id", oldId));
                newEntry.put(new ShortTag("lvl", level));
                newEnchantments.add(newEntry);
            }
        }

        // Put here to hide empty enchantment from 1.14 rewrites
        if (!storedEnch && !hasValidEnchants) {
            IntTag hideFlags = tag.get("HideFlags");
            if (hideFlags == null) {
                hideFlags = new IntTag("HideFlags");
                tag.put(new ByteTag(extraNbtTag + "|DummyEnchant"));
            } else {
                tag.put(new IntTag(extraNbtTag + "|OldHideFlags", hideFlags.getValue()));
            }

            if (newEnchantments.size() == 0) {
                CompoundTag enchEntry = new CompoundTag("");
                enchEntry.put(new ShortTag("id", (short) 0));
                enchEntry.put(new ShortTag("lvl", (short) 0));
                newEnchantments.add(enchEntry);
            }

            int value = hideFlags.getValue() | 1;
            hideFlags.setValue(value);
            tag.put(hideFlags);
        }

        if (noMapped.size() != 0) {
            tag.put(noMapped);

            if (!lore.isEmpty()) {
                CompoundTag display = tag.get("display");
                if (display == null) {
                    tag.put(display = new CompoundTag("display"));
                }

                ListTag loreTag = display.get("Lore");
                if (loreTag == null) {
                    display.put(loreTag = new ListTag("Lore", StringTag.class));
                    tag.put(new ByteTag(extraNbtTag + "|DummyLore"));
                } else if (loreTag.size() != 0) {
                    ListTag oldLore = new ListTag(extraNbtTag + "|OldLore", StringTag.class);
                    for (Tag value : loreTag) {
                        oldLore.add(value.clone());
                    }
                    tag.put(oldLore);

                    lore.addAll(loreTag.getValue());
                }

                loreTag.setValue(lore);
            }
        }

        tag.remove("Enchantments");
        tag.put(newEnchantments);
    }

    @Override
    public Item handleItemToServer(Item item) {
        if (item == null) return null;
        CompoundTag tag = item.getTag();

        // Save original id
        int originalId = (item.getIdentifier() << 16 | item.getData() & 0xFFFF);

        int rawId = (item.getIdentifier() << 4 | item.getData() & 0xF);

        // NBT Additions
        if (isDamageable(item.getIdentifier())) {
            if (tag == null) item.setTag(tag = new CompoundTag("tag"));
            tag.put(new IntTag("Damage", item.getData()));
        }
        if (item.getIdentifier() == 358) { // map
            if (tag == null) item.setTag(tag = new CompoundTag("tag"));
            tag.put(new IntTag("map", item.getData()));
        }

        // NBT Changes
        if (tag != null) {
            // Shield and banner
            invertShieldAndBannerId(item, tag);

            // Display Name now uses JSON
            Tag display = tag.get("display");
            if (display instanceof CompoundTag) {
                CompoundTag displayTag = (CompoundTag) display;
                StringTag name = displayTag.get("Name");
                if (name instanceof StringTag) {
                    StringTag via = displayTag.remove(extraNbtTag + "|Name");
                    name.setValue(via != null ? via.getValue() : ChatRewriter.legacyTextToJson(name.getValue()).toString());
                }
            }

            // ench is now Enchantments and now uses identifiers
            rewriteEnchantmentsToServer(tag, false);
            rewriteEnchantmentsToServer(tag, true);

            rewriteCanPlaceToServer(tag, "CanPlaceOn");
            rewriteCanPlaceToServer(tag, "CanDestroy");

            // Handle SpawnEggs
            if (item.getIdentifier() == 383) {
                CompoundTag entityTag = tag.get("EntityTag");
                StringTag identifier;
                if (entityTag != null && (identifier = entityTag.get("id")) != null) {
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
        int identifier = item.getIdentifier();
        item.setIdentifier(rawId);
        super.handleItemToServer(item);
        // Mapped with original data, we can return here
        if (item.getIdentifier() != rawId) return item;

        // Set to legacy id again
        item.setIdentifier(identifier);

        int newId = -1;
        if (!MappingData.oldToNewItems.containsKey(rawId)) {
            if (!isDamageable(item.getIdentifier()) && item.getIdentifier() != 358) { // Map
                if (tag == null) item.setTag(tag = new CompoundTag("tag"));
                tag.put(new IntTag(extraNbtTag, originalId)); // Data will be lost, saving original id
            }

            if (item.getIdentifier() == 229) { // purple shulker box
                newId = 362; // directly set the new id -> base/colorless shulker box
            } else if (item.getIdentifier() == 31 && item.getData() == 0) { // Shrub was removed
                rawId = 32 << 4; // Dead Bush
            } else if (MappingData.oldToNewItems.containsKey(rawId & ~0xF)) {
                rawId &= ~0xF; // Remove data
            } else {
                if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                    ViaBackwards.getPlatform().getLogger().warning("Failed to get 1.13 item for " + item.getIdentifier());
                }
                rawId = 16; // Stone
            }
        }

        if (newId == -1) {
            newId = MappingData.oldToNewItems.get(rawId);
        }

        item.setIdentifier(newId);
        item.setData((short) 0);
        return item;
    }

    private void rewriteCanPlaceToServer(CompoundTag tag, String tagName) {
        ListTag blockTag = tag.remove(extraNbtTag + "|" + tagName);
        if (blockTag != null) {
            tag.put(ConverterRegistry.convertToTag(tagName, ConverterRegistry.convertToValue(blockTag)));
        } else if ((blockTag = tag.get(tagName)) != null) {
            ListTag newCanPlaceOn = new ListTag(tagName, StringTag.class);
            for (Tag oldTag : blockTag) {
                Object value = oldTag.getValue();
                String oldId = value.toString().replace("minecraft:", "");
                String numberConverted = BlockIdData.numberIdToString.get(Ints.tryParse(oldId));
                if (numberConverted != null) {
                    oldId = numberConverted;
                }

                String lowerCaseId = oldId.toLowerCase(Locale.ROOT);
                String[] newValues = BlockIdData.blockIdMapping.get(lowerCaseId);
                if (newValues != null) {
                    for (String newValue : newValues) {
                        newCanPlaceOn.add(new StringTag("", newValue));
                    }
                } else {
                    newCanPlaceOn.add(new StringTag("", lowerCaseId));
                }
            }
            tag.put(newCanPlaceOn);
        }
    }

    private void rewriteEnchantmentsToServer(CompoundTag tag, boolean storedEnch) {
        String key = storedEnch ? "StoredEnchantments" : "Enchantments";
        ListTag enchantments = tag.get(storedEnch ? key : "ench");
        if (enchantments == null) return;

        ListTag newEnchantments = new ListTag(key, CompoundTag.class);
        boolean dummyEnchant = false;
        if (!storedEnch) {
            IntTag hideFlags = tag.remove(extraNbtTag + "|OldHideFlags");
            if (hideFlags != null) {
                tag.put(new IntTag("HideFlags", hideFlags.getValue()));
                dummyEnchant = true;
            } else if (tag.remove(extraNbtTag + "|DummyEnchant") != null) {
                tag.remove("HideFlags");
                dummyEnchant = true;
            }
        }

        for (Tag enchEntry : enchantments) {
            CompoundTag enchantmentEntry = new CompoundTag("");
            short oldId = ((Number) ((CompoundTag) enchEntry).get("id").getValue()).shortValue();
            short level = ((Number) ((CompoundTag) enchEntry).get("lvl").getValue()).shortValue();
            if (dummyEnchant && oldId == 0 && level == 0) {
                continue; //Skip dummy enchatment
            }
            String newId = MappingData.oldEnchantmentsIds.get(oldId);
            if (newId == null) {
                newId = "viaversion:legacy/" + oldId;
            }
            enchantmentEntry.put(new StringTag("id", newId));

            enchantmentEntry.put(new ShortTag("lvl", level));
            newEnchantments.add(enchantmentEntry);
        }

        ListTag noMapped = tag.remove(extraNbtTag + "|Enchantments");
        if (noMapped != null) {
            for (Tag value : noMapped) {
                newEnchantments.add(value);
            }
        }

        CompoundTag display = tag.get("display");
        if (display == null) {
            tag.put(display = new CompoundTag("display"));
        }

        ListTag oldLore = tag.remove(extraNbtTag + "|OldLore");
        if (oldLore != null) {
            ListTag lore = display.get("Lore");
            if (lore == null) {
                tag.put(lore = new ListTag("Lore"));
            }

            lore.setValue(oldLore.getValue());
        } else if (tag.remove(extraNbtTag + "|DummyLore") != null) {
            display.remove("Lore");
            if (display.isEmpty()) {
                tag.remove("display");
            }
        }

        if (!storedEnch) {
            tag.remove("ench");
        }
        tag.put(newEnchantments);
    }

    private void invertShieldAndBannerId(Item item, CompoundTag tag) {
        if (item.getIdentifier() != 442 && item.getIdentifier() != 425) return;

        Tag blockEntityTag = tag.get("BlockEntityTag");
        if (!(blockEntityTag instanceof CompoundTag)) return;

        CompoundTag blockEntityCompoundTag = (CompoundTag) blockEntityTag;
        Tag base = blockEntityCompoundTag.get("Base");
        if (base instanceof IntTag) {
            IntTag baseTag = (IntTag) base;
            baseTag.setValue(15 - baseTag.getValue()); // invert color id
        }

        Tag patterns = blockEntityCompoundTag.get("Patterns");
        if (patterns instanceof ListTag) {
            ListTag patternsTag = (ListTag) patterns;
            for (Tag pattern : patternsTag) {
                if (!(pattern instanceof CompoundTag)) continue;

                IntTag colorTag = ((CompoundTag) pattern).get("Color");
                colorTag.setValue(15 - colorTag.getValue()); // Invert color id
            }
        }
    }

    // TODO find a less hacky way to do this (https://bugs.mojang.com/browse/MC-74231)
    private static void flowerPotSpecialTreatment(UserConnection user, int blockState, Position position) throws Exception {
        if (FlowerPotHandler.isFlowah(blockState)) {
            BackwardsBlockEntityProvider beProvider = Via.getManager().getProviders().get(BackwardsBlockEntityProvider.class);

            CompoundTag nbt = beProvider.transform(user, position, "minecraft:flower_pot");

            // Remove the flowerpot
            PacketWrapper blockUpdateRemove = new PacketWrapper(0x0B, null, user);
            blockUpdateRemove.write(Type.POSITION, position);
            blockUpdateRemove.write(Type.VAR_INT, 0);
            blockUpdateRemove.send(Protocol1_12_2To1_13.class, true);

            // Create the flowerpot
            PacketWrapper blockCreate = new PacketWrapper(0x0B, null, user);
            blockCreate.write(Type.POSITION, position);
            blockCreate.write(Type.VAR_INT, toOldId(blockState));
            blockCreate.send(Protocol1_12_2To1_13.class, true);

            // Send a block entity update
            PacketWrapper wrapper = new PacketWrapper(0x09, null, user);
            wrapper.write(Type.POSITION, position);
            wrapper.write(Type.UNSIGNED_BYTE, (short) 5);
            wrapper.write(Type.NBT, nbt);
            wrapper.send(Protocol1_12_2To1_13.class, true);

        }
    }
}
