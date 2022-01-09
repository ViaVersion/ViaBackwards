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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.type.Type;

public class SoundRewriter extends com.viaversion.viaversion.rewriter.SoundRewriter {

    private final BackwardsProtocol protocol;

    public SoundRewriter(BackwardsProtocol protocol) {
        super(protocol);
        this.protocol = protocol;
    }

    public void registerNamedSound(ClientboundPacketType packetType) {
        protocol.registerClientbound(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Sound identifier
                handler(getNamedSoundHandler());
            }
        });
    }

    public void registerStopSound(ClientboundPacketType packetType) {
        protocol.registerClientbound(packetType, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(getStopSoundHandler());
            }
        });
    }

    public PacketHandler getNamedSoundHandler() {
        return wrapper -> {
            String soundId = wrapper.get(Type.STRING, 0);
            String mappedId = protocol.getMappingData().getMappedNamedSound(soundId);
            if (mappedId == null) return;
            if (!mappedId.isEmpty()) {
                wrapper.set(Type.STRING, 0, mappedId);
            } else {
                wrapper.cancel();
            }
        };
    }

    public PacketHandler getStopSoundHandler() {
        return wrapper -> {
            byte flags = wrapper.passthrough(Type.BYTE);
            if ((flags & 0x02) == 0) return; // No sound specified

            if ((flags & 0x01) != 0) {
                wrapper.passthrough(Type.VAR_INT); // Source
            }

            String soundId = wrapper.read(Type.STRING);
            String mappedId = protocol.getMappingData().getMappedNamedSound(soundId);
            if (mappedId == null) {
                // No mapping found
                wrapper.write(Type.STRING, soundId);
                return;
            }

            if (!mappedId.isEmpty()) {
                wrapper.write(Type.STRING, mappedId);
            } else {
                // Cancel if set to empty
                wrapper.cancel();
            }
        };
    }
}
