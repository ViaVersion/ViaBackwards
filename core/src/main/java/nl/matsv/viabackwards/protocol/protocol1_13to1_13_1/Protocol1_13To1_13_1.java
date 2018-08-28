package nl.matsv.viabackwards.protocol.protocol1_13to1_13_1;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets.EntityPackets;
import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets.InventoryPackets;
import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets.WorldPackets;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueTransformer;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.storage.EntityTracker;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class Protocol1_13To1_13_1 extends BackwardsProtocol {

    @Override
    protected void registerPackets() {
        EntityPackets.register(this);
        InventoryPackets.register(this);
        WorldPackets.register(this);

        //Tab complete
        registerIncoming(State.PLAY, 0x05, 0x05, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.STRING, new ValueTransformer<String, String>(Type.STRING) {
                    @Override
                    public String transform(PacketWrapper wrapper, String inputValue) {
                        // 1.13 starts sending slash at start, so we remove it for compatibility
                        return !inputValue.startsWith("/") ? "/" + inputValue : inputValue;
                    }
                });
            }
        });

        //Edit Book
        registerIncoming(State.PLAY, 0x0B, 0x0B, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.FLAT_ITEM);
                map(Type.BOOLEAN);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        wrapper.write(Type.VAR_INT, 0);
                    }
                });
            }
        });

    }

    public static int getNewBlockStateId(int blockId) {
        if (blockId > 8590) {
            blockId -= 17;
        } else if (blockId > 8573) {
            blockId = 0;  //TODO replace new blocks
        } else if (blockId > 8479) {
            blockId -= 16;
        } else if (blockId > 8469 && blockId % 2 == 0) {
            if (blockId % 2 == 0) {
                blockId = 8459 + (blockId - 8470) / 2;
            } else {
                blockId = 0;  //TODO replace new blocks
            }
        } else if (blockId > 8463) {
            blockId = 0;   //TODO replace new blocks
        } else if (blockId > 1127) {
            blockId -= 1;
        } else if (blockId == 1127) {
            blockId = 1126;
        }

        return blockId;
    }

    public static int getNewBlockId(int blockId) {
        if (blockId > 565) {
            blockId -= 5;
        } else if (blockId > 561) {
            blockId = 0;  //TODO replace new blocks
        }

        return blockId;
    }

    @Override
    public void init(UserConnection userConnection) {
        userConnection.put(new EntityTracker(userConnection));
        if (!userConnection.has(ClientWorld.class))
            userConnection.put(new ClientWorld(userConnection));
    }
}
