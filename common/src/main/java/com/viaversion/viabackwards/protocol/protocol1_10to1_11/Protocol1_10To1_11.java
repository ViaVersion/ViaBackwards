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

package com.viaversion.viabackwards.protocol.protocol1_10to1_11;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.packets.BlockItemPackets1_11;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.packets.EntityPackets1_11;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.packets.PlayerPackets1_11;
import com.viaversion.viabackwards.protocol.protocol1_10to1_11.storage.WindowTracker;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_11Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class Protocol1_10To1_11 extends BackwardsProtocol<ClientboundPackets1_9_3, ClientboundPackets1_9_3, ServerboundPackets1_9_3, ServerboundPackets1_9_3> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.11", "1.10", null, true);
    private EntityPackets1_11 entityPackets; // Required for the item rewriter
    private BlockItemPackets1_11 blockItemPackets;

    public Protocol1_10To1_11() {
        super(ClientboundPackets1_9_3.class, ClientboundPackets1_9_3.class, ServerboundPackets1_9_3.class, ServerboundPackets1_9_3.class);
    }

    @Override
    protected void registerPackets() {
        (entityPackets = new EntityPackets1_11(this)).register();
        new PlayerPackets1_11().register(this);
        (blockItemPackets = new BlockItemPackets1_11(this)).register();

        SoundRewriter soundRewriter = new SoundRewriter(this);
        soundRewriter.registerNamedSound(ClientboundPackets1_9_3.NAMED_SOUND);
        soundRewriter.registerSound(ClientboundPackets1_9_3.SOUND);
    }

    @Override
    public void init(UserConnection user) {
        // Register ClientWorld
        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld(user));
        }

        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, Entity1_11Types.EntityType.PLAYER, true));

        if (!user.has(WindowTracker.class)) {
            user.put(new WindowTracker());
        }
    }

    public EntityPackets1_11 getEntityPackets() {
        return entityPackets;
    }

    public BlockItemPackets1_11 getBlockItemPackets() {
        return blockItemPackets;
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public boolean hasMappingDataToLoad() {
        return true;
    }
}
