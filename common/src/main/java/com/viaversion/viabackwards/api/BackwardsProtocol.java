/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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

import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class BackwardsProtocol<CU extends ClientboundPacketType, CM extends ClientboundPacketType, SM extends ServerboundPacketType, SU extends ServerboundPacketType>
        extends AbstractProtocol<CU, CM, SM, SU> {

    protected BackwardsProtocol() {
    }

    protected BackwardsProtocol(@Nullable Class<CU> oldClientboundPacketEnum, @Nullable Class<CM> clientboundPacketEnum,
                                @Nullable Class<SM> oldServerboundPacketEnum, @Nullable Class<SU> serverboundPacketEnum) {
        super(oldClientboundPacketEnum, clientboundPacketEnum, oldServerboundPacketEnum, serverboundPacketEnum);
    }

    /**
     * Waits for the given protocol to be loaded to then asynchronously execute the runnable for this protocol.
     */
    protected void executeAsyncAfterLoaded(Class<? extends Protocol> protocolClass, Runnable runnable) {
        Via.getManager().getProtocolManager().addMappingLoaderFuture(getClass(), protocolClass, runnable);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        final BackwardsMappings mappingData = getMappingData();
        if (mappingData != null && mappingData.getViaVersionProtocolClass() != null) {
            executeAsyncAfterLoaded(mappingData.getViaVersionProtocolClass(), this::loadMappingData);
        }
    }

    @Override
    public boolean hasMappingDataToLoad() {
        // Manually load them later, since they depend on VV's mappings
        return false;
    }

    @Override
    public @Nullable BackwardsMappings getMappingData() { // Change return type to BackwardsMappings
        return null;
    }

    public @Nullable TranslatableRewriter<CU> getTranslatableRewriter() {
        return null;
    }
}
