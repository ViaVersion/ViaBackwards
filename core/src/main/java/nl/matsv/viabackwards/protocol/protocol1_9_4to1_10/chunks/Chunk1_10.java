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

import lombok.AllArgsConstructor;
import lombok.Data;
import us.myles.ViaVersion.api.minecraft.chunks.Chunk;
import us.myles.viaversion.libs.opennbt.tag.builtin.CompoundTag;

import java.util.List;

@Data
@AllArgsConstructor
public class Chunk1_10 implements Chunk {
    private int x;
    private int z;
    private boolean groundUp;
    private int bitmask;
    private ChunkSection1_10[] sections;
    private byte[] biomeData;
    private List<CompoundTag> blockEntities;

    @Override
    public boolean isBiomeData() {
        return biomeData != null;
    }
}