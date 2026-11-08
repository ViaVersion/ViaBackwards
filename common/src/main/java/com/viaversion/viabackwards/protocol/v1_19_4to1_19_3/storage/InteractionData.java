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
package com.viaversion.viabackwards.protocol.v1_19_4to1_19_3.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.util.MathUtil;

public class InteractionData implements StorableObject {
  private static final double SLIME_DIMENSIONS = 0.52;

  private float width;
  private float height;

  private int slimeSize;

  public InteractionData() {
    this.width = 1.0F;
    this.height = 1.0F;
    calculateSize();
  }

  public int update(final float dimension, final boolean isWidth) {
    if (isWidth) {
      this.width = dimension;
    } else {
      this.height = dimension;
    }
    calculateSize();
    return slimeSize;
  }

  private void calculateSize() {
    slimeSize = MathUtil.clamp((int) Math.round(Math.min(width, height) / SLIME_DIMENSIONS), 1, 127);
  }
}
