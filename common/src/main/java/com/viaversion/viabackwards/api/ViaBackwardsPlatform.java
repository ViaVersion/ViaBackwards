/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
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

package com.viaversion.viabackwards.api;

import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.ViaBackwardsConfig;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.Protocol1_11_1To1_12;
import com.viaversion.viabackwards.protocol.protocol1_11to1_11_1.Protocol1_11To1_11_1;
import com.viaversion.viabackwards.protocol.protocol1_12_1to1_12_2.Protocol1_12_1To1_12_2;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viabackwards.protocol.protocol1_12to1_12_1.Protocol1_12To1_12_1;
import com.viaversion.viabackwards.protocol.protocol1_13_1to1_13_2.Protocol1_13_1To1_13_2;
import com.viaversion.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import com.viaversion.viabackwards.protocol.protocol1_13to1_13_1.Protocol1_13To1_13_1;
import com.viaversion.viabackwards.protocol.protocol1_14_1to1_14_2.Protocol1_14_1To1_14_2;
import com.viaversion.viabackwards.protocol.protocol1_14_2to1_14_3.Protocol1_14_2To1_14_3;
import com.viaversion.viabackwards.protocol.protocol1_14_3to1_14_4.Protocol1_14_3To1_14_4;
import com.viaversion.viabackwards.protocol.protocol1_14_4to1_15.Protocol1_14_4To1_15;
import com.viaversion.viabackwards.protocol.protocol1_14to1_14_1.Protocol1_14To1_14_1;
import com.viaversion.viabackwards.protocol.protocol1_15_1to1_15_2.Protocol1_15_1To1_15_2;
import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import com.viaversion.viabackwards.protocol.protocol1_15to1_15_1.Protocol1_15To1_15_1;
import com.viaversion.viabackwards.protocol.protocol1_16_1to1_16_2.Protocol1_16_1To1_16_2;
import com.viaversion.viabackwards.protocol.protocol1_16_2to1_16_3.Protocol1_16_2To1_16_3;
import com.viaversion.viabackwards.protocol.protocol1_16_3to1_16_4.Protocol1_16_3To1_16_4;
import com.viaversion.viabackwards.protocol.protocol1_16_4to1_17.Protocol1_16_4To1_17;
import com.viaversion.viabackwards.protocol.protocol1_16to1_16_1.Protocol1_16To1_16_1;
import com.viaversion.viabackwards.protocol.protocol1_17_1to1_18.Protocol1_17_1To1_18;
import com.viaversion.viabackwards.protocol.protocol1_17to1_17_1.Protocol1_17To1_17_1;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19_1.Protocol1_18_2To1_19_1;
import com.viaversion.viabackwards.protocol.protocol1_18to1_18_2.Protocol1_18To1_18_2;
import com.viaversion.viabackwards.protocol.protocol1_9_4to1_10.Protocol1_9_4To1_10;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.ProtocolManager;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.update.Version;

import java.io.File;
import java.util.logging.Logger;

public interface ViaBackwardsPlatform {

    String MINIMUM_VV_VERSION = "4.4.0";
    String IMPL_VERSION = "$IMPL_VERSION";

