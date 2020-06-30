package nl.matsv.viabackwards.protocol.protocol1_13to1_13_1;

import nl.matsv.viabackwards.api.BackwardsProtocol;
import nl.matsv.viabackwards.api.entities.storage.EntityTracker;
import nl.matsv.viabackwards.api.rewriters.TranslatableRewriter;
import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets.EntityPackets1_13_1;
import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets.InventoryPackets1_13_1;
import nl.matsv.viabackwards.protocol.protocol1_13to1_13_1.packets.WorldPackets1_13_1;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueTransformer;
import us.myles.ViaVersion.api.rewriters.TagRewriter;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;
import us.myles.ViaVersion.protocols.protocol1_9_3to1_9_1_2.storage.ClientWorld;

public class Protocol1_13To1_13_1 extends BackwardsProtocol<ClientboundPackets1_13, ClientboundPackets1_13, ServerboundPackets1_13, ServerboundPackets1_13> {

    public Protocol1_13To1_13_1() {
        super(ClientboundPackets1_13.class, ClientboundPackets1_13.class, ServerboundPackets1_13.class, ServerboundPackets1_13.class);
    }

    @Override
    protected void registerPackets() {
        new EntityPackets1_13_1(this).register();
        InventoryPackets1_13_1.register(this);
        WorldPackets1_13_1.register(this);

        TranslatableRewriter translatableRewriter = new TranslatableRewriter(this);
        translatableRewriter.registerChatMessage(ClientboundPackets1_13.CHAT_MESSAGE);
        translatableRewriter.registerLegacyOpenWindow(ClientboundPackets1_13.OPEN_WINDOW);
        translatableRewriter.registerCombatEvent(ClientboundPackets1_13.COMBAT_EVENT);
        translatableRewriter.registerDisconnect(ClientboundPackets1_13.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_13.TAB_LIST);
        translatableRewriter.registerTitle(ClientboundPackets1_13.TITLE);
        translatableRewriter.registerPing();

        registerIncoming(ServerboundPackets1_13.TAB_COMPLETE, new PacketRemapper() {
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

        registerIncoming(ServerboundPackets1_13.EDIT_BOOK, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.FLAT_ITEM);
                map(Type.BOOLEAN);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        InventoryPackets1_13_1.toServer(wrapper.get(Type.FLAT_ITEM, 0));
                        wrapper.write(Type.VAR_INT, 0);
                    }
                });
            }
        });

        registerOutgoing(ClientboundPackets1_13.TAB_COMPLETE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Transaction id
                map(Type.VAR_INT); // Start
                map(Type.VAR_INT); // Length
                map(Type.VAR_INT); // Count
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int start = wrapper.get(Type.VAR_INT, 1);
                        wrapper.set(Type.VAR_INT, 1, start - 1); // Offset by +1 to take into account / at beginning
                        // Passthrough suggestions
                        int count = wrapper.get(Type.VAR_INT, 3);
                        for (int i = 0; i < count; i++) {
                            wrapper.passthrough(Type.STRING);
                            boolean hasTooltip = wrapper.passthrough(Type.BOOLEAN);
                            if (hasTooltip) {
                                wrapper.passthrough(Type.STRING); // JSON Tooltip
                            }
                        }
                    }
                });
            }
        });

        registerOutgoing(ClientboundPackets1_13.BOSSBAR, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UUID);
                map(Type.VAR_INT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int action = wrapper.get(Type.VAR_INT, 0);
                        if (action == 0 || action == 3) {
                            translatableRewriter.processText(wrapper.passthrough(Type.COMPONENT));
                            if (action == 0) {
                                wrapper.passthrough(Type.FLOAT);
                                wrapper.passthrough(Type.VAR_INT);
                                wrapper.passthrough(Type.VAR_INT);
                                short flags = wrapper.read(Type.UNSIGNED_BYTE);
                                if ((flags & 0x04) != 0) flags |= 0x02;
                                wrapper.write(Type.UNSIGNED_BYTE, flags);
                            }
                        }
                    }
                });
            }
        });

        registerOutgoing(ClientboundPackets1_13.ADVANCEMENTS, new PacketRemapper() {
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
                                wrapper.passthrough(Type.COMPONENT); // Title
                                wrapper.passthrough(Type.COMPONENT); // Description
                                Item icon = wrapper.passthrough(Type.FLAT_ITEM);
                                InventoryPackets1_13_1.toClient(icon);
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

        new TagRewriter(this, Protocol1_13To1_13_1::getNewBlockId,
                InventoryPackets1_13_1::getOldItemId, null).register(ClientboundPackets1_13.TAGS);
    }

    public static int getNewBlockStateId(int blockId) {
        if (blockId > 8590) {
            blockId -= 17;
        } else if (blockId > 8588) {
            blockId = 8573;
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
            blockId = 0;  // Replacements not needed
        }

        return blockId;
    }

    @Override
    public void init(UserConnection user) {
        // Register EntityTracker if it doesn't exist yet.
        if (!user.has(EntityTracker.class))
            user.put(new EntityTracker(user));

        // Init protocol in EntityTracker
        user.get(EntityTracker.class).initProtocol(this);

        if (!user.has(ClientWorld.class))
            user.put(new ClientWorld(user));
    }
}
