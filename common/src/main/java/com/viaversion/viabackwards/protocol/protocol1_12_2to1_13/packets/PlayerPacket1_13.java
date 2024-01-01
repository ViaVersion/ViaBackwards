/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.packets;

import com.google.common.base.Joiner;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.Protocol1_12_2To1_13;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.data.ParticleMapping;
import com.viaversion.viabackwards.protocol.protocol1_12_2to1_13.storage.TabCompleteStorage;
import com.viaversion.viabackwards.utils.ChatUtil;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.Position;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.rewriter.RewriterBase;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ServerboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.ServerboundPackets1_13;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.packets.InventoryPackets;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerPacket1_13 extends RewriterBase<Protocol1_12_2To1_13> {

    private final CommandRewriter<ClientboundPackets1_13> commandRewriter = new CommandRewriter<>(protocol);

    public PlayerPacket1_13(Protocol1_12_2To1_13 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        // Login Plugin Request
        protocol.registerClientbound(State.LOGIN, 0x04, -1, new PacketHandlers() {
            @Override
            public void register() {
                handler(packetWrapper -> {
                    packetWrapper.cancel();
                    packetWrapper.create(0x02, new PacketHandler() { // Plugin response
                        @Override
                        public void handle(PacketWrapper newWrapper) throws Exception {
                            newWrapper.write(Type.VAR_INT, packetWrapper.read(Type.VAR_INT)); // Packet id
                            newWrapper.write(Type.BOOLEAN, false); // Success
                        }
                    }).sendToServer(Protocol1_12_2To1_13.class);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.PLUGIN_MESSAGE, wrapper -> {
            String channel = wrapper.read(Type.STRING);
            if (channel.equals("minecraft:trader_list")) {
                wrapper.write(Type.STRING, "MC|TrList");
                wrapper.passthrough(Type.INT); //Passthrough Window ID

                int size = wrapper.passthrough(Type.UNSIGNED_BYTE);
                for (int i = 0; i < size; i++) {
                    //Input Item
                    Item input = wrapper.read(Type.ITEM1_13);
                    wrapper.write(Type.ITEM1_8, protocol.getItemRewriter().handleItemToClient(input));
                    //Output Item
                    Item output = wrapper.read(Type.ITEM1_13);
                    wrapper.write(Type.ITEM1_8, protocol.getItemRewriter().handleItemToClient(output));

                    boolean secondItem = wrapper.passthrough(Type.BOOLEAN); //Has second item
                    if (secondItem) {
                        //Second Item
                        Item second = wrapper.read(Type.ITEM1_13);
                        wrapper.write(Type.ITEM1_8, protocol.getItemRewriter().handleItemToClient(second));
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
        });

        protocol.registerClientbound(ClientboundPackets1_13.SPAWN_PARTICLE, new PacketHandlers() {
            @Override
            public void register() {
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
                handler(wrapper -> {
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
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.PLAYER_INFO, new PacketHandlers() {
            @Override
            public void register() {
                handler(packetWrapper -> {
                    TabCompleteStorage storage = packetWrapper.user().get(TabCompleteStorage.class);
                    int action = packetWrapper.passthrough(Type.VAR_INT);
                    int nPlayers = packetWrapper.passthrough(Type.VAR_INT);
                    for (int i = 0; i < nPlayers; i++) {
                        UUID uuid = packetWrapper.passthrough(Type.UUID);
                        if (action == 0) { // Add
                            String name = packetWrapper.passthrough(Type.STRING);
                            storage.usernames().put(uuid, name);
                            int nProperties = packetWrapper.passthrough(Type.VAR_INT);
                            for (int j = 0; j < nProperties; j++) {
                                packetWrapper.passthrough(Type.STRING);
                                packetWrapper.passthrough(Type.STRING);
                                packetWrapper.passthrough(Type.OPTIONAL_STRING);
                            }
                            packetWrapper.passthrough(Type.VAR_INT);
                            packetWrapper.passthrough(Type.VAR_INT);
                            packetWrapper.passthrough(Type.OPTIONAL_COMPONENT);
                        } else if (action == 1) { // Update Game Mode
                            packetWrapper.passthrough(Type.VAR_INT);
                        } else if (action == 2) { // Update Ping
                            packetWrapper.passthrough(Type.VAR_INT);
                        } else if (action == 3) { // Update Display Name
                            packetWrapper.passthrough(Type.OPTIONAL_COMPONENT);
                        } else if (action == 4) { // Remove Player
                            storage.usernames().remove(uuid);
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.SCOREBOARD_OBJECTIVE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING);
                map(Type.BYTE);
                handler(wrapper -> {
                    byte mode = wrapper.get(Type.BYTE, 0);
                    if (mode == 0 || mode == 2) {
                        JsonElement value = wrapper.read(Type.COMPONENT);
                        String legacyValue = protocol.jsonToLegacy(value);
                        wrapper.write(Type.STRING, ChatUtil.fromLegacy(legacyValue, 'f', 32));
                        int type = wrapper.read(Type.VAR_INT);
                        wrapper.write(Type.STRING, type == 1 ? "hearts" : "integer");
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.TEAMS, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Name
                map(Type.BYTE); // Action
                handler(wrapper -> {
                    byte action = wrapper.get(Type.BYTE, 0);
                    if (action == 0 || action == 2) {
                        JsonElement displayName = wrapper.read(Type.COMPONENT);
                        String legacyTextDisplayName = protocol.jsonToLegacy(displayName);
                        wrapper.write(Type.STRING, ChatUtil.fromLegacy(legacyTextDisplayName, 'f', 32));

                        byte flags = wrapper.read(Type.BYTE);
                        String nameTagVisibility = wrapper.read(Type.STRING);
                        String collisionRule = wrapper.read(Type.STRING);

                        int colour = wrapper.read(Type.VAR_INT);
                        if (colour == 21) {
                            colour = -1;
                        }

                        JsonElement prefixComponent = wrapper.read(Type.COMPONENT);
                        JsonElement suffixComponent = wrapper.read(Type.COMPONENT);

                        String prefix = protocol.jsonToLegacy(prefixComponent);
                        if (ViaBackwards.getConfig().addTeamColorTo1_13Prefix()) {
                            prefix += "§" + (colour > -1 && colour <= 15 ? Integer.toHexString(colour) : "r");
                        }
                        String suffix = protocol.jsonToLegacy(suffixComponent);

                        wrapper.write(Type.STRING, ChatUtil.fromLegacyPrefix(prefix, 'f', 16));
                        wrapper.write(Type.STRING, ChatUtil.fromLegacy(suffix, '\0', 16));

                        wrapper.write(Type.BYTE, flags);
                        wrapper.write(Type.STRING, nameTagVisibility);
                        wrapper.write(Type.STRING, collisionRule);

                        wrapper.write(Type.BYTE, (byte) colour);
                    }

                    if (action == 0 || action == 3 || action == 4) {
                        wrapper.passthrough(Type.STRING_ARRAY); //Entities
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.DECLARE_COMMANDS, null, wrapper -> {
            wrapper.cancel();

            TabCompleteStorage storage = wrapper.user().get(TabCompleteStorage.class);

            if (!storage.commands().isEmpty()) {
                storage.commands().clear();
            }

            int size = wrapper.read(Type.VAR_INT);
            boolean initialNodes = true;
            for (int i = 0; i < size; i++) {
                byte flags = wrapper.read(Type.BYTE);
                wrapper.read(Type.VAR_INT_ARRAY_PRIMITIVE); // Children indices
                if ((flags & 0x08) != 0) {
                    wrapper.read(Type.VAR_INT); // Redirect node index
                }

                byte nodeType = (byte) (flags & 0x03);
                if (initialNodes && nodeType == 2) {
                    initialNodes = false;
                }

                if (nodeType == 1 || nodeType == 2) { // Literal/argument node
                    String name = wrapper.read(Type.STRING);
                    if (nodeType == 1 && initialNodes) {
                        storage.commands().add('/' + name);
                    }
                }

                if (nodeType == 2) { // Argument node
                    commandRewriter.handleArgument(wrapper, wrapper.read(Type.STRING));
                }

                if ((flags & 0x10) != 0) {
                    wrapper.read(Type.STRING); // Suggestion type
                }
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.TAB_COMPLETE, wrapper -> {
            TabCompleteStorage storage = wrapper.user().get(TabCompleteStorage.class);
            if (storage.lastRequest() == null) {
                wrapper.cancel();
                return;
            }
            if (storage.lastId() != wrapper.read(Type.VAR_INT)) wrapper.cancel();
            int start = wrapper.read(Type.VAR_INT);
            int length = wrapper.read(Type.VAR_INT);

            int lastRequestPartIndex = storage.lastRequest().lastIndexOf(' ') + 1;
            if (lastRequestPartIndex != start) wrapper.cancel(); // Client only replaces after space

            if (length != storage.lastRequest().length() - lastRequestPartIndex) {
                wrapper.cancel(); // We can't set the length in previous versions
            }

            int count = wrapper.passthrough(Type.VAR_INT);
            for (int i = 0; i < count; i++) {
                String match = wrapper.read(Type.STRING);
                wrapper.write(Type.STRING, (start == 0 && !storage.isLastAssumeCommand() ? "/" : "") + match);
                wrapper.read(Type.OPTIONAL_COMPONENT); // Remove tooltip
            }
        });

        protocol.registerServerbound(ServerboundPackets1_12_1.TAB_COMPLETE, wrapper -> {
            TabCompleteStorage storage = wrapper.user().get(TabCompleteStorage.class);
            List<String> suggestions = new ArrayList<>();

            String command = wrapper.read(Type.STRING);
            boolean assumeCommand = wrapper.read(Type.BOOLEAN);
            wrapper.read(Type.OPTIONAL_POSITION1_8);

            if (!assumeCommand && !command.startsWith("/")) {
                // Complete usernames for non-commands
                String buffer = command.substring(command.lastIndexOf(' ') + 1);
                for (String value : storage.usernames().values()) {
                    if (startsWithIgnoreCase(value, buffer)) {
                        suggestions.add(value);
                    }
                }
            } else if (!storage.commands().isEmpty() && !command.contains(" ")) {
                // Complete commands names with values from 'Declare Commands' packet
                for (String value : storage.commands()) {
                    if (startsWithIgnoreCase(value, command)) {
                        suggestions.add(value);
                    }
                }
            }

            if (!suggestions.isEmpty()) {
                wrapper.cancel();
                PacketWrapper response = wrapper.create(ClientboundPackets1_12_1.TAB_COMPLETE);
                response.write(Type.VAR_INT, suggestions.size());
                for (String value : suggestions) {
                    response.write(Type.STRING, value);
                }
                response.scheduleSend(Protocol1_12_2To1_13.class);
                storage.setLastRequest(null);
                return;
            }

            if (!assumeCommand && command.startsWith("/")) {
                command = command.substring(1);
            }

            int id = ThreadLocalRandom.current().nextInt();
            wrapper.write(Type.VAR_INT, id);
            wrapper.write(Type.STRING, command);

            storage.setLastId(id);
            storage.setLastAssumeCommand(assumeCommand);
            storage.setLastRequest(command);
        });

        protocol.registerServerbound(ServerboundPackets1_12_1.PLUGIN_MESSAGE, wrapper -> {
            String channel = wrapper.read(Type.STRING);
            switch (channel) {
                case "MC|BSign":
                case "MC|BEdit":
                    wrapper.setPacketType(ServerboundPackets1_13.EDIT_BOOK);
                    Item book = wrapper.read(Type.ITEM1_8);
                    wrapper.write(Type.ITEM1_13, protocol.getItemRewriter().handleItemToServer(book));
                    boolean signing = channel.equals("MC|BSign");
                    wrapper.write(Type.BOOLEAN, signing);
                    break;
                case "MC|ItemName":
                    wrapper.setPacketType(ServerboundPackets1_13.RENAME_ITEM);
                    break;
                case "MC|AdvCmd":
                    byte type = wrapper.read(Type.BYTE);
                    if (type == 0) {
                        //Information from https://wiki.vg/index.php?title=Plugin_channels&oldid=14089
                        //The Notchain client only uses this for command block minecarts and uses MC|AutoCmd for blocks, but the Notchian server still accepts it for either.
                        //Maybe older versions used this and we need to implement this? The issues is that we would have to save the command block types
                        wrapper.setPacketType(ServerboundPackets1_13.UPDATE_COMMAND_BLOCK);
                        wrapper.cancel();
                        ViaBackwards.getPlatform().getLogger().warning("Client send MC|AdvCmd custom payload to update command block, weird!");
                    } else if (type == 1) {
                        wrapper.setPacketType(ServerboundPackets1_13.UPDATE_COMMAND_BLOCK_MINECART);
                        wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Entity Id
                        wrapper.passthrough(Type.STRING); //Command
                        wrapper.passthrough(Type.BOOLEAN); //Track Output

                    } else {
                        wrapper.cancel();
                    }
                    break;
                case "MC|AutoCmd": {
                    wrapper.setPacketType(ServerboundPackets1_13.UPDATE_COMMAND_BLOCK);

                    int x = wrapper.read(Type.INT);
                    int y = wrapper.read(Type.INT);
                    int z = wrapper.read(Type.INT);

                    wrapper.write(Type.POSITION1_8, new Position(x, (short) y, z));

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
                    wrapper.setPacketType(ServerboundPackets1_13.UPDATE_STRUCTURE_BLOCK);
                    int x = wrapper.read(Type.INT);
                    int y = wrapper.read(Type.INT);
                    int z = wrapper.read(Type.INT);
                    wrapper.write(Type.POSITION1_8, new Position(x, (short) y, z));
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
                    wrapper.setPacketType(ServerboundPackets1_13.SET_BEACON_EFFECT);
                    wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Primary Effect

                    wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Secondary Effect

                    break;
                case "MC|TrSel":
                    wrapper.setPacketType(ServerboundPackets1_13.SELECT_TRADE);
                    wrapper.write(Type.VAR_INT, wrapper.read(Type.INT)); //Slot

                    break;
                case "MC|PickItem":
                    wrapper.setPacketType(ServerboundPackets1_13.PICK_ITEM);
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

        protocol.registerClientbound(ClientboundPackets1_13.STATISTICS, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                handler(wrapper -> {
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
                                name = protocol.getMappingData().getStatisticMappings().get(statisticId);
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
                });
            }
        });
    }

    private static boolean startsWithIgnoreCase(String string, String prefix) {
        if (string.length() < prefix.length()) {
            return false;
        }
        return string.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
