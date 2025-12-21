/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2025 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.v1_19_1to1_19;

import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.nbt.tag.ListTag;
import com.viaversion.nbt.tag.NumberTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_19_1to1_19.rewriter.EntityPacketRewriter1_19_1;
import com.viaversion.viabackwards.protocol.v1_19_1to1_19.storage.ChatRegistryStorage;
import com.viaversion.viabackwards.protocol.v1_19_1to1_19.storage.ChatRegistryStorage1_19_1;
import com.viaversion.viabackwards.protocol.v1_19_1to1_19.storage.NonceStorage;
import com.viaversion.viabackwards.protocol.v1_19_1to1_19.storage.ReceivedMessagesStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19;
import com.viaversion.viaversion.api.minecraft.signature.SignableCommandArgumentsProvider;
import com.viaversion.viaversion.api.minecraft.signature.model.DecoratableMessage;
import com.viaversion.viaversion.api.minecraft.signature.model.MessageMetadata;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_1;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.Protocol1_18_2To1_19;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ClientboundPackets1_19;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ServerboundPackets1_19;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.Protocol1_19To1_19_1;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.packet.ClientboundPackets1_19_1;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.packet.ServerboundPackets1_19_1;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;
import com.viaversion.viaversion.util.CipherUtil;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.Pair;
import com.viaversion.viaversion.util.TagUtil;
import java.security.SignatureException;
import java.util.List;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Protocol1_19_1To1_19 extends BackwardsProtocol<ClientboundPackets1_19_1, ClientboundPackets1_19, ServerboundPackets1_19_1, ServerboundPackets1_19> {

    public static final int SYSTEM_CHAT_ID = 1;
    public static final int GAME_INFO_ID = 2;
    private static final UUID ZERO_UUID = new UUID(0, 0);
    private static final byte[] EMPTY_BYTES = new byte[0];
    private final EntityPacketRewriter1_19_1 entityRewriter = new EntityPacketRewriter1_19_1(this);
    private final JsonNBTComponentRewriter<ClientboundPackets1_19_1> translatableRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);

    public Protocol1_19_1To1_19() {
        super(ClientboundPackets1_19_1.class, ClientboundPackets1_19.class, ServerboundPackets1_19_1.class, ServerboundPackets1_19.class);
    }

    @Override
    protected void registerPackets() {
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_19_1.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19_1.TAB_LIST);
        translatableRewriter.registerSetPlayerTeam1_13(ClientboundPackets1_19_1.SET_PLAYER_TEAM);
        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_19_1.OPEN_SCREEN);
        translatableRewriter.registerPlayerCombatKill(ClientboundPackets1_19_1.PLAYER_COMBAT_KILL);
        translatableRewriter.registerSetObjective(ClientboundPackets1_19_1.SET_OBJECTIVE);
        translatableRewriter.registerPing();

        entityRewriter.register();

        registerClientbound(ClientboundPackets1_19_1.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity ID
                map(Types.BOOLEAN); // Hardcore
                map(Types.BYTE); // Gamemode
                map(Types.BYTE); // Previous Gamemode
                map(Types.STRING_ARRAY); // World List
                map(Types.NAMED_COMPOUND_TAG); // Dimension registry
                map(Types.STRING); // Dimension key
                map(Types.STRING); // World
                handler(wrapper -> {
                    final ChatRegistryStorage chatTypeStorage = wrapper.user().get(ChatRegistryStorage1_19_1.class);
                    chatTypeStorage.clear();

                    final CompoundTag registry = wrapper.get(Types.NAMED_COMPOUND_TAG, 0);
                    final ListTag<CompoundTag> chatTypes = TagUtil.removeRegistryEntries(registry, "chat_type", new ListTag<>(CompoundTag.class));
                    for (final CompoundTag chatType : chatTypes) {
                        final NumberTag idTag = chatType.getNumberTag("id");
                        chatTypeStorage.addChatType(idTag.asInt(), chatType);
                    }

                    // Replace with 1.19 chat types
                    // Ensures that the client has a chat type for system message, with and without overlay
                    registry.put("minecraft:chat_type", Protocol1_18_2To1_19.MAPPINGS.chatRegistry());
                });
                handler(entityRewriter.worldTrackerHandlerByKey());
            }
        });

        registerClientbound(ClientboundPackets1_19_1.PLAYER_CHAT, ClientboundPackets1_19.SYSTEM_CHAT, wrapper -> {
            wrapper.read(Types.OPTIONAL_BYTE_ARRAY_PRIMITIVE); // Previous signature

            final PlayerMessageSignature signature = wrapper.read(Types.PLAYER_MESSAGE_SIGNATURE);

            // Store message signature for last seen
            if (!signature.uuid().equals(ZERO_UUID) && signature.signatureBytes().length != 0) {
                final ReceivedMessagesStorage messagesStorage = wrapper.user().get(ReceivedMessagesStorage.class);
                messagesStorage.add(signature);
                if (messagesStorage.tickUnacknowledged() > 64) {
                    messagesStorage.resetUnacknowledgedCount();

                    // Send chat acknowledgement
                    final PacketWrapper chatAckPacket = wrapper.create(ServerboundPackets1_19_1.CHAT_ACK);
                    chatAckPacket.write(Types.PLAYER_MESSAGE_SIGNATURE_ARRAY, messagesStorage.lastSignatures());
                    chatAckPacket.write(Types.OPTIONAL_PLAYER_MESSAGE_SIGNATURE, null);
                    chatAckPacket.sendToServer(Protocol1_19_1To1_19.class);
                }
            }

            // Send the unsigned message if present, otherwise the signed message
            final String plainMessage = wrapper.read(Types.STRING); // Plain message
            JsonElement message = null;
            JsonElement decoratedMessage = wrapper.read(Types.OPTIONAL_COMPONENT);
            if (decoratedMessage != null) {
                message = decoratedMessage;
            }

            wrapper.read(Types.LONG); // Timestamp
            wrapper.read(Types.LONG); // Salt
            wrapper.read(Types.PLAYER_MESSAGE_SIGNATURE_ARRAY); // Last seen

            final JsonElement unsignedMessage = wrapper.read(Types.OPTIONAL_COMPONENT);
            if (unsignedMessage != null) {
                message = unsignedMessage;
            }
            if (message == null) {
                // If no decorated or unsigned message is given, use the plain one
                message = ComponentUtil.plainToJson(plainMessage);
            }

            final int filterMaskType = wrapper.read(Types.VAR_INT);
            if (filterMaskType == 2) { // Partially filtered
                wrapper.read(Types.LONG_ARRAY_PRIMITIVE); // Mask
            }

            final int chatTypeId = wrapper.read(Types.VAR_INT);
            final JsonElement senderName = wrapper.read(Types.COMPONENT);
            final JsonElement targetName = wrapper.read(Types.OPTIONAL_COMPONENT);
            decoratedMessage = decorateChatMessage(this, wrapper.user().get(ChatRegistryStorage1_19_1.class), chatTypeId, senderName, targetName, message);
            if (decoratedMessage == null) {
                wrapper.cancel();
                return;
            }

            translatableRewriter.processText(wrapper.user(), decoratedMessage);
            wrapper.write(Types.COMPONENT, decoratedMessage);
            wrapper.write(Types.VAR_INT, SYSTEM_CHAT_ID);
        });

        registerClientbound(ClientboundPackets1_19_1.SYSTEM_CHAT, wrapper -> {
            final JsonElement content = wrapper.passthrough(Types.COMPONENT);
            translatableRewriter.processText(wrapper.user(), content);

            final boolean overlay = wrapper.read(Types.BOOLEAN);
            wrapper.write(Types.VAR_INT, overlay ? GAME_INFO_ID : SYSTEM_CHAT_ID);
        });

        registerServerbound(ServerboundPackets1_19.CHAT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Message
                map(Types.LONG); // Timestamp
                map(Types.LONG); // Salt
                read(Types.BYTE_ARRAY_PRIMITIVE); // Signature
                read(Types.BOOLEAN); // Signed preview
                handler(wrapper -> {
                    final ChatSession1_19_1 chatSession = wrapper.user().get(ChatSession1_19_1.class);
                    final ReceivedMessagesStorage messagesStorage = wrapper.user().get(ReceivedMessagesStorage.class);

                    if (chatSession != null) {
                        final UUID sender = wrapper.user().getProtocolInfo().getUuid();
                        final String message = wrapper.get(Types.STRING, 0);
                        final long timestamp = wrapper.get(Types.LONG, 0);
                        final long salt = wrapper.get(Types.LONG, 1);

                        final MessageMetadata metadata = new MessageMetadata(sender, timestamp, salt);
                        final DecoratableMessage decoratableMessage = new DecoratableMessage(message);
                        final byte[] signature;
                        try {
                            signature = chatSession.signChatMessage(metadata, decoratableMessage, messagesStorage.lastSignatures());
                        } catch (final SignatureException e) {
                            throw new RuntimeException(e);
                        }

                        wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, signature); // Signature
                        wrapper.write(Types.BOOLEAN, decoratableMessage.isDecorated()); // Signed preview
                    } else {
                        wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, EMPTY_BYTES); // Signature
                        wrapper.write(Types.BOOLEAN, false); // Signed preview
                    }

                    messagesStorage.resetUnacknowledgedCount();
                    wrapper.write(Types.PLAYER_MESSAGE_SIGNATURE_ARRAY, messagesStorage.lastSignatures());
                    wrapper.write(Types.OPTIONAL_PLAYER_MESSAGE_SIGNATURE, null); // No last unacknowledged
                });
            }
        });

        registerServerbound(ServerboundPackets1_19.CHAT_COMMAND, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Command
                map(Types.LONG); // Timestamp
                map(Types.LONG); // Salt
                handler(wrapper -> {
                    final ReceivedMessagesStorage messagesStorage = wrapper.user().get(ReceivedMessagesStorage.class);
                    final ChatSession1_19_1 chatSession = wrapper.user().get(ChatSession1_19_1.class);
                    final SignableCommandArgumentsProvider argumentsProvider = Via.getManager().getProviders().get(SignableCommandArgumentsProvider.class);

                    if (chatSession != null && argumentsProvider != null) {
                        final int signatures = wrapper.read(Types.VAR_INT);
                        for (int i = 0; i < signatures; i++) {
                            wrapper.read(Types.STRING); // Argument name
                            wrapper.read(Types.BYTE_ARRAY_PRIMITIVE); // Signature
                        }

                        final UUID sender = wrapper.user().getProtocolInfo().getUuid();
                        final String command = wrapper.get(Types.STRING, 0);
                        final long timestamp = wrapper.get(Types.LONG, 0);
                        final long salt = wrapper.get(Types.LONG, 1);

                        final MessageMetadata metadata = new MessageMetadata(sender, timestamp, salt);

                        final List<Pair<String, String>> arguments = argumentsProvider.getSignableArguments(command);
                        wrapper.write(Types.VAR_INT, arguments.size());
                        for (final Pair<String, String> argument : arguments) {
                            final byte[] signature;
                            try {
                                signature = chatSession.signChatMessage(metadata, new DecoratableMessage(argument.value()), messagesStorage.lastSignatures());
                            } catch (final SignatureException e) {
                                throw new RuntimeException(e);
                            }

                            wrapper.write(Types.STRING, argument.key());
                            wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, signature);
                        }
                    } else {
                        final int signatures = wrapper.passthrough(Types.VAR_INT);
                        for (int i = 0; i < signatures; i++) {
                            wrapper.passthrough(Types.STRING); // Argument name

                            // Set empty signature
                            wrapper.read(Types.BYTE_ARRAY_PRIMITIVE);
                            wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, EMPTY_BYTES);
                        }
                    }

                    wrapper.passthrough(Types.BOOLEAN); // Signed preview

                    messagesStorage.resetUnacknowledgedCount();
                    wrapper.write(Types.PLAYER_MESSAGE_SIGNATURE_ARRAY, messagesStorage.lastSignatures());
                    wrapper.write(Types.OPTIONAL_PLAYER_MESSAGE_SIGNATURE, null); // No last unacknowledged
                });
            }
        });

        registerClientbound(ClientboundPackets1_19_1.SERVER_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.OPTIONAL_COMPONENT); // Motd
                map(Types.OPTIONAL_STRING); // Encoded icon
                map(Types.BOOLEAN); // Previews chat
                read(Types.BOOLEAN); // Enforces secure chat
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Name
                handler(wrapper -> {
                    final ProfileKey profileKey = wrapper.read(Types.OPTIONAL_PROFILE_KEY); // Profile Key

                    final ChatSession1_19_1 chatSession = wrapper.user().get(ChatSession1_19_1.class);
                    wrapper.write(Types.OPTIONAL_PROFILE_KEY, chatSession == null ? null : chatSession.getProfileKey()); // Profile Key
                    wrapper.write(Types.OPTIONAL_UUID, chatSession == null ? null : chatSession.getUuid()); // Profile uuid

                    if (profileKey == null || chatSession != null) {
                        wrapper.user().put(new NonceStorage(null));
                    }
                });
            }
        });

        registerClientbound(State.LOGIN, ClientboundLoginPackets.HELLO, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Server id
                handler(wrapper -> {
                    if (wrapper.user().has(NonceStorage.class)) {
                        return;
                    }

                    final byte[] publicKey = wrapper.passthrough(Types.BYTE_ARRAY_PRIMITIVE);
                    final byte[] nonce = wrapper.passthrough(Types.BYTE_ARRAY_PRIMITIVE);
                    wrapper.user().put(new NonceStorage(CipherUtil.encryptNonce(publicKey, nonce)));
                });
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.ENCRYPTION_KEY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BYTE_ARRAY_PRIMITIVE); // Key
                handler(wrapper -> {
                    final NonceStorage nonceStorage = wrapper.user().remove(NonceStorage.class);
                    if (nonceStorage.nonce() == null) {
                        return;
                    }

                    final boolean isNonce = wrapper.read(Types.BOOLEAN);
                    wrapper.write(Types.BOOLEAN, true);
                    if (!isNonce) { // Should never be true at this point, but /shrug otherwise
                        wrapper.read(Types.LONG); // Salt
                        wrapper.read(Types.BYTE_ARRAY_PRIMITIVE); // Signature
                        wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, nonceStorage.nonce());
                    }
                });
            }
        });

        registerClientbound(State.LOGIN, ClientboundLoginPackets.CUSTOM_QUERY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT);
                map(Types.STRING);
                handler(wrapper -> {
                    final String identifier = wrapper.get(Types.STRING, 0);
                    if (identifier.equals("velocity:player_info")) {
                        byte[] data = wrapper.passthrough(Types.REMAINING_BYTES);
                        // Velocity modern forwarding version above 1 includes the players public key.
                        // This is an issue because the player does not have a public key.
                        // Velocity itself does adjust the version accordingly: https://github.com/PaperMC/Velocity/blob/1a3fba4250553702d9dcd05731d04347bfc24c9f/proxy/src/main/java/com/velocitypowered/proxy/connection/backend/LoginSessionHandler.java#L176-L197
                        // However this becomes an issue when an 1.19.0 client tries to join a 1.19.1 server.
                        // (The protocol translation will go 1.19.1 -> 1.18.2 -> 1.19.0)
                        // The player does have a public key, but an outdated one.
                        // Velocity modern forwarding versions: https://github.com/PaperMC/Velocity/blob/1a3fba4250553702d9dcd05731d04347bfc24c9f/proxy/src/main/java/com/velocitypowered/proxy/connection/VelocityConstants.java#L27-L29
                        // And the version can be specified with a single byte: https://github.com/PaperMC/Velocity/blob/1a3fba4250553702d9dcd05731d04347bfc24c9f/proxy/src/main/java/com/velocitypowered/proxy/connection/backend/LoginSessionHandler.java#L88
                        if (data.length == 1 && data[0] > 1) {
                            data[0] = 1;
                        } else if (data.length == 0) { // Or the version is omitted (default version would be used)
                            data = new byte[]{1};
                            wrapper.set(Types.REMAINING_BYTES, 0, data);
                        } else {
                            getLogger().warning("Received unexpected data in velocity:player_info (length=" + data.length + ")");
                        }
                    }
                });
            }
        });

        cancelClientbound(ClientboundPackets1_19_1.CUSTOM_CHAT_COMPLETIONS); // Can't do anything with them unless we add clutter clients with fake player profiles
        cancelClientbound(ClientboundPackets1_19_1.DELETE_CHAT); // Can't do without the old "send 50 empty lines and then resend previous messages" trick
        cancelClientbound(ClientboundPackets1_19_1.PLAYER_CHAT_HEADER);
    }

    @Override
    public void init(final UserConnection user) {
        user.put(new ChatRegistryStorage1_19_1());
        user.put(new ReceivedMessagesStorage());
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_19.PLAYER));
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPackets1_19_1> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public EntityPacketRewriter1_19_1 getEntityRewriter() {
        return entityRewriter;
    }

    public static @Nullable JsonElement decorateChatMessage(final Protocol protocol, final ChatRegistryStorage chatRegistryStorage,
                                                            final int chatTypeId, final JsonElement senderName,
                                                            @Nullable final JsonElement targetName, final JsonElement message) {
        CompoundTag chatType = chatRegistryStorage.chatType(chatTypeId);
        if (chatType == null) {
            protocol.getLogger().warning("Chat message has unknown chat type id " + chatTypeId + ". Message: " + message);
            return null;
        }

        chatType = chatType.getCompoundTag("element").getCompoundTag("chat");
        if (chatType == null) {
            return null;
        }

        return Protocol1_19To1_19_1.translatabaleComponentFromTag(chatType, senderName, targetName, message);
    }
}
