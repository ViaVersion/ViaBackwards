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
package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.StringTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;

public class BackwardsRegistryRewriter extends RegistryDataRewriter {

    private final BackwardsProtocol<?, ?, ?, ?> protocol;

    public BackwardsRegistryRewriter(final BackwardsProtocol<?, ?, ?, ?> protocol) {
        super(protocol);
        this.protocol = protocol;
    }

    @Override
    public void updateJukeboxSongs(final RegistryEntry[] entries) {
        for (final RegistryEntry entry : entries) {
            if (entry.tag() == null) {
                continue;
            }

            final StringTag soundEvent = ((CompoundTag) entry.tag()).getStringTag("sound_event");
            if (soundEvent != null) {
                final String mappedNamedSound = protocol.getMappingData().getMappedNamedSound(soundEvent.getValue());
                if (mappedNamedSound != null) {
                    soundEvent.setValue(mappedNamedSound);
                }
            }
        }
    }
}
