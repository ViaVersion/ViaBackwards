package nl.matsv.viabackwards.protocol.protocol1_13_1to1_13_2;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.protocol.protocol1_13_1to1_13_2.packets.EntityPackets1_13_2;
import nl.matsv.viabackwards.protocol.protocol1_13_1to1_13_2.packets.InventoryPackets1_13_2;
import nl.matsv.viabackwards.protocol.protocol1_13_1to1_13_2.packets.WorldPackets1_13_2;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class Protocol1_13_1To1_13_2 extends BackwardsProtocol {

    @Override
    protected void registerPackets() {
        InventoryPackets1_13_2.register(this);
        WorldPackets1_13_2.register(this);
        EntityPackets1_13_2.register(this);

        //Edit Book
        registerIncoming(State.PLAY, 0x0B, 0x0B, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.FLAT_ITEM, Type.FLAT_VAR_INT_ITEM);
            }
        });

        // Advancements
        registerOutgoing(State.PLAY, 0x51, 0x51, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.passthrough(Type.BOOLEAN); // Reset/clear
                        int size = wrapper.passthrough(Type.VAR_INT); // Mapping size

                        for (int i = 0; i < size; i++) {
                            wrapper.passthrough(Type.STRING); // Identifier

                            // Parent
                            if (wrapper.passthrough(Type.BOOLEAN))
                                wrapper.passthrough(Type.STRING);

                            // Display data
                            if (wrapper.passthrough(Type.BOOLEAN)) {
                                wrapper.passthrough(Type.STRING); // Title
                                wrapper.passthrough(Type.STRING); // Description
                                Item icon = wrapper.read(Type.FLAT_VAR_INT_ITEM);
                                wrapper.write(Type.FLAT_ITEM, icon);
                                wrapper.passthrough(Type.VAR_INT); // Frame type
                                int flags = wrapper.passthrough(Type.INT); // Flags
                                if ((flags & 1) != 0)
                                    wrapper.passthrough(Type.STRING); // Background texture
                                wrapper.passthrough(Type.FLOAT); // X
                                wrapper.passthrough(Type.FLOAT); // Y
                            }

                            wrapper.passthrough(Type.STRING_ARRAY); // Criteria

                            int arrayLength = wrapper.passthrough(Type.VAR_INT);
                            for (int array = 0; array < arrayLength; array++) {
                                wrapper.passthrough(Type.STRING_ARRAY); // String array
                            }
                        }
                    }
                });
            }
        });
    }

    @Override
    public void init(UserConnection userConnection) {

    }
}
