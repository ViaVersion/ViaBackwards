/*
 *
 *     Copyright (C) 2016 Matsv
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.chunks;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang.IllegalClassException;
import org.bukkit.World;
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
            if (world.getEnvironment() == World.Environment.NORMAL) {
                section.readSkyLight(input);
            }
        }

        byte[] biomeData = groundUp ? new byte[256] : null;
        if (groundUp) {
            input.readBytes(biomeData);
        }

        List<CompoundTag> nbtData = Arrays.asList(Type.NBT_ARRAY.read(input));

        return new Chunk1_10(chunkX, chunkZ, groundUp, primaryBitmask, sections, biomeData, nbtData);
    }

    @Override
    public void write(ByteBuf output, ClientWorld world, Chunk input) throws Exception {
        if (!(input instanceof Chunk1_10))
            throw new IllegalClassException("Tried to send the wrong chunk type from 1.9.3-4 chunk: " + input.getClass());
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
