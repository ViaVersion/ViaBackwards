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

package com.viaversion.viabackwards;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.viaversion.viabackwards.api.ViaBackwardsPlatform;
import com.viaversion.viabackwards.utils.VersionInfo;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.velocity.util.LoggerWrapper;
import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(id = "viabackwards",
    name = "ViaBackwards",
    version = VersionInfo.VERSION,
    authors = {"Matsv", "kennytv", "Gerrygames", "creeper123123321", "ForceUpdate1", "EnZaXD"},
    description = "Allows the connection of older clients to newer server versions for Minecraft servers.",
    dependencies = {@Dependency(id = "viaversion")}
)
public class VelocityPlugin implements ViaBackwardsPlatform {
    private Logger logger;
    @Inject
    private org.slf4j.Logger loggerSlf4j;
    @Inject
    @DataDirectory
    private Path configPath;

    @Subscribe(order = PostOrder.LATE)
    public void onProxyStart(ProxyInitializeEvent event) {
        this.logger = new LoggerWrapper(loggerSlf4j);
        Via.getManager().addEnableListener(() -> this.init(new File(getDataFolder(), "config.yml")));
        Via.getManager().addPostEnableListener(this::enable);
    }

    @Override
    public void disable() {
        // Not possible
    }

    @Override
    public File getDataFolder() {
        return configPath.toFile();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
