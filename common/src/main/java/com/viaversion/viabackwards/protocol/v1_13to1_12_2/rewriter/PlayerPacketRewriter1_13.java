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
package com.viaversion.viabackwards.protocol.v1_13to1_12_2.rewriter;

import com.google.common.base.Joiner;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.data.ParticleIdMappings1_12_2;
import com.viaversion.viabackwards.protocol.v1_13to1_12_2.storage.TabCompleteStorage;
import com.viaversion.viabackwards.utils.ChatUtil;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.BlockPosition;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.rewriter.RewriterBase;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.rewriter.ItemPacketRewriter1_13;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.v1_12to1_12_1.packet.ServerboundPackets1_12_1;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ClientboundPackets1_13;
import com.viaversion.viaversion.protocols.v1_12_2to1_13.packet.ServerboundPackets1_13;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import com.viaversion.viaversion.util.Key;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class PlayerPacketRewriter1_13 extends RewriterBase<Protocol1_13To1_12_2> {

    private final CommandRewriter<ClientboundPackets1_13> commandRewriter = new CommandRewriter<>(protocol);

    public PlayerPacketRewriter1_13(Protocol1_13To1_12_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerPackets() {
        // Login Plugin Request
        protocol.registerClientbound(State.LOGIN, ClientboundLoginPackets.CUSTOM_QUERY.getId(), -1, new PacketHandlers() {
            @Override
            public void register() {
                handler(packetWrapper -> {
                    packetWrapper.cancel();
                    // Plugin response
                    packetWrapper.create(ServerboundLoginPackets.CUSTOM_QUERY_ANSWER.getId(), wrapper -> {
                        wrapper.write(Types.VAR_INT, packetWrapper.read(Types.VAR_INT)); // Packet id
                        wrapper.write(Types.BOOLEAN, false); // Success
                    }).sendToServer(Protocol1_13To1_12_2.class);
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.CUSTOM_PAYLOAD, wrapper -> {
            String channel = wrapper.read(Types.STRING);
            if (channel.equals("minecraft:trader_list")) {
                wrapper.write(Types.STRING, "MC|TrList");
                protocol.getItemRewriter().handleTradeList(wrapper);
            } else {
                String oldChannel = ItemPacketRewriter1_13.getOldPluginChannelId(channel);
                if (oldChannel == null) {
                    if (!Via.getConfig().isSuppressConversionWarnings()) {
                        protocol.getLogger().warning("Ignoring clientbound plugin message with channel: " + channel);
                    }
                    wrapper.cancel();
                    return;
                }
                wrapper.write(Types.STRING, oldChannel);

                if (oldChannel.equals("REGISTER") || oldChannel.equals("UNREGISTER")) {
                    String[] channels = new String(wrapper.read(Types.REMAINING_BYTES), StandardCharsets.UTF_8).split("\0");
                    List<String> rewrittenChannels = new ArrayList<>();
                    for (String s : channels) {
                        String rewritten = ItemPacketRewriter1_13.getOldPluginChannelId(s);
                        if (rewritten != null) {
                            rewrittenChannels.add(rewritten);
                        } else if (!Via.getConfig().isSuppressConversionWarnings()) {
                            protocol.getLogger().warning("Ignoring plugin channel in clientbound " + oldChannel + ": " + s);
                        }
                    }
                    wrapper.write(Types.REMAINING_BYTES, Joiner.on('\0').join(rewrittenChannels).getBytes(StandardCharsets.UTF_8));
                }
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.LEVEL_PARTICLES, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT);      // 0 - Particle ID
                map(Types.BOOLEAN);  // 1 - Long Distance
                map(Types.FLOAT);    // 2 - X
                map(Types.FLOAT);    // 3 - Y
                map(Types.FLOAT);    // 4 - Z
                map(Types.FLOAT);    // 5 - Offset X
                map(Types.FLOAT);    // 6 - Offset Y
                map(Types.FLOAT);    // 7 - Offset Z
                map(Types.FLOAT);    // 8 - Particle Data
                map(Types.INT);      // 9 - Particle Count
                handler(wrapper -> {
                    ParticleIdMappings1_12_2.ParticleData old = ParticleIdMappings1_12_2.getMapping(wrapper.get(Types.INT, 0));
                    wrapper.set(Types.INT, 0, old.getHistoryId());

                    int[] data = old.rewriteData(protocol, wrapper);
                    if (data != null) {
                        if (old.getHandler().isBlockHandler() && data[0] == 0) {
                            // Cancel air block particles
                            wrapper.cancel();
                            return;
                        }

                        for (int i : data) {
                            wrapper.write(Types.VAR_INT, i);
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
                    int action = packetWrapper.passthrough(Types.VAR_INT);
                    int nPlayers = packetWrapper.passthrough(Types.VAR_INT);
                    for (int i = 0; i < nPlayers; i++) {
                        UUID uuid = packetWrapper.passthrough(Types.UUID);
                        if (action == 0) { // Add
                            String name = packetWrapper.passthrough(Types.STRING);
                            storage.usernames().put(uuid, name);
                            int nProperties = packetWrapper.passthrough(Types.VAR_INT);
                            for (int j = 0; j < nProperties; j++) {
                                packetWrapper.passthrough(Types.STRING);
                                packetWrapper.passthrough(Types.STRING);
                                packetWrapper.passthrough(Types.OPTIONAL_STRING);
                            }
                            packetWrapper.passthrough(Types.VAR_INT);
                            packetWrapper.passthrough(Types.VAR_INT);
                            packetWrapper.passthrough(Types.OPTIONAL_COMPONENT);
                        } else if (action == 1) { // Update Game Mode
                            packetWrapper.passthrough(Types.VAR_INT);
                        } else if (action == 2) { // Update Ping
                            packetWrapper.passthrough(Types.VAR_INT);
                        } else if (action == 3) { // Update Display Name
                            packetWrapper.passthrough(Types.OPTIONAL_COMPONENT);
                        } else if (action == 4) { // Remove Player
                            storage.usernames().remove(uuid);
                        }
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.SET_OBJECTIVE, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING);
                map(Types.BYTE);
                handler(wrapper -> {
                    byte mode = wrapper.get(Types.BYTE, 0);
                    if (mode == 0 || mode == 2) {
                        JsonElement value = wrapper.read(Types.COMPONENT);
                        String legacyValue = protocol.jsonToLegacy(wrapper.user(), value);
                        wrapper.write(Types.STRING, ChatUtil.fromLegacy(legacyValue, 'f', 32));
                        int type = wrapper.read(Types.VAR_INT);
                        wrapper.write(Types.STRING, type == 1 ? "hearts" : "integer");
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.SET_PLAYER_TEAM, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Name
                map(Types.BYTE); // Action
                handler(wrapper -> {
                    byte action = wrapper.get(Types.BYTE, 0);
                    if (action == 0 || action == 2) {
                        JsonElement displayName = wrapper.read(Types.COMPONENT);
                        String legacyTextDisplayName = protocol.jsonToLegacy(wrapper.user(), displayName);
                        wrapper.write(Types.STRING, ChatUtil.fromLegacy(legacyTextDisplayName, 'f', 32));

                        byte flags = wrapper.read(Types.BYTE);
                        String nameTagVisibility = wrapper.read(Types.STRING);
                        String collisionRule = wrapper.read(Types.STRING);

                        int colour = wrapper.read(Types.VAR_INT);
                        if (colour == 21) {
                            colour = -1;
                        }

                        JsonElement prefixComponent = wrapper.read(Types.COMPONENT);
                        JsonElement suffixComponent = wrapper.read(Types.COMPONENT);

                        String prefix = protocol.jsonToLegacy(wrapper.user(), prefixComponent);
                        if (ViaBackwards.getConfig().addTeamColorTo1_13Prefix()) {
                            prefix += "§" + (colour > -1 && colour <= 15 ? Integer.toHexString(colour) : "r");
                        }
                        String suffix = protocol.jsonToLegacy(wrapper.user(), suffixComponent);

                        wrapper.write(Types.STRING, ChatUtil.fromLegacyPrefix(prefix, 'f', 16));
                        wrapper.write(Types.STRING, ChatUtil.fromLegacy(suffix, '\0', 16));

                        wrapper.write(Types.BYTE, flags);
                        wrapper.write(Types.STRING, nameTagVisibility);
                        wrapper.write(Types.STRING, collisionRule);

                        wrapper.write(Types.BYTE, (byte) colour);
                    }

                    if (action == 0 || action == 3 || action == 4) {
                        wrapper.passthrough(Types.STRING_ARRAY); //Entities
                    }
                });
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.COMMANDS, null, wrapper -> {
            wrapper.cancel();

            TabCompleteStorage storage = wrapper.user().get(TabCompleteStorage.class);

            if (!storage.commands().isEmpty()) {
                storage.commands().clear();
            }

            int size = wrapper.read(Types.VAR_INT);
            boolean initialNodes = true;
            for (int i = 0; i < size; i++) {
                byte flags = wrapper.read(Types.BYTE);
                wrapper.read(Types.VAR_INT_ARRAY_PRIMITIVE); // Children indices
                if ((flags & 0x08) != 0) {
                    wrapper.read(Types.VAR_INT); // Redirect node index
                }

                byte nodeType = (byte) (flags & 0x03);
                if (initialNodes && nodeType == 2) {
                    initialNodes = false;
                }

                if (nodeType == 1 || nodeType == 2) { // Literal/argument node
                    String name = wrapper.read(Types.STRING);
                    if (nodeType == 1 && initialNodes) {
                        storage.commands().add('/' + name);
                    }
                }

                if (nodeType == 2) { // Argument node
                    commandRewriter.handleArgument(wrapper, wrapper.read(Types.STRING));
                }

                if ((flags & 0x10) != 0) {
                    wrapper.read(Types.STRING); // Suggestion type
                }
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.COMMAND_SUGGESTIONS, wrapper -> {
            TabCompleteStorage storage = wrapper.user().get(TabCompleteStorage.class);
            if (storage.lastRequest() == null) {
                wrapper.cancel();
                return;
            }
            if (storage.lastId() != wrapper.read(Types.VAR_INT)) wrapper.cancel();
            int start = wrapper.read(Types.VAR_INT);
            int length = wrapper.read(Types.VAR_INT);

            int lastRequestPartIndex = storage.lastRequest().lastIndexOf(' ') + 1;
            if (lastRequestPartIndex != start) wrapper.cancel(); // Client only replaces after space

            if (length != storage.lastRequest().length() - lastRequestPartIndex) {
                wrapper.cancel(); // We can't set the length in previous versions
            }

            int count = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < count; i++) {
                String match = wrapper.read(Types.STRING);
                wrapper.write(Types.STRING, (start == 0 && !storage.isLastAssumeCommand() ? "/" : "") + match);
                wrapper.read(Types.OPTIONAL_COMPONENT); // Remove tooltip
            }
        });

        protocol.registerServerbound(ServerboundPackets1_12_1.COMMAND_SUGGESTION, wrapper -> {
            TabCompleteStorage storage = wrapper.user().get(TabCompleteStorage.class);
            List<String> suggestions = new ArrayList<>();

            String command = wrapper.read(Types.STRING);
            boolean assumeCommand = wrapper.read(Types.BOOLEAN);
            wrapper.read(Types.OPTIONAL_POSITION1_8);

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
                PacketWrapper response = wrapper.create(ClientboundPackets1_12_1.COMMAND_SUGGESTIONS);
                response.write(Types.VAR_INT, suggestions.size());
                for (String value : suggestions) {
                    response.write(Types.STRING, value);
                }
                response.scheduleSend(Protocol1_13To1_12_2.class);
                storage.setLastRequest(null);
                return;
            }

            if (!assumeCommand && command.startsWith("/")) {
                command = command.substring(1);
            }

            int id = ThreadLocalRandom.current().nextInt();
            wrapper.write(Types.VAR_INT, id);
            wrapper.write(Types.STRING, command);

            storage.setLastId(id);
            storage.setLastAssumeCommand(assumeCommand);
            storage.setLastRequest(command);
        });

        protocol.registerServerbound(ServerboundPackets1_12_1.CUSTOM_PAYLOAD, wrapper -> {
            String channel = wrapper.read(Types.STRING);
            switch (channel) {
                case "MC|BSign", "MC|BEdit" -> {
                    wrapper.setPacketType(ServerboundPackets1_13.EDIT_BOOK);
                    Item book = wrapper.read(Types.ITEM1_8);
                    wrapper.write(Types.ITEM1_13, protocol.getItemRewriter().handleItemToServer(wrapper.user(), book));
                    boolean signing = channel.equals("MC|BSign");
                    wrapper.write(Types.BOOLEAN, signing);
                }
                case "MC|ItemName" -> wrapper.setPacketType(ServerboundPackets1_13.RENAME_ITEM);
                case "MC|AdvCmd" -> {
                    byte type = wrapper.read(Types.BYTE);
                    if (type == 0) {
                        //Information from https://wiki.vg/index.php?title=Plugin_channels&oldid=14089 (see web archive)
                        //The Vanilla client only uses this for command block minecarts and uses MC|AutoCmd for blocks, but the server still accepts it for either.
                        //Maybe older versions used this and we need to implement this? The issue is that we would have to save the command block types
                        wrapper.setPacketType(ServerboundPackets1_13.SET_COMMAND_BLOCK);
                        wrapper.cancel();
                        if (!Via.getConfig().isSuppressConversionWarnings()) {
                            protocol.getLogger().warning("Client send MC|AdvCmd custom payload to update command block, weird!");
                        }
                    } else if (type == 1) {
                        wrapper.setPacketType(ServerboundPackets1_13.SET_COMMAND_MINECART);
                        wrapper.write(Types.VAR_INT, wrapper.read(Types.INT)); //Entity Id
                        wrapper.passthrough(Types.STRING); //Command
                        wrapper.passthrough(Types.BOOLEAN); //Track Output

                    } else {
                        wrapper.cancel();
                    }
                }
                case "MC|AutoCmd" -> {
                    wrapper.setPacketType(ServerboundPackets1_13.SET_COMMAND_BLOCK);

                    int x = wrapper.read(Types.INT);
                    int y = wrapper.read(Types.INT);
                    int z = wrapper.read(Types.INT);

                    wrapper.write(Types.BLOCK_POSITION1_8, new BlockPosition(x, (short) y, z));

                    wrapper.passthrough(Types.STRING);  //Command

                    byte flags = 0;
                    if (wrapper.read(Types.BOOLEAN)) flags |= 0x01; //Track Output

                    String mode = wrapper.read(Types.STRING);

                    int modeId = mode.equals("SEQUENCE") ? 0 : mode.equals("AUTO") ? 1 : 2;
                    wrapper.write(Types.VAR_INT, modeId);

                    if (wrapper.read(Types.BOOLEAN)) flags |= 0x02; //Is conditional
                    if (wrapper.read(Types.BOOLEAN)) flags |= 0x04; //Automatic

                    wrapper.write(Types.BYTE, flags);
                }
                case "MC|Struct" -> {
                    wrapper.setPacketType(ServerboundPackets1_13.SET_STRUCTURE_BLOCK);
                    int x = wrapper.read(Types.INT);
                    int y = wrapper.read(Types.INT);
                    int z = wrapper.read(Types.INT);
                    wrapper.write(Types.BLOCK_POSITION1_8, new BlockPosition(x, (short) y, z));
                    wrapper.write(Types.VAR_INT, wrapper.read(Types.BYTE) - 1);
                    String mode = wrapper.read(Types.STRING);
                    int modeId = mode.equals("SAVE") ? 0 : mode.equals("LOAD") ? 1 : mode.equals("CORNER") ? 2 : 3;
                    wrapper.write(Types.VAR_INT, modeId);
                    wrapper.passthrough(Types.STRING); //Name

                    wrapper.write(Types.BYTE, wrapper.read(Types.INT).byteValue()); //Offset X

                    wrapper.write(Types.BYTE, wrapper.read(Types.INT).byteValue()); //Offset Y

                    wrapper.write(Types.BYTE, wrapper.read(Types.INT).byteValue()); //Offset Z

                    wrapper.write(Types.BYTE, wrapper.read(Types.INT).byteValue()); //Size X

                    wrapper.write(Types.BYTE, wrapper.read(Types.INT).byteValue()); //Size Y

                    wrapper.write(Types.BYTE, wrapper.read(Types.INT).byteValue()); //Size Z

                    String mirror = wrapper.read(Types.STRING);
                    int mirrorId = mode.equals("NONE") ? 0 : mode.equals("LEFT_RIGHT") ? 1 : 2;
                    String rotation = wrapper.read(Types.STRING);
                    int rotationId = mode.equals("NONE") ? 0 : mode.equals("CLOCKWISE_90") ? 1 : mode.equals("CLOCKWISE_180") ? 2 : 3;
                    wrapper.passthrough(Types.STRING); //Metadata

                    byte flags = 0;
                    if (wrapper.read(Types.BOOLEAN)) flags |= 0x01; //Ignore entities
                    if (wrapper.read(Types.BOOLEAN)) flags |= 0x02; //Show air
                    if (wrapper.read(Types.BOOLEAN)) flags |= 0x04; //Show bounding box
                    wrapper.passthrough(Types.FLOAT); //Integrity

                    wrapper.passthrough(Types.VAR_LONG); //Seed

                    wrapper.write(Types.BYTE, flags);
                }
                case "MC|Beacon" -> {
                    wrapper.setPacketType(ServerboundPackets1_13.SET_BEACON);
                    wrapper.write(Types.VAR_INT, wrapper.read(Types.INT)); //Primary Effect
                    wrapper.write(Types.VAR_INT, wrapper.read(Types.INT)); //Secondary Effect
                }
                case "MC|TrSel" -> {
                    wrapper.setPacketType(ServerboundPackets1_13.SELECT_TRADE);
                    wrapper.write(Types.VAR_INT, wrapper.read(Types.INT)); //Slot
                }
                case "MC|PickItem" -> wrapper.setPacketType(ServerboundPackets1_13.PICK_ITEM);
                default -> {
                    String newChannel = ItemPacketRewriter1_13.getNewPluginChannelId(channel);
                    if (newChannel == null) {
                        if (!Via.getConfig().isSuppressConversionWarnings()) {
                            protocol.getLogger().warning("Ignoring serverbound plugin message with channel: " + channel);
                        }
                        wrapper.cancel();
                        return;
                    }
                    wrapper.write(Types.STRING, newChannel);

                    if (newChannel.equals("minecraft:register") || newChannel.equals("minecraft:unregister")) {
                        String[] channels = new String(wrapper.read(Types.SERVERBOUND_CUSTOM_PAYLOAD_DATA), StandardCharsets.UTF_8).split("\0");
                        List<String> rewrittenChannels = new ArrayList<>();
                        for (String s : channels) {
                            String rewritten = ItemPacketRewriter1_13.getNewPluginChannelId(s);
                            if (rewritten != null) {
                                rewrittenChannels.add(rewritten);
                            } else if (!Via.getConfig().isSuppressConversionWarnings()) {
                                protocol.getLogger().warning("Ignoring plugin channel in serverbound " + Key.stripMinecraftNamespace(newChannel).toUpperCase(Locale.ROOT) + ": " + s);
                            }
                        }
                        if (!rewrittenChannels.isEmpty()) {
                            wrapper.write(Types.SERVERBOUND_CUSTOM_PAYLOAD_DATA, Joiner.on('\0').join(rewrittenChannels).getBytes(StandardCharsets.UTF_8));
                        } else {
                            wrapper.cancel();
                        }
                    }
                }
            }
        });

        protocol.registerClientbound(ClientboundPackets1_13.AWARD_STATS, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                handler(wrapper -> {
                    int size = wrapper.get(Types.VAR_INT, 0);
                    int newSize = size;
                    for (int i = 0; i < size; i++) {
                        int categoryId = wrapper.read(Types.VAR_INT);
                        int statisticId = wrapper.read(Types.VAR_INT);

                        String name = "";
                        // categories 0-7 (items, blocks, entities) - probably not feasible
                        switch (categoryId) {
                            case 0, 1, 2, 3, 4, 5, 6, 7 -> {
                                wrapper.read(Types.VAR_INT); // remove value
                                newSize--;
                                continue;
                            }
                            case 8 -> {
                                name = protocol.getMappingData().getStatisticMappings().get(statisticId);
                                if (name == null) {
                                    wrapper.read(Types.VAR_INT);
                                    newSize--;
                                    continue;
                                }
                            }
                        }

                        wrapper.write(Types.STRING, name); // string id
                        wrapper.passthrough(Types.VAR_INT); // value
                    }

                    if (newSize != size) {
                        wrapper.set(Types.VAR_INT, 0, newSize);
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