    /**
     * Initialize ViaBackwards.
     */
    default void init(File dataFolder) {
        ViaBackwardsConfig config = new ViaBackwardsConfig(new File(dataFolder, "config.yml"));
        config.reloadConfig();

        ViaBackwards.init(this, config);

        if (isOutdated()) return;

        Via.getManager().getSubPlatforms().add(IMPL_VERSION);

        getLogger().info("Loading translations...");
        TranslatableRewriter.loadTranslatables();

        ProtocolManager protocolManager = Via.getManager().getProtocolManager();
        protocolManager.registerProtocol(new Protocol1_9_4To1_10(), ProtocolVersion.v1_9_3, ProtocolVersion.v1_10);

        protocolManager.registerProtocol(new Protocol1_10To1_11(), ProtocolVersion.v1_10, ProtocolVersion.v1_11);
        protocolManager.registerProtocol(new Protocol1_11To1_11_1(), ProtocolVersion.v1_11, ProtocolVersion.v1_11_1);

        protocolManager.registerProtocol(new Protocol1_11_1To1_12(), ProtocolVersion.v1_11_1, ProtocolVersion.v1_12);
        protocolManager.registerProtocol(new Protocol1_12To1_12_1(), ProtocolVersion.v1_12, ProtocolVersion.v1_12_1);
        protocolManager.registerProtocol(new Protocol1_12_1To1_12_2(), ProtocolVersion.v1_12_1, ProtocolVersion.v1_12_2);

        protocolManager.registerProtocol(new Protocol1_12_2To1_13(), ProtocolVersion.v1_12_2, ProtocolVersion.v1_13);
        protocolManager.registerProtocol(new Protocol1_13To1_13_1(), ProtocolVersion.v1_13, ProtocolVersion.v1_13_1);
        protocolManager.registerProtocol(new Protocol1_13_1To1_13_2(), ProtocolVersion.v1_13_1, ProtocolVersion.v1_13_2);

        protocolManager.registerProtocol(new Protocol1_13_2To1_14(), ProtocolVersion.v1_13_2, ProtocolVersion.v1_14);
        protocolManager.registerProtocol(new Protocol1_14To1_14_1(), ProtocolVersion.v1_14, ProtocolVersion.v1_14_1);
        protocolManager.registerProtocol(new Protocol1_14_1To1_14_2(), ProtocolVersion.v1_14_1, ProtocolVersion.v1_14_2);
        protocolManager.registerProtocol(new Protocol1_14_2To1_14_3(), ProtocolVersion.v1_14_2, ProtocolVersion.v1_14_3);
        protocolManager.registerProtocol(new Protocol1_14_3To1_14_4(), ProtocolVersion.v1_14_3, ProtocolVersion.v1_14_4);

        protocolManager.registerProtocol(new Protocol1_14_4To1_15(), ProtocolVersion.v1_14_4, ProtocolVersion.v1_15);
        protocolManager.registerProtocol(new Protocol1_15To1_15_1(), ProtocolVersion.v1_15, ProtocolVersion.v1_15_1);
        protocolManager.registerProtocol(new Protocol1_15_1To1_15_2(), ProtocolVersion.v1_15_1, ProtocolVersion.v1_15_2);

        protocolManager.registerProtocol(new Protocol1_15_2To1_16(), ProtocolVersion.v1_15_2, ProtocolVersion.v1_16);
        protocolManager.registerProtocol(new Protocol1_16To1_16_1(), ProtocolVersion.v1_16, ProtocolVersion.v1_16_1);
        protocolManager.registerProtocol(new Protocol1_16_1To1_16_2(), ProtocolVersion.v1_16_1, ProtocolVersion.v1_16_2);
        protocolManager.registerProtocol(new Protocol1_16_2To1_16_3(), ProtocolVersion.v1_16_2, ProtocolVersion.v1_16_3);
        protocolManager.registerProtocol(new Protocol1_16_3To1_16_4(), ProtocolVersion.v1_16_3, ProtocolVersion.v1_16_4);

        protocolManager.registerProtocol(new Protocol1_16_4To1_17(), ProtocolVersion.v1_16_4, ProtocolVersion.v1_17);
        protocolManager.registerProtocol(new Protocol1_17To1_17_1(), ProtocolVersion.v1_17, ProtocolVersion.v1_17_1);

        protocolManager.registerProtocol(new Protocol1_17_1To1_18(), ProtocolVersion.v1_17_1, ProtocolVersion.v1_18);
        protocolManager.registerProtocol(new Protocol1_18To1_18_2(), ProtocolVersion.v1_18, ProtocolVersion.v1_18_2);

        // 1.19.0 clients cannot join 1.19.1+ servers due to changes in the public profile key. 1.19.0 servers can use ViaBackwards 4.3.1
        protocolManager.registerProtocol(new Protocol1_18_2To1_19_1(), ProtocolVersion.v1_18_2, ProtocolVersion.v1_19_1);
    }

    /**
     * Logger provided by the platform.
     *
     * @return logger instance
     */
    Logger getLogger();

    default boolean isOutdated() {
        String vvVersion = Via.getPlatform().getPluginVersion();
        if (vvVersion != null && new Version(vvVersion).compareTo(new Version(MINIMUM_VV_VERSION + "--")) < 0) {
            getLogger().severe("================================");
            getLogger().severe("YOUR VIAVERSION IS OUTDATED");
            getLogger().severe("PLEASE USE VIAVERSION " + MINIMUM_VV_VERSION + " OR NEWER");
            getLogger().severe("LINK: https://ci.viaversion.com/");
            getLogger().severe("VIABACKWARDS WILL NOW DISABLE");
            getLogger().severe("================================");

            disable();
            return true;
        }

        return false;
    }

    /**
     * Disable the plugin.
     */
    void disable();

    /**
     * Returns ViaBackwards's data folder.
     *
     * @return data folder
     */
    File getDataFolder();
}
