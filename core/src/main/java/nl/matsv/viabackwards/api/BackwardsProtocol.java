/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.api;

import us.myles.ViaVersion.api.protocol.Protocol;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.packets.State;

import java.util.concurrent.CompletableFuture;

public abstract class BackwardsProtocol extends Protocol {

    protected BackwardsProtocol() {
    }

    protected BackwardsProtocol(boolean hasMappingDataToLoad) {
        super(hasMappingDataToLoad);
    }

    public void out(State state, int oldPacketID, int newPacketID) {
        this.registerOutgoing(state, oldPacketID, newPacketID, null);
    }

    public void out(State state, int oldPacketID, int newPacketID, PacketRemapper packetRemapper) {
        this.registerOutgoing(state, oldPacketID, newPacketID, packetRemapper);
    }

    public void in(State state, int oldPacketID, int newPacketID) {
        this.registerIncoming(state, oldPacketID, newPacketID, null);
    }

    public void in(State state, int oldPacketID, int newPacketID, PacketRemapper packetRemapper) {
        this.registerIncoming(state, oldPacketID, newPacketID, packetRemapper);
    }

    /**
     * Waits for the given protocol to be loaded to then asynchronously execute the runnable for this protocol.
     */
    protected void executeAsyncAfterLoaded(Class<? extends Protocol> protocolClass, Runnable runnable) {
        CompletableFuture<Void> future = ProtocolRegistry.getMappingLoaderFuture(protocolClass);
        if (future == null) {
            runnable.run();
            return;
        }

        // If the protocol to depend on has been loaded, execute the new runnable async and schedule it for necessary completion
        future.whenComplete((v, t) -> ProtocolRegistry.addMappingLoaderFuture(getClass(), runnable));
    }
}
