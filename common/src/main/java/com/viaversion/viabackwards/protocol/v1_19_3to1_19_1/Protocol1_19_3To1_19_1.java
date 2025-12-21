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
package com.viaversion.viabackwards.protocol.v1_19_3to1_19_1;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingData;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.text.JsonNBTComponentRewriter;
import com.viaversion.viabackwards.protocol.v1_19_1to1_19.Protocol1_19_1To1_19;
import com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.rewriter.BlockItemPacketRewriter1_19_3;
import com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.rewriter.EntityPacketRewriter1_19_3;
import com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.storage.ChatSessionStorage;
import com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.storage.ChatTypeStorage1_19_3;
import com.viaversion.viabackwards.protocol.v1_19_3to1_19_1.storage.NonceStorage;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.PlayerMessageSignature;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_19_3;
import com.viaversion.viaversion.api.minecraft.signature.SignableCommandArgumentsProvider;
import com.viaversion.viaversion.api.minecraft.signature.model.MessageMetadata;
import com.viaversion.viaversion.api.minecraft.signature.storage.ChatSession1_19_3;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.BitSetType;
import com.viaversion.viaversion.api.type.types.ByteArrayType;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.Protocol1_19_1To1_19_3;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19_1to1_19_3.packet.ServerboundPackets1_19_3;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.packet.ClientboundPackets1_19_1;
import com.viaversion.viaversion.protocols.v1_19to1_19_1.packet.ServerboundPackets1_19_1;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import com.viaversion.viaversion.rewriter.ParticleRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.rewriter.text.ComponentRewriterBase;
import com.viaversion.viaversion.util.CipherUtil;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.Pair;
import java.security.SignatureException;
import java.util.BitSet;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Protocol1_19_3To1_19_1 extends BackwardsProtocol<ClientboundPackets1_19_3, ClientboundPackets1_19_1, ServerboundPackets1_19_3, ServerboundPackets1_19_1> {

    public static final BackwardsMappingData MAPPINGS = new BackwardsMappingData("1.19.3", "1.19", Protocol1_19_1To1_19_3.class);
    public static final ByteArrayType.OptionalByteArrayType OPTIONAL_SIGNATURE_BYTES_TYPE = new ByteArrayType.OptionalByteArrayType(256);
    public static final ByteArrayType SIGNATURE_BYTES_TYPE = new ByteArrayType(256);
    private final EntityPacketRewriter1_19_3 entityRewriter = new EntityPacketRewriter1_19_3(this);
    private final BlockItemPacketRewriter1_19_3 itemRewriter = new BlockItemPacketRewriter1_19_3(this);
    private final ParticleRewriter<ClientboundPackets1_19_3> particleRewriter = new ParticleRewriter<>(this);
    private final JsonNBTComponentRewriter<ClientboundPackets1_19_3> translatableRewriter = new JsonNBTComponentRewriter<>(this, ComponentRewriterBase.ReadType.JSON);
    private final TagRewriter<ClientboundPackets1_19_3> tagRewriter = new TagRewriter<>(this);

    public Protocol1_19_3To1_19_1() {
        super(ClientboundPackets1_19_3.class, ClientboundPackets1_19_1.class, ServerboundPackets1_19_3.class, ServerboundPackets1_19_1.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_3.SYSTEM_CHAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_3.SET_ACTION_BAR_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_3.SET_TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_3.SET_SUBTITLE_TEXT);
        translatableRewriter.registerBossEvent(ClientboundPackets1_19_3.BOSS_EVENT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_3.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19_3.TAB_LIST);
        translatableRewriter.registerSetPlayerTeam1_13(ClientboundPackets1_19_3.SET_PLAYER_TEAM);
        translatableRewriter.registerOpenScreen1_14(ClientboundPackets1_19_3.OPEN_SCREEN);
        translatableRewriter.registerPlayerCombatKill(ClientboundPackets1_19_3.PLAYER_COMBAT_KILL);
        translatableRewriter.registerSetObjective(ClientboundPackets1_19_3.SET_OBJECTIVE);
        translatableRewriter.registerPing();

        particleRewriter.registerLevelParticles1_19(ClientboundPackets1_19_3.LEVEL_PARTICLES);

        final SoundRewriter<ClientboundPackets1_19_3> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerStopSound(ClientboundPackets1_19_3.STOP_SOUND);
        registerClientbound(ClientboundPackets1_19_3.SOUND, wrapper -> {
            final String mappedIdentifier = rewriteSound(wrapper);
            if (mappedIdentifier != null) {
                wrapper.write(Types.STRING, mappedIdentifier);
                wrapper.setPacketType(ClientboundPackets1_19_1.CUSTOM_SOUND);
            }
        });
        registerClientbound(ClientboundPackets1_19_3.SOUND_ENTITY, wrapper -> {
            final String mappedIdentifier = rewriteSound(wrapper);
            if (mappedIdentifier == null) {
                return;
            }

            final int mappedId = MAPPINGS.getFullSoundMappings().mappedId(mappedIdentifier);
            if (mappedId == -1) {
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.VAR_INT, mappedId);
        });

        tagRewriter.addEmptyTag(RegistryType.BLOCK, "minecraft:non_flammable_wood");
        tagRewriter.addEmptyTag(RegistryType.ITEM, "minecraft:overworld_natural_logs");
        tagRewriter.registerGeneric(ClientboundPackets1_19_3.UPDATE_TAGS);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_19_3.AWARD_STATS);

        final CommandRewriter<ClientboundPackets1_19_3> commandRewriter = new CommandRewriter<>(this);
        registerClientbound(ClientboundPackets1_19_3.COMMANDS, wrapper -> {
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
                    final int mappedArgumentTypeId = MAPPINGS.getArgumentTypeMappings().getNewId(argumentTypeId);
                    Preconditions.checkArgument(mappedArgumentTypeId != -1, "Unknown command argument type id: " + argumentTypeId);
                    wrapper.write(Types.VAR_INT, mappedArgumentTypeId);

                    final String identifier = MAPPINGS.getArgumentTypeMappings().identifier(argumentTypeId);
                    commandRewriter.handleArgument(wrapper, identifier);
                    if (identifier.equals("minecraft:gamemode")) {
                        wrapper.write(Types.VAR_INT, 0); // Word
                    }

                    if ((flags & 0x10) != 0) {
                        wrapper.passthrough(Types.STRING); // Suggestion type
                    }
                }
            }

            wrapper.passthrough(Types.VAR_INT); // Root node index
        });

        registerClientbound(ClientboundPackets1_19_3.SERVER_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.OPTIONAL_COMPONENT); // Motd
                map(Types.OPTIONAL_STRING); // Encoded icon
                create(Types.BOOLEAN, false); // Previews chat
            }
        });

        // Remove the key once again
        registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Name
                handler(wrapper -> {
                    final ProfileKey profileKey = wrapper.read(Types.OPTIONAL_PROFILE_KEY);
                    if (profileKey == null) {
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
                map(Types.BYTE_ARRAY_PRIMITIVE); // Keys
                handler(wrapper -> {
                    final NonceStorage nonceStorage = wrapper.user().remove(NonceStorage.class);
                    final boolean isNonce = wrapper.read(Types.BOOLEAN);
                    if (!isNonce) {
                        wrapper.read(Types.LONG); // Salt
                        wrapper.read(Types.BYTE_ARRAY_PRIMITIVE); // Signature
                        wrapper.write(Types.BYTE_ARRAY_PRIMITIVE, nonceStorage.nonce() != null ? nonceStorage.nonce() : new byte[0]);
                    }
                });
            }
        });

        registerServerbound(ServerboundPackets1_19_1.CHAT, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Message
                map(Types.LONG); // Timestamp
                map(Types.LONG); // Salt
                read(Types.BYTE_ARRAY_PRIMITIVE); // Signature
                read(Types.BOOLEAN); // Signed preview
                read(Types.PLAYER_MESSAGE_SIGNATURE_ARRAY); // Last seen messages
                read(Types.OPTIONAL_PLAYER_MESSAGE_SIGNATURE); // Last received message
                handler(wrapper -> {
                    final ChatSession1_19_3 chatSession = wrapper.user().get(ChatSession1_19_3.class);

                    if (chatSession != null) {
                        final String message = wrapper.get(Types.STRING, 0);
                        final long timestamp = wrapper.get(Types.LONG, 0);
                        final long salt = wrapper.get(Types.LONG, 1);

                        final MessageMetadata metadata = new MessageMetadata(null, timestamp, salt);
                        final byte[] signature;
                        try {
                            signature = chatSession.signChatMessage(metadata, message, new PlayerMessageSignature[0]);
                        } catch (final SignatureException e) {
                            throw new RuntimeException(e);
                        }

                        wrapper.write(Protocol1_19_3To1_19_1.OPTIONAL_SIGNATURE_BYTES_TYPE, signature); // Signature
                    } else {
                        wrapper.write(Protocol1_19_3To1_19_1.OPTIONAL_SIGNATURE_BYTES_TYPE, null); // Signature
                    }

                    //TODO is this fine (probably not)? same for chat_command
                    wrapper.write(Types.VAR_INT, 0); // Offset
                    wrapper.write(new BitSetType(20), new BitSet(20)); // Acknowledged
                });
            }
        });
        registerServerbound(ServerboundPackets1_19_1.CHAT_COMMAND, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.STRING); // Command
                map(Types.LONG); // Timestamp
                map(Types.LONG); // Salt
                handler(wrapper -> {
                    final ChatSession1_19_3 chatSession = wrapper.user().get(ChatSession1_19_3.class);
                    final SignableCommandArgumentsProvider argumentsProvider = Via.getManager().getProviders().get(SignableCommandArgumentsProvider.class);

                    final String command = wrapper.get(Types.STRING, 0);
                    final long timestamp = wrapper.get(Types.LONG, 0);
                    final long salt = wrapper.get(Types.LONG, 1);

                    final int signatures = wrapper.read(Types.VAR_INT);
                    for (int i = 0; i < signatures; i++) {
                        wrapper.read(Types.STRING); // Name
                        wrapper.read(Types.BYTE_ARRAY_PRIMITIVE); // Signature
                    }
                    wrapper.read(Types.BOOLEAN); // Signed preview

                    if (chatSession != null && argumentsProvider != null) {
                        final MessageMetadata metadata = new MessageMetadata(null, timestamp, salt);

                        final List<Pair<String, String>> arguments = argumentsProvider.getSignableArguments(command);
                        wrapper.write(Types.VAR_INT, arguments.size());
                        for (final Pair<String, String> argument : arguments) {
                            final byte[] signature;
                            try {
                                signature = chatSession.signChatMessage(metadata, argument.value(), new PlayerMessageSignature[0]);
                            } catch (final SignatureException e) {
                                throw new RuntimeException(e);
                            }

                            wrapper.write(Types.STRING, argument.key());
                            wrapper.write(Protocol1_19_3To1_19_1.SIGNATURE_BYTES_TYPE, signature);
                        }
                    } else {
                        wrapper.write(Types.VAR_INT, 0); // No signatures
                    }

                    final int offset = 0;
                    final BitSet acknowledged = new BitSet(20);
                    wrapper.write(Types.VAR_INT, offset);
                    wrapper.write(new BitSetType(20), acknowledged);
                });
                read(Types.PLAYER_MESSAGE_SIGNATURE_ARRAY); // Last seen messages
                read(Types.OPTIONAL_PLAYER_MESSAGE_SIGNATURE); // Last received message
            }
        });
        registerClientbound(ClientboundPackets1_19_3.PLAYER_CHAT, ClientboundPackets1_19_1.SYSTEM_CHAT, new PacketHandlers() {
            @Override
            public void register() {
                read(Types.UUID); // Sender
                read(Types.VAR_INT); // Index
                read(OPTIONAL_SIGNATURE_BYTES_TYPE); // Signature
                handler(wrapper -> {
                    final String plainContent = wrapper.read(Types.STRING);
                    wrapper.read(Types.LONG); // Timestamp
                    wrapper.read(Types.LONG); // Salt
                    final int lastSeen = wrapper.read(Types.VAR_INT);
                    for (int i = 0; i < lastSeen; i++) {
                        final int index = wrapper.read(Types.VAR_INT);
                        if (index == 0) {
                            wrapper.read(SIGNATURE_BYTES_TYPE);
                        }
                    }

                    final JsonElement unsignedContent = wrapper.read(Types.OPTIONAL_COMPONENT);
                    final JsonElement content = unsignedContent != null ? unsignedContent : ComponentUtil.plainToJson(plainContent);
                    translatableRewriter.processText(wrapper.user(), content);
                    final int filterMaskType = wrapper.read(Types.VAR_INT);
                    if (filterMaskType == 2) {
                        wrapper.read(Types.LONG_ARRAY_PRIMITIVE); // Mask
                    }

                    final int chatTypeId = wrapper.read(Types.VAR_INT);
                    final JsonElement senderName = wrapper.read(Types.COMPONENT);
                    final JsonElement targetName = wrapper.read(Types.OPTIONAL_COMPONENT);
                    final JsonElement result = Protocol1_19_1To1_19.decorateChatMessage(Protocol1_19_3To1_19_1.this, wrapper.user().get(ChatTypeStorage1_19_3.class), chatTypeId, senderName, targetName, content);
                    if (result == null) {
                        wrapper.cancel();
                        return;
                    }

                    wrapper.write(Types.COMPONENT, result);
                    wrapper.write(Types.BOOLEAN, false);
                });
            }
        });
        registerClientbound(ClientboundPackets1_19_3.DISGUISED_CHAT, ClientboundPackets1_19_1.SYSTEM_CHAT, wrapper -> {
            final JsonElement content = wrapper.read(Types.COMPONENT);
            translatableRewriter.processText(wrapper.user(), content);
            final int chatTypeId = wrapper.read(Types.VAR_INT);
            final JsonElement senderName = wrapper.read(Types.COMPONENT);
            final JsonElement targetName = wrapper.read(Types.OPTIONAL_COMPONENT);
            final JsonElement result = Protocol1_19_1To1_19.decorateChatMessage(this, wrapper.user().get(ChatTypeStorage1_19_3.class), chatTypeId, senderName, targetName, content);
            if (result == null) {
                wrapper.cancel();
                return;
            }

            wrapper.write(Types.COMPONENT, result);
            wrapper.write(Types.BOOLEAN, false);
        });

        cancelClientbound(ClientboundPackets1_19_3.UPDATE_ENABLED_FEATURES);
        cancelServerbound(ServerboundPackets1_19_1.CHAT_PREVIEW);
        cancelServerbound(ServerboundPackets1_19_1.CHAT_ACK);
    }

    private @Nullable String rewriteSound(final PacketWrapper wrapper) {
        final Holder<SoundEvent> holder = wrapper.read(Types.SOUND_EVENT);
        if (holder.hasId()) {
            final int mappedId = MAPPINGS.getSoundMappings().getNewId(holder.id());
            if (mappedId == -1) {
                wrapper.cancel();
                return null;
            }

            wrapper.write(Types.VAR_INT, mappedId);
            return null;
        }

        // Convert the resource location to the corresponding integer id
        final String soundIdentifier = holder.value().identifier();
        final String mappedIdentifier = MAPPINGS.getMappedNamedSound(soundIdentifier);
        if (mappedIdentifier == null) {
            return soundIdentifier;
        }

        if (mappedIdentifier.isEmpty()) {
            wrapper.cancel();
            return null;
        }

        return mappedIdentifier;
    }

    @Override
    public void init(final UserConnection user) {
        user.put(new ChatSessionStorage());
        user.put(new ChatTypeStorage1_19_3());
        addEntityTracker(user, new EntityTrackerBase(user, EntityTypes1_19_3.PLAYER));
    }

    @Override
    public BackwardsMappingData getMappingData() {
        return MAPPINGS;
    }

    @Override
    public JsonNBTComponentRewriter<ClientboundPackets1_19_3> getComponentRewriter() {
        return translatableRewriter;
    }

    @Override
    public BlockItemPacketRewriter1_19_3 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public ParticleRewriter<ClientboundPackets1_19_3> getParticleRewriter() {
        return particleRewriter;
    }

    @Override
    public EntityPacketRewriter1_19_3 getEntityRewriter() {
        return entityRewriter;
    }

    @Override
    public TagRewriter<ClientboundPackets1_19_3> getTagRewriter() {
        return tagRewriter;
    }
}
