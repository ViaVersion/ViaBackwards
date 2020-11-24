/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_10to1_11.packets;

import nl.matsv.viabackwards.api.data.MappedLegacyBlockItem;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.LegacyBlockItemRewriter;
import nl.matsv.viabackwards.api.rewriters.LegacyEnchantmentRewriter;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.storage.ChestedHorseStorage;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.storage.WindowTracker;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.entities.Entity1_11Types;
import us.myles.ViaVersion.api.minecraft.BlockChangeRecord;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_11to1_10.EntityIdRewriter;
import us.myles.ViaVersion.protocols.protocol1_9_1_2to1_9_3_4.types.Chunk1_9_3_4Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.ListTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

import java.util.Arrays;
import java.util.Optional;

public class BlockItemPackets1_11 extends LegacyBlockItemRewriter<Protocol1_10To1_11> {

    private LegacyEnchantmentRewriter enchantmentRewriter;

    public BlockItemPackets1_11(Protocol1_10To1_11 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);

        protocol.registerOutgoing(ClientboundPackets1_9_3.SET_SLOT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE); // 0 - Window ID
                map(Type.SHORT); // 1 - Slot ID
                map(Type.ITEM); // 2 - Slot Value

                handler(itemRewriter.itemToClientHandler(Type.ITEM));

                // Handle Llama
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        if (isLlama(wrapper.user())) {
                            Optional<ChestedHorseStorage> horse = getChestedHorse(wrapper.user());
                            if (!horse.isPresent())
                                return;
                            ChestedHorseStorage storage = horse.get();
                            int currentSlot = wrapper.get(Type.SHORT, 0);
                            wrapper.set(Type.SHORT, 0, ((Integer) (currentSlot = getNewSlotId(storage, currentSlot))).shortValue());
                            wrapper.set(Type.ITEM, 0, getNewItem(storage, currentSlot, wrapper.get(Type.ITEM, 0)));
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_9_3.WINDOW_ITEMS, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.ITEM_ARRAY); // 1 - Window Values

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Item[] stacks = wrapper.get(Type.ITEM_ARRAY, 0);
                        for (int i = 0; i < stacks.length; i++)
                            stacks[i] = handleItemToClient(stacks[i]);

