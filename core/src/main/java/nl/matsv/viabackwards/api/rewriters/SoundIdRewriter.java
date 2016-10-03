/*
 *
 *     Copyright (C) 2016 Matsv
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
