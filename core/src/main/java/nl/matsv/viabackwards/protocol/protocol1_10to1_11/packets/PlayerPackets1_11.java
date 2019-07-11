/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_10to1_11.packets;

import nl.matsv.viabackwards.protocol.protocol1_10to1_11.Protocol1_10To1_11;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueTransformer;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;

public class PlayerPackets1_11 {
    private static final ValueTransformer<Short, Float> toNewFloat = new ValueTransformer<Short, Float>(Type.FLOAT) {
        @Override
        public Float transform(PacketWrapper wrapper, Short inputValue) throws Exception {
            return inputValue / 15f;
        }
    };

    public void register(Protocol1_10To1_11 protocol) {
        /* Outgoing packets */

        // Title packet
        protocol.registerOutgoing(State.PLAY, 0x45, 0x45, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Action

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int action = wrapper.get(Type.VAR_INT, 0);

                        // Handle the new ActionBar
                        if (action == 2) {
                            // Convert to the old actionbar way
                            PacketWrapper actionbar = new PacketWrapper(0x0F, null, wrapper.user()); // Chat Message packet
                            String msg = wrapper.read(Type.STRING);
                            msg = ChatRewriter.jsonTextToLegacy(msg);
                            msg = "{\"text\":\"" + msg + "\"}";
                            actionbar.write(Type.STRING, msg);
                            actionbar.write(Type.BYTE, (byte) 2); // Above hotbar

                            actionbar.send(Protocol1_10To1_11.class);

                            wrapper.cancel(); // Cancel the title packet
                            return;
                        }

                        if (action > 2)
                            wrapper.set(Type.VAR_INT, 0, action - 1); // Move everything one position down
                    }
                });


            }
        });

        // Collect item packet
        protocol.registerOutgoing(State.PLAY, 0x48, 0x48, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // 0 - Collected entity id
                map(Type.VAR_INT); // 1 - Collector entity id

                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper packetWrapper) throws Exception {
                        packetWrapper.read(Type.VAR_INT); // Ignore pickup item count
                    }
                });
            }
        });

        /* Incoming packets */

        // Block placement packet
        protocol.registerIncoming(State.PLAY, 0x1C, 0x1C, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION); // 0 - Location
                map(Type.VAR_INT); // 1 - Face
                map(Type.VAR_INT); // 2 - Hand

                map(Type.UNSIGNED_BYTE, toNewFloat);
                map(Type.UNSIGNED_BYTE, toNewFloat);
                map(Type.UNSIGNED_BYTE, toNewFloat);
            }
        });
    }
}
