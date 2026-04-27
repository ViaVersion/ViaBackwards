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
package com.viaversion.viabackwards.protocol.v26_1to1_21_11.storage;

import com.viaversion.viaversion.api.connection.StorableObject;

public final class DayTimeStorage implements StorableObject {

    private long gameTime;
    private long dayTime;
    private boolean advanceTime;

    public long gameTime() {
        return gameTime;
    }

    public void setGameTime(final long gameTime) {
        this.gameTime = gameTime;
    }

    public long setGameTimeAndUpdateDayTime(final long gameTime) {
        if (advanceTime) {
            this.dayTime += gameTime - this.gameTime;
        }
        this.gameTime = gameTime;
        return dayTime;
    }

    public long dayTime() {
        return dayTime;
    }

    public void setDayTime(final long dayTime) {
        this.dayTime = dayTime;
    }

    public boolean advanceTime() {
        return advanceTime;
    }

    public void setAdvanceTime(final boolean advanceTime) {
        this.advanceTime = advanceTime;
    }
}
