/*
 * Copyright (c) 2016 Matsv
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13;

import lombok.Getter;
import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.PaintingMapping;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets.BlockItemPackets1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets.EntityPackets1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets.PlayerPacket1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets.SoundPackets1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.providers.BackwardsBlockEntityProvider;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.BackwardsBlockStorage;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.PlayerPositionStorage1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.TabCompleteStorage;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.platform.providers.ViaProviders;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

@Getter
public class Protocol1_12_2To1_13 extends BackwardsProtocol {

    private BlockItemPackets1_13 blockItemPackets;

    static {
        BackwardsMappings.init();
        PaintingMapping.init();
    }

    @Override
    protected void registerPackets() {
        (blockItemPackets = new BlockItemPackets1_13()).register(this);
        new EntityPackets1_13().register(this);
        new PlayerPacket1_13().register(this);
        new SoundPackets1_13().register(this);

        // Thanks to  https://wiki.vg/index.php?title=Pre-release_protocol&oldid=14150

        out(State.PLAY, 0x11, -1, cancel()); // Declare Commands TODO NEW
        out(State.PLAY, 0x12, 0x11); // Confirm Transaction (clientbound)
        out(State.PLAY, 0x13, 0x12); // Close Window (clientbound)
        out(State.PLAY, 0x16, 0x15); // Window Property
        out(State.PLAY, 0x1C, 0x1B); // Entity Status
        out(State.PLAY, 0x1D, -1, cancel()); // NBT Query Response (client won't send a request, so the server should not answer)
        out(State.PLAY, 0x1E, 0x1C); // Explosion
        out(State.PLAY, 0x1F, 0x1D); // Unload Chunk
        out(State.PLAY, 0x20, 0x1E); // Change Game State
        out(State.PLAY, 0x21, 0x1F); // Keep Alive (clientbound)
        out(State.PLAY, 0x27, 0x25); // Entity
        out(State.PLAY, 0x28, 0x26); // Entity Relative Move
        out(State.PLAY, 0x29, 0x27); // Entity Look And Relative Move
        out(State.PLAY, 0x2A, 0x28); // Entity Look
        out(State.PLAY, 0x2B, 0x29); // Vehicle Move (clientbound)
        out(State.PLAY, 0x2C, 0x2A); //	Open Sign Editor
        out(State.PLAY, 0x2D, 0x2B, cancel()); // Craft Recipe Response TODO MODIFIED
        out(State.PLAY, 0x2E, 0x2C); // Player Abilities (clientbound)
        out(State.PLAY, 0x33, 0x30); // Use Bed
        out(State.PLAY, 0x34, 0x31, cancel()); // Unlock Recipes TODO MODIFIED
        out(State.PLAY, 0x36, 0x33); // Remove Entity Effect
        out(State.PLAY, 0x37, 0x34); // Resource Pack Send
        out(State.PLAY, 0x39, 0x36); // Entity Head Look
        out(State.PLAY, 0x3A, 0x37); // Select Advancement Tab
        out(State.PLAY, 0x3B, 0x38); // World Border
        out(State.PLAY, 0x3C, 0x39); // Camera
        out(State.PLAY, 0x3D, 0x3A); // Held Item Change (clientbound)
        out(State.PLAY, 0x3E, 0x3B); // Display Scoreboard
        out(State.PLAY, 0x40, 0x3D); // Attach Entity
        out(State.PLAY, 0x41, 0x3E); // Entity Velocity
        out(State.PLAY, 0x43, 0x40); // Set Experience
        out(State.PLAY, 0x44, 0x41); // Update Health
        out(State.PLAY, 0x46, 0x43); //	Set Passengers
        out(State.PLAY, 0x48, 0x45); // Update Score
        out(State.PLAY, 0x49, 0x46); // Spawn Position
        out(State.PLAY, 0x4A, 0x47); // Time Update
        out(State.PLAY, 0x4F, 0x4B); // Collect Item
        out(State.PLAY, 0x50, 0x4C); // Entity Teleport
        out(State.PLAY, 0x51, 0x4D, cancel()); // Advancements
        out(State.PLAY, 0x52, 0x4E); // Entity Properties
        out(State.PLAY, 0x53, 0x4F); // Entity Effect
        out(State.PLAY, 0x54, -1, cancel()); // Declare Recipes TODO NEW
        out(State.PLAY, 0x55, -1, cancel()); // Tags (the client won't need this)

        in(State.PLAY, 0x06, 0x05); // Confirm Transaction (serverbound)
        in(State.PLAY, 0x07, 0x06); // Enchant Item
        in(State.PLAY, 0x09, 0x08); // Close Window (serverbound)
        in(State.PLAY, 0x0D, 0x0A); // Use Entity
        in(State.PLAY, 0x0E, 0x0B); // Keep Alive (serverbound)
        in(State.PLAY, 0x0F, 0x0C); // Player
        in(State.PLAY, 0x12, 0x0F); // Player Look
        in(State.PLAY, 0x14, 0x11); // Steer Boat
        in(State.PLAY, 0x16, 0x12, cancel()); // Craft Recipe Request TODO MODIFIED
        in(State.PLAY, 0x17, 0x13); // Player Abilities (serverbound)
        in(State.PLAY, 0x18, 0x14); // Player Digging
        in(State.PLAY, 0x19, 0x15); // Entity Action
        in(State.PLAY, 0x1A, 0x16); // Steer Vehicle
        in(State.PLAY, 0x1B, 0x17, cancel()); // Recipe Book Data TODO MODIFIED
        in(State.PLAY, 0x1D, 0x18); // Resource Pack Status
        in(State.PLAY, 0x1E, 0x19); // Advancement Tab
        //in(State.PLAY, 0x1F, -1); // Select Trade (the client won't send this)
        //in(State.PLAY, 0x20, -1); // Set Beacon Effect (the client won't send this)
        in(State.PLAY, 0x21, 0x1A); // Held Item Change (serverbound)
        in(State.PLAY, 0x26, 0x1C); // Update Sign
        in(State.PLAY, 0x27, 0x1D); // Animation (serverbound)
        in(State.PLAY, 0x28, 0x1E); // Spectate
        in(State.PLAY, 0x29, 0x1F); // Player Block Placement
        in(State.PLAY, 0x2A, 0x20); // Use Item

        // Handle translation key changes

        out(State.LOGIN, 0x00, 0x00, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> wrapper.write(Type.STRING, TranslationRewriter.processTranslate(wrapper.read(Type.STRING))));
            }
        });

        // Bossbar
        out(State.LOGIN, 0x0C, 0x0C, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UUID);
                map(Type.VAR_INT);
                handler(wrapper -> {
                    int action = wrapper.get(Type.VAR_INT, 0);
                    if (action == 0 || action == 3) {
                        wrapper.write(Type.STRING, TranslationRewriter.processTranslate(wrapper.read(Type.STRING)));
                    }
                });
            }
        });

        // Chat Message
        out(State.PLAY, 0x0E, 0x0F, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> wrapper.write(Type.STRING, TranslationRewriter.processTranslate(wrapper.read(Type.STRING))));
            }
        });

        // Open Window
        out(State.PLAY, 0x14, 0x13, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE); // Id
                map(Type.STRING); // Window Type
                handler(wrapper -> wrapper.write(Type.STRING, TranslationRewriter.processTranslate(wrapper.read(Type.STRING))));
            }
        });

        // Disconnect
        out(State.PLAY, 0x1B, 0x1A, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> wrapper.write(Type.STRING, TranslationRewriter.processTranslate(wrapper.read(Type.STRING))));
            }
        });

        // Combat Event
        out(State.PLAY, 0x2F, 0x2D, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    if (wrapper.passthrough(Type.VAR_INT) == 2) {
                        wrapper.passthrough(Type.VAR_INT);
                        wrapper.passthrough(Type.INT);
                        wrapper.write(Type.STRING, TranslationRewriter.processTranslate(wrapper.read(Type.STRING)));
                    }
                });
            }
        });

        // Title
        out(State.PLAY, 0x4B, 0x48, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    int action = wrapper.passthrough(Type.VAR_INT);
                    if (action >= 0 && action <= 2) {
                        wrapper.write(Type.STRING, TranslationRewriter.processTranslate(wrapper.read(Type.STRING)));
                    }
                });
            }
        });

        // Player List Header And Footer
        out(State.PLAY, 0x4E, 0x4A, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    wrapper.write(Type.STRING, TranslationRewriter.processTranslate(wrapper.read(Type.STRING)));
                    wrapper.write(Type.STRING, TranslationRewriter.processTranslate(wrapper.read(Type.STRING)));
                });
            }
        });
    }

    @Override
    public void init(UserConnection user) {
        // Register ClientWorld
        if (!user.has(ClientWorld.class))
            user.put(new ClientWorld(user));

        // Register EntityTracker if it doesn't exist yet.
        if (!user.has(EntityTracker.class))
            user.put(new EntityTracker(user));

        // Init protocol in EntityTracker
        user.get(EntityTracker.class).initProtocol(this);

        // Register Block Storage
        if (!user.has(BackwardsBlockStorage.class))
            user.put(new BackwardsBlockStorage(user));
        // Register Block Storage
        if (!user.has(TabCompleteStorage.class))
            user.put(new TabCompleteStorage(user));

        if (ViaBackwards.getConfig().isFix1_13FacePlayer() && !user.has(PlayerPositionStorage1_13.class)) {
            user.put(new PlayerPositionStorage1_13(user));
        }
    }

    @Override
    protected void register(ViaProviders providers) {
        providers.register(BackwardsBlockEntityProvider.class, new BackwardsBlockEntityProvider());
    }

    public PacketRemapper cancel() {
        return new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(PacketWrapper::cancel);
            }
        };
    }
}
