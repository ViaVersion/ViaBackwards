package nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.packets;

import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.protocol.protocol1_13_2to1_14.Protocol1_13_2To1_14;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class PlayerPackets1_14 extends Rewriter<Protocol1_13_2To1_14> {
    @Override
    protected void registerPackets(Protocol1_13_2To1_14 protocol) {

        // Server Difficulty
        protocol.registerOutgoing(State.PLAY, 0x0D, 0x0D, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UNSIGNED_BYTE);
                map(Type.BOOLEAN, Type.NOTHING); // Locked
            }
        });

        // Open Sign Editor
        protocol.registerOutgoing(State.PLAY, 0x2D, 0x2C, new PacketRemapper() { // c
            @Override
            public void registerMap() {
                map(Type.POSITION1_14, Type.POSITION);
            }
        });

        // Query Block NBT
        protocol.registerIncoming(State.PLAY, 0x01, 0x01, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.POSITION, Type.POSITION1_14);
            }
        });

        // Edit Book
        protocol.registerIncoming(State.PLAY, 0x0c, 0x0B, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Protocol1_13_2To1_14.blockItem.handleItemToServer(wrapper.passthrough(Type.FLAT_VAR_INT_ITEM));
                    }
                });
            }
        });

        // Player Digging
        protocol.registerIncoming(State.PLAY, 0x1a, 0x18, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.POSITION, Type.POSITION1_14);
                map(Type.BYTE);
            }
        });

        // Recipe Book Data
        protocol.registerIncoming(State.PLAY, 0x1d, 0x1B, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int type = wrapper.get(Type.VAR_INT, 0);
                        if (type == 0) {
                            wrapper.passthrough(Type.STRING);
                        } else if (type == 1) {
                            wrapper.passthrough(Type.BOOLEAN); // Crafting Recipe Book Open
                            wrapper.passthrough(Type.BOOLEAN); // Crafting Recipe Filter Active
                            wrapper.passthrough(Type.BOOLEAN); // Smelting Recipe Book Open
                            wrapper.passthrough(Type.BOOLEAN); // Smelting Recipe Filter Active

                            // Unknown new booleans
                            wrapper.read(Type.BOOLEAN);
                            wrapper.read(Type.BOOLEAN);
                            wrapper.read(Type.BOOLEAN);
                            wrapper.read(Type.BOOLEAN);
                        }
                    }
                });
            }
        });

        // Update Command Block
        protocol.registerIncoming(State.PLAY, 0x24, 0x22, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION, Type.POSITION1_14);
            }
        });

        // Update Structure Block
        protocol.registerIncoming(State.PLAY, 0x27, 0x25, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION, Type.POSITION1_14);
            }
        });

        // Update Sign
        protocol.registerIncoming(State.PLAY, 0x28, 0x26, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION, Type.POSITION1_14);
            }
        });

        // Player Block Placement
        protocol.registerIncoming(State.PLAY, 0x2b, 0x29, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        Position position = wrapper.read(Type.POSITION);
                        int face = wrapper.read(Type.VAR_INT);
                        int hand = wrapper.read(Type.VAR_INT);
                        float x = wrapper.read(Type.FLOAT);
                        float y = wrapper.read(Type.FLOAT);
                        float z = wrapper.read(Type.FLOAT);

                        wrapper.write(Type.VAR_INT, hand);
                        wrapper.write(Type.POSITION1_14, position);
                        wrapper.write(Type.VAR_INT, face);
                        wrapper.write(Type.FLOAT, x);
                        wrapper.write(Type.FLOAT, y);
                        wrapper.write(Type.FLOAT, z);
                        wrapper.write(Type.BOOLEAN, false); // Inside block
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {

    }
}
