/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
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

import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.protocol.shared_registration.PacketBound;
import com.viaversion.viaversion.protocol.shared_registration.RegistrationContext;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;

final class RegistryRegistrations {

    static <CU extends ClientboundPacketType> void registerNamedSound1_10(final RegistrationContext<CU, ?> ctx) {
        ctx.clientbound(ClientboundPackets1_9_3.CUSTOM_SOUND, new SoundRewriter<>(ctx.protocol())::registerNamedSound, PacketBound.REMOVED_AT_MAX);
    }

    static <CU extends ClientboundPacketType> void registerStopSound1_14(final RegistrationContext<CU, ?> ctx) {
        ctx.clientbound(ClientboundPackets1_14.STOP_SOUND, new SoundRewriter<>(ctx.protocol())::registerStopSound);
    }
}
