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

package com.viaversion.viabackwards.protocol.protocol1_10to1_11.packets;

import com.viaversion.viabackwards.api.data.MappedLegacyBlockItem;
import com.viaversion.viabackwards.api.rewriters.LegacyBlockItemRewriter;
import com.viaversion.viabackwards.api.rewriters.LegacyEnchantmentRewriter;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.storage.ChestedHorseStorage;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.storage.WindowTracker;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_11;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.protocols.protocol1_11to1_10.EntityIdRewriter;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import java.util.Arrays;
import java.util.Optional;

public class BlockItemPackets1_11 extends LegacyBlockItemRewriter<ClientboundPackets1_9_3, ServerboundPackets1_9_3, Protocol1_10To1_11> {

    private LegacyEnchantmentRewriter enchantmentRewriter;

    public BlockItemPackets1_11(Protocol1_10To1_11 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_9_3.SET_SLOT, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.SHORT); // 1 - Slot ID
                map(Type.ITEM1_8); // 2 - Slot Value

                handler(itemToClientHandler(Type.ITEM1_8));

                // Handle Llama
                handler(wrapper -> {
                    if (isLlama(wrapper.user())) {
                        Optional<ChestedHorseStorage> horse = getChestedHorse(wrapper.user());
                        if (!horse.isPresent())
                            return;
                        ChestedHorseStorage storage = horse.get();
                        int currentSlot = wrapper.get(Type.SHORT, 0);
                        wrapper.set(Type.SHORT, 0, ((Integer) (currentSlot = getNewSlotId(storage, currentSlot))).shortValue());
                        wrapper.set(Type.ITEM1_8, 0, getNewItem(storage, currentSlot, wrapper.get(Type.ITEM1_8, 0)));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.WINDOW_ITEMS, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.ITEM1_8_SHORT_ARRAY); // 1 - Window Values

                handler(wrapper -> {
                    Item[] stacks = wrapper.get(Type.ITEM1_8_SHORT_ARRAY, 0);
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
                        wrapper.set(Type.ITEM1_8_SHORT_ARRAY, 0, stacks);
                    }
                });
            }
        });

        registerEntityEquipment(ClientboundPackets1_9_3.ENTITY_EQUIPMENT);

        // Plugin message -> Trading
        protocol.registerClientbound(ClientboundPackets1_9_3.PLUGIN_MESSAGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // 0 - Channel

                handler(wrapper -> {
                    if (wrapper.get(Type.STRING, 0).equalsIgnoreCase("MC|TrList")) {
                        wrapper.passthrough(Type.INT); // Passthrough Window ID

                        int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                        for (int i = 0; i < size; i++) {
                            wrapper.write(Type.ITEM1_8, handleItemToClient(wrapper.read(Type.ITEM1_8))); // Input Item
                            wrapper.write(Type.ITEM1_8, handleItemToClient(wrapper.read(Type.ITEM1_8))); // Output Item

                            boolean secondItem = wrapper.passthrough(Type.BOOLEAN); // Has second item
                            if (secondItem) {
                                wrapper.write(Type.ITEM1_8, handleItemToClient(wrapper.read(Type.ITEM1_8))); // Second Item
                            }

                            wrapper.passthrough(Type.BOOLEAN); // Trade disabled
                            wrapper.passthrough(Type.INT); // Number of tools uses
                            wrapper.passthrough(Type.INT); // Maximum number of trade uses
                        }
                    }
                });
            }
        });

        protocol.registerServerbound(ServerboundPackets1_9_3.CLICK_WINDOW, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.SHORT); // 1 - Slot
                map(Type.BYTE); // 2 - Button
                map(Type.SHORT); // 3 - Action number
                map(Type.VAR_INT); // 4 - Mode
                map(Type.ITEM1_8); // 5 - Clicked Item

                handler(itemToServerHandler(Type.ITEM1_8));

                // Llama slot
                handler(wrapper -> {
                    if (isLlama(wrapper.user())) {
                        Optional<ChestedHorseStorage> horse = getChestedHorse(wrapper.user());
                        if (!horse.isPresent())
                            return;
                        ChestedHorseStorage storage = horse.get();
                        int clickSlot = wrapper.get(Type.SHORT, 0);
                        int correctSlot = getOldSlotId(storage, clickSlot);

                        wrapper.set(Type.SHORT, 0, ((Integer) correctSlot).shortValue());
                    }
                });
            }
        });

        registerCreativeInvAction(ServerboundPackets1_9_3.CREATIVE_INVENTORY_ACTION);

        protocol.registerClientbound(ClientboundPackets1_9_3.CHUNK_DATA, wrapper -> {
            ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

            ChunkType1_9_3 type = ChunkType1_9_3.forEnvironment(clientWorld.getEnvironment()); // Use the 1.10 Chunk type since nothing changed.
            Chunk chunk = wrapper.passthrough(type);

            handleChunk(chunk);

            // only patch it for signs for now
            for (CompoundTag tag : chunk.getBlockEntities()) {
                StringTag idTag = tag.getStringTag("id");
                if (idTag == null) continue;

                String id = idTag.getValue();
                if (id.equals("minecraft:sign")) {
                    idTag.setValue("Sign");
                }
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.BLOCK_CHANGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_8); // 0 - Block Position
                map(Type.VAR_INT); // 1 - Block

                handler(wrapper -> {
                    int idx = wrapper.get(Type.VAR_INT, 0);
                    wrapper.set(Type.VAR_INT, 0, handleBlockID(idx));
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.MULTI_BLOCK_CHANGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // 0 - Chunk X
                map(Type.INT); // 1 - Chunk Z
                map(Type.BLOCK_CHANGE_RECORD_ARRAY);

                handler(wrapper -> {
                    for (BlockChangeRecord record : wrapper.get(Type.BLOCK_CHANGE_RECORD_ARRAY, 0)) {
                        record.setBlockId(handleBlockID(record.getBlockId()));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.POSITION1_8); // 0 - Position
                map(Type.UNSIGNED_BYTE); // 1 - Action
                map(Type.NAMED_COMPOUND_TAG); // 2 - NBT

                handler(wrapper -> {
                    // Remove on shulkerbox decleration
                    if (wrapper.get(Type.UNSIGNED_BYTE, 0) == 10) {
                        wrapper.cancel();
                    }
                    // Handler Spawners
                    if (wrapper.get(Type.UNSIGNED_BYTE, 0) == 1) {
                        CompoundTag tag = wrapper.get(Type.NAMED_COMPOUND_TAG, 0);
                        EntityIdRewriter.toClientSpawner(tag, true);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.OPEN_WINDOW, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.STRING); // 1 - Window Type
                map(Type.COMPONENT); // 2 - Title
                map(Type.UNSIGNED_BYTE); // 3 - Slots

                handler(wrapper -> {
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
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.CLOSE_WINDOW, new PacketHandlers() {
            @Override
            public void register() {
                // Inventory tracking
                handler(wrapper -> {
                    WindowTracker windowTracker = wrapper.user().get(WindowTracker.class);
                    windowTracker.setInventory(null);
                    windowTracker.setEntityId(-1);
                });
            }
        });


        protocol.registerServerbound(ServerboundPackets1_9_3.CLOSE_WINDOW, new PacketHandlers() {
            @Override
            public void register() {
                // Inventory tracking
                handler(wrapper -> {
                    WindowTracker windowTracker = wrapper.user().get(WindowTracker.class);
                    windowTracker.setInventory(null);
                    windowTracker.setEntityId(-1);
                });
            }
        });

        protocol.getEntityRewriter().filter().handler((event, meta) -> {
            if (meta.metaType().type().equals(Type.ITEM1_8)) // Is Item
                meta.setValue(handleItemToClient((Item) meta.getValue()));
        });
    }

    @Override
    protected void registerRewrites() {
        // Handle spawner block entity (map to itself with custom handler)
        MappedLegacyBlockItem data = replacementData.computeIfAbsent(52 << 4, s -> new MappedLegacyBlockItem(52, (short) -1, null, false));
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

        CompoundTag tag = item.tag();
        if (tag == null) return item;

        // Rewrite spawn eggs (id checks are done in the method itself)
        EntityIdRewriter.toClientItem(item, true);

        if (tag.getListTag("ench") != null) {
            enchantmentRewriter.rewriteEnchantmentsToClient(tag, false);
        }
        if (tag.getListTag("StoredEnchantments") != null) {
            enchantmentRewriter.rewriteEnchantmentsToClient(tag, true);
        }
        return item;
    }

    @Override
    public Item handleItemToServer(Item item) {
        if (item == null) return null;
        super.handleItemToServer(item);

        CompoundTag tag = item.tag();
        if (tag == null) return item;

        // Rewrite spawn eggs (id checks are done in the method itself)
        EntityIdRewriter.toServerItem(item, true);

        if (tag.getListTag(nbtTagName + "|ench") != null) {
            enchantmentRewriter.rewriteEnchantmentsToServer(tag, false);
        }
        if (tag.getListTag(nbtTagName + "|StoredEnchantments") != null) {
            enchantmentRewriter.rewriteEnchantmentsToServer(tag, true);
        }
        return item;
    }

    private boolean isLlama(UserConnection user) {
        WindowTracker tracker = user.get(WindowTracker.class);
        if (tracker.getInventory() != null && tracker.getInventory().equals("EntityHorse")) {
            EntityTracker entTracker = user.getEntityTracker(Protocol1_10To1_11.class);
            StoredEntityData entityData = entTracker.entityData(tracker.getEntityId());
            return entityData != null && entityData.type().is(EntityTypes1_11.EntityType.LIAMA);
        }
        return false;
    }

    private Optional<ChestedHorseStorage> getChestedHorse(UserConnection user) {
        WindowTracker tracker = user.get(WindowTracker.class);
        if (tracker.getInventory() != null && tracker.getInventory().equals("EntityHorse")) {
            EntityTracker entTracker = user.getEntityTracker(Protocol1_10To1_11.class);
            StoredEntityData entityData = entTracker.entityData(tracker.getEntityId());
            if (entityData != null)
                return Optional.of(entityData.get(ChestedHorseStorage.class));
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
            return new DataItem(166, (byte) 1, (short) 0, getNamedTag("§4SLOT DISABLED"));
        if (slotId == 1)
            return null;
        return current;
    }
}
