/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12_1to1_12_2;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.protocol.protocol1_12to1_12_1.KeepAliveTracker;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class Protocol1_12_1To1_12_2 extends BackwardsProtocol {
    @Override
    protected void registerPackets() {
        // Outgoing
        // 0x1f - Keep alive
        registerOutgoing(State.PLAY, 0x1f, 0x1f, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper packetWrapper) throws Exception {
                        long keepAlive = packetWrapper.read(Type.LONG);
                        packetWrapper.user().get(KeepAliveTracker.class).setKeepAlive(keepAlive);
                        packetWrapper.write(Type.VAR_INT, (int) keepAlive);
                    }
                });
            }
        }); // Keep alive
        // Incoming
        // 0xb - Keep alive
        registerIncoming(State.PLAY, 0xb, 0xb, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper packetWrapper) throws Exception {
                        int keepAlive = packetWrapper.read(Type.VAR_INT);
                        long realKeepAlive = packetWrapper.user().get(KeepAliveTracker.class).getKeepAlive();
                        if (keepAlive != (int) realKeepAlive) {
                            packetWrapper.cancel(); // Wrong data, cancel packet
                        }
                        packetWrapper.write(Type.LONG, realKeepAlive);
                    }
                });
            }
        });
    }

    @Override
    public void init(UserConnection userConnection) {
        userConnection.put(new KeepAliveTracker(userConnection));
    }
}
