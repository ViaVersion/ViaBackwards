package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.TabCompleteStorage;
import nl.matsv.viabackwards.utils.ChatUtil;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueCreator;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.packets.InventoryPackets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerPacket1_13 extends Rewriter<Protocol1_12_2To1_13> {
    @Override
    protected void registerPackets(Protocol1_12_2To1_13 protocol) {
        // Login Plugin Request
        protocol.out(State.LOGIN, 0x04, -1, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper packetWrapper) throws Exception {
                        packetWrapper.cancel();
                        packetWrapper.create(0x02, new ValueCreator() { // Plugin response
                            @Override
                            public void write(PacketWrapper newWrapper) throws Exception {
                                newWrapper.write(Type.VAR_INT, packetWrapper.read(Type.VAR_INT)); // Packet id
                                newWrapper.write(Type.BOOLEAN, false); // Success
                            }
                        }).sendToServer(Protocol1_12_2To1_13.class);
                    }
                });
            }
        });

        //Plugin Message
        protocol.out(State.PLAY, 0x19, 0x18, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        String channel = wrapper.read(Type.STRING);
                        if (channel.equals("minecraft:trader_list")) {
                            wrapper.write(Type.STRING, "MC|TrList");
                            wrapper.passthrough(Type.INT); //Passthrough Window ID

                            int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                            for (int i = 0; i < size; i++) {
                                //Input Item
                                Item input = wrapper.read(Type.FLAT_ITEM);
                                wrapper.write(Type.ITEM, getProtocol().getBlockItemPackets().handleItemToClient(input));
                                //Output Item
                                Item output = wrapper.read(Type.FLAT_ITEM);
                                wrapper.write(Type.ITEM, getProtocol().getBlockItemPackets().handleItemToClient(output));

                                boolean secondItem = wrapper.passthrough(Type.BOOLEAN); //Has second item
                                if (secondItem) {
                                    //Second Item
                                    Item second = wrapper.read(Type.FLAT_ITEM);
                                    wrapper.write(Type.ITEM, getProtocol().getBlockItemPackets().handleItemToClient(second));
                                }

                                wrapper.passthrough(Type.BOOLEAN); //Trade disabled
                                wrapper.passthrough(Type.INT); //Number of tools uses
                                wrapper.passthrough(Type.INT); //Maximum number of trade uses
                            }
                        } else {
                            String oldChannel = InventoryPackets.getOldPluginChannelId(channel);
                            if (oldChannel == null) {
                                ViaBackwards.getPlatform().getLogger().warning("Could not find old channel for " + channel);
                                wrapper.cancel();
                                return;
                            }
                            wrapper.write(Type.STRING, oldChannel);
                        }
                    }
                });
            }
        });

        // Player List Item
        protocol.out(State.PLAY, 0x30, 0x2E, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper packetWrapper) throws Exception {
                        TabCompleteStorage storage = packetWrapper.user().get(TabCompleteStorage.class);
                        int action = packetWrapper.passthrough(Type.VAR_INT);
                        int nPlayers = packetWrapper.passthrough(Type.VAR_INT);
                        for (int i = 0; i < nPlayers; i++) {
                            UUID uuid = packetWrapper.passthrough(Type.UUID);
                            if (action == 0) { // Add
                                String name = packetWrapper.passthrough(Type.STRING);
                                storage.usernames.put(uuid, name);
                                int nProperties = packetWrapper.passthrough(Type.VAR_INT);
                                for (int j = 0; j < nProperties; j++) {
                                    packetWrapper.passthrough(Type.STRING);
                                    packetWrapper.passthrough(Type.STRING);
                                    if (packetWrapper.passthrough(Type.BOOLEAN)) {
                                        packetWrapper.passthrough(Type.STRING);
                                    }
                                }
                                packetWrapper.passthrough(Type.VAR_INT);
                                packetWrapper.passthrough(Type.VAR_INT);
                                if (packetWrapper.passthrough(Type.BOOLEAN)) {
                                    packetWrapper.passthrough(Type.STRING);
                                }
                            } else if (action == 1) { // Update Game Mode
                                packetWrapper.passthrough(Type.VAR_INT);
                            } else if (action == 2) { // Update Ping
                                packetWrapper.passthrough(Type.VAR_INT);
                            } else if (action == 3) { // Update Display Name
                                if (packetWrapper.passthrough(Type.BOOLEAN)) {
                                    packetWrapper.passthrough(Type.STRING);
                                }
                            } else if (action == 4) { // Remove Player
                                storage.usernames.remove(uuid);
                            }
                        }
                    }
                });
            }
        });

        //Scoreboard Objective
        protocol.out(State.PLAY, 0x45, 0x42, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING);
                map(Type.BYTE);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        byte mode = wrapper.get(Type.BYTE, 0);
                        if (mode == 0 || mode == 2) {
                            String value = wrapper.read(Type.STRING);
                            value = ChatRewriter.jsonTextToLegacy(value);
                            if (value.length() > 32) value = value.substring(0, 32);
                            wrapper.write(Type.STRING, value);
                            int type = wrapper.read(Type.VAR_INT);
                            wrapper.write(Type.STRING, type == 1 ? "hearts" : "integer");
                        }
                    }
                });
            }
        });

        //Teams
        protocol.out(State.PLAY, 0x47, 0x44, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING);
                map(Type.BYTE);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        byte action = wrapper.get(Type.BYTE, 0);
                        if (action == 0 || action == 2) {
                            String displayName = wrapper.read(Type.STRING);
                            displayName = ChatRewriter.jsonTextToLegacy(displayName);
                            displayName = ChatUtil.removeUnusedColor(displayName, 'f');
                            if (displayName.length() > 32) displayName = displayName.substring(0, 32);
                            wrapper.write(Type.STRING, displayName);

                            byte flags = wrapper.read(Type.BYTE);
                            String nameTagVisibility = wrapper.read(Type.STRING);
                            String collisionRule = wrapper.read(Type.STRING);

                            int colour = wrapper.read(Type.VAR_INT);
                            if (colour == 21) {
                                colour = -1;
                            }

                            //TODO team color/prefix handling changed from 1.12.2 to 1.13 and to 1.13.1 again afaik
                            String prefix = wrapper.read(Type.STRING);
                            String suffix = wrapper.read(Type.STRING);
                            prefix = prefix == null || prefix.equals("null") ? "" : ChatRewriter.jsonTextToLegacy(prefix);
                            prefix += "§" + (colour > -1 && colour <= 15 ? Integer.toHexString(colour) : "r");
                            prefix = ChatUtil.removeUnusedColor(prefix, 'f', true);
                            if (prefix.length() > 16) prefix = prefix.substring(0, 16);
                            if (prefix.endsWith("§")) prefix = prefix.substring(0, prefix.length() - 1);
                            suffix = suffix == null || suffix.equals("null") ? "" : ChatRewriter.jsonTextToLegacy(suffix);
                            suffix = ChatUtil.removeUnusedColor(suffix, 'f');
                            if (suffix.endsWith("§")) suffix = suffix.substring(0, suffix.length() - 1);
                            wrapper.write(Type.STRING, prefix);
                            wrapper.write(Type.STRING, suffix);

                            wrapper.write(Type.BYTE, flags);
                            wrapper.write(Type.STRING, nameTagVisibility);
                            wrapper.write(Type.STRING, collisionRule);

                            wrapper.write(Type.BYTE, (byte) colour);
                        }

                        if (action == 0 || action == 3 || action == 4) {
                            wrapper.passthrough(Type.STRING_ARRAY); //Entities
                        }
                    }
                });
            }
        });

        // Tab-Complete (clientbound) TODO MODIFIED
        protocol.out(State.PLAY, 0x10, 0x0E, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        TabCompleteStorage storage = wrapper.user().get(TabCompleteStorage.class);
                        if (storage.lastRequest == null) {
                            wrapper.cancel();
                            return;
                        }
                        if (storage.lastId != wrapper.read(Type.VAR_INT)) wrapper.cancel();
                        int start = wrapper.read(Type.VAR_INT);
                        int length = wrapper.read(Type.VAR_INT);

                        int lastRequestPartIndex = storage.lastRequest.lastIndexOf(' ') + 1;
                        if (lastRequestPartIndex != start) wrapper.cancel(); // Client only replaces after space

                        if (length != storage.lastRequest.length() - lastRequestPartIndex) {
                            wrapper.cancel(); // We can't set the length in previous versions
                        }

                        int count = wrapper.passthrough(Type.VAR_INT);
                        for (int i = 0; i < count; i++) {
                            String match = wrapper.read(Type.STRING);
                            wrapper.write(Type.STRING, (start == 0 && !storage.lastAssumeCommand ? "/" : "") + match);
                            // Ignore tooltip
                            if (wrapper.read(Type.BOOLEAN)) {
                                wrapper.read(Type.STRING);
                            }
                        }
                    }
                });
            }
        });

        // Tab-Complete (serverbound)
        protocol.in(State.PLAY, 0x05, 0x01, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        TabCompleteStorage storage = wrapper.user().get(TabCompleteStorage.class);
                        int id = ThreadLocalRandom.current().nextInt();
                        wrapper.write(Type.VAR_INT, id);

                        String command = wrapper.read(Type.STRING);
                        boolean assumeCommand = wrapper.read(Type.BOOLEAN);
                        wrapper.read(Type.OPTIONAL_POSITION);

                        if (!assumeCommand) {
                            if (command.startsWith("/")) {
                                command = command.substring(1);
                            } else {
                                wrapper.cancel();
                                PacketWrapper response = wrapper.create(0xE);
                                List<String> usernames = new ArrayList<>();
                                for (String value : storage.usernames.values()) {
                                    if (value.toLowerCase().startsWith(command.substring(command.lastIndexOf(' ') + 1).toLowerCase())) {
                                        usernames.add(value);
                                    }
                                }
                                response.write(Type.VAR_INT, usernames.size());
                                for (String value : usernames) {
                                    response.write(Type.STRING, value);
                                }
                                response.send(protocol.getClass());
                            }
                        }

                        wrapper.write(Type.STRING, command);
                        storage.lastId = id;
                        storage.lastAssumeCommand = assumeCommand;
                        storage.lastRequest = command;
                    }
                });
            }
        });

        //Plugin Message
        protocol.in(State.PLAY, 0x0A, 0x09, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        String channel = wrapper.read(Type.STRING);
                        if (channel.equals("MC|BSign") || channel.equals("MC|BEdit")) {
                            wrapper.setId(0x0B);
                            Item book = wrapper.read(Type.ITEM);
                            wrapper.write(Type.FLAT_ITEM, getProtocol().getBlockItemPackets().handleItemToServer(book));
                            boolean signing = channel.equals("MC|BSign");
                            wrapper.write(Type.BOOLEAN, signing);
                        } else if (channel.equals("MC|ItemName")) {
                            wrapper.setId(0x1C);
                        } else if (channel.equals("MC|AdvCmd")) {
                            byte type = wrapper.read(Type.BYTE);
                            if (type == 0) {
                                //Information from https://wiki.vg/index.php?title=Plugin_channels&oldid=14089
                                //The Notchain client only uses this for command block minecarts and uses MC|AutoCmd for blocks, but the Notchian server still accepts it for either.
                                //Maybe older versions used this and we need to implement this? The issues is that we would have to save the command block types
                                wrapper.setId(0x22);
                                wrapper.cancel();
                                ViaBackwards.getPlatform().getLogger().warning("Client send MC|AdvCmd custom payload to update command block, weird!");
                            } else if (type == 1) {
                                wrapper.setId(0x23);
                                wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Entity Id
                                wrapper.passthrough(Type.STRING); //Command
                                wrapper.passthrough(Type.BOOLEAN); //Track Output

                            } else {
                                wrapper.cancel();
                            }
                        } else if (channel.equals("MC|AutoCmd")) {
                            wrapper.setId(0x22);
                            Integer x = wrapper.read(Type.INT);
                            Integer y = wrapper.read(Type.INT);
                            Integer z = wrapper.read(Type.INT);
                            wrapper.write(Type.POSITION, new Position(x.longValue(), y.longValue(), z.longValue()));
                            wrapper.passthrough(Type.STRING);  //Command
                            byte flags = 0;
                            if (wrapper.read(Type.BOOLEAN)) flags |= 0x01; //Track Output
                            String mode = wrapper.read(Type.STRING);
                            int modeId = mode.equals("SEQUENCE") ? 0 : mode.equals("AUTO") ? 1 : 2;
                            wrapper.write(Type.VAR_INT, modeId);
                            if (wrapper.read(Type.BOOLEAN)) flags |= 0x02; //Is conditional
                            if (wrapper.read(Type.BOOLEAN)) flags |= 0x04; //Automatic
                        } else if (channel.equals("MC|Struct")) {
                            wrapper.setId(0x25);
                            Integer x = wrapper.read(Type.INT);
                            Integer y = wrapper.read(Type.INT);
                            Integer z = wrapper.read(Type.INT);
                            wrapper.write(Type.POSITION, new Position(x.longValue(), y.longValue(), z.longValue()));
                            wrapper.write(Type.VAR_INT, wrapper.read(Type.BYTE) - 1);
                            String mode = wrapper.read(Type.STRING);
                            int modeId = mode.equals("SAVE") ? 0 : mode.equals("LOAD") ? 1 : mode.equals("CORNER") ? 2 : 3;
                            wrapper.write(Type.VAR_INT, modeId);
                            wrapper.passthrough(Type.STRING); //Name
                            wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Offset X
                            wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Offset Y
                            wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Offset Z
                            wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Size X
                            wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Size Y
                            wrapper.write(Type.BYTE, wrapper.read(Type.INT).byteValue()); //Size Z
                            String mirror = wrapper.read(Type.STRING);
                            int mirrorId = mode.equals("NONE") ? 0 : mode.equals("LEFT_RIGHT") ? 1 : 2;
                            String rotation = wrapper.read(Type.STRING);
                            int rotationId = mode.equals("NONE") ? 0 : mode.equals("CLOCKWISE_90") ? 1 : mode.equals("CLOCKWISE_180") ? 2 : 3;
                            wrapper.passthrough(Type.STRING); //Metadata
                            byte flags = 0;
                            if (wrapper.read(Type.BOOLEAN)) flags |= 0x01; //Ignore entities
                            if (wrapper.read(Type.BOOLEAN)) flags |= 0x02; //Show air
                            if (wrapper.read(Type.BOOLEAN)) flags |= 0x04; //Show bounding box
                            wrapper.passthrough(Type.FLOAT); //Integrity
                            wrapper.passthrough(Type.VAR_LONG); //Seed
                            wrapper.write(Type.BYTE, flags);
                        } else if (channel.equals("MC|Beacon")) {
                            wrapper.setId(0x20);
                            wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Primary Effect
                            wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Secondary Effect
                        } else if (channel.equals("MC|TrSel")) {
                            wrapper.setId(0x1F);
                            wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Slot
                        } else if (channel.equals("MC|PickItem")) {
                            wrapper.setId(0x15);
                        } else {
                            String newChannel = InventoryPackets.getNewPluginChannelId(channel);
                            if (newChannel == null) {
                                ViaBackwards.getPlatform().getLogger().warning("Could not find new channel for " + channel);
                                wrapper.cancel();
                                return;
                            }
                            wrapper.write(Type.STRING, newChannel);
                            //TODO REGISTER and UNREGISTER (see ViaVersion)
                            wrapper.cancel();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void registerRewrites() {

    }
}
