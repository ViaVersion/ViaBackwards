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
package com.viaversion.viabackwards.protocol.protocol1_19to1_19_1;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.packets.EntityPackets1_19_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ServerboundPackets1_19;

public final class Protocol1_19To1_19_1 extends BackwardsProtocol<ClientboundPackets1_19, ClientboundPackets1_19, ServerboundPackets1_19, ServerboundPackets1_19> {

    private final EntityPackets1_19_1 entityRewriter = new EntityPackets1_19_1(this);

    @Override
    protected void registerPackets() {
        entityRewriter.register();

        // Skip 1.19 and assume 1.19.1->1.18.2 translation
        registerClientbound(ClientboundPackets1_19.SYSTEM_CHAT, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.COMPONENT);
                handler(wrapper -> {
                    if (wrapper.read(Type.BOOLEAN)) {
                        wrapper.cancel();
                        return;
                    }

                    wrapper.write(Type.VAR_INT, 0); // TODO
                });
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO.getId(), ServerboundLoginPackets.HELLO.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Name
                // Write empty profile key and uuid
                read(Type.OPTIONAL_PROFILE_KEY);
                create(Type.OPTIONAL_PROFILE_KEY, null);
                create(Type.OPTIONAL_UUID, null);
            }
        });
    }

    @Override
    public EntityPackets1_19_1 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public void init(final UserConnection user) {
        addEntityTracker(user, new EntityTrackerBase(user, null));
    }
}
