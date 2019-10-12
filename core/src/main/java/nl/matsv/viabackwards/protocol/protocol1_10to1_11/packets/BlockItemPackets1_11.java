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

import net.md_5.bungee.api.ChatColor;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.BlockItemRewriter;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.EntityTypeNames;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.storage.ChestedHorseStorage;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.storage.WindowTracker;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data.BlockColors;
import nl.matsv.viabackwards.utils.Block;
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
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_1_2to1_9_3_4.types.Chunk1_9_3_4Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;

import java.util.Arrays;
import java.util.Optional;

public class BlockItemPackets1_11 extends BlockItemRewriter<Protocol1_10To1_11> {
    @Override
    protected void registerPackets(Protocol1_10To1_11 protocol) {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);

        // Set slot packet
        protocol.registerOutgoing(State.PLAY, 0x16, 0x16, new PacketRemapper() {
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

        // Window items packet
        protocol.registerOutgoing(State.PLAY, 0x14, 0x14, new PacketRemapper() {
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

        // Entity Equipment Packet
        itemRewriter.registerEntityEquipment(Type.ITEM, 0x3C, 0x3C);

        // Plugin message Packet -> Trading
        protocol.registerOutgoing(State.PLAY, 0x18, 0x18, new PacketRemapper() {
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
                                if (secondItem)
                                    wrapper.write(Type.ITEM, handleItemToClient(wrapper.read(Type.ITEM))); // Second Item

                                wrapper.passthrough(Type.BOOLEAN); // Trade disabled
                                wrapper.passthrough(Type.INT); // Number of tools uses
                                wrapper.passthrough(Type.INT); // Maximum number of trade uses
                            }
                        }
                    }
                });
            }
        });

        // Click window packet
        protocol.registerIncoming(State.PLAY, 0x07, 0x07, new PacketRemapper() {
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
                }
        );

        // Creative Inventory Action
        itemRewriter.registerCreativeInvAction(Type.ITEM, 0x18, 0x18);

        /* Block packets */
        // Chunk packet
        protocol.registerOutgoing(State.PLAY, 0x20, 0x20, new PacketRemapper() {
                    @Override
                    public void registerMap() {
                        handler(new PacketHandler() {
                            @Override
                            public void handle(PacketWrapper wrapper) throws Exception {
                                ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

                                Chunk1_9_3_4Type type = new Chunk1_9_3_4Type(clientWorld); // Use the 1.10 Chunk type since nothing changed.
                                Chunk chunk = wrapper.passthrough(type);

                                handleChunk(chunk);

                                // only patch it for signs for now, TODO-> Find all the block entities old/new to replace ids and implement in ViaVersion
                                chunk.getBlockEntities().stream()
                                        .filter(tag -> tag.contains("id") && tag.get("id") instanceof StringTag)
                                        .forEach(tag -> {
                                            String id = (String) tag.get("id").getValue();
                                            if (id.equals("minecraft:sign")) {
                                                ((StringTag) tag.get("id")).setValue("Sign");
                                            }
                                        });
                            }
                        });
                    }
                }
        );

        // Block Change Packet
        protocol.registerOutgoing(State.PLAY, 0x0B, 0x0B, new PacketRemapper() {
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
                }
        );

        // Multi Block Change Packet
        protocol.registerOutgoing(State.PLAY, 0x10, 0x10, new PacketRemapper() {
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
                }
        );

        // Update Block Entity
        protocol.registerOutgoing(State.PLAY, 0x09, 0x09, new PacketRemapper() {
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
                            EntityTypeNames.toClientSpawner(tag);
                        }
                    }
                });
            }
        });

        // Open window packet
        protocol.registerOutgoing(State.PLAY, 0x13, 0x13, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // 0 - Window ID
                map(Type.STRING); // 1 - Window Type
                map(Type.STRING); // 2 - Title
                map(Type.UNSIGNED_BYTE); // 3 - Slots

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int entityId = -1;
                        // Passthrough Entity ID
                        if (wrapper.get(Type.STRING, 0).equals("EntityHorse"))
                            entityId = wrapper.passthrough(Type.INT);

                        // Track Inventory
                        String inventory = wrapper.get(Type.STRING, 0);
                        WindowTracker windowTracker = wrapper.user().get(WindowTracker.class);
                        windowTracker.setInventory(inventory);
                        windowTracker.setEntityId(entityId);

                        // Change llama slotcount to the donkey one
                        if (isLlama(wrapper.user()))
                            wrapper.set(Type.UNSIGNED_BYTE, 1, (short) 17);
                    }
                });
            }
        });

        // Close Window Packet
        protocol.registerOutgoing(State.PLAY, 0x12, 0x12, new PacketRemapper() {
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


        // Close Window Incoming Packet
        protocol.registerIncoming(State.PLAY, 0x08, 0x08, new PacketRemapper() {
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
        // ShulkerBoxes to EnderChest
        for (int i = 219; i < 235; i++)
            rewrite(i)
                    .repItem(new Item((short) 130, (byte) 1, (short) 0, getNamedTag("1.11 " + BlockColors.get(i - 219) + " Shulker Box")))
                    .repBlock(new Block(130, 4)) // TODO investigate this
                    .blockEntityHandler((block, tag) -> {
                        tag.remove("id");
                        tag.put(new StringTag("id", "minecraft:chest"));
                        return tag;
                    });

        // Observer to Dispenser
        rewrite(218).repItem(new Item((short) 23, (byte) 1, (short) 0, getNamedTag("1.11 Observer"))).repBlock(new Block(23, -1));

        // Handle spawner block entity
        rewrite(52).blockEntityHandler((b, tag) -> {
            EntityTypeNames.toClientSpawner(tag);
            return tag;
        });

        // Rewrite spawn eggs
        rewrite(383).itemHandler((i) -> {
            EntityTypeNames.toClientItem(i);
            return i;
        });

        // Totem of Undying to Dead Bush
        rewrite(449).repItem(new Item((short) 32, (byte) 1, (short) 0, getNamedTag("1.11 Totem of Undying")));

        // Shulker shell to Popped Chorus Fruit
        rewrite(450).repItem(new Item((short) 433, (byte) 1, (short) 0, getNamedTag("1.11 Shulker Shell")));

    }

    private boolean isLlama(UserConnection user) {
        WindowTracker tracker = user.get(WindowTracker.class);
        if (tracker.getInventory() != null && tracker.getInventory().equals("EntityHorse")) {
            EntityTracker.ProtocolEntityTracker entTracker = user.get(EntityTracker.class).get(getProtocol());
            Optional<EntityTracker.StoredEntity> optEntity = entTracker.getEntity(tracker.getEntityId());
            return optEntity.isPresent() && optEntity.get().getType().is(Entity1_11Types.EntityType.LIAMA);
        }
        return false;
    }

    private Optional<ChestedHorseStorage> getChestedHorse(UserConnection user) {
        WindowTracker tracker = user.get(WindowTracker.class);
        if (tracker.getInventory() != null && tracker.getInventory().equals("EntityHorse")) {
            EntityTracker.ProtocolEntityTracker entTracker = user.get(EntityTracker.class).get(getProtocol());
            Optional<EntityTracker.StoredEntity> optEntity = entTracker.getEntity(tracker.getEntityId());
            if (optEntity.isPresent())
                return Optional.of(optEntity.get().get(ChestedHorseStorage.class));
        }
        return Optional.empty();
    }

    // TODO improve the llama inventory part
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
            return new Item((short) 166, (byte) 1, (short) 0, getNamedTag(ChatColor.RED + "SLOT DISABLED"));
        if (slotId == 1)
            return null;
        return current;
    }
}
