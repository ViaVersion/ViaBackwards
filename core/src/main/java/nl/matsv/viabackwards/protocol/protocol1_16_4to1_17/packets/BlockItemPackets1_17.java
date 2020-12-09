package nl.matsv.viabackwards.protocol.protocol1_16_4to1_17.packets;

import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import nl.matsv.viabackwards.protocol.protocol1_16_4to1_17.Protocol1_16_4To1_17;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.BlockChangeRecord;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.rewriters.BlockRewriter;
import us.myles.ViaVersion.api.rewriters.ItemRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.types.Chunk1_16_2Type;
import us.myles.ViaVersion.protocols.protocol1_16to1_15_2.data.RecipeRewriter1_16;
import us.myles.ViaVersion.protocols.protocol1_17to1_16_4.ClientboundPackets1_17;
import us.myles.ViaVersion.protocols.protocol1_17to1_16_4.types.Chunk1_17Type;

import java.util.ArrayList;
import java.util.List;

public class BlockItemPackets1_17 extends nl.matsv.viabackwards.api.rewriters.ItemRewriter<Protocol1_16_4To1_17> {

    public BlockItemPackets1_17(Protocol1_16_4To1_17 protocol, TranslatableRewriter translatableRewriter) {
        super(protocol, translatableRewriter);
    }

