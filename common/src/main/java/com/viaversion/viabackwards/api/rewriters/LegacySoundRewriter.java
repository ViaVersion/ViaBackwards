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
import com.viaversion.viaversion.api.rewriter.RewriterBase;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;

@Deprecated
public abstract class LegacySoundRewriter<T extends BackwardsProtocol<?, ?, ?, ?>> extends RewriterBase<T> {
    protected final Int2ObjectMap<SoundData> soundRewrites = new Int2ObjectOpenHashMap<>(64);

    protected LegacySoundRewriter(T protocol) {
        super(protocol);
    }

    public SoundData added(int id, int replacement) {
        return added(id, replacement, -1);
    }

    public SoundData added(int id, int replacement, float newPitch) {
        SoundData data = new SoundData(replacement, true, newPitch, true);
        soundRewrites.put(id, data);
        return data;
    }

    public SoundData removed(int id) {
        SoundData data = new SoundData(-1, false, -1, false);
        soundRewrites.put(id, data);
        return data;
    }

    public int handleSounds(int soundId) {
        int newSoundId = soundId;
        SoundData data = soundRewrites.get(soundId);
        if (data != null) return data.getReplacementSound();

        for (Int2ObjectMap.Entry<SoundData> entry : soundRewrites.int2ObjectEntrySet()) {
            if (soundId > entry.getIntKey()) {
                if (entry.getValue().isAdded()) {
                    newSoundId--;
                } else {
                    newSoundId++;
                }
            }
        }
        return newSoundId;
    }

    public boolean hasPitch(int soundId) {
        SoundData data = soundRewrites.get(soundId);
        return data != null && data.isChangePitch();
    }

    public float handlePitch(int soundId) {
        SoundData data = soundRewrites.get(soundId);
        return data != null ? data.getNewPitch() : 1F;
    }

    public static final class SoundData {
        private final int replacementSound;
        private final boolean changePitch;
        private final float newPitch;
        private final boolean added;

        public SoundData(int replacementSound, boolean changePitch, float newPitch, boolean added) {
            this.replacementSound = replacementSound;
            this.changePitch = changePitch;
            this.newPitch = newPitch;
            this.added = added;
        }

        public int getReplacementSound() {
            return replacementSound;
        }

        public boolean isChangePitch() {
            return changePitch;
        }

        public float getNewPitch() {
            return newPitch;
        }

        public boolean isAdded() {
            return added;
        }
    }
}
