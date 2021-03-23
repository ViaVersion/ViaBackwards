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

import com.google.inject.Inject;
import nl.matsv.viabackwards.api.ViaBackwardsPlatform;
import nl.matsv.viabackwards.utils.VersionInfo;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.sponge.util.LoggerWrapper;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

@Plugin(id = "viabackwards",
        name = "ViaBackwards",
        version = VersionInfo.VERSION,
        authors = {"Matsv", "KennyTV", "Gerrygames", "creeper123123321", "ForceUpdate1"},
        description = "Allow older Minecraft versions to connect to a newer server version.",
        dependencies = {@Dependency(id = "viaversion")}
)
public class SpongePlugin implements ViaBackwardsPlatform {
    private Logger logger;
    @Inject
    private org.slf4j.Logger loggerSlf4j;
    @Inject
    @ConfigDir(sharedRoot = false)
    private Path configPath;

    @Listener(order = Order.LATE)
    public void onGameStart(GameInitializationEvent e) {
        // Setup Logger
        this.logger = new LoggerWrapper(loggerSlf4j);
        // Init!
        Via.getManager().addEnableListener(() -> this.init(configPath.resolve("config.yml").toFile()));
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
