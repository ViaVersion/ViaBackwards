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
import com.viaversion.viabackwards.fabric.util.LoggerWrapper;
import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;

public class ViaFabricAddon implements ViaBackwardsPlatform, Runnable {
    private final Logger logger = new LoggerWrapper(LogManager.getLogger("ViaBackwards"));
    private File configDir;

    @Override
    public void run() {
        Path configDirPath = FabricLoader.getInstance().getConfigDir().resolve("ViaBackwards");
        configDir = configDirPath.toFile();
        this.init(new File(getDataFolder(), "config.yml"));
    }

    @Override
    public void disable() {
        // Not possible
    }

    @Override
    public File getDataFolder() {
        return configDir;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
