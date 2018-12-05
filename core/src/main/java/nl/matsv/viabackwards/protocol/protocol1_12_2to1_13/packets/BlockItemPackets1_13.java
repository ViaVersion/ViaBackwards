/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import nl.matsv.viabackwards.api.rewriters.BlockItemRewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.BackwardsMappings;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.types.Chunk1_13Type;
import us.myles.ViaVersion.protocols.protocol1_9_1_2to1_9_3_4.types.Chunk1_9_3_4Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class BlockItemPackets1_13 extends BlockItemRewriter<Protocol1_12_2To1_13> {
    @Override
    protected void registerPackets(Protocol1_12_2To1_13 protocol) {

        // Chunk packet
        protocol.out(State.PLAY, 0x22, 0x20, new PacketRemapper() {
                    @Override
                    public void registerMap() {
                        handler(new PacketHandler() {
                            @Override
                            public void handle(PacketWrapper wrapper) throws Exception {
                                ClientWorld clientWorld = wrapper.user().get(ClientWorld.class);

                                Chunk1_9_3_4Type type_old = new Chunk1_9_3_4Type(clientWorld);
                                Chunk1_13Type type = new Chunk1_13Type(clientWorld);
                                Chunk chunk = wrapper.read(type);

                                for (int i = 0; i < chunk.getSections().length; i++) {
                                    ChunkSection section = chunk.getSections()[i];
                                    if (section == null) {
                                        continue;
                                    }

                                    for (int p = 0; p < section.getPaletteSize(); p++) {
                                        int old = section.getPaletteEntry(p);
                                        if (old != 0) {
                                            section.setPaletteEntry(p, toNewId(old));
                                        }
                                    }
                                }

                                // Rewrite biome id 255 to plains
                                if (chunk.isBiomeData()) {
                                    for (int i = 0; i < 256; i++) {
                                        chunk.getBiomeData()[i] = 1; // Plains
                                    }
                                }

                                chunk.getBlockEntities().clear();
                                wrapper.write(type_old, chunk);
                            }
                        });
                    }
                }
        );
    }

    public static int toNewId(int oldId) {
        if (oldId < 0) {
            oldId = 0; // Some plugins use negative numbers to clear blocks, remap them to air.
        }
        int newId = BackwardsMappings.blockMappings.getNewBlock(oldId);
        if (newId != -1)
            return newId;

        Via.getPlatform().getLogger().warning("Missing block completely " + oldId);
        // Default stone
        return 1 << 4;
    }

    @Override
    protected void registerRewrites() {

    }
}
