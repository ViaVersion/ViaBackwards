package nl.matsv.viabackwards;

import org.bukkit.plugin.java.JavaPlugin;

public class BukkitPlugin extends JavaPlugin implements ViaBackwardsPlatform {
    @Override
    public void onEnable() {
        this.init();
    }
}
