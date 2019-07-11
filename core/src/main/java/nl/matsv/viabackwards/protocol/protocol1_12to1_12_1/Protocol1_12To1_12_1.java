/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12to1_12_1;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.packets.State;

public class Protocol1_12To1_12_1 extends BackwardsProtocol {
    @Override
    protected void registerPackets() {
        registerOutgoing(State.PLAY, 0x2b, -1, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.cancel();
                    }
                });
            }
        }); // TODO
        registerOutgoing(State.PLAY, 0x2c, 0x2b);
        registerOutgoing(State.PLAY, 0x2d, 0x2c);
        registerOutgoing(State.PLAY, 0x2e, 0x2d);
        registerOutgoing(State.PLAY, 0x2f, 0x2e);
        registerOutgoing(State.PLAY, 0x30, 0x2f);
        registerOutgoing(State.PLAY, 0x31, 0x30);
        registerOutgoing(State.PLAY, 0x32, 0x31);
        registerOutgoing(State.PLAY, 0x33, 0x32);
        registerOutgoing(State.PLAY, 0x34, 0x33);
        registerOutgoing(State.PLAY, 0x35, 0x34);
        registerOutgoing(State.PLAY, 0x36, 0x35);
        registerOutgoing(State.PLAY, 0x37, 0x36);
        registerOutgoing(State.PLAY, 0x38, 0x37);
        registerOutgoing(State.PLAY, 0x39, 0x38);
        registerOutgoing(State.PLAY, 0x3a, 0x39);
        registerOutgoing(State.PLAY, 0x3b, 0x3a);
        registerOutgoing(State.PLAY, 0x3c, 0x3b);
        registerOutgoing(State.PLAY, 0x3d, 0x3c);
        registerOutgoing(State.PLAY, 0x3e, 0x3d);
        registerOutgoing(State.PLAY, 0x3f, 0x3e);
        registerOutgoing(State.PLAY, 0x40, 0x3f);
        registerOutgoing(State.PLAY, 0x41, 0x40);
        registerOutgoing(State.PLAY, 0x42, 0x41);
        registerOutgoing(State.PLAY, 0x43, 0x42);
        registerOutgoing(State.PLAY, 0x44, 0x43);
        registerOutgoing(State.PLAY, 0x45, 0x44);
        registerOutgoing(State.PLAY, 0x46, 0x45);
        registerOutgoing(State.PLAY, 0x47, 0x46);
        registerOutgoing(State.PLAY, 0x48, 0x47);
        registerOutgoing(State.PLAY, 0x49, 0x48);
        registerOutgoing(State.PLAY, 0x4a, 0x49);
        registerOutgoing(State.PLAY, 0x4b, 0x4a);
        registerOutgoing(State.PLAY, 0x4c, 0x4b);
        registerOutgoing(State.PLAY, 0x4d, 0x4c);
        registerOutgoing(State.PLAY, 0x4e, 0x4d);
        registerOutgoing(State.PLAY, 0x4f, 0x4e);

        registerIncoming(State.PLAY, -1, 0x1, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.cancel();
                    }
                });
            }
        }); // TODO
        registerIncoming(State.PLAY, 0x1, 0x2);
        registerIncoming(State.PLAY, 0x2, 0x3);
        registerIncoming(State.PLAY, 0x3, 0x4);
        registerIncoming(State.PLAY, 0x4, 0x5);
        registerIncoming(State.PLAY, 0x5, 0x6);
        registerIncoming(State.PLAY, 0x6, 0x7);
        registerIncoming(State.PLAY, 0x7, 0x8);
        registerIncoming(State.PLAY, 0x8, 0x9);
        registerIncoming(State.PLAY, 0x9, 0xa);
        registerIncoming(State.PLAY, 0xa, 0xb);
        registerIncoming(State.PLAY, 0xb, 0xc);
        registerIncoming(State.PLAY, 0xc, 0xd);
        registerIncoming(State.PLAY, 0xd, 0xe);
        registerIncoming(State.PLAY, 0xe, 0xf);
        registerIncoming(State.PLAY, 0xf, 0x10);
        registerIncoming(State.PLAY, 0x10, 0x11);
        registerIncoming(State.PLAY, 0x11, 0x12);
        // New incoming 0x12 - No sent by client, sad :(
    }

    @Override
    public void init(UserConnection userConnection) {

    }
}
