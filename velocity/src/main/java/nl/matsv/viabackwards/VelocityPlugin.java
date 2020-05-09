/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import nl.matsv.viabackwards.api.ViaBackwardsPlatform;
import nl.matsv.viabackwards.velocity.VersionInfo;
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
public class VelocityPlugin implements ViaBackwardsPlatform {
    private Logger logger;
    @Inject
    private org.slf4j.Logger loggerSlf4j;
    @Inject
    @DataDirectory
    private Path configPath;

    @Subscribe(order = PostOrder.LATE)
    public void onProxyStart(ProxyInitializeEvent e) {
        // Setup Logger
        this.logger = new LoggerWrapper(loggerSlf4j);
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
