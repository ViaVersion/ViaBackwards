/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_19to1_18_2.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.DataPaletteImpl;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockAckStorage implements StorableObject {

    private final Int2ObjectLinkedOpenHashMap<BlockAction> actions = new Int2ObjectLinkedOpenHashMap<>();
    private final Map<Long, DataPalette[]> cachedChunks = new HashMap<>();
    private int sequenceId;

    public int nextSequence() {
        return sequenceId++;
    }

    public void add(final int sequence, final BlockPosition position, final int action) {
        actions.put(sequence, new BlockAction(position, (byte) action));

        // Some actions may be left unacknowledged by modded servers
        if (actions.size() > 100) {
            actions.removeFirst();
        }
    }

    public @Nullable BlockAction remove(final int sequence) {
        return actions.remove(sequence);
    }

    public void cacheChunk(final int chunkX, final int chunkZ, final ChunkSection[] sections) {
        final DataPalette[] blockPalettes = new DataPalette[sections.length];
        for (int i = 0; i < sections.length; i++) {
            final ChunkSection section = sections[i];
            if (section == null) {
                continue;
            }

            final DataPalette palette = section.palette(PaletteType.BLOCKS);
            if (palette != null) {
                blockPalettes[i] = copyPalette(palette);
            }
        }
        cachedChunks.put(packChunk(chunkX, chunkZ), blockPalettes);
    }

    public void forgetChunk(final int chunkX, final int chunkZ) {
        cachedChunks.remove(packChunk(chunkX, chunkZ));
    }

    public void clear() {
        actions.clear();
        cachedChunks.clear();
        sequenceId = 0;
    }

    public void updateBlockState(final int blockX, final int blockY, final int blockZ, final int minSectionY, final int state) {
        final DataPalette[] sections = cachedChunks.get(packChunk(blockX >> 4, blockZ >> 4));
        if (sections == null) {
            return;
        }
        final int sectionIdx = (blockY >> 4) - minSectionY;
        if (sectionIdx < 0 || sectionIdx >= sections.length) {
            return;
        }
        final DataPalette palette = sections[sectionIdx];
        if (palette == null) {
            return;
        }
        palette.setIdAt(blockX & 15, blockY & 15, blockZ & 15, state);
    }

    public int getBlockStateAt(final BlockPosition pos, final int minSectionY) {
        final DataPalette[] sections = cachedChunks.get(packChunk(pos.x() >> 4, pos.z() >> 4));
        if (sections == null) {
            return -1;
        }
        final int sectionIdx = (pos.y() >> 4) - minSectionY;
        if (sectionIdx < 0 || sectionIdx >= sections.length) {
            return -1;
        }
        final DataPalette palette = sections[sectionIdx];
        if (palette == null) {
            return -1;
        }
        return palette.idAt(pos.x() & 15, pos.y() & 15, pos.z() & 15);
    }

    private static DataPalette copyPalette(final DataPalette palette) {
        final DataPaletteImpl copy = new DataPaletteImpl(ChunkSection.SIZE, palette.size());
        for (int i = 0; i < palette.size(); i++) {
            copy.addId(palette.idByIndex(i));
        }
        for (int i = 0; i < ChunkSection.SIZE; i++) {
            copy.setPaletteIndexAt(i, palette.paletteIndexAt(i));
        }
        return copy;
    }

    private static long packChunk(final int x, final int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    public record BlockAction(BlockPosition position, byte action) {}
}
