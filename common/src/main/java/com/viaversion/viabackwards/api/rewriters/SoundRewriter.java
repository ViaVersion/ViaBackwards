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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.Types;

public class SoundRewriter<C extends ClientboundPacketType> extends com.viaversion.viaversion.rewriter.SoundRewriter<C> {

    private final BackwardsProtocol<C, ?, ?, ?> protocol;

    public SoundRewriter(final BackwardsProtocol<C, ?, ?, ?> protocol) {
        super(protocol);
        this.protocol = protocol;
    }

    public void registerNamedSound(final C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Sound identifier
                handler(getNamedSoundHandler());
            }
        });
    }

    public void registerStopSound(final C packetType) {
        protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                handler(getStopSoundHandler());
            }
        });
    }

    public PacketHandler getNamedSoundHandler() {
        return wrapper -> {
            final String soundId = wrapper.get(Types.STRING, 0);
            final String mappedId = protocol.getMappingData().getMappedNamedSound(soundId);
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

    public PacketHandler getStopSoundHandler() {
        return wrapper -> {
            final byte flags = wrapper.passthrough(Types.BYTE);
            if ((flags & 0x02) == 0) return; // No sound specified

            if ((flags & 0x01) != 0) {
                wrapper.passthrough(Types.VAR_INT); // Source
            }

            final String soundId = wrapper.read(Types.STRING);
            final String mappedId = protocol.getMappingData().getMappedNamedSound(soundId);
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
        };
    }

    @Override
    public void register1_19_3Sound(final C packetType) {
        protocol.registerClientbound(packetType, get1_19_3SoundHandler());
    }

    public PacketHandler get1_19_3SoundHandler() {
        return wrapper -> {
            Holder<SoundEvent> soundEventHolder = wrapper.read(Types.SOUND_EVENT);
            if (soundEventHolder.isDirect()) {
                wrapper.write(Types.SOUND_EVENT, rewriteSoundEvent(wrapper, soundEventHolder));
                return;
            }

            final int mappedId = idRewriter.rewrite(soundEventHolder.id());
            if (mappedId == -1) {
                wrapper.cancel();
                return;
            }

            if (mappedId != soundEventHolder.id()) {
                soundEventHolder = Holder.of(mappedId);
            }

            wrapper.write(Types.SOUND_EVENT, soundEventHolder);
        };
    }

    public Holder<SoundEvent> rewriteSoundEvent(final PacketWrapper wrapper, final Holder<SoundEvent> soundEventHolder) {
        final SoundEvent soundEvent = soundEventHolder.value();
        final String mappedIdentifier = protocol.getMappingData().getMappedNamedSound(soundEvent.identifier());
        if (mappedIdentifier != null) {
            if (!mappedIdentifier.isEmpty()) {
                return Holder.of(soundEvent.withIdentifier(mappedIdentifier));
            }
            wrapper.cancel();
        }
        return soundEventHolder;
    }
}
