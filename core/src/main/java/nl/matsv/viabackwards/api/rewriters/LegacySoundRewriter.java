/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api.rewriters;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.util.fastutil.CollectionUtil;
import us.myles.ViaVersion.util.fastutil.IntObjectMap;

import java.util.Map;

public abstract class LegacySoundRewriter<T extends BackwardsProtocol> extends Rewriter<T> {
    protected final IntObjectMap<SoundData> soundRewrites = CollectionUtil.createIntObjectMap(64);

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

        for (Map.Entry<Integer, SoundData> entry : soundRewrites.getMap().entrySet()) {
            if (soundId > entry.getKey()) {
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
