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
package com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3;

import com.google.common.base.Preconditions;
import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.rewriters.SoundRewriter;
import com.viaversion.viabackwards.api.rewriters.TranslatableRewriter;
import com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.data.BackwardsMappings;
import com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.packets.BlockItemPackets1_19_3;
import com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.packets.EntityPackets1_19_3;
import com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.storage.ChatSessionStorage;
import com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.storage.ChatTypeStorage1_19_3;
import com.viaversion.viabackwards.protocol.protocol1_19_1to1_19_3.storage.NonceStorage;
import com.viaversion.viabackwards.protocol.protocol1_19to1_19_1.Protocol1_19To1_19_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.entities.Entity1_19_3Types;
import com.viaversion.viaversion.api.protocol.packet.State;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.BitSetType;
import com.viaversion.viaversion.api.type.types.ByteArrayType;
import com.viaversion.viaversion.data.entity.EntityTrackerBase;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.kyori.adventure.text.Component;
import com.viaversion.viaversion.libs.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import com.viaversion.viaversion.protocols.base.ClientboundLoginPackets;
import com.viaversion.viaversion.protocols.base.ServerboundLoginPackets;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ClientboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_1to1_19.ServerboundPackets1_19_1;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ClientboundPackets1_19_3;
import com.viaversion.viaversion.protocols.protocol1_19_3to1_19_1.ServerboundPackets1_19_3;
import com.viaversion.viaversion.rewriter.CommandRewriter;
import com.viaversion.viaversion.rewriter.StatisticsRewriter;
import com.viaversion.viaversion.rewriter.TagRewriter;
import com.viaversion.viaversion.util.CipherUtil;
import java.util.BitSet;

public final class Protocol1_19_1To1_19_3 extends BackwardsProtocol<ClientboundPackets1_19_3, ClientboundPackets1_19_1, ServerboundPackets1_19_3, ServerboundPackets1_19_1> {

    public static final BackwardsMappings MAPPINGS = new BackwardsMappings();
    public static final ByteArrayType.OptionalByteArrayType OPTIONAL_SIGNATURE_BYTES_TYPE = new ByteArrayType.OptionalByteArrayType(256);
    public static final ByteArrayType SIGNATURE_BYTES_TYPE = new ByteArrayType(256);
    private final EntityPackets1_19_3 entityRewriter = new EntityPackets1_19_3(this);
    private final BlockItemPackets1_19_3 itemRewriter = new BlockItemPackets1_19_3(this);
    private final TranslatableRewriter<ClientboundPackets1_19_3> translatableRewriter = new TranslatableRewriter<>(this);

    public Protocol1_19_1To1_19_3() {
        super(ClientboundPackets1_19_3.class, ClientboundPackets1_19_1.class, ServerboundPackets1_19_3.class, ServerboundPackets1_19_1.class);
    }

