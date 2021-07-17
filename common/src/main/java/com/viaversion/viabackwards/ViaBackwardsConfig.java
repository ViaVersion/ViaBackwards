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
package com.viaversion.viabackwards;

import com.viaversion.viaversion.util.Config;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ViaBackwardsConfig extends Config implements com.viaversion.viabackwards.api.ViaBackwardsConfig {

    private boolean addCustomEnchantsToLore;
    private boolean addTeamColorToPrefix;
    private boolean fix1_13FacePlayer;
    private boolean alwaysShowOriginalMobName;
    private boolean handlePingsAsInvAcknowledgements;

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
        handlePingsAsInvAcknowledgements = getBoolean("handle-pings-as-inv-acknowledgements", false);
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
    public boolean handlePingsAsInvAcknowledgements() {
        return handlePingsAsInvAcknowledgements || Boolean.getBoolean("com.viaversion.handlePingsAsInvAcknowledgements");
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
