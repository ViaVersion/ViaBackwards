package nl.matsv.viabackwards;

import us.myles.ViaVersion.util.Config;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ViaBackwardsConfig extends Config implements nl.matsv.viabackwards.api.ViaBackwardsConfig {

    private boolean addCustomEnchantsToLore;

    public ViaBackwardsConfig(File configFile) {
        super(configFile);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadFields();
    }

    private void loadFields() {
        addCustomEnchantsToLore = getBoolean("add-custom-enchants-into-lore", true);
    }

    @Override
    public boolean addCustomEnchantsToLore() {
        return addCustomEnchantsToLore;
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
