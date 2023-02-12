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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;

public class SoundRewriter<C extends ClientboundPacketType> extends com.viaversion.viaversion.rewriter.SoundRewriter<C> {

    private final BackwardsProtocol<C, ?, ?, ?> protocol;

    public SoundRewriter(BackwardsProtocol<C, ?, ?, ?> protocol) {
        super(protocol);
        this.protocol = protocol;
    }

    public void registerNamedSound(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Sound identifier
                handler(getNamedSoundHandler());
            }
        });
    }

    public void registerStopSound(C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                handler(getStopSoundHandler());
            }
        });
    }

    public PacketHandler getNamedSoundHandler() {
        return wrapper -> {
            String soundId = wrapper.get(Type.STRING, 0);
            String mappedId = protocol.getMappingData().getMappedNamedSound(soundId);
            if (mappedId == null) {
                return;
            }

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

    public PacketHandler get1_19_3SoundHandler() {
        return wrapper -> {
            final int soundId = wrapper.read(Type.VAR_INT);
            if (soundId != 0) {
                final int mappedId = idRewriter.rewrite(soundId - 1); // Normalize the id
                if (mappedId == -1) {
                    wrapper.cancel();
                    return;
                }

                wrapper.write(Type.VAR_INT, mappedId + 1);
                return;
            }

            // Is followed by the resource loation
            wrapper.write(Type.VAR_INT, 0);

            String soundIdentifier = wrapper.read(Type.STRING);
            final String mappedIdentifier = protocol.getMappingData().getMappedNamedSound(soundIdentifier);
            if (mappedIdentifier != null) {
                if (mappedIdentifier.isEmpty()) {
                    wrapper.cancel();
                    return;
                }

                soundIdentifier = mappedIdentifier;
            }
            wrapper.write(Type.STRING, soundIdentifier);
        };
    }
}
