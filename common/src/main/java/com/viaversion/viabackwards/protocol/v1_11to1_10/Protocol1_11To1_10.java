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

package com.viaversion.viabackwards.protocol.v1_11to1_10;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.v1_11to1_10.rewriter.BlockItemPacketRewriter1_11;
import com.viaversion.viabackwards.protocol.v1_11to1_10.rewriter.EntityPacketRewriter1_11;
import com.viaversion.viabackwards.protocol.v1_11to1_10.rewriter.PlayerPacketRewriter1_11;
import com.viaversion.viabackwards.protocol.v1_11to1_10.storage.WindowTracker;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_11;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_11_1to1_12.packet.ClientboundPackets1_12;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import com.viaversion.viaversion.rewriter.ComponentRewriter;

public class Protocol1_11To1_10 extends BackwardsProtocol<ClientboundPackets1_9_3, ClientboundPackets1_9_3, ServerboundPackets1_9_3, ServerboundPackets1_9_3> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.11", "1.10");
    private final EntityPacketRewriter1_11 entityRewriter = new EntityPacketRewriter1_11(this);
    private final BlockItemPacketRewriter1_11 itemRewriter = new BlockItemPacketRewriter1_11(this);
    private TranslatableRewriter<ClientboundPackets1_9_3> componentRewriter;

    public Protocol1_11To1_10() {
        super(ClientboundPackets1_9_3.class, ClientboundPackets1_9_3.class, ServerboundPackets1_9_3.class, ServerboundPackets1_9_3.class);
    }

    @Override
    protected void registerPackets() {
        entityRewriter.register();
        itemRewriter.register();
        PlayerPacketRewriter1_11.register(this);

        SoundRewriter<ClientboundPackets1_9_3> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerNamedSound(ClientboundPackets1_9_3.CUSTOM_SOUND);
        soundRewriter.registerSound(ClientboundPackets1_9_3.SOUND);

        componentRewriter = new TranslatableRewriter<>(this, ComponentRewriter.ReadType.JSON);
        componentRewriter.registerComponentPacket(ClientboundPackets1_9_3.CHAT);
    }

    @Override
    public void init(UserConnection user) {
        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, EntityTypes1_11.EntityType.PLAYER));
        user.addClientWorld(this.getClass(), new ClientWorld());

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
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_11 getItemRewriter() {
        return itemRewriter;
    }

    public TranslatableRewriter<ClientboundPackets1_9_3> getComponentRewriter() {
        return componentRewriter;
    }

    @Override
    public boolean hasMappingDataToLoad() {
        return true;
    }
}
