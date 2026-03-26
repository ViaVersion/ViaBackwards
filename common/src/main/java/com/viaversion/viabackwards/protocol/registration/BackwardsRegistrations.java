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
package com.viaversion.viabackwards.protocol.registration;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.protocol.shared_registration.SharedRegistrations;

public final class BackwardsRegistrations {

    private static final SharedRegistrations REGISTRATIONS = SharedRegistrations.create();

    public static void apply() {
        REGISTRATIONS.registrations()
            .range(ProtocolVersion.v1_10, ProtocolVersion.v1_19_3, RegistryRegistrations::registerNamedSound1_10)
            .since(ProtocolVersion.v1_14, RegistryRegistrations::registerStopSound1_14)

            .register();
    }

    public static SharedRegistrations registrations() {
        return REGISTRATIONS;
    }
}
