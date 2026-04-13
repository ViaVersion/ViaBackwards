/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2026 ViaVersion and contributors
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

import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.protocol.AbstractProtocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.type.Types;

public class SoundRewriter<C extends ClientboundPacketType> extends com.viaversion.viaversion.rewriter.SoundRewriter<C> {

    public SoundRewriter(final AbstractProtocol<C, ?, ?, ?> protocol) {
        super(protocol);
    }

    public void registerNamedSound(final C packetType) {
        if (protocol.getMappingData() == null || Mappings.isFullIdentity(protocol.getMappingData().getFullSoundMappings())) {
            return;
        }
        protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Types.STRING); // Sound identifier
            getNamedSoundHandler().handle(wrapper);
        });
    }

    public void registerStopSound(final C packetType) {
        if (protocol.getMappingData() == null || Mappings.isFullIdentity(protocol.getMappingData().getFullSoundMappings())) {
            return;
        }
        protocol.registerClientbound(packetType, wrapper -> {
            final byte flags = wrapper.passthrough(Types.BYTE);
            if ((flags & 0x01) != 0) {
                wrapper.passthrough(Types.VAR_INT); // Source
            }

            if ((flags & 0x02) == 0) return; // No sound specified
            final String soundId = wrapper.read(Types.STRING);
            final String mappedId = protocol.getMappingData().getFullSoundMappings().mappedIdentifier(soundId);
            if (mappedId == null) {
                // No mapping found
                wrapper.write(Types.STRING, soundId);
                return;
            }

            if (!mappedId.isEmpty()) {
                wrapper.write(Types.STRING, mappedId);
            } else {
                // Cancel if set to empty
                wrapper.cancel();
            }
        });
    }

    public PacketHandler getNamedSoundHandler() {
        return wrapper -> {
            final String soundId = wrapper.get(Types.STRING, 0);
            final String mappedId = protocol.getMappingData().getFullSoundMappings().mappedIdentifier(soundId);
            if (mappedId == null) {
                return;
            }

            if (!mappedId.isEmpty()) {
                wrapper.set(Types.STRING, 0, mappedId);
            } else {
                wrapper.cancel();
            }
        };
    }
}
