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
package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.packets;

import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.data.NamedSoundMapping;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.rewriter.RewriterBase;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;

public class SoundPackets1_13 extends RewriterBase<Protocol1_12_2To1_13> {
    private static final String[] SOUND_SOURCES = {"master", "music", "record", "weather", "block", "hostile", "neutral", "player", "ambient", "voice"};

    public SoundPackets1_13(Protocol1_12_2To1_13 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerClientbound(ClientboundPackets1_13.NAMED_SOUND, wrapper -> {
            String sound = wrapper.read(Type.STRING);
            String mappedSound = NamedSoundMapping.getOldId(sound);
            if (mappedSound != null || (mappedSound = protocol.getMappingData().getMappedNamedSound(sound)) != null) {
                wrapper.write(Type.STRING, mappedSound);
            } else {
                wrapper.write(Type.STRING, sound);
            }
        });

        // Stop Sound -> Plugin Message
        protocol.registerClientbound(ClientboundPackets1_13.STOP_SOUND, ClientboundPackets1_12_1.PLUGIN_MESSAGE, wrapper -> {
            wrapper.write(Type.STRING, "MC|StopSound");
            byte flags = wrapper.read(Type.BYTE);
            String source;
            if ((flags & 0x01) != 0) {
                source = SOUND_SOURCES[wrapper.read(Type.VAR_INT)];
            } else {
                source = "";
            }

            String sound;
            if ((flags & 0x02) != 0) {
                String newSound = wrapper.read(Type.STRING);
                sound = protocol.getMappingData().getMappedNamedSound(newSound);
                if (sound == null) {
                    sound = "";
                }
            } else {
                sound = "";
            }

            wrapper.write(Type.STRING, source);
            wrapper.write(Type.STRING, sound);
        });

        protocol.registerClientbound(ClientboundPackets1_13.SOUND, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                handler(wrapper -> {
                    int newSound = wrapper.get(Type.VAR_INT, 0);
                    int oldSound = protocol.getMappingData().getSoundMappings().getNewId(newSound);
                    if (oldSound == -1) {
                        wrapper.cancel();
                    } else {
                        wrapper.set(Type.VAR_INT, 0, oldSound);
                    }
                });
            }
        });
    }
}
