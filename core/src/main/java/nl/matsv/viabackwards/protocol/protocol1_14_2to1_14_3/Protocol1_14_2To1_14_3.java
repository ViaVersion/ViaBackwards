package nl.matsv.viabackwards.protocol.protocol1_14_2to1_14_3;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.ClientboundPackets1_14;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.ServerboundPackets1_14;

public class Protocol1_14_2To1_14_3 extends BackwardsProtocol<ClientboundPackets1_14, ClientboundPackets1_14, ServerboundPackets1_14, ServerboundPackets1_14> {

    public Protocol1_14_2To1_14_3() {
        super(ClientboundPackets1_14.class, ClientboundPackets1_14.class, ServerboundPackets1_14.class, ServerboundPackets1_14.class);
    }

    @Override
    protected void registerPackets() {
        registerOutgoing(ClientboundPackets1_14.TRADE_LIST, new PacketRemapper() {
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

        registerOutgoing(ClientboundPackets1_14.DECLARE_RECIPES, new PacketRemapper() {
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

                            switch (type) {
                                case "crafting_shapeless": {
                                    wrapper.passthrough(Type.STRING); // Group

                                    int ingredientsNo = wrapper.passthrough(Type.VAR_INT);
                                    for (int j = 0; j < ingredientsNo; j++) {
                                        wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                    }
                                    wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);// Result

                                    break;
                                }
                                case "crafting_shaped": {
                                    int ingredientsNo = wrapper.passthrough(Type.VAR_INT) * wrapper.passthrough(Type.VAR_INT);
                                    wrapper.passthrough(Type.STRING); // Group

                                    for (int j = 0; j < ingredientsNo; j++) {
                                        wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients
                                    }
                                    wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);// Result

                                    break;
                                }
                                case "stonecutting":
                                    wrapper.passthrough(Type.STRING); // Group?

                                    wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients

                                    wrapper.passthrough(Type.FLAT_VAR_INT_ITEM); // Result

                                    break;
                                case "smelting":
                                case "blasting":
                                case "campfire_cooking":
                                case "smoking":
                                    wrapper.passthrough(Type.STRING); // Group

                                    wrapper.passthrough(Type.FLAT_VAR_INT_ITEM_ARRAY_VAR_INT); // Ingredients

                                    wrapper.passthrough(Type.FLAT_VAR_INT_ITEM);
                                    wrapper.passthrough(Type.FLOAT); // EXP

                                    wrapper.passthrough(Type.VAR_INT); // Cooking time

                                    break;
                            }
                        }

                        wrapper.set(Type.VAR_INT, 0, size - deleted);
                    }
                });
            }
        });
    }
}
