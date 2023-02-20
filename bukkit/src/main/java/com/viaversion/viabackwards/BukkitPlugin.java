/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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

package com.viaversion.viabackwards;

import com.viaversion.viabackwards.api.ViaBackwardsPlatform;
import com.viaversion.viabackwards.listener.FireDamageListener;
import com.viaversion.viabackwards.listener.FireExtinguishListener;
import com.viaversion.viabackwards.listener.LecternInteractListener;
import com.viaversion.viaversion.ViaVersionPlugin;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.bukkit.platform.BukkitViaLoader;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitPlugin extends JavaPlugin implements ViaBackwardsPlatform {

    public BukkitPlugin() {
        // This is the only place we can enable this before ViaVersion is loaded and registers protocols
        MappingDataLoader.enableMappingsCache();
    }

    @Override
    public void onLoad() {
        if (!ViaVersionPlugin.getInstance().isLateBind()) {
            init();
        }
    }

    @Override
    public void onEnable() {
        if (ViaVersionPlugin.getInstance().isLateBind()) {
            init();
        }
    }

    private void init() {
        init(getDataFolder());
        Via.getPlatform().runSync(this::onServerLoaded);
    }

    private void onServerLoaded() {
        BukkitViaLoader loader = (BukkitViaLoader) Via.getManager().getLoader();
        int protocolVersion = Via.getAPI().getServerVersion().highestSupportedVersion();
        if (protocolVersion >= ProtocolVersion.v1_16.getVersion()) {
            loader.storeListener(new FireExtinguishListener(this)).register();
        }
        if (protocolVersion >= ProtocolVersion.v1_14.getVersion()) {
            loader.storeListener(new LecternInteractListener(this)).register();
        }
        if (protocolVersion >= ProtocolVersion.v1_12.getVersion()) {
            loader.storeListener(new FireDamageListener(this)).register();
        }
    }

    @Override
    public void disable() {
        getPluginLoader().disablePlugin(this);
    }
}
