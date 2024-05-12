/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
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

package com.viaversion.viabackwards.protocol.v1_12to1_11_1;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.storage.ShoulderTracker;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.rewriter.BlockItemPacketRewriter1_12;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.rewriter.ComponentRewriter1_12;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.rewriter.EntityPacketRewriter1_12;
import com.viaversion.viabackwards.protocol.v1_12to1_11_1.rewriter.SoundPacketRewriter1_12;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_12;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ServerboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_8to1_9.Protocol1_8To1_9;

public class Protocol1_12To1_11_1 extends BackwardsProtocol<ClientboundPackets1_12, ClientboundPackets1_9_3, ServerboundPackets1_12, ServerboundPackets1_9_3> {

    private static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.12", "1.11");
    private final EntityPacketRewriter1_12 entityRewriter = new EntityPacketRewriter1_12(this);
    private final BlockItemPacketRewriter1_12 itemRewriter = new BlockItemPacketRewriter1_12(this);

    public Protocol1_12To1_11_1() {
        super(ClientboundPackets1_12.class, ClientboundPackets1_9_3.class, ServerboundPackets1_12.class, ServerboundPackets1_9_3.class);
    }

    @Override
    protected void registerPackets() {
        itemRewriter.register();
        entityRewriter.register();
        new SoundPacketRewriter1_12(this).register();
        new ComponentRewriter1_12(this).register();

        registerClientbound(ClientboundPackets1_12.SET_TITLES, wrapper -> {
            int action = wrapper.passthrough(Types.VAR_INT);
            if (action >= 0 && action <= 2) {
                JsonElement component = wrapper.read(Types.COMPONENT);
                wrapper.write(Types.COMPONENT, Protocol1_8To1_9.STRING_TO_JSON.transform(wrapper, component.toString()));
            }
        });

        cancelClientbound(ClientboundPackets1_12.UPDATE_ADVANCEMENTS);
        cancelClientbound(ClientboundPackets1_12.RECIPE);
        cancelClientbound(ClientboundPackets1_12.SELECT_ADVANCEMENTS_TAB);
    }

    @Override
    public void init(UserConnection user) {
        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld());
        }

        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, EntityTypes1_12.EntityType.PLAYER));

        user.put(new ShoulderTracker(user));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_12 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_12 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public boolean hasMappingDataToLoad() {
        return true;
    }
}
