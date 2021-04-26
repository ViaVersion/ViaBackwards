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
package com.viaversion.viabackwards.api;

import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.entities.storage.EntityTracker;
import org.checkerframework.checker.nullness.qual.Nullable;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;

public abstract class BackwardsProtocol<C1 extends ClientboundPacketType, C2 extends ClientboundPacketType, S1 extends ServerboundPacketType, S2 extends ServerboundPacketType>
        extends Protocol<C1, C2, S1, S2> {

    protected BackwardsProtocol() {
    }

    protected BackwardsProtocol(@Nullable Class<C1> oldClientboundPacketEnum, @Nullable Class<C2> clientboundPacketEnum,
                                @Nullable Class<S1> oldServerboundPacketEnum, @Nullable Class<S2> serverboundPacketEnum) {
        super(oldClientboundPacketEnum, clientboundPacketEnum, oldServerboundPacketEnum, serverboundPacketEnum);
    }

    /**
     * Waits for the given protocol to be loaded to then asynchronously execute the runnable for this protocol.
     */
    protected void executeAsyncAfterLoaded(Class<? extends Protocol> protocolClass, Runnable runnable) {
        Via.getManager().getProtocolManager().addMappingLoaderFuture(getClass(), protocolClass, runnable);
    }

    protected void initEntityTracker(UserConnection user) {
        EntityTracker entityTracker = user.get(EntityTracker.class);
        if (entityTracker == null) {
            user.put(entityTracker = new EntityTracker(user));
        }

        entityTracker.initProtocol(this);
    }

    @Override
    public boolean hasMappingDataToLoad() {
        // Manually load them later, since they depend on VV's mappings
        return false;
    }

    @Override
    public BackwardsMappings getMappingData() {
        return null;
    }
}
