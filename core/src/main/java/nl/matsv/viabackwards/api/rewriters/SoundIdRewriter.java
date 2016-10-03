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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class SoundIdRewriter<T extends BackwardsProtocol> extends Rewriter<T> {
    private Map<Integer, Integer> soundRewriter = new ConcurrentHashMap<>();
    private Map<Integer, Float> pitchRewriter = new ConcurrentHashMap<>();

    protected void rewriteSound(int newId, int oldId) {
        soundRewriter.put(newId, oldId);
    }

    protected void rewriteSound(int newId, int oldId, float newPitch) {
        rewriteSound(newId, oldId);
        pitchRewriter.put(newId, newPitch);
    }

    public int handleSounds(int soundId) {
        int newSoundId = soundId;
        if (soundRewriter.containsKey(soundId))
            newSoundId = soundId = soundRewriter.get(soundId);
        for (Integer i : soundRewriter.keySet()) {
            if (soundId > i)
                newSoundId--;
        }
        return newSoundId;
    }

    public boolean hasPitch(int soundId) {
        return pitchRewriter.containsKey(soundId);
    }

    public float handlePitch(int soundId) {
        if (pitchRewriter.containsKey(soundId))
            return pitchRewriter.get(soundId);
        return -1;
    }
}
