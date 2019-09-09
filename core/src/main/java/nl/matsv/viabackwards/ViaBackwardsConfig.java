package nl.matsv.viabackwards;

import us.myles.ViaVersion.util.Config;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ViaBackwardsConfig extends Config implements nl.matsv.viabackwards.api.ViaBackwardsConfig {

    public ViaBackwardsConfig(File configFile) {
        super(configFile);
    }

    @Override
    public boolean addCustomEnchantsToLore() {
        return getBoolean("add-custom-enchants-into-lore", true);
    }

    @Override
    public URL getDefaultConfigURL() {
        return getClass().getClassLoader().getResource("assets/viabackwards/config.yml");
    }

    @Override
    protected void handleConfig(Map<String, Object> map) {
    }

    @Override
    public List<String> getUnsupportedOptions() {
        return Collections.emptyList();
    }
}
