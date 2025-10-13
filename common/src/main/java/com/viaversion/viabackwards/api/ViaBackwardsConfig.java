/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
     * Adds bedrock at y=0 for sub 1.17 clients.
     *
     * @return true if enabled
     */
    boolean bedrockAtY0();

    /**
     * Shows sculk shriekers as crying obsidian for 1.18.2 clients on 1.19+ servers. This fixes collision and block breaking issues.
     *
     * @return true if enabled
     */
    boolean sculkShriekerToCryingObsidian();

    /**
     * Shows scaffolding as water for 1.13.2 clients on 1.14+ servers. This fixes collision issues.
     *
     * @return true if enabled
     */
    boolean scaffoldingToWater();

    /**
     * Maps the darkness effect to blindness for 1.18.2 clients on 1.19+ servers.
     *
     * @return true if enabled
     */
    boolean mapDarknessEffect();

    /**
     * If enabled, 1.21.3 clients will receive the first float of 1.21.4+ custom model data as int. Disable if you handle this change yourself.
     *
     * @return true if enabled
     */
    boolean mapCustomModelData();

    /**
     * If enabled, 1.19.3 clients will receive display entities as armor stands with custom entity data on 1.19.4+ servers.
     *
     * @return true if enabled
     */
    boolean mapDisplayEntities();

    /**
     * Suppresses warnings of missing emulations for certain features that are not supported (e.g. world height in 1.17+).
     *
     * @return true if enabled
     */
    boolean suppressEmulationWarnings();

    /**
     * If enabled, dialogs will be shown via chest inventories for 1.21.5 clients on 1.21.6+ servers.
     *
     * @return true if enabled
     */
    boolean dialogsViaChests();

    /**
     * Returns the dialog style configuration.
     *
     * @return the dialog style configuration
     */
    DialogStyleConfig dialogStyleConfig();
}
