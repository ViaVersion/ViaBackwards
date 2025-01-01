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

package com.viaversion.viabackwards;

import com.viaversion.viabackwards.api.ViaBackwardsPlatform;
import com.viaversion.viabackwards.listener.BlockBreakListener;
import com.viaversion.viabackwards.listener.FireDamageListener;
import com.viaversion.viabackwards.listener.FireExtinguishListener;
import com.viaversion.viabackwards.listener.LecternInteractListener;
import com.viaversion.viabackwards.listener.PlayerItemDropListener;
import com.viaversion.viabackwards.protocol.v1_20_2to1_20.provider.AdvancementCriteriaProvider;
import com.viaversion.viabackwards.provider.BukkitAdvancementCriteriaProvider;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.platform.providers.ViaProviders;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;

public class BukkitPlugin extends JavaPlugin implements ViaBackwardsPlatform {

    public BukkitPlugin() {
        Via.getManager().addEnableListener(() -> init(new File(getDataFolder(), "config.yml")));
    }

    @Override
    public void onEnable() {
        if (Via.getManager().getInjector().lateProtocolVersionSetting()) {
            // Enable in the next tick
            Via.getPlatform().runSync(this::enable, 1);
        } else {
            enable();
        }
    }

    @Override
    public void enable() {
        ViaBackwardsPlatform.super.enable();

        final ProtocolVersion protocolVersion = Via.getAPI().getServerVersion().highestSupportedProtocolVersion();
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_17)) {
            new PlayerItemDropListener(this).register();
        }
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_16)) {
            new FireExtinguishListener(this).register();
        }
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_14)) {
            new LecternInteractListener(this).register();
        }
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_12)) {
            new FireDamageListener(this).register();
        }
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_11)) {
            new BlockBreakListener(this).register();
        }

        final ViaProviders providers = Via.getManager().getProviders();
        if (protocolVersion.newerThanOrEqualTo(ProtocolVersion.v1_20_2)) {
            providers.use(AdvancementCriteriaProvider.class, new BukkitAdvancementCriteriaProvider());
        }
    }

    @Override
    public void disable() {
        getPluginLoader().disablePlugin(this);
    }
}
