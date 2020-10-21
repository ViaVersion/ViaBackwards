package nl.matsv.viabackwards.protocol.protocol1_16_3to1_16_4;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.protocol.protocol1_16_3to1_16_4.storage.PlayerHandStorage;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.ClientboundPackets1_16_2;
import us.myles.ViaVersion.protocols.protocol1_16_2to1_16_1.ServerboundPackets1_16_2;

public class Protocol1_16_3To1_16_4 extends BackwardsProtocol<ClientboundPackets1_16_2, ClientboundPackets1_16_2, ServerboundPackets1_16_2, ServerboundPackets1_16_2> {

    public Protocol1_16_3To1_16_4() {
        super(ClientboundPackets1_16_2.class, ClientboundPackets1_16_2.class, ServerboundPackets1_16_2.class, ServerboundPackets1_16_2.class);
    }

    @Override
    protected void registerPackets() {
        registerIncoming(ServerboundPackets1_16_2.EDIT_BOOK, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.FLAT_VAR_INT_ITEM);
                map(Type.BOOLEAN);
                handler(wrapper -> {
                    int slot = wrapper.read(Type.VAR_INT);
                    if (slot == 1) {
                        wrapper.write(Type.VAR_INT, 40); // offhand
                    } else {
                        wrapper.write(Type.VAR_INT, wrapper.user().get(PlayerHandStorage.class).getCurrentHand());
                    }
                });
            }
        });

        registerIncoming(ServerboundPackets1_16_2.HELD_ITEM_CHANGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    short slot = wrapper.passthrough(Type.SHORT);
                    wrapper.user().get(PlayerHandStorage.class).setCurrentHand(slot);
                });
            }
        });
    }

    @Override
    public void init(UserConnection user) {
        user.put(new PlayerHandStorage(user));
    }
}