    @Override
    protected void registerPackets() {
        ItemRewriter itemRewriter = new ItemRewriter(protocol, this::handleItemToClient, this::handleItemToServer);
        BlockRewriter blockRewriter = new BlockRewriter(protocol, Type.POSITION1_14);

        new RecipeRewriter1_16(protocol, this::handleItemToClient).registerDefaultHandler(ClientboundPackets1_17.DECLARE_RECIPES);

        itemRewriter.registerSetCooldown(ClientboundPackets1_17.COOLDOWN);
        itemRewriter.registerWindowItems(ClientboundPackets1_17.WINDOW_ITEMS, Type.FLAT_VAR_INT_ITEM_ARRAY);
        itemRewriter.registerSetSlot(ClientboundPackets1_17.SET_SLOT, Type.FLAT_VAR_INT_ITEM);
        itemRewriter.registerEntityEquipmentArray(ClientboundPackets1_17.ENTITY_EQUIPMENT, Type.FLAT_VAR_INT_ITEM);
        itemRewriter.registerTradeList(ClientboundPackets1_17.TRADE_LIST, Type.FLAT_VAR_INT_ITEM);
        itemRewriter.registerAdvancements(ClientboundPackets1_17.ADVANCEMENTS, Type.FLAT_VAR_INT_ITEM);

        blockRewriter.registerAcknowledgePlayerDigging(ClientboundPackets1_17.ACKNOWLEDGE_PLAYER_DIGGING);
        blockRewriter.registerBlockAction(ClientboundPackets1_17.BLOCK_ACTION);
        blockRewriter.registerEffect(ClientboundPackets1_17.EFFECT, 1010, 2001);

        itemRewriter.registerClickWindow(ServerboundPackets1_16_2.CLICK_WINDOW, Type.FLAT_VAR_INT_ITEM);
        itemRewriter.registerCreativeInvAction(ServerboundPackets1_16_2.CREATIVE_INVENTORY_ACTION, Type.FLAT_VAR_INT_ITEM);
        protocol.registerIncoming(ServerboundPackets1_16_2.EDIT_BOOK, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM)));
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_17.SPAWN_PARTICLE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Particle id
                map(Type.BOOLEAN); // Long distance
                map(Type.DOUBLE); // X
                map(Type.DOUBLE); // Y
                map(Type.DOUBLE); // Z
                map(Type.FLOAT); // Offset X
                map(Type.FLOAT); // Offset Y
                map(Type.FLOAT); // Offset Z
                map(Type.FLOAT); // Particle data
                map(Type.INT); // Particle count
                handler(wrapper -> {
                    int id = wrapper.get(Type.INT, 0);
                    if (id == 14 || id == 15) {
                        wrapper.write(Type.FLOAT, wrapper.read(Type.DOUBLE).floatValue()); // R
                        wrapper.write(Type.FLOAT, wrapper.read(Type.DOUBLE).floatValue()); // G
                        wrapper.write(Type.FLOAT, wrapper.read(Type.DOUBLE).floatValue()); // B
                        wrapper.passthrough(Type.FLOAT); // Scale

                        if (id == 15) {
                            // Dust color transition -> Dust
                            wrapper.read(Type.DOUBLE); // R
                            wrapper.read(Type.DOUBLE); // G
                            wrapper.read(Type.DOUBLE); // B
                        }
                    } else if (id == 36) {
                        // Vibration signal - no nice mapping possible without tracking entity positions and doing particle tasks
                        wrapper.cancel();
                    }
                });
                handler(itemRewriter.getSpawnParticleHandler(Type.FLAT_VAR_INT_ITEM, Type.DOUBLE));
            }
        });

        // The Great Shrunkening
        // Some chunk sections will be lost ¯\_(ツ)_/¯
        protocol.registerOutgoing(ClientboundPackets1_17.UPDATE_LIGHT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // X
                map(Type.VAR_INT); // Z
                map(Type.BOOLEAN); // Trust edges
                handler(wrapper -> {
                    int skyLightMask = cutLongArrayMask(wrapper.read(Type.LONG_ARRAY_PRIMITIVE));
                    int blockLightMask = cutLongArrayMask(wrapper.read(Type.LONG_ARRAY_PRIMITIVE));
                    wrapper.write(Type.VAR_INT, skyLightMask); // Sky light mask
                    wrapper.write(Type.VAR_INT, blockLightMask); // Block light mask
                    wrapper.write(Type.VAR_INT, cutLongArrayMask(wrapper.read(Type.LONG_ARRAY_PRIMITIVE)));// Empty sky light mask
                    wrapper.write(Type.VAR_INT, cutLongArrayMask(wrapper.read(Type.LONG_ARRAY_PRIMITIVE))); // Empty block light mask

                    writeLightArrays(wrapper, skyLightMask);
                    writeLightArrays(wrapper, blockLightMask);
                });
            }

            private int cutLongArrayMask(long[] mask) {
                if (mask.length == 0) return 0;

                // Only keep the first 18 bits (16 sections + one above and below)
                long l = mask[0];
                return (int) (l & 0x3ffff);
            }

            private void writeLightArrays(PacketWrapper wrapper, int bitMask) throws Exception {
                wrapper.read(Type.VAR_INT); // Length - throw it away

                List<byte[]> light = new ArrayList<>();
                for (int i = 0; i <= 17; i++) {
                    if (isSet(bitMask, i)) {
                        light.add(wrapper.read(Type.BYTE_ARRAY_PRIMITIVE));
                    }
                }

                for (byte[] bytes : light) {
                    wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, bytes);
                }
            }

            private boolean isSet(int mask, int i) {
                return (mask & (1 << i)) != 0;
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_17.MULTI_BLOCK_CHANGE, new PacketRemapper() {
            public void registerMap() {
                map(Type.LONG); // Chunk pos
                map(Type.BOOLEAN); // Suppress light updates
                this.handler((wrapper) -> {
                    // Cancel if above the 256 block limit
                    int chunkY = (int) (wrapper.get(Type.LONG, 0) << 44 >> 44);
                    if (chunkY > 15) {
                        wrapper.cancel();
                        return;
                    }

                    BlockChangeRecord[] records = wrapper.passthrough(Type.VAR_LONG_BLOCK_CHANGE_RECORD_ARRAY);
                    for (BlockChangeRecord record : records) {
                        record.setBlockId(protocol.getMappingData().getNewBlockStateId(record.getBlockId()));
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_17.BLOCK_CHANGE, new PacketRemapper() {
            public void registerMap() {
                map(Type.POSITION1_14);
                map(Type.VAR_INT);
                handler((wrapper) -> {
                    if (wrapper.get(Type.POSITION1_14, 0).getY() >= 256) {
                        wrapper.cancel();
                        return;
                    }

                    wrapper.set(Type.VAR_INT, 0, protocol.getMappingData().getNewBlockStateId(wrapper.get(Type.VAR_INT, 0)));
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_17.CHUNK_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int currentWorldSectionHeight = wrapper.user().get(EntityTracker.class).getCurrentWorldSectionHeight();
                    Chunk chunk = wrapper.read(new Chunk1_17Type(currentWorldSectionHeight));
                    wrapper.write(new Chunk1_16_2Type(), chunk);
                    for (int i = 0; i < 16; i++) { // Only need to process the first 16 sections
                        ChunkSection section = chunk.getSections()[i];
                        if (section == null) continue;
                        for (int j = 0; j < section.getPaletteSize(); j++) {
                            int old = section.getPaletteEntry(j);
                            section.setPaletteEntry(j, protocol.getMappingData().getNewBlockStateId(old));
                        }
                    }
                });
            }
        });
    }
}
