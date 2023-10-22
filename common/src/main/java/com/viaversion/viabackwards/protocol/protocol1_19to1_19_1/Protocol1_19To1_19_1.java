/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2023 ViaVersion and contributors
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
package com.viaversion.viabackwards.protocol.protocol1_19to1_19_1;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.packets.EntityPackets1_19_1;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.storage.ChatRegistryStorage;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.storage.ChatRegistryStorage1_19_1;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.storage.NonceStorage;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.storage.ReceivedMessagesStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19;
import com.viaversion.viaversion.api.minecraft.signature.model.DecoratableMessage;
import com.viaversion.viaversion.api.minecraft.signature.model.MessageMetadata;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_1;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.kyori.adventure.text.Component;
import com.viaversion.viaversion.libs.kyori.adventure.text.TranslatableComponent;
import com.viaversion.viaversion.libs.kyori.adventure.text.format.NamedTextColor;
import com.viaversion.viaversion.libs.kyori.adventure.text.format.Style;
import com.viaversion.viaversion.libs.kyori.adventure.text.format.TextDecoration;
import com.viaversion.viaversion.libs.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.*;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ClientboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ServerboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ClientboundPackets1_19;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.ServerboundPackets1_19;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.packets.EntityPackets;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
import com.viaversion.viaversion.util.CipherUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Protocol1_19To1_19_1 extends BackwardsProtocol<ClientboundPackets1_19_1, ClientboundPackets1_19, ServerboundPackets1_19_1, ServerboundPackets1_19> {

    public static final int SYSTEM_CHAT_ID = 1;
    public static final int GAME_INFO_ID = 2;
    private static final UUID ZERO_UUID = new UUID(0, 0);
    private static final byte[] EMPTY_BYTES = new byte[0];
    private final EntityPackets1_19_1 entityRewriter = new EntityPackets1_19_1(this);
    private final TranslatableRewriter<ClientboundPackets1_19_1> translatableRewriter = new TranslatableRewriter<>(this, ComponentRewriter.ReadType.JSON);

    public Protocol1_19To1_19_1() {
        super(ClientboundPackets1_19_1.class, ClientboundPackets1_19.class, ServerboundPackets1_19_1.class, ServerboundPackets1_19.class);
    }

    @Override
    protected void registerPackets() {
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.ACTIONBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.TITLE_SUBTITLE);
        translatableRewriter.registerBossBar(ClientboundPackets1_19_1.BOSSBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19_1.TAB_LIST);
        translatableRewriter.registerOpenWindow(ClientboundPackets1_19_1.OPEN_WINDOW);
        translatableRewriter.registerCombatKill(ClientboundPackets1_19_1.COMBAT_KILL);
        translatableRewriter.registerPing();

        entityRewriter.register();

        registerClientbound(ClientboundPackets1_19_1.JOIN_GAME, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.INT); // Entity ID
                map(Type.BOOLEAN); // Hardcore
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // World List
                map(Type.NAMED_COMPOUND_TAG); // Dimension registry
                map(Type.STRING); // Dimension key
                map(Type.STRING); // World
                handler(wrapper -> {
                    final ChatRegistryStorage chatTypeStorage = wrapper.user().get(ChatRegistryStorage1_19_1.class);
                    chatTypeStorage.clear();

                    final CompoundTag registry = wrapper.get(Type.NAMED_COMPOUND_TAG, 0);
                    final ListTag chatTypes = ((CompoundTag) registry.get("minecraft:chat_type")).get("value");
                    for (final Tag chatType : chatTypes) {
                        final CompoundTag chatTypeCompound = (CompoundTag) chatType;
                        final NumberTag idTag = chatTypeCompound.get("id");
                        chatTypeStorage.addChatType(idTag.asInt(), chatTypeCompound);
                    }

                    // Replace with 1.19 chat types
                    // Ensures that the client has a chat type for system message, with and without overlay
                    registry.put("minecraft:chat_type", EntityPackets.CHAT_REGISTRY.clone());
                });
                handler(entityRewriter.worldTrackerHandlerByKey());
            }
        });

        registerClientbound(ClientboundPackets1_19_1.PLAYER_CHAT, ClientboundPackets1_19.SYSTEM_CHAT, wrapper -> {
            wrapper.read(Type.OPTIONAL_BYTE_ARRAY_PRIMITIVE); // Previous signature

            final PlayerMessageSignature signature = wrapper.read(Type.PLAYER_MESSAGE_SIGNATURE);

            // Store message signature for last seen
            if (!signature.uuid().equals(ZERO_UUID) && signature.signatureBytes().length != 0) {
                final ReceivedMessagesStorage messagesStorage = wrapper.user().get(ReceivedMessagesStorage.class);
                messagesStorage.add(signature);
                if (messagesStorage.tickUnacknowledged() > 64) {
                    messagesStorage.resetUnacknowledgedCount();

                    // Send chat acknowledgement
                    final PacketWrapper chatAckPacket = wrapper.create(ServerboundPackets1_19_1.CHAT_ACK);
                    chatAckPacket.write(Type.PLAYER_MESSAGE_SIGNATURE_ARRAY, messagesStorage.lastSignatures());
                    chatAckPacket.write(Type.OPTIONAL_PLAYER_MESSAGE_SIGNATURE, null);
                    chatAckPacket.sendToServer(Protocol1_19To1_19_1.class);
                }
            }

            // Send the unsigned message if present, otherwise the signed message
            final String plainMessage = wrapper.read(Type.STRING); // Plain message
            JsonElement message = null;
            JsonElement decoratedMessage = wrapper.read(Type.OPTIONAL_COMPONENT);
            if (decoratedMessage != null) {
                message = decoratedMessage;
            }

            wrapper.read(Type.LONG); // Timestamp
            wrapper.read(Type.LONG); // Salt
            wrapper.read(Type.PLAYER_MESSAGE_SIGNATURE_ARRAY); // Last seen

            final JsonElement unsignedMessage = wrapper.read(Type.OPTIONAL_COMPONENT);
            if (unsignedMessage != null) {
                message = unsignedMessage;
            }
            if (message == null) {
                // If no decorated or unsigned message is given, use the plain one
                message = GsonComponentSerializer.gson().serializeToTree(Component.text(plainMessage));
            }

            final int filterMaskType = wrapper.read(Type.VAR_INT);
            if (filterMaskType == 2) { // Partially filtered
                wrapper.read(Type.LONG_ARRAY_PRIMITIVE); // Mask
            }

            final int chatTypeId = wrapper.read(Type.VAR_INT);
            final JsonElement senderName = wrapper.read(Type.COMPONENT);
            final JsonElement targetName = wrapper.read(Type.OPTIONAL_COMPONENT);
            decoratedMessage = decorateChatMessage(wrapper.user().get(ChatRegistryStorage1_19_1.class), chatTypeId, senderName, targetName, message);
            if (decoratedMessage == null) {
                wrapper.cancel();
                return;
            }

            translatableRewriter.processText(decoratedMessage);
            wrapper.write(Type.COMPONENT, decoratedMessage);
            wrapper.write(Type.VAR_INT, SYSTEM_CHAT_ID);
        });

        registerClientbound(ClientboundPackets1_19_1.SYSTEM_CHAT, wrapper -> {
            final JsonElement content = wrapper.passthrough(Type.COMPONENT);
            translatableRewriter.processText(content);

            final boolean overlay = wrapper.read(Type.BOOLEAN);
            wrapper.write(Type.VAR_INT, overlay ? GAME_INFO_ID : SYSTEM_CHAT_ID);
        });

        registerServerbound(ServerboundPackets1_19.CHAT_MESSAGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Message
                map(Type.LONG); // Timestamp
                map(Type.LONG); // Salt
                read(Type.BYTE_ARRAY_PRIMITIVE); // Signature
                read(Type.BOOLEAN); // Signed preview
                handler(wrapper -> {
                    final ChatSession1_19_1 chatSession = wrapper.user().get(ChatSession1_19_1.class);
                    final ReceivedMessagesStorage messagesStorage = wrapper.user().get(ReceivedMessagesStorage.class);

                    if (chatSession != null) {
                        final UUID sender = wrapper.user().getProtocolInfo().getUuid();
                        final String message = wrapper.get(Type.STRING, 0);
                        final long timestamp = wrapper.get(Type.LONG, 0);
                        final long salt = wrapper.get(Type.LONG, 1);

                        final MessageMetadata metadata = new MessageMetadata(sender, timestamp, salt);
                        final DecoratableMessage decoratableMessage = new DecoratableMessage(message);
                        final byte[] signature = chatSession.signChatMessage(metadata, decoratableMessage, messagesStorage.lastSignatures());

                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, signature); // Signature
                        wrapper.write(Type.BOOLEAN, decoratableMessage.isDecorated()); // Signed preview
                    } else {
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, EMPTY_BYTES); // Signature
                        wrapper.write(Type.BOOLEAN, false); // Signed preview
                    }

                    messagesStorage.resetUnacknowledgedCount();
                    wrapper.write(Type.PLAYER_MESSAGE_SIGNATURE_ARRAY, messagesStorage.lastSignatures());
                    wrapper.write(Type.OPTIONAL_PLAYER_MESSAGE_SIGNATURE, null); // No last unacknowledged
                });
            }
        });

        registerServerbound(ServerboundPackets1_19.CHAT_COMMAND, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Command
                map(Type.LONG); // Timestamp
                map(Type.LONG); // Salt
                handler(wrapper -> {
                    final int signatures = wrapper.passthrough(Type.VAR_INT);
                    for (int i = 0; i < signatures; i++) {
                        wrapper.passthrough(Type.STRING); // Argument name

                        // Set empty signature
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE);
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, EMPTY_BYTES);
                    }

                    wrapper.passthrough(Type.BOOLEAN); // Signed preview

                    final ReceivedMessagesStorage messagesStorage = wrapper.user().get(ReceivedMessagesStorage.class);
                    messagesStorage.resetUnacknowledgedCount();
                    wrapper.write(Type.PLAYER_MESSAGE_SIGNATURE_ARRAY, messagesStorage.lastSignatures());
                    wrapper.write(Type.OPTIONAL_PLAYER_MESSAGE_SIGNATURE, null); // No last unacknowledged
                });
            }
        });

        registerClientbound(ClientboundPackets1_19_1.SERVER_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.OPTIONAL_COMPONENT); // Motd
                map(Type.OPTIONAL_STRING); // Encoded icon
                map(Type.BOOLEAN); // Previews chat
                read(Type.BOOLEAN); // Enforces secure chat
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO.getId(), ServerboundLoginPackets.HELLO.getId(), new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Name
                handler(wrapper -> {
                    final ProfileKey profileKey = wrapper.read(Type.OPTIONAL_PROFILE_KEY); // Profile Key

                    final ChatSession1_19_1 chatSession = wrapper.user().get(ChatSession1_19_1.class);
                    wrapper.write(Type.OPTIONAL_PROFILE_KEY, chatSession == null ? null : chatSession.getProfileKey()); // Profile Key
                    wrapper.write(Type.OPTIONAL_UUID, chatSession == null ? null : chatSession.getUuid()); // Profile uuid

                    if (profileKey == null || chatSession != null) {
                        wrapper.user().put(new NonceStorage(null));
                    }
                });
            }
        });

        registerClientbound(State.LOGIN, ClientboundLoginPackets.HELLO.getId(), ClientboundLoginPackets.HELLO.getId(), new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Server id
                handler(wrapper -> {
                    if (wrapper.user().has(NonceStorage.class)) {
                        return;
                    }

                    final byte[] publicKey = wrapper.passthrough(Type.BYTE_ARRAY_PRIMITIVE);
                    final byte[] nonce = wrapper.passthrough(Type.BYTE_ARRAY_PRIMITIVE);
                    wrapper.user().put(new NonceStorage(CipherUtil.encryptNonce(publicKey, nonce)));
                });
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.ENCRYPTION_KEY.getId(), ServerboundLoginPackets.ENCRYPTION_KEY.getId(), new PacketHandlers() {
            @Override
            public void register() {
                map(Type.BYTE_ARRAY_PRIMITIVE); // Key
                handler(wrapper -> {
                    final NonceStorage nonceStorage = wrapper.user().remove(NonceStorage.class);
                    if (nonceStorage.nonce() == null) {
                        return;
                    }

                    final boolean isNonce = wrapper.read(Type.BOOLEAN);
                    wrapper.write(Type.BOOLEAN, true);
                    if (!isNonce) { // Should never be true at this point, but /shrug otherwise
                        wrapper.read(Type.LONG); // Salt
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE); // Signature
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, nonceStorage.nonce());
                    }
                });
            }
        });

        registerClientbound(State.LOGIN, ClientboundLoginPackets.CUSTOM_QUERY.getId(), ClientboundLoginPackets.CUSTOM_QUERY.getId(), new PacketHandlers() {
            @Override
            public void register() {
                map(Type.VAR_INT);
                map(Type.STRING);
                handler(wrapper -> {
                    final String identifier = wrapper.get(Type.STRING, 0);
                    if (identifier.equals("velocity:player_info")) {
                        byte[] data = wrapper.passthrough(Type.REMAINING_BYTES);
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
                            wrapper.set(Type.REMAINING_BYTES, 0, data);
                        } else {
                            ViaBackwards.getPlatform().getLogger().warning("Received unexpected data in velocity:player_info (length=" + data.length + ")");
                        }
                    }
                });
            }
        });

        cancelClientbound(ClientboundPackets1_19_1.CUSTOM_CHAT_COMPLETIONS); // Can't do anything with them unless we add clutter clients with fake player profiles
        cancelClientbound(ClientboundPackets1_19_1.DELETE_CHAT_MESSAGE); // Can't do without the old "send 50 empty lines and then resend previous messages" trick
        cancelClientbound(ClientboundPackets1_19_1.PLAYER_CHAT_HEADER);
    }

    @Override
    public void init(final UserConnection user) {
        user.put(new ChatRegistryStorage1_19_1());
        user.put(new ReceivedMessagesStorage());
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_19.PLAYER));
    }

    @Override
    public TranslatableRewriter<ClientboundPackets1_19_1> getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    public EntityPackets1_19_1 getEntityRewriter() {
        return entityRewriter;
    }

    public static @Nullable JsonElement decorateChatMessage(final ChatRegistryStorage chatRegistryStorage, final int chatTypeId, final JsonElement senderName, @Nullable final JsonElement targetName, final JsonElement message) {
        CompoundTag chatType = chatRegistryStorage.chatType(chatTypeId);
        if (chatType == null) {
            ViaBackwards.getPlatform().getLogger().warning("Chat message has unknown chat type id " + chatTypeId + ". Message: " + message);
            return null;
        }

        chatType = chatType.<CompoundTag>get("element").get("chat");
        if (chatType == null) {
            return null;
        }

        final String translationKey = (String) chatType.get("translation_key").getValue();
        final TranslatableComponent.Builder componentBuilder = Component.translatable().key(translationKey);

        // Add the style
        final CompoundTag style = chatType.get("style");
        if (style != null) {
            final Style.Builder styleBuilder = Style.style();
            final StringTag color = style.get("color");
            if (color != null) {
                final NamedTextColor textColor = NamedTextColor.NAMES.value(color.getValue());
                if (textColor != null) {
                    styleBuilder.color(NamedTextColor.NAMES.value(color.getValue()));
                }
            }

            for (final String key : TextDecoration.NAMES.keys()) {
                if (style.contains(key)) {
                    styleBuilder.decoration(TextDecoration.NAMES.value(key), style.<ByteTag>get(key).asByte() == 1);
                }
            }
            componentBuilder.style(styleBuilder.build());
        }

        // Add the replacements
        final ListTag parameters = chatType.get("parameters");
        if (parameters != null) {
            final List<Component> arguments = new ArrayList<>();
            for (final Tag element : parameters) {
                JsonElement argument = null;
                switch ((String) element.getValue()) {
                    case "sender":
                        argument = senderName;
                        break;
                    case "content":
                        argument = message;
                        break;
                    case "target":
                        Preconditions.checkNotNull(targetName, "Target name is null");
                        argument = targetName;
                        break;
                    default:
                        ViaBackwards.getPlatform().getLogger().warning("Unknown parameter for chat decoration: " + element.getValue());
                }
                if (argument != null) {
                    arguments.add(GsonComponentSerializer.gson().deserializeFromTree(argument));
                }
            }
            componentBuilder.args(arguments);
        }

        return GsonComponentSerializer.gson().serializeToTree(componentBuilder.build());
    }
}
