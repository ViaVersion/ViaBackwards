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
package com.viaversion.viabackwards.protocol.v1_19to1_18_2;

import com.google.common.primitives.Longs;
import com.viaversion.nbt.tag.CompoundTag;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_19_1to1_19.Protocol1_19_1To1_19;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.data.BackwardsMappingData1_19;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.rewriter.BlockItemPacketRewriter1_19;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.rewriter.CommandRewriter1_19;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.rewriter.EntityPacketRewriter1_19;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.storage.DimensionRegistryStorage;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.storage.EntityTracker1_19;
import com.viaversion.viabackwards.protocol.v1_19to1_18_2.storage.NonceStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.signature.SignableCommandArgumentsProvider;
import com.viaversion.viaversion.api.minecraft.signature.model.DecoratableMessage;
import com.viaversion.viaversion.api.minecraft.signature.model.MessageMetadata;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_0;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.v1_16_4to1_17.packet.ServerboundPackets1_17;
import com.viaversion.viaversion.protocols.v1_17_1to1_18.packet.ClientboundPackets1_18;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ClientboundPackets1_19;
import com.viaversion.viaversion.protocols.v1_18_2to1_19.packet.ServerboundPackets1_19;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.Protocol1_19To1_19_1;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.data.ChatDecorationResult;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;
import com.viaversion.viaversion.util.Pair;
import java.security.SignatureException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class Protocol1_19To1_18_2 extends BackwardsProtocol<ClientboundPackets1_19, ClientboundPackets1_18, ServerboundPackets1_19, ServerboundPackets1_17> {

    public static final BackwardsMappingData1_19 MAPPINGS = new BackwardsMappingData1_19();
    private static final UUID ZERO_UUID = new UUID(0, 0);
    private static final byte[] EMPTY_BYTES = new byte[0];
    private final EntityPacketRewriter1_19 entityRewriter = new EntityPacketRewriter1_19(this);
    private final BlockItemPacketRewriter1_19 blockItemPackets = new BlockItemPacketRewriter1_19(this);
    private final ParticleRewriter<ClientboundPackets1_19> particleRewriter = new ParticleRewriter<>(this);
    private final JsonNBTComponentRewriter<ClientboundPackets1_19> translatableRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);
    private final TagRewriter<ClientboundPackets1_19> tagRewriter = new TagRewriter<>(this);

    public Protocol1_19To1_18_2() {
        super(ClientboundPackets1_19.class, ClientboundPackets1_18.class, ServerboundPackets1_19.class, ServerboundPackets1_17.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        translatableRewriter.registerComponentPacket(ClientboundPackets1_19.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_19.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19.TAB_LIST);
        translatableRewriter.registerSetPlayerTeam1_13(ClientboundPackets1_19.SET_PLAYER_TEAM);
        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_19.OPEN_SCREEN);
        translatableRewriter.registerPlayerCombatKill(ClientboundPackets1_19.PLAYER_COMBAT_KILL);
        translatableRewriter.registerSetObjective(ClientboundPackets1_19.SET_OBJECTIVE);
        translatableRewriter.registerPing();

        final SoundRewriter<ClientboundPackets1_19> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerStopSound(ClientboundPackets1_19.STOP_SOUND);
        registerClientbound(ClientboundPackets1_19.SOUND, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Sound id
                map(Types.VAR_INT); // Source
                map(Types.INT); // X
                map(Types.INT); // Y
                map(Types.INT); // Z
                map(Types.FLOAT); // Volume
                map(Types.FLOAT); // Pitch
                read(Types.LONG); // Seed
                handler(soundRewriter.getSoundHandler());
            }
        });
        registerClientbound(ClientboundPackets1_19.SOUND_ENTITY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.VAR_INT); // Sound id
                map(Types.VAR_INT); // Source
                map(Types.VAR_INT); // Entity id
                map(Types.FLOAT); // Volume
                map(Types.FLOAT); // Pitch
                read(Types.LONG); // Seed
                handler(soundRewriter.getSoundHandler());
            }
        });
        registerClientbound(ClientboundPackets1_19.CUSTOM_SOUND, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Sound name
                map(Types.VAR_INT); // Source
                map(Types.INT); // X
                map(Types.INT); // Y
                map(Types.INT); // Z
                map(Types.FLOAT); // Volume
                map(Types.FLOAT); // Pitch
                read(Types.LONG); // Seed
                handler(soundRewriter.getNamedSoundHandler());
            }
        });

        tagRewriter.removeTags("minecraft:banner_pattern");
        tagRewriter.removeTags("minecraft:instrument");
        tagRewriter.removeTags("minecraft:cat_variant");
        tagRewriter.removeTags("minecraft:painting_variant");
        tagRewriter.addEmptyTag(RegistryType.BLOCK, "minecraft:polar_bears_spawnable_on_in_frozen_ocean");
        tagRewriter.renameTag(RegistryType.BLOCK, "minecraft:wool_carpets", "minecraft:carpets");
        tagRewriter.renameTag(RegistryType.ITEM, "minecraft:wool_carpets", "minecraft:carpets");
        tagRewriter.addEmptyTag(RegistryType.ITEM, "minecraft:occludes_vibration_signals");
        tagRewriter.registerGeneric(ClientboundPackets1_19.UPDATE_TAGS);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_19.AWARD_STATS);

        final CommandRewriter<ClientboundPackets1_19> commandRewriter = new CommandRewriter1_19(this);
        registerClientbound(ClientboundPackets1_19.COMMANDS, wrapper -> {
            final int size = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < size; i++) {
                final byte flags = wrapper.passthrough(Types.BYTE);
                wrapper.passthrough(Types.VAR_INT_ARRAY_PRIMITIVE); // Children indices
                if ((flags & 0x08) != 0) {
                    wrapper.passthrough(Types.VAR_INT); // Redirect node index
                }

                final int nodeType = flags & 0x03;
                if (nodeType == 1 || nodeType == 2) { // Literal/argument node
                    wrapper.passthrough(Types.STRING); // Name
                }

                if (nodeType == 2) { // Argument node
                    final int argumentTypeId = wrapper.read(Types.VAR_INT);
                    String argumentType = MAPPINGS.getArgumentTypeMappings().identifier(argumentTypeId);
                    if (argumentType == null) {
                        getLogger().warning("Unknown command argument type id: " + argumentTypeId);
                        argumentType = "minecraft:no";
                    }

                    wrapper.write(Types.STRING, commandRewriter.handleArgumentType(argumentType));
                    commandRewriter.handleArgument(wrapper, argumentType);

                    if ((flags & 0x10) != 0) {
                        wrapper.passthrough(Types.STRING); // Suggestion type
                    }
                }
            }

            wrapper.passthrough(Types.VAR_INT); // Root node index
        });

        cancelClientbound(ClientboundPackets1_19.SERVER_DATA);
        cancelClientbound(ClientboundPackets1_19.CHAT_PREVIEW);
        cancelClientbound(ClientboundPackets1_19.SET_DISPLAY_CHAT_PREVIEW);
        registerClientbound(ClientboundPackets1_19.PLAYER_CHAT, ClientboundPackets1_18.CHAT, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    final JsonElement signedContent = wrapper.read(Types.COMPONENT);
                    final JsonElement unsignedContent = wrapper.read(Types.OPTIONAL_COMPONENT);
                    final int chatTypeId = wrapper.read(Types.VAR_INT);
                    final UUID sender = wrapper.read(Types.UUID);
                    final JsonElement senderName = wrapper.read(Types.COMPONENT);
                    final JsonElement teamName = wrapper.read(Types.OPTIONAL_COMPONENT);

                    final CompoundTag chatType = wrapper.user().get(DimensionRegistryStorage.class).chatType(chatTypeId);
                    final ChatDecorationResult decorationResult = Protocol1_19To1_19_1.decorateChatMessage(chatType, chatTypeId, senderName, teamName, unsignedContent != null ? unsignedContent : signedContent);
                    if (decorationResult == null) {
                        wrapper.cancel();
                        return;
                    }

                    translatableRewriter.processText(wrapper.user(), decorationResult.content());
                    wrapper.write(Types.COMPONENT, decorationResult.content());
                    wrapper.write(Types.BYTE, decorationResult.overlay() ? (byte) 2 : 1);
                    wrapper.write(Types.UUID, sender);
                });
                read(Types.LONG); // Timestamp
                read(Types.LONG); // Salt
                read(Types.BYTE_ARRAY_PRIMITIVE); // Signature
            }
        });

        registerClientbound(ClientboundPackets1_19.SYSTEM_CHAT, ClientboundPackets1_18.CHAT, new PacketHandlers() {
            @Override
            public void register() {
                handler(wrapper -> {
                    final JsonElement content = wrapper.passthrough(Types.COMPONENT);
                    translatableRewriter.processText(wrapper.user(), content);

                    // Screw everything that isn't a system or game info type (which would only happen on funny 1.19.0 servers)
                    final int typeId = wrapper.read(Types.VAR_INT);
                    wrapper.write(Types.BYTE, typeId == Protocol1_19_1To1_19.GAME_INFO_ID ? (byte) 2 : (byte) 0);
                });
                create(Types.UUID, ZERO_UUID); // Sender
            }
        });

        registerServerbound(ServerboundPackets1_17.CHAT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Message
                handler(wrapper -> {
                    final ChatSession1_19_0 chatSession = wrapper.user().get(ChatSession1_19_0.class);

                    final UUID sender = wrapper.user().getProtocolInfo().getUuid();
                    final Instant timestamp = Instant.now();
                    final long salt = ThreadLocalRandom.current().nextLong();

                    wrapper.write(Types.LONG, timestamp.toEpochMilli()); // Timestamp
                    wrapper.write(Types.LONG, chatSession != null ? salt : 0L); // Salt

                    final String message = wrapper.get(Types.STRING, 0);
                    if (!message.isEmpty() && message.charAt(0) == '/') {
                        final String command = message.substring(1);

                        wrapper.setPacketType(ServerboundPackets1_19.CHAT_COMMAND);
                        wrapper.set(Types.STRING, 0, command);

                        final SignableCommandArgumentsProvider argumentsProvider = Via.getManager().getProviders().get(SignableCommandArgumentsProvider.class);
                        if (chatSession != null && argumentsProvider != null) {
                            final MessageMetadata metadata = new MessageMetadata(sender, timestamp, salt);

                            final List<Pair<String, String>> arguments = argumentsProvider.getSignableArguments(command);
                            wrapper.write(Types.VAR_INT, arguments.size());
                            for (final Pair<String, String> argument : arguments) {
                                final byte[] signature;
                                try {
                                    signature = chatSession.signChatMessage(metadata, new DecoratableMessage(argument.value()));
                                } catch (final SignatureException e) {
                                    throw new RuntimeException(e);
                                }

                                wrapper.write(Types.STRING, argument.key());
                                wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, signature);
                            }
                        } else {
                            wrapper.write(Types.VAR_INT, 0); // No signatures
                        }
                    } else {
                        if (chatSession != null) {
                            final MessageMetadata metadata = new MessageMetadata(sender, timestamp, salt);
                            final DecoratableMessage decoratableMessage = new DecoratableMessage(message);
                            final byte[] signature;
                            try {
                                signature = chatSession.signChatMessage(metadata, decoratableMessage);
                            } catch (final SignatureException e) {
                                throw new RuntimeException(e);
                            }

                            wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, signature); // Signature
                        } else {
                            wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, EMPTY_BYTES); // Signature
                        }
                    }

                    wrapper.write(Types.BOOLEAN, false); // No signed preview
                });
            }
        });

        // Login changes
        registerClientbound(State.LOGIN, ClientboundLoginPackets.LOGIN_FINISHED, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.UUID); // UUID
                map(Types.STRING); // Name
                read(Types.PROFILE_PROPERTY_ARRAY);
            }
        });

        registerClientbound(State.LOGIN, ClientboundLoginPackets.HELLO, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Server id
                map(Types.BYTE_ARRAY_PRIMITIVE); // Public key
                handler(wrapper -> {
                    if (wrapper.user().has(ChatSession1_19_0.class)) {
                        wrapper.user().put(new NonceStorage(wrapper.passthrough(Types.BYTE_ARRAY_PRIMITIVE))); // Nonce
                    }
                });
            }
        });
        registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Name
                handler(wrapper -> {
                    final ChatSession1_19_0 chatSession = wrapper.user().get(ChatSession1_19_0.class);
                    wrapper.write(Types.OPTIONAL_PROFILE_KEY, chatSession == null ? null : chatSession.getProfileKey()); // Profile Key
                });
            }
        });

        registerServerbound(State.LOGIN, ServerboundLoginPackets.ENCRYPTION_KEY, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.BYTE_ARRAY_PRIMITIVE); // Public key
                handler(wrapper -> {
                    final ChatSession1_19_0 chatSession = wrapper.user().get(ChatSession1_19_0.class);

                    final byte[] verifyToken = wrapper.read(Types.BYTE_ARRAY_PRIMITIVE); // Verify token
                    wrapper.write(Types.BOOLEAN, chatSession == null); // Is nonce
                    if (chatSession != null) {
                        final long salt = ThreadLocalRandom.current().nextLong();
                        final byte[] signature;
                        try {
                            signature = chatSession.sign(signer -> {
                                signer.accept(wrapper.user().remove(NonceStorage.class).nonce());
                                signer.accept(Longs.toByteArray(salt));
                            });
                        } catch (final SignatureException e) {
                            throw new RuntimeException(e);
                        }
                        wrapper.write(Types.LONG, salt); // Salt
                        wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, signature); // Signature
                    } else {
                        wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, verifyToken); // Nonce
                    }
                });
            }
        });
    }

    @Override
    public void init(final UserConnection user) {
        user.put(new DimensionRegistryStorage());
        addEntityTracker(user, new EntityTracker1_19(user));
    }

    @Override
    public BackwardsMappingData1_19 getMappingData() {
        return MAPPINGS;
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPackets1_19> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public EntityPacketRewriter1_19 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_19 getItemRewriter() {
        return blockItemPackets;
    }

    @Override
    public ParticleRewriter<ClientboundPackets1_19> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public TagRewriter<ClientboundPackets1_19> getTagRewriter() {
        return tagRewriter;
    }
}
