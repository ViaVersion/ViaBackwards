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

package com.viaversion.viabackwards.protocol.v1_11to1_10;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.protocol.v1_11to1_10.rewriter.BlockItemPacketRewriter1_11;
import com.viaversion.viabackwards.protocol.v1_11to1_10.rewriter.EntityPacketRewriter1_11;
import com.viaversion.viabackwards.protocol.v1_11to1_10.rewriter.PlayerPacketRewriterRewriter1_11;
import com.viaversion.viabackwards.protocol.v1_11to1_10.storage.WindowTracker;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_11;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;

public class Protocol1_11To1_10 extends BackwardsProtocol<ClientboundPackets1_9_3, ClientboundPackets1_9_3, ServerboundPackets1_9_3, ServerboundPackets1_9_3> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.11", "1.10");
    private final EntityPacketRewriter1_11 entityPackets = new EntityPacketRewriter1_11(this);
    private final BlockItemPacketRewriter1_11 blockItemPackets = new BlockItemPacketRewriter1_11(this);

    public Protocol1_11To1_10() {
        super(ClientboundPackets1_9_3.class, ClientboundPackets1_9_3.class, ServerboundPackets1_9_3.class, ServerboundPackets1_9_3.class);
    }

    @Override
    protected void registerPackets() {
        blockItemPackets.register();
        entityPackets.register();
        PlayerPacketRewriterRewriter1_11.register(this);

        SoundRewriter<ClientboundPackets1_9_3> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerNamedSound(ClientboundPackets1_9_3.CUSTOM_SOUND);
        soundRewriter.registerSound(ClientboundPackets1_9_3.SOUND);
    }

    @Override
    public void init(UserConnection user) {
        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld());
        }

        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, EntityTypes1_11.EntityType.PLAYER));

        if (!user.has(WindowTracker.class)) {
            user.put(new WindowTracker());
        }
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_11 getEntityRewriter() {
        return entityPackets;
    }

    @Override
    public BlockItemPacketRewriter1_11 getItemRewriter() {
        return blockItemPackets;
    }

    @Override
    public boolean hasMappingDataToLoad() {
        return true;
    }
}
