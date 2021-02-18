package nl.matsv.viabackwards;

import us.myles.ViaVersion.util.Config;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ViaBackwardsConfig extends Config implements nl.matsv.viabackwards.api.ViaBackwardsConfig {

    private boolean addCustomEnchantsToLore;
    private boolean addTeamColorToPrefix;
    private boolean fix1_13FacePlayer;
    private boolean alwaysShowOriginalMobName;

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
        addTeamColorToPrefix = getBoolean("add-teamcolor-to-prefix", true);
        fix1_13FacePlayer = getBoolean("fix-1_13-face-player", false);
        alwaysShowOriginalMobName = getBoolean("always-show-original-mob-name", true);
    }

    @Override
    public boolean addCustomEnchantsToLore() {
        return addCustomEnchantsToLore;
    }

    @Override
    public boolean addTeamColorTo1_13Prefix() {
        return addTeamColorToPrefix;
    }

    @Override
    public boolean isFix1_13FacePlayer() {
        return fix1_13FacePlayer;
    }

    @Override
    public boolean alwaysShowOriginalMobName() {
        return alwaysShowOriginalMobName;
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
