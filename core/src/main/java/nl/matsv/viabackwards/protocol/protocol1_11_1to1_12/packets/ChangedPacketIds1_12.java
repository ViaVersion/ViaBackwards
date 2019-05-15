/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.packets;

import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.protocol.protocol1_11_1to1_12.Protocol1_11_1To1_12;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.packets.State;

public class ChangedPacketIds1_12 extends Rewriter<Protocol1_11_1To1_12> {

    @Override
    protected void registerPackets(Protocol1_11_1To1_12 p) {
        p.registerOutgoing(State.PLAY, 0x25, 0x28); // Entity
        p.registerOutgoing(State.PLAY, 0x26, 0x25); // Entity Relative Move
        p.registerOutgoing(State.PLAY, 0x27, 0x26); // Entity Look and Relative Move
        p.registerOutgoing(State.PLAY, 0x28, 0x27); // Entity Look
        p.registerOutgoing(State.PLAY, 0x30, -1, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.cancel();
                    }
                });
            }
        }); // Unlock Recipes TODO does ViaVersion cancel the packet if the new id is -1?
        // 0x31 -> 0x30 Destroy Entities handled in EntityPackets1_12.java
        p.registerOutgoing(State.PLAY, 0x32, 0x31); // Remove Entity Effect
        p.registerOutgoing(State.PLAY, 0x33, 0x32); // Resource Pack Send
        // 0x34 -> 0x33 Respawn handled in EntityPackets1_12.java
        p.registerOutgoing(State.PLAY, 0x35, 0x34); // Entity Head Look
        p.registerOutgoing(State.PLAY, 0x36, -1, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.cancel();
                    }
                });
            }
        }); // Advancement Progress
        p.registerOutgoing(State.PLAY, 0x37, 0x35); // World Border
        p.registerOutgoing(State.PLAY, 0x38, 0x36); //Camera
        p.registerOutgoing(State.PLAY, 0x39, 0x37); // Held Item Change (ClientBound)
        p.registerOutgoing(State.PLAY, 0x3A, 0x38); // Display Scoreboard
        // 0x3B -> 0x39 Entity Metadata handled in EntityPackets1_12.java
        p.registerOutgoing(State.PLAY, 0x3C, 0x3A); // Attach Entity
        p.registerOutgoing(State.PLAY, 0x3D, 0x3B); // Entity Velocity
        // 0x3E -> 0x3C Entity Equipment handled in BlockItemPackets1_12.java
        p.registerOutgoing(State.PLAY, 0x3F, 0x3D); // Set Experience
        p.registerOutgoing(State.PLAY, 0x40, 0x3E); // Update Health
        p.registerOutgoing(State.PLAY, 0x41, 0x3F); // ScoreBoard Objective
        p.registerOutgoing(State.PLAY, 0x42, 0x40); // Set Passengers
        p.registerOutgoing(State.PLAY, 0x43, 0x41); // Teams
        p.registerOutgoing(State.PLAY, 0x44, 0x42); // Update Score
        p.registerOutgoing(State.PLAY, 0x45, 0x43); // Spawn Position
        p.registerOutgoing(State.PLAY, 0x46, 0x44); // Time Update
        p.registerOutgoing(State.PLAY, 0x47, 0x45); // Title
        // 0x48 -> 0x46 Sound Effect handled in SoundPackets1_12.java
        p.registerOutgoing(State.PLAY, 0x49, 0x47); // Player List Header And Footer
        p.registerOutgoing(State.PLAY, 0x4A, 0x48); // Collect Item
        p.registerOutgoing(State.PLAY, 0x4B, 0x49); // Entity Teleport
        p.registerOutgoing(State.PLAY, 0x4C, -1, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.cancel();
                    }
                });
            }
        }); // Advancements
        p.registerOutgoing(State.PLAY, 0x4D, 0x4A); // Entity Properties
        p.registerOutgoing(State.PLAY, 0x4E, 0x4B); // Entity Effect

        // New incoming packet 0x01 - Prepare Crafting Grid
        p.registerIncoming(State.PLAY, 0x02, 0x01); // Tab-Complete (Serverbound)
        p.registerIncoming(State.PLAY, 0x03, 0x02); // Chat Message (Serverbound)
        // 0x04->0x03 Client Status handled in BlockItemPackets1_12.java
        p.registerIncoming(State.PLAY, 0x05, 0x04); // Client Settings
        p.registerIncoming(State.PLAY, 0x06, 0x05); // Confirm Transaction (Serverbound)
        p.registerIncoming(State.PLAY, 0x07, 0x06); // Enchant Item
        // 0x08 -> 0x07 Click Window handled in BlockItemPackets1_12.java
        p.registerIncoming(State.PLAY, 0x09, 0x08); // Close Window (Serverbound)
        p.registerIncoming(State.PLAY, 0x0A, 0x09); // Plugin message (Serverbound)
        p.registerIncoming(State.PLAY, 0x0B, 0x0A); // Use Entity
        p.registerIncoming(State.PLAY, 0x0C, 0x0B); // Keep Alive (Serverbound)
        p.registerIncoming(State.PLAY, 0x0D, 0x0F); // Player
        p.registerIncoming(State.PLAY, 0x0E, 0x0C); // Player Position
        p.registerIncoming(State.PLAY, 0x0F, 0x0D); // Player Position And Look (ServerBound)
        p.registerIncoming(State.PLAY, 0x10, 0x0E); // Player Look
        p.registerIncoming(State.PLAY, 0x11, 0x10); // Vehicle Move
        p.registerIncoming(State.PLAY, 0x12, 0x11); // Steer Boat
        p.registerIncoming(State.PLAY, 0x13, 0x12); // Player Abilities (Serverbound)
        p.registerIncoming(State.PLAY, 0x14, 0x13); // Player Digging
        p.registerIncoming(State.PLAY, 0x15, 0x14); // Entity Action
        p.registerIncoming(State.PLAY, 0x16, 0x15); // Steer Vehicle
        // New incoming packet 0x17 - Crafting Book Data
        p.registerIncoming(State.PLAY, 0x18, 0x16); // Resource Pack Status
        // New incoming packet 0x19 - Advancement Tab
        p.registerIncoming(State.PLAY, 0x1A, 0x17); // Held Item Change (Serverbound)
        // 0x1B -> 0x18 Creative Inventory Action handled in BlockItemPackets.java
        p.registerIncoming(State.PLAY, 0x1C, 0x19); // Update Sign
        p.registerIncoming(State.PLAY, 0x1D, 0x1A); // Animatin (Serverbound)
        p.registerIncoming(State.PLAY, 0x1E, 0x1B); // Spectate
        p.registerIncoming(State.PLAY, 0x1F, 0x1C); // Player Block Placement
        p.registerIncoming(State.PLAY, 0x20, 0x1D); // Use Item
    }

    @Override
    protected void registerRewrites() {

    }
}
