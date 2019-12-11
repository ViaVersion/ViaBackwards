package nl.matsv.viabackwards.protocol.protocol1_14_3to1_14_4;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class Protocol1_14_3To1_14_4 extends BackwardsProtocol {

    @Override
    protected void registerPackets() {
        // Acknowledge Player Digging - added in pre4
        registerOutgoing(State.PLAY, 0x5C, 0x0B, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.POSITION1_14);
                map(Type.VAR_INT);
                handler(wrapper -> {
                    int status = wrapper.read(Type.VAR_INT);
                    boolean allGood = wrapper.read(Type.BOOLEAN);
                    if (allGood && status == 0) wrapper.cancel();
                });
            }
        });

        // Trade list
        registerOutgoing(State.PLAY, 0x27, 0x27, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.passthrough(Type.VAR_INT);
                        int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                        for (int i = 0; i < size; i++) {
                            wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                            wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                            if (wrapper.passthrough(Type.BOOLEAN)) {
                                wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                            }
                            wrapper.passthrough(Type.BOOLEAN);
                            wrapper.passthrough(Type.INT);
                            wrapper.passthrough(Type.INT);
                            wrapper.passthrough(Type.INT);
                            wrapper.passthrough(Type.INT);
                            wrapper.passthrough(Type.FLOAT);
                            wrapper.read(Type.INT); // demand value added in pre-5
                        }
                    }
                });
            }
        });
    }

    @Override
    public void init(UserConnection userConnection) {}
}
