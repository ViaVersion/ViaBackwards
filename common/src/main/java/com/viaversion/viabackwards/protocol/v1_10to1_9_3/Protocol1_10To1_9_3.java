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

package com.viaversion.viabackwards.protocol.v1_10to1_9_3;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.v1_10to1_9_3.rewriter.BlockItemPacketRewriter1_10;
import com.viaversion.viabackwards.protocol.v1_10to1_9_3.rewriter.EntityPacketRewriter1_10;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ClientWorld;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_10;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.protocol.remapper.ValueTransformer;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.v1_9_1to1_9_3.packet.ServerboundPackets1_9_3;
import com.viaversion.viaversion.rewriter.ComponentRewriter;

public class Protocol1_10To1_9_3 extends BackwardsProtocol<ClientboundPackets1_9_3, ClientboundPackets1_9_3, ServerboundPackets1_9_3, ServerboundPackets1_9_3> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.10", "1.9.4");
    private static final ValueTransformer<Float, Short> TO_OLD_PITCH = new ValueTransformer<>(Types.UNSIGNED_BYTE) {
        public Short transform(PacketWrapper packetWrapper, Float inputValue) {
            return (short) Math.round(inputValue * 63.5F);
        }
    };
    private final EntityPacketRewriter1_10 entityRewriter = new EntityPacketRewriter1_10(this);
    private final BlockItemPacketRewriter1_10 itemRewriter = new BlockItemPacketRewriter1_10(this);

    public Protocol1_10To1_9_3() {
        super(ClientboundPackets1_9_3.class, ClientboundPackets1_9_3.class, ServerboundPackets1_9_3.class, ServerboundPackets1_9_3.class);
    }

    @Override
    protected void registerPackets() {
        entityRewriter.register();
        itemRewriter.register();

        SoundRewriter<ClientboundPackets1_9_3> soundRewriter = new SoundRewriter<>(this);
        registerClientbound(ClientboundPackets1_9_3.CUSTOM_SOUND, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // 0 - Sound name
                map(Types.VAR_INT); // 1 - Sound Category
                map(Types.INT); // 2 - x
                map(Types.INT); // 3 - y
                map(Types.INT); // 4 - z
                map(Types.FLOAT); // 5 - Volume
                map(Types.FLOAT, TO_OLD_PITCH); // 6 - Pitch
                handler(soundRewriter.getNamedSoundHandler());
            }
        });
        registerClientbound(ClientboundPackets1_9_3.SOUND, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // 0 - Sound name
                map(Types.VAR_INT); // 1 - Sound Category
                map(Types.INT); // 2 - x
                map(Types.INT); // 3 - y
                map(Types.INT); // 4 - z
                map(Types.FLOAT); // 5 - Volume
                map(Types.FLOAT, TO_OLD_PITCH); // 6 - Pitch
                handler(soundRewriter.getSoundHandler());
            }
        });

        registerServerbound(ServerboundPackets1_9_3.RESOURCE_PACK, new PacketHandlers() {
            @Override
            public void register() {
                read(Types.STRING); // 0 - Hash
                map(Types.VAR_INT); // 1 - Result
            }
        });

        TranslatableRewriter<ClientboundPackets1_9_3> componentRewriter = new TranslatableRewriter<>(this, ComponentRewriter.ReadType.JSON);
        componentRewriter.registerComponentPacket(ClientboundPackets1_9_3.CHAT);
    }

    @Override
    public void init(UserConnection user) {
        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld());
        }

        user.addEntityTracker(this.getClass(), new EntityTrackerBase(user, EntityTypes1_10.EntityType.PLAYER));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public EntityPacketRewriter1_10 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_10 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public boolean hasMappingDataToLoad() {
        return true;
    }
}
