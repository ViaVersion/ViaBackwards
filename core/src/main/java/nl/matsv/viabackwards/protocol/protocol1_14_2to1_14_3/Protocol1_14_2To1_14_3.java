package nl.matsv.viabackwards.protocol.protocol1_14_2to1_14_3;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;

public class Protocol1_14_2To1_14_3 extends BackwardsProtocol {

    @Override
    protected void registerPackets() {
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
                        }
                        wrapper.passthrough(Type.VAR_INT);
                        wrapper.passthrough(Type.VAR_INT);

                        wrapper.passthrough(Type.BOOLEAN);
                        wrapper.read(Type.BOOLEAN);
                    }
                });
            }
        });

        // Declare recipes
        registerOutgoing(State.PLAY, 0x5A, 0x5A, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int size = wrapper.passthrough(Type.VAR_INT);
                        int deleted = 0;
                        for (int i = 0; i < size; i++) {
                            String fullType = wrapper.read(Type.STRING);
                            String type = fullType.replace("minecraft:", "");
                            String id = wrapper.read(Type.STRING); // id

                            if (type.equals("crafting_special_repairitem")) {
                                deleted++;
                                continue;
                            }

                            wrapper.write(Type.STRING, fullType);
                            wrapper.write(Type.STRING, id);

                            if (type.equals("crafting_shapeless")) {
                                wrapper.passthrough(Type.STRING); // Group
                                int ingredientsNo = wrapper.passthrough(Type.VAR_INT);
                                for (int j = 0; j < ingredientsNo; j++) {
                                    wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                }
                                wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);// Result
                            } else if (type.equals("crafting_shaped")) {
                                int ingredientsNo = wrapper.passthrough(Type.VAR_INT) * wrapper.passthrough(Type.VAR_INT);
                                wrapper.passthrough(Type.STRING); // Group
                                for (int j = 0; j < ingredientsNo; j++) {
                                    wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                }
                                wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);// Result
                            } else if (type.equals("stonecutting")) {
                                wrapper.passthrough(Type.STRING); // Group?
                                wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                wrapper.passthrough(Type.FLAT_VAR_INT_ITEM); // Result
                            } else if (type.equals("smelting") || type.equals("blasting") || type.equals("campfire_cooking") || type.equals("smoking")) {
                                wrapper.passthrough(Type.STRING); // Group
                                wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                                wrapper.passthrough(Type.FLOAT); // EXP
                                wrapper.passthrough(Type.VAR_INT); // Cooking time
                            }
                        }

                        wrapper.set(Type.VAR_INT, 0, size - deleted);
                    }
                });
            }
        });
    }

    @Override
    public void init(UserConnection userConnection) {
    }
}
