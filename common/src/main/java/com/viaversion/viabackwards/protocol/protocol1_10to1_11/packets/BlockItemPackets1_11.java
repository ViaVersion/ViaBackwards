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

package com.viaversion.viabackwards.protocol.protocol1_10to1_11.packets;

import com.viaversion.viabackwards.api.data.MappedLegacyBlockItem;
import com.viaversion.viabackwards.api.entities.storage.EntityTracker;
import com.viaversion.viabackwards.api.rewriters.LegacyBlockItemRewriter;
import com.viaversion.viabackwards.api.rewriters.LegacyEnchantmentRewriter;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.storage.ChestedHorseStorage;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.storage.WindowTracker;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_11Types;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.rewriter.ItemRewriter;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_11to1_10.EntityIdRewriter;
import com.viaversion.viaversion.protocols.protocol1_9_1_2to1_9_3_4.types.Chunk1_9_3_4Type;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;

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