    @Override
    protected void registerPackets() {
        super.registerPackets();

        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_3.SYSTEM_CHAT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_3.ACTIONBAR);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_3.TITLE_TEXT);
        translatableRewriter.registerComponentPacket(ClientboundPackets1_19_3.TITLE_SUBTITLE);
        translatableRewriter.registerBossBar(ClientboundPackets1_19_3.BOSSBAR);
        translatableRewriter.registerDisconnect(ClientboundPackets1_19_3.DISCONNECT);
        translatableRewriter.registerTabList(ClientboundPackets1_19_3.TAB_LIST);
        translatableRewriter.registerOpenWindow(ClientboundPackets1_19_3.OPEN_WINDOW);
        translatableRewriter.registerCombatKill(ClientboundPackets1_19_3.COMBAT_KILL);
        translatableRewriter.registerPing();

        final SoundRewriter<ClientboundPackets1_19_3> soundRewriter = new SoundRewriter<>(this);
        soundRewriter.registerStopSound(ClientboundPackets1_19_3.STOP_SOUND);
        registerClientbound(ClientboundPackets1_19_3.SOUND, wrapper -> {
            final int soundId = wrapper.read(Type.VAR_INT) - 1; // Normalize the id
            if (soundId != -1) {
                final int mappedId = MAPPINGS.getSoundMappings().getNewId(soundId);
                if (mappedId == -1) {
                    wrapper.cancel();
                    return;
                }

                wrapper.write(Type.VAR_INT, mappedId);
                return;
            }

            String soundIdentifier = wrapper.read(Type.STRING);
            wrapper.read(Type.OPTIONAL_FLOAT); // Fixed range
            final String mappedIdentifier = MAPPINGS.getMappedNamedSound(soundIdentifier);
            if (mappedIdentifier != null) {
                if (mappedIdentifier.isEmpty()) {
                    wrapper.cancel();
                    return;
                }

                soundIdentifier = mappedIdentifier;
            }

            wrapper.write(Type.STRING, soundIdentifier);
            wrapper.setPacketType(ClientboundPackets1_19_1.NAMED_SOUND);
        });
        registerClientbound(ClientboundPackets1_19_3.ENTITY_SOUND, wrapper -> {
            final int soundId = wrapper.read(Type.VAR_INT) - 1; // Normalize the id
            if (soundId != -1) {
                final int mappedId = MAPPINGS.getSoundMappings().getNewId(soundId);
                if (mappedId == -1) {
                    wrapper.cancel();
                    return;
                }

                wrapper.write(Type.VAR_INT, mappedId);
                return;
            }

            // Convert the resource location to the corresponding integer id
            String soundIdentifier = wrapper.read(Type.STRING);
            wrapper.read(Type.OPTIONAL_FLOAT); // Fixed range
            final String mappedIdentifier = MAPPINGS.getMappedNamedSound(soundIdentifier);
            if (mappedIdentifier != null) {
                if (mappedIdentifier.isEmpty()) {
                    wrapper.cancel();
                    return;
                }

                soundIdentifier = mappedIdentifier;
            }

            final int mappedId = MAPPINGS.mappedSound(soundIdentifier);
            if (mappedId == -1) {
                wrapper.cancel();
                return;
            }

            wrapper.write(Type.VAR_INT, mappedId);
        });

        final TagRewriter<ClientboundPackets1_19_3> tagRewriter = new TagRewriter<>(this);
        tagRewriter.addEmptyTag(RegistryType.BLOCK, "minecraft:non_flammable_wood");
        tagRewriter.addEmptyTag(RegistryType.ITEM, "minecraft:overworld_natural_logs");
        tagRewriter.registerGeneric(ClientboundPackets1_19_3.TAGS);

        new StatisticsRewriter<>(this).register(ClientboundPackets1_19_3.STATISTICS);

        final CommandRewriter<ClientboundPackets1_19_3> commandRewriter = new CommandRewriter<>(this);
        registerClientbound(ClientboundPackets1_19_3.DECLARE_COMMANDS, wrapper -> {
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
                    final int mappedArgumentTypeId = MAPPINGS.getArgumentTypeMappings().mappings().getNewId(argumentTypeId);
                    Preconditions.checkArgument(mappedArgumentTypeId != -1, "Unknown command argument type id: " + argumentTypeId);
                    wrapper.write(Type.VAR_INT, mappedArgumentTypeId);

                    final String identifier = MAPPINGS.getArgumentTypeMappings().identifier(argumentTypeId);
                    commandRewriter.handleArgument(wrapper, identifier);
                    if (identifier.equals("minecraft:gamemode")) {
                        wrapper.write(Type.VAR_INT, 0); // Word
                    }

                    if ((flags & 0x10) != 0) {
                        wrapper.passthrough(Type.STRING); // Suggestion type
                    }
                }
            }

            wrapper.passthrough(Type.VAR_INT); // Root node index
        });

        registerClientbound(ClientboundPackets1_19_3.SERVER_DATA, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.OPTIONAL_COMPONENT); // Motd
                map(Type.OPTIONAL_STRING); // Encoded icon
                create(Type.BOOLEAN, false); // Previews chat
            }
        });

        // Remove the key once again
        registerServerbound(State.LOGIN, ServerboundLoginPackets.HELLO.getId(), ServerboundLoginPackets.HELLO.getId(), new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Name
                handler(wrapper -> {
                    final ProfileKey profileKey = wrapper.read(Type.OPTIONAL_PROFILE_KEY);
                    if (profileKey == null) {
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
                map(Type.BYTE_ARRAY_PRIMITIVE); // Keys
                handler(wrapper -> {
                    final NonceStorage nonceStorage = wrapper.user().remove(NonceStorage.class);
                    final boolean isNonce = wrapper.read(Type.BOOLEAN);
                    if (!isNonce) {
                        wrapper.read(Type.LONG); // Salt
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE); // Signature
                        wrapper.write(Type.BYTE_ARRAY_PRIMITIVE, nonceStorage.nonce() != null ? nonceStorage.nonce() : new byte[0]);
                    }
                });
            }
        });

        registerServerbound(ServerboundPackets1_19_1.CHAT_MESSAGE, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Message
                map(Type.LONG); // Timestamp
                map(Type.LONG); // Salt
                read(Type.BYTE_ARRAY_PRIMITIVE); // Signature
                create(OPTIONAL_SIGNATURE_BYTES_TYPE, null); // No signature
                read(Type.BOOLEAN); // Signed preview
                read(Type.PLAYER_MESSAGE_SIGNATURE_ARRAY); // Last seen messages
                read(Type.OPTIONAL_PLAYER_MESSAGE_SIGNATURE); // Last received message
                handler(wrapper -> {
                    //TODO is this fine (probably not)? same for chat_command
                    final int offset = 0;
                    final BitSet acknowledged = new BitSet(20);
                    wrapper.write(Type.VAR_INT, offset);
                    wrapper.write(new BitSetType(20), acknowledged);
                });
            }
        });
        registerServerbound(ServerboundPackets1_19_1.CHAT_COMMAND, new PacketHandlers() {
            @Override
            public void register() {
                map(Type.STRING); // Command
                map(Type.LONG); // Timestamp
                map(Type.LONG); // Salt
                handler(wrapper -> {
                    final int signatures = wrapper.read(Type.VAR_INT);
                    wrapper.write(Type.VAR_INT, 0);
                    for (int i = 0; i < signatures; i++) {
                        wrapper.read(Type.STRING); // Name
                        wrapper.read(Type.BYTE_ARRAY_PRIMITIVE); // Signature
                    }
                    wrapper.read(Type.BOOLEAN); // Signed preview

                    final int offset = 0;
                    final BitSet acknowledged = new BitSet(20);
                    wrapper.write(Type.VAR_INT, offset);
                    wrapper.write(new BitSetType(20), acknowledged);
                });
                read(Type.PLAYER_MESSAGE_SIGNATURE_ARRAY); // Last seen messages
                read(Type.OPTIONAL_PLAYER_MESSAGE_SIGNATURE); // Last received message
            }
        });
        registerClientbound(ClientboundPackets1_19_3.PLAYER_CHAT, ClientboundPackets1_19_1.SYSTEM_CHAT, new PacketHandlers() {
            @Override
            public void register() {
                read(Type.UUID); // Sender
                read(Type.VAR_INT); // Index
                read(OPTIONAL_SIGNATURE_BYTES_TYPE); // Signature
                handler(wrapper -> {
                    final String plainContent = wrapper.read(Type.STRING);
                    wrapper.read(Type.LONG); // Timestamp
                    wrapper.read(Type.LONG); // Salt
                    final int lastSeen = wrapper.read(Type.VAR_INT);
                    for (int i = 0; i < lastSeen; i++) {
                        final int index = wrapper.read(Type.VAR_INT);
                        if (index == 0) {
                            wrapper.read(SIGNATURE_BYTES_TYPE);
                        }
                    }

                    final JsonElement unsignedContent = wrapper.read(Type.OPTIONAL_COMPONENT);
                    final JsonElement content = unsignedContent != null ? unsignedContent : GsonComponentSerializer.gson().serializeToTree(Component.text(plainContent));
                    translatableRewriter.processText(content);
                    final int filterMaskType = wrapper.read(Type.VAR_INT);
                    if (filterMaskType == 2) {
                        wrapper.read(Type.LONG_ARRAY_PRIMITIVE); // Mask
                    }

                    final int chatTypeId = wrapper.read(Type.VAR_INT);
                    final JsonElement senderName = wrapper.read(Type.COMPONENT);
                    final JsonElement targetName = wrapper.read(Type.OPTIONAL_COMPONENT);
                    final JsonElement result = Protocol1_19To1_19_1.decorateChatMessage(wrapper.user().get(ChatTypeStorage1_19_3.class), chatTypeId, senderName, targetName, content);
                    if (result == null) {
                        wrapper.cancel();
                        return;
                    }

                    wrapper.write(Type.COMPONENT, result);
                    wrapper.write(Type.BOOLEAN, false);
                });
            }
        });
        registerClientbound(ClientboundPackets1_19_3.DISGUISED_CHAT, ClientboundPackets1_19_1.SYSTEM_CHAT, wrapper -> {
            final JsonElement content = wrapper.read(Type.COMPONENT);
            translatableRewriter.processText(content);
            final int chatTypeId = wrapper.read(Type.VAR_INT);
            final JsonElement senderName = wrapper.read(Type.COMPONENT);
            final JsonElement targetName = wrapper.read(Type.OPTIONAL_COMPONENT);
            final JsonElement result = Protocol1_19To1_19_1.decorateChatMessage(wrapper.user().get(ChatTypeStorage1_19_3.class), chatTypeId, senderName, targetName, content);
            if (result == null) {
                wrapper.cancel();
                return;
            }

            wrapper.write(Type.COMPONENT, result);
            wrapper.write(Type.BOOLEAN, false);
        });

        cancelClientbound(ClientboundPackets1_19_3.UPDATE_ENABLED_FEATURES);
        cancelServerbound(ServerboundPackets1_19_1.CHAT_PREVIEW);
        cancelServerbound(ServerboundPackets1_19_1.CHAT_ACK);
    }

    @Override
    public void init(final UserConnection user) {
        user.put(new ChatSessionStorage());
        user.put(new ChatTypeStorage1_19_3());
        addEntityTracker(user, new EntityTrackerBase(user, Entity1_19_3Types.PLAYER));
    }

    @Override
    public BackwardsMappings getMappingData() {
        return MAPPINGS;
    }

    @Override
    public TranslatableRewriter<ClientboundPackets1_19_3> getTranslatableRewriter() {
        return translatableRewriter;
    }

    @Override
    public BlockItemPackets1_19_3 getItemRewriter() {
        return itemRewriter;
    }

    @Override
    public EntityPackets1_19_3 getEntityRewriter() {
        return entityRewriter;
    }
}