                        if (isLlama(wrapper.user())) {
                            Optional<ChestedHorseStorage> horse = getChestedHorse(wrapper.user());
                            if (!horse.isPresent())
                                return;
                            ChestedHorseStorage storage = horse.get();
                            stacks = Arrays.copyOf(stacks, !storage.isChested() ? 38 : 53);

                            for (int i = stacks.length - 1; i >= 0; i--) {
                                stacks[getNewSlotId(storage, i)] = stacks[i];
                                stacks[i] = getNewItem(storage, i, stacks[i]);
                            }
                            wrapper.set(Type.ITEM_ARRAY, 0, stacks);
                        }
                    }
                });
            }
        });

        itemRewriter.registerEntityEquipment(ClientboundPackets1_9_3.ENTITY_EQUIPMENT, Type.ITEM);

        // Plugin message Packet -> Trading
        protocol.registerOutgoing(ClientboundPackets1_9_3.PLUGIN_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // 0 - Channel

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        if (wrapper.get(Type.STRING, 0).equalsIgnoreCase("MC|TrList")) {
                            wrapper.passthrough(Type.INT); // Passthrough Window ID

                            int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                            for (int i = 0; i < size; i++) {
                                wrapper.write(Type.ITEM, handleItemToClient(wrapper.read(Type.ITEM))); // Input Item
                                wrapper.write(Type.ITEM, handleItemToClient(wrapper.read(Type.ITEM))); // Output Item

                                boolean secondItem = wrapper.passthrough(Type.BOOLEAN); // Has second item
                                if (secondItem) {
                                    wrapper.write(Type.ITEM, handleItemToClient(wrapper.read(Type.ITEM))); // Second Item
                                }

                                wrapper.passthrough(Type.BOOLEAN); // Trade disabled
                                wrapper.passthrough(Type.INT); // Number of tools uses
                                wrapper.passthrough(Type.INT); // Maximum number of trade uses
                            }
                        }
                    }
                });
            }
        });

        protocol.registerIncoming(ServerboundPackets1_9_3.CLICK_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.SHORT); // 1 - Slot
                map(Type.BYTE); // 2 - Button
                map(Type.SHORT); // 3 - Action number
                map(Type.VAR_INT); // 4 - Mode
                map(Type.ITEM); // 5 - Clicked Item

                handler(itemRewriter.itemToServerHandler(Type.ITEM));

                // Llama slot
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        if (isLlama(wrapper.user())) {
                            Optional<ChestedHorseStorage> horse = getChestedHorse(wrapper.user());
                            if (!horse.isPresent())
                                return;
                            ChestedHorseStorage storage = horse.get();
                            int clickSlot = wrapper.get(Type.SHORT, 0);
                            int correctSlot = getOldSlotId(storage, clickSlot);

                            wrapper.set(Type.SHORT, 0, ((Integer) correctSlot).shortValue());
                        }
                    }
                });
            }
        });

        itemRewriter.registerCreativeInvAction(ServerboundPackets1_9_3.CREATIVE_INVENTORY_ACTION, Type.ITEM);

        protocol.registerOutgoing(ClientboundPackets1_9_3.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

                        Chunk1_9_3_4Type type = new Chunk1_9_3_4Type(clientWorld); // Use the 1.10 Chunk type since nothing changed.
                        Chunk chunk = wrapper.passthrough(type);

                        handleChunk(chunk);

                        // only patch it for signs for now
                        for (CompoundTag tag : chunk.getBlockEntities()) {
                            Tag idTag = tag.get("id");
                            if (!(idTag instanceof StringTag)) continue;

                            String id = (String) idTag.getValue();
                            if (id.equals("minecraft:sign")) {
                                ((StringTag) idTag).setValue("Sign");
                            }
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_9_3.BLOCK_CHANGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // 0 - Block Position
                map(Type.VAR_INT); // 1 - Block

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int idx = wrapper.get(Type.VAR_INT, 0);
                        wrapper.set(Type.VAR_INT, 0, handleBlockID(idx));
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_9_3.MULTI_BLOCK_CHANGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // 0 - Chunk X
                map(Type.INT); // 1 - Chunk Z
                map(Type.BLOCK_CHANGE_RECORD_ARRAY);

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        for (BlockChangeRecord record : wrapper.get(Type.BLOCK_CHANGE_RECORD_ARRAY, 0)) {
                            record.setBlockId(handleBlockID(record.getBlockId()));
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_9_3.BLOCK_ENTITY_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // 0 - Position
                map(Type.UNSIGNED_BYTE); // 1 - Action
                map(Type.NBT); // 2 - NBT

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        // Remove on shulkerbox decleration
                        if (wrapper.get(Type.UNSIGNED_BYTE, 0) == 10) {
                            wrapper.cancel();
                        }
                        // Handler Spawners
                        if (wrapper.get(Type.UNSIGNED_BYTE, 0) == 1) {
                            CompoundTag tag = wrapper.get(Type.NBT, 0);
                            EntityIdRewriter.toClientSpawner(tag, true);
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_9_3.OPEN_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.STRING); // 1 - Window Type
                map(Type.COMPONENT); // 2 - Title
                map(Type.UNSIGNED_BYTE); // 3 - Slots

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = -1;
                        // Passthrough Entity ID
                        if (wrapper.get(Type.STRING, 0).equals("EntityHorse")) {
                            entityId = wrapper.passthrough(Type.INT);
                        }

                        // Track Inventory
                        String inventory = wrapper.get(Type.STRING, 0);
                        WindowTracker windowTracker = wrapper.user().get(WindowTracker.class);
                        windowTracker.setInventory(inventory);
                        windowTracker.setEntityId(entityId);

                        // Change llama slotcount to the donkey one
                        if (isLlama(wrapper.user())) {
                            wrapper.set(Type.UNSIGNED_BYTE, 1, (short) 17);
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_9_3.CLOSE_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                // Inventory tracking
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        WindowTracker windowTracker = wrapper.user().get(WindowTracker.class);
                        windowTracker.setInventory(null);
                        windowTracker.setEntityId(-1);
                    }
                });
            }
        });


        protocol.registerIncoming(ServerboundPackets1_9_3.CLOSE_WINDOW, new PacketRemapper() {
            @Override
            public void registerMap() {
                // Inventory tracking
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        WindowTracker windowTracker = wrapper.user().get(WindowTracker.class);
                        windowTracker.setInventory(null);
                        windowTracker.setEntityId(-1);
                    }
                });
            }
        });

        protocol.getEntityPackets().registerMetaHandler().handle(e -> {
            Metadata data = e.getData();

            if (data.getMetaType().getType().equals(Type.ITEM)) // Is Item
                data.setValue(handleItemToClient((Item) data.getValue()));

            return data;
        });
    }

    @Override
    protected void registerRewrites() {
        // Handle spawner block entity (map to itself with custom handler)
        MappedLegacyBlockItem data = replacementData.computeIfAbsent(52, s -> new MappedLegacyBlockItem(52, (short) -1, null, false));
        data.setBlockEntityHandler((b, tag) -> {
            EntityIdRewriter.toClientSpawner(tag, true);
            return tag;
        });

        enchantmentRewriter = new LegacyEnchantmentRewriter(nbtTagName);
        enchantmentRewriter.registerEnchantment(71, "§cCurse of Vanishing");
        enchantmentRewriter.registerEnchantment(10, "§cCurse of Binding");

        enchantmentRewriter.setHideLevelForEnchants(71, 10); // Curses do not display their level
    }

    @Override
    public Item handleItemToClient(Item item) {
        if (item == null) return null;
        super.handleItemToClient(item);

        CompoundTag tag = item.getTag();
        if (tag == null) return item;

        // Rewrite spawn eggs (id checks are done in the method itself)
        EntityIdRewriter.toClientItem(item, true);

        if (tag.get("ench") instanceof ListTag) {
            enchantmentRewriter.rewriteEnchantmentsToClient(tag, false);
        }
        if (tag.get("StoredEnchantments") instanceof ListTag) {
            enchantmentRewriter.rewriteEnchantmentsToClient(tag, true);
        }
        return item;
    }

    @Override
    public Item handleItemToServer(final Item item) {
        if (item == null) return null;
        super.handleItemToServer(item);

        CompoundTag tag = item.getTag();
        if (tag == null) return item;

        // Rewrite spawn eggs (id checks are done in the method itself)
        EntityIdRewriter.toServerItem(item, true);

        if (tag.contains(nbtTagName + "|ench")) {
            enchantmentRewriter.rewriteEnchantmentsToServer(tag, false);
        }
        if (tag.contains(nbtTagName + "|StoredEnchantments")) {
            enchantmentRewriter.rewriteEnchantmentsToServer(tag, true);
        }
        return item;
    }

    private boolean isLlama(UserConnection user) {
        WindowTracker tracker = user.get(WindowTracker.class);
        if (tracker.getInventory() != null && tracker.getInventory().equals("EntityHorse")) {
            EntityTracker.ProtocolEntityTracker entTracker = user.get(EntityTracker.class).get(getProtocol());
            EntityTracker.StoredEntity storedEntity = entTracker.getEntity(tracker.getEntityId());
            return storedEntity != null && storedEntity.getType().is(Entity1_11Types.EntityType.LIAMA);
        }
        return false;
    }

    private Optional<ChestedHorseStorage> getChestedHorse(UserConnection user) {
        WindowTracker tracker = user.get(WindowTracker.class);
        if (tracker.getInventory() != null && tracker.getInventory().equals("EntityHorse")) {
            EntityTracker.ProtocolEntityTracker entTracker = user.get(EntityTracker.class).get(getProtocol());
            EntityTracker.StoredEntity storedEntity = entTracker.getEntity(tracker.getEntityId());
            if (storedEntity != null)
                return Optional.of(storedEntity.get(ChestedHorseStorage.class));
        }
        return Optional.empty();
    }

    private int getNewSlotId(ChestedHorseStorage storage, int slotId) {
        int totalSlots = !storage.isChested() ? 38 : 53;
        int strength = storage.isChested() ? storage.getLiamaStrength() : 0;
        int startNonExistingFormula = 2 + 3 * strength;
        int offsetForm = 15 - (3 * strength);

        if (slotId >= startNonExistingFormula && totalSlots > (slotId + offsetForm))
            return offsetForm + slotId;
        if (slotId == 1)
            return 0;
        return slotId;
    }

    private int getOldSlotId(ChestedHorseStorage storage, int slotId) {
        int strength = storage.isChested() ? storage.getLiamaStrength() : 0;
        int startNonExistingFormula = 2 + 3 * strength;
        int endNonExistingFormula = 2 + 3 * (storage.isChested() ? 5 : 0);
        int offsetForm = endNonExistingFormula - startNonExistingFormula;

        if (slotId == 1 || slotId >= startNonExistingFormula && slotId < endNonExistingFormula)
            return 0;
        if (slotId >= endNonExistingFormula)
            return slotId - offsetForm;
        if (slotId == 0)
            return 1;
        return slotId;
    }

    private Item getNewItem(ChestedHorseStorage storage, int slotId, Item current) {
        int strength = storage.isChested() ? storage.getLiamaStrength() : 0;
        int startNonExistingFormula = 2 + 3 * strength;
        int endNonExistingFormula = 2 + 3 * (storage.isChested() ? 5 : 0);

        if (slotId >= startNonExistingFormula && slotId < endNonExistingFormula)
            return new Item(166, (byte) 1, (short) 0, getNamedTag("§4SLOT DISABLED"));
        if (slotId == 1)
            return null;
        return current;
    }
}
