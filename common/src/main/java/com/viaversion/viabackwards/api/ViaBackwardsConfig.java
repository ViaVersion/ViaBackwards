/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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
package com.viaversion.viabackwards.api;

import com.viaversion.viaversion.api.configuration.Config;

public interface ViaBackwardsConfig extends Config {

    /**
     * Mimics name and level of a custom enchant through the item's lore.
     *
     * @return true if enabled
     */
    boolean addCustomEnchantsToLore();

    /**
     * Writes the color of the scoreboard team after the prefix.
     *
     * @return true if enabled
     */
    boolean addTeamColorTo1_13Prefix();

    /**
     * Converts the new 1.13 face player packets to look packets.
     *
     * @return true if enabled
     */
    boolean isFix1_13FacePlayer();

    /**
     * Converts the 1.13 face look-at packet for 1.12- players. Requires a bit of extra caching.
     *
     * @return true if enabled
     */
    boolean fix1_13FormattedInventoryTitle();

    /**
     * Always shows the original mob's name instead of only when hovering over them with the cursor.
     *
     * @return true if enabled
     */
    boolean alwaysShowOriginalMobName();

    /**
     * Sends inventory acknowledgement packets to act as a replacement for ping packets for sub 1.17 clients.
     * This only takes effect for ids in the short range. Useful for anticheat compatibility.
     *
     * @return true if enabled
     */
    boolean handlePingsAsInvAcknowledgements();

    /**
     * Suppresses warnings of missing emulations for certain features that are not supported (e.g. world height in 1.17+).
     *
     * @return true if enabled
     */
    boolean suppressEmulationWarnings();
}
