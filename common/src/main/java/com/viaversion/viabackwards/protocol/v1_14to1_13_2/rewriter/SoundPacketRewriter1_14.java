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
package com.viaversion.viabackwards.protocol.v1_14to1_13_2.rewriter;

import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.Protocol1_14To1_13_2;
import com.viaversion.viabackwards.protocol.v1_14to1_13_2.storage.EntityPositionStorage1_14;
import com.viaversion.viaversion.api.data.entity.StoredEntityData;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.rewriter.RewriterBase;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_13_2to1_14.packet.ClientboundPackets1_14;

public class SoundPacketRewriter1_14 extends RewriterBase<Protocol1_14To1_13_2> {

    public SoundPacketRewriter1_14(Protocol1_14To1_13_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        SoundRewriter<ClientboundPackets1_14> soundRewriter = new SoundRewriter<>(protocol);
        soundRewriter.registerSound(ClientboundPackets1_14.SOUND);
        soundRewriter.registerNamedSound(ClientboundPackets1_14.CUSTOM_SOUND);
        soundRewriter.registerStopSound(ClientboundPackets1_14.STOP_SOUND);

        // Entity Sound Effect
        protocol.registerClientbound(ClientboundPackets1_14.SOUND_ENTITY, null, wrapper -> {
            wrapper.cancel();

            int soundId = wrapper.read(Types.VAR_INT);
            int newId = protocol.getMappingData().getSoundMappings().getNewId(soundId);
            if (newId == -1) return;

            int category = wrapper.read(Types.VAR_INT);
            int entityId = wrapper.read(Types.VAR_INT);

            StoredEntityData storedEntity = wrapper.user().getEntityTracker(protocol.getClass()).entityData(entityId);
            EntityPositionStorage1_14 entityStorage;
            if (storedEntity == null || (entityStorage = storedEntity.get(EntityPositionStorage1_14.class)) == null) {
                protocol.getLogger().warning("Untracked entity with id " + entityId);
                return;
            }

            float volume = wrapper.read(Types.FLOAT);
            float pitch = wrapper.read(Types.FLOAT);
            int x = (int) (entityStorage.x() * 8D);
            int y = (int) (entityStorage.y() * 8D);
            int z = (int) (entityStorage.z() * 8D);

            PacketWrapper soundPacket = wrapper.create(ClientboundPackets1_13.SOUND);
            soundPacket.write(Types.VAR_INT, newId);
            soundPacket.write(Types.VAR_INT, category);
            soundPacket.write(Types.INT, x);
            soundPacket.write(Types.INT, y);
            soundPacket.write(Types.INT, z);
            soundPacket.write(Types.FLOAT, volume);
            soundPacket.write(Types.FLOAT, pitch);
            soundPacket.send(Protocol1_14To1_13_2.class);
        });
    }
}
