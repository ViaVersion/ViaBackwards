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

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.data.ShoulderTracker;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.packets.BlockItemPackets1_12;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.packets.ChatPackets1_12;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.packets.EntityPackets1_12;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.packets.SoundPackets1_12;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_12to1_11_1.ClientboundPackets1_12;
import us.myles.ViaVersion.protocols.protocol1_12to1_11_1.ServerboundPackets1_12;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;
import us.myles.ViaVersion.protocols.protocol1_9to1_8.Protocol1_9To1_8;
import us.myles.viaversion.libs.gson.JsonElement;

public class Protocol1_11_1To1_12 extends BackwardsProtocol<ClientboundPackets1_12, ClientboundPackets1_9_3, ServerboundPackets1_12, ServerboundPackets1_9_3> {

    private EntityPackets1_12 entityPackets;
    private BlockItemPackets1_12 blockItemPackets;

    public Protocol1_11_1To1_12() {
        super(ClientboundPackets1_12.class, ClientboundPackets1_9_3.class, ServerboundPackets1_12.class, ServerboundPackets1_9_3.class);
    }

    @Override
    protected void registerPackets() {
        (entityPackets = new EntityPackets1_12(this)).register();
        (blockItemPackets = new BlockItemPackets1_12(this)).register();
        new SoundPackets1_12(this).register();
        new ChatPackets1_12(this).register();

        registerOutgoing(ClientboundPackets1_12.TITLE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int action = wrapper.passthrough(Type.VAR_INT);
                    if (action >= 0 && action <= 2) {
                        JsonElement component = wrapper.read(Type.COMPONENT);
                        wrapper.write(Type.COMPONENT, Protocol1_9To1_8.fixJson(component.toString()));
                    }
                });
            }
        });

        cancelOutgoing(ClientboundPackets1_12.ADVANCEMENTS);
        cancelOutgoing(ClientboundPackets1_12.UNLOCK_RECIPES);
        cancelOutgoing(ClientboundPackets1_12.SELECT_ADVANCEMENTS_TAB);
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

        user.put(new ShoulderTracker(user));

        // Init protocol in EntityTracker
        user.get(EntityTracker.class).initProtocol(this);
    }

    public EntityPackets1_12 getEntityPackets() {
        return entityPackets;
    }

    public BlockItemPackets1_12 getBlockItemPackets() {
        return blockItemPackets;
    }
}
