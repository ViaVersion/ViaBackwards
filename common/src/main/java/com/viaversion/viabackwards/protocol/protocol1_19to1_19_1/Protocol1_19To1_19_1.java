/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2022 ViaVersion and contributors
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
import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19_1.storage.ReceivedMessagesStorage;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.packets.EntityPackets1_19_1;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.storage.ChatTypeStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.MappingDataLoader;
import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature;
import com.viaversion.viaversion.api.minecraft.nbt.BinaryTagIO;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
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
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.storage.NonceStorage;
import com.viaversion.viaversion.util.CipherUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Protocol1_19To1_19_1 extends BackwardsProtocol<ClientboundPackets1_19_1, ClientboundPackets1_19, ServerboundPackets1_19_1, ServerboundPackets1_19> {

    private static final byte[] EMPTY_BYTES = new byte[0];
    private static final UUID ZERO_UUID = new UUID(0, 0);
    // Contains 1.19 chat types
    private static final CompoundTag CHAT_REGISTRY;

    static {
        try {
            CHAT_REGISTRY = new CompoundTag();
            CHAT_REGISTRY.put("type", new StringTag("minecraft:chat_type"));
            ListTag values = BinaryTagIO.readCompressedInputStream(MappingDataLoader.getResource("chat-types-1.19.nbt")).get("values");
            CHAT_REGISTRY.put("value", values);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private final TranslatableRewriter translatableRewriter = new TranslatableRewriter(this, "1.19.1");
    private final EntityPackets1_19_1 entityRewriter = new EntityPackets1_19_1(this);

    public Protocol1_19To1_19_1() {
        super(ClientboundPackets1_19_1.class, ClientboundPackets1_19.class, ServerboundPackets1_19_1.class, ServerboundPackets1_19.class);
    }

    @Override
    protected void registerPackets() {
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.ACTIONBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.TITLE_SUBTITLE);
        translatableRewriter.registerBossBar(ClientboundPackets1_19_1.BOSSBAR);
        translatableRewriter.registerDisconnect(ClientboundPackets1_19_1.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19_1.TAB_LIST);
        translatableRewriter.registerOpenWindow(ClientboundPackets1_19_1.OPEN_WINDOW);
        translatableRewriter.registerCombatKill(ClientboundPackets1_19_1.COMBAT_KILL);
        translatableRewriter.registerPing();

        entityRewriter.register();

        registerClientbound(ClientboundPackets1_19_1.JOIN_GAME, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.INT); // Entity ID
                map(Type.BOOLEAN); // Hardcore
                map(Type.UNSIGNED_BYTE); // Gamemode
                map(Type.BYTE); // Previous Gamemode
                map(Type.STRING_ARRAY); // World List
                handler(wrapper -> {
                    final ChatTypeStorage chatTypeStorage = wrapper.user().get(ChatTypeStorage.class);
                    chatTypeStorage.clear();

                    final CompoundTag registry = wrapper.passthrough(Type.NBT);
                    final ListTag chatTypes = ((CompoundTag) registry.get("minecraft:chat_type")).get("value");
                    for (final Tag chatType : chatTypes) {
                        final CompoundTag chatTypeCompound = (CompoundTag) chatType;
                        final NumberTag idTag = chatTypeCompound.get("id");
                        chatTypeStorage.addChatType(idTag.asInt(), chatTypeCompound);
                    }

                    // Replace with 1.19 chat types
                    // Ensures that the client has a chat type for system message, with and without overlay
                    registry.put("minecraft:chat_type", CHAT_REGISTRY.clone());
                });
            }
        });

        registerClientbound(ClientboundPackets1_19_1.SYSTEM_CHAT, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    final JsonElement content = wrapper.passthrough(Type.COMPONENT);
                    translatableRewriter.processText(content);

                    final boolean overlay = wrapper.read(Type.BOOLEAN);
                    final int type = overlayId(overlay);
                    wrapper.write(Type.VAR_INT, type); // Chat type
                });
            }
        });

        registerClientbound(ClientboundPackets1_19_1.PLAYER_CHAT, ClientboundPackets1_19.SYSTEM_CHAT, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    if (wrapper.read(Type.BOOLEAN)) {
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE); // Previous signature
                    }

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
                    final String plainMessage = wrapper.read(Type.STRING);
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
                    final JsonElement senderName = wrapper.read(Type.COMPONENT); // Chat sender name
                    final JsonElement targetName = wrapper.read(Type.OPTIONAL_COMPONENT); // Chat sender target/team name
                    decoratedMessage = decorateChatMessage(wrapper, chatTypeId, senderName, targetName, message);
                    if (decoratedMessage == null) {
                        wrapper.cancel();
                        return;
                    }
                    wrapper.write(Type.COMPONENT, decoratedMessage);
                    wrapper.write(Type.VAR_INT, overlayId(false));
                });
            }
        });

        registerServerbound(ServerboundPackets1_19.CHAT_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Message
                map(Type.LONG); // Timestamp
                map(Type.LONG); // Salt
                read(Type.BYTE_ARRAY_PRIMITIVE);
                create(Type.BYTE_ARRAY_PRIMITIVE, EMPTY_BYTES); // Signature
                read(Type.BOOLEAN);
                create(Type.BOOLEAN, false); // Signed preview
                handler(wrapper -> {
                    final ReceivedMessagesStorage messagesStorage = wrapper.user().get(ReceivedMessagesStorage.class);
                    messagesStorage.resetUnacknowledgedCount();
                    wrapper.write(Type.PLAYER_MESSAGE_SIGNATURE_ARRAY, messagesStorage.lastSignatures());
                    wrapper.write(Type.OPTIONAL_PLAYER_MESSAGE_SIGNATURE, null); // No last unacknowledged
                });
            }
        });

        registerServerbound(ServerboundPackets1_19.CHAT_COMMAND, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Command
                map(Type.LONG); // Timestamp
                map(Type.LONG); // Salt
                handler(wrapper -> {
                    final int signatures = wrapper.read(Type.VAR_INT);
                    for (int i = 0; i < signatures; i++) {
                        wrapper.read(Type.STRING); // Argument name
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE); // Signature
                    }
                    wrapper.write(Type.VAR_INT, 0); // No signatures
                });
                read(Type.BOOLEAN);
                create(Type.BOOLEAN, false); // Signed preview
                handler(wrapper -> {
                    final ReceivedMessagesStorage messagesStorage = wrapper.user().get(ReceivedMessagesStorage.class);
                    messagesStorage.resetUnacknowledgedCount();
                    wrapper.write(Type.PLAYER_MESSAGE_SIGNATURE_ARRAY, messagesStorage.lastSignatures());
                    wrapper.write(Type.OPTIONAL_PLAYER_MESSAGE_SIGNATURE, null); // No last unacknowledged
                });
            }
        });

        registerClientbound(ClientboundPackets1_19_1.SERVER_DATA, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.OPTIONAL_COMPONENT); // Motd
                map(Type.OPTIONAL_STRING); // Encoded icon
                map(Type.BOOLEAN); // Previews chat
                read(Type.BOOLEAN); // Enforces secure chat
            }
        });

        // Login changes
        registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO.getId(), ServerboundLoginPackets.HELLO.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Name
                read(Type.OPTIONAL_PROFILE_KEY);
                create(Type.OPTIONAL_PROFILE_KEY, null); // Profile key is incompatible, use empty
                create(Type.OPTIONAL_UUID, null); // Profile id
            }
        });

        registerClientbound(State.LOGIN, ClientboundLoginPackets.HELLO.getId(), ClientboundLoginPackets.HELLO.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Server id
                handler(wrapper -> {
                    final byte[] publicKey = wrapper.passthrough(Type.BYTE_ARRAY_PRIMITIVE);
                    final byte[] nonce = wrapper.passthrough(Type.BYTE_ARRAY_PRIMITIVE);
                    wrapper.user().put(new NonceStorage(CipherUtil.encryptNonce(publicKey, nonce)));
                });
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.ENCRYPTION_KEY.getId(), ServerboundLoginPackets.ENCRYPTION_KEY.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE_ARRAY_PRIMITIVE); // Key
                handler(wrapper -> {
                    final boolean isNonce = wrapper.read(Type.BOOLEAN);
                    wrapper.write(Type.BOOLEAN, true); // Always use nonce since it is compatible
                    if (isNonce) {
                        // Nonce, just pass it through
                        wrapper.passthrough(Type.BYTE_ARRAY_PRIMITIVE);
                    } else {
                        final NonceStorage nonceStorage = wrapper.user().remove(NonceStorage.class);
                        if (nonceStorage == null) {
                            throw new IllegalArgumentException("Server sent nonce is missing");
                        }

                        wrapper.read(Type.LONG); // Salt
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE); // Signature
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, nonceStorage.nonce());
                    }
                });
            }
        });

        cancelClientbound(ClientboundPackets1_19_1.CUSTOM_CHAT_COMPLETIONS);
        cancelClientbound(ClientboundPackets1_19_1.DELETE_CHAT_MESSAGE);
        cancelClientbound(ClientboundPackets1_19_1.PLAYER_CHAT_HEADER);
    }

    @Override
    public TranslatableRewriter getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    public EntityRewriter<Protocol1_19To1_19_1> getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public void init(final UserConnection user) {
        user.put(new ReceivedMessagesStorage());
        user.put(new ChatTypeStorage());
        addEntityTracker(user, new EntityTrackerBase(user, null));
    }

    private @Nullable JsonElement decorateChatMessage(final PacketWrapper wrapper, final int chatTypeId, final JsonElement senderName, @Nullable final JsonElement targetName, final JsonElement message) {
        translatableRewriter.processText(message);

        final CompoundTag chatType = wrapper.user().get(ChatTypeStorage.class).chatType(chatTypeId);
        if (chatType == null) {
            ViaBackwards.getPlatform().getLogger().warning("Chat message has unknown chat type id " + chatTypeId + ". Message: " + message);
            return null;
        }

        final CompoundTag chatData = chatType.<CompoundTag>get("element").get("chat");
        if (chatData == null) {
            return null;
        }

        final String translationKey = (String) chatData.get("translation_key").getValue();
        final TranslatableComponent.Builder componentBuilder = Component.translatable().key(translationKey);

        // Add the style
        final CompoundTag style = chatData.get("style");
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
        final ListTag parameters = chatData.get("parameters");
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

    private static int overlayId(boolean overlay) {
        return overlay ? 2 : 1;
    }

}
