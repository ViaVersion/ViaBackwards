/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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

import com.viaversion.viaversion.configuration.AbstractViaConfig;
import com.viaversion.viaversion.platform.UserConnectionViaVersionPlatform;
import java.util.logging.Logger;

final class TestViaVersionPlatform extends UserConnectionViaVersionPlatform {

    TestViaVersionPlatform() {
        super(null);
    }

    @Override
    public String getPlatformName() {
        return "Test";
    }

    @Override
    public String getPlatformVersion() {
        return "test";
    }

    @Override
    public Logger createLogger(final String name) {
        return Logger.getGlobal();
    }

    @Override
    protected AbstractViaConfig createConfig() {
        return new AbstractViaConfig(null, null) {
            @Override
            public void reload() {
            }
        };
    }
}
