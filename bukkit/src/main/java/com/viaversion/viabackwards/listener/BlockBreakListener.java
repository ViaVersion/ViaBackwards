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
package com.viaversion.viabackwards.listener;

import com.viaversion.viabackwards.BukkitPlugin;
import com.viaversion.viabackwards.protocol.v1_11to1_10.Protocol1_11To1_10;
import com.viaversion.viaversion.bukkit.listeners.ViaBukkitListener;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class BlockBreakListener extends ViaBukkitListener {

    public BlockBreakListener(final BukkitPlugin plugin) {
        super(plugin, Protocol1_11To1_10.class);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (!event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || !isOnPipe(player)) {
            return;
        }

        // Resend the item in the hand to sync durability
        final int slot = player.getInventory().getHeldItemSlot();
        final ItemStack item = player.getInventory().getItem(slot);
        if (item != null && item.getType().getMaxDurability() > 0) {
            player.getInventory().setItem(slot, item);
        }
    }
}
