/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_10to1_11;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.BackwardsMappings;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.SoundRewriter;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.packets.BlockItemPackets1_11;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.packets.EntityPackets1_11;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.packets.PlayerPackets1_11;
import nl.matsv.viabackwards.protocol.protocol1_10to1_11.storage.WindowTracker;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class Protocol1_10To1_11 extends BackwardsProtocol<ClientboundPackets1_9_3, ClientboundPackets1_9_3, ServerboundPackets1_9_3, ServerboundPackets1_9_3> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.11", "1.10", null, true);
    private EntityPackets1_11 entityPackets; // Required for the item rewriter
    private BlockItemPackets1_11 blockItemPackets;

    public Protocol1_10To1_11() {
        super(ClientboundPackets1_9_3.class, ClientboundPackets1_9_3.class, ServerboundPackets1_9_3.class, ServerboundPackets1_9_3.class);
    }

    @Override
    protected void registerPackets() {
        (entityPackets = new EntityPackets1_11(this)).register();
        new PlayerPackets1_11().register(this);
        (blockItemPackets = new BlockItemPackets1_11(this)).register();

        SoundRewriter soundRewriter = new SoundRewriter(this);
        soundRewriter.registerNamedSound(ClientboundPackets1_9_3.NAMED_SOUND);
        soundRewriter.registerSound(ClientboundPackets1_9_3.SOUND);
    }

    @Override
    public void init(UserConnection user) {
        // Register ClientWorld
        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld(user));
        }

        // Register EntityTracker if it doesn't exist yet.
        if (!user.has(EntityTracker.class)) {
            user.put(new EntityTracker(user));
        }

        if (!user.has(WindowTracker.class)) {
            user.put(new WindowTracker(user));
        }

        // Init protocol in EntityTracker
        user.get(EntityTracker.class).initProtocol(this);
    }

    public EntityPackets1_11 getEntityPackets() {
        return entityPackets;
    }

    public BlockItemPackets1_11 getBlockItemPackets() {
        return blockItemPackets;
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public boolean hasMappingDataToLoad() {
        return true;
    }
}
