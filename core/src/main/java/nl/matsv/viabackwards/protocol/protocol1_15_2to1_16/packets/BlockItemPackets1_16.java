package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.EnchantmentRewriter;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.data.RecipeRewriter1_15;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data.BackwardsMappings;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.BlockRewriter;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.UUIDIntArrayType;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.types.Chunk1_15Type;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.data.MappingData;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.types.Chunk1_16Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.ViaVersion.util.CompactArrayUtil;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.LongArrayTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.StringTag;
import us.myles.viaversion.libs.opennbt.tag.builtin.Tag;

import java.util.UUID;

public class BlockItemPackets1_16 extends nl.matsv.viabackwards.api.rewriters.ItemRewriter<Protocol1_15_2To1_16> {

    private EnchantmentRewriter enchantmentRewriter;

    public BlockItemPackets1_16(Protocol1_15_2To1_16 protocol) {
        super(protocol, BlockItemPackets1_16::getOldItemId, BlockItemPackets1_16::getNewItemId, id -> BackwardsMappings.itemMappings.getMappedItem(id));
    }

    @Override
    protected void registerPackets() {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);
        BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION1_14, Protocol1_15_2To1_16::getNewBlockStateId, Protocol1_15_2To1_16::getNewBlockId);

        // Declare Recipes
        new RecipeRewriter1_15(this).registerDefaultHandler(0x5B, 0x5B);

        // Edit Book
        protocol.registerIncoming(State.PLAY, 0x0C, 0x0C, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)));
            }
        });

        // Set cooldown
        itemRewriter.registerSetCooldown(0x18, 0x18, BlockItemPackets1_16::getOldItemId);

        // Window items packet
        itemRewriter.registerWindowItems(Type.FLAT_VAR_INT_ITEM_ARRAY, 0x15, 0x15);

        // Set slot packet
        itemRewriter.registerSetSlot(Type.FLAT_VAR_INT_ITEM, 0x17, 0x17);

        // Trade list
        protocol.out(State.PLAY, 0x28, 0x28, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.passthrough(Type.VAR_INT);
                    int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                    for (int i = 0; i < size; i++) {
                        Item input = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                        handleItemToClient(input);

                        Item output = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                        handleItemToClient(output);

                        if (wrapper.passthrough(Type.BOOLEAN)) { // Has second item
                            // Second Item
                            Item second = wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                            handleItemToClient(second);
                        }

                        wrapper.passthrough(Type.BOOLEAN);
                        wrapper.passthrough(Type.INT);
                        wrapper.passthrough(Type.INT);

                        wrapper.passthrough(Type.INT);
                        wrapper.passthrough(Type.INT);
                        wrapper.passthrough(Type.FLOAT);
                        wrapper.passthrough(Type.INT);
                    }

                    wrapper.passthrough(Type.VAR_INT);
                    wrapper.passthrough(Type.VAR_INT);
                    wrapper.passthrough(Type.BOOLEAN);
                });
            }
        });

        // Entity Equipment Packet
        itemRewriter.registerEntityEquipment(Type.FLAT_VAR_INT_ITEM, 0x48, 0x47);

        // Click window packet
        itemRewriter.registerClickWindow(Type.FLAT_VAR_INT_ITEM, 0x09, 0x09);

        // Creative Inventory Action
        itemRewriter.registerCreativeInvAction(Type.FLAT_VAR_INT_ITEM, 0x27, 0x26);

        // Acknowledge player digging
        blockRewriter.registerAcknowledgePlayerDigging(0x08, 0x08);

        // Block Action
        blockRewriter.registerBlockAction(0x0B, 0x0B);

        // Block Change
        blockRewriter.registerBlockChange(0x0C, 0x0C);

        // Multi Block Change
        blockRewriter.registerMultiBlockChange(0x10, 0x10);

        // Chunk
        protocol.registerOutgoing(State.PLAY, 0x22, 0x22, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);
                    Chunk chunk = wrapper.read(new Chunk1_16Type(clientWorld));
                    wrapper.write(new Chunk1_15Type(clientWorld), chunk);

                    for (int i = 0; i < chunk.getSections().length; i++) {
                        ChunkSection section = chunk.getSections()[i];
                        if (section == null) continue;
                        for (int j = 0; j < section.getPaletteSize(); j++) {
                            int old = section.getPaletteEntry(j);
                            section.setPaletteEntry(j, Protocol1_15_2To1_16.getNewBlockStateId(old));
                        }
                    }

                    CompoundTag heightMaps = chunk.getHeightMap();
                    for (Tag heightMapTag : heightMaps) {
                        LongArrayTag heightMap = (LongArrayTag) heightMapTag;
                        int[] heightMapData = new int[256];
                        CompactArrayUtil.iterateCompactArrayWithPadding(9, heightMapData.length, heightMap.getValue(), (i, v) -> heightMapData[i] = v);
                        heightMap.setValue(CompactArrayUtil.createCompactArray(9, heightMapData.length, i -> heightMapData[i]));
                    }

                    if (chunk.isBiomeData()) {
                        for (int i = 0; i < 1024; i++) {
                            int biome = chunk.getBiomeData()[i];
                            int newId = -1;
                            switch (biome) {
                                case 170: // new nether biomes
                                case 171:
                                case 172:
                                case 173:
                                    newId = 8;
                                    break;
                            }

                            if (newId != -1) {
                                chunk.getBiomeData()[i] = newId;
                            }
                        }
                    }

                    if (chunk.getBlockEntities() == null) return;
                    for (CompoundTag blockEntity : chunk.getBlockEntities()) {
                        String id = ((StringTag) blockEntity.get("id")).getValue();
                        if (id.equals("minecraft:conduit")) {
                            IntArrayTag targetUuidTag = blockEntity.remove("Target");
                            if (targetUuidTag == null) continue;

                            // Target -> target_uuid
                            UUID targetUuid = UUIDIntArrayType.uuidFromIntArray(targetUuidTag.getValue());
                            blockEntity.put(new StringTag("target_uuid", targetUuid.toString()));
                        } else if (id.equals("minecraft:skull") && blockEntity.get("SkullOwner") instanceof CompoundTag) {
                            CompoundTag skullOwnerTag = blockEntity.remove("SkullOwner");
                            IntArrayTag ownerUuidTag = skullOwnerTag.remove("Id");
                            if (ownerUuidTag != null) {
                                UUID ownerUuid = UUIDIntArrayType.uuidFromIntArray(ownerUuidTag.getValue());
                                skullOwnerTag.put(new StringTag("Id", ownerUuid.toString()));
                            }

                            // SkullOwner -> Owner
                            CompoundTag ownerTag = new CompoundTag("Owner");
                            for (Tag tag : skullOwnerTag) {
                                ownerTag.put(tag);
                            }
                            blockEntity.put(ownerTag);
                        }
                    }
                });
            }
        });

        // Effect packet
        blockRewriter.registerEffect(0x23, 0x23, 1010, 2001, BlockItemPackets1_16::getOldItemId);

        // Spawn particle
        blockRewriter.registerSpawnParticle(Type.DOUBLE, 0x24, 0x24, 3, 23, 32,
                BlockItemPackets1_16::getNewParticleId, this::handleItemToClient, Type.FLAT_VAR_INT_ITEM);

        // Window Property
        protocol.registerOutgoing(State.PLAY, 0x16, 0x16, new PacketRemapper() {
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
    }

    public static int getNewParticleId(int id) {
        switch (id) {
            case 27: // soul flame -> flame
                return 26;
            case 28: // soul -> smoke
                return 42;
            case 64: // ash, crimson spore, warped spore -> mycelium
            case 65:
            case 66:
                return 37;
            case 67: // dripping obsidian tear -> dripping lava
                return 9;
            case 68: // falling obsidian tear
                return 10;
            case 69: // landing obsidian tear
                return 11;
            case 70: // reversed portal -> portal
                return 40;
        }
        if (id > 27) {
            id -= 2;
        }
        return id;
    }

    @Override
    protected void registerRewrites() {
        enchantmentRewriter = new EnchantmentRewriter(nbtTagName);
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
                    ownerCompundTag.put(new StringTag("Id", ownerUuid.toString()));
                }
            }
        }

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
                    ownerCompundTag.put(new IntArrayTag("Id", UUIDIntArrayType.uuidToIntArray(ownerUuid)));
                }
            }
        }

        enchantmentRewriter.handleToServer(item);
        return item;
    }

    public static int getNewItemId(int id) {
        Integer newId = MappingData.oldToNewItems.get(id);
        if (newId == null) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.16 item for 1.15 item " + id);
            return 1;
        }
        return newId;
    }

    public static int getOldItemId(int id) {
        Integer oldId = MappingData.oldToNewItems.inverse().get(id);
        if (oldId == null) {
            ViaBackwards.getPlatform().getLogger().warning("Missing 1.15 item for 1.16 item " + id);
            return 1;
        }
        return oldId;
    }
}
