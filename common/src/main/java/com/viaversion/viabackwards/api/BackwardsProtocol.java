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
package com.viaversion.viabackwards.api;

import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.BackwardsRegistryRewriter;
import com.viaversion.viabackwards.protocol.registration.BackwardsRegistrations;
import com.viaversion.viabackwards.utils.BackwardsProtocolLogger;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.util.ProtocolLogger;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class BackwardsProtocol<CU extends ClientboundPacketType, CM extends ClientboundPacketType, SM extends ServerboundPacketType, SU extends ServerboundPacketType>
    extends AbstractProtocol<CU, CM, SM, SU> {

    @Deprecated
    protected BackwardsProtocol() {
    }

    protected BackwardsProtocol(@Nullable Class<CU> oldClientboundPacketEnum, @Nullable Class<CM> clientboundPacketEnum,
                                @Nullable Class<SM> oldServerboundPacketEnum, @Nullable Class<SU> serverboundPacketEnum) {
        super(oldClientboundPacketEnum, clientboundPacketEnum, oldServerboundPacketEnum, serverboundPacketEnum);
    }

    @Override
    protected void applySharedRegistrations() {
        super.applySharedRegistrations();
        BackwardsRegistrations.registrations().applyMatching(this);
    }

    @Override
    protected ProtocolLogger createLogger() {
        return new BackwardsProtocolLogger(getClass());
    }

    @Override
    public @Nullable Class<? extends Protocol<?, ?, ?, ?>> dependsOn() {
        return getMappingData() != null ? getMappingData().getViaVersionProtocolClass() : null;
    }

    @Override
    public @Nullable BackwardsMappingData getMappingData() {
        return null;
    }

    @Override
    public @Nullable BackwardsRegistryRewriter getRegistryDataRewriter() {
        return null;
    }
}
