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
package com.viaversion.viabackwards.protocol.v1_14_1to1_14;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.protocol.v1_14_1to1_14.rewriter.EntityPacketRewriter1_14_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_15;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ServerboundPackets1_14;

public class Protocol1_14_1To1_14 extends BackwardsProtocol<ClientboundPackets1_14, ClientboundPackets1_14, ServerboundPackets1_14, ServerboundPackets1_14> {

    private final EntityPacketRewriter1_14_1 entityRewriter = new EntityPacketRewriter1_14_1(this);

    public Protocol1_14_1To1_14() {
        super(ClientboundPackets1_14.class, ClientboundPackets1_14.class, ServerboundPackets1_14.class, ServerboundPackets1_14.class);
    }

    @Override
    protected void registerPackets() {
        entityRewriter.register();
    }

    @Override
    public void init(UserConnection user) {
        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, EntityTypes1_15.PLAYER));
        user.addClientWorld(this.getClass(), new ClientWorld());
    }

    @Override
    public EntityPacketRewriter1_14_1 getEntityRewriter() {
        return entityRewriter;
    }
}
