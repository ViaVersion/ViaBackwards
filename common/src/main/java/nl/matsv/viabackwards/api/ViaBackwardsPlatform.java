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

package nl.matsv.viabackwards.api;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.ViaBackwardsConfig;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.Protocol1_11_1To1_12;
import nl.matsv.viabackwards.protocol.protocol1_11to1_11_1.Protocol1_11To1_11_1;
import nl.matsv.viabackwards.protocol.protocol1_12_1to1_12_2.Protocol1_12_1To1_12_2;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12to1_12_1.Protocol1_12To1_12_1;
import nl.matsv.viabackwards.protocol.protocol1_13_1to1_13_2.Protocol1_13_1To1_13_2;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.Protocol1_13To1_13_1;
import nl.matsv.viabackwards.protocol.protocol1_14_1to1_14_2.Protocol1_14_1To1_14_2;
import nl.matsv.viabackwards.protocol.protocol1_14_2to1_14_3.Protocol1_14_2To1_14_3;
import nl.matsv.viabackwards.protocol.protocol1_14_3to1_14_4.Protocol1_14_3To1_14_4;
import nl.matsv.viabackwards.protocol.protocol1_14_4to1_15.Protocol1_14_4To1_15;
import nl.matsv.viabackwards.protocol.protocol1_14to1_14_1.Protocol1_14To1_14_1;
import nl.matsv.viabackwards.protocol.protocol1_15_1to1_15_2.Protocol1_15_1To1_15_2;
import nl.matsv.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import nl.matsv.viabackwards.protocol.protocol1_15to1_15_1.Protocol1_15To1_15_1;
import nl.matsv.viabackwards.protocol.protocol1_16_2to1_16_3.Protocol1_16_2To1_16_3;
import nl.matsv.viabackwards.protocol.protocol1_16_3to1_16_4.Protocol1_16_3To1_16_4;
import nl.matsv.viabackwards.protocol.protocol1_16_4to1_17.Protocol1_16_4To1_17;
import nl.matsv.viabackwards.protocol.protocol1_16to1_16_1.Protocol1_16To1_16_1;
import nl.matsv.viabackwards.protocol.protocol1_16_1to1_16_2.Protocol1_16_1To1_16_2;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.Protocol1_9_4To1_10;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.protocol.ProtocolVersion;
import us.myles.ViaVersion.update.Version;

import java.io.File;
import java.util.logging.Logger;

import static us.myles.ViaVersion.api.protocol.ProtocolRegistry.registerProtocol;

public interface ViaBackwardsPlatform {

    String MINIMUM_VV_VERSION = "3.3.0";
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

        getLogger().info("Registering protocols...");
        registerProtocol(new Protocol1_9_4To1_10(), ProtocolVersion.v1_9_3, ProtocolVersion.v1_10);

        registerProtocol(new Protocol1_10To1_11(), ProtocolVersion.v1_10, ProtocolVersion.v1_11);
        registerProtocol(new Protocol1_11To1_11_1(), ProtocolVersion.v1_11, ProtocolVersion.v1_11_1);

        registerProtocol(new Protocol1_11_1To1_12(), ProtocolVersion.v1_11_1, ProtocolVersion.v1_12);
        registerProtocol(new Protocol1_12To1_12_1(), ProtocolVersion.v1_12, ProtocolVersion.v1_12_1);
        registerProtocol(new Protocol1_12_1To1_12_2(), ProtocolVersion.v1_12_1, ProtocolVersion.v1_12_2);

        registerProtocol(new Protocol1_12_2To1_13(), ProtocolVersion.v1_12_2, ProtocolVersion.v1_13);
        registerProtocol(new Protocol1_13To1_13_1(), ProtocolVersion.v1_13, ProtocolVersion.v1_13_1);
        registerProtocol(new Protocol1_13_1To1_13_2(), ProtocolVersion.v1_13_1, ProtocolVersion.v1_13_2);

        registerProtocol(new Protocol1_13_2To1_14(), ProtocolVersion.v1_13_2, ProtocolVersion.v1_14);
        registerProtocol(new Protocol1_14To1_14_1(), ProtocolVersion.v1_14, ProtocolVersion.v1_14_1);
        registerProtocol(new Protocol1_14_1To1_14_2(), ProtocolVersion.v1_14_1, ProtocolVersion.v1_14_2);
        registerProtocol(new Protocol1_14_2To1_14_3(), ProtocolVersion.v1_14_2, ProtocolVersion.v1_14_3);
        registerProtocol(new Protocol1_14_3To1_14_4(), ProtocolVersion.v1_14_3, ProtocolVersion.v1_14_4);

        registerProtocol(new Protocol1_14_4To1_15(), ProtocolVersion.v1_14_4, ProtocolVersion.v1_15);
        registerProtocol(new Protocol1_15To1_15_1(), ProtocolVersion.v1_15, ProtocolVersion.v1_15_1);
        registerProtocol(new Protocol1_15_1To1_15_2(), ProtocolVersion.v1_15_1, ProtocolVersion.v1_15_2);

        registerProtocol(new Protocol1_15_2To1_16(), ProtocolVersion.v1_15_2, ProtocolVersion.v1_16);
        registerProtocol(new Protocol1_16To1_16_1(), ProtocolVersion.v1_16, ProtocolVersion.v1_16_1);
        registerProtocol(new Protocol1_16_1To1_16_2(), ProtocolVersion.v1_16_1, ProtocolVersion.v1_16_2);
        registerProtocol(new Protocol1_16_2To1_16_3(), ProtocolVersion.v1_16_2, ProtocolVersion.v1_16_3);
        registerProtocol(new Protocol1_16_3To1_16_4(), ProtocolVersion.v1_16_3, ProtocolVersion.v1_16_4);

        registerProtocol(new Protocol1_16_4To1_17(), ProtocolVersion.v1_16_4, ProtocolVersion.v1_17);
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
