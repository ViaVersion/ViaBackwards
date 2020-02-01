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
import us.myles.ViaVersion.packets.State;

public class ChangedPacketIds1_12 extends Rewriter<Protocol1_11_1To1_12> {

    public ChangedPacketIds1_12(Protocol1_11_1To1_12 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        protocol.registerOutgoing(State.PLAY, 0x25, 0x28); // Entity
        protocol.registerOutgoing(State.PLAY, 0x26, 0x25); // Entity Relative Move
        protocol.registerOutgoing(State.PLAY, 0x27, 0x26); // Entity Look and Relative Move
        protocol.registerOutgoing(State.PLAY, 0x28, 0x27); // Entity Look

        protocol.cancelOutgoing(State.PLAY, 0x30); // Unlock Recipes

        // 0x31 -> 0x30 Destroy Entities handled in EntityPackets1_12.java
        protocol.registerOutgoing(State.PLAY, 0x32, 0x31); // Remove Entity Effect
        protocol.registerOutgoing(State.PLAY, 0x33, 0x32); // Resource Pack Send
        // 0x34 -> 0x33 Respawn handled in EntityPackets1_12.java
        protocol.registerOutgoing(State.PLAY, 0x35, 0x34); // Entity Head Look

        protocol.cancelOutgoing(State.PLAY, 0x36); // Advancement Progress

        protocol.registerOutgoing(State.PLAY, 0x37, 0x35); // World Border
        protocol.registerOutgoing(State.PLAY, 0x38, 0x36); //Camera
        protocol.registerOutgoing(State.PLAY, 0x39, 0x37); // Held Item Change (ClientBound)
        protocol.registerOutgoing(State.PLAY, 0x3A, 0x38); // Display Scoreboard
        // 0x3B -> 0x39 Entity Metadata handled in EntityPackets1_12.java
        protocol.registerOutgoing(State.PLAY, 0x3C, 0x3A); // Attach Entity
        protocol.registerOutgoing(State.PLAY, 0x3D, 0x3B); // Entity Velocity
        // 0x3E -> 0x3C Entity Equipment handled in BlockItemPackets1_12.java
        protocol.registerOutgoing(State.PLAY, 0x3F, 0x3D); // Set Experience
        protocol.registerOutgoing(State.PLAY, 0x40, 0x3E); // Update Health
        protocol.registerOutgoing(State.PLAY, 0x41, 0x3F); // ScoreBoard Objective
        protocol.registerOutgoing(State.PLAY, 0x42, 0x40); // Set Passengers
        protocol.registerOutgoing(State.PLAY, 0x43, 0x41); // Teams
        protocol.registerOutgoing(State.PLAY, 0x44, 0x42); // Update Score
        protocol.registerOutgoing(State.PLAY, 0x45, 0x43); // Spawn Position
        protocol.registerOutgoing(State.PLAY, 0x46, 0x44); // Time Update
        protocol.registerOutgoing(State.PLAY, 0x47, 0x45); // Title
        // 0x48 -> 0x46 Sound Effect handled in SoundPackets1_12.java
        protocol.registerOutgoing(State.PLAY, 0x49, 0x47); // Player List Header And Footer
        protocol.registerOutgoing(State.PLAY, 0x4A, 0x48); // Collect Item
        protocol.registerOutgoing(State.PLAY, 0x4B, 0x49); // Entity Teleport

        protocol.cancelOutgoing(State.PLAY, 0x4C); // Advancements

        protocol.registerOutgoing(State.PLAY, 0x4E, 0x4B); // Entity Effect

        // New incoming packet 0x01 - Prepare Crafting Grid
        protocol.registerIncoming(State.PLAY, 0x02, 0x01); // Tab-Complete (Serverbound)
        protocol.registerIncoming(State.PLAY, 0x03, 0x02); // Chat Message (Serverbound)
        // 0x04->0x03 Client Status handled in BlockItemPackets1_12.java
        protocol.registerIncoming(State.PLAY, 0x05, 0x04); // Client Settings
        protocol.registerIncoming(State.PLAY, 0x06, 0x05); // Confirm Transaction (Serverbound)
        protocol.registerIncoming(State.PLAY, 0x07, 0x06); // Enchant Item
        // 0x08 -> 0x07 Click Window handled in BlockItemPackets1_12.java
        protocol.registerIncoming(State.PLAY, 0x09, 0x08); // Close Window (Serverbound)
        protocol.registerIncoming(State.PLAY, 0x0A, 0x09); // Plugin message (Serverbound)
        protocol.registerIncoming(State.PLAY, 0x0B, 0x0A); // Use Entity
        protocol.registerIncoming(State.PLAY, 0x0C, 0x0B); // Keep Alive (Serverbound)
        protocol.registerIncoming(State.PLAY, 0x0D, 0x0F); // Player
        protocol.registerIncoming(State.PLAY, 0x0E, 0x0C); // Player Position
        protocol.registerIncoming(State.PLAY, 0x0F, 0x0D); // Player Position And Look (ServerBound)
        protocol.registerIncoming(State.PLAY, 0x10, 0x0E); // Player Look
        protocol.registerIncoming(State.PLAY, 0x11, 0x10); // Vehicle Move
        protocol.registerIncoming(State.PLAY, 0x12, 0x11); // Steer Boat
        protocol.registerIncoming(State.PLAY, 0x13, 0x12); // Player Abilities (Serverbound)
        protocol.registerIncoming(State.PLAY, 0x14, 0x13); // Player Digging
        protocol.registerIncoming(State.PLAY, 0x15, 0x14); // Entity Action
        protocol.registerIncoming(State.PLAY, 0x16, 0x15); // Steer Vehicle
        // New incoming packet 0x17 - Crafting Book Data
        protocol.registerIncoming(State.PLAY, 0x18, 0x16); // Resource Pack Status
        // New incoming packet 0x19 - Advancement Tab
        protocol.registerIncoming(State.PLAY, 0x1A, 0x17); // Held Item Change (Serverbound)
        // 0x1B -> 0x18 Creative Inventory Action handled in BlockItemPackets.java
        protocol.registerIncoming(State.PLAY, 0x1C, 0x19); // Update Sign
        protocol.registerIncoming(State.PLAY, 0x1D, 0x1A); // Animatin (Serverbound)
        protocol.registerIncoming(State.PLAY, 0x1E, 0x1B); // Spectate
        protocol.registerIncoming(State.PLAY, 0x1F, 0x1C); // Player Block Placement
        protocol.registerIncoming(State.PLAY, 0x20, 0x1D); // Use Item
    }
}
