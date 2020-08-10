package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.packets;

import com.google.common.collect.ImmutableSet;
import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.EnchantmentRewriter;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.storage.ChunkLightStorage;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.entities.Entity1_14Types;
import us.myles.ViaVersion.api.entities.EntityType;
import us.myles.ViaVersion.api.minecraft.Environment;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_13_2;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.BlockRewriter;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.rewriters.RecipeRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.version.Types1_13;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.data.RecipeRewriter1_13_2;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.types.Chunk1_13Type;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.types.Chunk1_14Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.viaversion.libs.gson.JsonElement;
import us.myles.viaversion.libs.gson.JsonObject;
import us.myles.viaversion.libs.opennbt.conversion.ConverterRegistry;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BlockItemPackets1_14 extends nl.matsv.viabackwards.api.rewriters.ItemRewriter<Protocol1_13_2To1_14> {

    private EnchantmentRewriter enchantmentRewriter;

    public BlockItemPackets1_14(Protocol1_13_2To1_14 protocol, TranslatableRewriter translatableRewriter) {
        super(protocol, translatableRewriter, BlockItemPackets1_14::getOldItemId, BlockItemPackets1_14::getNewItemId, id -> BackwardsMappings.itemMappings.getMappedItem(id));
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
        BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION, Protocol1_13_2To1_14::getNewBlockStateId, Protocol1_13_2To1_14::getNewBlockId);

        itemRewriter.registerSetCooldown(ClientboundPackets1_14.COOLDOWN, BlockItemPackets1_14::getOldItemId);
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

                        if (entityType.isOrHasParent(Entity1_14Types.EntityType.ABSTRACT_HORSE)) {
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
                    int mappedId = Protocol1_13_2To1_14.getNewBlockId(wrapper.get(Type.VAR_INT, 0));
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

                        wrapper.set(Type.VAR_INT, 0, Protocol1_13_2To1_14.getNewBlockStateId(id));
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
                                            if (MappingData.nonFullBlocks.contains(id)) {
                                                section.getBlockLightNibbleArray().set(x, y, z, 0);
                                            }
                                        }
                                    }
                                }
                            }

                            for (int j = 0; j < section.getPaletteSize(); j++) {
                                int old = section.getPaletteEntry(j);
                                int newId = Protocol1_13_2To1_14.getNewBlockStateId(old);
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
                            wrapper.set(Type.INT, 1, data = BlockItemPackets1_14.getOldItemId(data));
                        } else if (id == 2001) { // Block break + block break sound
                            wrapper.set(Type.INT, 1, data = Protocol1_13_2To1_14.getNewBlockStateId(data));
                        }
                    }
                });
            }
        });

        blockRewriter.registerSpawnParticle(ClientboundPackets1_14.SPAWN_PARTICLE, 3, 23, 32,
                EntityPackets1_14::getOldParticleId, this::handleItemToClient, Type.FLAT_VAR_INT_ITEM, Type.FLOAT);

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
        enchantmentRewriter = new EnchantmentRewriter(nbtTagName, false);
        enchantmentRewriter.registerEnchantment("minecraft:multishot", "§7Multishot");
        enchantmentRewriter.registerEnchantment("minecraft:quick_charge", "§7Quick Charge");
        enchantmentRewriter.registerEnchantment("minecraft:piercing", "§7Piercing");
    }

    @Override
    public Item handleItemToClient(Item item) {
        if (item == null) return null;
        super.handleItemToClient(item);

        CompoundTag tag = item.getTag();
        if (tag != null) {
            // Display Name now uses JSON
            if (tag.get("display") instanceof CompoundTag) {
                CompoundTag display = tag.get("display");
                if (((CompoundTag) tag.get("display")).get("Lore") instanceof ListTag) {
                    ListTag lore = display.get("Lore");
                    ListTag via = display.remove(nbtTagName + "|Lore");
                    if (via != null) {
                        display.put(ConverterRegistry.convertToTag("Lore", ConverterRegistry.convertToValue(via)));
                    } else {
                        for (Tag loreEntry : lore) {
                            if (!(loreEntry instanceof StringTag)) continue;

                            String value = ((StringTag) loreEntry).getValue();
                            if (value != null && !value.isEmpty()) {
                                ((StringTag) loreEntry).setValue(ChatRewriter.jsonTextToLegacy(value));
                            }
                        }
                    }
                }
            }

            enchantmentRewriter.handleToClient(item);
        }
        return item;
    }

    @Override
    public Item handleItemToServer(Item item) {
        if (item == null) return null;
        super.handleItemToServer(item);

        CompoundTag tag = item.getTag();
        if (tag != null) {
            // Display Lore now uses JSON
            if (tag.get("display") instanceof CompoundTag) {
                CompoundTag display = tag.get("display");
                if (display.get("Lore") instanceof ListTag) {
                    ListTag lore = display.get("Lore");
                    display.put(ConverterRegistry.convertToTag(nbtTagName + "|Lore", ConverterRegistry.convertToValue(lore)));
                    for (Tag loreEntry : lore) {
                        if (loreEntry instanceof StringTag) {
                            ((StringTag) loreEntry).setValue(ChatRewriter.legacyTextToJson(((StringTag) loreEntry).getValue()).toString());
                        }
                    }
                }
            }

            enchantmentRewriter.handleToServer(item);
        }
        return item;
    }


    public static int getNewItemId(int id) {
        int newId = MappingData.oldToNewItems.get(id);
        if (newId == -1) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.14 item for 1.13.2 item " + id);
            return 1;
        }
        return newId;
    }

    public static int getOldItemId(int id) {
        int oldId = MappingData.oldToNewItems.inverse().get(id);
        if (oldId == -1) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.13.2 item for 1.14 item " + id);
            return 1;
        }
        return oldId;
    }
}
