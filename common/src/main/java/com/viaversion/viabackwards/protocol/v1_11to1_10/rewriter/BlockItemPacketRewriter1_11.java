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

package com.viaversion.viabackwards.protocol.v1_11to1_10.rewriter;

import com.viaversion.viabackwards.api.data.MappedLegacyBlockItem;
import com.viaversion.viabackwards.api.rewriters.LegacyBlockItemRewriter;
import com.viaversion.viabackwards.api.rewriters.LegacyEnchantmentRewriter;
import com.viaversion.viabackwards.protocol.v1_11to1_10.Protocol1_11To1_10;
import com.viaversion.viabackwards.protocol.v1_11to1_10.storage.ChestedHorseStorage;
import com.viaversion.viabackwards.protocol.v1_11to1_10.storage.WindowTracker;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.entity.EntityTracker;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_11;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_9_3;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viaversion.protocols.v1_10to1_11.data.EntityMappings1_11;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import com.viaversion.viaversion.util.IdAndData;
import java.util.Arrays;
import java.util.Optional;

public class BlockItemPacketRewriter1_11 extends LegacyBlockItemRewriter<ClientboundPackets1_9_3, ServerboundPackets1_9_3, Protocol1_11To1_10> {

    private LegacyEnchantmentRewriter enchantmentRewriter;

    public BlockItemPacketRewriter1_11(Protocol1_11To1_10 protocol) {
        super(protocol, "1.11");
    }

    @Override
    protected void registerPackets() {
        registerBlockChange(ClientboundPackets1_9_3.BLOCK_UPDATE);
        registerMultiBlockChange(ClientboundPackets1_9_3.CHUNK_BLOCKS_UPDATE);

        protocol.registerClientbound(ClientboundPackets1_9_3.CONTAINER_SET_SLOT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE); // 0 - Window ID
                map(Types.SHORT); // 1 - Slot ID
                map(Types.ITEM1_8); // 2 - Slot Value

                handler(wrapper -> handleItemToClient(wrapper.user(), wrapper.get(Types.ITEM1_8, 0)));

                // Handle Llama
                handler(wrapper -> {
                    if (isLlama(wrapper.user())) {
                        Optional<ChestedHorseStorage> horse = getChestedHorse(wrapper.user());
                        if (horse.isEmpty()) {
                            return;
                        }

                        ChestedHorseStorage storage = horse.get();
                        int currentSlot = wrapper.get(Types.SHORT, 0);
                        wrapper.set(Types.SHORT, 0, ((Integer) (currentSlot = getNewSlotId(storage, currentSlot))).shortValue());
                        wrapper.set(Types.ITEM1_8, 0, getNewItem(storage, currentSlot, wrapper.get(Types.ITEM1_8, 0)));
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.CONTAINER_SET_CONTENT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE); // 0 - Window ID
                map(Types.ITEM1_8_SHORT_ARRAY); // 1 - Window Values

                handler(wrapper -> {
                    Item[] stacks = wrapper.get(Types.ITEM1_8_SHORT_ARRAY, 0);
                    for (int i = 0; i < stacks.length; i++)
                        stacks[i] = handleItemToClient(wrapper.user(), stacks[i]);

                    if (isLlama(wrapper.user())) {
                        Optional<ChestedHorseStorage> horse = getChestedHorse(wrapper.user());
                        if (horse.isEmpty()) {
                            return;
                        }
                        ChestedHorseStorage storage = horse.get();
                        stacks = Arrays.copyOf(stacks, !storage.isChested() ? 38 : 53);

                        for (int i = stacks.length - 1; i >= 0; i--) {
                            stacks[getNewSlotId(storage, i)] = stacks[i];
                            stacks[i] = getNewItem(storage, i, stacks[i]);
                        }
                        wrapper.set(Types.ITEM1_8_SHORT_ARRAY, 0, stacks);
                    }
                });
            }
        });

        registerSetEquippedItem(ClientboundPackets1_9_3.SET_EQUIPPED_ITEM);
        registerCustomPayloadTradeList(ClientboundPackets1_9_3.CUSTOM_PAYLOAD);

        protocol.registerServerbound(ServerboundPackets1_9_3.CONTAINER_CLICK, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE); // 0 - Window ID
                map(Types.SHORT); // 1 - Slot
                map(Types.BYTE); // 2 - Button
                map(Types.SHORT); // 3 - Action number
                map(Types.VAR_INT); // 4 - Mode
                map(Types.ITEM1_8); // 5 - Clicked Item

                handler(wrapper -> handleItemToServer(wrapper.user(), wrapper.get(Types.ITEM1_8, 0)));

