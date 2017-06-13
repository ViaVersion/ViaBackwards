/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.packets;

import nl.matsv.viabackwards.api.rewriters.SoundRewriter;
import nl.matsv.viabackwards.protocol.protocol1_9_4to1_10.Protocol1_9_4To1_10;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueTransformer;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class SoundPackets1_10 extends SoundRewriter<Protocol1_9_4To1_10> {
    protected static ValueTransformer<Float, Short> toOldPitch = new ValueTransformer<Float, Short>(Type.UNSIGNED_BYTE) {
        public Short transform(PacketWrapper packetWrapper, Float inputValue) throws Exception {
            return (short) Math.round(inputValue * 63.5F);
        }
    };

    @Override
    protected void registerPackets(Protocol1_9_4To1_10 protocol) {
        // Named sound effect
        protocol.registerOutgoing(State.PLAY, 0x19, 0x19, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // 0 - Sound name
                map(Type.VAR_INT); // 1 - Sound Category
                map(Type.INT); // 2 - x
                map(Type.INT); // 3 - y
                map(Type.INT); // 4 - z
                map(Type.FLOAT); // 5 - Volume
                map(Type.FLOAT, toOldPitch); // 6 - Pitch
            }
        });

        // Sound effect
        protocol.registerOutgoing(State.PLAY, 0x46, 0x46, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Sound name
                map(Type.VAR_INT); // 1 - Sound Category
                map(Type.INT); // 2 - x
                map(Type.INT); // 3 - y
                map(Type.INT); // 4 - z
                map(Type.FLOAT); // 5 - Volume
                map(Type.FLOAT, toOldPitch); // 6 - Pitch

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int oldId = wrapper.get(Type.VAR_INT, 0);
                        int newId = handleSounds(oldId);
                        if (newId == -1)
                            wrapper.cancel();
                        else {
                            if (hasPitch(oldId))
                                wrapper.set(Type.UNSIGNED_BYTE, 0, (short) Math.round(handlePitch(oldId) * 63.5F));
                            wrapper.set(Type.VAR_INT, 0, newId);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {
        added(24, -1); // Enchantment table sound
        
        // Husk
        added(249, 381); // Husk -> Zombie ambient
        added(250, 385); // Husk -> Zombie death
        added(251, 386); // Husk -> Zombie hurt
        added(252, 388); // Husk -> Zombie step

        // Polar bear
        added(301, 381, .6F); // Polar bear ambient -> Zombie ambient
        added(302, 381, 1.9F); // Polar baby bear ambient -> Zombie ambient
        added(303, 385, .7F); // Polar bear death -> Zombie death
        added(304, 309, .6F); // Polar bear hurt -> Shulker hurt
        added(305, 240, .6F); // Polar bear step -> Horse step
        added(306, 374, 1.2F); // Polar bear warning -> Wolf growl

        // Stray
        added(365, 320); // Stray -> Skeleton ambient
        added(366, 321); // Stray -> Skeleton death
        added(367, 322); // Stray -> Skeleton hurt
        added(368, 324); // Stray -> Skeleton step

        // Wither skeleton
        added(387, 320); // Wither skeleton -> Skeleton ambient
        added(388, 321); // Wither skeleton -> Skeleton death
        added(389, 322); // Wither skeleton -> Skeleton hurt
        added(390, 324); // Wither skeleton -> Skeleton step
    }

}
