/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_9_4to1_10;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.data.BackwardsMappings;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.SoundRewriter;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.packets.BlockItemPackets1_10;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.packets.EntityPackets1_10;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueTransformer;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class Protocol1_9_4To1_10 extends BackwardsProtocol<ClientboundPackets1_9_3, ClientboundPackets1_9_3, ServerboundPackets1_9_3, ServerboundPackets1_9_3> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings("1.10", "1.9.4", null, true);
    private static final ValueTransformer<Float, Short> TO_OLD_PITCH = new ValueTransformer<Float, Short>(Type.UNSIGNED_BYTE) {
        public Short transform(PacketWrapper packetWrapper, Float inputValue) throws Exception {
            return (short) Math.round(inputValue * 63.5F);
        }
    };
    private EntityPackets1_10 entityPackets; // Required for the item rewriter
    private BlockItemPackets1_10 blockItemPackets;

    public Protocol1_9_4To1_10() {
        super(ClientboundPackets1_9_3.class, ClientboundPackets1_9_3.class, ServerboundPackets1_9_3.class, ServerboundPackets1_9_3.class);
    }

    protected void registerPackets() {
        (entityPackets = new EntityPackets1_10(this)).register();
        (blockItemPackets = new BlockItemPackets1_10(this)).register();

        SoundRewriter soundRewriter = new SoundRewriter(this);
        registerOutgoing(ClientboundPackets1_9_3.NAMED_SOUND, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // 0 - Sound name
                map(Type.VAR_INT); // 1 - Sound Category
                map(Type.INT); // 2 - x
                map(Type.INT); // 3 - y
                map(Type.INT); // 4 - z
                map(Type.FLOAT); // 5 - Volume
                map(Type.FLOAT, TO_OLD_PITCH); // 6 - Pitch
                handler(soundRewriter.getNamedSoundHandler());
            }
        });
        registerOutgoing(ClientboundPackets1_9_3.SOUND, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Sound name
                map(Type.VAR_INT); // 1 - Sound Category
                map(Type.INT); // 2 - x
                map(Type.INT); // 3 - y
                map(Type.INT); // 4 - z
                map(Type.FLOAT); // 5 - Volume
                map(Type.FLOAT, TO_OLD_PITCH); // 6 - Pitch
                handler(soundRewriter.getSoundHandler());
            }
        });

        registerIncoming(ServerboundPackets1_9_3.RESOURCE_PACK_STATUS, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING, Type.NOTHING); // 0 - Hash
                map(Type.VAR_INT); // 1 - Result
            }
        });
    }

    public void init(UserConnection user) {
        // Register ClientWorld
        if (!user.has(ClientWorld.class)) {
            user.put(new ClientWorld(user));
        }

        // Register EntityTracker if it doesn't exist yet.
        if (!user.has(EntityTracker.class)) {
            user.put(new EntityTracker(user));
        }

        // Init protocol in EntityTracker
        user.get(EntityTracker.class).initProtocol(this);
    }

    public EntityPackets1_10 getEntityPackets() {
        return entityPackets;
    }

    public BlockItemPackets1_10 getBlockItemPackets() {
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
