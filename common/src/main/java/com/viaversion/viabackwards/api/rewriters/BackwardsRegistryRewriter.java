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
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.rewriter.RegistryDataRewriter;
import com.viaversion.viaversion.util.Key;

public class BackwardsRegistryRewriter extends RegistryDataRewriter {

    private final BackwardsProtocol<?, ?, ?, ?> protocol;

    public BackwardsRegistryRewriter(final BackwardsProtocol<?, ?, ?, ?> protocol) {
        super(protocol);
        this.protocol = protocol;
    }

    @Override
    public RegistryEntry[] handle(final UserConnection connection, final String key, final RegistryEntry[] entries) {
        if (Key.stripMinecraftNamespace(key).equals("worldgen/biome")) {
            for (final RegistryEntry entry : entries) {
                final CompoundTag biome = (CompoundTag) entry.tag();
                if (biome == null) {
                    continue;
                }

                final CompoundTag effects = biome.getCompoundTag("effects");
                updateBiomeEffects(effects);
            }
        }
        return super.handle(connection, key, entries);
    }

    @Override
    public void updateJukeboxSongs(final RegistryEntry[] entries) {
        for (final RegistryEntry entry : entries) {
            if (entry.tag() == null) {
                continue;
            }

            updateSound((CompoundTag) entry.tag(), "sound_event");
        }
    }

    private void updateBiomeEffects(final CompoundTag effects) {
        updateSound(effects.getCompoundTag("mood_sound"), "sound");
        updateSound(effects.getCompoundTag("additions_sound"), "sound");
        updateSound(effects.getCompoundTag("music"), "sound");
        updateSound(effects, "ambient_sound");
    }

    private void updateSound(final CompoundTag tag, final String name) {
        if (tag == null) {
            return;
        }

        final String sound = tag.getString(name);
        if (sound == null) {
            return;
        }

        final String mappedSound = protocol.getMappingData().getMappedNamedSound(sound);
        if (mappedSound == null) {
            return;
        }

        if (mappedSound.isEmpty()) {
            tag.putString(name, "minecraft:intentionally_empty");
        } else {
            tag.putString(name, mappedSound);
        }
    }
}
