/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2021 ViaVersion and contributors
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

package nl.matsv.viabackwards;

import nl.matsv.viabackwards.api.ViaBackwardsPlatform;
import nl.matsv.viabackwards.listener.FireDamageListener;
import nl.matsv.viabackwards.listener.FireExtinguishListener;
import nl.matsv.viabackwards.listener.LecternInteractListener;
import org.bukkit.plugin.java.JavaPlugin;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;
import us.myles.ViaVersion.bukkit.platform.BukkitViaLoader;

public class BukkitPlugin extends JavaPlugin implements ViaBackwardsPlatform {

    @Override
    public void onEnable() {
        init(getDataFolder());
        Via.getPlatform().runSync(this::onServerLoaded);
    }

    private void onServerLoaded() {
        BukkitViaLoader loader = (BukkitViaLoader) Via.getManager().getLoader();
        if (Via.getAPI().getServerVersion() >= ProtocolVersion.v1_16.getVersion()) {
            loader.storeListener(new FireExtinguishListener(this)).register();
        }
        if (Via.getAPI().getServerVersion() >= ProtocolVersion.v1_14.getVersion()) {
            loader.storeListener(new LecternInteractListener(this)).register();
        }
        if (Via.getAPI().getServerVersion() >= ProtocolVersion.v1_12.getVersion()) {
            loader.storeListener(new FireDamageListener(this)).register();
        }
    }

    @Override
    public void disable() {
        getPluginLoader().disablePlugin(this);
    }
}
