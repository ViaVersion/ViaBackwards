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

import lombok.AllArgsConstructor;
import lombok.Data;
import nl.matsv.viabackwards.api.BackwardsProtocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SoundRewriter<T extends BackwardsProtocol> extends Rewriter<T> {
    private Map<Integer, SoundData> soundRewrites = new ConcurrentHashMap<>();

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
        if (soundRewrites.containsKey(soundId))
            return soundRewrites.get(soundId).getReplacementSound();
        for (Map.Entry<Integer, SoundData> entry : soundRewrites.entrySet()) {
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
        if (soundRewrites.containsKey(soundId))
            return soundRewrites.get(soundId).isChangePitch();
        return false;
    }

    public float handlePitch(int soundId) {
        if (soundRewrites.containsKey(soundId))
            return soundRewrites.get(soundId).getNewPitch();
        return 1f;
    }

    @Data
    @AllArgsConstructor
    public class SoundData {
        private int replacementSound;
        private boolean changePitch = false;
        private float newPitch = 1f;
        private boolean added;
    }
}
