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

import nl.matsv.viabackwards.api.ViaBackwardsPlatform;
import nl.matsv.viabackwards.listener.FireDamageListener;
import nl.matsv.viabackwards.listener.FireExtinguishListener;
import nl.matsv.viabackwards.listener.LecternInteractListener;
import org.bukkit.plugin.java.JavaPlugin;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
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
        if (ProtocolRegistry.SERVER_PROTOCOL >= ProtocolVersion.v1_16.getVersion()) {
            loader.storeListener(new FireExtinguishListener(this)).register();
        }
        if (ProtocolRegistry.SERVER_PROTOCOL >= ProtocolVersion.v1_14.getVersion()) {
            loader.storeListener(new LecternInteractListener(this)).register();
        }
        if (ProtocolRegistry.SERVER_PROTOCOL >= ProtocolVersion.v1_12.getVersion()) {
            loader.storeListener(new FireDamageListener(this)).register();
        }
    }

    @Override
    public void disable() {
        getPluginLoader().disablePlugin(this);
    }
}
