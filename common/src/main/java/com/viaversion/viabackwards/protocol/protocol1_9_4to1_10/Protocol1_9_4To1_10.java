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

package com.viaversion.viabackwards.protocol.protocol1_9_4to1_10;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappings;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.protocol.protocol1_9_4to1_10.packets.BlockItemPackets1_10;
import com.viaversion.viabackwards.protocol.protocol1_9_4to1_10.packets.EntityPackets1_10;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.remapper.ValueTransformer;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class Protocol1_9_4To1_10 extends BackwardsProtocol<ClientboundPackets1_9_3, ClientboundPackets1_9_3, ServerboundPackets1_9_3, ServerboundPackets1_9_3> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.10", "1.9.4");
    private static final ValueTransformer<Float, Short> TO_OLD_PITCH = new ValueTransformer<Float, Short>(Type.UNSIGNED_BYTE) {
        public Short transform(PacketWrapper packetWrapper, Float inputValue) throws Exception {
            return (short) Math.round(inputValue * 63.5F);
        }
    };
    private final EntityPackets1_10 entityPackets = new EntityPackets1_10(this);
    private final BlockItemPackets1_10 blockItemPackets = new BlockItemPackets1_10(this);

    public Protocol1_9_4To1_10() {
        super(ClientboundPackets1_9_3.class, ClientboundPackets1_9_3.class, ServerboundPackets1_9_3.class, ServerboundPackets1_9_3.class);
    }

    protected void registerPackets() {
        entityPackets.register();
        blockItemPackets.register();

        SoundRewriter<ClientboundPackets1_9_3> soundRewriter = new SoundRewriter<>(this);
        registerClientbound(ClientboundPackets1_9_3.NAMED_SOUND, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // 0 - Sound name
                map(Type.VAR_INT); // 1 - Sound Category
                map(Type.INT); // 2 - x
                map(Type.INT); // 3 - y
                map(Type.INT); // 4 - z
                map(Type.FLOAT); // 5 - Volume
                map(Type.FLOAT, TO_OLD_PITCH); // 6 - Pitch
                handler(soundRewriter.getNamedSoundHandler());
            }
        });
        registerClientbound(ClientboundPackets1_9_3.SOUND, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT); // 0 - Sound name
                map(Type.VAR_INT); // 1 - Sound Category
                map(Type.INT); // 2 - x
                map(Type.INT); // 3 - y
                map(Type.INT); // 4 - z
                map(Type.FLOAT); // 5 - Volume
                map(Type.FLOAT, TO_OLD_PITCH); // 6 - Pitch
                handler(soundRewriter.getSoundHandler());
            }
        });

        registerServerbound(ServerboundPackets1_9_3.RESOURCE_PACK_STATUS, new PacketHandlers() {
            @Override
            public void register() {
                read(Type.STRING); // 0 - Hash
                map(Type.VAR_INT); // 1 - Result
            }
        });
    }

    public void init(UserConnection user) {
        // Register ClientWorld
        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld(user));
        }

        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, EntityTypes1_10.EntityType.PLAYER));
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPackets1_10 getEntityRewriter() {
        return entityPackets;
    }

    @Override
    public BlockItemPackets1_10 getItemRewriter() {
        return blockItemPackets;
    }

    @Override
    public boolean hasMappingDataToLoad() {
        return true;
    }
}
