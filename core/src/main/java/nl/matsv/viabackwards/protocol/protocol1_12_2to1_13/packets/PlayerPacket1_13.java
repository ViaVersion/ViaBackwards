package nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.packets;

import com.google.common.base.Joiner;
import nl.matsv.viabackwards.ViaBackwards;
import nl.matsv.viabackwards.api.rewriters.Rewriter;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.BackwardsMappings;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.data.ParticleMapping;
import nl.matsv.viabackwards.protocol.protocol1_12_2to1_13.storage.TabCompleteStorage;
import nl.matsv.viabackwards.utils.ChatUtil;
import us.myles.ViaVersion.api.PacketWrapper;
import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.minecraft.Position;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.remapper.PacketHandler;
import us.myles.ViaVersion.api.remapper.PacketRemapper;
import us.myles.ViaVersion.api.remapper.ValueCreator;
import us.myles.ViaVersion.api.type.Type;
import us.myles.ViaVersion.packets.State;
import us.myles.ViaVersion.protocols.protocol1_12_1to1_12.ServerboundPackets1_12_1;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ChatRewriter;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import us.myles.ViaVersion.protocols.protocol1_13to1_12_2.packets.InventoryPackets;
import us.myles.viaversion.libs.gson.JsonElement;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerPacket1_13 extends Rewriter<Protocol1_12_2To1_13> {

    public PlayerPacket1_13(Protocol1_12_2To1_13 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        // Login Plugin Request
        protocol.registerOutgoing(State.LOGIN, 0x04, -1, new PacketRemapper() {
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

        protocol.registerOutgoing(ClientboundPackets1_13.PLUGIN_MESSAGE, new PacketRemapper() {
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
                                if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                                    ViaBackwards.getPlatform().getLogger().warning("Ignoring outgoing plugin message with channel: " + channel);
                                }
                                wrapper.cancel();
                                return;
                            }
                            wrapper.write(Type.STRING, oldChannel);

                            if (oldChannel.equals("REGISTER") || oldChannel.equals("UNREGISTER")) {
                                String[] channels = new String(wrapper.read(Type.REMAINING_BYTES), StandardCharsets.UTF_8).split("\0");
                                List<String> rewrittenChannels = new ArrayList<>();
                                for (String s : channels) {
                                    String rewritten = InventoryPackets.getOldPluginChannelId(s);
                                    if (rewritten != null) {
                                        rewrittenChannels.add(rewritten);
                                    } else if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                                        ViaBackwards.getPlatform().getLogger().warning("Ignoring plugin channel in outgoing REGISTER: " + s);
                                    }
                                }
                                wrapper.write(Type.REMAINING_BYTES, Joiner.on('\0').join(rewrittenChannels).getBytes(StandardCharsets.UTF_8));
                            }
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.SPAWN_PARTICLE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT);      // 0 - Particle ID
                map(Type.BOOLEAN);  // 1 - Long Distance
                map(Type.FLOAT);    // 2 - X
                map(Type.FLOAT);    // 3 - Y
                map(Type.FLOAT);    // 4 - Z
                map(Type.FLOAT);    // 5 - Offset X
                map(Type.FLOAT);    // 6 - Offset Y
                map(Type.FLOAT);    // 7 - Offset Z
                map(Type.FLOAT);    // 8 - Particle Data
                map(Type.INT);      // 9 - Particle Count
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        ParticleMapping.ParticleData old = ParticleMapping.getMapping(wrapper.get(Type.INT, 0));
                        wrapper.set(Type.INT, 0, old.getHistoryId());

                        int[] data = old.rewriteData(protocol, wrapper);
                        if (data != null) {
                            if (old.getHandler().isBlockHandler() && data[0] == 0) {
                                // Cancel air block particles
                                wrapper.cancel();
                                return;
                            }

                            for (int i : data) {
                                wrapper.write(Type.VAR_INT, i);
                            }
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.PLAYER_INFO, new PacketRemapper() {
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
                                    packetWrapper.passthrough(Type.COMPONENT);
                                }
                            } else if (action == 1) { // Update Game Mode
                                packetWrapper.passthrough(Type.VAR_INT);
                            } else if (action == 2) { // Update Ping
                                packetWrapper.passthrough(Type.VAR_INT);
                            } else if (action == 3) { // Update Display Name
                                if (packetWrapper.passthrough(Type.BOOLEAN)) {
                                    packetWrapper.passthrough(Type.COMPONENT);
                                }
                            } else if (action == 4) { // Remove Player
                                storage.usernames.remove(uuid);
                            }
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.SCOREBOARD_OBJECTIVE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING);
                map(Type.BYTE);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        byte mode = wrapper.get(Type.BYTE, 0);
                        if (mode == 0 || mode == 2) {
                            String value = wrapper.read(Type.COMPONENT).toString();
                            value = ChatRewriter.jsonTextToLegacy(value);
                            if (value.length() > 32) {
                                value = value.substring(0, 32);
                            }

                            wrapper.write(Type.STRING, value);
                            int type = wrapper.read(Type.VAR_INT);
                            wrapper.write(Type.STRING, type == 1 ? "hearts" : "integer");
                        }
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.TEAMS, new PacketRemapper() {
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
                            if (displayName.length() > 32) {
                                displayName = displayName.substring(0, 32);
                            }
                            wrapper.write(Type.STRING, displayName);

                            byte flags = wrapper.read(Type.BYTE);
                            String nameTagVisibility = wrapper.read(Type.STRING);
                            String collisionRule = wrapper.read(Type.STRING);

                            int colour = wrapper.read(Type.VAR_INT);
                            if (colour == 21) {
                                colour = -1;
                            }

                            JsonElement prefixComponent = wrapper.read(Type.COMPONENT);
                            JsonElement suffixComponent = wrapper.read(Type.COMPONENT);

                            String prefix = prefixComponent == null || prefixComponent.isJsonNull() ? "" : ChatRewriter.jsonTextToLegacy(prefixComponent.toString());
                            if (ViaBackwards.getConfig().addTeamColorTo1_13Prefix()) {
                                prefix += "§" + (colour > -1 && colour <= 15 ? Integer.toHexString(colour) : "r");
                            }

                            prefix = ChatUtil.removeUnusedColor(prefix, 'f', true);
                            if (prefix.length() > 16) prefix = prefix.substring(0, 16);
                            if (prefix.endsWith("§")) prefix = prefix.substring(0, prefix.length() - 1);

                            String suffix = suffixComponent == null || suffixComponent.isJsonNull() ? "" : ChatRewriter.jsonTextToLegacy(suffixComponent.toString());
                            suffix = ChatUtil.removeUnusedColor(suffix, '\0'); // Don't remove white coloring
                            if (suffix.length() > 16) suffix = suffix.substring(0, 16);
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

        protocol.registerOutgoing(ClientboundPackets1_13.TAB_COMPLETE, new PacketRemapper() {
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

        protocol.registerIncoming(ServerboundPackets1_12_1.TAB_COMPLETE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
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
                });
            }
        });

        protocol.registerIncoming(ServerboundPackets1_12_1.PLUGIN_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    String channel = wrapper.read(Type.STRING);
                    switch (channel) {
                        case "MC|BSign":
                        case "MC|BEdit":
                            wrapper.setId(0x0B);
                            Item book = wrapper.read(Type.ITEM);
                            wrapper.write(Type.FLAT_ITEM, getProtocol().getBlockItemPackets().handleItemToServer(book));
                            boolean signing = channel.equals("MC|BSign");
                            wrapper.write(Type.BOOLEAN, signing);
                            break;
                        case "MC|ItemName":
                            wrapper.setId(0x1C);
                            break;
                        case "MC|AdvCmd":
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
                            break;
                        case "MC|AutoCmd": {
                            wrapper.setId(0x22);

                            int x = wrapper.read(Type.INT);
                            int y = wrapper.read(Type.INT);
                            int z = wrapper.read(Type.INT);

                            wrapper.write(Type.POSITION, new Position(x, (short) y, z));

                            wrapper.passthrough(Type.STRING);  //Command


                            byte flags = 0;
                            if (wrapper.read(Type.BOOLEAN)) flags |= 0x01; //Track Output

                            String mode = wrapper.read(Type.STRING);

                            int modeId = mode.equals("SEQUENCE") ? 0 : mode.equals("AUTO") ? 1 : 2;
                            wrapper.write(Type.VAR_INT, modeId);

                            if (wrapper.read(Type.BOOLEAN)) flags |= 0x02; //Is conditional
                            if (wrapper.read(Type.BOOLEAN)) flags |= 0x04; //Automatic

                            wrapper.write(Type.BYTE, flags);
                            break;
                        }
                        case "MC|Struct": {
                            wrapper.setId(0x25);
                            int x = wrapper.read(Type.INT);
                            int y = wrapper.read(Type.INT);
                            int z = wrapper.read(Type.INT);
                            wrapper.write(Type.POSITION, new Position(x, (short) y, z));
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
                            break;
                        }
                        case "MC|Beacon":
                            wrapper.setId(0x20);
                            wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Primary Effect

                            wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Secondary Effect

                            break;
                        case "MC|TrSel":
                            wrapper.setId(0x1F);
                            wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Slot

                            break;
                        case "MC|PickItem":
                            wrapper.setId(0x15);
                            break;
                        default:
                            String newChannel = InventoryPackets.getNewPluginChannelId(channel);
                            if (newChannel == null) {
                                if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                                    ViaBackwards.getPlatform().getLogger().warning("Ignoring incoming plugin message with channel: " + channel);
                                }
                                wrapper.cancel();
                                return;
                            }
                            wrapper.write(Type.STRING, newChannel);

                            if (newChannel.equals("minecraft:register") || newChannel.equals("minecraft:unregister")) {
                                String[] channels = new String(wrapper.read(Type.REMAINING_BYTES), StandardCharsets.UTF_8).split("\0");
                                List<String> rewrittenChannels = new ArrayList<>();
                                for (String s : channels) {
                                    String rewritten = InventoryPackets.getNewPluginChannelId(s);
                                    if (rewritten != null) {
                                        rewrittenChannels.add(rewritten);
                                    } else if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                                        ViaBackwards.getPlatform().getLogger().warning("Ignoring plugin channel in incoming REGISTER: " + s);
                                    }
                                }
                                if (!rewrittenChannels.isEmpty()) {
                                    wrapper.write(Type.REMAINING_BYTES, Joiner.on('\0').join(rewrittenChannels).getBytes(StandardCharsets.UTF_8));
                                } else {
                                    wrapper.cancel();
                                    return;
                                }
                            }
                            break;
                    }
                });
            }
        });

        protocol.registerOutgoing(ClientboundPackets1_13.STATISTICS, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                handler(new PacketHandler() {
                    @Override
                    public void handle(PacketWrapper wrapper) throws Exception {
                        int size = wrapper.get(Type.VAR_INT, 0);
                        int newSize = size;
                        for (int i = 0; i < size; i++) {
                            int categoryId = wrapper.read(Type.VAR_INT);
                            int statisticId = wrapper.read(Type.VAR_INT);

                            String name = "";
                            // categories 0-7 (items, blocks, entities) - probably not feasible
                            switch (categoryId) {
                                case 0:
                                case 1:
                                case 2:
                                case 3:
                                case 4:
                                case 5:
                                case 6:
                                case 7:
                                    wrapper.read(Type.VAR_INT); // remove value
                                    newSize--;
                                    continue;
                                case 8:
                                    name = BackwardsMappings.statisticMappings.get(statisticId);
                                    if (name == null) {
                                        wrapper.read(Type.VAR_INT);
                                        newSize--;
                                        continue;
                                    }
                                    break;
                            }

                            wrapper.write(Type.STRING, name); // string id
                            wrapper.passthrough(Type.VAR_INT); // value
                        }

                        if (newSize != size) {
                            wrapper.set(Type.VAR_INT, 0, newSize);
                        }
                    }
                });
            }
        });
    }
}
