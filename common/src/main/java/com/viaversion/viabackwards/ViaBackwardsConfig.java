/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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

import com.viaversion.viabackwards.api.DialogStyleConfig;
import com.viaversion.viaversion.util.ChatColorUtil;
import com.viaversion.viaversion.util.Config;
import com.viaversion.viaversion.util.ConfigSection;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class ViaBackwardsConfig extends Config implements com.viaversion.viabackwards.api.ViaBackwardsConfig {

    private boolean addCustomEnchantsToLore;
    private boolean addTeamColorToPrefix;
    private boolean fix1_13FacePlayer;
    private boolean alwaysShowOriginalMobName;
    private boolean fix1_13FormattedInventoryTitles;
    private boolean handlePingsAsInvAcknowledgements;
    private boolean bedrockAtY0;
    private boolean sculkShriekersToCryingObsidian;
    private boolean scaffoldingToWater;
    private boolean mapDarknessEffect;
    private boolean mapCustomModelData;
    private boolean mapDisplayEntities;
    private boolean suppressEmulationWarnings;
    private boolean dialogsViaChests;
    private DialogStyleConfig dialogStyleConfig;

    public ViaBackwardsConfig(File configFile, Logger logger) {
        super(configFile, logger);
    }

    @Override
    public void reload() {
        super.reload();
        loadFields();
    }

    private void loadFields() {
        addCustomEnchantsToLore = getBoolean("add-custom-enchants-into-lore", true);
        addTeamColorToPrefix = getBoolean("add-teamcolor-to-prefix", true);
        fix1_13FacePlayer = getBoolean("fix-1_13-face-player", false);
        fix1_13FormattedInventoryTitles = getBoolean("fix-formatted-inventory-titles", true);
        alwaysShowOriginalMobName = getBoolean("always-show-original-mob-name", true);
        handlePingsAsInvAcknowledgements = getBoolean("handle-pings-as-inv-acknowledgements", false);
        bedrockAtY0 = getBoolean("bedrock-at-y-0", false);
        sculkShriekersToCryingObsidian = getBoolean("sculk-shriekers-to-crying-obsidian", false);
        scaffoldingToWater = getBoolean("scaffolding-to-water", false);
        mapDarknessEffect = getBoolean("map-darkness-effect", true);
        mapCustomModelData = getBoolean("map-custom-model-data", true);
        mapDisplayEntities = getBoolean("map-display-entities", true);
        suppressEmulationWarnings = getBoolean("suppress-emulation-warnings", false);
        dialogsViaChests = getBoolean("dialogs-via-chests", true);
        dialogStyleConfig = loadDialogStyleConfig(getSection("dialog-style"));
    }

    private DialogStyleConfig loadDialogStyleConfig(final ConfigSection section) {
        return new DialogStyleConfig(
            getString(section, "page-navigation-title", "&9&lPage navigation"),
            getString(section, "page-navigation-next", "&9Left click: &6Go to next page"),
            getString(section, "page-navigation-previous", "&9Right click: &6Go to previous page"),
            getString(section, "increase-value", "&9Left click: &6Increase value by %s"),
            getString(section, "decrease-value", "&9Right click: &6Decrease value by %s"),
            getString(section, "value-range", "&7(Value between &a%s &7and &a%s&7)"),
            getString(section, "next-option", "&9Left click: &6Go to next option"),
            getString(section, "previous-option", "&9Right click: &6Go to previous option"),
            getString(section, "current-value", "&7Current value: &a%s"),
            getString(section, "edit-value", "&9Left click: &6Edit text"),
            getString(section, "set-text", "&9Left click/close: &6Set text"),
            getString(section, "close", "&9Left click: &6Close"),
            getString(section, "toggle-value", "&9Left click: &6Toggle value")
        );
    }

    protected String getString(final ConfigSection section, final String key, final String def) {
        return ChatColorUtil.translateAlternateColorCodes(section.getString(key, def));
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
    public boolean fix1_13FormattedInventoryTitle() {
        return fix1_13FormattedInventoryTitles;
    }

    @Override
    public boolean alwaysShowOriginalMobName() {
        return alwaysShowOriginalMobName;
    }

    @Override
    public boolean handlePingsAsInvAcknowledgements() {
        return handlePingsAsInvAcknowledgements || Boolean.getBoolean("com.viaversion.handlePingsAsInvAcknowledgements");
    }

    @Override
    public boolean bedrockAtY0() {
        return bedrockAtY0;
    }

    @Override
    public boolean sculkShriekerToCryingObsidian() {
        return sculkShriekersToCryingObsidian;
    }

    @Override
    public boolean scaffoldingToWater() {
        return scaffoldingToWater;
    }

    @Override
    public boolean mapDarknessEffect() {
        return mapDarknessEffect;
    }

    @Override
    public boolean mapCustomModelData() {
        return mapCustomModelData;
    }

    @Override
    public boolean mapDisplayEntities() {
        return mapDisplayEntities;
    }

    @Override
    public boolean suppressEmulationWarnings() {
        return suppressEmulationWarnings;
    }

    @Override
    public boolean dialogsViaChests() {
        return dialogsViaChests;
    }

    @Override
    public DialogStyleConfig dialogStyleConfig() {
        return dialogStyleConfig;
    }

    @Override
    public URL getDefaultConfigURL() {
        return getClass().getClassLoader().getResource("assets/viabackwards/config.yml");
    }

    @Override
    public InputStream getDefaultConfigInputStream() {
        return getClass().getClassLoader().getResourceAsStream("assets/viabackwards/config.yml");
    }

    @Override
    protected void handleConfig(Map<String, Object> map) {
    }

    @Override
    public List<String> getUnsupportedOptions() {
        return Collections.emptyList();
    }
}
