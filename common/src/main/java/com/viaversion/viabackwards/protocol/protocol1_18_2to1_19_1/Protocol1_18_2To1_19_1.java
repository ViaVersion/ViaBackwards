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
package com.viaversion.viabackwards.protocol.protocol1_18_2to1_19_1;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.ViaBackwards;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19_1.data.BackwardsMappings;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19_1.data.CommandRewriter1_19;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19_1.packets.BlockItemPackets1_19;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19_1.packets.EntityPackets1_19;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19_1.storage.DimensionRegistryStorage;
import com.viaversion.viabackwards.protocol.protocol1_18_2to1_19_1.storage.ReceivedMessagesStorage;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19Types;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketRemapper;
import com.viaversion.viaversion.api.rewriter.EntityRewriter;
import com.viaversion.viaversion.api.rewriter.ItemRewriter;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.kyori.adventure.text.Component;
import com.viaversion.viaversion.libs.kyori.adventure.text.TranslatableComponent;
import com.viaversion.viaversion.libs.kyori.adventure.text.format.NamedTextColor;
import com.viaversion.viaversion.libs.kyori.adventure.text.format.Style;
import com.viaversion.viaversion.libs.kyori.adventure.text.format.TextDecoration;
import com.viaversion.viaversion.libs.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_17to1_16_4.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.protocol1_18to1_17_1.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ClientboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ServerboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19to1_18_2.Protocol1_19To1_18_2;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Protocol1_18_2To1_19_1 extends BackwardsProtocol<ClientboundPackets1_19_1, ClientboundPackets1_18, ServerboundPackets1_19_1, ServerboundPackets1_17> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings();
    private static final UUID ZERO_UUID = new UUID(0, 0);
    private static final byte[] EMPTY_BYTES = new byte[0];
    private final EntityPackets1_19 entityRewriter = new EntityPackets1_19(this);
    private final BlockItemPackets1_19 blockItemPackets = new BlockItemPackets1_19(this);
    private final TranslatableRewriter translatableRewriter = new TranslatableRewriter(this);

    public Protocol1_18_2To1_19_1() {
        super(ClientboundPackets1_19_1.class, ClientboundPackets1_18.class, ServerboundPackets1_19_1.class, ServerboundPackets1_17.class);
    }

    @Override
    protected void registerPackets() {
        executeAsyncAfterLoaded(Protocol1_19To1_18_2.class, () -> {
            MAPPINGS.load();
            entityRewriter.onMappingDataLoaded();
        });

        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.ACTIONBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_1.TITLE_SUBTITLE);
        translatableRewriter.registerBossBar(ClientboundPackets1_19_1.BOSSBAR);
        translatableRewriter.registerDisconnect(ClientboundPackets1_19_1.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19_1.TAB_LIST);
        translatableRewriter.registerOpenWindow(ClientboundPackets1_19_1.OPEN_WINDOW);
        translatableRewriter.registerCombatKill(ClientboundPackets1_19_1.COMBAT_KILL);
        translatableRewriter.registerPing();

        blockItemPackets.register();
        entityRewriter.register();

        final SoundRewriter soundRewriter = new SoundRewriter(this);
        soundRewriter.registerStopSound(ClientboundPackets1_19_1.STOP_SOUND);
        registerClientbound(ClientboundPackets1_19_1.SOUND, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Sound id
                map(Type.VAR_INT); // Source
                map(Type.INT); // X
                map(Type.INT); // Y
                map(Type.INT); // Z
                map(Type.FLOAT); // Volume
                map(Type.FLOAT); // Pitch
                read(Type.LONG); // Seed
                handler(soundRewriter.getSoundHandler());
            }
        });
        registerClientbound(ClientboundPackets1_19_1.ENTITY_SOUND, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT); // Sound id
                map(Type.VAR_INT); // Source
                map(Type.VAR_INT); // Entity id
                map(Type.FLOAT); // Volume
                map(Type.FLOAT); // Pitch
                read(Type.LONG); // Seed
                handler(soundRewriter.getSoundHandler());
            }
        });
        registerClientbound(ClientboundPackets1_19_1.NAMED_SOUND, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Sound name
                map(Type.VAR_INT); // Source
                map(Type.INT); // X
                map(Type.INT); // Y
                map(Type.INT); // Z
                map(Type.FLOAT); // Volume
                map(Type.FLOAT); // Pitch
                read(Type.LONG); // Seed
                handler(soundRewriter.getNamedSoundHandler());
            }
        });

        final TagRewriter tagRewriter = new TagRewriter(this);
        tagRewriter.removeTags("minecraft:banner_pattern");
        tagRewriter.removeTags("minecraft:instrument");
        tagRewriter.removeTags("minecraft:cat_variant");
        tagRewriter.removeTags("minecraft:painting_variant");
        tagRewriter.addEmptyTag(RegistryType.BLOCK, "minecraft:polar_bears_spawnable_on_in_frozen_ocean");
        tagRewriter.renameTag(RegistryType.BLOCK, "minecraft:wool_carpets", "minecraft:carpets");
        tagRewriter.renameTag(RegistryType.ITEM, "minecraft:wool_carpets", "minecraft:carpets");
        tagRewriter.addEmptyTag(RegistryType.ITEM, "minecraft:occludes_vibration_signals");
        tagRewriter.registerGeneric(ClientboundPackets1_19_1.TAGS);

        new StatisticsRewriter(this).register(ClientboundPackets1_19_1.STATISTICS);

        final CommandRewriter commandRewriter = new CommandRewriter1_19(this);
        registerClientbound(ClientboundPackets1_19_1.DECLARE_COMMANDS, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    final int size = wrapper.passthrough(Type.VAR_INT);
                    for (int i = 0; i < size; i++) {
                        final byte flags = wrapper.passthrough(Type.BYTE);
                        wrapper.passthrough(Type.VAR_INT_ARRAY_PRIMITIVE); // Children indices
                        if ((flags & 0x08) != 0) {
                            wrapper.passthrough(Type.VAR_INT); // Redirect node index
                        }

                        final int nodeType = flags & 0x03;
                        if (nodeType == 1 || nodeType == 2) { // Literal/argument node
                            wrapper.passthrough(Type.STRING); // Name
                        }

                        if (nodeType == 2) { // Argument node
                            final int argumentTypeId = wrapper.read(Type.VAR_INT);
                            String argumentType = MAPPINGS.argumentType(argumentTypeId);
                            if (argumentType == null) {
                                ViaBackwards.getPlatform().getLogger().warning("Unknown command argument type id: " + argumentTypeId);
                                argumentType = "minecraft:no";
                            }

                            wrapper.write(Type.STRING, commandRewriter.handleArgumentType(argumentType));
                            commandRewriter.handleArgument(wrapper, argumentType);

                            if ((flags & 0x10) != 0) {
                                wrapper.passthrough(Type.STRING); // Suggestion type
                            }
                        }
                    }

                    wrapper.passthrough(Type.VAR_INT); // Root node index
                });
            }
        });

        cancelClientbound(ClientboundPackets1_19_1.SERVER_DATA);
        cancelClientbound(ClientboundPackets1_19_1.CHAT_PREVIEW);
        cancelClientbound(ClientboundPackets1_19_1.SET_DISPLAY_CHAT_PREVIEW);
        registerClientbound(ClientboundPackets1_19_1.PLAYER_CHAT, ClientboundPackets1_18.CHAT_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    if (wrapper.read(Type.BOOLEAN)) {
                        // Previous signature
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE);
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
                            chatAckPacket.sendToServer(Protocol1_18_2To1_19_1.class);
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
                    decoratedMessage = decorateChatMessage(wrapper, chatTypeId, senderName, targetName, message);
                    if (decoratedMessage == null) {
                        wrapper.cancel();
                        return;
                    }

                    translatableRewriter.processText(decoratedMessage);
                    wrapper.write(Type.COMPONENT, decoratedMessage);
                    wrapper.write(Type.BYTE, (byte) 1);
                    wrapper.write(Type.UUID, signature.uuid());
                });
            }
        });

        registerClientbound(ClientboundPackets1_19_1.SYSTEM_CHAT, ClientboundPackets1_18.CHAT_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                handler(wrapper -> {
                    final JsonElement content = wrapper.passthrough(Type.COMPONENT);
                    translatableRewriter.processText(content);

                    final boolean overlay = wrapper.read(Type.BOOLEAN);
                    wrapper.write(Type.BYTE, overlay ? (byte) 2 : (byte) 0);
                });
                create(Type.UUID, ZERO_UUID); // Sender
            }
        });

        registerServerbound(ServerboundPackets1_17.CHAT_MESSAGE, new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Message
                create(Type.LONG, Instant.now().toEpochMilli()); // Timestamp
                create(Type.LONG, 0L); // Salt
                handler(wrapper -> {
                    final String message = wrapper.get(Type.STRING, 0);
                    if (!message.isEmpty() && message.charAt(0) == '/') {
                        wrapper.setPacketType(ServerboundPackets1_19_1.CHAT_COMMAND);
                        wrapper.set(Type.STRING, 0, message.substring(1));
                        wrapper.write(Type.VAR_INT, 0); // No signatures
                    } else {
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, EMPTY_BYTES); // Signature
                    }
                    wrapper.write(Type.BOOLEAN, false); // No signed preview

                    final ReceivedMessagesStorage messagesStorage = wrapper.user().get(ReceivedMessagesStorage.class);
                    messagesStorage.resetUnacknowledgedCount();
                    wrapper.write(Type.PLAYER_MESSAGE_SIGNATURE_ARRAY, messagesStorage.lastSignatures());
                    wrapper.write(Type.OPTIONAL_PLAYER_MESSAGE_SIGNATURE, null); // No last unacknowledged
                });
            }
        });

        // Login changes
        registerClientbound(State.LOGIN, ClientboundLoginPackets.GAME_PROFILE.getId(), ClientboundLoginPackets.GAME_PROFILE.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.UUID); // UUID
                map(Type.STRING); // Name
                handler(wrapper -> {
                    final int properties = wrapper.read(Type.VAR_INT);
                    for (int i = 0; i < properties; i++) {
                        wrapper.read(Type.STRING); // Name
                        wrapper.read(Type.STRING); // Value
                        if (wrapper.read(Type.BOOLEAN)) {
                            wrapper.read(Type.STRING); // Signature
                        }
                    }
                });
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO.getId(), ServerboundLoginPackets.HELLO.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.STRING); // Name
                // Write empty profile key and uuid - requires the enforce-secure-profiles option to be disabled on the server
                create(Type.OPTIONAL_PROFILE_KEY, null);
                create(Type.OPTIONAL_UUID, null);
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.ENCRYPTION_KEY.getId(), ServerboundLoginPackets.ENCRYPTION_KEY.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.BYTE_ARRAY_PRIMITIVE); // Keys
                create(Type.BOOLEAN, true); // Is nonce
            }
        });

        registerClientbound(State.LOGIN, ClientboundLoginPackets.CUSTOM_QUERY.getId(), ClientboundLoginPackets.CUSTOM_QUERY.getId(), new PacketRemapper() {
            @Override
            public void registerMap() {
                map(Type.VAR_INT);
                map(Type.STRING);
                handler(wrapper -> {
                    String identifier = wrapper.get(Type.STRING, 0);
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
                            data = new byte[] { 1 };
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
        user.put(new DimensionRegistryStorage());
        user.put(new ReceivedMessagesStorage());
        addEntityTracker(user, new EntityTrackerBase(user, Entity1_19Types.PLAYER, true));
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public TranslatableRewriter getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    public EntityRewriter getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public ItemRewriter getItemRewriter() {
        return blockItemPackets;
    }

    private @Nullable JsonElement decorateChatMessage(final PacketWrapper wrapper, final int chatTypeId, final JsonElement senderName, @Nullable final JsonElement targetName, final JsonElement message) {
        translatableRewriter.processText(message);

        CompoundTag chatType = wrapper.user().get(DimensionRegistryStorage.class).chatType(chatTypeId);
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
