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
import java.util.Arrays;
import java.util.BitSet;
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
                    if (id == 15) {
                        wrapper.passthrough(Type.FLOAT); // R
                        wrapper.passthrough(Type.FLOAT); // G
                        wrapper.passthrough(Type.FLOAT); // B
                        wrapper.passthrough(Type.FLOAT); // Scale

                        // Dust color transition -> Dust
                        wrapper.read(Type.FLOAT); // R
                        wrapper.read(Type.FLOAT); // G
                        wrapper.read(Type.FLOAT); // B
                    } else if (id == 36) {
                        // Vibration signal - no nice mapping possible without tracking entity positions and doing particle tasks
                        wrapper.cancel();
                    }
                });
                handler(itemRewriter.getSpawnParticleHandler(Type.FLAT_VAR_INT_ITEM));
            }
        });

        // The Great Shrunkening
        // Chunk sections *will* be lost ¯\_(ツ)_/¯
        protocol.registerOutgoing(ClientboundPackets1_17.UPDATE_LIGHT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // X
                map(Type.VAR_INT); // Z
                map(Type.BOOLEAN); // Trust edges
                handler(wrapper -> {
                    EntityTracker tracker = wrapper.user().get(EntityTracker.class);
                    int startFromSection = Math.max(0, -(tracker.getCurrentMinY() >> 4));

                    long[] skyLightMask = wrapper.read(Type.LONG_ARRAY_PRIMITIVE);
                    long[] blockLightMask = wrapper.read(Type.LONG_ARRAY_PRIMITIVE);
                    int cutSkyLightMask = cutLightMask(skyLightMask, startFromSection);
                    int cutBlockLightMask = cutLightMask(blockLightMask, startFromSection);
                    wrapper.write(Type.VAR_INT, cutSkyLightMask);
                    wrapper.write(Type.VAR_INT, cutBlockLightMask);

                    long[] emptySkyLightMask = wrapper.read(Type.LONG_ARRAY_PRIMITIVE);
                    long[] emptyBlockLightMask = wrapper.read(Type.LONG_ARRAY_PRIMITIVE);
                    wrapper.write(Type.VAR_INT, cutLightMask(emptySkyLightMask, startFromSection));
                    wrapper.write(Type.VAR_INT, cutLightMask(emptyBlockLightMask, startFromSection));

                    writeLightArrays(wrapper, BitSet.valueOf(skyLightMask), cutSkyLightMask, startFromSection, tracker.getCurrentWorldSectionHeight());
                    writeLightArrays(wrapper, BitSet.valueOf(blockLightMask), cutBlockLightMask, startFromSection, tracker.getCurrentWorldSectionHeight());
                });
            }

            private void writeLightArrays(PacketWrapper wrapper, BitSet bitMask, int cutBitMask,
                                          int startFromSection, int sectionHeight) throws Exception {
                wrapper.read(Type.VAR_INT); // Length - throw it away

                List<byte[]> light = new ArrayList<>();

                // Remove lower bounds
                for (int i = 0; i < startFromSection; i++) {
                    if (bitMask.get(i)) {
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE);
                    }
                }

                // Add the important 18 sections
                for (int i = 0; i < 18; i++) {
                    if (isSet(cutBitMask, i)) {
                        light.add(wrapper.read(Type.BYTE_ARRAY_PRIMITIVE));
                    }
                }

                // Remove upper bounds
                for (int i = startFromSection + 18; i < sectionHeight + 2; i++) {
                    if (bitMask.get(i)) {
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE);
                    }
                }

                // Aaand we're done
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
                handler((wrapper) -> {
                    // Remove sections below y 0 and above 255
                    long chunkPos = wrapper.get(Type.LONG, 0);
                    int chunkY = (int) (chunkPos << 44 >> 44);
                    if (chunkY < 0 || chunkY > 15) {
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
                    int y = wrapper.get(Type.POSITION1_14, 0).getY();
                    if (y < 0 || y > 255) {
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
                    EntityTracker tracker = wrapper.user().get(EntityTracker.class);
                    int currentWorldSectionHeight = tracker.getCurrentWorldSectionHeight();

                    Chunk chunk = wrapper.read(new Chunk1_17Type(currentWorldSectionHeight));
                    wrapper.write(new Chunk1_16_2Type(), chunk);

                    // Cut sections
                    int startFromSection = Math.max(0, -(tracker.getCurrentMinY() >> 4));
                    chunk.setBiomeData(Arrays.copyOfRange(chunk.getBiomeData(), startFromSection * 64, (startFromSection * 64) + 1024));

                    chunk.setBitmask(cutMask(chunk.getChunkMask(), startFromSection, false));
                    chunk.setChunkMask(null);

                    ChunkSection[] sections = Arrays.copyOfRange(chunk.getSections(), startFromSection, startFromSection + 16);
                    chunk.setSections(sections);

                    for (int i = 0; i < 16; i++) {
                        ChunkSection section = sections[i];
                        if (section == null) continue;
                        for (int j = 0; j < section.getPaletteSize(); j++) {
                            int old = section.getPaletteEntry(j);
                            section.setPaletteEntry(j, protocol.getMappingData().getNewBlockStateId(old));
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_17.BLOCK_ENTITY_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int y = wrapper.passthrough(Type.POSITION1_14).getY();
                    if (y < 0 || y > 255) {
                        wrapper.cancel();
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_17.BLOCK_BREAK_ANIMATION, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                handler(wrapper -> {
                    int y = wrapper.passthrough(Type.POSITION1_14).getY();
                    if (y < 0 || y > 255) {
                        wrapper.cancel();
                    }
                });
            }
        });
    }

    private int cutLightMask(long[] mask, int startFromSection) {
        if (mask.length == 0) return 0;
        return cutMask(BitSet.valueOf(mask), startFromSection, true);
    }

    private int cutMask(BitSet mask, int startFromSection, boolean lightMask) {
        int cutMask = 0;
        // Light masks have a section below and above the 16 main sections
        int to = startFromSection + (lightMask ? 18 : 16);
        for (int i = startFromSection, j = 0; i < to; i++, j++) {
            if (mask.get(i)) {
                cutMask |= (1 << j);
            }
        }
        return cutMask;
    }
}
