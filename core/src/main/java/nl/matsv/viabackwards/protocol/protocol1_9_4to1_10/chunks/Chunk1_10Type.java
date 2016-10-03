/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.chunks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import nl.matsv.viabackwards.ViaBackwards;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.minecraft.Environment;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.ViaVersion.api.minecraft.chunks.ChunkSection;
import us.myles.ViaVersion.api.type.PartialType;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.api.type.types.minecraft.BaseChunkType;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class Chunk1_10Type extends PartialType<Chunk, ClientWorld> {

    public Chunk1_10Type(ClientWorld param) {
        super(param, Chunk.class);
    }

    @Override
    public Chunk read(ByteBuf input, ClientWorld world) throws Exception {
        int chunkX = input.readInt();
        int chunkZ = input.readInt();

        boolean groundUp = input.readBoolean();
        int primaryBitmask = Type.VAR_INT.read(input);
        Type.VAR_INT.read(input);

        BitSet usedSections = new BitSet(16);
        ChunkSection1_10[] sections = new ChunkSection1_10[16];
        // Calculate section count from bitmask
        for (int i = 0; i < 16; i++) {
            if ((primaryBitmask & (1 << i)) != 0) {
                usedSections.set(i);
            }
        }

        // Read sections
        for (int i = 0; i < 16; i++) {
            if (!usedSections.get(i)) continue; // Section not set
            ChunkSection1_10 section = new ChunkSection1_10();
            sections[i] = section;
            section.readBlocks(input);
            section.readBlockLight(input);
            if (world.getEnvironment() == Environment.NORMAL) {
                section.readSkyLight(input);
            }
        }

        byte[] biomeData = groundUp ? new byte[256] : null;
        if (groundUp) {
            input.readBytes(biomeData);
        }

        List<CompoundTag> nbtData = Arrays.asList(Type.NBT_ARRAY.read(input));

        // Temp patch for plugins that sent wrong too big chunks TODO find the issue in LibsDisguise and PR it.
        if (input.readableBytes() > 0) {
            byte[] array = new byte[input.readableBytes()];
            input.readBytes(array);
            if (Via.getManager().isDebug())
                ViaBackwards.getPlatform().getLogger().warning("Found " + array.length + " more bytes than expected while reading the chunk");
        }

        return new Chunk1_10(chunkX, chunkZ, groundUp, primaryBitmask, sections, biomeData, nbtData);
    }

    @Override
    public void write(ByteBuf output, ClientWorld world, Chunk input) throws Exception {
        if (!(input instanceof Chunk1_10))
            throw new Exception("Tried to send the wrong chunk type from 1.9.3-4 chunk: " + input.getClass());
        Chunk1_10 chunk = (Chunk1_10) input;

        output.writeInt(chunk.getX());
        output.writeInt(chunk.getZ());

        output.writeBoolean(chunk.isGroundUp());
        Type.VAR_INT.write(output, chunk.getBitmask());

        ByteBuf buf = Unpooled.buffer();
        for (int i = 0; i < 16; i++) {
            ChunkSection section = chunk.getSections()[i];
            if (section == null) continue; // Section not set
            section.writeBlocks(buf);
            section.writeBlockLight(buf);

            if (!section.hasSkyLight()) continue; // No sky light, we're done here.
            section.writeSkyLight(buf);

        }
        buf.readerIndex(0);
        Type.VAR_INT.write(output, buf.readableBytes() + (chunk.isBiomeData() ? 256 : 0));
        output.writeBytes(buf);
        buf.release(); // release buffer

        // Write biome data
        if (chunk.isBiomeData()) {
            output.writeBytes(chunk.getBiomeData());
        }

        Type.NBT_ARRAY.write(output, chunk.getBlockEntities().toArray(new CompoundTag[0]));
    }

    @Override
    public Class<? extends Type> getBaseClass() {
        return BaseChunkType.class;
    }
}
