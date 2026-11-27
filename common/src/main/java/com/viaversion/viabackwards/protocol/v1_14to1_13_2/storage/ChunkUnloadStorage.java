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
package com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viaversion.libs.fastutil.longs.LongOpenHashSet;
import com.viaversion.viaversion.libs.fastutil.longs.LongSet;
import com.viaversion.viaversion.util.BiIntConsumer;

public class ChunkUnloadStorage {
  private final LongSet toUnload = new LongOpenHashSet();
  private boolean justAcceptedTeleport;

  public void unloadChunk(int x, int z) {
    toUnload.add(ChunkLightStorage.getChunkSectionIndex(x, z));
  }

  public boolean popUnload(int x, int z) {
    if (!ViaBackwards.getConfig().queue1_13ChunkUnloads()) {
      return false;
    }
    return toUnload.remove(ChunkLightStorage.getChunkSectionIndex(x, z));
  }

  public void setJustAcceptedTeleport() {
    this.justAcceptedTeleport = true;
  }

  public void unloadAll(BiIntConsumer consumer) {
    if (!ViaBackwards.getConfig().queue1_13ChunkUnloads()) {
      return;
    }
    if (justAcceptedTeleport) {
      // 1.9+ clients send a position packet immediately after teleport, we must not unload chunks yet
      justAcceptedTeleport = false;
      return;
    }
    toUnload.removeIf(l -> {
      int x = (int) ((l >> 38) & 0x3FFFFFFL);
      int z = (int) (l << 38 >> 38);
      consumer.consume(x, z);
      return true;
    });
  }
}
