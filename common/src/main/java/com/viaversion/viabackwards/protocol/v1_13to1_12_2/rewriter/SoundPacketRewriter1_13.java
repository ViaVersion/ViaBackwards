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
package com.viaversion.viabackwards.protocol.v1_13to1_12_2.rewriter;

import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.data.NamedSoundMappings1_12_2;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.rewriter.RewriterBase;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;

public class SoundPacketRewriter1_13 extends RewriterBase<Protocol1_13To1_12_2> {
    private static final String[] SOUND_SOURCES = {"master", "music", "record", "weather", "block", "hostile", "neutral", "player", "ambient", "voice"};

    public SoundPacketRewriter1_13(Protocol1_13To1_12_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_13.CUSTOM_SOUND, wrapper -> {
            String sound = wrapper.read(Types.STRING);
            String mappedSound = NamedSoundMappings1_12_2.getOldId(sound);
            if (mappedSound != null || (mappedSound = protocol.getMappingData().getMappedNamedSound(sound)) != null) {
                wrapper.write(Types.STRING, mappedSound);
            } else {
                wrapper.write(Types.STRING, sound);
            }
        });

        // Stop Sound -> Plugin Message
        protocol.registerClientbound(ClientboundPackets1_13.STOP_SOUND, ClientboundPackets1_12_1.CUSTOM_PAYLOAD, wrapper -> {
            wrapper.write(Types.STRING, "MC|StopSound");
            byte flags = wrapper.read(Types.BYTE);
            String source;
            if ((flags & 0x01) != 0) {
                source = SOUND_SOURCES[wrapper.read(Types.VAR_INT)];
            } else {
                source = "";
            }

            String sound;
            if ((flags & 0x02) != 0) {
                String newSound = wrapper.read(Types.STRING);
                sound = protocol.getMappingData().getMappedNamedSound(newSound);
                if (sound == null) {
                    sound = "";
                }
            } else {
                sound = "";
            }

            wrapper.write(Types.STRING, source);
            wrapper.write(Types.STRING, sound);
        });

        protocol.registerClientbound(ClientboundPackets1_13.SOUND, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                handler(wrapper -> {
                    int newSound = wrapper.get(Types.VAR_INT, 0);
                    int oldSound = protocol.getMappingData().getSoundMappings().getNewId(newSound);
                    if (oldSound == -1) {
                        wrapper.cancel();
                    } else {
                        wrapper.set(Types.VAR_INT, 0, oldSound);
                    }
                });
            }
        });
    }
}
