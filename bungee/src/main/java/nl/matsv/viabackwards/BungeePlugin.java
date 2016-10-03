package nl.matsv.viabackwards;

import net.md_5.bungee.api.plugin.Plugin;

public class BungeePlugin extends Plugin implements ViaBackwardsPlatform {

    @Override
    public void onEnable() {
        this.init();
    }
}
