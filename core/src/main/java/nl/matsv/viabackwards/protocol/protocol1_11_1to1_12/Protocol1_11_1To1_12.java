/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_11_1to1_12;

import lombok.Getter;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data.ShoulderTracker;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.packets.*;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

@Getter
public class Protocol1_11_1To1_12 extends BackwardsProtocol {
    // TODO checkAndStore all rewriters and make them easy accessible?
    private EntityPackets1_12 entityPackets;
    private BlockItemPackets1_12 blockItemPackets;

    @Override
    protected void registerPackets() {
        new ChangedPacketIds1_12().register(this);
        (entityPackets = new EntityPackets1_12()).register(this);
        (blockItemPackets = new BlockItemPackets1_12()).register(this);
        new SoundPackets1_12().register(this);
        new ChatPackets1_12().register(this);
    }

    @Override
    public void init(UserConnection user) {
        // Register ClientWorld
        if (!user.has(ClientWorld.class))
            user.put(new ClientWorld(user));

        // Register EntityTracker if it doesn't exist yet.
        if (!user.has(EntityTracker.class))
            user.put(new EntityTracker(user));

        user.put(new ShoulderTracker(user));

        // Init protocol in EntityTracker
        user.get(EntityTracker.class).initProtocol(this);
    }
}
