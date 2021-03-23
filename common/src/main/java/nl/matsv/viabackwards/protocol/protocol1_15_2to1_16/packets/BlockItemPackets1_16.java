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
package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.packets;

import nl.matsv.viabackwards.api.rewriters.EnchantmentRewriter;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data.MapColorRewriter;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.BlockRewriter;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.UUIDIntArrayType;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.ServerboundPackets1_14;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.data.RecipeRewriter1_14;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.types.Chunk1_15Type;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.ClientboundPackets1_16;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.packets.InventoryPackets;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.types.Chunk1_16Type;
import us.myles.ViaVersion.util.CompactArrayUtil;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.LongArrayTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlockItemPackets1_16 extends nl.matsv.viabackwards.api.rewriters.ItemRewriter<Protocol1_15_2To1_16> {

    private EnchantmentRewriter enchantmentRewriter;

    public BlockItemPackets1_16(Protocol1_15_2To1_16 protocol, TranslatableRewriter translatableRewriter) {
        super(protocol, translatableRewriter);
    }

    @Override
    protected void registerPackets() {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);
        BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION1_14);

        RecipeRewriter1_14 recipeRewriter = new RecipeRewriter1_14(protocol, this::handleItemToClient);
        // Remove new smithing type, only in this handler
        protocol.registerOutgoing(ClientboundPackets1_16.DECLARE_RECIPES, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int size = wrapper.passthrough(Type.VAR_INT);
                    int newSize = size;
                    for (int i = 0; i < size; i++) {
                        String originalType = wrapper.read(Type.STRING);
                        String type = originalType.replace("minecraft:", "");
                        if (type.equals("smithing")) {
                            newSize--;

                            wrapper.read(Type.STRING);
                            wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT);
                            wrapper.read(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT);
                            wrapper.read(Type.FLAT_VAR_INT_ITEM);
                            continue;
                        }

                        wrapper.write(Type.STRING, originalType);
                        String id = wrapper.passthrough(Type.STRING); // Recipe Identifier
                        recipeRewriter.handle(wrapper, type);
                    }

                    wrapper.set(Type.VAR_INT, 0, newSize);
                });
            }
        });

        itemRewriter.registerSetCooldown(ClientboundPackets1_16.COOLDOWN);
        itemRewriter.registerWindowItems(ClientboundPackets1_16.WINDOW_ITEMS, Type.FLAT_VAR_INT_ITEM_ARRAY);
        itemRewriter.registerSetSlot(ClientboundPackets1_16.SET_SLOT, Type.FLAT_VAR_INT_ITEM);
        itemRewriter.registerTradeList(ClientboundPackets1_16.TRADE_LIST, Type.FLAT_VAR_INT_ITEM);
        itemRewriter.registerAdvancements(ClientboundPackets1_16.ADVANCEMENTS, Type.FLAT_VAR_INT_ITEM);

        blockRewriter.registerAcknowledgePlayerDigging(ClientboundPackets1_16.ACKNOWLEDGE_PLAYER_DIGGING);
        blockRewriter.registerBlockAction(ClientboundPackets1_16.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_16.BLOCK_CHANGE);
        blockRewriter.registerMultiBlockChange(ClientboundPackets1_16.MULTI_BLOCK_CHANGE);

        protocol.registerOutgoing(ClientboundPackets1_16.ENTITY_EQUIPMENT, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int entityId = wrapper.passthrough(Type.VAR_INT);

                    List<EquipmentData> equipmentData = new ArrayList<>();
                    byte slot;
                    do {
                        slot = wrapper.read(Type.BYTE);
                        Item item = handleItemToClient(wrapper.read(Type.FLAT_VAR_INT_ITEM));
                        int rawSlot = slot & 0x7F;
                        equipmentData.add(new EquipmentData(rawSlot, item));
                    } while ((slot & 0xFFFFFF80) != 0);

                    // Send first data in the current packet
                    EquipmentData firstData = equipmentData.get(0);
                    wrapper.write(Type.VAR_INT, firstData.slot);
                    wrapper.write(Type.FLAT_VAR_INT_ITEM, firstData.item);

                    // If there are more items, send new packets for them
                    for (int i = 1; i < equipmentData.size(); i++) {
                        PacketWrapper equipmentPacket = wrapper.create(ClientboundPackets1_15.ENTITY_EQUIPMENT);
                        EquipmentData data = equipmentData.get(i);
                        equipmentPacket.write(Type.VAR_INT, entityId);
                        equipmentPacket.write(Type.VAR_INT, data.slot);
                        equipmentPacket.write(Type.FLAT_VAR_INT_ITEM, data.item);
                        equipmentPacket.send(Protocol1_15_2To1_16.class);
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_16.UPDATE_LIGHT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // x
                map(Type.VAR_INT); // y
                map(Type.BOOLEAN, Type.NOTHING);
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_16.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    Chunk chunk = wrapper.read(new Chunk1_16Type());
                    wrapper.write(new Chunk1_15Type(), chunk);

                    for (int i = 0; i < chunk.getSections().length; i++) {
                        ChunkSection section = chunk.getSections()[i];
                        if (section == null) continue;
                        for (int j = 0; j < section.getPaletteSize(); j++) {
                            int old = section.getPaletteEntry(j);
                            section.setPaletteEntry(j, protocol.getMappingData().getNewBlockStateId(old));
                        }
                    }

                    CompoundTag heightMaps = chunk.getHeightMap();
                    for (Tag heightMapTag : heightMaps.values()) {
                        LongArrayTag heightMap = (LongArrayTag) heightMapTag;
                        int[] heightMapData = new int[256];
                        CompactArrayUtil.iterateCompactArrayWithPadding(9, heightMapData.length, heightMap.getValue(), (i, v) -> heightMapData[i] = v);
                        heightMap.setValue(CompactArrayUtil.createCompactArray(9, heightMapData.length, i -> heightMapData[i]));
                    }

                    if (chunk.isBiomeData()) {
                        for (int i = 0; i < 1024; i++) {
                            int biome = chunk.getBiomeData()[i];
                            switch (biome) {
                                case 170: // new nether biomes
                                case 171:
                                case 172:
                                case 173:
                                    chunk.getBiomeData()[i] = 8;
                                    break;
                            }
                        }
                    }

                    if (chunk.getBlockEntities() == null) return;
                    for (CompoundTag blockEntity : chunk.getBlockEntities()) {
                        handleBlockEntity(blockEntity);
                    }
                });
            }
        });

        blockRewriter.registerEffect(ClientboundPackets1_16.EFFECT, 1010, 2001);

        itemRewriter.registerSpawnParticle(ClientboundPackets1_16.SPAWN_PARTICLE, Type.FLAT_VAR_INT_ITEM, Type.DOUBLE);

        protocol.registerOutgoing(ClientboundPackets1_16.WINDOW_PROPERTY, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // Window id
                map(Type.SHORT); // Property
                map(Type.SHORT); // Value
                handler(wrapper -> {
                    short property = wrapper.get(Type.SHORT, 0);
                    if (property >= 4 && property <= 6) { // Enchantment id
                        short enchantmentId = wrapper.get(Type.SHORT, 1);
                        if (enchantmentId > 11) { // soul_speed
                            wrapper.set(Type.SHORT, 1, --enchantmentId);
                        } else if (enchantmentId == 11) {
                            wrapper.set(Type.SHORT, 1, (short) 9);
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_16.MAP_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Map ID
                map(Type.BYTE); // Scale
                map(Type.BOOLEAN); // Tracking Position
                map(Type.BOOLEAN); // Locked
                handler(wrapper -> {
                    int iconCount = wrapper.passthrough(Type.VAR_INT);
                    for (int i = 0; i < iconCount; i++) {
                        wrapper.passthrough(Type.VAR_INT); // Type
                        wrapper.passthrough(Type.BYTE); // X
                        wrapper.passthrough(Type.BYTE); // Z
                        wrapper.passthrough(Type.BYTE); // Direction
                        if (wrapper.passthrough(Type.BOOLEAN)) {
                            wrapper.passthrough(Type.COMPONENT); // Display Name
                        }
                    }

                    short columns = wrapper.passthrough(Type.UNSIGNED_BYTE);
                    if (columns < 1) return;

                    wrapper.passthrough(Type.UNSIGNED_BYTE); // Rows
                    wrapper.passthrough(Type.UNSIGNED_BYTE); // X
                    wrapper.passthrough(Type.UNSIGNED_BYTE); // Z
                    byte[] data = wrapper.passthrough(Type.BYTE_ARRAY_PRIMITIVE);
                    for (int i = 0; i < data.length; i++) {
                        int color = data[i] & 0xFF;
                        int mappedColor = MapColorRewriter.getMappedColor(color);
                        if (mappedColor != -1) {
                            data[i] = (byte) mappedColor;
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_16.BLOCK_ENTITY_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    Position position = wrapper.passthrough(Type.POSITION1_14);
                    short action = wrapper.passthrough(Type.UNSIGNED_BYTE);
                    CompoundTag tag = wrapper.passthrough(Type.NBT);
                    handleBlockEntity(tag);
                });
            }
        });

        itemRewriter.registerClickWindow(ServerboundPackets1_14.CLICK_WINDOW, Type.FLAT_VAR_INT_ITEM);
        itemRewriter.registerCreativeInvAction(ServerboundPackets1_14.CREATIVE_INVENTORY_ACTION, Type.FLAT_VAR_INT_ITEM);

        protocol.registerIncoming(ServerboundPackets1_14.EDIT_BOOK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)));
            }
        });
    }

    private void handleBlockEntity(CompoundTag tag) {
        StringTag idTag = tag.get("id");
        if (idTag == null) return;

        String id = idTag.getValue();
        if (id.equals("minecraft:conduit")) {
            Tag targetUuidTag = tag.remove("Target");
            if (!(targetUuidTag instanceof IntArrayTag)) return;

            // Target -> target_uuid
            UUID targetUuid = UUIDIntArrayType.uuidFromIntArray((int[]) targetUuidTag.getValue());
            tag.put("target_uuid", new StringTag(targetUuid.toString()));
        } else if (id.equals("minecraft:skull")) {
            Tag skullOwnerTag = tag.remove("SkullOwner");
            if (!(skullOwnerTag instanceof CompoundTag)) return;

            CompoundTag skullOwnerCompoundTag = (CompoundTag) skullOwnerTag;
            Tag ownerUuidTag = skullOwnerCompoundTag.remove("Id");
            if (ownerUuidTag instanceof IntArrayTag) {
                UUID ownerUuid = UUIDIntArrayType.uuidFromIntArray((int[]) ownerUuidTag.getValue());
                skullOwnerCompoundTag.put("Id", new StringTag(ownerUuid.toString()));
            }

            // SkullOwner -> Owner
            CompoundTag ownerTag = new CompoundTag();
            for (Map.Entry<String, Tag> entry : skullOwnerCompoundTag) {
                ownerTag.put(entry.getKey(), entry.getValue());
            }
            tag.put("Owner", ownerTag);
        }
    }

    @Override
    protected void registerRewrites() {
        enchantmentRewriter = new EnchantmentRewriter(this);
        enchantmentRewriter.registerEnchantment("minecraft:soul_speed", "§7Soul Speed");
    }

    @Override
    public Item handleItemToClient(Item item) {
        if (item == null) return null;

        super.handleItemToClient(item);

        CompoundTag tag = item.getTag();
        if (item.getIdentifier() == 771 && tag != null) {
            Tag ownerTag = tag.get("SkullOwner");
            if (ownerTag instanceof CompoundTag) {
                CompoundTag ownerCompundTag = (CompoundTag) ownerTag;
                Tag idTag = ownerCompundTag.get("Id");
                if (idTag instanceof IntArrayTag) {
                    UUID ownerUuid = UUIDIntArrayType.uuidFromIntArray((int[]) idTag.getValue());
                    ownerCompundTag.put("Id", new StringTag(ownerUuid.toString()));
                }
            }
        }

        InventoryPackets.newToOldAttributes(item);
        enchantmentRewriter.handleToClient(item);
        return item;
    }

    @Override
    public Item handleItemToServer(Item item) {
        if (item == null) return null;

        int identifier = item.getIdentifier();
        super.handleItemToServer(item);

        CompoundTag tag = item.getTag();
        if (identifier == 771 && tag != null) {
            Tag ownerTag = tag.get("SkullOwner");
            if (ownerTag instanceof CompoundTag) {
                CompoundTag ownerCompundTag = (CompoundTag) ownerTag;
                Tag idTag = ownerCompundTag.get("Id");
                if (idTag instanceof StringTag) {
                    UUID ownerUuid = UUID.fromString((String) idTag.getValue());
                    ownerCompundTag.put("Id", new IntArrayTag(UUIDIntArrayType.uuidToIntArray(ownerUuid)));
                }
            }
        }

        InventoryPackets.oldToNewAttributes(item);
        enchantmentRewriter.handleToServer(item);
        return item;
    }

    private static final class EquipmentData {
        private final int slot;
        private final Item item;

        private EquipmentData(final int slot, final Item item) {
            this.slot = slot;
            this.item = item;
        }
    }
}
