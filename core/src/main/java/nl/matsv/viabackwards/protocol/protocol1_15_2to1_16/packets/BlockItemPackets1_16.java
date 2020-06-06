package nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.EnchantmentRewriter;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.data.RecipeRewriter1_16;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.BlockRewriter;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.UUIDIntArrayType;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.ServerboundPackets1_14;
import us.myles.ViaVersion.protocols.protocol1_15to1_14_4.types.Chunk1_15Type;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.ClientboundPackets1_16;
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

    public BlockItemPackets1_16(Protocol1_15_2To1_16 protocol, TranslatableRewriter translatableRewriter) {
        super(protocol, translatableRewriter,
                BlockItemPackets1_16::getOldItemId, BlockItemPackets1_16::getNewItemId, id -> BackwardsMappings.itemMappings.getMappedItem(id));
    }

    @Override
    protected void registerPackets() {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);
        BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION1_14, Protocol1_15_2To1_16::getNewBlockStateId, Protocol1_15_2To1_16::getNewBlockId);

        new RecipeRewriter1_16(this).register(ClientboundPackets1_16.DECLARE_RECIPES);

        itemRewriter.registerSetCooldown(ClientboundPackets1_16.COOLDOWN, BlockItemPackets1_16::getOldItemId);
        itemRewriter.registerWindowItems(ClientboundPackets1_16.WINDOW_ITEMS, Type.FLAT_VAR_INT_ITEM_ARRAY);
        itemRewriter.registerSetSlot(ClientboundPackets1_16.SET_SLOT, Type.FLAT_VAR_INT_ITEM);

        protocol.registerOutgoing(ClientboundPackets1_16.TRADE_LIST, new PacketRemapper() {
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

        itemRewriter.registerEntityEquipment(ClientboundPackets1_16.ENTITY_EQUIPMENT, Type.FLAT_VAR_INT_ITEM);
        blockRewriter.registerAcknowledgePlayerDigging(ClientboundPackets1_16.ACKNOWLEDGE_PLAYER_DIGGING);
        blockRewriter.registerBlockAction(ClientboundPackets1_16.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_16.BLOCK_CHANGE);
        blockRewriter.registerMultiBlockChange(ClientboundPackets1_16.MULTI_BLOCK_CHANGE);

        protocol.registerOutgoing(ClientboundPackets1_16.CHUNK_DATA, new PacketRemapper() {
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

        blockRewriter.registerEffect(ClientboundPackets1_16.EFFECT, 1010, 2001, BlockItemPackets1_16::getOldItemId);
        blockRewriter.registerSpawnParticle(ClientboundPackets1_16.SPAWN_PARTICLE, 3, 23, 32,
                BlockItemPackets1_16::getNewParticleId, this::handleItemToClient, Type.FLAT_VAR_INT_ITEM, Type.DOUBLE);

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

        itemRewriter.registerClickWindow(ServerboundPackets1_14.CLICK_WINDOW, Type.FLAT_VAR_INT_ITEM);
        itemRewriter.registerCreativeInvAction(ServerboundPackets1_14.CREATIVE_INVENTORY_ACTION, Type.FLAT_VAR_INT_ITEM);

        protocol.registerIncoming(ServerboundPackets1_14.EDIT_BOOK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)));
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