                // Llama slot
                handler(wrapper -> {
                    if (isLlama(wrapper.user())) {
                        Optional<ChestedHorseStorage> horse = getChestedHorse(wrapper.user());
                        if (horse.isEmpty()) {
                            return;
                        }
                        ChestedHorseStorage storage = horse.get();
                        int clickSlot = wrapper.get(Types.SHORT, 0);
                        int correctSlot = getOldSlotId(storage, clickSlot);

                        wrapper.set(Types.SHORT, 0, ((Integer) correctSlot).shortValue());
                    }
                });
            }
        });

        registerSetCreativeModeSlot(ServerboundPackets1_9_3.SET_CREATIVE_MODE_SLOT);

        protocol.registerClientbound(ClientboundPackets1_9_3.LEVEL_CHUNK, wrapper -> {
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

        protocol.registerClientbound(ClientboundPackets1_9_3.BLOCK_ENTITY_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BLOCK_POSITION1_8); // 0 - Position
                map(Types.UNSIGNED_BYTE); // 1 - Action
                map(Types.NAMED_COMPOUND_TAG); // 2 - NBT

                handler(wrapper -> {
                    // Remove on shulkerbox decleration
                    if (wrapper.get(Types.UNSIGNED_BYTE, 0) == 10) {
                        wrapper.cancel();
                    }
                    // Handler Spawners
                    if (wrapper.get(Types.UNSIGNED_BYTE, 0) == 1) {
                        CompoundTag tag = wrapper.get(Types.NAMED_COMPOUND_TAG, 0);
                        EntityMappings1_11.toClientSpawner(tag, true);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.OPEN_SCREEN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UNSIGNED_BYTE); // 0 - Window ID
                map(Types.STRING); // 1 - Window Type
                map(Types.COMPONENT); // 2 - Title
                map(Types.UNSIGNED_BYTE); // 3 - Slots

                handler(wrapper -> {
                    int entityId = -1;
                    // Passthrough Entity ID
                    if (wrapper.get(Types.STRING, 0).equals("EntityHorse")) {
                        entityId = wrapper.passthrough(Types.INT);
                    }
                    // Rewrite window title
                    protocol.getComponentRewriter().processText(wrapper.user(), wrapper.get(Types.COMPONENT, 0));

                    // Track Inventory
                    String inventory = wrapper.get(Types.STRING, 0);
                    WindowTracker windowTracker = wrapper.user().get(WindowTracker.class);
                    windowTracker.setInventory(inventory);
                    windowTracker.setEntityId(entityId);

                    // Change llama slotcount to the donkey one
                    if (isLlama(wrapper.user())) {
                        wrapper.set(Types.UNSIGNED_BYTE, 1, (short) 17);
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_9_3.CONTAINER_CLOSE, new PacketHandlers() {
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


        protocol.registerServerbound(ServerboundPackets1_9_3.CONTAINER_CLOSE, new PacketHandlers() {
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

        protocol.getEntityRewriter().filter().handler((event, data) -> {
            if (data.dataType().type().equals(Types.ITEM1_8)) // Is Item
                data.setValue(handleItemToClient(event.user(), (Item) data.getValue()));
        });
    }

    @Override
    protected void registerRewrites() {
        // Handle spawner block entity (map to itself with custom handler)
        MappedLegacyBlockItem data = itemReplacements.computeIfAbsent(IdAndData.toRawData(52), s -> new MappedLegacyBlockItem(52));
        data.setBlockEntityHandler((b, tag) -> EntityMappings1_11.toClientSpawner(tag, true));

        enchantmentRewriter = new LegacyEnchantmentRewriter(nbtTagName());
        enchantmentRewriter.registerEnchantment(71, "§cCurse of Vanishing");
        enchantmentRewriter.registerEnchantment(10, "§cCurse of Binding");

        enchantmentRewriter.setHideLevelForEnchants(71, 10); // Curses do not display their level
    }

    @Override
    public Item handleItemToClient(UserConnection connection, Item item) {
        if (item == null) return null;
        super.handleItemToClient(connection, item);

        CompoundTag tag = item.tag();
        if (tag == null) return item;

        // Rewrite spawn eggs (id checks are done in the method itself)
        EntityMappings1_11.toClientItem(item, true);

        enchantmentRewriter.handleToClient(item);
        return item;
    }

    @Override
    public Item handleItemToServer(UserConnection connection, Item item) {
        if (item == null) return null;
        super.handleItemToServer(connection, item);

        CompoundTag tag = item.tag();
        if (tag == null) return item;

        // Rewrite spawn eggs (id checks are done in the method itself)
        EntityMappings1_11.toServerItem(item, true);

        enchantmentRewriter.handleToServer(item);
        return item;
    }

    private boolean isLlama(UserConnection user) {
        WindowTracker tracker = user.get(WindowTracker.class);
        if (tracker.getInventory() != null && tracker.getInventory().equals("EntityHorse")) {
            EntityTracker entTracker = user.getEntityTracker(Protocol1_11To1_10.class);
            StoredEntityData entityData = entTracker.entityData(tracker.getEntityId());
            return entityData != null && entityData.type().is(EntityTypes1_11.EntityType.LLAMA);
        }
        return false;
    }

    private Optional<ChestedHorseStorage> getChestedHorse(UserConnection user) {
        WindowTracker tracker = user.get(WindowTracker.class);
        if (tracker.getInventory() != null && tracker.getInventory().equals("EntityHorse")) {
            EntityTracker entTracker = user.getEntityTracker(Protocol1_11To1_10.class);
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
